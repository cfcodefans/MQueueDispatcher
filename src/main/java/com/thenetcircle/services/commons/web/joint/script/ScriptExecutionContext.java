package com.thenetcircle.services.commons.web.joint.script;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.SimpleScriptContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.thenetcircle.services.commons.web.mvc.ResCacheMgr;

public class ScriptExecutionContext {

	public HttpServletRequest req;
	public HttpServletResponse resp;
	public String basePathStr;
	public ScriptContext sc;
	public String mimeType;
	
	protected String scriptStr = StringUtils.EMPTY;
	
	private static Log log = LogFactory.getLog(ScriptExecutionContext.class);
	
	public String getScriptStr() {
		return scriptStr;
	}
	
	public ScriptExecutionContext(final String scriptScript,
								 final HttpServletRequest req, 
								 final HttpServletResponse resp, 
								 final String basePathStr,
								 final ScriptContext sc,
								 final String mimeType) {
		super();
		this.req = req;
		this.resp = resp;
		this.basePathStr = basePathStr;
		this.sc = sc;
		this.scriptStr = scriptScript;
		this.mimeType = mimeType;
	}
	
	public ScriptExecutionContext(HttpServletRequest req, 
								  HttpServletResponse resp, 
								  String baseUriStr, 
								  String currentPathStr, 
								  ScriptContext sc2,
								  String mimeType) {
		this(ResCacheMgr.getTextResource(req.getServletContext(), currentPathStr, baseUriStr), req, resp, currentPathStr, sc2, mimeType);
	}

	public ScriptContext inflateScriptContext(final ScriptEngine se) {
		if (sc == null) {
			sc = new SimpleScriptContext();
		}
		
		Bindings bindings = sc.getBindings(ScriptContext.GLOBAL_SCOPE);
		if (bindings == null) {
			log.debug("bindings are null, need initilization");
			bindings = se.createBindings();
			sc.setBindings(bindings, ScriptContext.GLOBAL_SCOPE);
		}
		
		bindings.put("req", req);
		bindings.put("resp", resp);

		return sc;
	}
	
}