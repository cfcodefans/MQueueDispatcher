package com.thenetcircle.services.web;

import java.util.List;

import javax.inject.Inject;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.thenetcircle.services.common.MiscUtils;
import com.thenetcircle.services.dispatcher.ampq.MQueues;
import com.thenetcircle.services.dispatcher.dao.QueueCfgDao;
import com.thenetcircle.services.dispatcher.entity.QueueCfg;


@WebListener
public class StartUpListener implements ServletContextListener {

	private static Log log = LogFactory.getLog(StartUpListener.class);
	@Inject
	private QueueCfgDao qcDao;

	@Override
	public void contextInitialized(final ServletContextEvent paramServletContextEvent) {
		log.info(MiscUtils.invocationInfo());
//		startup();
	}

	@Override
	public void contextDestroyed(final ServletContextEvent paramServletContextEvent) {
		log.info(MiscUtils.invocationInfo());
//		MQueues.instance().shutdown();
	}

	private void startup() {
		log.info(MiscUtils.invocationInfo());
		log.info("\n\tloading QueueCfg from database\n");
		
		final List<QueueCfg> qcList = qcDao.findAll();
		
		if (CollectionUtils.isEmpty(qcList)) {
			log.warn("\n\tnot QueueCfg found!\n");
			return ;
		}
		
//		MQueues.instance().initWithQueueCfgs(qcList);
		Runtime.getRuntime().addShutdownHook(MQueues.cleaner);
	}

}
