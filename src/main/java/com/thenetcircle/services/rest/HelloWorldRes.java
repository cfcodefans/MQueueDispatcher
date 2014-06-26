package com.thenetcircle.services.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Path("helloworld")
@Produces({MediaType.TEXT_PLAIN})
public class HelloWorldRes {

	@GET
	public String sayHelloTo(@QueryParam("name") String name) {
		return "Hello " + name;
	}
}
