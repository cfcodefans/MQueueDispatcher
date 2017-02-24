package com.thenetcircle.services.dispatcher.http;

import com.thenetcircle.services.commons.MiscUtils;
import com.thenetcircle.services.commons.MiscUtils.LoopingArrayIterator;
import com.thenetcircle.services.commons.actor.IActor;
import com.thenetcircle.services.dispatcher.ampq.MQueueMgr;
import com.thenetcircle.services.dispatcher.ampq.Responder;
import com.thenetcircle.services.dispatcher.entity.HttpDestinationCfg;
import com.thenetcircle.services.dispatcher.entity.MessageContext;
import com.thenetcircle.services.dispatcher.entity.MsgResp;
import com.thenetcircle.services.dispatcher.entity.QueueCfg;
import com.thenetcircle.services.dispatcher.mgr.MsgMonitor;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.sync.CloseableHttpClient;
import org.apache.hc.client5.http.impl.sync.FutureRequestExecutionService;
import org.apache.hc.client5.http.impl.sync.HttpClients;
import org.apache.hc.client5.http.methods.HttpGet;
import org.apache.hc.client5.http.methods.HttpPost;
import org.apache.hc.client5.http.methods.HttpUriRequest;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.sync.ResponseHandler;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.reactor.IOReactorConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.apache.commons.lang3.CharEncoding.UTF_8;

public class HttpDispatcherActor implements IActor<MessageContext> {

	private static class RespHandler implements ResponseHandler<HttpResponse>, FutureCallback<HttpResponse> {
		private MessageContext mc;
		private StopWatch sw = new StopWatch();

		public RespHandler(final MessageContext mc) {
			super();
			this.mc = mc;
			sw.start();
		}

		public void cancelled() {
			MQueueMgr.instance().acknowledge(mc);
		}

		public void completed(final HttpResponse resp) {
			sw.stop();
			String respStr;
			try {
				respStr = EntityUtils.toString(resp.getEntity());
			} catch (Exception e) {
				log.error("failed to process response from url: \n" + mc.getQueueCfg().getDestCfg().getUrl(), e);
				respStr = e.getMessage();
			}

			mc.setResponse(new MsgResp(resp.getStatusLine().getStatusCode(), StringUtils.substring(StringUtils.trimToEmpty(respStr), 0, 2000)));

			MsgMonitor.prefLog(mc, log);
			Responder.instance(mc.getDelivery().getEnvelope().getDeliveryTag()).handover(mc);
		}

		public void failed(final Exception e) {
			sw.stop();
			final long time = sw.getTime();
			final long deliveryTag = mc.getDelivery().getEnvelope().getDeliveryTag();
			log.error(String.format("Message: %d failed after %d ms \n gettng response from url: \n%s", deliveryTag, time, mc.getQueueCfg().getDestCfg().getUrl()), e);

			mc.setResponse(new MsgResp(MsgResp.FAILED,
					String.format("{status: %s, resp: '%s', time: %d}", e.getClass().getSimpleName(), e.getMessage(), time)));

			MsgMonitor.prefLog(mc, log);
			Responder.instance(deliveryTag).handover(mc);
		}

        @Override
        public HttpResponse handleResponse(HttpResponse resp) throws IOException {
            return null;
        }
    }

	public static final int DEFAULT_TIMEOUT = 30000;

	private static int CLIENT_NUM = MiscUtils.AVAILABLE_PROCESSORS * 5;

	private static HttpDispatcherActor instance = new HttpDispatcherActor();

    private static final Logger log = LogManager.getLogger(HttpDispatcherActor.class);

    private static Charset CHARSET = Charset.forName(UTF_8);

	public static HttpDispatcherActor instance() {
		return instance;
	}

	private static List<NameValuePair> getParamsList(final String... nameAndValues) {
		if (ArrayUtils.isEmpty(nameAndValues)) {
			return Collections.emptyList();
		}

		final List<NameValuePair> params = new ArrayList<>();
		IntStream.range(0, nameAndValues.length - 1).filter(i -> (i % 2 == 0))
				.forEach(i -> params.add(new BasicNameValuePair(nameAndValues[i], nameAndValues[i + 1])));

		return params;
	}

	private FutureRequestExecutionService[] closeableHttpClients;

	private LoopingArrayIterator<FutureRequestExecutionService> httpClientIterator = null;

	private HttpDispatcherActor() {
		CLIENT_NUM = (int) MiscUtils.getPropertyNumber("httpclient.number", CLIENT_NUM);
		initHttpAsyncClients();
	}

	public void handle(Collection<MessageContext> mcs) {
		mcs.forEach(this::handle);
	}

	protected String resolveDestUrl(final MessageContext mc) {
		final QueueCfg qc = mc.getQueueCfg();
		final HttpDestinationCfg destCfg = qc.getDestCfg();
		String defaultUrl = destCfg.getUrl();

		Map<String, Object> headers = mc.getDelivery().getProperties().getHeaders();
		if (MapUtils.isEmpty(headers)) {
			return defaultUrl;
		}

		String origin = ObjectUtils.defaultIfNull(headers.get("consumer_target"), defaultUrl).toString();
		return StringUtils.isBlank(origin) ? defaultUrl : origin;
	}

	@SuppressWarnings({ "deprecation" })
	public MessageContext handle(final MessageContext mc) {
		MsgMonitor.prefLog(mc, log);

		final QueueCfg qc = mc.getQueueCfg();
		final HttpDestinationCfg destCfg = qc.getDestCfg();
		final String destUrlStr = resolveDestUrl(mc);

		HttpUriRequest req;

		final String bodyStr = new String(mc.getMessageBody());
		try {
			if (!"get".equalsIgnoreCase(StringUtils.trim(destCfg.getHttpMethod()))) {
				final HttpPost post = new HttpPost(destUrlStr);

				//TODO should be queueName instead of config name
				final List<NameValuePair> paramList = getParamsList("queueName", qc.getName(), "bodyData", bodyStr);
				UrlEncodedFormEntity fe = new UrlEncodedFormEntity(paramList, CHARSET);
//				GzipCompressingEntity ze = new GzipCompressingEntity(fe);

				post.setEntity(fe);
				req = post;
			} else {
				req = new HttpGet(destUrlStr + "?" + URLEncoder.encode(bodyStr, "UTF-8"));
			}
		} catch (UnsupportedEncodingException e) {
			log.error("fail to encode message: " + bodyStr, e);
			return mc;
		}

		if (StringUtils.isNotBlank(destCfg.getHostHead())) {
			req.addHeader("host", destCfg.getHostHead());
		}

		final HttpClientContext httpClientCtx = HttpClientContext.create();
		final int timeout = (int) Math.max(destCfg.getTimeout(), DEFAULT_TIMEOUT);
		httpClientCtx.setRequestConfig(RequestConfig.custom()
										.setConnectionRequestTimeout(timeout)
										.setSocketTimeout(timeout)
										.setConnectTimeout(timeout)
//										.setDecompressionEnabled(true)
										.build());
		// }

		final FutureRequestExecutionService[] clientArray = httpClientIterator.getArray();
		clientArray[(int)(mc.getDelivery().getEnvelope().getDeliveryTag() % clientArray.length)]
			.execute(req, httpClientCtx, new RespHandler(mc));
		MsgMonitor.prefLog(mc, log);
		return mc;
	}

	public void handover(Collection<MessageContext> mcs) {
		handle(mcs);
	}

	public MessageContext handover(MessageContext mc) {
		try {
			return handle(mc);
		} catch (Exception e) {
			log.error("failed to sent\n" + mc, e);
		}
		return null;
	}

	public void stop() {
		if (ArrayUtils.isEmpty(closeableHttpClients))
			return;

		if (stopped.compareAndSet(false, true))
			Stream.of(closeableHttpClients).forEach(this::close);
	}

	private void close(final FutureRequestExecutionService hac) {
		try {
			hac.close();
		} catch (IOException e) {
			log.error("failed to close CloseableHttpAsyncClient", e);
		}
	}

	private void initHttpAsyncClients() {
		closeableHttpClients = new FutureRequestExecutionService[CLIENT_NUM];
		IntStream.range(0, CLIENT_NUM).forEach(i-> {
			final RequestConfig reqCfg = RequestConfig.custom()
											.setConnectTimeout(DEFAULT_TIMEOUT)
											.setSocketTimeout(DEFAULT_TIMEOUT)
											.setConnectTimeout(DEFAULT_TIMEOUT)
											.build();

			final IOReactorConfig ioCfg = IOReactorConfig.custom().setInterestOpQueued(true).build();
//to deal with https
			final CloseableHttpClient chc = HttpClients.custom()
													.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
													.setMaxConnTotal((int)MiscUtils.getPropertyNumber("http.client.max.connection", 50))
													.setMaxConnPerRoute((int)MiscUtils.getPropertyNumber("http.client.max.connection.per_route", 25))
													.setDefaultRequestConfig(reqCfg)
				.build();
//													.setDefaultIOReactorConfig(ioCfg)
//													.setConnectionReuseStrategy(new NoConnectionReuseStrategy())
			this.closeableHttpClients[i] = new FutureRequestExecutionService(chc, Executors.newWorkStealingPool());
		});
		httpClientIterator = new LoopingArrayIterator<>(closeableHttpClients);
	}

	@Override
	public boolean isStopped() {
		return stopped.get();
	}

	private AtomicBoolean stopped;
}
