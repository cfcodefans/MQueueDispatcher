package com.thenetcircle.comsumerdispatcher.config;

import java.io.StringWriter;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.thenetcircle.comsumerdispatcher.config.xml.Conf;
import com.thenetcircle.comsumerdispatcher.config.xml.Job;
import com.thenetcircle.comsumerdispatcher.config.xml.Jobs;
import com.thenetcircle.comsumerdispatcher.config.xml.Monitor;
import com.thenetcircle.comsumerdispatcher.config.xml.ObjectFactory;
import com.thenetcircle.comsumerdispatcher.config.xml.Queueserver;
import com.thenetcircle.comsumerdispatcher.config.xml.Servers;
import com.thenetcircle.services.dispatcher.entity.ExchangeCfg;
import com.thenetcircle.services.dispatcher.entity.HttpDestinationCfg;
import com.thenetcircle.services.dispatcher.entity.QueueCfg;
import com.thenetcircle.services.dispatcher.entity.ServerCfg;

public class DispatcherConfig {

	private static DispatcherConfig _self = null;
	private List<DispatcherJob> allJobs = null;
	private Map<String, QueueConf> servers = null;
	private MonitorConf monitorConf = null;
	static Logger log = Logger.getLogger(DispatcherConfig.class.getSimpleName());

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
	
	public static List<QueueCfg> dispatcherJobsToQueueCfgs(final List<DispatcherJob> jobs) {
		final List<QueueCfg> queueCfgs = new ArrayList<QueueCfg>(jobs.size());
		final Map<Integer, ServerCfg> nameAndServerCfgs = new HashMap<Integer, ServerCfg>();
		
		for (final DispatcherJob dj : jobs) {
			final QueueCfg qc = new QueueCfg();
			
			
			ServerCfg sc = queueConfToServerCfg(dj.getFetcherQConf());
			
			if (!nameAndServerCfgs.containsKey(sc.hashCode())) {
				nameAndServerCfgs.put(Integer.valueOf(sc.hashCode()), sc);
				log.info(sc.getHost() + " server is loaded");
			} else {
				sc = nameAndServerCfgs.get(sc.hashCode());
			}
			
			qc.setServerCfg(sc);
			qc.setName(dj.getName());
			qc.setQueueName(dj.getQueue());
			qc.setRouteKey(dj.getRouteKey());
			qc.setDurable(dj.isQueueDurable());
			
			{
				final ExchangeCfg exchangeCfg = new ExchangeCfg();
				
				exchangeCfg.setServerCfg(qc.getServerCfg());
				exchangeCfg.setExchangeName(dj.getExchange());
				exchangeCfg.setType(dj.getType());
				exchangeCfg.setDurable(dj.isExchangeDurable());
				
				exchangeCfg.getQueues().add(qc);
				qc.getExchanges().add(exchangeCfg);
			}
			
			{
				final HttpDestinationCfg destCfg = new HttpDestinationCfg();
//				destCfg.setRetry(dj.getRetryLimit());
				destCfg.setUrl(dj.getUrl());
				destCfg.setHostHead(dj.getUrlhost());
				destCfg.setTimeout(dj.getTimeout());
				qc.setDestCfg(destCfg);
				qc.setRetryLimit(dj.retryLimit);
				
			}
			
			queueCfgs.add(qc);
		}
		
		return queueCfgs;
	}
	
	public static String queueCfgsToXML(final List<QueueCfg> qcList) {
		if (CollectionUtils.isEmpty(qcList)) {
			return null;
		}
		
		final ObjectFactory of = new ObjectFactory();
		
		final Jobs jobs = of.createJobs();
		
		{/*
		 * <conf>
		 * <request-pre>http://10.20.0.254:8080/consumerDispatch/dispatch/
		 * </request-pre> <request-count>1</request-count>
		 * <request-timeout>30000</request-timeout> </conf>
		 */
			final Conf conf = of.createConf();
			conf.setRequestPre("http://10.20.0.254:8080/consumerDispatch/dispatch/");
			conf.setRequestCount(BigInteger.valueOf(1l));
			conf.setRequestTimeout(BigInteger.valueOf(30000l));
			jobs.setConf(conf);
		}
		
		{//<monitor rHost="0.0.0.0" rPort="9999" httpHost="0.0.0.0" httpPort="8888"/>
			final Monitor monitor = of.createMonitor();
			monitor.setHttpHost("0.0.0.0");
			monitor.setHttpPort(BigInteger.valueOf(8888));
			monitor.setRHost("0.0.0.0");
			monitor.setRPort(BigInteger.valueOf(9999));
			jobs.setMonitor(monitor);
		}
		
		{/*
		<servers>
		<queueserver name="a" host="snowball" port="5672" userName="guest" password="guest" vhost="/" logFileName="/var/log/consumerDispatcher/poppen_a.log" redisHost="moleman-poppen" redisPort="6379" /> 
		  */
			final Servers servers = of.createServers();
			
			final Set<ServerCfg> _scSet = new HashSet<ServerCfg>();
			for (final QueueCfg qc : qcList) {
				_scSet.add(qc.getServerCfg());
			}
			
			final List<Queueserver> qsList = servers.getQueueserver();
			for (final ServerCfg sc : _scSet) {
				final Queueserver qs = of.createQueueserver();
				qs.setHost(sc.getHost());
				qs.setLogFileName(sc.getLogFilePath());
				qs.setName(sc.getId().toString());
				qs.setUserName(sc.getUserName());
				qs.setPassword(sc.getPassword());
				qs.setPort(BigInteger.valueOf(sc.getPort()));
				qs.setVhost(sc.getVirtualHost());
				qsList.add(qs);
			}
			
			jobs.setServers(servers);
		}
		
		{
			/*
			 * <job name="text_mail" server="a" queue="text_mail" exchange="text_mail_router" timeout="20000" url="http://develop.poppen.lab/frontend_dev.php/consumerDispatch/dispatch/"  />
			 * */
			
			final List<Job> jobList = jobs.getJob();
			for (final QueueCfg qc : qcList) {
				final Job job = of.createJob();
				final ExchangeCfg ec = qc.getExchanges().iterator().next();
				job.setExchange(ec.getExchangeName());
				job.setExchangeDurable(ec.isEnabled());
				job.setName(qc.getName());
				job.setQueueDurable(qc.isDurable());
				job.setRetryLimit(BigInteger.valueOf(qc.getRetryLimit()));
				job.setRouteKey(qc.getRouteKey());
				job.setServer(String.valueOf(qc.getServerCfg().getId()));
				job.setUrl(qc.getDestCfg().getUrl());
				job.setUrlHost(qc.getDestCfg().getHostHead());
				job.setType(ec.getType());
				
				jobList.add(job);
			}
		}
		
		
		try (final StringWriter sw = new StringWriter()) {
			final JAXBContext context = JAXBContext.newInstance(Jobs.class);
			final Marshaller marshaller = context.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, "job.xsd");
			marshaller.marshal(jobs, sw);

			return sw.getBuffer().toString();
		} catch (final Exception e) {
			e.printStackTrace();
			return StringUtils.EMPTY;
		}
	}
	
}
