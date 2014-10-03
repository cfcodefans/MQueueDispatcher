import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.QueueingConsumer.Delivery;
import com.thenetcircle.services.common.MiscUtils;
import com.thenetcircle.services.dispatcher.ampq.MQueueMgr;
import com.thenetcircle.services.dispatcher.entity.ExchangeCfg;
import com.thenetcircle.services.dispatcher.entity.MessageContext;
import com.thenetcircle.services.dispatcher.entity.QueueCfg;
import com.thenetcircle.services.dispatcher.entity.ServerCfg;


public class ThreadsTest {
	
	public static final class ConsumerActor implements Callable<MessageContext> {
		private final QueueCfg qc;
		private final QueueingConsumer c;
		private final Channel ch;
		
		public ConsumerActor(final QueueCfg qc) throws Exception {
			this.qc = qc;
			ch = md.getChannel(qc);
			c = new QueueingConsumer(ch);
			ch.basicConsume(qc.getQueueName(), false, c);
		}

		public MessageContext call() throws Exception {
			final Delivery delivery = c.nextDelivery();
//			log.info(MiscUtils.invocationInfo() + " delivery: " + delivery);
			if (delivery == null) {
				return null;
			}
			final String msg = new String(delivery.getBody());
			log.info(msg);
//			if (msg.contains("9")) {
//				log.info("exit");
//				System.exit(0);
//			}
//			counter.incrementAndGet();
			return new MessageContext(qc, delivery);
		}
	}

	static AtomicInteger counter = new AtomicInteger(0);
	
	public static final class DispatchActor implements Callable<Void> {
		private final Future<MessageContext> future;
		
		public DispatchActor(final Future<MessageContext> future) {
			super();
			this.future = future;
		}

		public Void call() throws Exception {
			if (future == null || future.isCancelled()) {
				return null;
			}
			
			final MessageContext mc = future.get();
			if (mc == null) { 
				log.info(MiscUtils.invocationInfo() + " MessageContext: null");
				return null;
			}
			
			log.info(MiscUtils.invocationInfo() + " MessageContext: " + new String(mc.getMessageBody()));
			log.info(counter.incrementAndGet() + new String(mc.getMessageBody()));
			return null;
		}
	}


	static Logger log = Logger.getLogger(ThreadsTest.class.getSimpleName());

	static List<QueueCfg> queueCfgs = new ArrayList<QueueCfg>();
	static List<ServerCfg> serverCfgs = new ArrayList<ServerCfg>();
	static List<ExchangeCfg> exchangeCfgs = new ArrayList<ExchangeCfg>();
	
	static MQueueMgr md = MQueueMgr.instance();
	
	
	static Channel publisher = null;
	static Connection conn = null;
	
	@BeforeClass
	public static void initPublisher() {
		final ConnectionFactory cf = new ConnectionFactory();
		cf.setHost("localhost");
		try {
			conn = cf.newConnection(Executors.newSingleThreadExecutor());
			publisher = conn.createChannel();
//			publisher.addShutdownListener(new ShutdownListener() {
//				public void shutdownCompleted(ShutdownSignalException cause) {
//					log.info(MiscUtils.invocationInfo());
//				}
//			});
		} catch (final Exception e) {
			log.log(Level.SEVERE, "fail to initiate publisher", e);
		}
	}
	
	@BeforeClass
	public static void initCfgs() {
		final ServerCfg sc = new ServerCfg();
		serverCfgs.add(sc);
		
		for (int i = 0; i < 10; i++) {
			final ExchangeCfg ec = new ExchangeCfg();
			ec.setServerCfg(sc);
			ec.setExchangeName("ex" + i);
			exchangeCfgs.add(ec);
			
			final QueueCfg qc = new QueueCfg();
			qc.getExchanges().add(ec);
			qc.setQueueName("q" + i);
			qc.setServerCfg(sc);
			queueCfgs.add(qc);
		}
		
		md.startQueues(queueCfgs);
		
		for (final QueueCfg qc : queueCfgs) {
			md.getChannel(qc);
		}
	}
	
	public void publish(final ExchangeCfg ec, final String msgStr) {
		try {
			publisher.basicPublish(ec.getExchangeName(), QueueCfg.DEFAULT_ROUTE_KEY, null, msgStr.getBytes());
//			log.info(msgStr);
		} catch (final Exception e) {
			log.log(Level.SEVERE, "fail to publish " + msgStr + " on " + ec.getExchangeName(), e);
		}
	}
	
	public void publishTestMsgs() {
		for (int i = 0, j = exchangeCfgs.size(); i < j; i++) {
			final ExchangeCfg ec = exchangeCfgs.get(i);
			for (int mi = 0; mi < 100; mi++) {
				publish(ec, ec.getExchangeName() + " -> message_" + mi);
			}
		}
	}
	
	@Test
	public void sharedThreads() throws Exception {
		
		publishTestMsgs();
		
		
		final Map<QueueCfg, ConsumerActor> queueCfgAndConsumerActors = new HashMap<QueueCfg, ThreadsTest.ConsumerActor>();
//		final Map<QueueCfg, DispatchActor> queueCfgAndDispatchActors = new HashMap<QueueCfg, ThreadsTest.DispatchActor>();
		for (final QueueCfg qc : queueCfgs) {
			try {
				queueCfgAndConsumerActors.put(qc, new ConsumerActor(qc));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		final ExecutorService consumerExecutors = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		final ExecutorService dispatchExecutors = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		
		for (int i = 0; i < 1000; i = counter.get()) {
//			log.info("received: " + i);
			for (final QueueCfg qc : queueCfgs) {
				final ConsumerActor task = queueCfgAndConsumerActors.get(qc);
//				try {
//					log.info(String.valueOf(task.call()));
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
				final Future<MessageContext> result = consumerExecutors.submit(task);
//				result.get();
				dispatchExecutors.submit(new DispatchActor(result));
				log.info("task count: " + i);
			}
		}
		
//		consumerExecutors.shutdownNow();
//		dispatchExecutors.shutdownNow();
	}
	
	
	
	@AfterClass
	public static void shutdown() {
		log.info(MiscUtils.invocationInfo());
		try {
			publisher.close();
			publisher.getConnection().close();
		} catch (final IOException e) {
			log.log(Level.SEVERE, "fail to shutdown publisher", e);
		}
		md.shutdown();
	}

	
	
}
