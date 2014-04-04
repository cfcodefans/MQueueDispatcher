package com.thenetcircle.services.dispatcher.failsafe;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.rabbitmq.client.Channel;
import com.thenetcircle.services.dispatcher.ampq.MQueues;
import com.thenetcircle.services.dispatcher.ampq.MessageContext;
import com.thenetcircle.services.dispatcher.ampq.QueueCfg;

public class DefaultFailedMessageHandler implements Runnable {
	protected static final Log log = LogFactory.getLog(DefaultFailedMessageHandler.class.getSimpleName());

	private static DefaultFailedMessageHandler instance = new DefaultFailedMessageHandler();

	private BlockingQueue<MessageContext> buf = new LinkedBlockingQueue<MessageContext>();

	public void handover(final MessageContext mc) {
		buf.add(mc);
	}
	
	final ExecutorService executor = Executors.newSingleThreadExecutor();
	
	public void start() {
		executor.submit(this);
	}

	public void handle(final MessageContext mc) {
		if (mc == null) {
			return;
		}

		final QueueCfg qc = mc.getQueueCfg();
		final Channel ch = MQueues.getInstance().getChannel(qc);

		if (ch == null) {
			return;
		}

		try {
			ch.basicReject(mc.getDelivery().getEnvelope().getDeliveryTag(), true);
		} catch (IOException e) {
			log.error("failed to reject job: \n" + new String(mc.getMessageBody()) + " to queue: \n" + mc.getQueueCfg().getQueueName(), e);
		}
	}

	public static DefaultFailedMessageHandler instance() {
		return instance;
	}

	public void run() {
		while (!Thread.interrupted()) {
			handle(buf.poll());
		}
	}
	
	public void stop() {
		executor.shutdownNow();
	}
	
}
