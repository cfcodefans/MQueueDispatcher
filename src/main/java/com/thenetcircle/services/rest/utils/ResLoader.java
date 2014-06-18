package com.thenetcircle.services.rest.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.ResourceModel;
import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.ApplicationEvent.Type;
import org.glassfish.jersey.server.monitoring.ApplicationEventListener;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;

import com.thenetcircle.services.common.MiscUtils;
import com.thenetcircle.services.common.ProcTrace;
import com.thenetcircle.services.rest.JavaScriptProxyRes;

public class ResLoader extends ResourceConfig {
	private static Log log = LogFactory.getLog(ResLoader.class);

	public ResLoader() {
		ProcTrace.start(MiscUtils.invocationInfo());
		ProcTrace.ongoing("set packages scann");
		
		packages("com.thenetcircle.services.rest",
				"com.thenetcircle.services.rest.javascript.bridge");
		
//		this.registerClasses(ServerCfgRes.class);
		
		register(new ResLoaderListener());
		
		ProcTrace.end();
		log.info(ProcTrace.flush());
	}
	
	private static ResourceModel resModel = null;
	public static ResourceModel getResModel() {
		return resModel;
	}

	public static class ResLoaderListener implements ContainerLifecycleListener, ApplicationEventListener {
		@Override
		public void onStartup(Container c) {
			log.info(MiscUtils.invocationInfo() + "\n\n\t");
//			log.info(StringUtils.join(c.getConfiguration().getResources().iterator(), "\n"));
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
				resModel = ev.getResourceModel();
				
				JavaScriptProxyRes.build(resModel);
			}
		}

		@Override
		public RequestEventListener onRequest(RequestEvent reqEv) {
			return null;
		}
	}
}
