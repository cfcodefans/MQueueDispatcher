package com.thenetcircle.services.rest;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.thenetcircle.services.dispatcher.dao.GeneralDao;

@Path("jpa")
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
public class JPA_Res {
	
	@Inject GeneralDao dao;
	
	@SuppressWarnings("rawtypes")
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	public List query(@FormParam("hql") String hqlStr) {
		return dao.queryEntity(hqlStr);
	}
}
