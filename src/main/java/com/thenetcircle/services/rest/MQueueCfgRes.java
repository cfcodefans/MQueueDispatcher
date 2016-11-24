package com.thenetcircle.services.rest;

import com.thenetcircle.services.cluster.JGroupsActor;
import com.thenetcircle.services.commons.Jsons;
import com.thenetcircle.services.commons.MiscUtils;
import com.thenetcircle.services.dispatcher.ampq.MQueueMgr;
import com.thenetcircle.services.dispatcher.dao.QueueCfgDao;
import com.thenetcircle.services.dispatcher.dao.ServerCfgDao;
import com.thenetcircle.services.dispatcher.entity.HttpDestinationCfg;
import com.thenetcircle.services.dispatcher.entity.QueueCfg;
import com.thenetcircle.services.dispatcher.entity.ServerCfg;
import com.thenetcircle.services.dispatcher.mgr.QueueOperator;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.Collection;
import java.util.List;

@Path("mqueue_cfgs")
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
public class MQueueCfgRes {

	protected static final Logger log = LogManager.getLogger(MQueueCfgRes.class);

	@Inject
	private ServerCfgDao scDao;

	@Inject
	private QueueCfgDao qcDao;
	
	public MQueueCfgRes() {
		log.info(MiscUtils.invocationInfo());
	}

	private QueueCfg prepare(final QueueCfg qc) {
		if (qc == null) {
			return qc;
		}

		ServerCfg sc = qc.getServerCfg();
		Integer scId = sc.getId();
		if (sc == null || scId < 0) {
			throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity("null ServerCfg: ").build());
		}

		sc = scDao.find(scId);
		if (sc == null) {
			throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity("invalid ServerCfg: " + scId).build());
		}

		qc.setServerCfg(sc);
		if (qc.getId() < 0) {
			qc.getDestCfg().setId(-1);
		}
		
		return qc;
	}

	@PUT
	@Produces({ MediaType.APPLICATION_JSON })
	@Consumes({ MediaType.APPLICATION_FORM_URLENCODED })
	public QueueCfg create(@FormParam("entity") final String reqStr) {
		if (StringUtils.isEmpty(reqStr)) {
			throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity("invalid MQueueCfg: " + reqStr).build());
		}

		QueueCfg qc = null;
		try {
			qc = Jsons.read(reqStr, QueueCfg.class);
		} catch (Exception e) {
			log.error("failed to deserialize QueueCfg with: \n" + reqStr, e);
			throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity("invalid MQueueCfg: " + reqStr).build());
		}

		qc = prepare(qc);

		try {
			qc = qcDao.create(qc);
			MQueueMgr.instance().updateQueueCfg(qc);
			JGroupsActor.instance().restartQueues(qc);
			return qc;
		} catch (Exception e) {
			log.error("failed to save QueueCfg with: \n" + reqStr, e);
			throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity("can't save MQueueCfgRes: " + e.getMessage()).build());
		}
	}

	@DELETE
	@Path("/{qc_id}")
	@Produces(MediaType.APPLICATION_JSON)
	public QueueCfg switchQueue(@PathParam("qc_id") int id, @QueryParam("on") boolean on) {
		QueueCfg qc = qcDao.find(new Integer(id));
		if (qc == null) {
			throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity("invalid QueueCfg: " + id).build());
		}

		qc.setEnabled(on);
		qc = qcDao.update(qc);
		
		JGroupsActor jgoup = JGroupsActor.instance();
		MQueueMgr queueMgr = MQueueMgr.instance();
		if (on) {
			queueMgr.updateQueueCfg(qc);
			jgoup.restartQueues(qc);
		} else {
			queueMgr.stopQueue(qc);
			jgoup.stopQueues(qc);
		}
		
		return qc;
	}

	@GET
	@Path("/{qc_id}.xml")
	@Produces({ MediaType.APPLICATION_XML })
	public QueueCfg get(@PathParam("qc_id") int id) {
		final QueueCfg qc = qcDao.find(new Integer(id));
		if (qc == null) {
			throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity("invalid QueueCfg: " + id).build());
		}

		return qc;
	}

	@GET
	@Path("/{qc_id}.json")
	@Produces({ MediaType.APPLICATION_JSON })
	public QueueCfg getJson(@PathParam("qc_id") int id) {
		return get(id);
	}

	@GET
	@Produces({ MediaType.APPLICATION_XML })
	public Response getAll() {
		final List<QueueCfg> qcList = loadAll();
		return Response.ok(qcList.toArray(new QueueCfg[0]), MediaType.APPLICATION_XML_TYPE).header(HttpHeaders.CONTENT_ENCODING, "gzip").build();
	}
	
	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	@Path("json")
	public Response getAllJson() {
		final List<QueueCfg> qcList = loadAll();
		return Response.ok(qcList.toArray(new QueueCfg[0]), MediaType.APPLICATION_JSON).header(HttpHeaders.CONTENT_ENCODING, "gzip").build();
	}

	private List<QueueCfg> loadAll() {
		final List<QueueCfg> qcList = qcDao.findAll();
		final Collection<QueueCfg> queueCfgs = MQueueMgr.instance().getQueueCfgs();
		final Collection<QueueCfg> nonStartedQueueCfgs = CollectionUtils.subtract(qcList, queueCfgs);
		nonStartedQueueCfgs.forEach(qc->qc.setEnabled(false));
		return qcList;
	}

	@GET
	@Path("/page_{page_idx}")
	public List<QueueCfg> getQueueCfgs(@PathParam("page_idx") int pageIdx, @QueryParam("size") int pageSize) {
		// qcDao.page(pageIdx,  pageSize);
		final List<QueueCfg> qcPage = qcDao.findAll();
		return qcPage;
	}

	@OPTIONS
	public String options() {
		return "this endpoint is for MQueueCfg management";
	}

	@POST
	public QueueCfg update(@FormParam("entity") final String reqStr) {
		if (StringUtils.isEmpty(reqStr)) {
			throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity("invalid MQueueCfg: " + reqStr).build());
		}

		QueueCfg qc = null;
		try {
			qc = Jsons.read(reqStr, QueueCfg.class);
		} catch (Exception e) {
			log.error("failed to deserialize QueueCfg with: \n" + reqStr, e);
			throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity("invalid MQueueCfg: " + reqStr).build());
		}

		qc = prepare(qc);
		
		try {
			qc = qcDao.update(qc);
			MQueueMgr.instance().updateQueueCfg(qc);
			JGroupsActor.instance().restartQueues(qc);
			return qc;
		} catch (Exception e) {
			log.error("failed to save QueueCfg with: \n" + reqStr, e);
			throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity("can't save MQueueCfgRes: " + e.getMessage()).build());
		}
	}

	@GET
	@Path("new/json")
	@Produces({ MediaType.APPLICATION_JSON })
	public QueueCfg newQueueCfg() {
		QueueCfg qc = new QueueCfg();
		HttpDestinationCfg destCfg = new HttpDestinationCfg();
		qc.setDestCfg(destCfg);
		return qc;
	}
	
	@POST
	@Path("/{qc_id}/send")
	@Consumes({ MediaType.APPLICATION_FORM_URLENCODED })
	public void sendMessage(@FormParam("message") final String msgStr, @PathParam("qc_id") Integer qcId) {
		if (StringUtils.isEmpty(msgStr) || qcId == null) {
			throw new WebApplicationException(
					Response.status(Status.BAD_REQUEST).entity("invalid request: " + qcId + " message: " + msgStr).build());
		}
		
		final QueueCfg qc = qcDao.find(qcId);
		if (qc == null || !MQueueMgr.instance().isQueueRunning(qc)) {
			throw new WebApplicationException(
					Response.status(Status.BAD_REQUEST).entity("Queue: " + qcId + " isn't running now").build());
		}
		
		new QueueOperator(qc).sendMessage(msgStr);
	}
}
