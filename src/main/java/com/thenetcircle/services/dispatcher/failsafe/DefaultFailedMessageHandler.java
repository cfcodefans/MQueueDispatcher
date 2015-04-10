package com.thenetcircle.services.dispatcher.failsafe;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.thenetcircle.services.commons.MiscUtils;
import com.thenetcircle.services.dispatcher.entity.MessageContext;
import com.thenetcircle.services.dispatcher.entity.QueueCfg;

public class DefaultFailedMessageHandler implements Runnable, IFailsafe {
	protected static final Log log = LogFactory.getLog(DefaultFailedMessageHandler.class.getSimpleName());
	private static DefaultFailedMessageHandler instance = new DefaultFailedMessageHandler();
	private Queue<MessageContext> buf = new ConcurrentLinkedQueue<MessageContext>();
	
	private Map<QueueCfg, TreeMap<Long, MessageContext>> storage = new HashMap<QueueCfg, TreeMap<Long, MessageContext>>();

	public MessageContext handover(final MessageContext mc) {
		buf.add(mc);
		return mc;
	}

	final ExecutorService executor = Executors.newSingleThreadExecutor(MiscUtils.namedThreadFactory("DefaultFailedMessageHandler"));

	public void start() {
		executor.submit(this);
	}

	public MessageContext handle(final MessageContext mc) {
		if (mc == null) return null;
		
		try {
			TreeMap<Long, MessageContext> tagAndMsgs = null;
			if (!storage.containsKey(mc.getQueueCfg())) {
				tagAndMsgs = new TreeMap<Long, MessageContext>();
				storage.put(mc.getQueueCfg(), tagAndMsgs);
			} else {
				tagAndMsgs = storage.get(mc.getQueueCfg());
			}
			
			if (mc.isSucceeded()) {
				return tagAndMsgs.remove(mc.getId());
			}
			
			MessageContext _mc = tagAndMsgs.get(mc.getId());
			if (_mc == null) {
				_mc = mc;
			} 
			_mc.fail();
			tagAndMsgs.put(mc.getId(), _mc);
			
			return _mc;
		} catch (Exception e) {
			log.error("failed to handle: \n\t" + mc, e);
		}
		
		return mc;
	}

	public void run() {
		try {
			while (!Thread.interrupted()) {
				MessageContext polled = buf.poll();
				if (polled != null) {
					handle(polled);
				} else {
					Thread.sleep(1);
				}
			}
		} catch (InterruptedException e) {
			log.error("interrupted", e);
		}
	}

	public void stop() {
		executor.shutdownNow();
	}

	private DefaultFailedMessageHandler() {
		start();
	}

	public static DefaultFailedMessageHandler instance() {
		return instance;
	}

	public void handle(Collection<MessageContext> mcs) {
		mcs.forEach(this::handle);
	}

	public void handover(Collection<MessageContext> mcs) {
		buf.addAll(mcs);
	}
}
