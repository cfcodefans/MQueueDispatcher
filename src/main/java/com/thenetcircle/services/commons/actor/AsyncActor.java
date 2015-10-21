package com.thenetcircle.services.commons.actor;

import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.thenetcircle.services.commons.ProcTrace;
import com.thenetcircle.services.commons.ProcTrace.TraceEntry;

public abstract class AsyncActor<M, Q extends Queue<M>> implements IActor<M>, Runnable {

	protected AtomicBoolean		stopped	= new AtomicBoolean(false);
	protected static final Log	log		= LogFactory.getLog(AsyncActor.class);

	protected final Q			buf;

	protected M poll() throws Exception {
		return buf.poll();
	}

	public Queue<M> getBuf() {
		return buf;
	}

	@Override
	public void run() {
		if (!stopped.compareAndSet(false, true)) {
			return;
		}

		TraceEntry start = ProcTrace.start(String.format("%s starts", this.getClass().getSimpleName()));
		try {
			while (!(Thread.interrupted() || stopped.get())) {
				handle(poll());
			}
			ProcTrace.end();
		} catch (Exception e) {
			ProcTrace.end(start, e);
			log.error("Responder is interrupted", e);
		}
		stopped.set(true);

		log.info(ProcTrace.flush());
	}

	public M handover(final M m) {
		buf.offer(m);
		return m;
	}

	public void handover(final Collection<M> ms) {
		ms.forEach(buf::offer);
	}

	public AsyncActor(Q buf) {
		super();
		this.buf = buf;
	}

	public void handle(final Collection<M> ms) {
		ms.forEach(this::handle);
	}

	public void stop() {
		stopped.compareAndSet(false, true);
	}

	@Override
	public M handle(final M m) {
		return m;
	}

	@Override
	public boolean isStopped() {
		return stopped.get();
	}
}