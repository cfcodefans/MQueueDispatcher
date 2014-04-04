package com.thenetcircle.services.dispatcher.ampq;

import java.util.HashSet;
import java.util.Set;

import com.thenetcircle.services.dispatcher.cfg.Configuration;
import com.thenetcircle.services.dispatcher.http.HttpDestinationCfg;

public class QueueCfg extends Configuration {
	private static final long serialVersionUID = 1L;

	public static final String DEFAULT_ROUTE_KEY = "default_route_key";
	
	private int id;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	private String queueName;
	private boolean durable = true;
	private boolean exclusive = false;
	private boolean autoDelete = false;
	private String routeKey = DEFAULT_ROUTE_KEY;

	private Set<ExchangeCfg> exchanges = new HashSet<ExchangeCfg>();

	public String getRouteKey() {
		return routeKey;
	}

	public void setRouteKey(String routeKey) {
		this.routeKey = routeKey;
	}

	private ServerCfg serverCfg;

	private int priority;

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public String getQueueName() {
		return queueName;
	}

	public void setQueueName(String queueName) {
		this.queueName = queueName;
	}

	public boolean isDurable() {
		return durable;
	}

	public void setDurable(boolean durable) {
		this.durable = durable;
	}

	public boolean isExclusive() {
		return exclusive;
	}

	public void setExclusive(boolean exclusive) {
		this.exclusive = exclusive;
	}

	public boolean isAutoDelete() {
		return autoDelete;
	}

	public void setAutoDelete(boolean autoDelete) {
		this.autoDelete = autoDelete;
	}

	public ServerCfg getServerCfg() {
		return serverCfg;
	}

	public void setServerCfg(ServerCfg serverCfg) {
		this.serverCfg = serverCfg;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((queueName == null) ? 0 : queueName.hashCode());
		result = prime * result + ((serverCfg == null) ? 0 : serverCfg.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof QueueCfg))
			return false;
		QueueCfg other = (QueueCfg) obj;
		if (queueName == null) {
			if (other.queueName != null)
				return false;
		} else if (!queueName.equals(other.queueName))
			return false;
		if (serverCfg == null) {
			if (other.serverCfg != null)
				return false;
		} else if (!serverCfg.equals(other.serverCfg))
			return false;
		return true;
	}

	public Set<ExchangeCfg> getExchanges() {
		return exchanges;
	}

	public void setExchanges(Set<ExchangeCfg> exchanges) {
		this.exchanges = exchanges;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("{class:\"QueueCfg\",id:").append(id).append(", queueName:").append(queueName).append(", durable:").append(durable).append(", exclusive:").append(exclusive).append(", autoDelete:").append(autoDelete).append(", routeKey:").append(routeKey)
				.append(", exchanges:").append(exchanges).append(", serverCfg:").append(serverCfg).append(", priority:").append(priority).append(", destCfg:").append(destCfg).append(", enabled:").append(enabled).append("}");
		return builder.toString();
	}
	
	private HttpDestinationCfg destCfg;

	public HttpDestinationCfg getDestCfg() {
		return destCfg;
	}

	public void setDestCfg(HttpDestinationCfg destCfg) {
		this.destCfg = destCfg;
	}
	
	private boolean enabled = true;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
}
