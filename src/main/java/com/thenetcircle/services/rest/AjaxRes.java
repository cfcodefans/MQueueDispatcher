package com.thenetcircle.services.rest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceModel;

import com.thenetcircle.services.common.Jsons;
import com.thenetcircle.services.rest.ajax.AjaxResMetaData;


//import com.thenetcircle.services.common.MiscUtils;

@Path("ajax")
@Singleton
public class AjaxRes {

//	@PostConstruct
//	public void loadRess() {
//		log.info(MiscUtils.invocationInfo() + "\n\t");
//		build(ResLoader.getResModel());
//		log.info("\n");
//	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getJSProxy(@Context UriInfo uriInfo) {
		for (AjaxResMetaData armd : proxyList) {
			armd.setBaseUrl(StringUtils.removeEnd(uriInfo.getAbsolutePath().getPath(), "ajax"));
		}
		return Response.ok(Jsons.toString(proxyList.toArray(new AjaxResMetaData[0]))).build(); 
	}
	
	public static void build(final ResourceModel resModel) {
		Collection<Resource> resources = resModel.getResources();
		if (CollectionUtils.isEmpty(resources )) {
			return;
		}
		
		for (final Resource res : resources) {
			proxyList.add(AjaxResMetaData.build(res));
		}
	}
	
	private static Log log = LogFactory.getLog(AjaxRes.class);
	
	private static List<AjaxResMetaData> proxyList = new ArrayList<AjaxResMetaData>();
}

