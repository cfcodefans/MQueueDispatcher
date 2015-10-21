package com.thenetcircle.services.commons.actor;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

public interface IActor<T> {
	static final int		WAIT_FACTOR			= 1;
	static final TimeUnit	WAIT_FACTOR_UNIT	= TimeUnit.MILLISECONDS;

	T handover(final T m);

	void handover(final Collection<T> ms);

	void handle(final Collection<T> ms);

	T handle(final T m);

	void stop();

	boolean isStopped();
}
