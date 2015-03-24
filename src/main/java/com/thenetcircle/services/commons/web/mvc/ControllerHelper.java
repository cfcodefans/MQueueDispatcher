package com.thenetcircle.services.commons.web.mvc;

import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

public class ControllerHelper {

	public static String getBasePath(final HttpServletRequest _req) {
		final String basePathParam = _req.getServletContext().getServletRegistration("JerseyController").getInitParameter("joint.jersey.mvc.base");
		return StringUtils.defaultIfBlank(basePathParam, "/");
	}

	public static String joinPaths(final String... paths) {
		if (ArrayUtils.isEmpty(paths)) {
			return StringUtils.EMPTY;
		}

		final StringBuilder sb = new StringBuilder(0);

		Stream.of(paths).filter(StringUtils::isNotBlank).forEach(path -> {
			path = path.trim();
			path = StringUtils.strip(path, "/");
			path = StringUtils.stripEnd(path, "/");
			sb.append('/');
			sb.append(path);
		});

		sb.deleteCharAt(0);

		return sb.toString();
	}
}
