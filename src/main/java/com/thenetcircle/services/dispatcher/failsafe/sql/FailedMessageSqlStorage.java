package com.thenetcircle.services.dispatcher.failsafe.sql;

import java.util.Arrays;
import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.thenetcircle.services.commons.MiscUtils;
import com.thenetcircle.services.commons.persistence.jpa.JpaModule;
import com.thenetcircle.services.dispatcher.entity.MessageContext;
import com.thenetcircle.services.dispatcher.entity.MsgResp;
import com.thenetcircle.services.dispatcher.entity.QueueCfg;
import com.thenetcircle.services.dispatcher.failsafe.IFailsafe;
import com.thenetcircle.services.dispatcher.http.HttpDispatcherActor;

public class FailedMessageSqlStorage implements Runnable, IFailsafe {

	protected static final Log log = LogFactory.getLog(FailedMessageSqlStorage.class.getSimpleName());
	private static FailedMessageSqlStorage instance = new FailedMessageSqlStorage();

	private Queue<MessageContext> buf = new ConcurrentLinkedQueue<MessageContext>();
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
			if (mc.isSucceeded() && mc.getId() > 0) {
				delete(mc.getId());
				return mc;
			}
			
			mc.fail();
			
			if (mc.getId() > 0) {
				final Query q = em.createQuery("update MessageContext mc set "
						+ " mc.failTimes=:failTimes, "
						+ " mc.response.statusCode=:statusCode, "
						+ " mc.response.responseStr=:responseStr, "
						+ " mc.timestamp=:_timestamp "
						+ " where mc.id=:id");
				q.setParameter("failTimes", Long.valueOf(mc.getFailTimes()));
				final MsgResp resp = mc.getResponse();
				if (resp != null) {
					q.setParameter("statusCode", resp.getStatusCode());
					q.setParameter("responseStr", resp.getResponseStr());
				} 
				
				q.setParameter("_timestamp", Long.valueOf(mc.getTimestamp()));
				q.setParameter("id", Long.valueOf(mc.getId()));
				
			    final int updatedFailMsg = q.executeUpdate();
			    
				log.info("update failed job: " + updatedFailMsg);
				if (updatedFailMsg <= 0) {
					final MessageContext merge = em.merge(mc);
					em.flush();
					mc.setFailTimes(merge.getFailTimes());
					mc.setId(merge.getId());
				}
			} else {
				final MessageContext merge = em.merge(mc);
				em.flush();
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
				
//				final List<MessageContext> mcList = new ArrayList<MessageContext>(100);
//				
//				MessageContext mc = null;
//				for (int i = 0; i < 50; i++) {
//					mc = buf.poll(WAIT_FACTOR, WAIT_FACTOR_UNIT);
//					if (mc == null) {
//						break;
//					}
//					mcList.add(mc);
//				}
				
//				handle(Utils.pull(buf, 100));
//				log.info(String.format("polled %d failed messages......", mcList.size()));
//				handle(mcList);
				MessageContext polled = buf.poll();
				if (polled != null) {
					handle(Arrays.asList(polled));
				} else {
					Thread.sleep(1);
				}
			}
		} 
		catch (InterruptedException e) {
			log.error("Interrupted during waiting for new failed job", e);
		} 
		catch (final Exception e) {
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
		
		try {
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
//				em.flush();
				transaction.commit();
				em.close();
			} catch (Exception e) {
				log.error("failed by exception", e);
				if (transaction.isActive()) {
					log.error("rollback transaction");
					transaction.rollback();
				}
				log.error("close EntityManager");
				em.close();
			}
		} catch (Exception e) {
			if (e instanceof InterruptedException) {
				throw e;
			}
			
			log.error("failed by exception", e);
		}
	}

	public void handover(final Collection<MessageContext> mcs) {
		buf.addAll(mcs);
	}
}
