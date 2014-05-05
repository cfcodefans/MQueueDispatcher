package com.thenetcircle.services;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.thenetcircle.comsumerdispatcher.config.DispatcherConfig;
import com.thenetcircle.comsumerdispatcher.config.QueueConf;
import com.thenetcircle.services.common.MiscUtils;
import com.thenetcircle.services.dispatcher.ampq.MQueues;
import com.thenetcircle.services.dispatcher.entity.QueueCfg;

@WebListener
public class Bootstrap implements ServletContextListener {
	private static Log log = LogFactory.getLog(Bootstrap.class);
	
	public static boolean once = false;
	
	public static void main(String[] args) {
		String filePath = null;
		if (args.length > 0) {
			filePath = args[0];
		}
		if (args.length > 1) {
			once = "once".equals(args[2]);
		}
		
		try {
			// load configurations either from file or from distribution server
			final DispatcherConfig dispatcherConfig = DispatcherConfig.getInstance();
			dispatcherConfig.loadConfig(filePath);
			
			log.info("configuration is loaded");
			
			final List<QueueConf> qcs = new ArrayList<QueueConf>(dispatcherConfig.getServers().values());
			
//			final List<ServerCfg> serverCfgs = DispatcherConfig.queueConfsToServerCfgs(qcs);
			Collection<QueueCfg> queueCfgs = DispatcherConfig.dispatcherJobsToQueueCfgs(dispatcherConfig.getAllJobs());
			
			log.info(StringUtils.join(queueCfgs, '\n'));
			
			final MQueues mqueues = MQueues.instance();
//			mqueues.setQueueCfgs(queueCfgs);
			mqueues.initWithQueueCfgs(queueCfgs);
			
			Runtime.getRuntime().addShutdownHook(MQueues.cleaner);
			log.info("register MQueues.cleaner to shutdown hook");
			
			
			
			// determine if this one starts up as distribution master or distribution client or standalone
//			DistributionManager.getInstance();
//			
//			JobAssign ja = new JobAssign();
//			ja.startupJobs();
//			
//			ConsumerDispatcherMonitor.enableMonitor();
		} catch (Exception e) {
			log.error(e, e);
		}
	}

	@Override
	public void contextInitialized(final ServletContextEvent paramServletContextEvent) {
		log.info(MiscUtils.invocationInfo());
	}

	@Override
	public void contextDestroyed(final ServletContextEvent paramServletContextEvent) {
		log.info(MiscUtils.invocationInfo());
		
	}
}
