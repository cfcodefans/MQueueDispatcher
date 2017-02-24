package com.thenetcircle.services.commons.actor;

import java.util.concurrent.ConcurrentLinkedQueue;

public class ConcurrentAsynActor<M> extends AsyncActor<M, ConcurrentLinkedQueue<M>> {

	public ConcurrentAsynActor(ConcurrentLinkedQueue<M> _buf) {
		super(_buf);
	}

	public ConcurrentAsynActor() {
		this(new ConcurrentLinkedQueue<M>());
	}

	protected M poll(ConcurrentLinkedQueue<M> _buf) throws InterruptedException {
		M polled = _buf.poll();
		if (polled != null) {
			return polled;
		}

		Thread.sleep(1);
		return null;
	}
	
	protected M poll() throws InterruptedException {
		return poll(super.buf);
	}
}
