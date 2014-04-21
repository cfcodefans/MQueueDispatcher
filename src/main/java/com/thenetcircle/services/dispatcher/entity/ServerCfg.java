package com.thenetcircle.services.dispatcher.entity;

import javax.persistence.Basic;
import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.thenetcircle.services.dispatcher.cfg.Configuration;

@Entity
@Table(name="server_cfg")
@Cacheable
public class ServerCfg extends Configuration {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	@Basic
	private String host = "localhost";
	
	@Basic
	private int port = 5672;

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("{class:\"ServerCfg\", id:'").append(id).append("', host:'").append(host).append("', port:'").append(port).append("', userName:'").append(userName).append("', password:'").append(password).append("', virtualHost:'").append(virtualHost)
				.append("', logFilePath:'").append(logFilePath).append("', maxFileSize:'").append(maxFileSize).append("}");
		return builder.toString();
	}

	@Basic
	private String userName = "guest";
	@Basic
	private String password = "guest";
	@Basic
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
	@Basic
	private String logFilePath;
	
	@Basic
	private String maxFileSize = String.valueOf("2GB");

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
