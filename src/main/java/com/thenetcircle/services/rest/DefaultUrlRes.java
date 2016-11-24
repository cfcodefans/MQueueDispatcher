package com.thenetcircle.services.rest;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("default")
@Produces(MediaType.TEXT_PLAIN)
public class DefaultUrlRes {

	@GET
	@Path("ok")
	public String getOk() {
		return "ok";
	}
	
	@GET
	@Path("ko")
	public String getKo() {
		return "ko";
	}

	@POST
	@Path("ok")
	public String postOk() {
		return "ok";
	}
	
	@POST
	@Path("ko")
	public String postKo() {
		return "ko";
	}
	
	@POST
	@Path("echo")
	public String postEcho(final String reqStr) {
		return reqStr;
	}
	
	@GET
	@Path("echo")
	public String getEcho(final String reqStr) {
		return reqStr;
	}	
	

}
