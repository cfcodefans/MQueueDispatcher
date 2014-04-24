package com.thenetcircle.services.dispatcher.mgr;

import com.thenetcircle.services.common.BaseDao;
import com.thenetcircle.services.dispatcher.ampq.MQueues;
import com.thenetcircle.services.dispatcher.entity.QueueCfg;

public class MQueuesMgr {
	
	//TODO
	private BaseDao<QueueCfg> qcDao = null;
	
	public void create(QueueCfg qc) {
		qc = qcDao.create(qc);
		MQueues.instance().addQueueCfg(qc);
	}
	
	public void remove(QueueCfg qc) {
		
	}
	
}
