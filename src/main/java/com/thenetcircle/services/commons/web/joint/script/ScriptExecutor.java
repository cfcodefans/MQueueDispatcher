package com.thenetcircle.services.commons.web.joint.script;

import java.util.HashMap;
import java.util.Map;

import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.thenetcircle.services.commons.MiscUtils;

public class ScriptExecutor {

	private static Log log = LogFactory.getLog(ScriptExecutor.class);
	
	protected ScriptExecutionContext scriptCtx;
	
	protected static ScriptEngineManager sem = new ScriptEngineManager();
	
	static {
		log.info("initialize ScriptEngineManager ");
		
		for (ScriptEngineFactory sef : sem.getEngineFactories()) {
			
			final String _str = "engine: { \n\tname: " + sef.getEngineName()
					+ ", \n\tversion: " + sef.getEngineVersion()
					+ ", \n\textensions: " + sef.getExtensions()
					+ ", \n\tmime_types: " + sef.getMimeTypes()
					+ ", \n\tnames: " + sef.getNames()
					+ ", \n\tlanguage_name: " + sef.getLanguageName()
					+ ", \n\tlanguage_version: " + sef.getLanguageVersion() + "\n}\n";
			
			log.info(_str);

			sef.getMimeTypes().forEach(mimeType -> sem.registerEngineMimeType(mimeType, sef));
			sef.getExtensions().forEach(extension -> sem.registerEngineMimeType(extension, sef));
		}
	}
	
	protected static ThreadLocal<Map<String, ScriptEngine>> scriptEnginePool = new ThreadLocal<Map<String, ScriptEngine>>() {
		protected Map<String, ScriptEngine> initialValue() {
			return new HashMap<String, ScriptEngine>();
		}
	};

	public ScriptExecutor(ScriptExecutionContext scriptCtx) {
		super();
		this.scriptCtx = scriptCtx;
	}

	public ScriptContext execute() {
		Map<String, ScriptEngine> mineTypeAndScriptEngines = scriptEnginePool.get();
		
		String mimeType = scriptCtx.mimeType;
		ScriptEngine se = mineTypeAndScriptEngines.get(mimeType);
		
		if (se == null) {
			se = sem.getEngineByMimeType(mimeType);
			if (se == null) {
				se = sem.getEngineByExtension(mimeType);
			}
			if (se == null) {
				throw new IllegalArgumentException("not script engine found for mime type: " + mimeType);
			}
			
			mineTypeAndScriptEngines.put(mimeType, se);
		}
		
		String scriptStr = scriptCtx.getScriptStr();
		try {
			CompiledScript compiledScript = scriptCtx.getCompiledScript();
			if (compiledScript != null) {
				compiledScript.eval(scriptCtx.inflateScriptContext(compiledScript.getEngine()));
				return scriptCtx.sc;
			}

			log.warn(String.format("the script isn't compiled \n\n%s", scriptStr));

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
