package com.thenetcircle.services.commons.web.joint.joint.script;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.SimpleScriptContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;

public class PageScriptExecutionContext extends ScriptExecutionContext {
	public Element document;
	public Element scriptElement;
	
	private String scriptStr = StringUtils.EMPTY;
	
	private static Log log = LogFactory.getLog(PageScriptExecutionContext.class);
	
	public String getScriptStr() {
		if (StringUtils.isBlank(scriptStr) && scriptElement != null) {
			scriptStr = getTextResource(basePathStr, scriptElement.attr("src"));
			if (StringUtils.isBlank(scriptStr)) {
				scriptStr = scriptElement.textNodes().get(0).getWholeText();
			}
		}
		
		return scriptStr;
	}
	
	
	public PageScriptExecutionContext(final Element document, 
								 final Element scriptElement, 
								 final HttpServletRequest req, 
								 final HttpServletResponse resp, 
								 final String basePathStr,
								 final ScriptContext sc) {
		super(null, req, resp, basePathStr, sc);
		this.document = document;
		this.scriptElement = scriptElement;
	}

	public PageScriptExecutionContext(final String scriptScript,
								 final Element document,
								 final HttpServletRequest req, 
								 final HttpServletResponse resp, 
								 final String basePathStr,
								 final ScriptContext sc) {
		super(scriptScript, req, resp, basePathStr, sc);
		this.document = document;
		this.scriptElement = new Element(Tag.valueOf("script"), "");
		scriptElement.text(scriptScript);
	}
	
	public PageScriptExecutionContext(final HttpServletRequest req, 
								 final Element document,
								 final HttpServletResponse resp, 
								 final String basePathStr,
								 final String currentPathStr,
								 final ScriptContext sc) {
		super(null, req, resp, basePathStr, sc);
		this.document = document;
		this.scriptElement = new Element(Tag.valueOf("script"), "");
		this.scriptStr = getTextResource(currentPathStr, basePathStr);
		scriptElement.text(this.scriptStr);
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
		
		bindings.put("j$", document);
		bindings.put("req", req);
		bindings.put("resp", resp);
		bindings.put("self", scriptElement);

		return sc;
	}
	
}