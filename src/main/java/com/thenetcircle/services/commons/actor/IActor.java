package com.thenetcircle.services.commons.actor;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

public interface IActor<T> {
	static final int		WAIT_FACTOR			= 1;
	static final TimeUnit	WAIT_FACTOR_UNIT	= TimeUnit.MILLISECONDS;

	T handover(final T m);

//	void handover(final Collection<T> ms);

//	void handle(final Collection<T> ms);

	T handle(final T m);

	void stop();

	boolean isStopped();
	
	public interface IBatchActor<T> extends IActor<T> {
		default void handle(final Collection<T> ms) {
			for (T m : ms) {
				if (isStopped()) return;
				handle(m);
			}
		}
		
		default void handover(final Collection<T> ms) {
			ms.forEach(m -> handover(m));
		}
	}
}
