package com.thenetcircle.services.rest;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.thenetcircle.services.common.MiscUtils;
import com.thenetcircle.services.dispatcher.dao.MessageContextDao;
import com.thenetcircle.services.dispatcher.entity.MessageContext;

@Path("mqueue_cfgs/mqueue_{id}/failed_jobs")
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
public class FailedJobRes {

	@PathParam("id")
	private Integer mqueueId;
	
	@QueryParam("status_code")
	private Integer statusCode;

	protected static final Log log = LogFactory.getLog(FailedJobRes.class.getName());

	@Inject
	private MessageContextDao mcDao;

	public FailedJobRes() {
		log.info(MiscUtils.invocationInfo());
	}

	@GET
	@Path("/{mc_id}")
	public MessageContext get(@PathParam("mc_id") int id) {
		MessageContext mc = mcDao.find(new Integer(id));
		if (mc == null) {
			throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity("invalid QueueCfg: " + id).build());
		}

		return mc;
	}

	@GET
	public List<MessageContext> getFailedJobs() {
		return mcDao.findAll();
	}

	@GET
	@Path("/page_{page_idx}")
	public List<MessageContext> getQueueCfgs(@PathParam("page_idx") int pageIdx, @QueryParam("size") int pageSize) {
		return mcDao.page(pageIdx, pageSize);
	}

	@OPTIONS
	public String options() {
		return "this endpoint is for MQueueCfg management";
	}
}
