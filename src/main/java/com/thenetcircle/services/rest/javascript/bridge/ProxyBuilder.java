package com.thenetcircle.services.rest.javascript.bridge;

import org.apache.commons.lang3.builder.Builder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.glassfish.jersey.server.model.Invocable;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;

import com.thenetcircle.services.rest.javascript.bridge.JQueryAjaxProxy.DataType;

public class ProxyBuilder implements Builder<JQueryAjaxProxy> {
	private JQueryAjaxProxy jp = new JQueryAjaxProxy();
	private static Log log = LogFactory.getLog(ProxyBuilder.class);

	public static ProxyBuilder builder() {
		return new ProxyBuilder();
	}

	private void getPath(Resource res, StringBuilder sb) {
		if (res.getParent() == null) {
			sb.append(res.getPath());
			return;
		} else {
			getPath(res.getParent(), sb);
		}
		sb.append('/').append(res.getPath());
	}

	public ProxyBuilder with(ResourceMethod resMd) {
		if (resMd == null)
			return this;

		StringBuilder sb = new StringBuilder();

		getPath(resMd.getParent(), new StringBuilder());

		jp.url = sb.toString();
		jp.type = resMd.getHttpMethod();
		jp.dataTypes = DataType.convert(resMd.getProducedTypes());
		final Invocable iv = resMd.getInvocable();
		jp.params = iv.getParameters();
		jp.return_type = iv.getResponseType().toString();

//		jp.matedata = resMd.getInvocable();

		log.info(String.format("http method: \t%s url:\t%s", jp.type, jp.url));
		return this;
	}

	@Override
	public JQueryAjaxProxy build() {
		return jp;
	}
}