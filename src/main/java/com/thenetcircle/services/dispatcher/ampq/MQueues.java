package com.thenetcircle.services.dispatcher.ampq;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.collections4.map.LinkedMap;
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
import com.thenetcircle.services.dispatcher.IMessageActor;
import com.thenetcircle.services.dispatcher.entity.ExchangeCfg;
import com.thenetcircle.services.dispatcher.entity.MessageContext;
import com.thenetcircle.services.dispatcher.entity.QueueCfg;
import com.thenetcircle.services.dispatcher.entity.ServerCfg;
import com.thenetcircle.services.dispatcher.failsafe.sql.FailedMessageSqlStorage;
import com.thenetcircle.services.dispatcher.http.HttpDispatcherActor;
import com.thenetcircle.services.dispatcher.log.ConsumerLoggers;

//TODO this class becomes rather messy
// maintain configurations, maintain messages, maintain living queues and IMessageActor instances;
public class MQueues {

	private static final int CONN_NUM = MiscUtils.AVAILABLE_PROCESSORS * 3;
	private static final MQueues instance = new MQueues();
	protected static final Log log = LogFactory.getLog(MQueues.class.getName());

	public static MQueues instance() {
		return instance;
	}

	private final ExecutorService connExecutors = Executors.newFixedThreadPool(MiscUtils.AVAILABLE_PROCESSORS);
	private Map<ServerCfg, ConnectionFactory> connFactories = new HashMap<ServerCfg, ConnectionFactory>();

	private Map<ExchangeCfg, Channel> exchangeAndChannels = new HashMap<ExchangeCfg, Channel>();

	private Map<QueueCfg, Channel> queueAndChannels = new HashMap<QueueCfg, Channel>();

	private Map<QueueCfg, ConsumerActor> queueAndConsumers = new HashMap<QueueCfg, ConsumerActor>();

	private Collection<QueueCfg> queueCfgs = new HashSet<QueueCfg>();

	private Map<ServerCfg, LoopingArrayIterator<Connection>> serverAndConns = new HashMap<ServerCfg, LoopingArrayIterator<Connection>>();

//	private Map<ServerCfg, Channel> serverAndRejectChannels = new HashMap<ServerCfg, Channel>();

	public synchronized Channel getChannel(final QueueCfg qc) {
		if (qc == null) {
			return null;
		}

		Channel channel = queueAndChannels.get(qc);
		if (channel == null || !channel.isOpen()) {
			channel = initChannel(qc);
			queueAndChannels.put(qc, channel);
		}
		if (!channel.isOpen()) {
			log.warn("channel is not opened: \n" + qc.getQueueName());
		}
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
		
		final Logger logForSrv = ConsumerLoggers.getLoggerByQueueConf(qc.getServerCfg());
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

	public Collection<QueueCfg> getQueueCfgs() {
		if (queueCfgs == null) {
			queueCfgs = new HashSet<QueueCfg>();
		}
		return queueCfgs;
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
		
		for (IMessageActor actor : actors.keySet()) {
			actor.stop();
		}
	}

	public synchronized void removeQueueCfg(final QueueCfg qc) {
		if (qc == null) {
			return;
		}

		final Channel ch = getChannel(qc);
		if (ch == null || !ch.isOpen()) {
			return;
		}
		
		final Logger logForSrv = ConsumerLoggers.getLoggerByQueueConf(qc.getServerCfg());
		String logStr = "going to remove queue:\n\t" + qc;
		log.info(logStr);
		logForSrv.info(logStr);
		try {
			ch.close();
			
			logStr = "removed queue:\n\t" + qc.getQueueName();
			log.info(logStr);
			logForSrv.info(logStr);
		} catch (IOException e) {
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

	public synchronized void addQueueCfg(final QueueCfg qc) {
		if (qc == null) {
			return;
		}
		
		getQueueCfgs().add(qc);
		initWithQueue(qc);
	}
	
	private synchronized void clearInstances() {
		connExecutors.shutdownNow();
		
		connFactories.clear();
		exchangeAndChannels.clear();
		queueAndChannels.clear();
		queueAndConsumers.clear();
		serverAndConns.clear();
		queueCfgs.clear();
	}

	private Connection getConn(final ServerCfg sc) throws IOException {
		final ConnectionFactory connFactory = getConnFactory(sc);
		final Logger logForSrv = ConsumerLoggers.getLoggerByQueueConf(sc);
		
		
		if (connFactory == null) {
			String logStr = String.format("%s has not been associated with any ServerCfg", sc.toString());
			log.error(logStr);
			logForSrv.error(logStr);
			return null;
		}

		LoopingArrayIterator<Connection> li = serverAndConns.get(sc);
		if (li == null) {
			final Connection[] conns = new Connection[MiscUtils.AVAILABLE_PROCESSORS];
			
			for (int i = 0; i < conns.length; i++) {
				try {
					conns[i] = connFactory.newConnection(getExecutorsForConn());
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
		}
		return li.loop();
	}

	private ExecutorService getExecutorsForConn() {
		return connExecutors;
	}

	private Channel initChannel(final QueueCfg qc) {
		if (qc == null) {
			return null;
		}
		
		final Logger logForSrv = ConsumerLoggers.getLoggerByQueueConf(qc.getServerCfg());

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
			ch = conn.createChannel();
			ch.addShutdownListener(new ShutdownListener() {
				@Override
				public void shutdownCompleted(final ShutdownSignalException cause) {
					log.info("\n\n");
					log.error("channel is closed!", cause);
					log.info("\n\n");
				}
			});

			for (final ExchangeCfg ec : qc.getExchanges()) {
				if (!exchangeAndChannels.containsKey(ec)) {
					ch.exchangeDeclare(ec.getExchangeName(), ec.getType(), ec.isDurable(), ec.isAutoDelete(), null);
					exchangeAndChannels.put(ec, ch);
				}

				ch.queueDeclare(qc.getQueueName(), qc.isDurable(), qc.isExclusive(), qc.isAutoDelete(), null);
				ch.queueBind(qc.getQueueName(), ec.getExchangeName(), qc.getRouteKey());
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

		final Logger logForSrv = ConsumerLoggers.getLoggerByQueueConf(sc);
		
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

	
	public static final Thread cleaner = new Thread() {
		public void run() {
			MQueues.instance.shutdown();
		}
	};

	public void initWithQueueCfgs(final Collection<QueueCfg> queueCfgs2) {
		setQueueCfgs(queueCfgs2);
		for (final QueueCfg qc : queueCfgs) {
			initWithQueue(qc);
		}
	}
	
	public void initWithQueueCfg(final QueueCfg qc) {
		queueCfgs.add(qc);
		initWithQueue(qc);
	}

	private void initWithQueue(final QueueCfg qc) {
		final Logger logForSrv = ConsumerLoggers.getLoggerByQueueConf(qc.getServerCfg());
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
	}

	public MessageContext acknowledge(final MessageContext mc) {
		if (mc == null) {
			return mc;
		}
		
		final long deliveryTag = mc.getDelivery().getEnvelope().getDeliveryTag();
		
		try {
			final Channel ch = getChannel(mc.getQueueCfg());
			if (!ch.isOpen()) {
				log.error("can't acknowledge the message as channel is closed!");
				return mc;
			}
			ch.basicAck(deliveryTag, false);
//			log.info(queueCfg.getQueueName() + " acknowledged message: " + deliveryTag);
		} catch (final IOException e) {
			log.error("failed to acknowledge message: \n" + deliveryTag + "\nresponse: " + mc.getResponse(), e);
		}
		return mc;
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
	
	//this is pointless for now, unless some additional process we need in between
	private final LinkedMap<IMessageActor, IMessageActor> actors = new LinkedMap<IMessageActor, IMessageActor>();
	private void initActors() {
		actors.put(HttpDispatcherActor.instance(), Responder.instance());
		actors.put(Responder.instance(), HttpDispatcherActor.instance());
		actors.put(FailedMessageSqlStorage.instance(), HttpDispatcherActor.instance());
	}
	
	private MQueues() {
		initActors();
	}
	
	public IMessageActor firstActor() {
		final IMessageActor actor = actors.firstKey();
		return actor == null ? IMessageActor.DefaultMessageActor.instance : actor;
	}
	
	public IMessageActor getNextActor(final IMessageActor actor) {
		final IMessageActor nextActor = actors.get(actor);
		return nextActor == null ? IMessageActor.DefaultMessageActor.instance : nextActor;
	}
	
//	public static class MessageActorMgr {
//		
//	}
}
