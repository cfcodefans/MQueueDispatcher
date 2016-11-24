package com.thenetcircle.services.dispatcher.ampq;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.AMQP.Exchange.DeleteOk;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.thenetcircle.services.cluster.JGroupsActor;
import com.thenetcircle.services.commons.Lambdas;
import com.thenetcircle.services.commons.MiscUtils;
import com.thenetcircle.services.commons.ProcTrace;
import com.thenetcircle.services.commons.persistence.jpa.JpaModule;
import com.thenetcircle.services.dispatcher.dao.QueueCfgDao;
import com.thenetcircle.services.dispatcher.entity.ExchangeCfg;
import com.thenetcircle.services.dispatcher.entity.MessageContext;
import com.thenetcircle.services.dispatcher.entity.QueueCfg;
import com.thenetcircle.services.dispatcher.entity.QueueCfg.Status;
import com.thenetcircle.services.dispatcher.entity.ServerCfg;
import com.thenetcircle.services.dispatcher.failsafe.sql.FailedMessageSqlStorage;
import com.thenetcircle.services.dispatcher.http.HttpDispatcherActor;
import com.thenetcircle.services.dispatcher.log.ConsumerLoggers;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.thenetcircle.services.commons.Lambdas.falseSupplier;
import static com.thenetcircle.services.commons.MiscUtils.AVAILABLE_PROCESSORS;
import static com.thenetcircle.services.dispatcher.log.ConsumerLoggers._error;
import static com.thenetcircle.services.dispatcher.log.ConsumerLoggers._info;

public class MQueueMgr {

    // public static final Thread cleaner = new
    // Thread(MQueueMgr.instance()::shutdown);

    private static final ExecutorService EXECUTORS = Executors.newFixedThreadPool(AVAILABLE_PROCESSORS * 2,
        MiscUtils.namedThreadFactory(MQueueMgr.class.getSimpleName()));

    public static int NUM_CHANNEL_PER_CONN = 2;
    protected static final Logger log = LogManager.getLogger(MQueueMgr.class);

    static MQueueMgr instance = new MQueueMgr();

    public static MQueueMgr instance() {
        return instance;
    }

    private Map<QueueCfg, QueueCtx> cfgAndCtxs = new HashMap<>();

    private Map<ServerCfg, ConnectionFactory> connFactories = new HashMap<ServerCfg, ConnectionFactory>();

    private ScheduledExecutorService reconnActorThread = Executors.newSingleThreadScheduledExecutor();

    private Map<ServerCfg, Set<NamedConnection>> serverCfgAndConns = new HashMap<ServerCfg, Set<NamedConnection>>();

    public MQueueMgr() {
        NUM_CHANNEL_PER_CONN = (int) MiscUtils.getPropertyNumber("channel.number.connection", NUM_CHANNEL_PER_CONN);
        initActors();
    }

    public MessageContext acknowledge(final MessageContext mc) {
        if (mc == null || mc.getDelivery() == null) {
            return mc;
        }

        final long deliveryTag = mc.getDelivery().getEnvelope().getDeliveryTag();
        final QueueCfg qc = mc.getQueueCfg();

        try {
            final QueueCtx queueCtx = cfgAndCtxs.get(qc);
            if (queueCtx == null) {
                final String infoStr = "can't acknowledge the message as channel is not created!\n\t" + qc;
                log.error(infoStr);
                _error(qc.getServerCfg(), infoStr);
                return mc;
            }

            final Channel ch = queueCtx.ch;
            if (!ch.isOpen()) {
                final String infoStr = "can't acknowledge the message as channel is not opened!\n\t" + qc;
                log.error(infoStr);
                _error(qc.getServerCfg(), infoStr);
                return mc;
            }
            ch.basicAck(deliveryTag, false);
            _info(qc.getServerCfg(), "the result of job: " + deliveryTag + " for q " + qc.getName() + " on server " + qc.getServerCfg().getVirtualHost() + "\nresponse: " + mc.getResponse());
        } catch (final IOException e) {
            final String infoStr = "failed to acknowledge message: \n" + deliveryTag + "\nresponse: " + mc.getResponse();
            _error(log, qc.getServerCfg(), infoStr, e);
        }

        return mc;
    }

    public Channel getChannel(final QueueCfg qc) {
        final QueueCtx queueCtx = cfgAndCtxs.get(qc);
        return queueCtx != null ? queueCtx.ch : null;
    }

    public synchronized ConnectionFactory getConnFactory(final ServerCfg sc) {
        if (sc == null) {
            return null;
        }

        return connFactories.computeIfAbsent(sc, (_sc) -> {
            ConnectionFactory connFactory = initConnFactory(_sc);
            if (connFactory == null) {
                log.error("fail to create ConnectionFactory: \n\t" + sc);
                _error(sc, "fail to create ConnectionFactory: \n\t" + sc);
            }
            return connFactory;
        });
    }

    public Collection<QueueCfg> getQueueCfgs() {
        return cfgAndCtxs.keySet();
    }

    public boolean ifExchangeExists(ExchangeCfg ec) {
        if (ec == null || ec.getServerCfg() == null || StringUtils.isBlank(ec.getExchangeName()))
            return false;

        return operate(ec.getServerCfg(), Lambdas.wf((Channel ch) -> {
            ch.exchangeDeclarePassive(ec.getExchangeName());
            return Boolean.TRUE;
        }, falseSupplier, log::error));
    }

    public boolean ifQueueExists(QueueCfg qc) {
        if (qc == null || qc.getServerCfg() == null || StringUtils.isBlank(qc.getQueueName()))
            return false;

        return operate(qc.getServerCfg(), Lambdas.wf((Channel ch) -> {
            ch.queueDeclarePassive(qc.getQueueName());
            return Boolean.TRUE;
        }, falseSupplier, log::error));
    }

    public boolean isQueueRunning(final QueueCfg qc) {
        final QueueCtx queueCtx = cfgAndCtxs.get(qc);
        return queueCtx != null && queueCtx.ch != null && queueCtx.ch.isOpen();
    }

    public <R> R operate(final ServerCfg sc, Function<Channel, R> fn) {
        if (sc == null || fn == null) {
            return null;
        }

        Connection conn = null;
        Channel ch = null;
        try {
            try {
                conn = getConnFactory(sc).newConnection();
                ch = conn.createChannel();
                return fn.apply(ch);
            } finally {
                ch.close();
                conn.close();
            }
        } catch (IOException | TimeoutException e) {
            log.error("", e);
            return null;
        }
    }

    public synchronized void shutdown() {
        log.info(MiscUtils.invocationInfo());
        shutdownActors();
    }

    public synchronized QueueCfg startQueue(final QueueCfg qc) {
        ProcTrace.start();

        if (cfgAndCtxs.containsKey(qc)) {
            stopQueue(qc);
        }

        final ServerCfg sc = qc.getServerCfg();
        Set<NamedConnection> connSet = serverCfgAndConns.computeIfAbsent(sc, (_sc) -> new LinkedHashSet<NamedConnection>());
        NamedConnection nc = null;
        if (CollectionUtils.isNotEmpty(connSet)) {
            Optional<NamedConnection> it = connSet.stream().filter(_nc -> _nc.qcSet.size() < NUM_CHANNEL_PER_CONN).findFirst();
            nc = it.orElse(null);
        }

        try {
            if (nc == null) {
                nc = newNamedConn(sc);
                connSet.add(nc);
            }

            final QueueCtx queueCtx = new QueueCtx();
            final Channel ch = nc.conn.createChannel();

            ch.addShutdownListener(queueCtx);

            String queueName = qc.getQueueName();
            String routeKey = qc.getRouteKey();

            ch.queueDeclare(queueName, qc.isDurable(), qc.isExclusive(), qc.isAutoDelete(), null);
            ProcTrace.ongoing("declare channel for queue: " + queueName);

            if (qc.getPrefetchSize() != null) {
                ch.basicQos(qc.getPrefetchSize());
                ProcTrace.ongoing(String.format("set prefetch size: %d for queue: %s", qc.getPrefetchSize(), queueName));
            }
            for (final ExchangeCfg ec : qc.getExchanges()) {
                String exchangeName = StringUtils.defaultString(ec.getExchangeName(), StringUtils.EMPTY);
                ch.exchangeDeclare(exchangeName, ec.getType(), ec.isDurable(), ec.isAutoDelete(), null);
                ch.queueBind(queueName, exchangeName, routeKey);
                ProcTrace.ongoing(String.format("declare and bind exchange: %s for queue: %s", exchangeName, queueName));
            }

            final ConsumerActor ca = new ConsumerActor(ch, qc);
            ch.basicConsume(queueName, false, ca);
            ProcTrace.ongoing(String.format("Consumer registered for queue: %s", queueName));

            queueCtx.ca = ca;
            queueCtx.qc = qc;
            queueCtx.ch = ch;

            queueCtx.nc = nc;
            nc.qcSet.add(qc);

            qc.setStatus(Status.RUNNING);

            cfgAndCtxs.put(qc, queueCtx);
            ProcTrace.ongoing(String.format("queue: %s started", queueName));
        } catch (Throwable e) {
            final String infoStr = "failed to start queue: \n\t" + qc;
            _error(log, sc, infoStr, e);
            qc.setStatus(Status.STARTED);
        } finally {
            updateDatabase(qc);
            ProcTrace.end();
            log.info(ProcTrace.flush());
        }
        return qc;
    }

    private NamedConnection newNamedConn(final ServerCfg sc) {
        ProcTrace.start();
        NamedConnection nc;
        try {
            nc = new NamedConnection();
            nc.name = String.format("conn-%s-%s-%s", sc.getHost(), sc.getUserName(), UUID.randomUUID());
            nc.conn = getConnFactory(sc).newConnection(EXECUTORS);
            nc.conn.addShutdownListener(nc);
            nc.sc = sc;

            String logStr = String.format("created connection:\n\t%s to server:\n\t%s", sc, nc);
            _info(log, sc, logStr);
            ProcTrace.ongoing(logStr);
            return nc;
        } catch (IOException | TimeoutException e) {
            _error(log, sc, "failed to create named connection", e);
            return null;
        } finally {
            ProcTrace.end();
        }
    }

    public List<QueueCfg> startQueues(final List<QueueCfg> qcList) {
        qcList.forEach(this::startQueue);
        return qcList;
    }

    public synchronized QueueCfg stopQueue(final QueueCfg qc) {
        if (qc == null || !cfgAndCtxs.containsKey(qc)) {
            return qc;
        }
        ProcTrace.start();

        final QueueCtx queueCtx = cfgAndCtxs.get(qc);
        final Channel ch = queueCtx.ch;

        _info(log, qc.getServerCfg(), "going to remove queue:\n\t" + qc);
        try {
            disconnect(qc, ch);
            ProcTrace.ongoing("disconnect for queue: " + qc.getName());

            _info(log, qc.getServerCfg(), "removed queue:\n\t" + qc.getQueueName());
            final NamedConnection nc = queueCtx.nc;
            if (nc == null) {
                qc.setStatus(Status.STOPPED);
                cfgAndCtxs.remove(qc);
                return qc;
            }

            queueCtx.nc = null;
            nc.qcSet.remove(qc);

            cleanNamedConnection(nc);
            qc.setStatus(Status.STOPPED);
            cfgAndCtxs.remove(qc);
        } catch (final Exception e) {
            final String infoStr = "failed to shut down Queue: \n" + qc.getQueueName();
            log.error(infoStr, e);
            _error(qc.getServerCfg(), infoStr, e);
        } finally {
            updateDatabase(qc);
            ProcTrace.end();
        }

        return qc;
    }

    protected void disconnect(final QueueCfg qc, final Channel ch) {
        ProcTrace.start();
        try {
            if (ch != null && ch.isOpen()) {
                String queueName = qc.getQueueName();
                String routeKey = qc.getRouteKey();

                for (final ExchangeCfg ec : qc.getExchanges()) {
                    String exchangeName = StringUtils.defaultString(ec.getExchangeName(), StringUtils.EMPTY);

                    if (ifExchangeExists(ec) && ifQueueExists(qc)) {
                        _info(qc.getServerCfg(), String.format("disconnect exchange:\n%s to queue:\n%s", ec, qc));
                        ch.queueUnbind(queueName, exchangeName, routeKey);
                        ch.exchangeDelete(exchangeName, true);
                    }

                    ProcTrace.ongoing("unbind exchange: " + exchangeName);
                }

                ch.close(AMQP.CONNECTION_FORCED, "OK");
                ProcTrace.ongoing("close channel for queue: " + qc.getName());
            }
        } catch (Exception e) {
            log.error("what is up?", e);
        } finally {
            ProcTrace.end();
        }
    }

    public void updateExchange(ExchangeCfg ex) throws Exception {
        if (ex == null)
            return;
        ServerCfg sc = ex.getServerCfg();
        if (sc == null)
            return;
        Set<QueueCfg> queues = ex.getQueues();
        if (CollectionUtils.isEmpty(queues))
            return;

        queues.forEach(this::stopQueue);

        operate(sc, (Channel ch) -> {
            try {
                DeleteOk del = ch.exchangeDelete(ex.getExchangeName());
                log.info(del);
            } catch (Exception e) {
                log.error("failed to update Exchange:\n" + ex, e);
                return e;
            }
            return null;
        });

        queues.forEach(this::startQueue);
    }

    public void updateQueueCfg(final QueueCfg qc) {
        startQueue(qc);
    }

    public void updateServerCfg(final ServerCfg edited) {
        if (edited == null) {
            return;
        }

        final List<QueueCfg> qcs = cfgAndCtxs.keySet().stream().filter(qc -> edited.equals(qc.getServerCfg())).collect(Collectors.toList());
        qcs.forEach(this::updateQueueCfg);
        JGroupsActor.instance().restartQueues(qcs.toArray(new QueueCfg[0]));
    }

    private void cleanNamedConnection(final NamedConnection nc) throws IOException {
        if (CollectionUtils.isNotEmpty(nc.qcSet)) {
            return;
        }

        final Set<NamedConnection> conns = serverCfgAndConns.get(nc.sc);
        if (conns != null) {
            conns.remove(nc);
            if (conns.isEmpty()) {
                serverCfgAndConns.remove(nc.sc);
            }
        }

        try {
            if (nc.conn.isOpen()) {
                nc.conn.close(AMQP.CONNECTION_FORCED, "OK");
                log.info(String.format("closed connection:\t%s", nc));
            }
        } catch (Exception e) {
            log.error("what is up?", e);
        }
    }

    private void initActors() {
        Responder.instance();
        HttpDispatcherActor.instance();
        FailedMessageSqlStorage.instance();
    }

    private synchronized ConnectionFactory initConnFactory(final ServerCfg sc) {
        if (sc == null)
            return null;

        final Logger logForSrv = ConsumerLoggers.getLoggerByServerCfg(sc);

        String infoStr = String.format("initiating ConnectionFactory with ServerCfg: \n%s", sc);
        log.info(infoStr);
        logForSrv.info(infoStr);

        final ConnectionFactory cf = new ConnectionFactory();
        cf.setHost(sc.getHost());
        cf.setVirtualHost(sc.getVirtualHost());
        cf.setPort(sc.getPort());
        cf.setUsername(sc.getUserName());
        cf.setPassword(sc.getPassword());

        cf.setAutomaticRecoveryEnabled(true);
        cf.setTopologyRecoveryEnabled(true);

        infoStr = String.format("ConnectionFactory is instantiated with \n%s", sc.toString());
        log.info(infoStr);
        logForSrv.info(infoStr);

        return cf;
    }

    private void shutdownActors() {
        reconnActorThread.shutdownNow();
        Responder.stopAll();
        HttpDispatcherActor.instance().stop();
        FailedMessageSqlStorage.instance().stop();
    }

    private void updateDatabase(final QueueCfg qc) {
        @SuppressWarnings("resource")
        final QueueCfgDao qcDao = new QueueCfgDao(JpaModule.getEntityManager());
        try {
            qcDao.beginTransaction();
            qcDao.edit(qc);
            qcDao.endTransaction();
        } catch (Exception e) {
            log.error("failed to update \n" + qc, e);
        }
    }
}
