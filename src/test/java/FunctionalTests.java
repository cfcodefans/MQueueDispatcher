import java.util.Arrays;
import java.util.List;

import javax.persistence.EntityManager;

import junit.framework.Assert;
import mgr.dao.QueueCfgDao;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.thenetcircle.services.dispatcher.ampq.MQueues;
import com.thenetcircle.services.dispatcher.entity.ExchangeCfg;
import com.thenetcircle.services.dispatcher.entity.HttpDestinationCfg;
import com.thenetcircle.services.dispatcher.entity.QueueCfg;
import com.thenetcircle.services.dispatcher.entity.ServerCfg;
import com.thenetcircle.services.persistence.jpa.JpaModule;


public class FunctionalTests {
	private static final String DEST_URL = "http://edgy:8888/consumerDispatcher_ok";
	
	private static final long MSG_NUMBER = 20000;
	private static QueueCfg qc = null;
	
	@BeforeClass
	public static void initQueue() {
		final EntityManager em = JpaModule.getEntityManager();
		
		em.getTransaction().begin();
		
		QueueCfgDao qcDao = new QueueCfgDao(em);
		final List<QueueCfg> qcList = qcDao.query("select qc from QueueCfg qc LEFT JOIN FETCH qc.exchanges where qc.queueName=?1", "testQueue");
		if (!CollectionUtils.isEmpty(qcList)) {
			qc = qcList.get(0);
//			qc.getExchanges().addAll(qcDao.queryEntity("select exc from ExchangeCfg exc where ?1 member of exc.queues", qc));
			log.info(qc.getExchanges());
			em.getTransaction().commit();
			return;
		}
		
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
			qc.setServerCfg(serverCfg);
		}
		
		qc.setAutoDelete(false);
		{
			final HttpDestinationCfg destCfg = new HttpDestinationCfg();
			destCfg.setHostHead("test");
			destCfg.setUrl(DEST_URL);
			destCfg.setHttpMethod("get");
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
			exCfg.getQueues().add(qc);
			em.persist(exCfg);
			qc.getExchanges().add(exCfg);
		}
		
		qc.setRouteKey("test");
		qc.setQueueName("testQueue");
		qc.setExclusive(false);
		qc.setRetryLimit(3);
		
		Runtime.getRuntime().addShutdownHook(MQueues.cleaner);
		
		em.persist(qc);
		
		em.getTransaction().commit();
	}

	static Logger log = Logger.getLogger(FunctionalTests.class.getSimpleName());
	
	final ServerCfg sc = qc.getServerCfg();
	final ConnectionFactory cf = new ConnectionFactory();

	private Connection conn;

	private Channel ch;

	@Test
	public void testPublish() throws Exception {
		cf.setHost(sc.getHost());
		cf.setVirtualHost(sc.getVirtualHost());
		cf.setPort(sc.getPort());
		cf.setUsername(sc.getUserName());
		cf.setPassword(sc.getPassword());
		
		conn = cf.newConnection();
		ch = conn.createChannel();
		
		for (final ExchangeCfg ec : qc.getExchanges()) {
			ch.exchangeDeclare(ec.getExchangeName(), ec.getType(), ec.isDurable(), ec.isAutoDelete(), null);
			ch.queueDeclare(qc.getQueueName(), qc.isDurable(), qc.isExclusive(), qc.isAutoDelete(), null);
			ch.queueBind(qc.getQueueName(), ec.getExchangeName(), qc.getRouteKey());
		}
		
		for (long i = 0; i < MSG_NUMBER; i++) {
			for (final ExchangeCfg exCfg : qc.getExchanges()) {
				String msgStr = "test_message: " + System.currentTimeMillis() + RandomStringUtils.randomAscii(200);
				if (i == 350) {
					msgStr = "shutdown " + qc.getQueueName();
				}
				ch.basicPublish(exCfg.getExchangeName(), qc.getRouteKey(), null, msgStr.getBytes());
			}
		}
		
		ch.close();
		conn.close();
	}
	
	@Test
	public void testConsumerActor() throws Exception {
		MQueues.instance().startQueues(Arrays.asList(qc));

		final Channel ch = MQueues.instance().getChannel(qc);
		Assert.assertNotNull(ch);
	}

	@AfterClass
	public static void tearDown() {
//		MQueues.getInstance().shutdown();
		JpaModule.instance().destory();
	}
	
//	ScheduledExecutorService ses = Executors.newScheduledThreadPool(1);
//	
//	public void testShutDown() {
////		ses.schedule(command, delay, unit)
//	}
	
	public static void main(String[] args) {
		initQueue();
		final FunctionalTests ft = new FunctionalTests();
		
		try {
//			ft.testPublish();
//			ft.testPublish();
			Thread.sleep(1000);
			ft.testConsumerActor();
			
			Thread.sleep(1000);
			
//			ft.ch.close();
//			ft.conn.close();
		} catch (Exception e) {
			log.info("", e);
		}
		
//		tearDown();
	}
}
