package com.thenetcircle.services.dispatcher.mgr;

import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.glassfish.jersey.media.sse.SseBroadcaster;

import com.thenetcircle.services.dispatcher.IMessageActor;
import com.thenetcircle.services.dispatcher.entity.MessageContext;

public class Monitor implements IMessageActor, Runnable {

	private static Monitor instance = new Monitor();
	
	public Monitor() {
		executor.submit(this);
	}
	
	public static Monitor instance() {
		return instance;
	}
	
	protected static final Log log = LogFactory.getLog(Monitor.class.getName());
	private BlockingQueue<MessageContext> buf = new LinkedBlockingQueue<MessageContext>();
	private ExecutorService executor = Executors.newSingleThreadExecutor();

	@Override
	public void run() {
		try {
			while (!Thread.interrupted()) {
				handle(buf.poll(WAIT_FACTOR, WAIT_FACTOR_UNIT));
			}
		} catch (Exception e) {
			log.error("Responder is interrupted", e);
		}
		log.info("Responder quits");
	}

	@Override
	public MessageContext handover(MessageContext mc) {
		buf.offer(mc); 
		return mc;
	}

	@Override
	public void handover(Collection<MessageContext> mcs) {
		
	}

	@Override
	public void handle(Collection<MessageContext> mcs) {
		
	}

	@Override
	public MessageContext handle(MessageContext mc) {
		return null;
	}

	@Override
	public void stop() {
		executor.shutdownNow();		
	}
	
	private  SseBroadcaster broadcaster = new SseBroadcaster();

	public SseBroadcaster getBroadcaster() {
		return broadcaster;
	}
}
