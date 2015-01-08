package com.thenetcircle.services.commons.web.mvc;

import java.util.Map;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.collect.MapMaker;

@WebListener
public class ResCacheMgr implements ServletContextListener {

	public static final Map<String, Object> cacheMap = new MapMaker().softValues().makeMap();

	private static Log log = LogFactory.getLog(ResCacheMgr.class);

	@Override
	public void contextDestroyed(ServletContextEvent ce) {
		
	}

	@Override
	public void contextInitialized(ServletContextEvent ce) {
		String realPath = ce.getServletContext().getRealPath(".");
		log.info("getRealPath(): " + realPath);
	}

}
