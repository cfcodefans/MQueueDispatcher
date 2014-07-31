package com.thenetcircle.services.rest;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.thenetcircle.services.cluster.JGroupsActor;
import com.thenetcircle.services.common.Jsons;
import com.thenetcircle.services.common.MiscUtils;
import com.thenetcircle.services.dispatcher.ampq.MQueues;
import com.thenetcircle.services.dispatcher.dao.QueueCfgDao;
import com.thenetcircle.services.dispatcher.dao.ServerCfgDao;
import com.thenetcircle.services.dispatcher.entity.HttpDestinationCfg;
import com.thenetcircle.services.dispatcher.entity.QueueCfg;
import com.thenetcircle.services.dispatcher.entity.ServerCfg;
import com.thenetcircle.services.dispatcher.mgr.QueueOperator;

@Path("mqueue_cfgs")
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
public class MQueueCfgRes {

	protected static final Log log = LogFactory.getLog(MQueueCfgRes.class.getName());

	@Inject
	private ServerCfgDao scDao;

	@Inject
	private QueueCfgDao qcDao;
	
//	@Inject
//	private ExchangeCfgDao ecDao;

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
//			qc = qcDao.find(qc.getId());
			MQueues.instance().updateQueueCfg(qc);
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
		
		if (on) {
			MQueues.instance().updateQueueCfg(qc);
			JGroupsActor.instance().restartQueues(qc);
		} else {
			MQueues.instance().removeQueueCfg(qc);
			JGroupsActor.instance().stopQueues(qc);
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
//		final List<QueueCfg> qcList = qcDao.findAll();
		final QueueCfg[] qcs = MQueues.instance().getQueueCfgs().toArray(new QueueCfg[0]);
		return Response.ok(qcs, MediaType.APPLICATION_XML_TYPE).header(HttpHeaders.CONTENT_ENCODING, "gzip").build();
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
	@Path("/{qc_id}")
	// @Produces({ MediaType.APPLICATION_JSON })
	// @Consumes({ MediaType.APPLICATION_JSON })
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
			MQueues.instance().updateQueueCfg(qc);
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
		{
			HttpDestinationCfg destCfg = new HttpDestinationCfg();
			qc.setDestCfg(destCfg);
		}
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
		if (qc == null || !MQueues.instance().getQueueCfgs().contains(qc)) {
			throw new WebApplicationException(
					Response.status(Status.BAD_REQUEST).entity("Queue: " + qcId + " isn't running now").build());
		}
		
		new QueueOperator(qc).sendMessage(msgStr);
	}
	
}
