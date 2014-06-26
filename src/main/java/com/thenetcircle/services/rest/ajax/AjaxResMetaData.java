package com.thenetcircle.services.rest.ajax;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.functors.EqualPredicate;
import org.apache.commons.collections4.functors.NotPredicate;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.thenetcircle.services.common.MiscUtils;

@XmlRootElement
public class AjaxResMetaData implements Serializable {

	public static enum DataType {
		text, html, script, json, jsonp, xml;

		@SuppressWarnings("unchecked")
		public static final Map<MediaType, DataType> internalMap = MiscUtils.map(MediaType.APPLICATION_JSON_TYPE, json,

		MediaType.TEXT_PLAIN_TYPE, text,

		MediaType.APPLICATION_XML_TYPE, xml, MediaType.TEXT_XML_TYPE, xml,

		MediaType.TEXT_HTML, html, MediaType.APPLICATION_XHTML_XML_TYPE, html);

		public static List<DataType> convert(List<MediaType> mediaTypes) {
			final List<DataType> dataTypeList = new ArrayList<DataType>();

			for (MediaType mt : mediaTypes) {
				final DataType dt = internalMap.get(mt);
				if (dt != null) {
					dataTypeList.add(dt);
				}
			}

			if (CollectionUtils.isEmpty(dataTypeList)) {
				dataTypeList.add(text);
			}

			return dataTypeList;
		}
	}

	private static final long serialVersionUID = 1L;

	@XmlTransient
	public String path;
	public String name;
	
	@XmlTransient
	public String baseUrl;
	
	public String getUrl() {
		return baseUrl + (StringUtils.endsWith(baseUrl, "/") || StringUtils.startsWith(path, "/") ? "" : "/") + path;
	}

	public List<AjaxResMetaData> children = new ArrayList<AjaxResMetaData>();
	
	@XmlTransient
	@JsonIgnore
	public List<ParamMetaData> injectedParams = new ArrayList<ParamMetaData>();

	@XmlTransient
	@JsonIgnore
	public AjaxResMetaData parent;

	public List<AjaxResMethodMetaData> methods = new ArrayList<AjaxResMethodMetaData>();
	
	public static AjaxResMetaData build(Resource res) {
		if (res == null) {
			return null;
		}

		AjaxResMetaData resMD = new AjaxResMetaData();

		resMD.name = CollectionUtils.find(res.getNames(), NotPredicate.notPredicate(EqualPredicate.equalPredicate("[unnamed]")));
		resMD.name = StringUtils.substringAfterLast(resMD.name, ".");
		resMD.path = res.getPath();

		for (final Resource subRes : res.getChildResources()) {
			AjaxResMetaData subResMD = build(subRes);
			if (subResMD == null) {
				continue;
			}
			subResMD.parent = resMD;
			resMD.children.add(subResMD);
		}

		for (final ResourceMethod resMd : res.getResourceMethods()) {
			AjaxResMethodMetaData aResMd = AjaxResMethodMetaData.build(resMd);
			if (aResMd == null) {
				continue;
			}
			resMD.methods.add(aResMd);
		}

		return resMD;
	}

	public void setBaseUrl(String _baseUrl) {
		this.baseUrl = _baseUrl;
		for (AjaxResMetaData armd : children) {
			armd.setBaseUrl(_baseUrl + (StringUtils.endsWith(_baseUrl, "/") ? "" : "/") + path);
		}
	}

	public void appendInjectedParams(List<ParamMetaData> _params) {
		for (AjaxResMetaData armd : children) {
			armd.appendInjectedParams(_params);
		}
		for (AjaxResMethodMetaData md : methods) {
			md.params.addAll(_params);
		}
	}
}
