package com.thenetcircle.services.commons.web.mvc;

import java.io.InputStream;
import java.util.Map;

import javax.script.ScriptContext;
import javax.script.SimpleScriptContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.thenetcircle.services.commons.web.joint.script.PageScriptExecutionContext;
import com.thenetcircle.services.commons.web.joint.script.ScriptExecutor;
import com.thenetcircle.services.commons.web.joint.script.javascript.JSExecutor;
import com.thenetcircle.services.commons.web.mvc.ResCacheMgr.CachedEntry;
import com.thenetcircle.services.commons.web.mvc.ViewProcModel.ScriptCtxModel;
import com.thenetcircle.services.commons.web.mvc.ViewProcModel.ViewFacade;

public class HtmlViewProcessor extends ResViewProcessor {

	private static Log log = LogFactory.getLog(HtmlViewProcessor.class);

	public HtmlViewProcessor(final HttpServletRequest _req, final HttpServletResponse _resp, final String _basePathStr) {
		super(_req, _resp, StringUtils.defaultIfBlank(_basePathStr, "/"));
	}

	public HtmlViewProcessor(final HttpServletRequest _req, final HttpServletResponse _resp) {
		super(_req, _resp);
	}

	public byte[] process(final String currentPathStr, final String baseUriStr) throws Exception {
		if (StringUtils.isEmpty(currentPathStr)) {
			log.error("illegalArguement: currentPathStr is empty: " + currentPathStr);
			return new byte[0];
		}

		super.resp.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML);
		CachedEntry<?> cachedEntry = ResCacheMgr.cacheMap.get(currentPathStr);

		if (cachedEntry == null) {
			final InputStream resIS = getResAsInputStream(currentPathStr);

			final Document doc = Jsoup.parse(resIS, "UTF-8", baseUriStr);
			final Elements els = doc.select("script[data-runat=server]");

			ViewProcModel vpm = new ViewProcModel(doc);

			for (final Element el : els) {
				vpm.getScriptCtxModelList().add(new ScriptCtxModel(doc, el));
			}

			CachedEntry<ViewProcModel> newEntry = new CachedEntry<ViewProcModel>(vpm);
			ResCacheMgr.cacheMap.put(currentPathStr, newEntry);
			cachedEntry = newEntry;
		}

		if (cachedEntry.content instanceof ViewProcModel) {
			ViewProcModel vpm = (ViewProcModel) cachedEntry.content;
			ViewFacade vf = vpm.getViewFacade();
			Element doc = vf._doc;
			ScriptContext sc = new SimpleScriptContext();

			for (final Element scriptElement : vf.scriptElementList) {
				final ScriptExecutor se = new JSExecutor(new PageScriptExecutionContext(doc, scriptElement, req, resp, currentPathStr, sc));
				sc = se.execute();
			}

			return doc.html().getBytes();
		}

		return String.format("can't render this as html: \n%s", String.valueOf(cachedEntry)).getBytes();
	}

	public void bindValuesToFormFields(final Element form) {
		final Map<String, String[]> params = req.getParameterMap();
		for (final String fieldName : params.keySet()) {
			final Elements fields = form.select(String.format("input[name=%s]", fieldName));
			if (CollectionUtils.isEmpty(fields)) {
				continue;
			}

			final String[] values = params.get(fieldName);
			if (ArrayUtils.isEmpty(values)) {
				continue;
			}

			for (int i = 0, j = fields.size(), k = values.length; i < j && i < k; i++) {
				fields.get(i).val(values[i]);
			}
		}
	}
}
