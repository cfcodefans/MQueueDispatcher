package com.thenetcircle.services.dispatcher.ampq;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.thenetcircle.services.dispatcher.entity.QueueCfg;
import com.thenetcircle.services.dispatcher.entity.ServerCfg;
import com.thenetcircle.services.dispatcher.log.ConsumerLoggers;

public class MQueueMgr {

	public static int NUM_CHANNEL_PER_CONN = 3;
	
	private static class QueueCtx {
		public QueueCfg qc;
		public Channel ch;
		public ConsumerActor ca;
		
		@Override
		public int hashCode() {
			return ObjectUtils.hashCode(qc);
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof QueueCtx))
				return false;
			QueueCtx other = (QueueCtx) obj;
			if (qc == null) {
				if (other.qc != null)
					return false;
			} else if (!qc.equals(other.qc))
				return false;
			return true;
		}
	}

//	private static class ConnCtx {
////		public String key;
//		public ServerCfg sc;
//		public Map<String, Connection> keyAndConns = new HashMap<String, Connection>();
//		public Map<QueueCfg, String> qcAndConns = new HashMap<QueueCfg, String>();
//		
//	}
	
	private static class NamedConnection {
		public String name;
		public Connection conn;
		public ServerCfg sc;
		
		public Set<QueueCfg> qcSet = new HashSet<QueueCfg>();
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			result = prime * result + ((sc == null) ? 0 : sc.hashCode());
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof NamedConnection))
				return false;
			NamedConnection other = (NamedConnection) obj;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			if (sc == null) {
				if (other.sc != null)
					return false;
			} else if (!sc.equals(other.sc))
				return false;
			return true;
		}
		
		public static NamedConnection connWithFewestChannels(final Collection<NamedConnection> connSet) {
			if (CollectionUtils.isEmpty(connSet)) {
				return null;
			}
			
			return Collections.min(connSet, new Comparator<NamedConnection>() {
				@Override
				public int compare(NamedConnection o1, NamedConnection o2) {
					Integer s1 = (o1 != null && o1.qcSet != null) ? o1.qcSet.size() : Integer.MAX_VALUE;
					Integer s2 = (o2 != null && o2.qcSet != null) ? o2.qcSet.size() : Integer.MAX_VALUE;
					return s1.compareTo(s2);
				}
			});
		}
	}

	private Map<QueueCfg, QueueCtx> cfgAndCtxs = new HashMap<QueueCfg, QueueCtx>();
	private Map<QueueCfg, NamedConnection> cfgAndConns = new HashMap<QueueCfg, NamedConnection>();
	
	private Map<ServerCfg, Set<NamedConnection>> serverCfgAndConns = new HashMap<ServerCfg, Set<NamedConnection>>();
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
	
	protected static final Logger log = Logger.getLogger(MQueueMgr.class);
	
	public QueueCfg startQueue(final QueueCfg qc) {
		if (cfgAndCtxs.containsKey(qc)) {
			stopQueue(qc);
		}
		
		final ServerCfg sc = qc.getServerCfg();
		final Set<NamedConnection> connSet = serverCfgAndConns.get(sc);
		if (connSet == null) {
			serverCfgAndConns.put(sc, new LinkedHashSet<NamedConnection>());
			
			final NamedConnection nc = new NamedConnection();
//			nc.conn = getConnFactory(sc).newConnection(executor)
		}
		
		
		return null;
	}
	
	private QueueCfg stopQueue(final QueueCfg qc) {
		if (qc == null || !cfgAndCtxs.containsKey(qc)) {
			return qc;
		}
		
		final QueueCtx _qc  = cfgAndCtxs.get(qc);
		final Channel ch = _qc.ch;
		

		_info(qc.getServerCfg(), "going to remove queue:\n\t" + qc);

		try {
			if (ch == null || !ch.isOpen()) {
				return qc;
			}
			ch.close(AMQP.CONNECTION_FORCED, "OK");

			_info(qc.getServerCfg(), "removed queue:\n\t" + qc.getQueueName());
			
			final NamedConnection nc = cfgAndConns.remove(qc);
			if (nc == null) {
				return qc;
			}
			
			nc.qcSet.remove(qc);
			if (!cfgAndConns.values().contains(nc) && CollectionUtils.isEmpty(nc.qcSet)) {
				nc.conn.close(AMQP.CONNECTION_FORCED, "OK");
			}
			
			final Set<NamedConnection> conns = serverCfgAndConns.get(nc.sc);
			if (conns != null) {
				conns.remove(nc);
			}
		} catch (final Exception e) {
			_error(qc.getServerCfg(), "failed to shut down Queue: \n" + qc.getQueueName(), e);
		} 
		
		return qc;
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
				return null;
			}
			connFactories.put(sc, connFactory);
		}
	
		return connFactory;
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

	private static final void log(final ServerCfg sc, final Priority priority, final String infoStr) {
		log.log(priority, infoStr);
		final Logger logForSrv = ConsumerLoggers.getLoggerByServerCfg(sc);
		if (logForSrv != null) {
			log.log(priority, infoStr);
		}
	}
	
	private static final void _info(final ServerCfg sc, final String infoStr) {
		log(sc, Priority.INFO, infoStr);
	}
	
	private static final void _error(final ServerCfg sc, final String infoStr, final Throwable t) {
		log.error(infoStr, t);
		final Logger logForSrv = ConsumerLoggers.getLoggerByServerCfg(sc);
		if (logForSrv != null) {
			log.error(infoStr, t);
		}
	}
	

	private static MQueueMgr instance = new MQueueMgr();
	
	public static MQueueMgr instance() {
		return instance;
	}
	
	
}
