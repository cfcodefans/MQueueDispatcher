package com.thenetcircle.services.rest;

import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.OutboundEvent;
import org.glassfish.jersey.media.sse.SseBroadcaster;
import org.glassfish.jersey.media.sse.SseFeature;
import org.glassfish.jersey.server.ChunkedOutput;

import com.thenetcircle.services.common.MiscUtils;
import com.thenetcircle.services.dispatcher.IMessageActor;
import com.thenetcircle.services.dispatcher.ampq.MQueueMgr;
import com.thenetcircle.services.dispatcher.dao.QueueCfgDao;
import com.thenetcircle.services.dispatcher.entity.MessageContext;
import com.thenetcircle.services.dispatcher.entity.QueueCfg;
import com.thenetcircle.services.dispatcher.mgr.MsgMonitor;
import com.thenetcircle.services.dispatcher.mgr.QueueOperator;

@Path("monitor")
public class MonitorRes {

	protected static final Log log = LogFactory.getLog(MonitorRes.class.getName());

	private static class Watcher extends IMessageActor.AsyncMessageActor {
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
			// this.eventOutput = eventOutput;
			 this.qc = _qc;
		}

		public MessageContext handover(final MessageContext mc) {
			return handle(mc);
		}

		@Override
		public MessageContext handle(final MessageContext mc) {
			final OutboundEvent.Builder eventBuilder = new OutboundEvent.Builder();
			try {
				OutboundEvent oe = null;

				if (mc != null) {
					oe = eventBuilder.mediaType(MediaType.APPLICATION_JSON_TYPE).data(mc).build();
					log.info("Monitor for Queue: " + mc.getQueueCfg().getQueueName());
				} else {
					oe = eventBuilder.mediaType(MediaType.TEXT_PLAIN_TYPE).data("nothing").build();
				}

				// eventOutput.write(oe);
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

		Watcher watcher = (Watcher) MsgMonitor.instance().getQueueMonitor(qc);
		if (watcher == null) {
			watcher = new Watcher(qc);
			MsgMonitor.instance().register(qc, watcher);
		}

		watcher.broadcaster.add(eventOutput);
		// es.submit(watcher);

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
	
	@GET
	@Path("/report")
	@Produces(MediaType.TEXT_PLAIN)
	public String testMailReport() {
		try {
			MiscUtils.sendMail("sh.thenetcircle.com", 25, "dispatcher@thenetcircle.com", "fan@thenetcircle.com", "test", "test");
		} catch (Exception e) {
			e.printStackTrace();
			return e.getMessage();
		}
		return "ok";
	}
}
