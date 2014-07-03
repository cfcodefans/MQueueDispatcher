package com.thenetcircle.services.dispatcher.http;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.util.EntityUtils;

import com.thenetcircle.services.common.MiscUtils.LoopingArrayIterator;
import com.thenetcircle.services.dispatcher.IMessageActor;
import com.thenetcircle.services.dispatcher.ampq.MQueues;
import com.thenetcircle.services.dispatcher.entity.HttpDestinationCfg;
import com.thenetcircle.services.dispatcher.entity.MessageContext;

public class HttpDispatcherActor implements IMessageActor {

	private static class RespHandler implements FutureCallback<HttpResponse> {
		private MessageContext mc;

		public RespHandler(final MessageContext mc) {
			super();
			this.mc = mc;
		}

		public void cancelled() {

		}

		public void completed(final HttpResponse resp) {
			String respStr = null;
			try {
				respStr = EntityUtils.toString(resp.getEntity());
			} catch (Exception e) {
				log.error("failed to process response from url: \n" + mc.getQueueCfg().getDestCfg().getUrl(), e);
				respStr = e.getMessage();
			}

			mc.setResponse(String.format("{status: %d, resp: '%s'}", resp.getStatusLine().getStatusCode(), respStr.trim()));
			MQueues.instance().getNextActor(instance).handover(mc);
		}

		public void failed(final Exception e) {
			log.error("failed to process response from url: \n" + mc.getQueueCfg().getDestCfg().getUrl(), e);
			mc.setResponse(e.getMessage());
			MQueues.instance().getNextActor(instance).handover(mc);
		}
	}

	public static final int DEFAULT_TIMEOUT = 30000;

	private static HttpDispatcherActor instance = new HttpDispatcherActor();

	protected static final Log log = LogFactory.getLog(HttpDispatcherActor.class.getSimpleName());
	public static HttpDispatcherActor instance() {
		return instance;
	}

	private List<CloseableHttpAsyncClient> hacs;

	private LoopingArrayIterator<CloseableHttpAsyncClient> httpClientIterator = null;

	private HttpDispatcherActor() {
		initHttpAsyncClients();
	}

	public void handle(Collection<MessageContext> mcs) {
		for (final MessageContext mc : mcs) {
			handle(mc);
		}
	}

	public MessageContext handle(final MessageContext mc) {
		final HttpDestinationCfg destCfg = mc.getQueueCfg().getDestCfg();
		final String destUrlStr = destCfg.getUrl();
		
		
		HttpUriRequest req = null;
		
		if (!"get".equalsIgnoreCase(StringUtils.trim(destCfg.getHttpMethod()))) {
			final HttpPost post =  new HttpPost(destUrlStr);
			post.setEntity(new ByteArrayEntity(mc.getMessageBody()));
			req = post;
		} else {
			String queryStr = "";
			final String msgStr = new String(mc.getMessageBody());
			try {
				queryStr = URLEncoder.encode(msgStr, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				log.error("fail to encode message: " + msgStr, e);
			}
			final HttpGet get = new HttpGet(destUrlStr + "?" + queryStr);
			req = get;
		}
		
		if (StringUtils.isNotBlank(destCfg.getHostHead())) {
			req.addHeader("host", destCfg.getHostHead());
		}
		
		final HttpClientContext httpClientCtx = HttpClientContext.create();
		if (destCfg.getTimeout() != DEFAULT_TIMEOUT) {
			httpClientCtx.setRequestConfig(RequestConfig.custom().setSocketTimeout(30000).setConnectTimeout(30000).build());
		}
		
		httpClientIterator.loop().execute(req, httpClientCtx, new RespHandler(mc));
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
		if (CollectionUtils.isEmpty(hacs))
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
		hacs = new ArrayList<CloseableHttpAsyncClient>();
		for (int i = 0, j = 4; i < j; i++) {
			CloseableHttpAsyncClient hac = null;
			RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(DEFAULT_TIMEOUT).setConnectTimeout(DEFAULT_TIMEOUT).build();
			hac = HttpAsyncClients.custom().setDefaultRequestConfig(requestConfig).build();
			hac.start();
			hacs.add(hac);
		}
		httpClientIterator = new LoopingArrayIterator<CloseableHttpAsyncClient>(hacs.toArray(new CloseableHttpAsyncClient[0]));
	}

}
