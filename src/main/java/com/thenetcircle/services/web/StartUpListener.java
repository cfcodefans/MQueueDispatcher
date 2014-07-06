package com.thenetcircle.services.web;

import java.util.List;
import java.util.concurrent.Executors;

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
import com.thenetcircle.services.persistence.jpa.JpaModule;
import com.thenetcircle.services.rest.MonitorRes;


@WebListener
public class StartUpListener implements ServletContextListener {

	private static Log log = LogFactory.getLog(StartUpListener.class);

	@Override
	public void contextInitialized(final ServletContextEvent paramServletContextEvent) {
		log.info(MiscUtils.invocationInfo());
		startup();
	}

	@Override
	public void contextDestroyed(final ServletContextEvent paramServletContextEvent) {
		log.info(MiscUtils.invocationInfo());
		MQueues.instance().shutdown();
		JpaModule.instance().destory();
		MonitorRes.shutdown();
	}

	private void startup() {
		QueueCfgDao qcDao = new QueueCfgDao(JpaModule.getEntityManager());
		try {
			log.info(MiscUtils.invocationInfo());
			log.info("\n\tloading QueueCfg from database\n");

			final List<QueueCfg> qcList = qcDao.findAll();

			if (CollectionUtils.isEmpty(qcList)) {
				log.warn("\n\tnot QueueCfg found!\n");
				return;
			} 
			
			log.info(String.format("loading %d queues...", qcList.size()));

			Executors.newSingleThreadExecutor().submit(new Runnable() {
				@Override
				public void run() {
					try {
						MQueues.instance().initWithQueueCfgs(qcList);
					} catch (Exception e) {
						log.error("failed to load queues", e);
					}
				}
			});
			Runtime.getRuntime().addShutdownHook(MQueues.cleaner);

		} catch (Exception e) {
			log.error("failed to load queues", e);
		} finally {
			qcDao.clean();
		}
	}

}
