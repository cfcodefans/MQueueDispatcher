package com.thenetcircle.services.dispatcher.ampq;

import java.util.LinkedHashSet;
import java.util.Set;

import com.thenetcircle.services.dispatcher.entity.QueueCfg;

class ReconnectActor implements Runnable {
	private Set<QueueCfg> queuesForReconnect = new LinkedHashSet<QueueCfg>();

	public void reconnect(final QueueCfg qc) {
		if (qc == null) {
			return;
		}
		MQueueMgr._info(qc.getServerCfg(), "going to reconnect queue: /n/t" + qc);
		synchronized (queuesForReconnect) {
			queuesForReconnect.add(qc);
		}
	}

	public void run() {
		synchronized (queuesForReconnect) {
			if (queuesForReconnect.isEmpty()) {
				MQueueMgr.log.info("no queue needs to be reconnected");
				return;
			}

			Set<QueueCfg> _queuesForReconnect = new LinkedHashSet<QueueCfg>();
			for (final QueueCfg qc : queuesForReconnect) {
				QueueCfg _qc = MQueueMgr.instance.startQueue(qc);
				if (!qc.isEnabled()) {
					_queuesForReconnect.add(_qc);
				}
			}
			queuesForReconnect = _queuesForReconnect;
		}
	}

	public void stopReconnect(final QueueCfg qc) {
		MQueueMgr._info(qc.getServerCfg(), "not going to reconnect queue: /n/t" + qc);
		synchronized (queuesForReconnect) {
			queuesForReconnect.remove(qc);
		}
	}

	public boolean isInReconnectSet(final QueueCfg qc) {
		synchronized (queuesForReconnect) {
			return queuesForReconnect.contains(qc);
		}
	}
}