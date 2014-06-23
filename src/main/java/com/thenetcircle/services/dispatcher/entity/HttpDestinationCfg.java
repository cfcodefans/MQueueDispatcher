package com.thenetcircle.services.dispatcher.entity;

import javax.persistence.Basic;
import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;

import com.thenetcircle.services.dispatcher.cfg.Configuration;

@XmlRootElement
@Entity
@Table(name = "dest_cfg")
@Cacheable
public class HttpDestinationCfg extends Configuration {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id = -1;

	@Basic
	private String url;
	
//	@Basic
//	private int retry;
	
	@Basic
	private long timeout = 30000;

	public long getTimeout() {
		return timeout;
	}

	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

//	public int getRetry() {
//		return retry;
//	}
//
//	public void setRetry(int retry) {
//		this.retry = retry;
//	}

	@Basic
	private String hostHead;

	public String getHostHead() {
		return hostHead;
	}

	public void setHostHead(String hostHead) {
		this.hostHead = hostHead;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + id;
		
		if (id != -1) return id;
		
		result = prime * result + ((httpMethod == null) ? 0 : httpMethod.hashCode());
		result = prime * result + ((url == null) ? 0 : url.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof HttpDestinationCfg))
			return false;
		HttpDestinationCfg other = (HttpDestinationCfg) obj;
		if (id != -1) {
			return id.equals(other.id);
		}
		
		if (httpMethod == null) {
			if (other.httpMethod != null)
				return false;
		} else if (!httpMethod.equals(other.httpMethod))
			return false;

		if (url == null) {
			if (other.url != null)
				return false;
		} else if (!url.equals(other.url))
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("{class:\"HttpDestinationCfg\", id:'").append(id).append("', url:'").append(url).append("', hostHead:'").append(hostHead).append("', httpMethod:'").append(httpMethod).append("'}");
		return builder.toString();
	}
	
	private String httpMethod = "post";

	public String getHttpMethod() {
		return httpMethod;
	}

	public void setHttpMethod(String httpMethod) {
		this.httpMethod = httpMethod;
	}
	

}
