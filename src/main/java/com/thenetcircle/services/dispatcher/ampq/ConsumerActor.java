package com.thenetcircle.services.dispatcher.ampq;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.QueueingConsumer.Delivery;
import com.thenetcircle.services.dispatcher.http.HttpDispatcherActor;

public class ConsumerActor extends DefaultConsumer {

	private QueueCfg queueCfg;
	protected static final Log log = LogFactory.getLog(ConsumerActor.class.getSimpleName());
	
	public ConsumerActor(final QueueCfg queueCfg) {
		super(MQueues.getInstance().getChannel(queueCfg));
		this.queueCfg = queueCfg;
	}

	public void handleDelivery(final String consumerTag, final Envelope envelope, final AMQP.BasicProperties properties, final byte[] body) throws IOException {
		final Delivery d = new Delivery(envelope, properties, body);
		final MessageContext mc = new MessageContext(queueCfg, d);
		HttpDispatcherActor.instance().dispatch(mc);
	}
	
	private static final Map<QueueCfg, ConsumerActor> cfgAndConsumers = new HashMap<QueueCfg, ConsumerActor>();
	
	public static MessageContext acknowledge(final MessageContext mc) {
		if (mc == null || !cfgAndConsumers.containsKey(mc.getQueueCfg())) return mc;
		final ConsumerActor ac = cfgAndConsumers.get(mc.getQueueCfg());
		try {
			ac.getChannel().basicAck(mc.getDelivery().getEnvelope().getDeliveryTag(), false);
		} catch (final IOException e) {
			log.error("failed to acknowledge message: \n" + new String(mc.getMessageBody()) + "\nresponse: " + mc.getResponse(), e);
		}
		
		return mc;
	}
	
	public static MessageContext reject(final MessageContext mc) {
		if (mc == null || !cfgAndConsumers.containsKey(mc.getQueueCfg())) return mc;
		final ConsumerActor ac = cfgAndConsumers.get(mc.getQueueCfg());
		try {
			ac.getChannel().basicReject(mc.getDelivery().getEnvelope().getDeliveryTag(), true);
		} catch (final IOException e) {
			log.error("failed to reject message: \n" + new String(mc.getMessageBody()) + "\nresponse: " + mc.getResponse(), e);
		}
		
		return mc;
	}
	
	public static void register(final ConsumerActor ac) {
		if (ac == null) return;
		cfgAndConsumers.put(ac.queueCfg, ac);
	}
	
	public static void unmount(final ConsumerActor ac) {
		if (ac == null) return;
		cfgAndConsumers.remove(ac.queueCfg);
	}
}
