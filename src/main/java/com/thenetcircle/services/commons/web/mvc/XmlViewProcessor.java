package com.thenetcircle.services.commons.web.mvc;

import java.io.InputStream;

import javax.script.ScriptContext;
import javax.script.SimpleScriptContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Document.OutputSettings;
import org.jsoup.nodes.Document.OutputSettings.Syntax;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import com.thenetcircle.services.commons.web.joint.joint.script.PageScriptExecutionContext;
import com.thenetcircle.services.commons.web.joint.joint.script.ScriptExecutionContext;
import com.thenetcircle.services.commons.web.joint.joint.script.ScriptExecutor;
import com.thenetcircle.services.commons.web.joint.joint.script.javascript.JSExecutor;

public class XmlViewProcessor extends ResViewProcessor {

	private static Log log = LogFactory.getLog(XmlViewProcessor.class);
	
	public XmlViewProcessor(final HttpServletRequest _req, 
							final HttpServletResponse _resp, 
							final String _basePathStr) {//, final BeanManager _beanMgr) {
		super(_req, _resp, StringUtils.defaultIfBlank(_basePathStr, "/"));
	}
	
	public XmlViewProcessor(final HttpServletRequest _req, 
							final HttpServletResponse _resp) {//, final BeanManager _beanMgr) {
		super(_req, _resp);
	}
	
	public byte[] process(final String currentPathStr, final String baseUriStr) throws Exception {
		if (StringUtils.isEmpty(currentPathStr)) {
			log.error("illegalArguement: currentPathStr is empty: " + currentPathStr);
			return new byte[0];
		}
		
		super.resp.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML);
		
		final InputStream resIS = getResAsInputStream(currentPathStr);
		
		final Document doc = Jsoup.parse(resIS, "UTF-8", baseUriStr, Parser.xmlParser());
		final OutputSettings outputSettings = new OutputSettings();
		outputSettings.syntax(Syntax.xml);
		outputSettings.prettyPrint(false);
		doc.outputSettings(outputSettings);
		final Elements els =  doc.select("script[data-runat=server]");
		
		ScriptContext sc = new SimpleScriptContext();
		if (CollectionUtils.isNotEmpty(els)) {
			for (final Element el : els) {
				final ScriptExecutionContext scriptCtx = new PageScriptExecutionContext(doc,
																					el, 
																					req, 
																					resp, 
																					currentPathStr, 
																					sc);
				final ScriptExecutor se = new JSExecutor(scriptCtx);
				sc = se.execute();
			}
		}
		
		return doc.html().getBytes();
	}
}
