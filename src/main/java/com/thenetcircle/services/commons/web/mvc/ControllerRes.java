package com.thenetcircle.services.commons.web.mvc;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.thenetcircle.services.commons.MiscUtils;

@Path("/")
//@RequestScoped
public class ControllerRes {
	private static final String RES_PATH = "/";
	protected @Context ThreadLocal<HttpServletRequest> reqInvoker;
	protected @Context ThreadLocal<HttpServletResponse> respInvoker;
	protected static final Log log = LogFactory.getLog(ControllerRes.class);
	
	public ControllerRes() {
		log.debug(MiscUtils.invocationInfo());
	}

	@Path("{sub_path}")
	@POST
	public Response post(final @Context UriInfo ui, final @PathParam("sub_path") String subPathStr, MultivaluedMap<String, String> paramsMap) {
		log.debug("subPathStr: " + subPathStr);
		final HttpServletRequest _req = reqInvoker.get();
		final HttpServletResponse _resp = respInvoker.get(); 
		final String basePath = ControllerHelper.joinPaths(ControllerHelper.getBasePath(_req), RES_PATH);
		
		_req.setAttribute("paramsMap", MiscUtils.extractParams(paramsMap));
		return Response.ok(ProcessorFactory.getViewable(_resp, _req, basePath, subPathStr, this), ProcessorFactory.getMediaTypeByPath(subPathStr)).build();
	}

	@Path("{sub_path}")
	@GET
	public Response get(final @Context UriInfo ui, final @PathParam("sub_path") String subPathStr) {
		log.debug("subPathStr: " + subPathStr);
		final HttpServletRequest _req = reqInvoker.get();
		final HttpServletResponse _resp = respInvoker.get(); 
		final String basePath = ControllerHelper.joinPaths(ControllerHelper.getBasePath(_req), RES_PATH);
		return Response.ok(ProcessorFactory.getViewable(_resp, _req, basePath, subPathStr, this), ProcessorFactory.getMediaTypeByPath(subPathStr)).build();
	}
}
