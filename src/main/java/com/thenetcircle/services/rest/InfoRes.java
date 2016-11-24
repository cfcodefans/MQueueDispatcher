package com.thenetcircle.services.rest;

import com.thenetcircle.services.commons.MiscUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("info")
@Produces({ MediaType.TEXT_XML, MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN })
public class InfoRes {
	protected static final Log log = LogFactory.getLog(InfoRes.class.getName());

	@GET
	@Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
	public Response info() {
		return Response.ok().entity("started").build();
	}
	
	public InfoRes() {
		log.info(MiscUtils.invocationInfo());
	}
}
