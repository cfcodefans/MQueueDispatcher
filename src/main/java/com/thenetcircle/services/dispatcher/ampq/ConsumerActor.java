package com.thenetcircle.services.dispatcher.ampq;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.QueueingConsumer.Delivery;
import com.thenetcircle.services.dispatcher.entity.MessageContext;
import com.thenetcircle.services.dispatcher.entity.QueueCfg;
import com.thenetcircle.services.dispatcher.entity.ServerCfg;
import com.thenetcircle.services.dispatcher.http.HttpDispatcherActor;
import com.thenetcircle.services.dispatcher.mgr.MsgMonitor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

import static com.thenetcircle.services.dispatcher.log.ConsumerLoggers._info;

public class ConsumerActor extends DefaultConsumer {

	private QueueCfg queueCfg;
	protected static final Logger log = LogManager.getLogger(ConsumerActor.class);

	public ConsumerActor(final Channel ch, final QueueCfg queueCfg) {
		super(ch);
		this.queueCfg = queueCfg;
	}

	public void handleDelivery(final String consumerTag, final Envelope envelope, final AMQP.BasicProperties properties, final byte[] body) throws IOException {
		final Delivery d = new Delivery(envelope, properties, body);
		final MessageContext mc = new MessageContext(queueCfg, d);

		final ServerCfg sc = queueCfg.getServerCfg();
		_info(sc, "get message: " + d.getEnvelope().getDeliveryTag() + " for q: " + queueCfg.getName()  + " on server " + sc.getVirtualHost());
		
		// TODO use injection to decouple dependency
		MsgMonitor.prefLog(mc, log);
		HttpDispatcherActor.instance().handover(mc);
	}

}
