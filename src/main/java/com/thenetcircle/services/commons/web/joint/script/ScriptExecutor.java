package com.thenetcircle.services.commons.web.joint.script;

import javax.script.ScriptContext;

public class ScriptExecutor {

//	private static Log log = LogFactory.getLog(ScriptExecutor.class);
	
	protected ScriptExecutionContext scriptCtx;

	public ScriptExecutor(ScriptExecutionContext scriptCtx) {
		super();
		this.scriptCtx = scriptCtx;
	}

	public ScriptContext execute() {
		throw new UnsupportedOperationException();
	}
}
