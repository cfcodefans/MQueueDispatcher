package com.thenetcircle.services.dispatcher.ampq;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.thenetcircle.services.cluster.JGroupsActor;
import com.thenetcircle.services.common.MiscUtils;
import com.thenetcircle.services.dispatcher.dao.QueueCfgDao;
import com.thenetcircle.services.dispatcher.entity.ExchangeCfg;
import com.thenetcircle.services.dispatcher.entity.MessageContext;
import com.thenetcircle.services.dispatcher.entity.QueueCfg;
import com.thenetcircle.services.dispatcher.entity.ServerCfg;
import com.thenetcircle.services.dispatcher.failsafe.sql.FailedMessageSqlStorage;
import com.thenetcircle.services.dispatcher.http.HttpDispatcherActor;
import com.thenetcircle.services.dispatcher.log.ConsumerLoggers;
import com.thenetcircle.services.persistence.jpa.JpaModule;

public class MQueueMgr {

	public static int NUM_CHANNEL_PER_CONN = 3;
	static MQueueMgr instance = new MQueueMgr();
	protected static final Logger log = Logger.getLogger(MQueueMgr.class);
	
	public static MQueueMgr instance() {
		return instance;
	}
	
	private static final void _error(final ServerCfg sc, final String infoStr, final Throwable t) {
//		log.error(infoStr, t);
		final Logger logForSrv = ConsumerLoggers.getLoggerByServerCfg(sc);
		if (logForSrv != null) {
			logForSrv.error(infoStr, t);
		}
	}
	
	private static final void _error(final ServerCfg sc, final String infoStr) {
//		log.error(infoStr);
		final Logger logForSrv = ConsumerLoggers.getLoggerByServerCfg(sc);
		if (logForSrv != null) {
			logForSrv.error(infoStr);
		}
	}
	
	@SuppressWarnings("deprecation")
	static final void _info(final ServerCfg sc, final String infoStr) {
//		log.info(infoStr);
		final Logger logForSrv = ConsumerLoggers.getLoggerByServerCfg(sc);
		if (logForSrv != null) {
			logForSrv.info(infoStr);
		}
	}

	private static final void log(final ServerCfg sc, final Priority priority, final String infoStr) {
//		log.log(priority, infoStr);
		final Logger logForSrv = ConsumerLoggers.getLoggerByServerCfg(sc);
		if (logForSrv != null) {
			logForSrv.log(priority, infoStr);
		}
	}
	
	
	private Map<QueueCfg, QueueCtx> cfgAndCtxs = new HashMap<QueueCfg, QueueCtx>();

	private Map<ServerCfg, ConnectionFactory> connFactories = new HashMap<ServerCfg, ConnectionFactory>();

	ReconnectActor reconnActor = new ReconnectActor();
	
	private ScheduledExecutorService reconnActorThread = Executors.newSingleThreadScheduledExecutor();
	
	private Map<ServerCfg, Set<NamedConnection>> serverCfgAndConns = new HashMap<ServerCfg, Set<NamedConnection>>();
	public static final Thread cleaner = new Thread() {
		public void run() {
			log.info("system shutdown!");
			MQueueMgr.instance().shutdown();
		}
	};
	

	public MQueueMgr() {
		initActors();
	}
	
	public synchronized ConnectionFactory getConnFactory(final ServerCfg sc) {
		if (sc == null) {
			return null;
		}
	
		ConnectionFactory connFactory = connFactories.get(sc);
		if (connFactory == null) {
			connFactory = initConnFactory(sc);
			if (connFactory == null) {
				log.error("fail to create ConnectionFactory: \n\t" + sc);
				_error(sc, "fail to create ConnectionFactory: \n\t" + sc);
				return null;
			}
			connFactories.put(sc, connFactory);
		}
	
		return connFactory;
	}

	
	public QueueCfg startQueue(final QueueCfg qc) {
		if (cfgAndCtxs.containsKey(qc)) {
			stopQueue(qc);
		}

		final ServerCfg sc = qc.getServerCfg();
		Set<NamedConnection> connSet = serverCfgAndConns.get(sc);
		if (connSet == null) {
			connSet = new LinkedHashSet<NamedConnection>();
			serverCfgAndConns.put(sc, connSet);
		}

		NamedConnection nc = null;
		for (final NamedConnection _nc : connSet) {
			if (_nc.qcSet.size() < NUM_CHANNEL_PER_CONN) {
				nc = _nc;
				break;
			}
		}

		try {
			if (nc == null) {
				nc = new NamedConnection();
				nc.name = String.format("conn_%s_%s_%s", sc.getHost(), sc.getUserName(), UUID.randomUUID());
				nc.conn = getConnFactory(sc).newConnection(Executors.newSingleThreadExecutor());
				nc.conn.addShutdownListener(nc);
				connSet.add(nc);
			}

			{
				final QueueCtx queueCtx = new QueueCtx();

				final Channel ch = nc.conn.createChannel();
				
				ch.addShutdownListener(queueCtx);

				for (final ExchangeCfg ec : qc.getExchanges()) {
					// if (!exchangeAndChannels.containsKey(ec)) {
					ch.exchangeDeclare(ec.getExchangeName(), ec.getType(), ec.isDurable(), ec.isAutoDelete(), null);
					// exchangeAndChannels.put(ec, ch);
					// }

					ch.queueDeclare(qc.getQueueName(), qc.isDurable(), qc.isExclusive(), qc.isAutoDelete(), null);
					ch.queueBind(qc.getQueueName(), StringUtils.defaultIfBlank(ec.getExchangeName(), StringUtils.EMPTY), qc.getRouteKey());
					
					if (qc.getPrefetchSize() > 0) {
						ch.basicQos(qc.getPrefetchSize());
					}
				}
				final ConsumerActor ca = new ConsumerActor(ch, qc);
				ch.basicConsume(qc.getQueueName(), false, ca);

				queueCtx.ca = ca;
				queueCtx.qc = qc;
				queueCtx.ch = ch;

				queueCtx.nc = nc;
				nc.qcSet.add(qc);
				
				qc.setEnabled(true);
				
				final QueueCfgDao qcDao = new QueueCfgDao(JpaModule.getEntityManager());
				qcDao.update(qc);
				
				cfgAndCtxs.put(qc, queueCtx);
			}
		} catch (Exception e) {
			final String infoStr = "failed to start queue: \n\t" + qc;
			log.error(infoStr, e);
			_error(sc, infoStr, e);
		}

		return qc;
	}
	
	public QueueCfg stopQueue(final QueueCfg qc) {
		if (qc == null || !cfgAndCtxs.containsKey(qc)) {
			return qc;
		}
		
		reconnActor.stopReconnect(qc);
		
		final QueueCtx queueCtx  = cfgAndCtxs.get(qc);
		final Channel ch = queueCtx.ch;

		_info(qc.getServerCfg(), "going to remove queue:\n\t" + qc);

		try {
			if (ch != null && ch.isOpen()) {
				ch.close(AMQP.CONNECTION_FORCED, "OK");
			}

			_info(qc.getServerCfg(), "removed queue:\n\t" + qc.getQueueName());
			
			final NamedConnection nc = queueCtx.nc;
			if (nc == null) {
				qc.setEnabled(false);
				cfgAndCtxs.remove(qc);
				return qc;
			}
			
			queueCtx.nc = null;
			nc.qcSet.remove(qc);
			
			cleanNamedConnection(nc);
			
			qc.setEnabled(false);
			cfgAndCtxs.remove(qc);
		} catch (final Exception e) {
			final String infoStr = "failed to shut down Queue: \n" + qc.getQueueName();
			log.error(infoStr, e);
			_error(qc.getServerCfg(), infoStr, e);
		} 
		
		return qc;
	}
	
	private void cleanNamedConnection(final NamedConnection nc) throws IOException {
		if (!CollectionUtils.isEmpty(nc.qcSet)) {
			return;
		}
		
		if (nc.conn.isOpen()) {
			nc.conn.close(AMQP.CONNECTION_FORCED, "OK");
		}
		final Set<NamedConnection> conns = serverCfgAndConns.get(nc.sc);
		if (conns != null) {
			if (conns.contains(nc)) {
				conns.remove(nc);
			}
			if (conns.isEmpty()) {
				serverCfgAndConns.remove(nc.sc);
			}
		}
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

	public synchronized void shutdown() {
		log.info(MiscUtils.invocationInfo());
		shutdownActors();
	}

	private void initActors() {
		reconnActorThread.scheduleAtFixedRate(reconnActor, 30, 30, TimeUnit.SECONDS);
		Responder.instance();
		HttpDispatcherActor.instance();
		FailedMessageSqlStorage.instance();
	}

	private void shutdownActors() {
		reconnActorThread.shutdownNow();
		Responder.stopAll();
		HttpDispatcherActor.instance().shutdown();
		FailedMessageSqlStorage.instance().stop();
	}

	public MessageContext acknowledge(final MessageContext mc) {
		if (mc == null || mc.getDelivery() == null) {
			return mc;
		}

		final long deliveryTag = mc.getDelivery().getEnvelope().getDeliveryTag();
		final QueueCfg qc = mc.getQueueCfg();

		try {
			final QueueCtx queueCtx = cfgAndCtxs.get(qc);
			if (queueCtx == null) {
				final String infoStr = "can't acknowledge the message as channel is not created!\n\t" + qc;
				log.error(infoStr);
				_error(qc.getServerCfg(), infoStr);
				return mc;
			}
			
			final Channel ch = queueCtx.ch;
			if (!ch.isOpen()) {
				final String infoStr = "can't acknowledge the message as channel is closed!\n\t" + qc;
				log.error(infoStr);
				_error(qc.getServerCfg(), infoStr);
				return mc;
			}
			ch.basicAck(deliveryTag, false);
			// MsgMonitor.prefLog(mc, log);
		} catch (final IOException e) {
			final String infoStr = "failed to acknowledge message: \n" + deliveryTag + "\nresponse: " + mc.getResponse();
			log.error(infoStr, e);
			_error(qc.getServerCfg(), infoStr, e);
		}

		_info(qc.getServerCfg(), "the result of job for q " + qc.getName() + " on server " + qc.getServerCfg().getVirtualHost() + "\nresponse: " + mc.getResponse());

		return mc;
	}
	
	public Channel getChannel(final QueueCfg qc) {
		final QueueCtx queueCtx = cfgAndCtxs.get(qc);
		return queueCtx != null ? queueCtx.ch : null;
	}

	public void updateQueueCfg(final QueueCfg qc) {
		startQueue(qc);
	}

	public void startQueues(final List<QueueCfg> qcList) {
		for (final QueueCfg qc : qcList) {
			startQueue(qc);
		}
	}
	
	public boolean isQueueRunning(final QueueCfg qc) {
		final QueueCtx queueCtx = cfgAndCtxs.get(qc);
		return queueCtx != null && queueCtx.ch != null && queueCtx.ch.isOpen();
	}
	
	public boolean isInReconnectSet(final QueueCfg qc) {
		return reconnActor.isInReconnectSet(qc);
	}

	public Collection<QueueCfg> getQueueCfgs() {
		return cfgAndCtxs.keySet();
	}

	public void updateServerCfg(final ServerCfg edited) {
		if (edited == null) {
			return;
		}
		
		final List<QueueCfg> qcs = new ArrayList<QueueCfg>();
		
		for (final QueueCfg qc : cfgAndCtxs.keySet()) {
			if (edited.equals(qc.getServerCfg())) {
				qcs.add(qc);
			}
		}
		
		for (final QueueCfg qc : qcs) {
			updateQueueCfg(qc);
		}
		
		JGroupsActor.instance().restartQueues(qcs.toArray(new QueueCfg[0]));
	}
}
