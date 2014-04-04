package com.thenetcircle.services.dispatcher.ampq;

import java.io.Serializable;
import java.util.Arrays;

import com.rabbitmq.client.QueueingConsumer.Delivery;

public class MessageContext implements Serializable {
	private static final long serialVersionUID = 1L;

	private long id = -1;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	private QueueCfg queueCfg;
	private Delivery delivery;

	public Delivery getDelivery() {
		return delivery;
	}

	public void setDelivery(Delivery delivery) {
		this.delivery = delivery;
		setMessageBody(delivery.getBody());
	}

	private byte[] messageBody;
	private String response;

	private int bodyHash = 1;

	public QueueCfg getQueueCfg() {
		return queueCfg;
	}

	public void setQueueCfg(QueueCfg queueCfg) {
		this.queueCfg = queueCfg;
	}

	public byte[] getMessageBody() {
		return messageBody;
	}

	public String getResponse() {
		return response;
	}

	public void setResponse(String response) {
		this.response = response;
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

	public MessageContext(final QueueCfg queueCfg, final Delivery delivery) {
		super();
		this.queueCfg = queueCfg;
		this.setDelivery(delivery);
	}

	private long failTimes;

	public long getFailTimes() {
		return failTimes;
	}

	public void setFailTimes(long failTimes) {
		this.failTimes = failTimes;
	}

	public void setMessageBody(byte[] messageBody) {
		this.messageBody = messageBody;
		this.bodyHash = Arrays.hashCode(messageBody);
	}

}
