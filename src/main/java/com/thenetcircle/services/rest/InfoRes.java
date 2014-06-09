package com.thenetcircle.services.rest;

import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.thenetcircle.services.common.MiscUtils;

@Path("info")
@Produces({ MediaType.TEXT_XML, MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN, MediaType.TEXT_HTML })
public class InfoRes {
	protected static final Log log = LogFactory.getLog(InfoRes.class.getName());

	@OPTIONS
	@Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
	public Response info() {
		return Response.ok().entity("started").build();
	}
	
	public InfoRes() {
		log.info(MiscUtils.invocationInfo());
	}
}
