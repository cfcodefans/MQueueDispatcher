package com.thenetcircle.services.commons.actor;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

public interface IActor<T> {
    int WAIT_FACTOR = 1;
    TimeUnit WAIT_FACTOR_UNIT = TimeUnit.MILLISECONDS;

    T handover(final T m);

    T handle(final T m);

    void stop();
    boolean isStopped();

    interface IBatchActor<T> extends IActor<T> {
        default void handle(final Collection<T> ms) {
            for (T m : ms) {
                if (isStopped()) return;
                handle(m);
            }
        }

        default void handover(final Collection<T> ms) {
            ms.forEach(this::handover);
        }
    }
}
