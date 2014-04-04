package com.thenetcircle.services.dispatcher.ampq;

import com.thenetcircle.services.dispatcher.cfg.Configuration;

public class ServerCfg extends Configuration {
	private static final long serialVersionUID = 1L;

	private int id;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	private String host = "localhost";
	private int port = 5672;

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ServerCfg [id=").append(id).append(", host=").append(host).append(", port=").append(port).append(", userName=").append(userName).append(", password=").append(password).append(", virtualHost=").append(virtualHost).append(", logFilePath=")
				.append(logFilePath).append(", maxFileSize=").append(maxFileSize).append("]");
		return builder.toString();
	}

	private String userName = "guest";
	private String password = "guest";
	private String virtualHost = "/";

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((host == null) ? 0 : host.hashCode());
		result = prime * result + port;
		result = prime * result + ((virtualHost == null) ? 0 : virtualHost.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof ServerCfg))
			return false;
		ServerCfg other = (ServerCfg) obj;
		if (host == null) {
			if (other.host != null)
				return false;
		} else if (!host.equals(other.host))
			return false;
		if (port != other.port)
			return false;
		if (virtualHost == null) {
			if (other.virtualHost != null)
				return false;
		} else if (!virtualHost.equals(other.virtualHost))
			return false;
		return true;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getVirtualHost() {
		return virtualHost;
	}

	public void setVirtualHost(String virtualHost) {
		this.virtualHost = virtualHost;
	}

	// log file configuration
	// TODO need better abstraction
	String logFilePath;
	String maxFileSize = String.valueOf("2GB");

	public String getLogFilePath() {
		return logFilePath;
	}

	public void setLogFilePath(String logFilePath) {
		this.logFilePath = logFilePath;
	}

	public String getMaxFileSize() {
		return maxFileSize;
	}

	public void setMaxFileSize(String maxFileSize) {
		this.maxFileSize = maxFileSize;
	}

}
