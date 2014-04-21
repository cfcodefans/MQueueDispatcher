package com.thenetcircle.services.dispatcher.failsafe;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.thenetcircle.services.dispatcher.ampq.ConsumerActor;
import com.thenetcircle.services.dispatcher.entity.MessageContext;
import com.thenetcircle.services.dispatcher.entity.QueueCfg;

public class DefaultFailedMessageHandler implements Runnable, IFailsafe {
	protected static final Log log = LogFactory.getLog(DefaultFailedMessageHandler.class.getSimpleName());
	private static DefaultFailedMessageHandler instance = new DefaultFailedMessageHandler();
	private BlockingQueue<MessageContext> buf = new LinkedBlockingQueue<MessageContext>();
	
	private Map<QueueCfg, TreeMap<Long, MessageContext>> storage = new HashMap<QueueCfg, TreeMap<Long, MessageContext>>();

	public MessageContext handover(final MessageContext mc) {
		buf.add(mc);
		return mc;
	}

	final ExecutorService executor = Executors.newSingleThreadExecutor();

	public void start() {
		executor.submit(this);
	}

	public MessageContext handle(final MessageContext mc) {
		TreeMap<Long, MessageContext> tagAndMsgs = null;
		if (!storage.containsKey(mc.getQueueCfg())) {
			tagAndMsgs = new TreeMap<Long, MessageContext>();
			storage.put(mc.getQueueCfg(), tagAndMsgs);
		} else {
			tagAndMsgs = storage.get(mc.getQueueCfg());
		}
		
		MessageContext _mc = tagAndMsgs.get(mc.getId());
		if (_mc == null) {
			_mc = mc;
		} 
		_mc.failAgain();
		tagAndMsgs.put(mc.getId(), _mc);
		
		return ConsumerActor.reject(mc, !_mc.isExceedFailTimes());
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
