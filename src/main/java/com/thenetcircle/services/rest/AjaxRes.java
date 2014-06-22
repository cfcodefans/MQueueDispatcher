package com.thenetcircle.services.rest;

import java.lang.reflect.Field;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.inject.Singleton;
import javax.ws.rs.CookieParam;
import javax.ws.rs.Encoded;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.jersey.internal.inject.Providers;
import org.glassfish.jersey.internal.util.ReflectionHelper;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.AnnotatedMethod;
import org.glassfish.jersey.server.model.MethodList;
import org.glassfish.jersey.server.model.Parameter;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceModel;

import com.thenetcircle.services.common.Jsons;
import com.thenetcircle.services.rest.ajax.AjaxResMetaData;
import com.thenetcircle.services.rest.ajax.ParamMetaData;

@Path("ajax")
@Singleton
public class AjaxRes {

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getJSProxy(@Context UriInfo uriInfo) {
		for (AjaxResMetaData armd : proxyList) {
			armd.setBaseUrl(StringUtils.removeEnd(uriInfo.getAbsolutePath().getPath(), "ajax"));
		}
		return Response.ok(Jsons.toString(proxyList.toArray(new AjaxResMetaData[0]))).build();
	}

	@SuppressWarnings("rawtypes")
	public static void build(final ResourceModel _resModel, final ResourceConfig _app) {
		resModel = _resModel;
		appCfg = _app;
		
		Collection<Resource> resources = resModel.getResources();
		if (CollectionUtils.isEmpty(resources)) {
			return;
		}

		Set<Class<?>> resClss = appCfg.getClasses();
		log.info("\nfound resources classes: \n" + StringUtils.join(resClss, '\n'));
		log.info("\nfound singleton instances: \n" + StringUtils.join(appCfg.getSingletons(), '\n'));
		
		for (final Resource res : resources) {
			AjaxResMetaData resMd = AjaxResMetaData.build(res);
			Collection<Class<?>> _clss = CollectionUtils.intersection(resClss, res.getHandlerClasses());
			for (Class _cls : _clss) {
				if (isSingleton(_cls)) {
					continue;
				}
				
				for (Parameter p : getClassSetters(true, _cls)) {
					resMd.injectedParams.add(ParamMetaData.build(p));
				}
				
				for (Parameter p : getClassFields(true, _cls)) {
					resMd.injectedParams.add(ParamMetaData.build(p));
				}
			}
			
			log.info("\n injected Params: \n\t" + StringUtils.join(resMd.injectedParams, "\n\t"));
			
			resMd.appendInjectedParams(resMd.injectedParams);
			proxyList.add(resMd);
		}
	}

	private static ResourceModel resModel = null;
	private static ResourceConfig appCfg = null;
	
	private static Log log = LogFactory.getLog(AjaxRes.class);

	private static List<AjaxResMetaData> proxyList = new ArrayList<AjaxResMetaData>();
	
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
