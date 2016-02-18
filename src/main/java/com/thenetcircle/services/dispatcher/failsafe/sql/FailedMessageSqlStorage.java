package com.thenetcircle.services.dispatcher.failsafe.sql;

import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.thenetcircle.services.commons.MiscUtils;
import com.thenetcircle.services.commons.actor.ConcurrentAsynActor;
import com.thenetcircle.services.commons.persistence.jpa.BaseDao;
import com.thenetcircle.services.commons.persistence.jpa.JpaModule;
import com.thenetcircle.services.dispatcher.entity.MessageContext;
import com.thenetcircle.services.dispatcher.entity.MsgResp;
import com.thenetcircle.services.dispatcher.failsafe.IFailsafe;
import com.thenetcircle.services.dispatcher.http.HttpDispatcherActor;

public class FailedMessageSqlStorage  extends ConcurrentAsynActor<MessageContext> implements IFailsafe {

	private static FailedMessageSqlStorage instance = new FailedMessageSqlStorage();
	protected static final Log log = LogFactory.getLog(FailedMessageSqlStorage.class.getSimpleName());

	public static FailedMessageSqlStorage instance() {
		return instance;
	}
	
	private EntityManager em = null;
	
	final ExecutorService executor = Executors.newSingleThreadExecutor(MiscUtils.namedThreadFactory(FailedMessageSqlStorage.class.getSimpleName()));

	private FailedMessageSqlStorage() {
		start();
	}
	
	public void delete(final Long id) {
		final Query q = em.createQuery("delete from MessageContext mc where mc.id=:id");
		q.setParameter("id", id);
		q.executeUpdate();
	}

	public MessageContext handle(final MessageContext mc) {
		if (mc == null) return mc;
		
		try {
			em = JpaModule.getEntityManager();
			
			EntityTransaction transaction = null;
			try {
				transaction = BaseDao.beginTransaction(em);
				_handle(mc);
				transaction.commit();
				em.close();
			} catch (Exception e) {
				log.error("failed by exception", e);
				if (transaction != null && transaction.isActive())
					transaction.setRollbackOnly();
				BaseDao.endTransaction(em);
				
				log.error("close EntityManager");
				em.close();
			}
		} catch (Exception e) {
			if (e instanceof InterruptedException) {
				throw e;
			}
			
			log.error("failed by exception", e);
		}
		return mc;
	}

	public MessageContext _handle(final MessageContext mc) {
		if (mc == null) return mc;
		try {
			if (mc.isSucceeded() && mc.getId() > 0) {
				delete(mc.getId());
				return mc;
			}
			
			mc.fail();
			
			MessageContext _mc = em.find(MessageContext.class, Long.valueOf(mc.getId()));
			if (_mc == null) {
				_mc = em.merge(mc);
				mc.setId(_mc.getId());
			} else {
				_mc.setDelivery(mc.getDelivery());
				_mc.setFailTimes(mc.getFailTimes());
				_mc.setResponse(mc.getResponse());
				em.merge(_mc);
			}
			
			return HttpDispatcherActor.instance().handover(mc);
		} catch (Exception e) {
			log.error("failed to handle: \n\t" + mc, e);
		}
		return mc;
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
		super.stop();
		executor.shutdownNow();
		if (em != null && em.isOpen()) {
			em.close();
		}
	}
}
