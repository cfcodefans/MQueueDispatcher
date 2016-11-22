package com.thenetcircle.services.commons.web.mvc;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;

import javax.script.ScriptContext;
import javax.script.SimpleScriptContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.thenetcircle.services.commons.web.joint.script.ScriptExecutionContext;
import com.thenetcircle.services.commons.web.joint.script.ScriptExecutor;

public class JSProcessor extends ResViewProcessor {

	public static final String MIME_APPLICATION_JAVASCRIPT = "application/javascript";
	private static Log log = LogFactory.getLog(JSProcessor.class);
	
	public JSProcessor(final HttpServletRequest _req, 
					   final HttpServletResponse _resp, 
					   final String _basePathStr) {//, final BeanManager _beanMgr) {
		super(_req, _resp, StringUtils.defaultIfBlank(_basePathStr, "/"), MediaType.TEXT_PLAIN_TYPE);
	}
	
	public JSProcessor(final HttpServletRequest _req, 
					   final HttpServletResponse _resp) {//, final BeanManager _beanMgr) {
		super(_req, _resp);
	}
	
	public byte[] process(final String currentPathStr, final String baseUriStr) throws Exception {
		if (StringUtils.isEmpty(currentPathStr)) {
			log.error("illegalArguement: currentPathStr is empty: " + currentPathStr);
			return new byte[0];
		}
		
		ScriptContext sc = new SimpleScriptContext();
		final ByteArrayOutputStream outputBuf = new ByteArrayOutputStream();
		sc.setWriter(new OutputStreamWriter(outputBuf));
		final ScriptExecutionContext scriptCtx = new ScriptExecutionContext(req, resp, baseUriStr, currentPathStr, sc, MIME_APPLICATION_JAVASCRIPT);
		final ScriptExecutor se = new ScriptExecutor(scriptCtx);
		sc = se.execute();
		
		return outputBuf.toByteArray();
	}
}
