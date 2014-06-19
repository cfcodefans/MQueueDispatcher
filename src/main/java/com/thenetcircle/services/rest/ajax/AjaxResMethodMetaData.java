package com.thenetcircle.services.rest.ajax;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang3.EnumUtils;
import org.glassfish.jersey.server.model.Parameter;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.glassfish.jersey.server.model.ResourceMethod.JaxrsType;

import com.sun.research.ws.wadl.HTTPMethods;

@XmlRootElement
public class AjaxResMethodMetaData implements Serializable {

	private static final long serialVersionUID = 1L;

	public JaxrsType jaxrsType;
	public String name;
	@SuppressWarnings("rawtypes")
	public Class returnType;
	public HTTPMethods httpMethod;
	public List<ParamMetaData> params = new ArrayList<ParamMetaData>();
	public String baseUrl;
	
	public List<MediaType> produceMediaTypes = new ArrayList<MediaType>();
	public List<MediaType> consumedMediaTypes = new ArrayList<MediaType>();

	@SuppressWarnings("rawtypes")
	public static AjaxResMethodMetaData build(ResourceMethod resMd) {
		if (resMd == null) {
			return null;
		}
		
		AjaxResMethodMetaData aResMd = new AjaxResMethodMetaData();
		
		String httpMethodStr = resMd.getHttpMethod().toUpperCase();
		if (!EnumUtils.isValidEnum(HTTPMethods.class, httpMethodStr)) {
			return null;
		}
		
		aResMd.httpMethod = HTTPMethods.valueOf(httpMethodStr);
		aResMd.name = resMd.getInvocable().getHandlingMethod().getName();
		aResMd.returnType = resMd.getInvocable().getRawResponseType();
		aResMd.jaxrsType = resMd.getType();
		
		aResMd.produceMediaTypes.addAll(resMd.getProducedTypes());
		aResMd.consumedMediaTypes.addAll(resMd.getConsumedTypes());
		
		for (Parameter param : resMd.getInvocable().getParameters()) {
			ParamMetaData pMD = ParamMetaData.build(param);
			aResMd.params.add(pMD);
		}
		
		return aResMd;
	}
}
