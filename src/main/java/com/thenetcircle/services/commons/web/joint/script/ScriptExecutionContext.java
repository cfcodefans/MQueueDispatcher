package com.thenetcircle.services.commons.web.joint.script;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import javax.script.Bindings;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.SimpleScriptContext;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ScriptExecutionContext {
//	public BeanManager beanMgr;
	public HttpServletRequest req;
	public HttpServletResponse resp;
	public String basePathStr;
	public ScriptContext sc;
	
	protected String scriptStr = StringUtils.EMPTY;
	
	protected CompiledScript compiledScript = null;
	
	private static Log log = LogFactory.getLog(ScriptExecutionContext.class);
	
	public String getScriptStr() {
		return scriptStr;
	}
	
	public CompiledScript getCompiledScript() {
		return compiledScript;
	}

	public ScriptExecutionContext(final String scriptScript,
								 final HttpServletRequest req, 
								 final HttpServletResponse resp, 
								 final String basePathStr,
								 final ScriptContext sc) {
		super();
		this.req = req;
		this.resp = resp;
		this.basePathStr = basePathStr;
		this.sc = sc;
		this.scriptStr = scriptScript;
	}
	
	public ScriptExecutionContext(final HttpServletRequest req, 
								 final HttpServletResponse resp, 
								 final String basePathStr,
								 final String currentPathStr,
								 final ScriptContext sc) {
		super();
		this.req = req;
		this.resp = resp;
		this.basePathStr = basePathStr;
		this.sc = sc;
		this.scriptStr = getTextResource(currentPathStr, basePathStr);
	}


	public ScriptContext inflateScriptContext(final ScriptEngine se) {
		if (sc == null) {
			sc = new SimpleScriptContext();
		}
		
		Bindings bindings = sc.getBindings(ScriptContext.GLOBAL_SCOPE);
		if (bindings == null) {
			bindings = se.createBindings();
			sc.setBindings(bindings, ScriptContext.GLOBAL_SCOPE);
		}
		
		bindings.put("req", req);
		bindings.put("resp", resp);

		return sc;
	}


	public String getTextResource(final String currentPathStr, final String pathStr) {
		if (StringUtils.isBlank(pathStr)) {
			return StringUtils.EMPTY;
		}
		
		if (URI.create(pathStr).isAbsolute()) {
			log.error("not running external scripts on server side!\n\t" + pathStr);
			return StringUtils.EMPTY;
		}
		
		final ServletContext rootCtx = req.getServletContext();
		final String rootPath = rootCtx.getContextPath();
		String resPath = StringUtils.EMPTY;
		if (pathStr.startsWith(rootPath)) {
			resPath = StringUtils.substringAfter(pathStr, rootPath);
		} else {
			final String path = FilenameUtils.getPath(currentPathStr);
			resPath = (path + currentPathStr);
		}
		
		log.info("load Script: " + resPath);
		final InputStream resIS = rootCtx.getResourceAsStream(resPath);
		try {
			final String scriptStr = resIS != null ? IOUtils.toString(resIS) : StringUtils.EMPTY;
			return scriptStr;
		} catch (final IOException e) {
			log.error("failed to load script from path: " + pathStr, e);
		}
		return StringUtils.EMPTY;
	}
	
}