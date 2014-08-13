package com.thenetcircle.services.dispatcher.ampq;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Logger;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;
import com.thenetcircle.services.common.MiscUtils;
import com.thenetcircle.services.common.MiscUtils.LoopingArrayIterator;
import com.thenetcircle.services.dispatcher.entity.ExchangeCfg;
import com.thenetcircle.services.dispatcher.entity.MessageContext;
import com.thenetcircle.services.dispatcher.entity.QueueCfg;
import com.thenetcircle.services.dispatcher.entity.ServerCfg;
import com.thenetcircle.services.dispatcher.failsafe.sql.FailedMessageSqlStorage;
import com.thenetcircle.services.dispatcher.http.HttpDispatcherActor;
import com.thenetcircle.services.dispatcher.log.ConsumerLoggers;
import com.thenetcircle.services.dispatcher.mgr.Monitor;

//TODO this class becomes rather messy
// maintain configurations, maintain messages, maintain living queues and IMessageActor instances;
public class MQueues {
	
	private static final int CONN_NUM = MiscUtils.AVAILABLE_PROCESSORS * 2;

	public static final Thread cleaner = new Thread() {
		public void run() {
			log.info("system shutdown!");
			MQueues.instance.shutdown();
		}
	};
	// private static final int CONN_NUM = MiscUtils.AVAILABLE_PROCESSORS * 3;
	private static final MQueues instance = new MQueues();

	protected static final Log log = LogFactory.getLog(MQueues.class.getName());

	public static MQueues instance() {
		return instance;
	}

	// this is pointless for now, unless some additional process we need in
	// between
//	private final LinkedMap<IMessageActor, IMessageActor> actors = new LinkedMap<IMessageActor, IMessageActor>();

//	private static LoopingArrayIterator<ExecutorService> connExecutors = null;// Executors.newCachedThreadPool(); //.newFixedThreadPool(CONN_NUM);
//	static {
//		final List<ExecutorService> list = new ArrayList<ExecutorService>();
//		for (int i = 0, j = CONN_NUM; i < j; i++) {
//			list.add(Executors.newSingleThreadExecutor());
//		}
//		connExecutors = new LoopingArrayIterator<ExecutorService>(list.toArray(new ExecutorService[0]));
//	}

	private Map<ServerCfg, ConnectionFactory> connFactories = new HashMap<ServerCfg, ConnectionFactory>();

	private Map<ExchangeCfg, Channel> exchangeAndChannels = new HashMap<ExchangeCfg, Channel>();

	private Map<QueueCfg, Channel> queueAndChannels = new ConcurrentHashMap<QueueCfg, Channel>();

	private Map<QueueCfg, ConsumerActor> queueAndConsumers = new HashMap<QueueCfg, ConsumerActor>();

	private Collection<QueueCfg> queueCfgs = new LinkedHashSet<QueueCfg>();

	private Map<ServerCfg, LoopingArrayIterator<Connection>> serverAndConns = new HashMap<ServerCfg, LoopingArrayIterator<Connection>>();

	private MQueues() {
		initActors();
	}

	public MessageContext acknowledge(final MessageContext mc) {
		if (mc == null) {
			return mc;
		}

		final long deliveryTag = mc.getDelivery().getEnvelope().getDeliveryTag();
		final QueueCfg qc = mc.getQueueCfg();

		try {
			final Channel ch = getChannel(qc);
//			if (!ch.isOpen()) {
//				log.error("can't acknowledge the message as channel is closed!");
//				return mc;
//			}
			ch.basicAck(deliveryTag, false);
			Monitor.prefLog(mc, log);
		} catch (final IOException e) {
			log.error("failed to acknowledge message: \n" + deliveryTag + "\nresponse: " + mc.getResponse(), e);
		}

		final ServerCfg serverCfg = qc.getServerCfg();
		final Logger logForSrv = ConsumerLoggers.getLoggerByServerCfg(serverCfg);
//		String logStr = "cfg_name: \n\t" + qc.getName() + "\n posted message: \n\t" + new String(ArrayUtils.subarray(mc.getMessageBody(), 0, 50)) + "\n to url: " + qc.getDestCfg().getUrl();
		logForSrv.info("the result of job for q " + qc.getName() + " on server " + serverCfg.getVirtualHost() + "\nresponse: " + mc.getResponse());

//		logForSrv.info(logStr);
		return mc;
	}

	public synchronized void addQueueCfg(final QueueCfg qc) {
		if (qc == null) {
			return;
		}

		getQueueCfgs().add(qc);
		creatQueue(qc);
	}

//	public IMessageActor firstActor() {
//		final IMessageActor actor = actors.firstKey();
//		return actor == null ? IMessageActor.DefaultMessageActor.instance : actor;
//	}

	public Channel getChannel(final QueueCfg qc) {
		if (qc == null) {
			return null;
		}

		Channel channel = queueAndChannels.get(qc);
		if (channel == null || !channel.isOpen()) {
			channel = initChannel(qc);
			queueAndChannels.put(qc, channel);
		}
//		if (!channel.isOpen()) {
//			log.warn("channel is not opened: \n" + qc.getQueueName());
//		}
		return channel;
	}

	public synchronized ConnectionFactory getConnFactory(final ServerCfg sc) {
		if (sc == null) {
			return null;
		}

		ConnectionFactory connFactory = connFactories.get(sc);
		if (connFactory == null) {
			connFactory = initConnFactory(sc);
			connFactories.put(sc, connFactory);
		}

		return connFactory;
	}

	public ConsumerActor getConsumer(final QueueCfg qc) {
		ConsumerActor c = queueAndConsumers.get(qc);
		if (c != null) {
			return c;
		}

		final Logger logForSrv = ConsumerLoggers.getLoggerByServerCfg(qc.getServerCfg());
		String logStr = null;

		final Channel ch = getChannel(qc);
		c = new ConsumerActor(qc);
		try {
			ch.basicConsume(qc.getQueueName(), false, c);
			queueAndConsumers.put(qc, c);

			logStr = "created consumer for queue: \n\t" + qc;
			logForSrv.info(logStr);
			log.info(logStr);
		} catch (IOException e) {
			logStr = "failed to initiate consumer for queue: \n\t" + qc;
			log.error(logStr, e);
			logForSrv.error(logStr, e);
		}

		return c;
	}

//	public IMessageActor getNextActor(final IMessageActor actor) {
//		final IMessageActor nextActor = actors.get(actor);
//		return nextActor == null ? IMessageActor.DefaultMessageActor.instance : nextActor;
//	}

	public Collection<QueueCfg> getQueueCfgs() {
		if (queueCfgs == null) {
			queueCfgs = new HashSet<QueueCfg>();
		}
		return queueCfgs;
	}

	public void updateQueueCfg(final QueueCfg qc) {
		if (queueCfgs.contains(qc)) {
			removeQueueCfg(qc);
		}
		queueCfgs.add(qc);
		creatQueue(qc);
	}

	public void initWithQueueCfgs(final List<QueueCfg> queueCfgs2) {
		setQueueCfgs(queueCfgs2);

		for (final QueueCfg qc : queueCfgs2) {
			creatQueue(qc);
		}

		log.info(MiscUtils.invocationInfo());
	}

	public MessageContext reject(final MessageContext mc, final boolean requeue) {
		if (mc == null) {
			return mc;
		}
		try {
			final Channel ch = getChannel(mc.getQueueCfg());
			if (!ch.isOpen()) {
				log.error("can't reject the message as channel is closed!");
				return mc;
			}
			ch.basicReject(mc.getDelivery().getEnvelope().getDeliveryTag(), requeue);
		} catch (final IOException e) {
			log.error("failed to reject message: \n" + new String(mc.getMessageBody()) + "\nresponse: " + mc.getResponse(), e);
		}
		return mc;
	}

	public synchronized void removeQueueCfg(final QueueCfg qc) {
		if (qc == null) {
			return;
		}

		final Channel ch = getChannel(qc);
		if (ch == null || !ch.isOpen()) {
			return;
		}

		final Logger logForSrv = ConsumerLoggers.getLoggerByServerCfg(qc.getServerCfg());
		String logStr = "going to remove queue:\n\t" + qc;
		log.info(logStr);
		logForSrv.info(logStr);
		try {
			ch.close();

			logStr = "removed queue:\n\t" + qc.getQueueName();
			log.info(logStr);
			logForSrv.info(logStr);
		} catch (Exception e) {
			logStr = "failed to shut down Queue: \n" + qc.getQueueName();
			log.error(logStr, e);
			logForSrv.error(logStr, e);
		} finally {
			for (final ExchangeCfg ec : qc.getExchanges()) {
				exchangeAndChannels.remove(ec);
			}
			queueCfgs.remove(qc);
			queueAndChannels.remove(qc);
			queueAndConsumers.remove(qc);
		}
	}

	public void setQueueCfgs(Collection<QueueCfg> queueCfgs) {
		this.queueCfgs.clear();
		this.queueCfgs.addAll(queueCfgs);
	}

	public synchronized void shutdown() {
		log.info(MiscUtils.invocationInfo());

		final Set<Connection> conns = new HashSet<Connection>();
		for (final Channel ch : queueAndChannels.values()) {
			conns.add(ch.getConnection());
			try {
				if (ch.isOpen()) {
					ch.close();
				}
			} catch (Exception e) {
				log.error("failed to close channel: " + ch.getChannelNumber() + "\n\t" + e.getMessage());
			}
		}

		for (final Connection conn : conns) {
			try {
				if (conn.isOpen()) {
					conn.close();
				}
			} catch (Exception e) {
				log.error("failed to close connection: " + conn + "\n\t" + e.getMessage());
			}
		}

		clearInstances();

		shutdownActors();
		
//		for (IMessageActor actor : actors.keySet()) {
//			actor.stop();
//		}
	}

	private synchronized void clearInstances() {
//		for (ExecutorService es : connExecutors.getArray()) {
//			es.shutdownNow();
//		}

		connFactories.clear();
		exchangeAndChannels.clear();
		queueAndChannels.clear();
		queueAndConsumers.clear();
		serverAndConns.clear();
		queueCfgs.clear();
	}

	private void creatQueue(final QueueCfg qc) {
		final Logger logForSrv = ConsumerLoggers.getLoggerByServerCfg(qc.getServerCfg());
		try {
			if (getConnFactory(qc.getServerCfg()) == null) {
				final String errMsgStr = "failed to create ConnectionFactory for ServerCfg: \n" + qc.getServerCfg();
				log.error(errMsgStr);
				logForSrv.error(errMsgStr);
			}

			if (getChannel(qc) == null) {
				final String errMsgStr = "failed to create Channel for QueueCfg: \n" + qc;
				log.error(errMsgStr);
				logForSrv.error(errMsgStr);
			}

			if (getConsumer(qc) == null) {
				final String errMsgStr = "failed to create ConsumerActor for QueueCfg: \n" + qc;
				log.error(errMsgStr);
				logForSrv.error(errMsgStr);
			}

			log.info(String.format("%d: %s is created", qc.getId(), qc.getQueueName()));
		} catch (Exception e) {
			log.error("failed to load queues", e);
		}

	}

	private Connection getConn(final ServerCfg sc) throws IOException {
		final ConnectionFactory connFactory = getConnFactory(sc);
		final Logger logForSrv = ConsumerLoggers.getLoggerByServerCfg(sc);

		if (connFactory == null) {
			String logStr = String.format("%s has not been associated with any ServerCfg", sc.toString());
			log.error(logStr);
			logForSrv.error(logStr);
			return null;
		}

		LoopingArrayIterator<Connection> li = serverAndConns.get(sc);
		if (li != null) {
			return li.loop();
		}
			
		int queueNum = 0;
		for (final QueueCfg qc : this.queueCfgs) {
			if (sc.equals(qc.getServerCfg())) {
				queueNum ++;
			}
		}
		final int connSize = Math.max((int) Math.ceil(((double)queueNum / (double)this.queueCfgs.size()) * 200.0) + 1, 4);
		
		log.info(String.format("create %d connection to server: %s\t%s", connSize, sc.getHost(), sc.getVirtualHost()));
		final Connection[] conns = new Connection[connSize];

		for (int i = 0; i < conns.length; i++) {
			try {
				final ExecutorService es = Executors.newSingleThreadExecutor(MiscUtils.namedThreadFactory("Connection.ConsumerWorkService"));
//						Executors.newFixedThreadPool(Math.max(1, (int)Math.ceil(queueNum / connSize)), 
//																		MiscUtils.namedThreadFactory("Connection.ConsumerWorkService"));
				conns[i] = connFactory.newConnection(es);
			} catch (Exception e) {
				String logStr = String.format("failed to create connection for server: \n\t%s\n", sc.toString());
				log.error(logStr, e);
				logForSrv.error(logStr, e);
			}
		}

		li = new LoopingArrayIterator<Connection>(conns);
		serverAndConns.put(sc, li);

		String logStr = String.format("connection created for server: \n\t%s\n", sc.toString());
		log.info(logStr);
		logForSrv.info(logStr);
		
		return li.loop();
	}

//	private ExecutorService getExecutorsForConn() {
//		return connExecutors.loop();
//	}

	private void initActors() {
		Responder.instance();
		HttpDispatcherActor.instance();
		FailedMessageSqlStorage.instance();
	}

	private void shutdownActors() {
		Responder.stopAll();
		HttpDispatcherActor.instance().shutdown();
		FailedMessageSqlStorage.instance().stop();
	}
	
	private Channel initChannel(final QueueCfg qc) {
		if (qc == null) {
			return null;
		}

		final Logger logForSrv = ConsumerLoggers.getLoggerByServerCfg(qc.getServerCfg());

		String logStr = String.format("initiating Channel with QueueCfg %d: \n%s\n", qc.hashCode(), qc.toString());
		log.info(logStr);
		logForSrv.info(logStr);

		Channel ch = null;
		try {
			final Connection conn = getConn(qc.getServerCfg());
			if (conn == null) {
				logStr = "failed to get connection with QueueCfg: " + qc;
				log.error(logStr);
				logForSrv.info(logStr);

				return ch;
			}
			
			log.info(String.format("create channel [%s] with connection: %s", qc.getName(), conn.toString()));
			
			ch = conn.createChannel();
			ch.addShutdownListener(new ShutdownListener() {
				@Override
				public void shutdownCompleted(final ShutdownSignalException cause) {
					log.info("\n\n");
					log.error("channel is closed! \n\t" + cause.getMessage());
					log.info("\n\n");
				}
			});

			for (final ExchangeCfg ec : qc.getExchanges()) {
				if (!exchangeAndChannels.containsKey(ec)) {
					ch.exchangeDeclare(ec.getExchangeName(), ec.getType(), ec.isDurable(), ec.isAutoDelete(), null);
					exchangeAndChannels.put(ec, ch);
				}

				ch.queueDeclare(qc.getQueueName(), qc.isDurable(), qc.isExclusive(), qc.isAutoDelete(), null);
				ch.queueBind(qc.getQueueName(), StringUtils.defaultIfBlank(ec.getExchangeName(), StringUtils.EMPTY), qc.getRouteKey());
			}

			logStr = String.format("QueueCfg is initiated\n\t%s", qc.getQueueName());
			log.info(logStr);
			logForSrv.info(logStr);
		} catch (IOException e) {
			logStr = String.format("failed to initiate Connection for QueueCfg: \n%s", qc.getQueueName());
			log.error(logStr, e);
			logForSrv.error(logStr, e);
		}

		return ch;
	}

	private synchronized ConnectionFactory initConnFactory(final ServerCfg sc) {
		if (sc == null)
			return null;

		final Logger logForSrv = ConsumerLoggers.getLoggerByServerCfg(sc);

		String infoStr = String.format("initiating ConnectionFactory with ServerCfg: \n%s", sc.toString());
		log.info(infoStr);
		logForSrv.info(infoStr);

		final ConnectionFactory cf = new ConnectionFactory();
		cf.setHost(sc.getHost());
		cf.setVirtualHost(sc.getVirtualHost());
		cf.setPort(sc.getPort());
		cf.setUsername(sc.getUserName());
		cf.setPassword(sc.getPassword());

		infoStr = String.format("ConnectionFactory is instantiated with \n%s", sc.toString());
		log.info(infoStr);
		logForSrv.info(infoStr);

		return cf;
	}

}
