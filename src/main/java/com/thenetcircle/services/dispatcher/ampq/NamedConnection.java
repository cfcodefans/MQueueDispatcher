package com.thenetcircle.services.dispatcher.ampq;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;
import com.thenetcircle.services.dispatcher.entity.QueueCfg;
import com.thenetcircle.services.dispatcher.entity.ServerCfg;

class NamedConnection implements ShutdownListener {
	public Connection conn;
	public String name;
	public Set<QueueCfg> qcSet = new HashSet<QueueCfg>();
	
	public ServerCfg sc;
	protected static final Logger log = Logger.getLogger(NamedConnection.class);
	
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
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((sc == null) ? 0 : sc.hashCode());
		return result;
	}
	
	public void shutdownCompleted(final ShutdownSignalException cause) {
//		final Object ref = cause.getReference();
		final Object reasonObj = cause.getReason();
		
		if (reasonObj instanceof AMQP.Connection.Close) {
			AMQP.Connection.Close close  = (AMQP.Connection.Close) reasonObj;
			if (AMQP.CONNECTION_FORCED == close.getReplyCode() && "OK".equals(close.getReplyText())) {
				final String infoStr = String.format("\n close connection to server: \n\t %s", sc);
				log.error(infoStr);
				MQueueMgr._info(sc, infoStr);
				return;
			}
		}
		
		if (!cause.isHardError()) {
			final String infoStr = String.format("\n unexpected shutdown on connection to server: \n\t %s \n\n\t", sc, cause.getCause());
			log.error(infoStr);
			MQueueMgr._info(sc, infoStr);
			return;
		} 
		
		Set<QueueCfg> _queuesForReconnect = new LinkedHashSet<QueueCfg>(qcSet);
		for (final QueueCfg qc : _queuesForReconnect) {
			MQueueMgr.instance().stopQueue(qc);
			MQueueMgr.instance().reconnActor.reconnect(qc);
		}
	}
}