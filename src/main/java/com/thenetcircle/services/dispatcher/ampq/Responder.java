package com.thenetcircle.services.dispatcher.ampq;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Logger;

import com.thenetcircle.services.common.MiscUtils;
import com.thenetcircle.services.common.MiscUtils.LoopingArrayIterator;
import com.thenetcircle.services.dispatcher.IMessageActor;
import com.thenetcircle.services.dispatcher.entity.MessageContext;
import com.thenetcircle.services.dispatcher.entity.QueueCfg;
import com.thenetcircle.services.dispatcher.failsafe.IFailsafe;
import com.thenetcircle.services.dispatcher.failsafe.sql.FailedMessageSqlStorage;
import com.thenetcircle.services.dispatcher.log.ConsumerLoggers;
import com.thenetcircle.services.dispatcher.mgr.MsgMonitor;

public class Responder implements IMessageActor, Runnable {

	protected static final Log log = LogFactory.getLog(Responder.class.getSimpleName());
	private BlockingQueue<MessageContext> buf = new LinkedBlockingQueue<MessageContext>();
	
	private IFailsafe failsafe = FailedMessageSqlStorage.instance(); //DefaultFailedMessageHandler.instance();

	@Override
	public void run() {
		try {
			while (!Thread.interrupted()) {
//				handle(buf.poll(WAIT_FACTOR, WAIT_FACTOR_UNIT));
				handle(buf.take());
//				log.info(MiscUtils.invocationInfo());
			}
		} catch (Exception e) {
			log.error("Responder is interrupted");
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
		
		final MsgMonitor monitor = MsgMonitor.instance();
		final MQueueMgr qm = MQueueMgr.instance();
		monitor.handover(mc);
		
		try {
			if (mc.isSucceeded()) {
				if (mc.getFailTimes() > 0) {
					failsafe.handover(mc);
				}
				MsgMonitor.prefLog(mc, log);
				return qm.acknowledge(mc);
			}
			
			queueCfg.getStatus().failed();
			
			final QueueCfg qc = mc.getQueueCfg();
			final Logger srvLog = ConsumerLoggers.getLoggerByServerCfg(qc.getServerCfg());
			final String logStr = String.format("\nMessage failed %d times\n\tfrom queue: %s\n\tto url: %s\n\tcontent: %s\n\tresponse: %s\n", 
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
			return qm.acknowledge(mc);
		} catch (Exception e) {
			log.error("failed to handle: \n\t" + mc, e);
		}
		
		return mc;
	}

	@Override
	public void stop() {
		executor.shutdownNow();
	}
	
	public static void stopAll() {
		for (Responder instance : instances.getArray()) {
			instance.stop();
		}
	}

	private ExecutorService executor = Executors.newSingleThreadExecutor(MiscUtils.namedThreadFactory(Responder.class.getSimpleName()));
	
//	private static Responder instance = new Responder();
	
	private static LoopingArrayIterator<Responder> instances = null;
	static {
		final List<Responder> list = new ArrayList<Responder>();
		for (int i = 0, j = MiscUtils.AVAILABLE_PROCESSORS * 4; i < j; i++) {
			list.add(new Responder());
		}
		instances = new LoopingArrayIterator<Responder>(list.toArray(new Responder[0]));
	};
	
	public Responder() {
		executor.submit(this);
	}
	
	public static Responder instance() {
		return instances.loop();
	}
}
