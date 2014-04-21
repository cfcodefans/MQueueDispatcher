package com.thenetcircle.services.dispatcher.ampq;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.QueueingConsumer.Delivery;
import com.thenetcircle.services.dispatcher.IMessageActor;
import com.thenetcircle.services.dispatcher.entity.MessageContext;
import com.thenetcircle.services.dispatcher.entity.QueueCfg;
import com.thenetcircle.services.dispatcher.failsafe.IFailsafe;
import com.thenetcircle.services.dispatcher.failsafe.sql.FailedMessageSqlStorage;
import com.thenetcircle.services.dispatcher.http.HttpDispatcherActor;

public class ConsumerActor extends DefaultConsumer implements IMessageActor, Runnable {

	private QueueCfg queueCfg;
	protected static final Log log = LogFactory.getLog(ConsumerActor.class.getSimpleName());

	public ConsumerActor(final QueueCfg queueCfg) {
		super(MQueues.getInstance().getChannel(queueCfg));
		this.queueCfg = queueCfg;
		
		executor.submit(this);
	}

	public void handleDelivery(final String consumerTag, final Envelope envelope, final AMQP.BasicProperties properties, final byte[] body) throws IOException {
		final Delivery d = new Delivery(envelope, properties, body);
		final MessageContext mc = new MessageContext(queueCfg, d);
		HttpDispatcherActor.instance().handover(mc);//TODO use injection to decouple dependency
	}

	public static MessageContext acknowledge(final MessageContext mc) {
		if (mc == null) {
			return mc;
		}
		final ConsumerActor ac = (ConsumerActor) MQueues.getInstance().getConsumer(mc.getQueueCfg());
		if (ac == null) {
			return mc;
		}

		try {
			ac.getChannel().basicAck(mc.getDelivery().getEnvelope().getDeliveryTag(), false);
		} catch (final IOException e) {
			log.error("failed to acknowledge message: \n" + new String(mc.getMessageBody()) + "\nresponse: " + mc.getResponse(), e);
		}
		return mc;
	}

	public static MessageContext reject(final MessageContext mc, final boolean requeue) {
		if (mc == null) {
			return mc;
		}
		final ConsumerActor ac = (ConsumerActor) MQueues.getInstance().getConsumer(mc.getQueueCfg());
		if (ac == null) {
			return mc;
		}
		try {
			ac.getChannel().basicReject(mc.getDelivery().getEnvelope().getDeliveryTag(), requeue);
		} catch (final IOException e) {
			log.error("failed to reject message: \n" + new String(mc.getMessageBody()) + "\nresponse: " + mc.getResponse(), e);
		}
		return mc;
	}

	private LinkedBlockingQueue<MessageContext> buf = new LinkedBlockingQueue<MessageContext>(10000);

	public MessageContext handover(MessageContext mc) {
		buf.add(mc);
		return mc;
	}

	public void handover(Collection<MessageContext> mcs) {
		buf.addAll(mcs);
	}

	public void run() {
		while (!Thread.interrupted()) {
			try {
				handle(buf.poll(WAIT_FACTOR, WAIT_FACTOR_UNIT));
			} catch (InterruptedException e) {
				log.error("interrupted", e);
				break;
			}
		}
	}

	public void handle(Collection<MessageContext> mcs) {
		for (final MessageContext mc : mcs) {
			handle(mc);
		}
	}

	public MessageContext handle(MessageContext mc) {
		if (mc == null)
			return mc;

		try {
			if (mc.isSucceeded()) {
				getChannel().basicAck(mc.getDelivery().getEnvelope().getDeliveryTag(), false);
			}
			
			failsafe.handover(mc);
			
//			} else {
//				getChannel().basicReject(mc.getDelivery().getEnvelope().getDeliveryTag(), true);
//			}
		} catch (final IOException e) {
			log.error("failed to " + (mc.isSucceeded() ? "acknowledge" : "reject") + " message: \n" + new String(mc.getMessageBody()) + "\nresponse: " + mc.getResponse(), e);
		}
		return mc;
	}
	
	final ExecutorService executor = Executors.newSingleThreadExecutor();
	final IFailsafe failsafe = FailedMessageSqlStorage.getInstance();//TODO use injection to decouple dependency
}
