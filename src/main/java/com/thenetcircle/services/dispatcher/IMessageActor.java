package com.thenetcircle.services.dispatcher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Logger;

import com.thenetcircle.services.dispatcher.entity.MessageContext;
import com.thenetcircle.services.dispatcher.log.ConsumerLoggers;

public interface IMessageActor {
	MessageContext handover(final MessageContext mc);

	void handover(final Collection<MessageContext> mcs);

	void handle(final Collection<MessageContext> mcs);

	MessageContext handle(final MessageContext mc);
	
	void stop();
	
	static final int WAIT_FACTOR = 5;
	static final TimeUnit WAIT_FACTOR_UNIT = TimeUnit.MILLISECONDS;
	
	public static class Utils {
		public static final Collection<MessageContext> pull(final BlockingQueue<MessageContext> buf, final int size, final int wait, final TimeUnit waitTimeUnit) throws InterruptedException {
			final List<MessageContext> tempList = new ArrayList<>(size);
			final long start = System.currentTimeMillis();
			final long micros = waitTimeUnit.toMillis(wait);
			
			for (int i = 0; i < size && (System.currentTimeMillis() - start < micros); i++) {
				final MessageContext polled = buf.poll(wait, waitTimeUnit);
				if (polled == null) {
					return tempList;
				}
				tempList.add(polled);
			}
			
			return tempList;
		}
		
		public static final Collection<MessageContext> pull(final BlockingQueue<MessageContext> buf, final int size) throws InterruptedException {
			return pull(buf, size, WAIT_FACTOR, WAIT_FACTOR_UNIT);
		}
	}
	
	public static class DefaultMessageActor implements IMessageActor {
		
		@Override
		public MessageContext handover(final MessageContext mc) {
			return handle(mc);
		}

		@Override
		public void handover(final Collection<MessageContext> mcs) {
			handle(mcs);
		}

		@Override
		public void handle(final Collection<MessageContext> mcs) {
			for (final MessageContext mc : mcs) {
				handle(mc);
			}
		}

		@Override
		public MessageContext handle(final MessageContext mc) {
			final Logger log = ConsumerLoggers.getLoggerByQueueConf(mc.getQueueCfg().getServerCfg());
			log.info(mc);
			return mc;
		}

		@Override
		public void stop() {
			
		}
		
		public static final DefaultMessageActor instance = new DefaultMessageActor();
	}
	
	public static class AsyncMessageActor implements IMessageActor, Runnable {

		protected boolean stopped = false;
		protected BlockingQueue<MessageContext> buf = new LinkedBlockingQueue<MessageContext>();
		protected static final Log log = LogFactory.getLog(AsyncMessageActor.class.getName());

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
		public void stop() {
			stopped = true;
		}

		@Override
		public MessageContext handle(final MessageContext mc) {
			return null;
		}
	}
	
}
