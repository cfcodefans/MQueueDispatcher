package com.thenetcircle.comsumerdispatcher.config;

import java.util.List;
import java.util.Map;

public interface ConfigLoader {

	public Map<String, QueueConf> loadServers();
	
	public List<DispatcherJob> loadAllJobs();
	
	public MonitorConf loadJmxConfig();
}
