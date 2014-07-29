package com.thenetcircle.services;

import java.net.URI;
import java.util.Collection;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.UriBuilder;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.glassfish.jersey.server.ResourceConfig;

import com.thenetcircle.comsumerdispatcher.config.DispatcherConfig;
import com.thenetcircle.services.common.MiscUtils;
import com.thenetcircle.services.dispatcher.ampq.MQueues;
import com.thenetcircle.services.dispatcher.dao.QueueCfgDao;
import com.thenetcircle.services.dispatcher.entity.QueueCfg;
import com.thenetcircle.services.rest.WeldBinder;
//import com.thenetcircle.services.rest.javascript.bridge.JavaScriptProxyRes;
//import com.sun.jersey.api.core.PackagesResourceConfig;

@ApplicationScoped
public class Bootstrap {
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
			Collection<QueueCfg> queueCfgs = loadDefaultQueueCfgs(filePath);
//			
//			System.out.println(queueCfgs);
			
//			final MQueues mqueues = MQueues.instance();
//			mqueues.initWithQueueCfgs(queueCfgs);
//			
//			Runtime.getRuntime().addShutdownHook(MQueues.cleaner);
//			log.info("register MQueues.cleaner to shutdown hook");
//			Bootstrap bs = WeldContext.INSTANCE.getBean(Bootstrap.class);
//			bs.startup();
//			startHttpServer();
//			Thread.currentThread().join();
		} catch (Exception e) {
			log.error(e, e);
		}
	}

	private static Collection<QueueCfg> loadDefaultQueueCfgs(String filePath) throws Exception {
		// load configurations either from file or from distribution server
		final DispatcherConfig dispatcherConfig = DispatcherConfig.getInstance();
		dispatcherConfig.loadConfig(StringUtils.defaultIfBlank(filePath, "job.xml"));
		
		log.info("configuration is loaded");
		
//		final List<QueueConf> qcs = new ArrayList<QueueConf>(dispatcherConfig.getServers().values());
		Collection<QueueCfg> queueCfgs = DispatcherConfig.dispatcherJobsToQueueCfgs(dispatcherConfig.getAllJobs());
		
		log.info(StringUtils.join(queueCfgs, '\n'));
		return queueCfgs;
	}
	
	private static void startHttpServer() {
		log.info(MiscUtils.invocationInfo());
		
		URI baseUri = UriBuilder.fromUri("http://localhost/").port(9998).segment("mqueue_dispatcher").build();
//		final PackagesResourceConfig resCfg = new PackagesResourceConfig("com.thenetcircle.services.rest");
		ResourceConfig resCfg = new ResourceConfig();
		resCfg.packages("com.thenetcircle.services.rest").register(new WeldBinder());
		
//		HttpServer server = JdkHttpServerFactory.createHttpServer(baseUri, resCfg, false);
//		JavaScriptProxyRes.build(resCfg);
		
//		server.start();
	}

	@Inject
	private QueueCfgDao qcDao;
	
	private void startup() {
		log.info(MiscUtils.invocationInfo());
		log.info("\n\tloading QueueCfg from database\n");
		
		final List<QueueCfg> qcList = qcDao.findAll();
		
		if (CollectionUtils.isEmpty(qcList)) {
			log.warn("\n\tnot QueueCfg found!\n");
			return ;
		}
		
		MQueues.instance().initWithQueueCfgs(qcList);
		Runtime.getRuntime().addShutdownHook(MQueues.cleaner);
	}
}
