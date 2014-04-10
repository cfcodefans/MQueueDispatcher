import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

import com.thenetcircle.services.common.MiscUtils;


public class HttpAsynClientTest {

	private static final int URL_CNT = 100;
	private static final int MSG_CNT = 5000;
	static Logger log = Logger.getLogger(HttpAsynClientTest.class.getSimpleName());
	
	static class TestCallback implements FutureCallback<HttpResponse> {
		private long tag = 0;
		
		public TestCallback(long tag) {
			super();
			this.tag = tag;
		}

		public void cancelled() {
//			log.info(MiscUtils.invocationInfo());
			counter.incrementAndGet();
		}

		public void completed(final HttpResponse resp) {
//			log.info(MiscUtils.invocationInfo());
//			log.info(String.valueOf(resp.getStatusLine().getStatusCode()));
			try {
//				log.info(EntityUtils.toString(resp.getEntity()));
				counter.incrementAndGet();
				if (counter.get() >= MSG_CNT) {
					log.info("finished");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public void failed(final Exception ex) {
			log.info(MiscUtils.invocationInfo());
			ex.printStackTrace();
			counter.incrementAndGet();
		}
	}
	
	static final String TEST_URL_BASE = "http://wio.poppen.lab:8282/wan2/isonline?SHOW_IMAGE=0&NICKNAME=fan";
	static String[] TEST_URLS = null;
	static {
		TEST_URLS = new String[URL_CNT];
		for (int i = 0; i < URL_CNT; i++) {
			TEST_URLS[i] = TEST_URL_BASE + "&t=" + i;
		}
	}

	private static AtomicInteger counter = new AtomicInteger(0);
	private static StopWatch sw = new StopWatch();
	
	private static final int CLIENTS_CNT = 3; //3 is the most optimal 
	
	@Test
	public void testCallback() throws Exception {
		RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(30000).setConnectTimeout(30000).build();
		CloseableHttpAsyncClient[] hacs = new CloseableHttpAsyncClient[CLIENTS_CNT];
		for (int i = 0 ; i < CLIENTS_CNT; i ++) {
			hacs[i] = HttpAsyncClients.custom().setDefaultRequestConfig(requestConfig).build();
			hacs[i].start();
		}
		
		sw.start();
		
		for (int i = 0; i < MSG_CNT; i++) {
			final HttpPost req = new HttpPost(TEST_URLS[i % URL_CNT]);
			req.setEntity(new StringEntity(String.valueOf(i)));
			
			hacs[i % CLIENTS_CNT].execute(req, new TestCallback(i));
		}
		
		sw.stop();
		log.info("finish sending messages after: " + sw.getTime());

		sw.reset();
		sw.start();
		
		for (int i = 0, j = 0; i < MSG_CNT; j = i, i = counter.get()) {
			try {
				if (j != i) {
//					log.info("received: " + counter.get());
				}
				Thread.sleep(10);
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		
		sw.stop();
		log.info("receive all responses after: " + sw.getTime());
		
		for (int i = 0; i < hacs.length; i++) {
			CloseableHttpAsyncClient hac = hacs[i];
			if (hac.isRunning()) {
				hac.close();
			}
		}
	}
	
	static class PostReqSender implements Runnable {
		private long tag;
		
		public PostReqSender(long tag) {
			super();
			this.tag = tag;
		}

		public void run() {
			try {
				sendHttpPost(TEST_URLS[(int)tag % URL_CNT], "pc-201", String.valueOf(tag), 30);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			counter.incrementAndGet();
			if (counter.get() >= MSG_CNT) {
				log.info("finished");
			}
		}
	}
	
	@Test
	public void testSimplePostsInThreadPool() throws Exception {
		final ExecutorService es = Executors.newFixedThreadPool(MiscUtils.AVAILABLE_PROCESSORS * 128);
		
		sw.start();
		for (int i = 0; i < MSG_CNT; i++) {
			es.submit(new PostReqSender(i));
		}
		sw.stop();
		log.info("finish sending messages after: " + sw.getTime());
		
		sw.reset();
		sw.start();
		
		for (int i = 0, j = 0; i < MSG_CNT; j = i, i = counter.get()) {
			try {
				if (j != i) {
//					log.info("received: " + counter.get());
				}
				Thread.sleep(10);
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		
		sw.stop();
		log.info("receive all responses after: " + sw.getTime());
	}

	private static void abortConnection(final HttpRequestBase hrb, final HttpClient httpclient) {
		if (hrb != null) {
			hrb.abort();
		}
		if (httpclient != null) {
			httpclient.getConnectionManager().shutdown();
		}
	}

	public static String sendHttpPost(final String url, final String host, final String param, int timeout) throws Exception {
		DefaultHttpClient httpclient = getDefaultHttpClient(host);
//		UrlEncodedFormEntity formEntity = null;
//		try {
//			formEntity = new UrlEncodedFormEntity(getParamsList(parameters), HTTP.UTF_8);
//		} catch (UnsupportedEncodingException e) {
//			throw e;
//		}
		HttpPost hp = new HttpPost(url);
		hp.setEntity(new StringEntity(String.valueOf(param)));
		if (StringUtils.isNotBlank(host)) {
			hp.setHeader("Host", host);
		}
	
		try {
			HttpResponse response = httpclient.execute(hp);
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				HttpEntity entity = response.getEntity();
				return new String(EntityUtils.toByteArray(entity));
			} else {
				return  String.format("\nhttp code: %d \nstatus: %s", response.getStatusLine().getStatusCode(), response.getStatusLine().getReasonPhrase());
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, "", e);
			return e.getMessage();
		} finally {
			abortConnection(hp, httpclient);
		}
	}

	private static DefaultHttpClient getDefaultHttpClient(String host) {
		DefaultHttpClient httpclient = new DefaultHttpClient();
		httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
		httpclient.getParams().setParameter("Host", host);
	
		return httpclient;
	}
}
