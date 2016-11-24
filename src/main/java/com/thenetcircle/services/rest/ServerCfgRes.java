package com.thenetcircle.services.rest;

import com.thenetcircle.services.commons.Jsons;
import com.thenetcircle.services.dispatcher.ampq.MQueueMgr;
import com.thenetcircle.services.dispatcher.dao.ServerCfgDao;
import com.thenetcircle.services.dispatcher.entity.ServerCfg;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.List;

import static javax.ws.rs.core.HttpHeaders.CONTENT_ENCODING;
import static javax.ws.rs.core.MediaType.*;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

@Path("server_cfgs")
@Produces({APPLICATION_JSON, APPLICATION_XML})
public class ServerCfgRes {

    protected static final Logger log = LogManager.getLogger(ServerCfgRes.class);
    @Inject
    private ServerCfgDao scDao;

    @PUT
    public ServerCfg create(@FormParam("entity") final String reqStr) {
        if (StringUtils.isEmpty(reqStr)) {
            throw new WebApplicationException(Response.status(BAD_REQUEST).entity("invalid ServerCfg: " + reqStr).build());
        }

        try {
            ServerCfg sc = Jsons.read(reqStr, ServerCfg.class);
            if (sc == null) {
                throw new WebApplicationException(Response.status(BAD_REQUEST).entity("invalid ServerCfg: " + reqStr).build());
            }
            return scDao.create(sc);
        } catch (Exception e) {
            log.error("failed to save ServerCfg: \n\t" + reqStr, e);
            throw new WebApplicationException(Response.status(INTERNAL_SERVER_ERROR).entity("can't save ServerCfg: " + e.getMessage()).build());
        }
    }

    @GET
    @Path("{id}")
    @Produces(APPLICATION_XML)
    public ServerCfg get(@PathParam("id") Integer id) {
        if (id == null) {
            throw new WebApplicationException(Response.status(BAD_REQUEST).entity("invalid ServerCfg.id: " + id).build());
        }

        final ServerCfg sc = scDao.find(id);
        if (sc == null) {
            throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity("ServerCfg.id: " + id).build());
        }

        return sc;
    }

    @GET
    @Path("{id}/json")
    @Produces(APPLICATION_JSON)
    public ServerCfg getJson(@PathParam("id") Integer id) {
        return get(id);
    }

    @GET
    @Produces(APPLICATION_XML)
    // public List<ServerCfg> getAll() {
    public Response getAll() {
        final List<ServerCfg> scList = scDao.findAll();
        // return scList;
        return Response.ok(scList.toArray(new ServerCfg[0]), APPLICATION_XML_TYPE).header(CONTENT_ENCODING, "gzip").build();
    }

    @GET
    @Produces(APPLICATION_JSON)
    @Path("json")
    public Response getAllJson() {
        final List<ServerCfg> scList = scDao.findAll();
        return Response.ok(scList.toArray(new ServerCfg[0]), APPLICATION_JSON).header(CONTENT_ENCODING, "gzip").build();
    }


    @OPTIONS
    public String options() {
        return "ServerCfg Resource";
    }

    @POST
    @Produces({APPLICATION_JSON})
    public ServerCfg update(@FormParam("entity") final String reqStr) {
        if (StringUtils.isEmpty(reqStr)) {
            throw new WebApplicationException(Response.status(BAD_REQUEST).entity("invalid ServerCfg: " + reqStr).build());
        }

        try {
            final MQueueMgr qm = MQueueMgr.instance();
            ServerCfg sc = Jsons.read(reqStr, ServerCfg.class);
            if (sc == null) {
                throw new WebApplicationException(Response.status(BAD_REQUEST).entity("invalid ServerCfg: " + reqStr).build());
            }

            final ServerCfg edited = scDao.edit(sc);
            qm.updateServerCfg(edited);

            return edited;
        } catch (Exception e) {
            log.error("failed to save ServerCfg: \n\t" + reqStr, e);
            throw new WebApplicationException(Response.status(INTERNAL_SERVER_ERROR).entity("can't save ServerCfg: " + e.getMessage()).build());
        }
    }
}
