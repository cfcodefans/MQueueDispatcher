package com.thenetcircle.comsumerdispatcher.config;

import java.io.Serializable;

import org.apache.commons.lang.StringUtils;

public class QueueConf implements Serializable {
	private static final long serialVersionUID = 1L;

	String host;
	// added by fan@thenetcircle.com for issue:
	// http://sylvester:8001/issues/18938
	String logFileName;
	String maxFileSize = String.valueOf("2GB");
	String name;
	String password;
	int port;

	// added by fan@thenetcircle.com for issue:
	// http://sylvester:8001/issues/18938
	String redisHost;
	String redisPort;

	int retryIntervalSeconds = 30; // retry interval, seconds

	String userName;

	String vhost;

	public QueueConf() {
		super();
	}
	
	public QueueConf(String name, String host, int port, String username, String passwd, String vhost) {
		this.name = name;
		this.host = host;
		this.port = port;
		this.userName = username;
		this.password = passwd;
		this.vhost = vhost;
	}

	public QueueConf(String name, String host, int port, String username, String passwd, String vhost, String logFileName, String maxFileSize) {
		this(name, host, port, username, passwd, vhost);
		if (StringUtils.isBlank(logFileName)) {
			this.logFileName = String.format("./%s", name);
		} else {
			this.logFileName = logFileName;
		}

		if (StringUtils.isNotBlank(maxFileSize)) {
			this.maxFileSize = maxFileSize;
		}
	}

	public String getHost() {
		return host;
	}

	public String getLogFileName() {
		return logFileName;
	}

	public String getMaxFileSize() {
		return maxFileSize;
	}
	public String getName() {
		return name;
	}

	public String getPassword() {
		return password;
	}

	public int getPort() {
		return port;
	}

	public String getRedisHost() {
		return redisHost;
	}

	public String getRedisPort() {
		return redisPort;
	}

	public int getRetryIntervalSeconds() {
		return retryIntervalSeconds;
	}

	public String getUserName() {
		return userName;
	}

	public String getVhost() {
		return vhost;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public void setLogFileName(String logFileName) {
		this.logFileName = logFileName;
	}

	public void setMaxFileSize(String maxFileSize) {
		this.maxFileSize = maxFileSize;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void setRedisHost(String redisHost) {
		this.redisHost = redisHost;
	}

	public void setRedisPort(String redisPort) {
		this.redisPort = redisPort;
	}

	public void setRetryIntervalSeconds(int retryInterval) {
		this.retryIntervalSeconds = retryInterval;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public void setVhost(String vhost) {
		this.vhost = vhost;
	}

	@Override
	public String toString() {
		return "QueueConf [name=" + name + ", host=" + host + ", port=" + port + ", userName=" + userName + ", password=" + password + ", vhost=" + vhost + ", logFileName="
				+ logFileName + ", maxFileSize=" + maxFileSize + "]";
	}

}