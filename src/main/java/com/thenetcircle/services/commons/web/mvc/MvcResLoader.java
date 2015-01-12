package com.thenetcircle.services.commons.web.mvc;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.message.DeflateEncoder;
import org.glassfish.jersey.message.GZipEncoder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.EncodingFilter;
import org.glassfish.jersey.server.mvc.internal.MvcBinder;

import com.thenetcircle.services.commons.MiscUtils;
import com.thenetcircle.services.commons.ProcTrace;

public class MvcResLoader extends ResourceConfig {
	private static Log log = LogFactory.getLog(MvcResLoader.class);

	public MvcResLoader() {
		ProcTrace.start(MiscUtils.invocationInfo());
		ProcTrace.ongoing("set packages scann");

		register(JacksonFeature.class);
		register(EncodingFilter.class);
		register(GZipEncoder.class);
		register(DeflateEncoder.class);
		

		// this.packages("com.thenetcircle.services.rest");

		register(new MvcBinder());

		ProcTrace.end();
		log.info(ProcTrace.flush());
	}
}
