package com.thenetcircle.services.rest.javascript.bridge;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.collections4.CollectionUtils;

import com.thenetcircle.services.common.MiscUtils;

@XmlRootElement(name="JQueryProxy")
public class JQueryAjaxProxy {
	public static enum DataType {
		text, html, script, json, jsonp, xml;
		
		@SuppressWarnings("unchecked")
		public static final Map<MediaType, JQueryAjaxProxy.DataType> internalMap = MiscUtils.map(
					MediaType.APPLICATION_JSON_TYPE, json,
					
					MediaType.TEXT_PLAIN_TYPE, text,
					
					MediaType.APPLICATION_XML_TYPE, xml,
					MediaType.TEXT_XML_TYPE, xml,
					
					MediaType.TEXT_HTML, html,
					MediaType.APPLICATION_XHTML_XML_TYPE, html
				);
		
		public static List<JQueryAjaxProxy.DataType> convert(List<MediaType> mediaTypes) {
			final List<JQueryAjaxProxy.DataType> dataTypeList = new ArrayList<JQueryAjaxProxy.DataType>();
			
			for (MediaType mt : mediaTypes) {
				final JQueryAjaxProxy.DataType dt = internalMap.get(mt);
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
	
	public String type; //GET or POST
	public String url;  
	public String dataTypes; //produced by restful api
	public String contentType; // consumed by restful api;
}