package com.thenetcircle.services.dispatcher.failsafe.sql;

import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.thenetcircle.services.dispatcher.ampq.MQueues;
import com.thenetcircle.services.dispatcher.entity.MessageContext;
import com.thenetcircle.services.dispatcher.entity.QueueCfg;
import com.thenetcircle.services.dispatcher.failsafe.IFailsafe;
import com.thenetcircle.services.dispatcher.http.HttpDispatcherActor;
import com.thenetcircle.services.persistence.jpa.JpaModule;

public class FailedMessageSqlStorage implements Runnable, IFailsafe {

	protected static final Log log = LogFactory.getLog(FailedMessageSqlStorage.class.getSimpleName());
	private static FailedMessageSqlStorage instance = new FailedMessageSqlStorage();

	private BlockingQueue<MessageContext> buf = new LinkedBlockingQueue<MessageContext>();
	final ExecutorService executor = Executors.newSingleThreadExecutor();
	
	private EntityManager em = JpaModule.getEntityManager();
	
	public void delete(final Long id) {
		final Query q = em.createQuery("delete from MessageContext mc where mc.id=:id");
		q.setParameter("id", id);
		q.executeUpdate();
	}

	public MessageContext handle(final MessageContext mc) {
		if (mc == null) return mc;
		try {
			if (mc.isSucceeded()) {
				delete(mc.getId());
				return mc;
			}
			
//			MessageContext _mc = em.find(MessageContext.class, Long.valueOf(mc.getId()));
//			if (_mc == null) {
//				_mc = mc;
//			}
//			_mc.fail();
//			MQueues.instance().reject(mc, !_mc.isExceedFailTimes());
			
			mc.fail();
			log.info(String.format("\nMessage: %s failed %d times\n", mc.getQueueCfg().getQueueName(), mc.getFailTimes()));
			final MessageContext merge = em.merge(mc);
			mc.setFailTimes(merge.getFailTimes());
			return MQueues.instance().getNextActor(this).handover(mc);
		} catch (Exception e) {
			log.error("failed to handle: \n\t" + mc, e);
		}
		return mc;
	}
	
	private FailedMessageSqlStorage() {
		executor.submit(this);
	}

	public static FailedMessageSqlStorage instance() {
		return instance;
	}

	public void run() {
		log.info("FailedMessageSqlStorage starts");
		try {
			while (!Thread.interrupted()) {
				handle(Utils.pull(buf, 100));
			}
		} catch (InterruptedException e) {
			log.error("Interrupted during waiting for new failed job", e);
		}
		log.info("FailedMessageSqlStorage ends");
	}

	public MessageContext handover(final MessageContext mc) {
		log.info(" deliveryTag: " + mc.getDelivery().getEnvelope().getDeliveryTag());
		buf.offer(mc);
		return mc;
	}

	public void start() {
		executor.submit(this);
	}

	public void stop() {
		executor.shutdownNow();
	}

	public void retry(Collection<MessageContext> messages, QueueCfg qc) {
		for (final MessageContext msg : messages) {
			msg.setQueueCfg(qc);
			HttpDispatcherActor.instance().handover(msg);
		}
	}

	public void handle(final Collection<MessageContext> mcs) {
		if (CollectionUtils.isEmpty(mcs)) return;
		
		em.getTransaction().begin();
		for (final MessageContext mc : mcs) {
			log.info("handle Message: \n" + mc);
			handle(mc);
		}
		em.getTransaction().commit();
	}

	public void handover(final Collection<MessageContext> mcs) {
		buf.addAll(mcs);
	}
}
