package com.thenetcircle.services.rest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.glassfish.jersey.server.model.ResourceModel;

import com.thenetcircle.services.common.Jsons;
import com.thenetcircle.services.rest.javascript.bridge.JQueryAjaxProxy;
import com.thenetcircle.services.rest.javascript.bridge.ProxyBuilder;


//import com.thenetcircle.services.common.MiscUtils;

@Path("js")
@Singleton
public class JavaScriptProxyRes {

//	@PostConstruct
//	public void loadRess() {
//		log.info(MiscUtils.invocationInfo() + "\n\t");
//		build(ResLoader.getResModel());
//		log.info("\n");
//	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getJSProxy() {
		return Response.ok(Jsons.toString(proxyList.toArray(new JQueryAjaxProxy[0]))).build(); 
	}
	
	public static void build(final ResourceModel resModel) {
		Collection<Resource> resources = resModel.getResources();
		if (CollectionUtils.isEmpty(resources )) {
			return;
		}
		
		for (final Resource res : resources) {
			traverse(res);
		}
	}
	
	private static void traverse(final Resource res) {
		List<Resource> childResList = res.getChildResources();
		if (CollectionUtils.isNotEmpty(childResList)) {
			for (final Resource childRes : childResList) {
				traverse(childRes);
			}
		}
		
		for (final ResourceMethod resMd : res.getAllMethods()) {
			log.info(resMd.getHttpMethod() + "\t" + res.getPath());
			
			proxyList.add(ProxyBuilder.builder().with(resMd).build());
		}
	}

	private static Log log = LogFactory.getLog(JavaScriptProxyRes.class);
	
	private static List<JQueryAjaxProxy> proxyList = new ArrayList<JQueryAjaxProxy>();
}
