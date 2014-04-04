package com.thenetcircle.services.dispatcher.failsafe.sql;

import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.thenetcircle.services.dispatcher.ampq.MessageContext;
import com.thenetcircle.services.dispatcher.ampq.QueueCfg;
import com.thenetcircle.services.dispatcher.failsafe.IFailedMessageManagment;
import com.thenetcircle.services.dispatcher.failsafe.IFailsafe;

public class FailedMessageSqlStorage implements  Runnable, IFailsafe, IFailedMessageManagment {

	protected static final Log log = LogFactory.getLog(FailedMessageSqlStorage.class.getSimpleName());
	private static FailedMessageSqlStorage instance = new FailedMessageSqlStorage();
	
	private BlockingQueue<MessageContext> buf = new LinkedBlockingQueue<MessageContext>();
	final ExecutorService executor = Executors.newSingleThreadExecutor();

	public MessageContext handle(final MessageContext mc) {
		return mc;
	}
	
	public static FailedMessageSqlStorage getInstance() {
		return instance;
	}

	public void run() {
		while (!Thread.interrupted()) {
			handle(buf.poll());
		}
	}

	public MessageContext handover(final MessageContext mc) {
		buf.offer(mc);
		return mc;
	}

	public void start() {
		executor.submit(this);
	}

	public void stop() {
		executor.shutdownNow();
	}

	public void retry(Criterion c) {
		
	}

	public void retry(Collection<MessageContext> messages, QueueCfg qc) {
		
	}

	public Collection<MessageContext> query(Criterion c) {
		return null;
	}

	public void handle(Collection<MessageContext> mcs) {
		for (final MessageContext mc : mcs) {
			handle(mc);
		}
	}

	public void handover(Collection<MessageContext> mcs) {
		buf.addAll(mcs);
	}
}
