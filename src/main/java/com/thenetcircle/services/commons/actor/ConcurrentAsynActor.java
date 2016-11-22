package com.thenetcircle.services.commons.actor;

import java.util.concurrent.ConcurrentLinkedDeque;

public class ConcurrentAsynActor<M> extends AsyncActor<M, ConcurrentLinkedDeque<M>> {

	public ConcurrentAsynActor(ConcurrentLinkedDeque<M> _buf) {
		super(_buf);
	}

	public ConcurrentAsynActor() {
		this(new ConcurrentLinkedDeque<M>());
	}

	protected M poll(ConcurrentLinkedDeque<M> _buf) throws InterruptedException {
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
