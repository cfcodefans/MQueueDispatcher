package com.thenetcircle.services.dispatcher.ampq;

import java.io.Serializable;
import java.util.Arrays;

import com.rabbitmq.client.QueueingConsumer.Delivery;

public class MessageContext implements Serializable {
	private static final long serialVersionUID = 1L;

	private int bodyHash = 1;

	private Delivery delivery;

	private long failTimes;

	private long id = -1;
	private byte[] messageBody;

	private QueueCfg queueCfg;

	private String response;

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
		if (id != -1)
			return (int) id;

		result = prime * result + bodyHash;
		result = prime * result + ((queueCfg == null) ? 0 : queueCfg.hashCode());
		result = prime * result + ((response == null) ? 0 : response.hashCode());
		return result;
	}

	public void setDelivery(Delivery delivery) {
		this.delivery = delivery;
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
		builder.append("{class:\"MessageContext\",id:").append(id)
				.append(", queueCfg:").append(queueCfg)
				.append(", delivery:").append(delivery)
				.append(", messageBody:").append(Arrays.toString(messageBody))
				.append(", response:").append(response)
				.append(", bodyHash:").append(bodyHash)
				.append(", failTimes:").append(failTimes).append("}");
		return builder.toString();
	}

}
