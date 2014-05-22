package com.thenetcircle.services.rest.javascript.bridge;

import java.util.List;

import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;

import com.thenetcircle.services.common.MiscUtils;

//@Path("javascript")
//@Produces(MediaType.APPLICATION_JSON)
public class JavaScriptProxyRes {

	
	
	public static void build(final ResourceConfig resCfg) {
		if (resCfg == null) {
			return;
		}

		log.info(MiscUtils.invocationInfo() + " {");
		
		for (final Resource res : resCfg.getResources()) {
			traverse(res);
		}
		log.info("} " + MiscUtils.invocationInfo());
	}
	
	private static void traverse(final Resource res) {
		List<Resource> childResList = res.getChildResources();
		if (CollectionUtils.isNotEmpty(childResList)) {
			for (final Resource childRes : childResList) {
				traverse(childRes);
			}
		}
		
		for (final ResourceMethod resMd : res.getAllMethods()) {
			log.info(resMd.getHttpMethod() + "\t" + res.getPath() + "/");
		}
	}

	public static class ProxyBuilder {
		// public static ProxyBuilder
	}

	private static Log log = LogFactory.getLog(JavaScriptProxyRes.class);
}
