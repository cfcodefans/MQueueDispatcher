package com.thenetcircle.services.web;

import java.util.List;
import java.util.concurrent.Executors;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.thenetcircle.services.cluster.JGroupsActor;
import com.thenetcircle.services.common.MiscUtils;
import com.thenetcircle.services.dispatcher.ampq.MQueueMgr;
import com.thenetcircle.services.dispatcher.dao.QueueCfgDao;
import com.thenetcircle.services.dispatcher.entity.QueueCfg;
import com.thenetcircle.services.persistence.jpa.JpaModule;
import com.thenetcircle.services.rest.MonitorRes;


@WebListener
public class StartUpListener implements ServletContextListener {

	private static Log log = LogFactory.getLog(StartUpListener.class);

	@Override
	public void contextInitialized(final ServletContextEvent paramServletContextEvent) {
		log.info(MiscUtils.invocationInfo());
		loadQueues();
		JGroupsActor.instance().start();
	}

	@Override
	public void contextDestroyed(final ServletContextEvent paramServletContextEvent) {
		log.info(MiscUtils.invocationInfo());
		MQueueMgr.instance().shutdown();
		JpaModule.instance().destory();
		MonitorRes.shutdown();
		JGroupsActor.instance().stop();
	}

	private void loadQueues() {
		final QueueCfgDao qcDao = new QueueCfgDao(JpaModule.getEntityManager());
		try {
			log.info(MiscUtils.invocationInfo());
			log.info("\n\tloading QueueCfg from database\n");

			final List<QueueCfg> qcList = qcDao.findAll();

			if (CollectionUtils.isEmpty(qcList)) {
				log.warn("\n\tnot QueueCfg found!\n");
				return;
			} 
			
			log.info(String.format("loading %d queues...", qcList.size()));

			Executors.newSingleThreadExecutor(MiscUtils.namedThreadFactory("MQueueLoader")).submit(new Runnable() {
				@Override
				public void run() {
					MQueueMgr.instance().startQueues(qcList);
				}
			});
			Runtime.getRuntime().addShutdownHook(MQueueMgr.cleaner);

		} catch (Exception e) {
			log.error("failed to load queues", e);
		} finally {
			qcDao.close();
		}
	}

}
