package com.thenetcircle.services.rest;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.SseFeature;

import com.thenetcircle.services.dispatcher.dao.QueueCfgDao;
import com.thenetcircle.services.dispatcher.entity.QueueCfg;

@Path("monitor")
public class MonitorRes {

	protected static final Log log = LogFactory.getLog(MonitorRes.class.getName());
	
	private static final ExecutorService es = Executors.newCachedThreadPool();
	
	private static class Worker implements Runnable {
		private EventOutput eventOutput = null;
		
		public Worker(EventOutput eventOutput) {
			super();
			this.eventOutput = eventOutput;
		}

		@Override
		public void run() {
			
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
		
		return eventOutput;
	}
	
}
