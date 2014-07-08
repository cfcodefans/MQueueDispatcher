package com.thenetcircle.services.rest.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.ApplicationEvent.Type;
import org.glassfish.jersey.server.monitoring.ApplicationEventListener;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;

import com.thenetcircle.services.common.MiscUtils;
import com.thenetcircle.services.common.ProcTrace;
import com.thenetcircle.services.rest.AjaxRes;
import com.thenetcircle.services.rest.WeldBinder;

public class ResLoader extends ResourceConfig {
	private static Log log = LogFactory.getLog(ResLoader.class);

	public ResLoader() {
		ProcTrace.start(MiscUtils.invocationInfo());
		ProcTrace.ongoing("set packages scann");
		
		register(JacksonFeature.class);
		
		this.packages("com.thenetcircle.services.rest");
		register(new ResLoaderListener());
		
		register(new WeldBinder());
		
		ProcTrace.end();
		log.info(ProcTrace.flush());
	}

	public static class ResLoaderListener implements ContainerLifecycleListener, ApplicationEventListener {
		@Override
		public void onStartup(Container c) {
			log.info(MiscUtils.invocationInfo() + "\n\n\t");
		}

		@Override
		public void onReload(Container c) {
			log.info(MiscUtils.invocationInfo());
		}

		@Override
		public void onShutdown(Container c) {
			log.info(MiscUtils.invocationInfo());
		}

		@Override
		public void onEvent(ApplicationEvent ev) {
			log.info(MiscUtils.invocationInfo() + "\n\n\t");
			if (Type.INITIALIZATION_APP_FINISHED == ev.getType()) {
				AjaxRes.build(ev.getResourceModel(), ev.getResourceConfig());
			}
			
			log.info(MiscUtils.invocationInfo() + " finished loading restful apis.....");
		}

		@Override
		public RequestEventListener onRequest(RequestEvent reqEv) {
			return null;
		}
	}
}
