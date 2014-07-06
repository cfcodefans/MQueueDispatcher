package com.thenetcircle.services.dispatcher.mgr;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.thenetcircle.services.dispatcher.IMessageActor;
import com.thenetcircle.services.dispatcher.entity.MessageContext;
import com.thenetcircle.services.dispatcher.entity.QueueCfg;

public class Monitor implements IMessageActor, Runnable {

	private static Monitor instance = new Monitor();
	
	public Monitor() {
		executor.submit(this);
	}
	
	public static Monitor instance() {
		return instance;
	}
	
	protected static final Log log = LogFactory.getLog(Monitor.class.getName());
	private BlockingQueue<MessageContext> buf = new LinkedBlockingQueue<MessageContext>();
	private ExecutorService executor = Executors.newSingleThreadExecutor();

	@Override
	public void run() {
		try {
			while (!Thread.interrupted()) {
				handle(buf.poll(WAIT_FACTOR, WAIT_FACTOR_UNIT));
			}
		} catch (Exception e) {
			log.error("Responder is interrupted", e);
		}
		log.info("Responder quits");
	}

	@Override
	public MessageContext handover(final MessageContext mc) {
		buf.offer(mc); 
		return mc;
	}

	@Override
	public void handover(final Collection<MessageContext> mcs) {
		buf.addAll(mcs);
	}

	@Override
	public void handle(final Collection<MessageContext> mcs) {
		for (final MessageContext mc : mcs) {
			handle(mc);
		}
	}

	@Override
	public MessageContext handle(final MessageContext mc) {
		if (mc == null) {
			return mc;
		}
		
		final IMessageActor monitor = queueAndMonitors.get(mc.getQueueCfg());
		if (monitor != null) {
			monitor.handover(mc);
		}
		
		return mc;
	}

	@Override
	public void stop() {
		executor.shutdownNow();		
	}
	
	private Map<QueueCfg, IMessageActor> queueAndMonitors = new ConcurrentHashMap<QueueCfg, IMessageActor>();
	
	public IMessageActor register(final QueueCfg qc, final IMessageActor monitor) {
		if (qc == null || monitor == null) {
			return null;
		}
		
		return queueAndMonitors.put(qc, monitor);
	}
	
	public IMessageActor unregister(final QueueCfg qc) {
		return queueAndMonitors.remove(qc);
	}
	
}