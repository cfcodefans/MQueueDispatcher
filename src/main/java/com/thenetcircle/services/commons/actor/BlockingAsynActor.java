package com.thenetcircle.services.commons.actor;

import java.util.concurrent.LinkedBlockingQueue;

public class BlockingAsynActor<M> extends AsyncActor<M, LinkedBlockingQueue<M>> {

	public BlockingAsynActor(LinkedBlockingQueue<M> _buf) {
		super(_buf);
	}
	
	public BlockingAsynActor() {
		super(new LinkedBlockingQueue<M>());
	}

	protected M poll() throws Exception {
		return buf.poll(WAIT_FACTOR, WAIT_FACTOR_UNIT);
	}
	
}
