package com.thenetcircle.services.commons.web.mvc;

import com.thenetcircle.services.commons.MiscUtils;
import com.thenetcircle.services.commons.ProcTrace;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.message.DeflateEncoder;
import org.glassfish.jersey.message.GZipEncoder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.EncodingFilter;
import org.glassfish.jersey.server.mvc.internal.MvcBinder;

public class MvcResLoader extends ResourceConfig {
	private static final Logger log = LogManager.getLogger(MvcResLoader.class);

	public MvcResLoader() {
		ProcTrace.start(MiscUtils.invocationInfo());
		ProcTrace.ongoing("set packages scan");

		register(JacksonFeature.class);
		register(EncodingFilter.class);
		register(GZipEncoder.class);
		register(DeflateEncoder.class);
		register(new MvcBinder());

		ProcTrace.end();
		log.info(ProcTrace.flush());
	}
}
