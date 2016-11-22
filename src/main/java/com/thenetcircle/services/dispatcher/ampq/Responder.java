package com.thenetcircle.services.dispatcher.ampq;

import com.thenetcircle.services.commons.MiscUtils;
import com.thenetcircle.services.commons.MiscUtils.LoopingArrayIterator;
import com.thenetcircle.services.commons.actor.ConcurrentAsynActor;
import com.thenetcircle.services.dispatcher.entity.MessageContext;
import com.thenetcircle.services.dispatcher.entity.QueueCfg;
import com.thenetcircle.services.dispatcher.failsafe.IFailsafe;
import com.thenetcircle.services.dispatcher.failsafe.sql.FailedMessageSqlStorage;
import com.thenetcircle.services.dispatcher.log.ConsumerLoggers;
import com.thenetcircle.services.dispatcher.mgr.MsgMonitor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.thenetcircle.services.dispatcher.log.ConsumerLoggers._info;

public class Responder extends ConcurrentAsynActor<MessageContext> {
	protected static final Logger log = LogManager.getLogger(Responder.class);
	private IFailsafe failsafe = FailedMessageSqlStorage.instance(); // DefaultFailedMessageHandler.instance();

	protected MessageContext onSuccess(MessageContext mc) {
		final MQueueMgr qm = MQueueMgr.instance();
		if (mc.getFailTimes() == 0) {
			mc = qm.acknowledge(mc);
		} else if (mc.getFailTimes() > 0) {
			failsafe.handover(mc);
		}
		MsgMonitor.prefLog(mc, log);
		QueueCfg qc = mc.getQueueCfg();
		_info(qc.getServerCfg(), "the result of job: " + mc.getDelivery().getEnvelope().getDeliveryTag() + " for q " + qc.getName() + " on server " + qc.getServerCfg().getVirtualHost() + "\nresponse: " + mc.getResponse());
		return mc;
	}
	
	protected MessageContext onFail(MessageContext mc) {
		final MQueueMgr qm = MQueueMgr.instance();
		logFailure(mc);

		if (mc.getFailTimes() == 0) {
			mc = qm.acknowledge(mc);
		}
		mc.fail();
		if (!mc.isExceedFailTimes()) {
			return failsafe.handover(mc);
		}

		log.info(String.format("MessageContext: %d exceeds the retryLimit: %d", mc.getId(), mc.getQueueCfg().getRetryLimit()));
		return mc;
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
				return onSuccess(_mc);
			}

			return onFail(_mc);
		} catch (Exception e) {
			log.error("failed to handle: \n\t" + mc, e);
		}

		return mc;
	}

	protected void logFailure(MessageContext _mc) {
		final QueueCfg qc = _mc.getQueueCfg();
		final Logger srvLog = ConsumerLoggers.getLoggerByServerCfg(qc.getServerCfg());
		final String logStr = String.format("\nMessage: %d failed %d times\n\tfrom queue: %s\n\tto url: %s\n\tcontent: %s\n\tresponse: %s\n", _mc.getDelivery().getEnvelope().getDeliveryTag(), _mc.getFailTimes(), qc.getQueueName(), qc.getDestCfg().getUrl(), _mc.getMessageContent(),
				_mc.getResponse());
		log.info(logStr);
		srvLog.info(logStr);
	}

	@Override
	public void stop() {
		super.stop();
		executor.shutdownNow();
	}

	public static void stopAll() {
		Stream.of(instances.getArray()).forEach(instance -> instance.stop());
	}

	private ExecutorService executor = Executors.newSingleThreadExecutor(MiscUtils.namedThreadFactory(Responder.class.getSimpleName()));

	private static LoopingArrayIterator<Responder> instances = null;
	static {
		final List<Responder> list = new ArrayList<Responder>();
		final int RESPOND_NUMBER = (int) MiscUtils.getPropertyNumber("respond.number", MiscUtils.AVAILABLE_PROCESSORS * 4);
		IntStream.range(0, RESPOND_NUMBER).forEach(i -> list.add(new Responder()));
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
		return array[(int) (deliveryTag % array.length)];
	}
}
