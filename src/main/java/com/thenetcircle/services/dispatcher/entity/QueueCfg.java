package com.thenetcircle.services.dispatcher.entity;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Basic;
import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.thenetcircle.services.dispatcher.cfg.Configuration;
import com.thenetcircle.services.dispatcher.failsafe.FailsafeCfg;

@Entity
@Table(name="queue_cfg")
@Cacheable
public class QueueCfg extends Configuration {
	private static final long serialVersionUID = 1L;

	public static final String DEFAULT_ROUTE_KEY = "default_route_key";
	
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
	private String queueName;
	
	@Basic
	private boolean durable = true;
	
	@Basic
	private boolean exclusive = false;
	
	@Basic
	private boolean autoDelete = false;
	
	@Basic
	private String routeKey = DEFAULT_ROUTE_KEY;

	@ManyToMany(fetch=FetchType.EAGER, mappedBy="queues", cascade=CascadeType.PERSIST)
	private Set<ExchangeCfg> exchanges = new HashSet<ExchangeCfg>();

	public String getRouteKey() {
		return routeKey;
	}

	public void setRouteKey(String routeKey) {
		this.routeKey = routeKey;
	}

	@ManyToOne(fetch=FetchType.EAGER, cascade=CascadeType.PERSIST)
	@JoinColumn(name = "server_id")
	private ServerCfg serverCfg;

	@Basic
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
		builder.append("{class:\"QueueCfg\", id:'").append(id).append("', queueName:'").append(queueName).append("', durable:'").append(durable).append("', exclusive:'").append(exclusive).append("', autoDelete:'").append(autoDelete).append("', routeKey:'").append(routeKey)
				.append("', \nexchanges:").append(exchanges).append(", \nserverCfg:'").append(serverCfg).append("', \npriority:'").append(priority).append("', destCfg:'").append(destCfg).append("', enabled:'").append(enabled).append("', failsafeCfg:").append(failsafeCfg)
				.append(", retryLimit:'").append(retryLimit).append("'}");
		return builder.toString();
	}
	
	@OneToOne(fetch=FetchType.EAGER, cascade=CascadeType.PERSIST)
	@JoinColumn(name = "dest_cfg_id")
	private HttpDestinationCfg destCfg;

	public HttpDestinationCfg getDestCfg() {
		return destCfg;
	}

	public void setDestCfg(HttpDestinationCfg destCfg) {
		this.destCfg = destCfg;
	}
	
	@Basic
	private boolean enabled = true;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	
	@Transient
	private FailsafeCfg failsafeCfg = new FailsafeCfg();

	public FailsafeCfg getFailsafeCfg() {
		return failsafeCfg;
	}

	public void setFailsafeCfg(FailsafeCfg failsafeCfg) {
		this.failsafeCfg = failsafeCfg;
	}
	
	@Column(name = "retry_limit")
	private int retryLimit;

	public int getRetryLimit() {
		return retryLimit;
	}

	public void setRetryLimit(int retryLimit) {
		this.retryLimit = retryLimit;
	}
}
