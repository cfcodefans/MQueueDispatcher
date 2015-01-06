package com.thenetcircle.services.commons.web.mvc;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;

import org.apache.commons.io.FilenameUtils;
import org.glassfish.jersey.server.mvc.Viewable;
import org.glassfish.jersey.server.mvc.spi.ResolvedViewable;
import org.glassfish.jersey.server.mvc.spi.TemplateProcessor;

public class ProcessorFactory {
	
	public static ResolvedViewable<String> getViewable(HttpServletResponse _resp, 
													   HttpServletRequest _req, 
													   String basePath, 
													   String subPathStr, 
													   Object model) {
		TemplateProcessor<String> _processor = null;
		final String extension = FilenameUtils.getExtension(subPathStr);
		
		if ("js".equalsIgnoreCase(extension)) {
			_processor = new JSProcessor(_req, _resp);
		} else if ("html".equalsIgnoreCase(extension)
				   || "htm".equalsIgnoreCase(extension)
				   || "xhtml".equalsIgnoreCase(extension)) {
			_processor = new HtmlViewProcessor(_req, _resp, basePath);
		} else if ("xml".equalsIgnoreCase(extension) || "xsl".equalsIgnoreCase(extension)) {
			_processor = new XmlViewProcessor(_req, _resp, basePath);
		} else {
			_processor = new ResViewProcessor(_req, _resp, basePath);
		}
		
		return new ResolvedViewable<String>(_processor, subPathStr, new Viewable(subPathStr, model), getMediaTypeByExt(extension));
	}
	
	public static MediaType getMediaTypeByPath(final String resPath) {
		final String extension = FilenameUtils.getExtension(resPath);
		
		return getMediaTypeByExt(extension);
	}

	public static MediaType getMediaTypeByExt(final String extension) {
		if ("js".equalsIgnoreCase(extension)) {
			return MediaType.TEXT_PLAIN_TYPE;
		} else if ("html".equalsIgnoreCase(extension)
				   || "htm".equalsIgnoreCase(extension)
				   || "xhtml".equalsIgnoreCase(extension)) {
			return MediaType.TEXT_HTML_TYPE;
		} else if ("xml".equalsIgnoreCase(extension) || "xsl".equalsIgnoreCase(extension)) {
			return MediaType.APPLICATION_XML_TYPE;
		} else if ("json".equalsIgnoreCase(extension)) {
			return MediaType.APPLICATION_JSON_TYPE;
		} 
		
		return MediaType.WILDCARD_TYPE;
	}
}
