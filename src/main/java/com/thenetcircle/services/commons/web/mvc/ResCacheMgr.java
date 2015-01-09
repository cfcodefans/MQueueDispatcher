package com.thenetcircle.services.commons.web.mvc;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.collect.MapMaker;

@WebListener
public class ResCacheMgr implements ServletContextListener, Runnable {

	public static final Map<String, Object> cacheMap = new MapMaker().softValues().makeMap();

	private static Log log = LogFactory.getLog(ResCacheMgr.class);
	
	private ScheduledExecutorService worker = null;

	@Override
	public void contextDestroyed(ServletContextEvent ce) {
		
	}

	@Override
	public void contextInitialized(ServletContextEvent ce) {
		String realPath = ce.getServletContext().getRealPath(".");
		log.info("getRealPath(): " + realPath);
		
		log.info("start a thread to validate the cache entry");
		
		worker = Executors.newScheduledThreadPool(1);
		
		worker.scheduleAtFixedRate(this, 10, 30, TimeUnit.SECONDS);
	}

	public void run() {
		//valid the cache entry
		log.info("validate the cache entry");
	}
}
