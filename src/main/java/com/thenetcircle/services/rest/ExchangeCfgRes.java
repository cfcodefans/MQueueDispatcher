package com.thenetcircle.services.rest;

import com.thenetcircle.services.commons.Jsons;
import com.thenetcircle.services.dispatcher.ampq.MQueueMgr;
import com.thenetcircle.services.dispatcher.dao.ExchangeCfgDao;
import com.thenetcircle.services.dispatcher.dao.ServerCfgDao;
import com.thenetcircle.services.dispatcher.entity.ExchangeCfg;
import com.thenetcircle.services.dispatcher.entity.ServerCfg;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.List;

import static javax.ws.rs.core.MediaType.*;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

@Path("exchange_cfgs")
@Produces({ APPLICATION_JSON, APPLICATION_XML})
public class ExchangeCfgRes {

	protected static final Log log = LogFactory.getLog(ExchangeCfgRes.class.getSimpleName());
	@Inject
	private ExchangeCfgDao ecDao;

	@Inject
	private ServerCfgDao scDao;
	
	private ExchangeCfg prepare(final ExchangeCfg ec) {
		ServerCfg sc = ec.getServerCfg();
		Integer scId = sc.getId();
		if (sc == null || scId < 0) {
			throw new WebApplicationException(Response.status(BAD_REQUEST).entity("null ServerCfg: ").build());
		}
		
		sc = scDao.find(scId);
		if (sc == null) {
			throw new WebApplicationException(Response.status(BAD_REQUEST).entity("invalid ServerCfg: " + scId).build());
		}
		
		ec.setServerCfg(sc);
		return ec;
	}
	
	@PUT
	public ExchangeCfg create(@FormParam("entity") final String reqStr) {
		if (StringUtils.isEmpty(reqStr)) {
			throw new WebApplicationException(Response.status(BAD_REQUEST).entity("invalid ExchangeCfg: " + reqStr).build());
		}

		try {
			ExchangeCfg ec = Jsons.read(reqStr, ExchangeCfg.class);
			if (ec == null) {
				throw new WebApplicationException(Response.status(BAD_REQUEST).entity("invalid ExchangeCfg: " + reqStr).build());
			}

			return ecDao.create(prepare(ec));
		} catch (Exception e) {
			log.error("failed to save ExchangeCfg: \n\t" + reqStr, e);
			throw new WebApplicationException(Response.status(INTERNAL_SERVER_ERROR).entity("can't save ExchangeCfg: " + e.getMessage()).build());
		}
	}

	@GET
	@Path("{id}")
	@Produces(APPLICATION_XML)
	public ExchangeCfg get(@PathParam("id") Integer id) {
		if (id == null) {
			throw new WebApplicationException(Response.status(BAD_REQUEST).entity("invalid ExchangeCfg.id: " + id).build());
		}

		ExchangeCfg ec = ecDao.find(id);
		if (ec == null) {
			throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity("ExchangeCfg.id: " + id).build());
		}

		return ec;
	}
	
	@GET
	@Path("{id}.json")
	@Produces(APPLICATION_JSON)
	public ExchangeCfg getJson(@PathParam("id") Integer id) {
		return get(id);
	}

	@GET
	@Produces(APPLICATION_XML)
	public Response getAll() {
		final List<ExchangeCfg> ecList = ecDao.findAll();
		return Response.ok(ecList.toArray(new ExchangeCfg[0]), APPLICATION_XML_TYPE).header(HttpHeaders.CONTENT_ENCODING, "gzip").build();
	}
	
	@GET
	@Produces(APPLICATION_JSON)
	@Path("json")
	public Response getAllJson() {
		final List<ExchangeCfg> ecList = ecDao.findAll();
		return Response.ok(ecList.toArray(new ExchangeCfg[0]), APPLICATION_JSON).header(HttpHeaders.CONTENT_ENCODING, "gzip").build();
	}

	@OPTIONS
	public String options() {
		return "ExchangeCfg Resource";
	}

	@POST
	@Produces({APPLICATION_JSON})
	public ExchangeCfg update(@FormParam("entity") final String reqStr) {
		if (StringUtils.isEmpty(reqStr)) {
			throw new WebApplicationException(Response.status(BAD_REQUEST).entity("invalid ExchangeCfg: " + reqStr).build());
		}

		try {
			ExchangeCfg ec = Jsons.read(reqStr, ExchangeCfg.class);
			if (ec == null) {
				throw new WebApplicationException(Response.status(BAD_REQUEST).entity("invalid ExchangeCfg: " + reqStr).build());
			}

			final ExchangeCfg edited = ecDao.update(prepare(ec));
			MQueueMgr.instance().updateExchange(edited);

			return edited;
		} catch (Exception e) {
			log.error("failed to save ExchangeCfg: \n\t" + reqStr, e);
			throw new WebApplicationException(Response.status(INTERNAL_SERVER_ERROR).entity("can't save ExchangeCfg: " + e.getMessage()).build());
		}
	}
	
	@GET
	@Path("new/json")
	@Produces({ APPLICATION_JSON })
	public ExchangeCfg newExchangeCfg() {
		return new ExchangeCfg();
	}
	
	@GET
	@Path("/by_server")
	@Produces({ APPLICATION_XML })
	public List<ExchangeCfg> getExchangesByServer(@QueryParam("server_id") int srvId) {
		return ecDao.findExchangesByServer(scDao.find(srvId));
	}
	
	@GET
	@Path("/by_server/json")
	@Produces(APPLICATION_JSON )
	public List<ExchangeCfg> getExchangesJsonByServer(@QueryParam("server_id") int srvId) {
		return ecDao.findExchangesByServer(scDao.find(srvId));
	}
}
