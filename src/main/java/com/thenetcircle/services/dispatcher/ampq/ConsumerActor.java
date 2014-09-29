package com.thenetcircle.services.dispatcher.ampq;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.QueueingConsumer.Delivery;
import com.thenetcircle.services.dispatcher.entity.MessageContext;
import com.thenetcircle.services.dispatcher.entity.QueueCfg;
import com.thenetcircle.services.dispatcher.http.HttpDispatcherActor;
import com.thenetcircle.services.dispatcher.mgr.MsgMonitor;

public class ConsumerActor extends DefaultConsumer {

	private QueueCfg queueCfg;
	protected static final Log log = LogFactory.getLog(ConsumerActor.class.getName());

	public ConsumerActor(final Channel ch, final QueueCfg queueCfg) {
		super(ch);
		this.queueCfg = queueCfg;
	}

	public void handleDelivery(final String consumerTag, final Envelope envelope, final AMQP.BasicProperties properties, final byte[] body) throws IOException {
		final Delivery d = new Delivery(envelope, properties, body);
		final MessageContext mc = new MessageContext(queueCfg, d);

		// TODO use injection to decouple dependency
		MsgMonitor.prefLog(mc, log);
		HttpDispatcherActor.instance().handover(mc);
		// MQueues.instance().firstActor().handover(mc);
	}

//	public void handleShutdownSignal(final String consumerTag, final ShutdownSignalException sig) {
//		MQueues.instance().onError(queueCfg, sig);
//	}
}
