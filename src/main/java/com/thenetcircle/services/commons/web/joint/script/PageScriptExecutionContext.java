package com.thenetcircle.services.commons.web.joint.script;

import java.util.List;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.SimpleScriptContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;

public class PageScriptExecutionContext extends ScriptExecutionContext {
	public Element document;
	public Element scriptElement;
	
//	private static Log log = LogFactory.getLog(PageScriptExecutionContext.class);
	
	public String getScriptStr() {
		if (StringUtils.isBlank(scriptStr) && scriptElement != null) {
			scriptStr = getTextResource(req.getServletContext(), basePathStr, scriptElement.attr("src"));
			if (StringUtils.isNotBlank(scriptStr)) {
				return scriptStr;
			}

			List<TextNode> textNodes = scriptElement.textNodes();
			if (CollectionUtils.isNotEmpty(textNodes)) {
				scriptStr = textNodes.get(0).getWholeText();
			} else {
				scriptStr = scriptElement.data();
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
		super(null, req, resp, basePathStr, sc, scriptElement.attr("type"));
		this.document = document;
		this.scriptElement = scriptElement;
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
		
		bindings.put("doc", document);
		bindings.put("req", req);
		bindings.put("resp", resp);
		bindings.put("me", scriptElement);

		return sc;
	}
	
}