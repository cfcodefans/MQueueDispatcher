package com.thenetcircle.services.commons.web.mvc;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.collect.MapMaker;

@WebListener
public class ResCacheMgr implements ServletContextListener, Runnable {

	public static final Map<String, Object> cacheMap = new MapMaker().softValues().makeMap();

	private static Log log = LogFactory.getLog(ResCacheMgr.class);
	
	private String realPathStr = null;
	
	private ServletContext servletCtx = null;
	
	private ScheduledExecutorService worker = null;

	@Override
	public void contextDestroyed(ServletContextEvent ce) {
		
	}

	@Override
	public void contextInitialized(ServletContextEvent ce) {
		servletCtx = ce.getServletContext();
		realPathStr = servletCtx.getRealPath(".");
		log.info("getRealPath(): " + realPathStr);
		
		log.info("start a thread to validate the cache entry");
		
		worker = Executors.newScheduledThreadPool(1);
		
		worker.scheduleAtFixedRate(this, 10, 30, TimeUnit.SECONDS);
	}

	public void run() {
		//valid the cache entry
		log.info("validate the cache entry");
		if (StringUtils.isBlank(realPathStr)) {
			log.error("path of web application is blank");
			return;
		}
		
		if (MapUtils.isEmpty(cacheMap)) {
			log.info("cacheMap is empty now!");
			return;
		}

		final List<String> invalidPathList  = new LinkedList<String>();
		
		for (final String pathStr : cacheMap.keySet()) {
			if (StringUtils.isBlank(pathStr)) {
				invalidPathList.add(pathStr);
			}
			
			try {
				URL resUrl = servletCtx.getResource(pathStr);
				if (resUrl == null) {
					invalidPathList.add(pathStr);
				}
			} catch (MalformedURLException e) {
				log.error("invalid resource path", e);
				invalidPathList.add(pathStr);
			}
		}
		
		for (final String invalidPathStr : invalidPathList) {
			cacheMap.remove(invalidPathStr);
			log.warn(String.format("resource path: %s is invalid, evicted from %s", invalidPathStr, this.getClass().getSimpleName()));
		}
	}
}
