package com.thenetcircle.services.commons.rest.ajax;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.commons.lang3.EnumUtils;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.glassfish.jersey.server.model.ResourceMethod.JaxrsType;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

	public List<String> produceMediaTypes = new ArrayList<String>();
	public List<String> consumedMediaTypes = new ArrayList<String>();

	@JsonIgnore
	@XmlTransient
	public AjaxResMetaData parent;

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

		resMd.getProducedTypes().stream().map(MediaType::toString).forEach(aResMd.produceMediaTypes::add);
		resMd.getConsumedTypes().stream().map(MediaType::toString).forEach(aResMd.consumedMediaTypes::add);
		resMd.getInvocable().getParameters().stream().map(ParamMetaData::build).forEach(aResMd.params::add);

		return aResMd;
	}
}
