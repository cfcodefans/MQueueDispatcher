package com.thenetcircle.services.dispatcher.entity;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;

import javax.persistence.Basic;
import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import com.rabbitmq.client.QueueingConsumer.Delivery;
import com.thenetcircle.services.commons.MiscUtils;

@XmlRootElement
@Entity
@Table(name = "msg_ctx", indexes= {@Index(columnList="")})
@Cacheable
public class MessageContext implements Serializable {
	
	public static final int DEFAULT_RETRY_LIMIT = 100;

	private static final long serialVersionUID = 1L;

	@Transient
	private int bodyHash = 1;

	@Transient
	@XmlTransient
	private Delivery delivery;

	@Basic
	private long failTimes = 0;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id = -1;

	@Column(name = "msg_content", length = 10000)
	@Lob
	private byte[] messageBody = new byte[0];

	@ManyToOne(fetch = FetchType.EAGER, cascade=CascadeType.REFRESH)
	@JoinColumn(name = "queue_cfg_id")
	@XmlTransient
	private QueueCfg queueCfg;

	@Basic
	private long timestamp = System.currentTimeMillis();

	@Embedded
	private MsgResp response = null;
	
	public MsgResp getResponse() {
		return response;
	}

	public void setResponse(MsgResp response) {
		this.response = response;
	}

	public MessageContext() {
		super();
	}

	public MessageContext(final QueueCfg queueCfg, final Delivery delivery) {
		this();
		this.id = MiscUtils.uniqueLong();
		this.queueCfg = queueCfg;
		this.setDelivery(delivery);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof MessageContext))
			return false;
		final MessageContext other = (MessageContext) obj;

		if (id != other.id)
			return false;

		if (!Arrays.equals(messageBody, other.messageBody))
			return false;

		if (queueCfg == null) {
			if (other.queueCfg != null)
				return false;
		} else if (!queueCfg.equals(other.queueCfg))
			return false;
		if (response == null) {
			if (other.response != null)
				return false;
		} else if (!response.equals(other.response))
			return false;
		return true;
	}

	public long fail() {
		this.timestamp = System.currentTimeMillis();
		return ++failTimes;
	}

	@XmlTransient
	public Delivery getDelivery() {
		return delivery;
	}

	public long getFailTimes() {
		return failTimes;
	}

	public long getId() {
		return id;
	}

	@XmlTransient
	public byte[] getMessageBody() {
		return messageBody;
	}
	
	@XmlElement
	public String getMessageContent() {
		return new String(messageBody);
	}

	public long getMessageKey() {
		final long prime = 31;
		long result = 1;

		result = prime * result + bodyHash;
		result = prime * result + ((queueCfg == null) ? 0 : queueCfg.hashCode());
		result = prime * result + ((response == null) ? 0 : response.hashCode());
		return result;
	}

	@XmlTransient
	public QueueCfg getQueueCfg() {
		return queueCfg;
	}

	@XmlTransient
	public long getTimestamp() {
		return timestamp;
	}
	
	@XmlElement
	public String getTimestampStr() {
		return DateFormatUtils.format(timestamp, "yy-MM-dd HH:mm:ss");
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		
		if (id != -1) {
			return (int) id;
		}

		result = prime * result + bodyHash;
		result = prime * result + ((queueCfg == null) ? 0 : queueCfg.hashCode());
		result = prime * result + ((response == null) ? 0 : response.hashCode());
		return result;
	}

	@Transient
	public boolean isExceedFailTimes() {
		if (queueCfg != null) {
			int retryLimit = queueCfg.getRetryLimit();
			return failTimes > (retryLimit < 0 ? DEFAULT_RETRY_LIMIT : retryLimit);
		}
		return failTimes > DEFAULT_RETRY_LIMIT;
	}

	@Transient
	public boolean isSucceeded() {
		return response != null && StringUtils.equalsIgnoreCase(response.getResponseStr(), "ok");
	}

	public void setDelivery(Delivery delivery) {
		this.delivery = delivery;
//		this.id = delivery.getEnvelope().getDeliveryTag();
		setMessageBody(delivery.getBody());
	}

	public void setFailTimes(long failTimes) {
		this.failTimes = failTimes;
	}

	public void setId(long id) {
		this.id = id;
	}

	public void setMessageBody(byte[] messageBody) {
		this.messageBody = messageBody;
		this.bodyHash = Arrays.hashCode(messageBody);
	}

	public void setQueueCfg(QueueCfg queueCfg) {
		this.queueCfg = queueCfg;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
	
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("{class:\"MessageContext\",id: ").append(id)
				.append(", queueCfg: ").append(queueCfg)
//				.append(", delivery:").append(delivery)
				.append(", messageBody: '").append(new String(messageBody))
				.append("', response: ").append(response)
				.append(", failTimes: ").append(failTimes)
				.append(", timestamp: '").append(new Date(timestamp))
				.append("'}");
		return builder.toString();
	}
}
