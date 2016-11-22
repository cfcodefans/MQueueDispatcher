package com.thenetcircle.services.dispatcher.mgr;

import com.thenetcircle.services.commons.MiscUtils;
import com.thenetcircle.services.commons.actor.ConcurrentAsynActor;
import com.thenetcircle.services.commons.actor.IActor;
import com.thenetcircle.services.dispatcher.entity.MessageContext;
import com.thenetcircle.services.dispatcher.entity.QueueCfg;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MsgMonitor extends ConcurrentAsynActor<MessageContext> {

    private static MsgMonitor instance = new MsgMonitor();

    public MsgMonitor() {
        super();
        executor.submit(this);
    }

    public static MsgMonitor instance() {
        return instance;
    }

    protected static final Logger log = LogManager.getLogger(MsgMonitor.class);
    private ExecutorService executor = Executors.newSingleThreadExecutor(MiscUtils.namedThreadFactory("Monitor"));

    @Override
    public MessageContext handle(final MessageContext mc) {
        if (mc == null) {
            return mc;
        }

        final IActor<MessageContext> monitor = queueAndMonitors.get(mc.getQueueCfg());
        return (monitor != null ? monitor.handover(mc) : mc);
    }

    @Override
    public void stop() {
        super.stop();
        executor.shutdownNow();
    }

    private Map<QueueCfg, IActor<MessageContext>> queueAndMonitors = new ConcurrentHashMap<QueueCfg, IActor<MessageContext>>();

    public IActor<MessageContext> register(final QueueCfg qc, final IActor<MessageContext> monitor) {
        if (qc == null || monitor == null) {
            return null;
        }

        return queueAndMonitors.computeIfAbsent(qc, (QueueCfg _qc) -> monitor);
    }

    public IActor<MessageContext> getQueueMonitor(final QueueCfg qc) {
        return queueAndMonitors.get(qc);
    }

    public IActor<MessageContext> unregister(final QueueCfg qc) {
        IActor<MessageContext> removed = queueAndMonitors.remove(qc);
        if (removed != null) {
            removed.stop();
        }
        return removed;
    }

    public static void prefLog(final MessageContext mc, final Logger _log, String... infos) {
        final long dt = mc.getId();
        if (dt % 100 == 11) {
            _log.info("http performance: \t" + dt + " at \t" + System.currentTimeMillis());
        }
    }
}
