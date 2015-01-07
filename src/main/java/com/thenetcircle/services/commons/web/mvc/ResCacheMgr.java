package com.thenetcircle.services.commons.web.mvc;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@WebListener
public class ResCacheMgr implements ServletContextListener {

	public static Map<String, Object> cacheMap = new ConcurrentHashMap<String, Object>();
	private static Log log = LogFactory.getLog(ResCacheMgr.class);

	@Override
	public void contextDestroyed(ServletContextEvent ce) {
		String realPath = ce.getServletContext().getRealPath(".");
		
		log.info("getRealPath(): " + realPath);
	}

	@Override
	public void contextInitialized(ServletContextEvent ce) {
		
	}
}
