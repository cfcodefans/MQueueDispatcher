package com.thenetcircle.services.rest;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
import org.glassfish.jersey.media.sse.SseFeature;

import com.thenetcircle.services.dispatcher.IMessageActor;
import com.thenetcircle.services.dispatcher.dao.QueueCfgDao;
import com.thenetcircle.services.dispatcher.entity.MessageContext;
import com.thenetcircle.services.dispatcher.entity.QueueCfg;
import com.thenetcircle.services.dispatcher.mgr.Monitor;

@Path("monitor")
public class MonitorRes {

	protected static final Log log = LogFactory.getLog(MonitorRes.class.getName());
	
	private static final ExecutorService es = Executors.newCachedThreadPool();
	
	private static class Watcher extends IMessageActor.AsyncMessageActor {
		private EventOutput eventOutput = null;
		
		private QueueCfg qc = null;
		
		public Watcher(EventOutput eventOutput, QueueCfg _qc) {
			super();
			this.eventOutput = eventOutput;
			this.qc = _qc;
		}

		@Override
		public MessageContext handle(final MessageContext mc) {
			final OutboundEvent.Builder eventBuilder = new OutboundEvent.Builder();
			try {
				OutboundEvent oe = null;
				
				log.info("Monitor for Queue: " + qc.getQueueName());
				
				if (mc != null) {
					oe = eventBuilder.mediaType(MediaType.APPLICATION_XML_TYPE).data(mc).build();
				} else {
					oe = eventBuilder.mediaType(MediaType.TEXT_PLAIN_TYPE).data("nothing").build();
				}
				
				eventOutput.write(oe);
			} catch (IOException e) {
				throw new RuntimeException("Error when writing the event.", e);
			}
			return mc;
		}
		
		@Override
		public void run() {
			try {
				while (!(Thread.interrupted())) {
					handle(buf.poll(WAIT_FACTOR, WAIT_FACTOR_UNIT));
				}
			} catch (Exception e) {
				log.error("Responder is interrupted", e);
			} finally {
				try {
					eventOutput.close();
				} catch (IOException ioClose) {
					throw new RuntimeException("Error when closing the event output.", ioClose);
				}
			}
			log.info("Responder quits");
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
		
		Watcher watcher = new Watcher(eventOutput, qc);
		Monitor.instance().register(qc, watcher);
		es.submit(watcher);
		
		return eventOutput;
	}
	
	public static void shutdown() {
		es.shutdownNow();
	}
}
