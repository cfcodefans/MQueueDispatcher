package com.thenetcircle.services.dispatcher.http;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.NoConnectionReuseStrategy;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import com.thenetcircle.services.common.MiscUtils;
import com.thenetcircle.services.common.MiscUtils.LoopingArrayIterator;
import com.thenetcircle.services.dispatcher.IMessageActor;
import com.thenetcircle.services.dispatcher.ampq.Responder;
import com.thenetcircle.services.dispatcher.entity.HttpDestinationCfg;
import com.thenetcircle.services.dispatcher.entity.MessageContext;
import com.thenetcircle.services.dispatcher.entity.QueueCfg;
import com.thenetcircle.services.dispatcher.mgr.Monitor;

public class HttpDispatcherActor implements IMessageActor {

	private static class RespHandler implements FutureCallback<HttpResponse> {
		private MessageContext mc;
		private StopWatch sw = new StopWatch();

		public RespHandler(final MessageContext mc) {
			super();
			this.mc = mc;
			sw.start();
		}

		public void cancelled() {

		}

		public void completed(final HttpResponse resp) {
			sw.stop();
			String respStr = null;
			try {
				respStr = EntityUtils.toString(resp.getEntity());
			} catch (Exception e) {
				log.error("failed to process response from url: \n" + mc.getQueueCfg().getDestCfg().getUrl(), e);
				respStr = e.getMessage();
			}

			mc.setResponse(String.format("{status: %d, resp: '%s'}", resp.getStatusLine().getStatusCode(), respStr.trim()));
			
			Monitor.prefLog(mc, log);
			Responder.instance().handover(mc);
		}

		public void failed(final Exception e) {
			sw.stop();
			final long time = sw.getTime();
			log.error(String.format("after %d ms \nfailed to process response from url: \n%s", time, mc.getQueueCfg().getDestCfg().getUrl()), e);
			mc.setResponse(String.format("{status: %s, resp: '%s', time: %d}", e.getClass().getSimpleName(), e.getMessage(), time));
//			mc.setResponse(e.getClass().getSimpleName() + ": " + e.getMessage());
			
			
			Monitor.prefLog(mc, log);
			Responder.instance().handover(mc);
		}
	}

	public static final int DEFAULT_TIMEOUT = 30000;

	private static final int CLIENT_NUM = MiscUtils.AVAILABLE_PROCESSORS * 5;

	private static HttpDispatcherActor instance = new HttpDispatcherActor();

	protected static final Log log = LogFactory.getLog(HttpDispatcherActor.class.getSimpleName());

	public static HttpDispatcherActor instance() {
		return instance;
	}

	private final static List<NameValuePair> getParamsList(final String... nameAndValues) {
		if (ArrayUtils.isEmpty(nameAndValues)) {
			return null;
		}
		final List<NameValuePair> params = new ArrayList<NameValuePair>();
		for (int i = 0, j = nameAndValues.length; i < j - 1; i += 2) {
			params.add(new BasicNameValuePair(nameAndValues[i], nameAndValues[i + 1]));
		}
		return params;
	}

	private CloseableHttpAsyncClient[] hacs;

	private LoopingArrayIterator<CloseableHttpAsyncClient> httpClientIterator = null;

	private HttpDispatcherActor() {
		initHttpAsyncClients();
	}

	public void handle(Collection<MessageContext> mcs) {
		for (final MessageContext mc : mcs) {
			handle(mc);
		}
	}

	@SuppressWarnings({ "deprecation" })
	public MessageContext handle(final MessageContext mc) {
		Monitor.prefLog(mc, log);

		final QueueCfg qc = mc.getQueueCfg();
		final HttpDestinationCfg destCfg = qc.getDestCfg();
		final String destUrlStr = destCfg.getUrl();

		HttpUriRequest req = null;

		final String bodyStr = new String(mc.getMessageBody());
		try {
			if (!"get".equalsIgnoreCase(StringUtils.trim(destCfg.getHttpMethod()))) {
				final HttpPost post = new HttpPost(destUrlStr);

				final List<NameValuePair> paramList = getParamsList("queueName", qc.getName(), "bodyData", bodyStr);
				UrlEncodedFormEntity fe = new UrlEncodedFormEntity(paramList, HTTP.UTF_8);

				post.setEntity(fe);
				req = post;
			} else {
				final String msgStr = bodyStr;
				final String queryStr = URLEncoder.encode(msgStr, "UTF-8");
				final HttpGet get = new HttpGet(destUrlStr + "?" + queryStr);
				req = get;
			}
		} catch (UnsupportedEncodingException e) {
			log.error("fail to encode message: " + bodyStr, e);
			return mc;
		}

		if (StringUtils.isNotBlank(destCfg.getHostHead())) {
			req.addHeader("host", destCfg.getHostHead());
		}

		final HttpClientContext httpClientCtx = HttpClientContext.create();
		// if (destCfg.getTimeout() != DEFAULT_TIMEOUT) {
		final int timeout = (int) Math.max(destCfg.getTimeout(), DEFAULT_TIMEOUT);
		httpClientCtx.setRequestConfig(RequestConfig.custom().setSocketTimeout(timeout).setConnectTimeout(timeout).build());
		// }

		httpClientIterator.loop().execute(req, httpClientCtx, new RespHandler(mc));
		Monitor.prefLog(mc, log);
		return mc;
	}

	public void handover(Collection<MessageContext> mcs) {
		handle(mcs);
	}

	public MessageContext handover(MessageContext mc) {
		handle(mc);
		return mc;
	}

	public void shutdown() {
		if (ArrayUtils.isEmpty(hacs))
			return;
		try {
			for (final CloseableHttpAsyncClient hac : hacs) {
				hac.close();
			}
		} catch (IOException e) {
			log.error("failed to close httpclient", e);
		}
	}

	public void stop() {
		for (final CloseableHttpAsyncClient hac : hacs) {
			close(hac);
		}
	}

	private void close(final CloseableHttpAsyncClient hac) {
		if (!hac.isRunning()) {
			return;
		}
		try {
			hac.close();
		} catch (IOException e) {
			log.error("failed to close CloseableHttpAsyncClient", e);
		}
	}

	private void initHttpAsyncClients() {
		hacs = new CloseableHttpAsyncClient[CLIENT_NUM];
		for (int i = 0, j = CLIENT_NUM; i < j; i++) {
			final RequestConfig reqCfg = RequestConfig.custom()
											.setSocketTimeout(DEFAULT_TIMEOUT)
											.setConnectTimeout(DEFAULT_TIMEOUT)
											.build();
			
			final IOReactorConfig ioCfg = IOReactorConfig.custom().setInterestOpQueued(true).build();
			final CloseableHttpAsyncClient hac = HttpAsyncClients.custom()
//													.setMaxConnPerRoute(50)
													.setConnectionReuseStrategy(new NoConnectionReuseStrategy())
													.setDefaultRequestConfig(reqCfg)
													.setDefaultIOReactorConfig(ioCfg)
													.build();
			hac.start();
			hacs[i] = hac;
		}
		httpClientIterator = new LoopingArrayIterator<CloseableHttpAsyncClient>(hacs);
	}
}
