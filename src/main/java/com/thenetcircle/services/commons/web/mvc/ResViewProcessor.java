package com.thenetcircle.services.commons.web.mvc;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.glassfish.jersey.server.ContainerException;
import org.glassfish.jersey.server.mvc.Viewable;
import org.glassfish.jersey.server.mvc.spi.TemplateProcessor;

public class ResViewProcessor implements TemplateProcessor<String> {

	protected HttpServletRequest req;
	protected HttpServletResponse resp;
	
	protected String basePathStr = "/";  
	private static Log log = LogFactory.getLog(ResViewProcessor.class);
	
	public ResViewProcessor(final HttpServletRequest _req, 
							final HttpServletResponse _resp, 
							final String _basePathStr) {//, final BeanManager _beanMgr) {
		super();
		this.req = _req;
		this.resp = _resp;
		this.basePathStr = StringUtils.defaultIfBlank(_basePathStr, "/");
	}
	
	public ResViewProcessor(final HttpServletRequest _req, 
							final HttpServletResponse _resp) {//, final BeanManager _beanMgr) {
		super();
		this.req = _req;
		this.resp = _resp;
		this.basePathStr = ControllerHelper.getBasePath(_req);
	}
	
	@Override
	public String resolve(final String name, MediaType mediaType) {
		log.debug(name);
		return "/" + basePathStr + (basePathStr.endsWith("/") ? "" : "/") + name;
	}

	@Override
	public void writeTo(String templateReference, Viewable viewable, MediaType mediaType,
            final MultivaluedMap<String, Object> httpHeaders, OutputStream out) throws IOException {
//	public void writeTo(final String t, final Viewable viewable, final OutputStream out) throws IOException {
		final String resolvedPathStr = resolve(templateReference, mediaType);
		log.info(String.format("\n\t path: %s \n\t resolvedPath: %s \n\t viewable: %s \n\t req: %s", 
				templateReference, 
				resolvedPathStr, 
				ToStringBuilder.reflectionToString(viewable), 
				req.getParameterMap()));
		try {
			out.write(process(resolvedPathStr, "/"));
		} catch (Exception e) {
			log.error("failed to process path:\n\t" + resolvedPathStr, e);
			throw new ContainerException(e);
		}
	}
	
	public byte[] process(final String currentPathStr, final String baseUriStr) throws Exception {
		if (StringUtils.isEmpty(currentPathStr)) {
			return new byte[0];
		}
		
		return IOUtils.toByteArray(getResAsInputStream(currentPathStr));
	}

	public InputStream getResAsInputStream(final String currentPathStr) throws MalformedURLException, IOException {
		final ServletContext ctx = req.getServletContext();
		final URL resUrl = ctx.getResource(currentPathStr); //TODO, need cache
		
		if (resUrl == null) {
			final String errorMsg = "request a non-exist path: " + currentPathStr;
			log.error(errorMsg);
			throw new FileNotFoundException(errorMsg);
		}
		
		log.info(resUrl.toString());
		final InputStream resIS = resUrl.openStream();
		return resIS;
	}
}
