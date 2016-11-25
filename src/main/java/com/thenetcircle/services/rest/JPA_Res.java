package com.thenetcircle.services.rest;

import com.thenetcircle.services.dispatcher.dao.GeneralDao;

import javax.inject.Inject;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.Serializable;
import java.util.List;

@Path("jpa")
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
public class JPA_Res {
	
	@Inject GeneralDao dao;
	
	@SuppressWarnings("rawtypes")
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	public List<Serializable> query(@FormParam("hql") String hqlStr) {
		return dao.queryEntity(hqlStr);
	}
}
