package com.thenetcircle.services.dispatcher.failsafe.sql;

import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import javax.persistence.EntityManager;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.thenetcircle.services.dispatcher.ampq.MQueues;
import com.thenetcircle.services.dispatcher.entity.MessageContext;
import com.thenetcircle.services.dispatcher.entity.QueueCfg;
import com.thenetcircle.services.dispatcher.failsafe.IFailedMessageManagment;
import com.thenetcircle.services.dispatcher.failsafe.IFailsafe;
import com.thenetcircle.services.dispatcher.http.HttpDispatcherActor;

public class FailedMessageSqlStorage implements Runnable, IFailsafe, IFailedMessageManagment {

	protected static final Log log = LogFactory.getLog(FailedMessageSqlStorage.class.getSimpleName());
	private static FailedMessageSqlStorage instance = new FailedMessageSqlStorage();

	private BlockingQueue<MessageContext> buf = new LinkedBlockingQueue<MessageContext>();
	final ExecutorService executor = Executors.newSingleThreadExecutor();
	
	private EntityManager em = null;

	public MessageContext handle(final MessageContext mc) {
		if (mc == null) return mc;
		try {
			MessageContext _mc = em.find(MessageContext.class, Long.valueOf(mc.getId()));
			if (_mc == null) {
				_mc = mc;
			}
			_mc.failAgain();
			MQueues.getInstance().reject(mc, !_mc.isExceedFailTimes());
			return em.merge(_mc);
		} catch (Exception e) {
			log.error("failed to handle: \n\t" + mc, e);
		}
		return mc;
	}
	
	private FailedMessageSqlStorage() {
		executor.submit(this);
	}

	public static FailedMessageSqlStorage getInstance() {
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
		buf.offer(mc);
		return mc;
	}

	public void start() {
		executor.submit(this);
	}

	public void stop() {
		executor.shutdownNow();
	}

	public void retry(Criterion c) {
		
	}

	public void retry(Collection<MessageContext> messages, QueueCfg qc) {
		for (final MessageContext msg : messages) {
			msg.setQueueCfg(qc);
			HttpDispatcherActor.instance().handover(msg);
		}
	}

	public Collection<MessageContext> query(final Criterion c) {
		//TODO
		return null;
	}

	public void handle(final Collection<MessageContext> mcs) {
		if (CollectionUtils.isEmpty(mcs)) return;
		
		em.getTransaction().begin();
		for (final MessageContext mc : mcs) {
			handle(mc);
		}
		em.getTransaction().commit();
	}

	public void handover(final Collection<MessageContext> mcs) {
		buf.addAll(mcs);
	}
}
