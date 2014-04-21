package com.thenetcircle.services.dispatcher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import com.thenetcircle.services.dispatcher.entity.MessageContext;

public interface IMessageActor {
	MessageContext handover(final MessageContext mc);

	void handover(final Collection<MessageContext> mcs);

	void handle(final Collection<MessageContext> mcs);

	MessageContext handle(final MessageContext mc);
	
	static final int WAIT_FACTOR = 2;
	static final TimeUnit WAIT_FACTOR_UNIT = TimeUnit.SECONDS;
	
	public static class Utils {
		public static final Collection<MessageContext> pull(final BlockingQueue<MessageContext> buf, final int size, final int wait, final TimeUnit waitTimeUnit) throws InterruptedException {
			final List<MessageContext> tempList = new ArrayList<>(size);
			final long start = System.currentTimeMillis();
			final long micros = waitTimeUnit.toMicros(wait);
			
			for (int i = 0; i < size && (System.currentTimeMillis() - start < micros); i++) {
				tempList.add(buf.poll(wait, waitTimeUnit));
			}
			
			return tempList;
		}
		
		public static final Collection<MessageContext> pull(final BlockingQueue<MessageContext> buf, final int size) throws InterruptedException {
			return pull(buf, size, WAIT_FACTOR, WAIT_FACTOR_UNIT);
		}
	}
}
