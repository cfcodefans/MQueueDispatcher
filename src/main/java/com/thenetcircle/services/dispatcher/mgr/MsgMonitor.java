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

import com.thenetcircle.services.commons.MiscUtils;
import com.thenetcircle.services.dispatcher.IMessageActor;
import com.thenetcircle.services.dispatcher.entity.MessageContext;
import com.thenetcircle.services.dispatcher.entity.QueueCfg;

public class MsgMonitor implements IMessageActor, Runnable {

	private static MsgMonitor instance = new MsgMonitor();

	public MsgMonitor() {
		executor.submit(this);
	}

	public static MsgMonitor instance() {
		return instance;
	}

	protected static final Log log = LogFactory.getLog(MsgMonitor.class.getName());
	private BlockingQueue<MessageContext> buf = new LinkedBlockingQueue<MessageContext>();
	private ExecutorService executor = Executors.newSingleThreadExecutor(MiscUtils.namedThreadFactory("Monitor"));

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

	public IMessageActor getQueueMonitor(final QueueCfg qc) {
		return queueAndMonitors.get(qc);
	}

	public IMessageActor unregister(final QueueCfg qc) {
		IMessageActor removed = queueAndMonitors.remove(qc);
		if (removed != null) {
			removed.stop();
		}
		return removed;
	}

	public static void prefLog(final MessageContext mc, final Log _log, String...infos) {
		final long dt = mc.getId();
		if (dt % 100 == 11) {
			_log.info("http performance: \t" + dt + " at \t" + System.currentTimeMillis());
		}
	}
}
