package com.thenetcircle.services.commons.web.joint.script;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.thenetcircle.services.commons.MiscUtils;

public class ScriptExecutor {

	private static Log log = LogFactory.getLog(ScriptExecutor.class);
	
	protected ScriptExecutionContext scriptCtx;

	public ScriptExecutor(ScriptExecutionContext scriptCtx) {
		super();
		this.scriptCtx = scriptCtx;
	}

	public ScriptContext execute() {
		String mimeType = scriptCtx.mimeType;
		ScriptEngine se = ScriptUtils.getScriptEngineByMimeType(mimeType);
		
		String scriptStr = scriptCtx.getScriptStr();
		try {
			se.eval(scriptStr, scriptCtx.inflateScriptContext(se));
			scriptCtx.sc.getBindings(ScriptContext.ENGINE_SCOPE).putAll(se.getBindings(ScriptContext.ENGINE_SCOPE));
		} catch (ScriptException e) {
			log.info(MiscUtils.lineNumber(scriptStr));
			log.error(String.format("failed to execute script: \n\t %s \n\t", MiscUtils.lineNumber(scriptStr)), e);
		} finally {
			if (scriptCtx instanceof PageScriptExecutionContext) {
				PageScriptExecutionContext psc = (PageScriptExecutionContext) scriptCtx;
				psc.scriptElement.remove();
			}
		}
		return scriptCtx.sc;
	}
}
