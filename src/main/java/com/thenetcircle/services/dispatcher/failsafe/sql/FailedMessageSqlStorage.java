package com.thenetcircle.services.dispatcher.failsafe.sql;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.thenetcircle.services.common.MiscUtils;
import com.thenetcircle.services.dispatcher.entity.MessageContext;
import com.thenetcircle.services.dispatcher.entity.QueueCfg;
import com.thenetcircle.services.dispatcher.failsafe.IFailsafe;
import com.thenetcircle.services.dispatcher.http.HttpDispatcherActor;
import com.thenetcircle.services.persistence.jpa.JpaModule;

public class FailedMessageSqlStorage implements Runnable, IFailsafe {

	protected static final Log log = LogFactory.getLog(FailedMessageSqlStorage.class.getSimpleName());
	private static FailedMessageSqlStorage instance = new FailedMessageSqlStorage();

	private BlockingQueue<MessageContext> buf = new LinkedBlockingQueue<MessageContext>();
	final ExecutorService executor = Executors.newSingleThreadExecutor(MiscUtils.namedThreadFactory(FailedMessageSqlStorage.class.getSimpleName()));
	
	private EntityManager em = null;
	
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
			
			mc.fail();
//			final MessageContext merge = em.
//			mc.setFailTimes(merge.getFailTimes());
			
			final Query q = em.createQuery("update MessageContext mc set "
					+ " mc.failTimes=:failTimes, "
					+ " mc.response=:response, "
					+ " mc.timestamp=:_timestamp "
					+ " where mc.id=:id");
			q.setParameter("failTimes", Long.valueOf(mc.getFailTimes()));
			q.setParameter("response", mc.getResponse());
			q.setParameter("_timestamp", Long.valueOf(mc.getTimestamp()));
			q.setParameter("id", Long.valueOf(mc.getId()));
			
		    final int updatedFailMsg = q.executeUpdate();
			log.info("update failed job: " + updatedFailMsg);
			if (updatedFailMsg <= 0) {
				final MessageContext merge = em.merge(mc);
				mc.setFailTimes(merge.getFailTimes());
				mc.setId(merge.getId());
			}
			
			return HttpDispatcherActor.instance().handover(mc);
		} catch (Exception e) {
			log.error("failed to handle: \n\t" + mc, e);
		}
		return mc;
	}
	
	private FailedMessageSqlStorage() {
		start();
	}

	public static FailedMessageSqlStorage instance() {
		return instance;
	}

	public void run() {
		log.info("FailedMessageSqlStorage starts");
		try {
			while (!Thread.interrupted()) {
//				log.info("waiting for failed messages......");
				
				final List<MessageContext> mcList = new ArrayList<MessageContext>(100);
				
				MessageContext mc = null;
				for (int i = 0; i < 50; i++) {
					mc = buf.poll(WAIT_FACTOR, WAIT_FACTOR_UNIT);
					if (mc == null) {
						break;
					}
					mcList.add(mc);
				}
				
//				handle(Utils.pull(buf, 100));
//				log.info(String.format("polled %d failed messages......", mcList.size()));
				handle(mcList);
			}
		} catch (InterruptedException e) {
			log.error("Interrupted during waiting for new failed job", e);
		} catch (final Exception e) {
			log.error("Interrupted by exception", e);
		}
		log.info("FailedMessageSqlStorage ends");
	}

	public MessageContext handover(final MessageContext mc) {
		if (mc.getDelivery() != null) {
			log.info(" deliveryTag: " + mc.getDelivery().getEnvelope().getDeliveryTag());
		} else {
			log.info(String.format("failed message resent: %s \t failed times: %d", mc.getQueueCfg().getName(), mc.getFailTimes()));
		}
		buf.offer(mc);
		return mc;
	}

	public void start() {
		executor.submit(this);
	}

	public synchronized void stop() {
		executor.shutdownNow();
		if (em != null && em.isOpen()) {
			em.close();
		}
	}

	public void retry(Collection<MessageContext> messages, QueueCfg qc) {
		for (final MessageContext msg : messages) {
			msg.setQueueCfg(qc);
			HttpDispatcherActor.instance().handover(msg);
		}
	}

	public void handle(final Collection<MessageContext> mcs) {
		if (CollectionUtils.isEmpty(mcs)) return;
		
		em = JpaModule.getEntityManager();
		
		final EntityTransaction transaction = em.getTransaction();
		try {
			if (!transaction.isActive()) {
				transaction.begin();
			} else {
				em.joinTransaction();
			}
			
			for (final MessageContext mc : mcs) {
				if (mc != null) {
					log.info("handle Message: \n" + mc);
				}
				handle(mc);
			}
			em.flush();
			transaction.commit();
		} catch (Exception e) {
			log.error("failed by exception", e);
			transaction.rollback();
		}
	}

	public void handover(final Collection<MessageContext> mcs) {
		buf.addAll(mcs);
	}
}
