package com.thenetcircle.services.web;

import com.thenetcircle.services.cluster.JGroupsActor;
import com.thenetcircle.services.commons.L;
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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;


//@WebListener
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

            final List<QueueCfg> qcList = Collections.emptyList();// qcDao.findEnabled();
            if (CollectionUtils.isEmpty(qcList)) {
                log.warn("\n\tnot QueueCfg found!\n");
                return;
            }
            log.info(String.format("loading %d queues...", qcList.size()));

            final ExecutorService threadPool = Executors.newFixedThreadPool(MiscUtils.AVAILABLE_PROCESSORS, MiscUtils.namedThreadFactory("MQueueLoader"));

            List<Callable<QueueCfg>> initiators = qcList.stream().map(StartUpListener::makeQueueInitiator).collect(Collectors.toList());
            List<QueueCfg> starteds = threadPool.invokeAll(initiators).stream()
                .map(L.wf(Future::get))
                .filter(qc -> qc instanceof QueueCfg)
                .collect(Collectors.toList());

            threadPool.shutdown();

            log.info("\n\nWait for queues initialization");
            while (!threadPool.isTerminated()) {
                threadPool.awaitTermination(1, TimeUnit.SECONDS);
            }
            log.info("\n\nDone for queues initialization");
        } catch (Exception e) {
            log.error("failed to load queues", e);
        }
    }

    private static Callable<QueueCfg> makeQueueInitiator(QueueCfg qc) {
        return () -> MQueueMgr.instance().startQueue(qc);
    }
}
