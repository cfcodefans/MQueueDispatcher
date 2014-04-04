package com.thenetcircle.services.dispatcher.failsafe;

import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.thenetcircle.services.dispatcher.ampq.ConsumerActor;
import com.thenetcircle.services.dispatcher.ampq.MessageContext;

public class DefaultFailedMessageHandler implements Runnable, IFailsafe {
	protected static final Log log = LogFactory.getLog(DefaultFailedMessageHandler.class.getSimpleName());
	private static DefaultFailedMessageHandler instance = new DefaultFailedMessageHandler();
	private BlockingQueue<MessageContext> buf = new LinkedBlockingQueue<MessageContext>();

	public MessageContext handover(final MessageContext mc) {
		buf.add(mc);
		return mc;
	}

	final ExecutorService executor = Executors.newSingleThreadExecutor();

	public void start() {
		executor.submit(this);
	}

	public MessageContext handle(final MessageContext mc) {
		return ConsumerActor.reject(mc);
	}

	public void run() {
		while (!Thread.interrupted()) {
			handle(buf.poll());
		}
	}

	public void stop() {
		executor.shutdownNow();
	}

	private DefaultFailedMessageHandler() {

	}

	public static DefaultFailedMessageHandler getInstance() {
		return instance;
	}

	public void handle(Collection<MessageContext> mcs) {
		for (final MessageContext mc : mcs) {
			handle(mc);
		}
		return;
	}

	public void handover(Collection<MessageContext> mcs) {
		buf.addAll(mcs);
	}
}
