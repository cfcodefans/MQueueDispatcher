package com.thenetcircle.services.web;

import com.thenetcircle.services.cluster.JGroupsActor;
import com.thenetcircle.services.commons.MiscUtils;
import com.thenetcircle.services.commons.persistence.jpa.JpaModule;
import com.thenetcircle.services.dispatcher.ampq.MQueueMgr;
import com.thenetcircle.services.dispatcher.dao.QueueCfgDao;
import com.thenetcircle.services.dispatcher.entity.QueueCfg;
import com.thenetcircle.services.dispatcher.mgr.NotificationActor;
import com.thenetcircle.services.rest.MonitorRes;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;


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
		JpaModule.instance().destory();
		MonitorRes.shutdown();
		JGroupsActor.instance().stop();
		NotificationActor.instance().stop();
	}

	private void loadQueues() {
		try (QueueCfgDao qcDao = new QueueCfgDao(JpaModule.getEntityManager())) {
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
			
			List<Callable<QueueCfg>> initiators = qcList.stream().map(StartUpListener::makeQueueInitiator).collect(Collectors.toList()); 

			List<QueueCfg> starteds = threadPool.invokeAll(initiators).stream().map(f -> {
				try {
					return f.get();
				} catch(Exception e) {
					log.error("can't load queue", e);
					return null;
				}
			}).filter(qc -> qc instanceof QueueCfg).collect(Collectors.toList());
			threadPool.shutdown();
			
			qcDao.beginTransaction();
			starteds.forEach(qcDao::edit);
			qcDao.endTransaction();
			
			CollectionUtils.subtract(qcList, starteds).forEach(mqueueMgr.getReconnActor()::addReconnect);
			starteds.stream().filter(qc -> !qc.isRunning()).forEach(mqueueMgr.getReconnActor()::addReconnect);
			
			log.info("\n\nWait for queues initialization");
			while (!threadPool.isTerminated()) {
				Thread.sleep(1);
			}
			log.info("\n\nDone for queues initialization");
		} catch (Exception e) {
			log.error("failed to load queues", e);
		} 
	}

	private static Callable<QueueCfg> makeQueueInitiator(QueueCfg qc) {
		return ()-> MQueueMgr.instance().startQueue(qc);
	}
}
