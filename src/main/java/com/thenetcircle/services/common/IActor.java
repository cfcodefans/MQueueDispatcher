package com.thenetcircle.services.common;

import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public interface IActor<T> {
	static final int WAIT_FACTOR = 1;
	static final TimeUnit WAIT_FACTOR_UNIT = TimeUnit.MILLISECONDS;

	public static class AsyncActor<M> implements IActor<M>, Runnable {

		protected boolean stopped = false;
		protected BlockingQueue<M> buf = new LinkedBlockingQueue<M>();
		protected static final Log log = LogFactory.getLog(AsyncActor.class.getName());

		@Override
		public void run() {
			stopped = false;
			try {
				while (!(Thread.interrupted() || stopped)) {
					handle(buf.poll(WAIT_FACTOR, WAIT_FACTOR_UNIT));
				}
			} catch (Exception e) {
				log.error("Responder is interrupted", e);
			}
			stopped = true;
			log.info("Responder quits");
		}

		public M handover(final M m) {
			buf.offer(m);
			return m;
		}

		public void handover(final Collection<M> ms) {
			buf.addAll(ms);
		}

		public void handle(final Collection<M> ms) {
			for (final M m : ms) {
				handle(m);
			}
		}

		public void stop() {
			stopped = true;
		}

		@Override
		public M handle(final M m) {
			return null;
		}
	}

	T handover(final T m);

	void handover(final Collection<T> ms);

	void handle(final Collection<T> ms);

	T handle(final T m);

	void stop();
}
