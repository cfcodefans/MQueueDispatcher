package com.thenetcircle.services.commons.web.joint.script.javascript;


import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.lang3.JavaVersion;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import sun.org.mozilla.javascript.internal.Context;
import sun.org.mozilla.javascript.internal.WrapFactory;

import com.sun.script.javascript.RhinoScriptEngine;
import com.thenetcircle.services.commons.MiscUtils;
import com.thenetcircle.services.commons.web.joint.script.PageScriptExecutionContext;
import com.thenetcircle.services.commons.web.joint.script.ScriptExecutionContext;
import com.thenetcircle.services.commons.web.joint.script.ScriptExecutor;

@SuppressWarnings("restriction")
public class JSExecutor extends ScriptExecutor {
	private static Log log = LogFactory.getLog(JSExecutor.class);

	public JSExecutor(final ScriptExecutionContext scriptCtx) {
		super(scriptCtx);
	}
	
	private static final ScriptEngineManager sem = new ScriptEngineManager();
	
//	public static WrapFactory wf = new WrapFactory() {
//		{this.setJavaPrimitiveWrap(false);}
//	};
	
	private static ThreadLocal<Pair<ScriptEngineManager, ScriptEngine>> pool = new ThreadLocal<Pair<ScriptEngineManager,ScriptEngine>>() {
		protected Pair<ScriptEngineManager,ScriptEngine> initialValue() {
			final boolean isRhino = SystemUtils.IS_JAVA_1_6 || SystemUtils.IS_JAVA_1_7;
			final boolean isNashorn = SystemUtils.isJavaVersionAtLeast(JavaVersion.JAVA_1_8);
			
			ScriptEngine se = null;
			if (isRhino) {
				se = sem.getEngineByName("rhino");
				if (se instanceof RhinoScriptEngine) {
//					final RhinoScriptEngine rse = (RhinoScriptEngine)se;
					Context.enter();
					WrapFactory wf = new WrapFactory();
					wf.setJavaPrimitiveWrap(false);
					Context.getCurrentContext().setWrapFactory(wf);
				}
			} else if (isNashorn) {
				se = sem.getEngineByName("Nashorn");
			}
			
			return new MutablePair<ScriptEngineManager, ScriptEngine>(sem, se);
		};
	};

	public ScriptContext execute() {
		String scriptStr = scriptCtx.getScriptStr();
		if (StringUtils.isEmpty(scriptStr)) {
			log.error("script is blank string");
			return scriptCtx.sc;
		}

		try {
			CompiledScript compiledScript = scriptCtx.getCompiledScript();
			if (compiledScript != null) {
				compiledScript.eval(scriptCtx.inflateScriptContext(compiledScript.getEngine()));
				return scriptCtx.sc;
			}

			log.warn(String.format("the script isn't compiled \n\n%s", scriptStr));
			
			final ScriptEngine se = pool.get().getValue();
			se.eval(scriptStr, scriptCtx.inflateScriptContext(se));
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
