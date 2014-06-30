package com.thenetcircle.services.rest;

import javax.inject.Singleton;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Path("dummy")
@Singleton
@Produces(MediaType.TEXT_HTML)
public class DummyRes {

	@GET
	public String getFoo(@QueryParam("param_1") String param1) {
		return "this is dummy: " + param1;
	}
	
	@POST
	public String postFoo(@FormParam("param_1") String param1) {
		return "this is dummy: " + param1;
	}
}
