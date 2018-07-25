package com.thenetcircle.services.commons.actor;

import com.thenetcircle.services.commons.ProcTrace;
import com.thenetcircle.services.commons.ProcTrace.TraceEntry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public abstract class AsyncActor<M, Q extends Queue<M>> implements IActor<M>, Runnable {

    protected AtomicBoolean stopped = new AtomicBoolean(false);
    private static final Logger log = LogManager.getLogger(AsyncActor.class);

    protected final Q buf;

    protected M poll() throws Exception {
        return buf.poll();
    }

    public Queue<M> getBuf() {
        return buf;
    }

    protected Collection<M> pollBatch(int num) throws Exception {
        if (num <= 0) return Collections.emptyList();
        List<M> reList = new ArrayList<M>();
        for (int i = 0; i < num; i++) {
            reList.add(poll());
        }
        return reList.stream().filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    public void run() {
        if (this instanceof IBatchProvider) {
            processBatch();
            return;
        }
        process();
    }

    protected void process() {
        TraceEntry start = ProcTrace.start(String.format("%s starts", this.getClass().getSimpleName()));
        try {
            while (!(Thread.interrupted() || stopped.get())) {
                handle(poll());
            }
            ProcTrace.end();
        } catch (Exception e) {
            ProcTrace.end(start, e);
            log.error("Responder is interrupted", e);
        }
        stop();

        log.info(ProcTrace.flush());
    }

    protected void processBatch() {
        TraceEntry start = ProcTrace.start(String.format("%s starts", this.getClass().getSimpleName()));
        IBatchProvider<M> bp = (IBatchProvider<M>) this;
        try {
            while (!(Thread.interrupted() || stopped.get())) {
                handle(bp.pollBatch());
            }
            ProcTrace.end();
        } catch (Exception e) {
            ProcTrace.end(start, e);
            log.error("Responder is interrupted", e);
        }
        stop();
        log.info(ProcTrace.flush());
    }

    public interface IBatchProvider<T> {
        Collection<T> pollBatch() throws Exception;
    }

    public M handover(final M m) {
        buf.offer(m);
        return m;
    }

    public void handover(final Collection<M> ms) {
        ms.forEach(buf::offer);
    }

    public AsyncActor(Q buf) {
        super();
        this.buf = buf;
    }

    public void handle(final Collection<M> ms) {
        ms.forEach(this::handle);
    }

    public void stop() {
        stopped.compareAndSet(false, true);
    }

    @Override
    public M handle(final M m) {
        return m;
    }

    @Override
    public boolean isStopped() {
        return stopped.get();
    }
}