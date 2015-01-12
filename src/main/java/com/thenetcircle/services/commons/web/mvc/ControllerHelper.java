package com.thenetcircle.services.commons.web.mvc;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

public class ControllerHelper {
	
	public static String getBasePath(final HttpServletRequest _req) {
		final String basePathParam = _req.getServletContext().getServletRegistration("JerseyController").getInitParameter("joint.jersey.mvc.base");
		return StringUtils.defaultIfBlank(basePathParam, "/"); 
	}
	
	public static String joinPaths(final String...paths) {
		if (ArrayUtils.isEmpty(paths)) {
			return StringUtils.EMPTY;
		}
		
		final StringBuilder sb = new StringBuilder(0);
		
		for (int i = 0; i < paths.length; i++) {
			String path = paths[i];
			if (StringUtils.isBlank(path)) {
				continue;
			}
			
			path = path.trim();
			path = StringUtils.strip(path, "/");
			path = StringUtils.stripEnd(path, "/");
			
			if (i > 0) {
				sb.append('/');
			}
			sb.append(path);
		}
		
		return sb.toString();
	}
}
