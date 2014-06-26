package com.thenetcircle.services.dispatcher.entity;

import java.io.Serializable;
import java.util.Arrays;

import javax.persistence.Basic;
import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
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
import javax.xml.bind.annotation.XmlRootElement;

import com.rabbitmq.client.QueueingConsumer.Delivery;

@XmlRootElement
@Entity
@Table(name = "msg_ctx", indexes= {@Index(columnList="")})
@Cacheable
public class MessageContext implements Serializable {
	private static final long serialVersionUID = 1L;

	@Transient
	private int bodyHash = 1;

	@Transient
	private Delivery delivery;

	@Basic
	private long failTimes;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id = -1;

	@Column(name = "msg_content", length = 10000)
	@Lob
	private byte[] messageBody;

	@ManyToOne(fetch = FetchType.EAGER, cascade=CascadeType.REFRESH)
	@JoinColumn(name = "queue_cfg_id")
	private QueueCfg queueCfg;

	@Basic
	private String response;

	public MessageContext() {
		super();
	}

	public MessageContext(final QueueCfg queueCfg, final Delivery delivery) {
		super();
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

	public Delivery getDelivery() {
		return delivery;
	}

	public long getFailTimes() {
		return failTimes;
	}

	public long getId() {
		return id;
	}

	public byte[] getMessageBody() {
		return messageBody;
	}

	public QueueCfg getQueueCfg() {
		return queueCfg;
	}

	public String getResponse() {
		return response;
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

	public void setDelivery(Delivery delivery) {
		this.delivery = delivery;
		this.id = delivery.getEnvelope().getDeliveryTag();
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

	public void setResponse(String response) {
		this.response = response;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("{class:\"MessageContext\",id:").append(id).append(", queueCfg:").append(queueCfg).append(", delivery:").append(delivery).append(", messageBody:").append(Arrays.toString(messageBody)).append(", response:").append(response).append(", bodyHash:")
				.append(bodyHash).append(", failTimes:").append(failTimes).append("}");
		return builder.toString();
	}

	@Transient
	public boolean isSucceeded() {
		return "ok".equalsIgnoreCase(response);
	}

	@Transient
	public boolean isExceedFailTimes() {
		return failTimes > queueCfg.getRetryLimit();
	}

	public long fail() {
		return ++failTimes;
	}

	@Basic
	private long timestamp;

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
	
	
	public long getMessageKey() {
		final long prime = 31;
		long result = 1;

		result = prime * result + bodyHash;
//		result = prime * result + timestamp;
		result = prime * result + ((queueCfg == null) ? 0 : queueCfg.hashCode());
		result = prime * result + ((response == null) ? 0 : response.hashCode());
		return result;
	}
}
