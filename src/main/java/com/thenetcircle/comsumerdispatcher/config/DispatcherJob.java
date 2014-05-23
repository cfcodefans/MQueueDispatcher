package com.thenetcircle.comsumerdispatcher.config;

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;


public class DispatcherJob implements Serializable {
	
	private static final long serialVersionUID = 1L;

	protected String name;
	protected String url;
	protected String queue;
	protected String exchange;
	protected String type;
	protected int timeout = 0;
	protected int count = -1;
	protected String urlhost;
	protected String encoding;
	
	protected int defaultTimeout = 30000;
	protected int defaultCount = 0;
	protected String defaultUrl;
	protected String defaultUrlHost = "";
	protected String defaultEncoding;
	
	protected String routeKey = StringUtils.EMPTY;
	public String getRouteKey() {
		return routeKey;
	}
	public void setRouteKey(String routeKey) {
		this.routeKey = routeKey;
	}

	//added by fan@thenetcircle.com for issue: http://sylvester:8001/issues/18938
	protected int retryLimit = -1;
	public int getRetryLimit() {
		return retryLimit;
	}
	public void setRetryLimit(int retryLimit) {
		this.retryLimit = retryLimit;
	}

	public String getQueue() {
		if (queue != null && queue.length() > 0) return queue;
		return name;
	}

	public void setQueue(String queue) {
		this.queue = queue;
	}
	
	public String getExchange() {
		if (exchange != null && exchange.length() > 0) return exchange;
		return getName() + "_router";
	}

	public void setExchange(String exchange) {
		this.exchange = exchange;
	}

	public String getType() {
		if(type != null && type.length() > 0) return type;
		return "direct";
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	public String getName() {
		return name;
	}
	
	public String getLogicName() {
		return name + "-" + getFetcherQConf().getName();
	}

	public void setName(String consumerName) {
		this.name = consumerName;
	}

	public String getUrl() {
		if(url != null && url.length() > 0) return url;
		return defaultUrl;
	}

	public void setUrl(String url) {
		this.url = url.trim();
	}
	
	public String getEncoding() {
		if(encoding != null && encoding.length() > 0) return encoding;
		return getDefaultEncoding();
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding.trim();
	}

	public int getTimeout() {
		if (timeout <= 0) return defaultTimeout;
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public int getCount() {
		if (count == -1) return defaultCount;
		return count;
	}

	public void setCount(int threadCount) {
		this.count = threadCount;
	}

	public int getDefaultTimeout() {
		return defaultTimeout;
	}

	public void setDefaultTimeout(int defaultTimeout) {
		this.defaultTimeout = defaultTimeout;
	}

	public int getDefaultCount() {
		return defaultCount;
	}

	public void setDefaultCount(int defaultCount) {
		this.defaultCount = defaultCount;
	}
	
	public String getDefaultUrl() {
		return defaultUrl;
	}

	public void setDefaultUrl(String defaultUrl) {
		this.defaultUrl = defaultUrl;
	}
	
	public String getDefaultUrlHost() {
		return defaultUrlHost;
	}

	public void setDefaultUrlHost(String defaultUrlHost) {
		this.defaultUrlHost = defaultUrlHost;
	}

	public String getDefaultEncoding() {
		if(defaultEncoding == null || defaultEncoding.length() == 0)
			return "utf-8";
		return defaultEncoding;
	}

	public void setDefaultEncoding(String defaultEncoding) {
		this.defaultEncoding = defaultEncoding;
	}

	protected QueueConf fetcherQConf = null;

	public QueueConf getFetcherQConf() {
		return fetcherQConf;
	}

	public void setFetcherQConf(QueueConf fetcherQConf) {
		this.fetcherQConf = fetcherQConf;
	}

	public String getUrlhost() {
		if(urlhost != null && urlhost.length() > 0) return urlhost;
		return getDefaultUrlHost();
	}

	public void setUrlhost(String urlhost) {
		this.urlhost = urlhost;
	}
	
	@Override
	public String toString() {
		return "DispatcherJob [getRouteKey()=" + getRouteKey() + ", getRetryLimit()=" + getRetryLimit() + ", getQueue()=" + getQueue() + ", getExchange()=" + getExchange()
				+ ", getType()=" + getType() + ", getName()=" + getName() + ", getLogicName()=" + getLogicName() + ", getUrl()=" + getUrl() + ", getEncoding()=" + getEncoding()
				+ ", getTimeout()=" + getTimeout() + ", getCount()=" + getCount() + ", getDefaultTimeout()=" + getDefaultTimeout() + ", getDefaultCount()=" + getDefaultCount()
				+ ", getDefaultUrl()=" + getDefaultUrl() + ", getDefaultUrlHost()=" + getDefaultUrlHost() + ", getDefaultEncoding()=" + getDefaultEncoding()
				+ ", getFetcherQConf()=" + getFetcherQConf() + ", getUrlhost()=" + getUrlhost() + "]";
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
//	@Override
//	public String toString() {
//		return "DispatcherJob [name=" + name + ", url=" + url + ", queue="
//				+ queue + ", exchange=" + exchange + ", type=" + type
//				+ ", timeout=" + timeout + ", count=" + count + ", urlhost="
//				+ urlhost + ", encoding=" + encoding + ", defaultTimeout="
//				+ defaultTimeout + ", defaultCount=" + defaultCount
//				+ ", defaultUrl=" + defaultUrl + ", defaultUrlHost="
//				+ defaultUrlHost + ", defaultEncoding=" + defaultEncoding
//				+ ", fetcherQConf=" + fetcherQConf + ", retryLimit=" + retryLimit + "]";
//	}
	
	private boolean exchangeDurable = true;
	private boolean queueDurable = true;
	public boolean isExchangeDurable() {
		return exchangeDurable;
	}
	public void setExchangeDurable(boolean exchangeDurable) {
		this.exchangeDurable = exchangeDurable;
	}
	public boolean isQueueDurable() {
		return queueDurable;
	}
	public void setQueueDurable(boolean queueDurable) {
		this.queueDurable = queueDurable;
	}
	
	
}
