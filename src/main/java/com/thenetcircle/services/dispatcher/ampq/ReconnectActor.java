package com.thenetcircle.services.dispatcher.ampq;

import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import com.thenetcircle.services.commons.MiscUtils;
import com.thenetcircle.services.commons.persistence.jpa.JpaModule;
import com.thenetcircle.services.dispatcher.dao.QueueCfgDao;
import com.thenetcircle.services.dispatcher.entity.QueueCfg;
import com.thenetcircle.services.dispatcher.entity.QueueCfg.Status;

public class ReconnectActor implements Runnable {
	private Set<QueueCfg> queuesForReconnect = new LinkedHashSet<QueueCfg>();
	protected static final Logger log = Logger.getLogger(ReconnectActor.class);

	public void reconnect(final QueueCfg qc) {
		if (qc == null) {
			return;
		}
		
		final String infoStr = "going to reconnect queue: \n\t" + qc;
		log.info(infoStr);
		MQueueMgr._info(qc.getServerCfg(), infoStr);
		synchronized (queuesForReconnect) {
			queuesForReconnect.add(qc);
		}
	}

	public void run() {
		synchronized (queuesForReconnect) {
			log.info(MiscUtils.invocationInfo() + "\n\n\n");
			
			if (queuesForReconnect.isEmpty()) {
				log.info("no queue needs to be reconnected");
				return;
			}
			try {
				final QueueCfgDao qcDao = new QueueCfgDao(JpaModule.getEntityManager());
				final Set<QueueCfg> _queuesForReconnect = new LinkedHashSet<QueueCfg>(queuesForReconnect);
				final Set<QueueCfg> tempSet = new LinkedHashSet<QueueCfg>(queuesForReconnect);
				for (QueueCfg qc : _queuesForReconnect) {
					try {
						qc = qcDao.find(qc.getId());
					} catch (Exception e) {
						// TODO: handle exception
						log.error("what is up?", e);
					}
					log.info("reconnecting queue: " + qc.getName());
					final QueueCfg _qc = MQueueMgr.instance.startQueue(qc);
					if (qc.isEnabled()) {
						if (!Status.running.equals(qc.getStatus())) {
							final String infoStr = "failed to reconnect queue: \n\t" + qc.getName();
							log.info(infoStr);
							MQueueMgr._info(qc.getServerCfg(), infoStr);
						} else {
							tempSet.remove(_qc);
							final String infoStr = "successfully reconnected queue: \n\t" + qc.getName();
							log.info(infoStr);
							MQueueMgr._info(qc.getServerCfg(), infoStr);
						}
					}
				}
				queuesForReconnect = tempSet;
				qcDao.close();
			} catch (Exception e) {
				log.error("what is up?", e);
			}
		}
	}

	public void stopReconnect(final QueueCfg qc) {
		final String infoStr = "will not reconnect queue: \n\t" + qc;
		log.info(infoStr);
		MQueueMgr._info(qc.getServerCfg(), infoStr);
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