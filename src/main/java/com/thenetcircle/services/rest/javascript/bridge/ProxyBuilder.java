package com.thenetcircle.services.rest.javascript.bridge;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.Builder;
import org.glassfish.jersey.server.model.ResourceMethod;

import com.thenetcircle.services.rest.javascript.bridge.JQueryAjaxProxy.DataType;

public class ProxyBuilder implements Builder<JQueryAjaxProxy> {
		private JQueryAjaxProxy jp = new JQueryAjaxProxy();
		
		public static ProxyBuilder builder() {
			return new ProxyBuilder();
		}
		
		public ProxyBuilder with(ResourceMethod resMd) {
			if (resMd == null) return this;
			
			jp.url = resMd.getParent().getPath();
			jp.type = resMd.getHttpMethod();
			jp.dataTypes = StringUtils.join(DataType.convert(resMd.getProducedTypes()).iterator(), " ");
//			jp.contentType = resMd.getConsumedTypes().get(0).getType();
			
			return this;
		}
		
		@Override
		public JQueryAjaxProxy build() {
			return jp;
		}
	}