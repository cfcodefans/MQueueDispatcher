package com.thenetcircle.services.dispatcher.ampq;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.collections4.iterators.LoopingListIterator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.QueueingConsumer;
import com.thenetcircle.services.common.MiscUtils;
import com.thenetcircle.services.common.MiscUtils.LoopingArrayIterator;

public class MQueues {

	private static final int CONN_NUM = MiscUtils.AVAILABLE_PROCESSORS * 3;
	private static final MQueues instance = new MQueues();
	protected static final Log log = LogFactory.getLog(MQueues.class.getSimpleName());

	public static MQueues getInstance() {
		return instance;
	}

	private final ExecutorService connExecutors = Executors.newFixedThreadPool(MiscUtils.AVAILABLE_PROCESSORS);
	private Map<ServerCfg, ConnectionFactory> connFactories = new HashMap<ServerCfg, ConnectionFactory>();

	private Map<ExchangeCfg, Channel> exchangeAndChannels = new HashMap<ExchangeCfg, Channel>();

	private Map<QueueCfg, Channel> queueAndChannels = new HashMap<QueueCfg, Channel>();

	private Map<QueueCfg, Consumer> queueAndConsumers = new HashMap<QueueCfg, Consumer>();

	private Collection<QueueCfg> queueCfgs;

	private Map<ServerCfg, LoopingArrayIterator<Connection>> serverAndConns = new HashMap<ServerCfg, LoopingArrayIterator<Connection>>();

//	private Map<ServerCfg, Channel> serverAndRejectChannels = new HashMap<ServerCfg, Channel>();

	public synchronized Channel getChannel(final QueueCfg qc) {
		if (qc == null) {
			return null;
		}

		Channel channel = queueAndChannels.get(qc);
		if (channel == null) {
			channel = initChannel(qc);
			queueAndChannels.put(qc, channel);
		}
		if (!channel.isOpen()) {
			log.warn("channel is not opened: \n" + qc);
		}
		return channel;
	}

	public synchronized ConnectionFactory getConnFactory(final ServerCfg sc) {
		if (sc == null)
			return null;

		ConnectionFactory connFactory = connFactories.get(sc);
		if (connFactory == null) {
			connFactory = initConnFactory(sc);
			connFactories.put(sc, connFactory);
		}

		return connFactory;
	}

	public synchronized Consumer getConsumer(final QueueCfg qc) {
		Consumer c = queueAndConsumers.get(qc);
		if (c == null) {
			final Channel ch = getChannel(qc);
			c = new ConsumerActor(qc);
			try {
				ch.basicConsume(qc.getQueueName(), false, c);
			} catch (IOException e) {
				log.error("failed to initiate consumer for queue: \n" + qc, e);
			}
			queueAndConsumers.put(qc, c);
		}
		return c;
	}

	public Collection<QueueCfg> getQueueCfgs() {
		return queueCfgs;
	}

	public void setQueueCfgs(Collection<QueueCfg> queueCfgs) {
		this.queueCfgs = queueCfgs;
	}

	public synchronized void shutdown() {
		final Set<Connection> conns = new HashSet<Connection>();
		for (final Channel ch : queueAndChannels.values()) {
			conns.add(ch.getConnection());
			try {
				ch.close();
			} catch (IOException e) {
				log.error("failed to close channel: " + ch.getChannelNumber(), e);
			}
		}

		for (final Connection conn : conns) {
			try {
				conn.close();
			} catch (Exception e) {
				log.error("failed to close connection: " + conn, e);
			}
		}
		
		clearInstances();
	}

	public synchronized void shutdown(final QueueCfg qc) {
		if (qc == null) {
			return;
		}
		
		final Channel ch = getChannel(qc);
		if (ch == null || !ch.isOpen()) {
			return;
		}
		
		try {
			ch.close();
		} catch (IOException e) {
			log.error("failed to shut down Queue: \n" + qc.getQueueName(), e);
		}
	}

	private synchronized void clearInstances() {
		connExecutors.shutdownNow();
		
		connFactories.clear();
		exchangeAndChannels.clear();
		queueAndChannels.clear();
		queueAndConsumers.clear();
		queueAndConsumers.clear();
		serverAndConns.clear();
//		serverAndRejectChannels.clear();
	}

	private Connection getConn(final ServerCfg sc) throws IOException {
		final ConnectionFactory connFactory = getConnFactory(sc);
		if (connFactory == null) {
			log.error(String.format("%s has not been associated with any ServerCfg", sc.toString()));
			return null;
		}

		LoopingArrayIterator<Connection> li = serverAndConns.get(sc);
		if (li == null) {
			final Connection[] conns = new Connection[MiscUtils.AVAILABLE_PROCESSORS];
			for (int i = 0; i < conns.length; i++) {
				conns[i] = connFactory.newConnection(getExecutorsForConn());
			}
			li = new LoopingArrayIterator<Connection>(conns);
			serverAndConns.put(sc, li);
		}
		return li.loop();
	}

	private ExecutorService getExecutorsForConn() {
		return connExecutors;
	}

//	public Channel getRejecter(final QueueCfg qc) {
//		Channel rejecterChannel = serverAndRejectChannels.get(qc);
//		if (rejecterChannel == null || !rejecterChannel.isOpen()) {
//			Connection conn;
//			try {
//				conn = getConn(qc.getServerCfg());
//				rejecterChannel = conn.createChannel();
//			} catch (IOException e) {
//				log.error("fail to create rejecter channel for Server: " + qc.getServerCfg(), e);
//				return null;
//			}
//			serverAndRejectChannels.put(qc.getServerCfg(), rejecterChannel);
//		}
//
//		return rejecterChannel;
//	}

	private Channel initChannel(final QueueCfg qc) {
		if (qc == null)
			return null;

		log.info(String.format("initiating Channel with QueueCfg %d: \n%s", qc.hashCode(), qc.toString()));
		Channel ch = null;
		try {
			final Connection conn = getConn(qc.getServerCfg());
			if (conn == null) {
				log.error("failed to get connection with QueueCfg: " + qc);
				return ch;
			}
			ch = conn.createChannel();

			// ch.exchangeDeclare(exchange, type, durable, autoDelete,
			// arguments);
			// ch.queueDeclare(queue, durable, exclusive, autoDelete, arguments)
			// ch.queueBind(queue, exchange, routingKey)

			for (final ExchangeCfg ec : qc.getExchanges()) {
				if (!exchangeAndChannels.containsKey(ec)) {
					ch.exchangeDeclare(ec.getExchangeName(), ec.getType(), ec.isDurable(), ec.isAutoDelete(), null);
					exchangeAndChannels.put(ec, ch);
				}

				ch.queueDeclare(qc.getQueueName(), qc.isDurable(), qc.isExclusive(), qc.isAutoDelete(), null);
				ch.queueBind(qc.getQueueName(), ec.getExchangeName(), qc.getRouteKey());
			}

			log.info(String.format("QueueCfg is initiated\n\t%s", qc.toString()));
		} catch (IOException e) {
			log.error(String.format("failed to initiate Connection for ServerCfg: \n%s", qc.getServerCfg().toString()), e);
		}

		return ch;
	}

	private synchronized ConnectionFactory initConnFactory(final ServerCfg sc) {
		if (sc == null)
			return null;

		log.info(String.format("initiating ConnectionFactory with ServerCfg: \n%s", sc.toString()));

		final ConnectionFactory cf = new ConnectionFactory();
		cf.setHost(sc.getHost());
		cf.setVirtualHost(sc.getVirtualHost());
		cf.setPort(sc.getPort());
		cf.setUsername(sc.getUserName());
		cf.setPassword(sc.getPassword());

		log.info(String.format("ConnectionFactory is instantiated with \n%s", sc.toString()));

		return cf;
	}

	
	public static final Thread cleaner = new Thread() {
		public void run() {
			MQueues.instance.shutdown();
		}
	};

	public void initWithQueueCfgs(final Collection<QueueCfg> queueCfgs2) {
		setQueueCfgs(queueCfgs2);
		for (final QueueCfg qc : queueCfgs) {
			if (getConnFactory(qc.getServerCfg()) == null) {
				log.error("failed to create ConnectionFactory for ServerCfg: \n" + qc.getServerCfg());
			}
			if (getChannel(qc) == null) {
				log.error("failed to create Channel for QueueCfg: \n" + qc);
			}
			if (getConsumer(qc) == null) {
				log.error("failed to create ConsumerActor for QueueCfg: \n" + qc);
			}
		}
	}
}
