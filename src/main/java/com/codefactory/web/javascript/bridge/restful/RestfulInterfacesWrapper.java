package com.codefactory.web.javascript.bridge.restful;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.thenetcircle.services.common.MiscUtils;

public class RestfulInterfacesWrapper extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static Log log = LogFactory.getLog(RestfulInterfacesWrapper.class);

	public RestfulInterfacesWrapper() {

	}

	public void init() {
		log.info(MiscUtils.invocationInfo());
	}

	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

	}

	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

	}

	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

	}
	
	public void destroy() {
		log.info(MiscUtils.invocationInfo());
	}
}
