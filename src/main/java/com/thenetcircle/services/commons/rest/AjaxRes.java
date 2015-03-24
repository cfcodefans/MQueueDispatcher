package com.thenetcircle.services.commons.rest;

import java.util.List;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.thenetcircle.services.commons.Jsons;
import com.thenetcircle.services.commons.rest.ajax.AjaxResMetaData;
import com.thenetcircle.services.commons.rest.utils.AjaxResLoader.AjaxResContext;

@Path("ajax")
@Singleton
public class AjaxRes {

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getJSProxy(@Context UriInfo uriInfo, @QueryParam("appName") String name) {
		List<AjaxResMetaData> proxyList = AjaxResContext.getInstance(name).getProxyList();
		proxyList.forEach(armd -> armd.setBaseUrl(StringUtils.removeEnd(uriInfo.getAbsolutePath().getPath(), "ajax")));
		return Response.ok(Jsons.toString(proxyList.toArray(new AjaxResMetaData[0]))).build();
	}

	private static Log log = LogFactory.getLog(AjaxRes.class);
}
