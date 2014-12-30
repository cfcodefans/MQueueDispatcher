package com.thenetcircle.services.web;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.thenetcircle.services.cluster.JGroupsActor;
import com.thenetcircle.services.commons.MiscUtils;
import com.thenetcircle.services.commons.persistence.jpa.JpaModule;
import com.thenetcircle.services.dispatcher.ampq.MQueueMgr;
import com.thenetcircle.services.dispatcher.dao.QueueCfgDao;
import com.thenetcircle.services.dispatcher.entity.QueueCfg;
import com.thenetcircle.services.dispatcher.entity.QueueCfg.Status;
import com.thenetcircle.services.dispatcher.mgr.NotificationActor;
import com.thenetcircle.services.rest.MonitorRes;


@WebListener
public class StartUpListener implements ServletContextListener {

	private static Log log = LogFactory.getLog(StartUpListener.class);

	@Override
	public void contextInitialized(final ServletContextEvent paramServletContextEvent) {
		log.info(MiscUtils.invocationInfo());
		loadQueues();
		JGroupsActor.instance().start();
		NotificationActor.instance().start();
	}

	@Override
	public void contextDestroyed(final ServletContextEvent paramServletContextEvent) {
		log.info(MiscUtils.invocationInfo());
		MQueueMgr.instance().shutdown();
		JpaModule.instance().destory();
		MonitorRes.shutdown();
		JGroupsActor.instance().stop();
		NotificationActor.instance().stop();
	}

	private void loadQueues() {
		final QueueCfgDao qcDao = new QueueCfgDao(JpaModule.getEntityManager());
		try {
			log.info(MiscUtils.invocationInfo());
			log.info("\n\tloading QueueCfg from database\n");

			final List<QueueCfg> qcList = qcDao.findEnabled();

			if (CollectionUtils.isEmpty(qcList)) {
				log.warn("\n\tnot QueueCfg found!\n");
				return;
			} 
			
			log.info(String.format("loading %d queues...", qcList.size()));

			final ExecutorService threadPool = Executors.newFixedThreadPool(MiscUtils.AVAILABLE_PROCESSORS, MiscUtils.namedThreadFactory("MQueueLoader"));
			final MQueueMgr mqueueMgr = MQueueMgr.instance();
			
			for (final QueueCfg qc : qcList) {
				threadPool.submit(new Runnable() {
					@Override
					public void run() {
						final QueueCfg startedQueue = mqueueMgr.startQueue(qc);
						if (startedQueue.isEnabled() && Status.running.equals(startedQueue.getStatus())) return;
						mqueueMgr.getReconnActor().reconnect(qc);
					}
				});
			}
			threadPool.shutdown();
			
			log.info("\n\nWait for queues initialization");
			while (!threadPool.isTerminated());
			log.info("\n\nDone for queues initialization");
			
			Runtime.getRuntime().addShutdownHook(MQueueMgr.cleaner);

		} catch (Exception e) {
			log.error("failed to load queues", e);
		} finally {
			qcDao.close();
		}
	}

}
