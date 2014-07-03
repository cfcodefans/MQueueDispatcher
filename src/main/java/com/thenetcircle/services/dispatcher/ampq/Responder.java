package com.thenetcircle.services.dispatcher.ampq;

import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Logger;

import com.thenetcircle.services.dispatcher.IMessageActor;
import com.thenetcircle.services.dispatcher.entity.MessageContext;
import com.thenetcircle.services.dispatcher.entity.QueueCfg;
import com.thenetcircle.services.dispatcher.failsafe.IFailsafe;
import com.thenetcircle.services.dispatcher.failsafe.sql.FailedMessageSqlStorage;
import com.thenetcircle.services.dispatcher.log.ConsumerLoggers;
import com.thenetcircle.services.dispatcher.mgr.Monitor;

public class Responder implements IMessageActor, Runnable {

	protected static final Log log = LogFactory.getLog(Responder.class.getSimpleName());
	private BlockingQueue<MessageContext> buf = new LinkedBlockingQueue<MessageContext>();
	
	private IFailsafe failsafe = FailedMessageSqlStorage.instance(); //DefaultFailedMessageHandler.instance();

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
//		final String msgStr = new String(mc.getMessageBody());
//		log.info(" deliveryTag: " + mc.getDelivery().getEnvelope().getDeliveryTag());
//		if (StringUtils.contains(msgStr, "shutdown")) {
//			final String queueNameStr = StringUtils.substringAfter(msgStr, " ");
//			log.info(String.format("shutdown QueueCfg[name='%s']", queueNameStr));
//			final QueueCfg qc = MQueues.instance().getQueueCfgs().iterator().next();
//			MQueues.instance().removeQueueCfg(qc);
//		}
		
		buf.offer(mc); 
//		log.info(MiscUtils.invocationInfo() + re);
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
	public MessageContext handle(final MessageContext mc) {
		if (mc == null) {
			return null;
		}
		
		final QueueCfg queueCfg = mc.getQueueCfg();
		queueCfg.getStatus().processed();
		
		Monitor.instance().handover(mc);
		
		try {
			if (mc.isSucceeded()) {
				if (mc.getFailTimes() > 1) {
					failsafe.handover(mc);
				}
				return MQueues.instance().acknowledge(mc);
			}
			
			queueCfg.getStatus().failed();
			
			QueueCfg qc = mc.getQueueCfg();
			Logger srvLog = ConsumerLoggers.getLoggerByQueueConf(qc.getServerCfg());
			String logStr = String.format("\nMessage failed %d times\n\tfrom queue: %s\n\tto url: %s\n\tcontent: %s\n\tresponse: %s\n", 
					mc.getFailTimes(), 
					qc.getQueueName(), 
					qc.getDestCfg().getUrl(),
					mc.getMessageContent(), 
					mc.getResponse());
			log.info(logStr);
			srvLog.info(logStr);
			
			if (!mc.isExceedFailTimes()) {
				return failsafe.handover(mc);
			}
			
			log.info(String.format("MessageContext: %d exceeds the retryLimit: %d", mc.getId(), mc.getQueueCfg().getRetryLimit()));
			return MQueues.instance().acknowledge(mc);
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
	
	public static Responder instance() {
		return instance;
	}
}
