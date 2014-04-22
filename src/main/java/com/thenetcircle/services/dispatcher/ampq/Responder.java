package com.thenetcircle.services.dispatcher.ampq;

import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.thenetcircle.services.common.MiscUtils;
import com.thenetcircle.services.dispatcher.IMessageActor;
import com.thenetcircle.services.dispatcher.entity.MessageContext;
import com.thenetcircle.services.dispatcher.failsafe.DefaultFailedMessageHandler;
import com.thenetcircle.services.dispatcher.failsafe.IFailsafe;
import com.thenetcircle.services.dispatcher.http.HttpDispatcherActor;

public class Responder implements IMessageActor, Runnable {

	protected static final Log log = LogFactory.getLog(Responder.class.getSimpleName());
	private BlockingQueue<MessageContext> buf = new LinkedBlockingQueue<MessageContext>();
	
	private IFailsafe failsafe = DefaultFailedMessageHandler.getInstance();

	@Override
	public void run() {
		try {
			while (!Thread.interrupted()) {
				handle(buf.poll(WAIT_FACTOR, WAIT_FACTOR_UNIT));
//				log.info(MiscUtils.invocationInfo());
			}
		} catch (Exception e) {
			log.error("Responder is interrupted", e);
		}
		log.info("Responder quits");
	}

	@Override
	public MessageContext handover(final MessageContext mc) {
		final boolean re = buf.offer(mc); 
		log.info(MiscUtils.invocationInfo() + re);
		return mc;
	}

	@Override
	public void handover(Collection<MessageContext> mcs) {
		buf.addAll(mcs);
	}

	@Override
	public void handle(Collection<MessageContext> mcs) {
		for (final MessageContext mc : mcs) {
			handle(mc);
		}
	}

	@Override
	public MessageContext handle(MessageContext mc) {
		if (mc == null) return null;
		
		try {
			if (mc.isSucceeded()) {
				MQueues.getInstance().acknowledge(mc);
				return mc;
			}
			
			if (!mc.isExceedFailTimes()) {
//			MQueues.getInstance().reject(mc, true);
				failsafe.handle(mc);
				return HttpDispatcherActor.instance().handover(mc);
			}
			
			log.info(String.format("MessageContext: %d exceeds the retryLimit: %d", mc.getDelivery().getEnvelope().getDeliveryTag(), mc.getQueueCfg().getRetryLimit()));
//		return MQueues.getInstance().reject(mc, false);
			return MQueues.getInstance().acknowledge(mc);
		} catch (Exception e) {
			log.error("failed to handle: \n\t" + mc, e);
		}
		
		return mc;
	}

	@Override
	public void stop() {
		executor.shutdownNow();
	}

	private ExecutorService executor = Executors.newSingleThreadExecutor();
	
	private static Responder instance = new Responder();
	
	public Responder() {
		executor.submit(this);
	}
	
	public static Responder getInstance() {
		return instance;
	}
}
