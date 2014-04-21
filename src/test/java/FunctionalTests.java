import java.util.Arrays;

import junit.framework.Assert;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.rabbitmq.client.Channel;
import com.thenetcircle.services.dispatcher.ampq.MQueues;
import com.thenetcircle.services.dispatcher.entity.ExchangeCfg;
import com.thenetcircle.services.dispatcher.entity.HttpDestinationCfg;
import com.thenetcircle.services.dispatcher.entity.QueueCfg;
import com.thenetcircle.services.dispatcher.entity.ServerCfg;


public class FunctionalTests {
	private static final String DEST_URL = "http://edgy:8888/consumerDispatcher_ok";
	
	private static final long MSG_NUMBER = 100000;
	private static QueueCfg qc = null;
	
	@BeforeClass
	public static void initQueue() {
		qc = new QueueCfg();
		{
			final ServerCfg serverCfg = new ServerCfg();
			serverCfg.setHost("snowball");
			serverCfg.setLogFilePath("./test/test.log");
			serverCfg.setMaxFileSize("20MB");
			serverCfg.setPassword("guest");
			serverCfg.setUserName("guest");
			serverCfg.setVirtualHost("/");
			serverCfg.setPort(5672);
			qc.setServerCfg(serverCfg );
		}
		
		qc.setAutoDelete(false);
		{
			final HttpDestinationCfg destCfg = new HttpDestinationCfg();
			destCfg.setHostHead("test");
			destCfg.setUrl(DEST_URL);
			qc.setDestCfg(destCfg );
		}
		
		qc.setDurable(true);
		qc.setEnabled(true);
		{
			final ExchangeCfg exCfg = new ExchangeCfg();
			exCfg.setAutoDelete(false);
			exCfg.setDurable(true);
			exCfg.setExchangeName("test_ex");
			exCfg.setType("direct");
			qc.getExchanges().add(exCfg);
		}
		
		qc.setRouteKey("test");
		qc.setQueueName("testQueue");
		qc.setExclusive(true);
	}
	
	@Test
	public void testConsumerActor() throws Exception {
		MQueues.getInstance().initWithQueueCfgs(Arrays.asList(qc));

		final Channel ch = MQueues.getInstance().getChannel(qc);
		Assert.assertNotNull(ch);

		for (long i = 0; i < MSG_NUMBER; i++) {
			for (final ExchangeCfg exCfg : qc.getExchanges()) {
				final String msgStr = "test_message: " + System.currentTimeMillis();
				ch.basicPublish(exCfg.getExchangeName(), qc.getRouteKey(), null, msgStr.getBytes());
			}
		}

	}
	
	@AfterClass
	public static void tearDown() {
		MQueues.getInstance().shutdown();
	}
}
