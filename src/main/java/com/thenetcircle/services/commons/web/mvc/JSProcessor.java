package com.thenetcircle.services.commons.web.mvc;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;

import javax.script.ScriptContext;
import javax.script.SimpleScriptContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.thenetcircle.services.commons.web.joint.script.ScriptExecutionContext;
import com.thenetcircle.services.commons.web.joint.script.ScriptExecutor;
import com.thenetcircle.services.commons.web.joint.script.javascript.JSExecutor;

public class JSProcessor extends ResViewProcessor {

	private static Log log = LogFactory.getLog(JSProcessor.class);
	
	public JSProcessor(final HttpServletRequest _req, 
					   final HttpServletResponse _resp, 
					   final String _basePathStr) {//, final BeanManager _beanMgr) {
		super(_req, _resp, StringUtils.defaultIfBlank(_basePathStr, "/"));
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
		final ScriptExecutionContext scriptCtx = new ScriptExecutionContext(req, resp, baseUriStr, currentPathStr, sc);
		final ScriptExecutor se = new JSExecutor(scriptCtx);
		sc = se.execute();
		
		return outputBuf.toByteArray();
	}
}
