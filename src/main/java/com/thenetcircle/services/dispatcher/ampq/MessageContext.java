package com.thenetcircle.services.dispatcher.ampq;

import java.io.Serializable;
import java.util.Arrays;

import com.rabbitmq.client.QueueingConsumer.Delivery;

public class MessageContext implements Serializable {
	private static final long serialVersionUID = 1L;

	private QueueCfg queueCfg;
	private Delivery delivery;

	public Delivery getDelivery() {
		return delivery;
	}

	public void setDelivery(Delivery delivery) {
		this.delivery = delivery;
		this.messageBody = delivery.getBody();
		bodyHash = Arrays.hashCode(messageBody);
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
		MessageContext other = (MessageContext) obj;
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
		builder.append("MessageContext [queueCfg=");
		builder.append(queueCfg);
		builder.append(", messageBody=");
		builder.append(new String(messageBody));
		builder.append(", response=");
		builder.append(response);
		builder.append(", bodyHash=");
		builder.append(bodyHash);
		builder.append("]");
		return builder.toString();
	}

	public MessageContext(final QueueCfg queueCfg, final Delivery delivery) {
		super();
		this.queueCfg = queueCfg;
		this.setDelivery(delivery);
	}
	
	
}
