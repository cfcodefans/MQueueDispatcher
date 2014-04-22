package com.thenetcircle.services.dispatcher.http;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.util.EntityUtils;

import com.thenetcircle.services.common.MiscUtils.LoopingArrayIterator;
import com.thenetcircle.services.dispatcher.IMessageActor;
import com.thenetcircle.services.dispatcher.ampq.Responder;
import com.thenetcircle.services.dispatcher.entity.HttpDestinationCfg;
import com.thenetcircle.services.dispatcher.entity.MessageContext;
import com.thenetcircle.services.dispatcher.failsafe.DefaultFailedMessageHandler;
import com.thenetcircle.services.dispatcher.failsafe.IFailsafe;

public class HttpDispatcherActor implements IMessageActor {

	private List<CloseableHttpAsyncClient> hacs;
	

	private static class RespHandler implements FutureCallback<HttpResponse> {
		private static IFailsafe failsafe = DefaultFailedMessageHandler.getInstance();
		private MessageContext mc;

		public RespHandler(final MessageContext mc) {
			super();
			this.mc = mc;
		}

		public void completed(final HttpResponse resp) {
			String respStr = null;
			try {
				respStr = EntityUtils.toString(resp.getEntity());
			} catch (Exception e) {
				log.error("failed to process response from url: \n" + mc.getQueueCfg().getDestCfg().getUrl(), e);
				respStr = e.getMessage();
			}

			mc.setResponse(respStr);
			Responder.getInstance().handover(mc);
		}

		public void failed(final Exception e) {
			log.error("failed to process response from url: \n" + mc.getQueueCfg().getDestCfg().getUrl(), e);
			mc.setResponse(e.getMessage());
			failsafe.handle(mc);
		}

		public void cancelled() {

		}
	}

	private void initHttpAsyncClients() {
		hacs = new ArrayList<CloseableHttpAsyncClient>();
		for (int i = 0, j = 3; i < j; i++) {
			CloseableHttpAsyncClient hac = null;
			RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(30000).setConnectTimeout(30000).build();
			hac = HttpAsyncClients.custom().setDefaultRequestConfig(requestConfig).build();
			hac.start();
			hacs.add(hac);
		}
		httpClientIterator = new LoopingArrayIterator<CloseableHttpAsyncClient>(hacs.toArray(new CloseableHttpAsyncClient[0]));
	}

	private LoopingArrayIterator<CloseableHttpAsyncClient> httpClientIterator = null;

	public MessageContext handle(final MessageContext mc) {
		final HttpDestinationCfg destCfg = mc.getQueueCfg().getDestCfg();
		final String destUrlStr = destCfg.getUrl();
		final HttpPost post = new HttpPost(destUrlStr);
		if (StringUtils.isNotBlank(destCfg.getHostHead())) {
			post.addHeader("host", destCfg.getHostHead());
		}
		post.setEntity(new ByteArrayEntity(mc.getMessageBody()));

		httpClientIterator.loop().execute(post, new RespHandler(mc));
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

	protected static final Log log = LogFactory.getLog(HttpDispatcherActor.class.getSimpleName());

	private static HttpDispatcherActor instance = new HttpDispatcherActor();

	private HttpDispatcherActor() {
		initHttpAsyncClients();
	}

	public static HttpDispatcherActor instance() {
		return instance;
	}

	public MessageContext handover(MessageContext mc) {
		handle(mc);
		return mc;
	}

	public void handover(Collection<MessageContext> mcs) {
		handle(mcs);
	}

	public void handle(Collection<MessageContext> mcs) {
		for (final MessageContext mc : mcs) {
			handle(mc);
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

}
