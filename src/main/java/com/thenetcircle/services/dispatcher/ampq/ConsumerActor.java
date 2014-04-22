package com.thenetcircle.services.dispatcher.ampq;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.QueueingConsumer.Delivery;
import com.thenetcircle.services.dispatcher.entity.MessageContext;
import com.thenetcircle.services.dispatcher.entity.QueueCfg;
import com.thenetcircle.services.dispatcher.http.HttpDispatcherActor;

public class ConsumerActor extends DefaultConsumer {

	private QueueCfg queueCfg;
	protected static final Log log = LogFactory.getLog(ConsumerActor.class.getSimpleName());

	public ConsumerActor(final QueueCfg queueCfg) {
		super(MQueues.getInstance().getChannel(queueCfg));
		this.queueCfg = queueCfg;

		// executor.submit(this);
	}

	public void handleDelivery(final String consumerTag, final Envelope envelope, final AMQP.BasicProperties properties, final byte[] body) throws IOException {
		final Delivery d = new Delivery(envelope, properties, body);
		final MessageContext mc = new MessageContext(queueCfg, d);

		// TODO use injection to decouple dependency
		HttpDispatcherActor.instance().handover(mc);
	}
}

