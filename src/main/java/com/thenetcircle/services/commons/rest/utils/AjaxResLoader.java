package com.thenetcircle.services.commons.rest.utils;

import java.lang.reflect.Field;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.inject.Singleton;
import javax.ws.rs.CookieParam;
import javax.ws.rs.Encoded;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.jersey.internal.inject.Providers;
import org.glassfish.jersey.internal.util.ReflectionHelper;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.message.DeflateEncoder;
import org.glassfish.jersey.message.GZipEncoder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.EncodingFilter;
import org.glassfish.jersey.server.model.AnnotatedMethod;
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

import com.thenetcircle.services.commons.MiscUtils;
import com.thenetcircle.services.commons.ProcTrace;
import com.thenetcircle.services.commons.cdi.rest.WeldBinder;
import com.thenetcircle.services.commons.rest.ajax.AjaxResMetaData;
import com.thenetcircle.services.commons.rest.ajax.ParamMetaData;

public class AjaxResLoader extends ResourceConfig {
	private static Log log = LogFactory.getLog(AjaxResLoader.class);

	public AjaxResLoader() {
		ProcTrace.start(MiscUtils.invocationInfo());
		ProcTrace.ongoing("set packages scann");

		register(JacksonFeature.class);
		register(EncodingFilter.class);
		register(GZipEncoder.class);
		register(DeflateEncoder.class);
		register(new ResLoaderListener());
		register(new WeldBinder());

		ProcTrace.end();
		log.info(ProcTrace.flush());
	}
	
	public static class AjaxResContext {
		
		private static final ConcurrentMap<String, AjaxResContext> cache = new ConcurrentHashMap<String, AjaxResContext>();
		
		private AjaxResContext() {}
		
		public static AjaxResContext getInstance(String name) {
			cache.putIfAbsent(name, new AjaxResContext());
			return cache.get(name);
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
		    	final MethodList methodList = new MethodList(handlerClass, true);
		    	List<Parameter> params = new ArrayList<Parameter>();
		        for (AnnotatedMethod method : methodList.withoutMetaAnnotation(HttpMethod.class).
		                withoutAnnotation(Path.class).
		                withoutAnnotation(Context.class).
		                hasNumParams(1).
		                hasReturnType(void.class).
		                nameStartsWith("set")) {
		            Parameter p = Parameter.create(
		                    handlerClass,
		                    method.getMethod().getDeclaringClass(),
		                    encodedFlag || method.isAnnotationPresent(Encoded.class),
		                    method.getParameterTypes()[0],
		                    method.getGenericParameterTypes()[0],
		                    method.getAnnotations());
		            if (null != p) {
		//                ResourceMethodValidator.validateParameter(p, method.getMethod(), method.getMethod().toGenericString(), "1",
		//                        isSingleton(handlerClass));
		            	params.add(p);
		            }
		        }
		        return params;
		    }

		@SuppressWarnings("rawtypes")
		    private static List<Parameter> getClassFields(final boolean encodedFlag, Class handlerClass) {
		    	List<Parameter> params = new ArrayList<Parameter>();
		        for (Field field : AccessController.doPrivileged(ReflectionHelper.getDeclaredFieldsPA(handlerClass))) {
		            if (field.getDeclaredAnnotations().length > 0 
		            		&& (field.isAnnotationPresent(QueryParam.class)
		            		|| field.isAnnotationPresent(PathParam.class)
		            		|| field.isAnnotationPresent(CookieParam.class)
		            		|| field.isAnnotationPresent(MatrixParam.class)
		            		|| field.isAnnotationPresent(FormParam.class)
		            		|| field.isAnnotationPresent(HeaderParam.class))) {
		                Parameter p = Parameter.create(
		                        handlerClass,
		                        field.getDeclaringClass(),
		                        encodedFlag || field.isAnnotationPresent(Encoded.class),
		                        field.getType(),
		                        field.getGenericType(),
		                        field.getAnnotations());
		                if (null != p) {
		//                    ResourceMethodValidator.validateParameter(p, field, field.toGenericString(), field.getName(),
		//                            isInSingleton);
		                	params.add(p);
		                }
		            }
		        }
		        return params;
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
