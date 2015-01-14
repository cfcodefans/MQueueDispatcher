package com.thenetcircle.services.commons.web.mvc;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
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

	public static class CachedEntry<E> {
		public final Date catchedDate;
		public final E content;

		public CachedEntry(E _entry) {
			catchedDate = new Date();
			this.content = _entry;
		}

		@Override
		public String toString() {
			return "CachedEntry [catchedDate=" + catchedDate + ", content=" + content + "]";
		}
	}

	public static final Map<String, CachedEntry<?>> cacheMap = new MapMaker().softValues().makeMap();

	private static Log log = LogFactory.getLog(ResCacheMgr.class);

	private String realPathStr = null;

	private ServletContext servletCtx = null;

	private ScheduledExecutorService worker = null;

	@Override
	public void contextDestroyed(ServletContextEvent ce) {
		cacheMap.clear();
	}

	@Override
	public void contextInitialized(ServletContextEvent ce) {
		servletCtx = ce.getServletContext();
		realPathStr = servletCtx.getRealPath(".");
		log.info("getRealPath(): " + realPathStr);

		log.info("start a thread to validate the cache entry");

		worker = Executors.newScheduledThreadPool(1);
		worker.scheduleAtFixedRate(this, 10, 5, TimeUnit.SECONDS);
	}

	private boolean validate(final String pathStr) {
		if (StringUtils.isBlank(pathStr)) {
			return false;
		}

		String absoluteResPathStr = realPathStr + pathStr;

		try {
			URL resUrl = servletCtx.getResource(pathStr);
			if (resUrl == null) {
				log.info(String.format("resource is invalid at path: %s", absoluteResPathStr));
				return false;
			}
		} catch (MalformedURLException e) {
			log.error("invalid resource path", e);
			return false;
		}

		Path path = Paths.get(absoluteResPathStr);
		if (!Files.isReadable(path)) {
			log.info(String.format("resource is not readable at path: %s", absoluteResPathStr));
			return false;
		}

		CachedEntry<?> entry = cacheMap.get(pathStr);
		if (entry == null) {
			log.info(String.format("resource is null at path: %s", absoluteResPathStr));
			return false;
		}

		try {
			if (Files.getLastModifiedTime(path).toMillis() > entry.catchedDate.getTime()) {
				return false;
			}
		} catch (IOException e) {
			log.error(String.format("failed to get last modified time of resource at path: %s", absoluteResPathStr), e);
			return false;
		}

		return true;
	}

	public void run() {
		// valid the cache entry
		log.debug("validate the cache entry");
		if (StringUtils.isBlank(realPathStr)) {
			log.error("path of web application is blank");
			return;
		}

		if (MapUtils.isEmpty(cacheMap)) {
			log.info("cacheMap is empty now!");
			return;
		}

		final List<String> invalidPathList = new LinkedList<String>();

		for (final String pathStr : cacheMap.keySet()) {
			if (validate(pathStr))
				continue;
			invalidPathList.add(pathStr);
		}

		for (final String invalidPathStr : invalidPathList) {
			cacheMap.remove(invalidPathStr);
			log.warn(String.format("resource path: %s is invalid, evicted from %s", invalidPathStr, this.getClass().getSimpleName()));
		}
	}
}
