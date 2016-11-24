package com.thenetcircle.services.commons.rest.utils;

import com.thenetcircle.services.commons.MiscUtils;
import com.thenetcircle.services.commons.ProcTrace;
import com.thenetcircle.services.commons.rest.ajax.AjaxResMetaData;
import com.thenetcircle.services.commons.rest.ajax.ParamMetaData;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.jersey.internal.inject.Providers;
import org.glassfish.jersey.internal.util.ReflectionHelper;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.message.DeflateEncoder;
import org.glassfish.jersey.message.GZipEncoder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.EncodingFilter;
import org.glassfish.jersey.server.model.MethodList;
import org.glassfish.jersey.server.model.Parameter;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceModel;
import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.ApplicationEvent.Type;
import org.glassfish.jersey.server.monitoring.ApplicationEventListener;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;

import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import java.security.AccessController;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AjaxResLoader extends ResourceConfig {
	private static final Logger log = LogManager.getLogger(AjaxResLoader.class);

	public AjaxResLoader() {
		ProcTrace.start(MiscUtils.invocationInfo());
		ProcTrace.ongoing("set packages scann");

		register(JacksonFeature.class);
		register(EncodingFilter.class);
		register(GZipEncoder.class);
		register(DeflateEncoder.class);
		register(new ResLoaderListener());

		ProcTrace.end();
		log.info(ProcTrace.flush());
		
		instance = this;
	}
	
	private static AjaxResLoader instance = null;
	public static AjaxResLoader instance() {
		return instance;
	}
	
	public static class AjaxResContext {
		
		private static final ConcurrentMap<String, AjaxResContext> cache = new ConcurrentHashMap<String, AjaxResContext>();
		
		private AjaxResContext() {}
		
		public static AjaxResContext getInstance(String name) {
			return cache.computeIfAbsent(StringUtils.defaultIfBlank(name, ""), (n)->new AjaxResContext());
		}

		private ResourceModel resModel = null;
		private ResourceConfig appCfg = null;
		private List<AjaxResMetaData> proxyList = new ArrayList<AjaxResMetaData>();

		public List<AjaxResMetaData> getProxyList() {
			return proxyList;
		}

		public void build(final ResourceModel _resModel, final ResourceConfig _app) {
			resModel = _resModel;
			appCfg = _app;
			
			Collection<Resource> resources = resModel.getResources();
			if (CollectionUtils.isEmpty(resources)) {
				return;
			}
		
			Set<Class<?>> resClss = appCfg.getClasses();
			log.info("\nfound resources classes: \n" + StringUtils.join(resClss, '\n'));
			log.info("\nfound singleton instances: \n" + StringUtils.join(appCfg.getSingletons(), '\n'));
			
			resources.forEach(res -> {
				AjaxResMetaData resMd = AjaxResMetaData.build(res);
				Collection<Class<?>> _clss = CollectionUtils.intersection(resClss, res.getHandlerClasses());
				
				_clss.stream().filter(_cls -> !isSingleton(_cls)).forEach(_cls->{
					getClassSetters(true, _cls).stream().map(ParamMetaData::build).forEach(resMd.injectedParams::add);
					getClassFields(true, _cls).stream().map(ParamMetaData::build).forEach(resMd.injectedParams::add);
				});
				
				log.info("\n injected Params: \n\t" + StringUtils.join(resMd.injectedParams, "\n\t"));
				
				resMd.appendInjectedParams(resMd.injectedParams);
				proxyList.add(resMd);
			});
		}

		@SuppressWarnings("rawtypes")
			private static List<Parameter> getClassSetters(final boolean encodedFlag, final Class handlerClass) {
		    	MethodList methodList = new MethodList(handlerClass, true);
		    	
		    	methodList = methodList.withoutMetaAnnotation(HttpMethod.class).
                withoutAnnotation(Path.class).
                withoutAnnotation(Context.class).
                hasNumParams(1).
                hasReturnType(void.class).
                nameStartsWith("set");
		    	
		    	if (!methodList.iterator().hasNext())
		    		return Collections.emptyList();
		    	
		    	//AnnotatedMethod
		    	return Stream.generate(methodList.iterator()::next).map(method->Parameter.create(
	                    handlerClass,
	                    method.getMethod().getDeclaringClass(),
	                    encodedFlag || method.isAnnotationPresent(Encoded.class),
	                    method.getParameterTypes()[0],
	                    method.getGenericParameterTypes()[0],
	                    method.getAnnotations())).filter(param->param != null).collect(Collectors.toList());
		    	
		    }

		@SuppressWarnings("rawtypes")
		    private static List<Parameter> getClassFields(final boolean encodedFlag, Class handlerClass) {
				return Stream.of(AccessController.doPrivileged(ReflectionHelper.getDeclaredFieldsPA(handlerClass))).filter(field->(field.getDeclaredAnnotations().length > 0 
		            		&& (field.isAnnotationPresent(QueryParam.class)
		            		|| field.isAnnotationPresent(PathParam.class)
		            		|| field.isAnnotationPresent(CookieParam.class)
		            		|| field.isAnnotationPresent(MatrixParam.class)
		            		|| field.isAnnotationPresent(FormParam.class)
		            		|| field.isAnnotationPresent(HeaderParam.class)))).map(field->Parameter.create(
		                        handlerClass,
		                        field.getDeclaringClass(),
		                        encodedFlag || field.isAnnotationPresent(Encoded.class),
		                        field.getType(),
		                        field.getGenericType(),
		                        field.getAnnotations())).collect(Collectors.toList());
			
		    }

		/**
		 * Check if the resource class is declared to be a singleton.
		 *
		 * @param resourceClass resource class.
		 * @return {@code true} if the resource class is a singleton, {@code false} otherwise.
		 */
		private static boolean isSingleton(Class<?> resourceClass) {
		    return resourceClass.isAnnotationPresent(Singleton.class)
		            || (Providers.isProvider(resourceClass) && !resourceClass.isAnnotationPresent(PerLookup.class));
		}
		
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
				AjaxResContext.getInstance(ev.getResourceConfig().getApplicationName()).build(ev.getResourceModel(), ev.getResourceConfig());
				//AjaxRes.build(ev.getResourceModel(), ev.getResourceConfig());
			}

			log.info(MiscUtils.invocationInfo() + " finished loading restful apis.....");
		}

		@Override
		public RequestEventListener onRequest(RequestEvent reqEv) {
			return null;
		}
	}
}
