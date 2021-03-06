package com.thenetcircle.services.rest;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.thenetcircle.services.cluster.JGroupsActor;
import com.thenetcircle.services.commons.Jsons;
import com.thenetcircle.services.dispatcher.ampq.MQueueMgr;
import com.thenetcircle.services.dispatcher.dao.QueueCfgDao;
import com.thenetcircle.services.dispatcher.dao.ServerCfgDao;
import com.thenetcircle.services.dispatcher.entity.QueueCfg;
import com.thenetcircle.services.dispatcher.entity.ServerCfg;

@Path("server_cfgs")
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
public class ServerCfgRes {

	protected static final Log log = LogFactory.getLog(ServerCfgRes.class.getSimpleName());
	@Inject
	private ServerCfgDao scDao;
	
	@Inject
	private QueueCfgDao qcDao;

	@PUT
	public ServerCfg create(@FormParam("entity") final String reqStr) {
		if (StringUtils.isEmpty(reqStr)) {
			throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity("invalid ServerCfg: " + reqStr).build());
		}

		try {
			ServerCfg sc = Jsons.read(reqStr, ServerCfg.class);
			if (sc == null) {
				throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity("invalid ServerCfg: " + reqStr).build());
			}

			return scDao.create(sc);
		} catch (Exception e) {
			log.error("failed to save ServerCfg: \n\t" + reqStr, e);
			throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity("can't save ServerCfg: " + e.getMessage()).build());
		}
	}

	@GET
	@Path("{id}")
	@Produces(MediaType.APPLICATION_XML)
	public ServerCfg get(@PathParam("id") Integer id) {
		if (id == null) {
			throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity("invalid ServerCfg.id: " + id).build());
		}

		final ServerCfg sc = scDao.find(id);
		if (sc == null) {
			throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity("ServerCfg.id: " + id).build());
		}

		return sc;
	}

	@GET
	@Path("{id}/json")
	@Produces(MediaType.APPLICATION_JSON)
	public ServerCfg getJson(@PathParam("id") Integer id) {
		return get(id);
	}

	@GET
	@Produces(MediaType.APPLICATION_XML)
	// public List<ServerCfg> getAll() {
	public Response getAll() {
		final List<ServerCfg> scList = scDao.findAll();
		// return scList;
		return Response.ok(scList.toArray(new ServerCfg[0]), MediaType.APPLICATION_XML_TYPE).header(HttpHeaders.CONTENT_ENCODING, "gzip").build();
	}

	@OPTIONS
	public String options() {
		return "ServerCfg Resource";
	}

	@POST
	@Produces({ MediaType.APPLICATION_JSON })
	public ServerCfg update(@FormParam("entity") final String reqStr) {
		if (StringUtils.isEmpty(reqStr)) {
			throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity("invalid ServerCfg: " + reqStr).build());
		}

		try {
			final MQueueMgr qm = MQueueMgr.instance();
			ServerCfg sc = Jsons.read(reqStr, ServerCfg.class);
			if (sc == null) {
				throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity("invalid ServerCfg: " + reqStr).build());
			}

			final ServerCfg edited = scDao.edit(sc);
			qm.updateServerCfg(edited);
			
			return edited;
		} catch (Exception e) {
			log.error("failed to save ServerCfg: \n\t" + reqStr, e);
			throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity("can't save ServerCfg: " + e.getMessage()).build());
		}
	}
}
