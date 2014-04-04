package com.thenetcircle.comsumerdispatcher.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.thenetcircle.services.dispatcher.ampq.ExchangeCfg;
import com.thenetcircle.services.dispatcher.ampq.QueueCfg;
import com.thenetcircle.services.dispatcher.ampq.ServerCfg;
import com.thenetcircle.services.dispatcher.http.HttpDestinationCfg;

public class DispatcherConfig {

	private static DispatcherConfig _self = null;
	private List<DispatcherJob> allJobs = null;
	private Map<String, QueueConf> servers = null;
	private MonitorConf monitorConf = null;

	public static synchronized DispatcherConfig getInstance() {
		if (null == _self) {
			_self = new DispatcherConfig();
		}
		return _self;
	}

	public void loadConfig(String configFilePath) throws Exception {
		ConfigLoader cLoader = null;
		if (null == configFilePath) {
			// cLoader = new
			// DistributedConfigLoader(DistributionManager.getInstance().getZk());
		} else {
			cLoader = new FileConfigLoader(configFilePath);
		}

		setServers(cLoader.loadServers());// go first
		setAllJobs(cLoader.loadAllJobs());
		setMonitorConf(cLoader.loadJmxConfig());
	}

	public List<DispatcherJob> getAllJobs() {
		return this.allJobs;
	}

	public Map<String, QueueConf> getServers() {
		return this.servers;
	}

	public MonitorConf getMonitorConf() {
		return monitorConf;
	}

	public void setAllJobs(List<DispatcherJob> allJobs) {
		this.allJobs = allJobs;
	}

	public void setServers(Map<String, QueueConf> servers) {
		this.servers = servers;
	}

	public void setMonitorConf(MonitorConf monitorConf) {
		this.monitorConf = monitorConf;
	}

	private DispatcherConfig() {
	}
	
	public static List<ServerCfg> queueConfsToServerCfgs(final List<QueueConf> servers) {
		final List<ServerCfg> serverCfgs = new ArrayList<ServerCfg>(servers.size());
		
		for (final QueueConf queueConf : servers) {
			final ServerCfg sc = queueConfToServerCfg(queueConf);
			serverCfgs.add(sc);
		}
		
		return serverCfgs;
	}

	private static ServerCfg queueConfToServerCfg(final QueueConf queueConf) {
		final ServerCfg sc = new ServerCfg();
		
		sc.setHost(queueConf.host);
		sc.setPort(queueConf.port);
		sc.setVirtualHost(queueConf.vhost);
		
		sc.setUserName(queueConf.userName);
		sc.setPassword(queueConf.password);
		
		sc.setLogFilePath(queueConf.getLogFileName());
		sc.setMaxFileSize(queueConf.maxFileSize);
		return sc;
	}
	
	public static List<QueueCfg> dispatcherJobsToQueueCfgs(final List<DispatcherJob> jobs, final List<ServerCfg> serverCfgs) {
		final List<QueueCfg> queueCfgs = new ArrayList<QueueCfg>(jobs.size());
		
		for (final DispatcherJob dj : jobs) {
			final QueueCfg qc = new QueueCfg();
			
			qc.setServerCfg(queueConfToServerCfg(dj.getFetcherQConf()));
			qc.setQueueName(dj.getQueue());
			qc.setRouteKey(dj.getRouteKey());
			qc.setDurable(dj.isQueueDurable());
			
			{
				final ExchangeCfg exchangeCfg = new ExchangeCfg();
				exchangeCfg.setServerCfg(qc.getServerCfg());
				exchangeCfg.setExchangeName(dj.getExchange());
				exchangeCfg.setType(dj.getType());
				exchangeCfg.setDurable(dj.isExchangeDurable());
				qc.getExchanges().add(exchangeCfg);
			}
			
			{
				final HttpDestinationCfg destCfg = new HttpDestinationCfg();
				destCfg.setRetry(dj.getRetryLimit());
				destCfg.setUrl(dj.getUrl());
				destCfg.setHostHead(dj.getUrlhost());
				qc.setDestCfg(destCfg);
			}
			
			queueCfgs.add(qc);
		}
		
		return queueCfgs;
	}
}
