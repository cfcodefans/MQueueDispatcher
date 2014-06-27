package com.thenetcircle.services.rest;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.thenetcircle.services.common.Jsons;
import com.thenetcircle.services.dispatcher.dao.ServerCfgDao;
import com.thenetcircle.services.dispatcher.entity.ServerCfg;

@Path("server_cfgs")
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
public class ServerCfgRes {

	protected static final Log log = LogFactory.getLog(ServerCfgRes.class.getSimpleName());
	@Inject
	private ServerCfgDao scDao;

	@PUT
	@Consumes({ MediaType.APPLICATION_JSON })
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
	public ServerCfg get(@PathParam("id") Integer id) {
		if (id == null) {
			throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity("invalid ServerCfg.id: " + id).build());
		}

		ServerCfg sc = scDao.find(id);
		if (sc == null) {
			throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity("ServerCfg.id: " + id).build());
		}

		return sc;
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("{id}/json")
	public String getJson(@PathParam("id") Integer id) {
		return Jsons.toString(get(id));
	}

	@GET
	public List<ServerCfg> getAll() {
		return scDao.findAll();
	}

	@OPTIONS
	public String options() {
		return "ServerCfg Resource";
	}

	@POST
	public ServerCfg update(@FormParam("entity") final String reqStr) {
		if (StringUtils.isEmpty(reqStr)) {
			throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity("invalid ServerCfg: " + reqStr).build());
		}

		try {
			ServerCfg sc = Jsons.read(reqStr, ServerCfg.class);
			if (sc == null) {
				throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity("invalid ServerCfg: " + reqStr).build());
			}

			return scDao.edit(sc);
		} catch (Exception e) {
			log.error("failed to save ServerCfg: \n\t" + reqStr, e);
			throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity("can't save ServerCfg: " + e.getMessage()).build());
		}
	}
}
