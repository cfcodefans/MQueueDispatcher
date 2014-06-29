package com.thenetcircle.services.rest;

import java.util.Collection;
import java.util.HashSet;
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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.thenetcircle.services.common.Jsons;
import com.thenetcircle.services.common.MiscUtils;
import com.thenetcircle.services.dispatcher.ampq.MQueues;
import com.thenetcircle.services.dispatcher.dao.ExchangeCfgDao;
import com.thenetcircle.services.dispatcher.dao.QueueCfgDao;
import com.thenetcircle.services.dispatcher.dao.ServerCfgDao;
import com.thenetcircle.services.dispatcher.entity.ExchangeCfg;
import com.thenetcircle.services.dispatcher.entity.HttpDestinationCfg;
import com.thenetcircle.services.dispatcher.entity.QueueCfg;
import com.thenetcircle.services.dispatcher.entity.ServerCfg;

@Path("mqueue_cfgs")
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
public class MQueueCfgRes {

	protected static final Log log = LogFactory.getLog(MQueueCfgRes.class.getName());

	@Inject
	private ExchangeCfgDao ecDao;

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

		if (CollectionUtils.isEmpty(qc.getExchanges())) {
			ExchangeCfg _ec = QueueCfg.defaultExchange(qc);
			qc.getExchanges().add(_ec);
			_ec.getQueues().add(qc);
		} else {
			Collection<ExchangeCfg> ecs = new HashSet<ExchangeCfg>(qc.getExchanges());
			qc.getExchanges().clear();
			for (ExchangeCfg ec : ecs) {
				if (ec == null) {
					continue;
				}
				if (ec.getId() < 0) {
					ec = ecDao.create(ec);
				}
				ec = ecDao.find(ec.getId());
				ec.getQueues().add(qc);
				qc.getExchanges().add(ec);
			}
		}

		return qc;
	}

	@PUT
	@Produces({ MediaType.APPLICATION_JSON })
	@Consumes({ MediaType.APPLICATION_JSON })
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
		
		for (ExchangeCfg ec : qc.getExchanges()) {
			ec = ecDao.edit(ec);
			ec.getQueues().add(qc);
		}

		try {
			qc = qcDao.edit(qc);
			MQueues.instance().initWithQueueCfg(qc);
			return qc;
		} catch (Exception e) {
			log.error("failed to save QueueCfg with: \n" + reqStr, e);
			throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity("can't save MQueueCfgRes: " + e.getMessage()).build());
		}
	}

	@DELETE
	@Path("/{qc_id}")
	public QueueCfg delete(@PathParam("qc_id") int id) {
		QueueCfg qc = qcDao.find(new Integer(id));
		if (qc == null) {
			throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity("invalid QueueCfg: " + id).build());
		}

		MQueues.instance().removeQueueCfg(qc);
		qc.setEnabled(false);
		return qcDao.edit(qc);
	}

	@GET
	@Path("/{qc_id}")
	@Produces({ MediaType.APPLICATION_XML })
	public QueueCfg get(@PathParam("qc_id") int id) {
		QueueCfg qc = qcDao.find(new Integer(id));
		if (qc == null) {
			throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity("invalid QueueCfg: " + id).build());
		}

		return qc;
	}

	@GET
	@Path("/{qc_id}/json")
	@Produces({ MediaType.APPLICATION_JSON })
	public QueueCfg getJson(@PathParam("qc_id") int id) {
		return get(id);
	}

	@GET
	@Produces({ MediaType.APPLICATION_XML })
	public List<QueueCfg> getAll() {
		List<QueueCfg> qcList = qcDao.findAll();
		return qcList;
	}

	@GET
	@Path("/page_{page_idx}")
	public List<QueueCfg> getQueueCfgs(@PathParam("page_idx") int pageIdx, @QueryParam("size") int pageSize) {
		List<QueueCfg> qcPage = qcDao.findAll();// qcDao.page(pageIdx,
												// pageSize);
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
		
		for (ExchangeCfg ec : qc.getExchanges()) {
			ec = ecDao.edit(ec);
			ec.getQueues().add(qc);
		}

		try {
			qc = qcDao.edit(qc);
			MQueues.instance().initWithQueueCfg(qc);
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
}
