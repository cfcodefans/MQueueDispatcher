package com.thenetcircle.services.rest;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;

@Path("mqueue_cfgs")
@Produces({ MediaType.TEXT_XML, MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN, MediaType.TEXT_HTML })
public class MQueueCfgRes {

	@POST
	@Produces({MediaType.APPLICATION_JSON})
	@Consumes({MediaType.APPLICATION_JSON})
	public Response createMQueueCfg(final String reqStr) {
		if (StringUtils.isEmpty(reqStr)) {
			return Response.status(Status.BAD_REQUEST).entity("invalid MQueueCfg: " + reqStr).build();
		}
		
		
		
		return Response.ok().build();
	}
	

}
