package com.thenetcircle.services.rest;

import com.thenetcircle.services.commons.actor.ConcurrentAsynActor;
import com.thenetcircle.services.dispatcher.ampq.MQueueMgr;
import com.thenetcircle.services.dispatcher.dao.QueueCfgDao;
import com.thenetcircle.services.dispatcher.entity.MessageContext;
import com.thenetcircle.services.dispatcher.entity.QueueCfg;
import com.thenetcircle.services.dispatcher.mgr.MsgMonitor;
import com.thenetcircle.services.dispatcher.mgr.QueueOperator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.OutboundEvent;
import org.glassfish.jersey.media.sse.SseBroadcaster;
import org.glassfish.jersey.media.sse.SseFeature;
import org.glassfish.jersey.server.ChunkedOutput;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.concurrent.atomic.AtomicInteger;

@Path("monitor")
public class MonitorRes {

    protected static final Logger log = LogManager.getLogger(MonitorRes.class);

    private static class Watcher extends ConcurrentAsynActor<MessageContext> {
        // private EventOutput eventOutput = null;
        private SseBroadcaster broadcaster = new SseBroadcaster() {
            private AtomicInteger cnt = new AtomicInteger(0);

            public void onException(final ChunkedOutput<OutboundEvent> chunkedOutput, final Exception exception) {
                log.warn("Monitor ends: " + exception);
                exception.printStackTrace();
            }

            public synchronized void onClose(ChunkedOutput<OutboundEvent> chunkedOutput) {
                log.info("Monitor ends");
                super.remove(chunkedOutput);
                if (cnt.get() <= 0) {
                    MsgMonitor.instance().unregister(qc);
                }
            }

            public synchronized <OUT extends ChunkedOutput<OutboundEvent>> boolean add(final OUT chunkedOutput) {
                cnt.incrementAndGet();
                return super.add(chunkedOutput);
            }

            public synchronized <OUT extends ChunkedOutput<OutboundEvent>> boolean remove(final OUT chunkedOutput) {
                cnt.decrementAndGet();
                return super.remove(chunkedOutput);
            }
        };

        private QueueCfg qc = null;

        public Watcher(QueueCfg _qc) {
            super();
            this.qc = _qc;
        }

        public MessageContext handover(final MessageContext mc) {
            return handle(mc);
        }

        final OutboundEvent.Builder eventBuilder = new OutboundEvent.Builder();

        @Override
        public MessageContext handle(final MessageContext mc) {
            try {
                OutboundEvent oe = null;
                if (mc != null) {
                    oe = eventBuilder.mediaType(MediaType.APPLICATION_JSON_TYPE).data(mc).build();
                    log.info("Monitor for Queue: " + mc.getQueueCfg().getQueueName());
                } else {
                    oe = eventBuilder.mediaType(MediaType.TEXT_PLAIN_TYPE).data("nothing").build();
                }

                broadcaster.broadcast(oe);
            } catch (Exception e) {
                throw new RuntimeException("Error when writing the event.", e);
            }
            return mc;
        }

        @Override
        public void stop() {
            super.stop();
            broadcaster.closeAll();
        }
    }

    @Inject
    private QueueCfgDao qcDao;

    @GET
    @Path("/queue/{id}/running")
    @Produces(SseFeature.SERVER_SENT_EVENTS)
    public EventOutput runningStatus(@PathParam("id") Integer queueId) {
        final QueueCfg qc = qcDao.find(queueId);
        if (queueId < 0) {
            throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity("wrong id: " + queueId).build());
        }

        final EventOutput eventOutput = new EventOutput();

        MsgMonitor monitor = MsgMonitor.instance();
        Watcher watcher = (Watcher) monitor.getQueueMonitor(qc);
        if (watcher == null) {
            watcher = new Watcher(qc);
        }
        monitor.register(qc, new Watcher(qc));

        watcher.broadcaster.add(eventOutput);
        return eventOutput;
    }

    public static void shutdown() {
        // es.shutdownNow();
    }

    @GET
    @Path("/queue/{id}/message_count")
    @Produces(MediaType.TEXT_PLAIN)
    public Long getMessageCountOnQueue(@PathParam("id") Integer qcId) {
        if (qcId == null) {
            throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity("invalid request: " + qcId).build());
        }

        final QueueCfg qc = qcDao.find(qcId);
        if (qc == null || !MQueueMgr.instance().isQueueRunning(qc)) {
            throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity("Queue: " + qcId + " isn't running now").build());
        }

        return new QueueOperator(qc).getTotalMessageCount();
    }

}
