package com.thenetcircle.services.commons.web.mvc;

import java.io.InputStream;
import java.util.Map;

import javax.script.ScriptContext;
import javax.script.SimpleScriptContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.thenetcircle.services.commons.web.joint.joint.script.PageScriptExecutionContext;
import com.thenetcircle.services.commons.web.joint.joint.script.ScriptExecutionContext;
import com.thenetcircle.services.commons.web.joint.joint.script.ScriptExecutor;
import com.thenetcircle.services.commons.web.joint.joint.script.javascript.JSExecutor;

public class HtmlViewProcessor extends ResViewProcessor {

	private static Log log = LogFactory.getLog(HtmlViewProcessor.class);
	
	public HtmlViewProcessor(final HttpServletRequest _req, 
							 final HttpServletResponse _resp, 
							 final String _basePathStr) {//, final BeanManager _beanMgr) {
		super(_req, _resp, StringUtils.defaultIfBlank(_basePathStr, "/"));
	}
	
	public HtmlViewProcessor(final HttpServletRequest _req, 
							 final HttpServletResponse _resp) {//, final BeanManager _beanMgr) {
		super(_req, _resp);
	}
	
	public byte[] process(final String currentPathStr, final String baseUriStr) throws Exception {
		if (StringUtils.isEmpty(currentPathStr)) {
			log.error("illegalArguement: currentPathStr is empty: " + currentPathStr);
			return new byte[0];
		}
		
		super.resp.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML);
		
		final InputStream resIS = getResAsInputStream(currentPathStr);
		
		final Document doc = Jsoup.parse(resIS, "UTF-8", baseUriStr);
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

	public void bindValuesToFormFields(final Element form) {
		final Map<String, String[]> params = req.getParameterMap();
		for (final String fieldName : params.keySet()) {
			final Elements fields = form.select(String.format("input[name=%s]", fieldName));
			if (CollectionUtils.isEmpty(fields)) {
				continue;
			}
			
			final String[] values = params.get(fieldName);
			if (ArrayUtils.isEmpty(values)) {
				continue;
			}
			
			for (int i = 0, j = fields.size(), k = values.length; i < j && i < k; i++) {
				fields.get(i).val(values[i]);
			}
		}
	}
}
