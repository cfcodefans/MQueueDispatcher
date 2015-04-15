package com.thenetcircle.services.dispatcher.ampq;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Logger;

import com.thenetcircle.services.commons.MiscUtils;
import com.thenetcircle.services.commons.MiscUtils.LoopingArrayIterator;
import com.thenetcircle.services.dispatcher.IMessageActor;
import com.thenetcircle.services.dispatcher.entity.MessageContext;
import com.thenetcircle.services.dispatcher.entity.QueueCfg;
import com.thenetcircle.services.dispatcher.failsafe.IFailsafe;
import com.thenetcircle.services.dispatcher.failsafe.sql.FailedMessageSqlStorage;
import com.thenetcircle.services.dispatcher.log.ConsumerLoggers;
import com.thenetcircle.services.dispatcher.mgr.MsgMonitor;

public class Responder implements IMessageActor, Runnable {

	protected static final Log log = LogFactory.getLog(Responder.class.getSimpleName());
	private Queue<MessageContext> buf = new ConcurrentLinkedQueue<MessageContext>();
	
	private IFailsafe failsafe = FailedMessageSqlStorage.instance(); //DefaultFailedMessageHandler.instance();

	@Override
	public void run() {
		try {
			while (!Thread.interrupted()) {
//				handle(buf.poll(WAIT_FACTOR, WAIT_FACTOR_UNIT));
				MessageContext polled = buf.poll();
				if (polled != null) {
					handle(polled);
				} else {
					Thread.sleep(1);
				}
//				log.info(MiscUtils.invocationInfo());
			}
		} catch (Exception e) {
			log.error("Responder is interrupted");
		}
		log.info("Responder quits");
	}

	@Override
	public MessageContext handover(final MessageContext mc) {
		buf.offer(mc); 
		return mc;
	}

	@Override
	public void handover(Collection<MessageContext> mcs) {
		buf.addAll(mcs);
	}

	@Override
	public void handle(Collection<MessageContext> mcs) {
		mcs.forEach(this::handle);
	}
	
	@Override
	public MessageContext handle(final MessageContext mc) {
		if (mc == null) {
			return null;
		}
		
		final MsgMonitor monitor = MsgMonitor.instance();
		final MQueueMgr qm = MQueueMgr.instance();
		MessageContext _mc = monitor.handover(mc);
		
		try {
			if (_mc.isSucceeded()) {
				_mc = qm.acknowledge(mc);
				if (_mc.getFailTimes() > 0) {
					failsafe.handover(mc);
				}
				MsgMonitor.prefLog(mc, log);
				return _mc;
			}
			
			final QueueCfg qc = _mc.getQueueCfg();
			final Logger srvLog = ConsumerLoggers.getLoggerByServerCfg(qc.getServerCfg());
			final String logStr = String.format("\nMessage: %d failed %d times\n\tfrom queue: %s\n\tto url: %s\n\tcontent: %s\n\tresponse: %s\n",
					_mc.getDelivery().getEnvelope().getDeliveryTag(),
					_mc.getFailTimes(), 
					qc.getQueueName(), 
					qc.getDestCfg().getUrl(),
					_mc.getMessageContent(), 
					_mc.getResponse());
			log.info(logStr);
			srvLog.info(logStr);
			
			if (!_mc.isExceedFailTimes()) {
				return failsafe.handover(_mc);
			}
			
			log.info(String.format("MessageContext: %d exceeds the retryLimit: %d", _mc.getId(), _mc.getQueueCfg().getRetryLimit()));
			return qm.acknowledge(_mc);
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
		Stream.of(instances.getArray()).forEach(instance->instance.stop());
	}

	private ExecutorService executor = Executors.newSingleThreadExecutor(MiscUtils.namedThreadFactory(Responder.class.getSimpleName()));
	
	private static LoopingArrayIterator<Responder> instances = null;
	static {
		final List<Responder> list = new ArrayList<Responder>();
		final int RESPOND_NUMBER = (int)MiscUtils.getPropertyNumber("respond.number", MiscUtils.AVAILABLE_PROCESSORS * 4);
		IntStream.range(0, RESPOND_NUMBER).forEach(i->list.add(new Responder()));
		instances = new LoopingArrayIterator<Responder>(list.toArray(new Responder[0]));
	};
	
	public Responder() {
		executor.submit(this);
	}
	
	public static Responder instance() {
		return instances.loop();
	}
	
	public static Responder instance(long deliveryTag) {
		final Responder[] array = instances.getArray();
		return array[(int)(deliveryTag % array.length)];
	}
}
