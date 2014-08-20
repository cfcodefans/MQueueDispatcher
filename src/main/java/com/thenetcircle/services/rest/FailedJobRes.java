package com.thenetcircle.services.rest;

import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.thenetcircle.services.common.MiscUtils;
import com.thenetcircle.services.dispatcher.dao.MessageContextDao;
import com.thenetcircle.services.dispatcher.dao.QueueCfgDao;
import com.thenetcircle.services.dispatcher.entity.MessageContext;
import com.thenetcircle.services.dispatcher.entity.QueueCfg;
import com.thenetcircle.services.dispatcher.http.HttpDispatcherActor;

@Path("mqueue_cfgs/queue_{qc_id}/failed_jobs")
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
public class FailedJobRes {

	protected static final Log log = LogFactory.getLog(FailedJobRes.class.getName());

	@PathParam("qc_id")
	private Integer qcId;
	
	@Inject
	private MessageContextDao mcDao;
	@Inject
	private QueueCfgDao qcDao;	

	public FailedJobRes() {
		log.info(MiscUtils.invocationInfo());
	}

	@GET
	@Path("/{mc_id}.xml")
	@Produces(MediaType.APPLICATION_XML)
	public MessageContext get(@PathParam("mc_id") int id) {
		MessageContext mc = mcDao.find(new Long(id));
		if (mc == null) {
			throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity("invalid QueueCfg: " + id).build());
		}

		return mc;
	}
	
	@GET
	@Path("/{mc_id}.json")
	@Produces(MediaType.APPLICATION_JSON)
	public MessageContext getJson(@PathParam("mc_id") int id) {
		MessageContext mc = mcDao.find(new Long(id));
		if (mc == null) {
			throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity("invalid QueueCfg: " + id).build());
		}

		return mc;
	}
	
	@POST
	@Path("/{mc_id}/resend")
	public MessageContext resendFailedMsg(@PathParam("mc_id") int id) {
		final MessageContext mc = mcDao.find(new Long(id));
		if (mc == null) {
			throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity("invalid QueueCfg: " + id).build());
		}

		HttpDispatcherActor.instance().handover(mc);
		return mc;
	}
	
	@DELETE
	@Path("/{mc_id}/resend") 
	public MessageContext deleteFailedMsg(@PathParam("mc_id") int id) {
		final MessageContext mc = mcDao.find(new Long(id));
		if (mc == null) {
			throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity("invalid QueueCfg: " + id).build());
		}
		mcDao.destroy(mc);
		return mc;
	}
	
	@GET
	public List<MessageContext> getFailedJobs() {
		final QueueCfg qc = qcDao.find(qcId);
		final Date now = new Date();
		final Date start = DateUtils.addDays(now, -100);
		return mcDao.queryFailedJobs(qc, start, now);
	}

	@GET
	@Path("/page_{page_idx}")
	public List<MessageContext> getFailedMessages(@PathParam("page_idx") int pageIdx, @QueryParam("size") int pageSize) {
		return mcDao.page(pageIdx, pageSize);
	}
	
	@GET
	@Path("/query")
	public List<MessageContext> queryFailedMessages(@QueryParam("start") long _start,
													@QueryParam("end") long _end) {
		final QueueCfg qc = qcDao.find(qcId);
		final Date start = new Date(_start);
		final Date end = new Date(_end);
		final String pattern = "MM-dd HH:mm:ss";
		log.info(String.format("query 1000 failed jobs with in \n\t %s - %s", 
				DateFormatUtils.format(start, pattern),
				DateFormatUtils.format(end, pattern)));
		return mcDao.queryFailedJobs(qc, start, end);
	}

	@OPTIONS
	public String options() {
		return "this endpoint is for MQueueCfg management";
	}
	
	@POST
	@Path("resend")
	@Produces(MediaType.TEXT_PLAIN)
	public void resendFailedMsgs() {
		if (qcId == null) {
			throw new WebApplicationException(
					Response.status(Status.BAD_REQUEST).entity("invalid request: " + qcId).build());
		}
		
		log.info("retrying messages for queue: " + qcId);
		
		final List<MessageContext> failedMsgs = getFailedJobs();
		for (final MessageContext mc : failedMsgs) {
			log.info("retrying message: \n\t" + mc);
			HttpDispatcherActor.instance().handover(mc);
		}
	}
}
