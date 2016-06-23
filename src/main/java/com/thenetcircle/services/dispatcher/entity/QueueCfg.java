package com.thenetcircle.services.dispatcher.entity;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Basic;
import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
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
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.thenetcircle.services.dispatcher.cfg.Configuration;
import com.thenetcircle.services.dispatcher.failsafe.FailsafeCfg;

@XmlRootElement
@Entity
@Table(name="queue_cfg")
@Cacheable(false)
public class QueueCfg extends Configuration {
	public static enum Status {
		RUNNING, STARTED, STOPPED
	}

	public static final String DEFAULT_ROUTE_KEY = "default_route_key";

	private static final int DEFAUTL_RETRY_TIMES = 100;
	
	private static final long serialVersionUID = 1L;

	public static ExchangeCfg defaultExchange(QueueCfg qc) {
		return new ExchangeCfg(qc.getQueueName() + "_router", qc.getServerCfg());
	}

	@Basic
	private boolean autoDelete = false;

	@OneToOne(fetch=FetchType.EAGER, cascade= {CascadeType.ALL})
	@JoinColumn(name = "dest_cfg_id")
	private HttpDestinationCfg destCfg;
	
	@Basic
	private boolean durable = true;
	
	@Basic
	private boolean enabled = true;
	
	@ManyToMany(fetch=FetchType.EAGER, mappedBy="queues", cascade= {CascadeType.PERSIST, CascadeType.REFRESH})
	private Set<ExchangeCfg> exchanges = new HashSet<ExchangeCfg>();
	
	@Basic
	private boolean exclusive = false;

	@Transient
	private FailsafeCfg failsafeCfg = new FailsafeCfg();
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id = -1;

	@Basic
	private String name;

	@Basic
	@Column(name="prefetch_size")
	private Integer prefetchSize;

	@Basic
	private String queueName;

	@Column(name = "retry_limit")
	private int retryLimit;

	@Basic
	private String routeKey = StringUtils.EMPTY;

	@ManyToOne(fetch=FetchType.EAGER, cascade= {CascadeType.PERSIST, CascadeType.REFRESH})
	@JoinColumn(name = "server_id")
	private ServerCfg serverCfg;

	@Enumerated(EnumType.ORDINAL)
	private Status status = Status.STOPPED;

	@Override
	public boolean equals(Object obj) { 
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof QueueCfg))
			return false;
		
		
		QueueCfg other = (QueueCfg) obj;
		if (id != -1) {
			return id.equals(other.id); 
		}
		
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

	public HttpDestinationCfg getDestCfg() {
		return destCfg;
	}

	public Set<ExchangeCfg> getExchanges() {
		return exchanges;
	}

	@XmlTransient
	public FailsafeCfg getFailsafeCfg() {
		return failsafeCfg;
	}

	public Integer getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public Integer getPrefetchSize() {
		return prefetchSize;
	}

	public String getQueueName() {
		return queueName;
	}

	public int getRetryLimit() {
		return retryLimit;
	}

	public String getRouteKey() {
		return routeKey != null ? routeKey : queueName + "_router";
	}

	public ServerCfg getServerCfg() {
		return serverCfg;
	}

	public Status getStatus() {
		return status;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		
		if (id != -1) {
			return id;
		}
		
		result = prime * result + ((queueName == null) ? 0 : queueName.hashCode());
		result = prime * result + ((serverCfg == null) ? 0 : serverCfg.hashCode());
		return result;
	}

	public boolean isAutoDelete() {
		return autoDelete;
	}

	public boolean isDurable() {
		return durable;
	}

	public boolean isEnabled() {
		return enabled;
	}
	
	public boolean isExclusive() {
		return exclusive;
	}

	public void setAutoDelete(boolean autoDelete) {
		this.autoDelete = autoDelete;
	}

	public void setDestCfg(HttpDestinationCfg destCfg) {
		this.destCfg = destCfg;
	}
	
	public void setDurable(boolean durable) {
		this.durable = durable;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public void setExchanges(Set<ExchangeCfg> exchanges) {
		this.exchanges = exchanges;
	}
	
	public void setExclusive(boolean exclusive) {
		this.exclusive = exclusive;
	}

	public void setFailsafeCfg(FailsafeCfg failsafeCfg) {
		this.failsafeCfg = failsafeCfg;
	}

	public void setId(Integer id) {
		this.id = id;
	}
	
	public void setName(String name) {
		this.name = name;
	}

	public void setPrefetchSize(int priority) {
		this.prefetchSize = priority;
	}

	public void setQueueName(String queueName) {
		this.queueName = queueName;
	}
	
	public void setRetryLimit(int _retryLimit) {
		if (_retryLimit < 0) {
			_retryLimit = DEFAUTL_RETRY_TIMES;
		}
		this.retryLimit = _retryLimit;
	}
	
	public void setRouteKey(String routeKey) {
		this.routeKey = routeKey;
	}
	
	public void setServerCfg(ServerCfg serverCfg) {
		this.serverCfg = serverCfg;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("{class:\"QueueCfg\", id:'").append(id).append("', queueName:'").append(queueName).append("', durable:'").append(durable).append("', exclusive:'").append(exclusive).append("', autoDelete:'").append(autoDelete).append("', routeKey:'").append(routeKey)
				.append("', \nexchanges:").append(exchanges).append(", \nserverCfg:'").append(serverCfg).append("', \npriority:'").append(prefetchSize).append("', destCfg:'").append(destCfg).append("', enabled:'").append(enabled).append("', failsafeCfg:").append(failsafeCfg)
				.append(", retryLimit:'").append(retryLimit).append("'}");
		return builder.toString();
	}
	
	@XmlTransient
	@JsonIgnoreProperties
	public boolean isRunning() {
		return isEnabled() && Status.RUNNING.equals(getStatus());
	}
}
