package com.thenetcircle.services.rest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.glassfish.jersey.server.ResourceConfig;

import com.thenetcircle.services.common.MiscUtils;
import com.thenetcircle.services.common.ProcTrace;

public class ResLoader extends ResourceConfig {
	private static Log log = LogFactory.getLog(ResLoader.class);

	public ResLoader() {
		ProcTrace.start(MiscUtils.invocationInfo());
		
		ProcTrace.ongoing("set packages scann");
		packages("com.thenetcircle.services.rest");
		
//		ProcTrace.ongoing("set CDI Binder");
//		register(new WeldBinder());
		
		ProcTrace.end();
		log.info(ProcTrace.flush());
	}
}
