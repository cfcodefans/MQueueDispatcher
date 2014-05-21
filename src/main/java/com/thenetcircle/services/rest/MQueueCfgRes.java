package com.thenetcircle.services.rest;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.thenetcircle.services.common.Jsons;
import com.thenetcircle.services.common.MiscUtils;
import com.thenetcircle.services.dispatcher.ampq.MQueues;
import com.thenetcircle.services.dispatcher.dao.ExchangeCfgDao;
import com.thenetcircle.services.dispatcher.dao.QueueCfgDao;
import com.thenetcircle.services.dispatcher.entity.ExchangeCfg;
import com.thenetcircle.services.dispatcher.entity.QueueCfg;

@Path("mqueue_cfgs")
@Produces({ MediaType.TEXT_XML, MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN, MediaType.TEXT_HTML })
public class MQueueCfgRes {

	@Inject
	private QueueCfgDao qcDao;
	
	@Inject
	private ExchangeCfgDao ecDao;
	
	protected static final Log log = LogFactory.getLog(MQueueCfgRes.class.getName());
	
	public MQueueCfgRes() {
		log.info(MiscUtils.invocationInfo());
	}

	@PUT
	@Produces({ MediaType.APPLICATION_JSON })
	@Consumes({ MediaType.APPLICATION_JSON })
	public Response createMQueueCfg(final String reqStr) {
		if (StringUtils.isEmpty(reqStr)) {
			return Response.status(Status.BAD_REQUEST).entity("invalid MQueueCfg: " + reqStr).build();
		}

		QueueCfg qc = null;
		try {
			qc = Jsons.read(reqStr, QueueCfg.class);
		} catch (Exception e) {
			log.error("failed to deserialize QueueCfg with: \n" + reqStr, e);
			return Response.status(Status.BAD_REQUEST).entity(Jsons.toString(e)).build();
		}
		
		for (ExchangeCfg ec : qc.getExchanges()) {
			ec = ecDao.edit(ec);
			ec.getQueues().add(qc);
		}
		
		try {
			qcDao.edit(qc);
			MQueues.instance().initWithQueueCfg(qc);
		} catch (Exception e) {
			log.error("failed to save QueueCfg with: \n" + reqStr, e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Jsons.toString(e)).build();
		}

		return Response.ok().build();
	}

	@POST
	@Produces({ MediaType.APPLICATION_JSON })
	@Consumes({ MediaType.APPLICATION_JSON })
	public Response updateMQueueCfg(final String reqStr) {
		if (StringUtils.isEmpty(reqStr)) {
			return Response.status(Status.BAD_REQUEST).entity("invalid MQueueCfg: " + reqStr).build();
		}

		QueueCfg qc = null;
		try {
			qc = Jsons.read(reqStr, QueueCfg.class);
		} catch (Exception e) {
			log.error("failed to deserialize QueueCfg with: \n" + reqStr, e);
			return Response.status(Status.BAD_REQUEST).entity(Jsons.toString(e)).build();
		}
		
		for (ExchangeCfg ec : qc.getExchanges()) {
			ec = ecDao.edit(ec);
			ec.getQueues().add(qc);
		}
		
		try {
			MQueues.instance().removeQueueCfg(qc);
			qcDao.edit(qc);
			MQueues.instance().initWithQueueCfg(qc);
		} catch (Exception e) {
			log.error("failed to save QueueCfg with: \n" + reqStr, e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Jsons.toString(e)).build();
		}

		return Response.ok().build();
	}
	
	@DELETE
	@Path("/{qc_id}")
	public Response deleteQueueCfg(@PathParam("qc_id") int id) {
		QueueCfg qc = qcDao.find(new Integer(id));
		if (qc == null) {
			return Response.status(Status.BAD_REQUEST).entity("invalid QueueCfg: " + id).build();
		}
		
		MQueues.instance().removeQueueCfg(qc);
		qc.setEnabled(false);
		qcDao.edit(qc);
		
		return Response.ok().build();
	}
	
	@GET
	@Path("/{qc_id}")
	public Response getQueueCfg(@PathParam("qc_id") int id) {
		QueueCfg qc = qcDao.find(new Integer(id));
		if (qc == null) {
			return Response.status(Status.BAD_REQUEST).entity("invalid QueueCfg: " + id).build();
		}
		
		return Response.ok().entity(Jsons.toString(qc)).build();
	}
	
	@GET
	@Path("all")
	public Response getQueueCfgs() {
		List<QueueCfg> qcList = qcDao.findAll();
		return Response.ok().entity(Jsons.toString(qcList)).build();
	}
	
	@GET
	@Path("info")
	public Response info() {
		return Response.ok().entity("this endpoint is for MQueueCfg management").build();
	}
}
