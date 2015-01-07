package com.thenetcircle.services.commons.web.joint.script;

import javax.script.ScriptContext;

//TODO
//add code to initiate script engine for each type
//add the engine into each threadlocal instance
public enum ScriptEngineProxy {
	
	beanshell {
		public void executeScript(final String scriptStr, final ScriptContext ctx) {
			
		}
	}, 
	
	javascript {
		public void executeScript(final String scriptStr, final ScriptContext ctx) {
			
		}
	};
	
	
	public void executeScript(final String scriptStr, final ScriptContext ctx) {
		
	}
	
	static class Interpreter<T> extends ThreadLocal<T> {
		
	}
}
