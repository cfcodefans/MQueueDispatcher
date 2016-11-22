package com.thenetcircle.services.dispatcher.log;

import com.thenetcircle.services.dispatcher.entity.ServerCfg;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.rolling.RolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.SizeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.xml.XmlConfiguration;

import java.util.HashMap;
import java.util.Map;

public class ConsumerLoggers {
    static final Map<String, Logger> serverKeyAndLoggers = new HashMap<String, Logger>();

    public static Logger getLoggerByServerCfg(final ServerCfg sc) {
        return serverKeyAndLoggers.computeIfAbsent(sc.getLogFilePath(), (String key) -> createLogger(sc));
    }

    private static final String APPENDER_NAME = "consumerDispatcherLog";

    private static Logger createLogger(final ServerCfg sc) {
        String logFileName = sc.getLogFilePath();
        final String maxLogSize = sc.getMaxFileSize();
        final String logName = sc.getHost() + "/" + sc.getVirtualHost();

        LoggerContext context = (LoggerContext) LogManager.getContext();
        ConfigurationSource cfgSrc = context.getConfiguration().getConfigurationSource();
        XmlConfiguration xmlCfg = new XmlConfiguration(context, cfgSrc);
        RollingFileAppender _appender = xmlCfg.getAppender(APPENDER_NAME);

        RolloverStrategy _rolloverStrategy = _appender.getManager().getRolloverStrategy();

        RollingFileAppender __appender = RollingFileAppender.newBuilder()
            .withConfiguration(xmlCfg)
            .withBufferedIo(true)
            .withLayout(_appender.getLayout())
            .withPolicy(SizeBasedTriggeringPolicy.createPolicy(maxLogSize))
            .withStrategy(_rolloverStrategy)
            .withFileName(logFileName)
            .withName(logName)
            .build();

        xmlCfg.getAppenders().keySet().forEach(xmlCfg::removeAppender);
        xmlCfg.addAppender(__appender);

        LoggerContext lc = new LoggerContext(logName);
        lc.start(xmlCfg);
        return lc.getLogger(logName);
    }

    protected static final Logger log = LogManager.getLogger(ConsumerLoggers.class.getName());

    public static final void _info(final ServerCfg sc, final String infoStr) {
        final Logger logForSrv = getLoggerByServerCfg(sc);
        if (logForSrv != null) {
            logForSrv.info(infoStr);
        }
    }

    public static final void _info(final ServerCfg sc, final String format, Object... args) {
        _info(sc, String.format(format, args));
    }

    public static final void _info(final Logger _log, final ServerCfg sc, final String infoStr) {
        _log.info(infoStr);
        _info(sc, infoStr);
    }

    public static final void _info(final Logger _log, final ServerCfg sc, final String format, Object... args) {
        _info(_log, sc, String.format(format, args));
    }

    public static final void _error(final ServerCfg sc, final String infoStr) {
        final Logger logForSrv = getLoggerByServerCfg(sc);
        if (logForSrv != null) {
            logForSrv.error(infoStr);
        }
    }

    public static final void _error(final ServerCfg sc, final String format, final Object... args) {
        final Logger logForSrv = getLoggerByServerCfg(sc);
        if (logForSrv != null) {
            logForSrv.error(String.format(format, args));
        }
    }

    public static final void _error(final Logger _log, final ServerCfg sc, final String infoStr) {
        _log.error(infoStr);
        _error(sc, infoStr);
    }

    public static final void _error(final ServerCfg sc, final String infoStr, final Throwable t) {
        final Logger logForSrv = getLoggerByServerCfg(sc);
        if (logForSrv != null) {
            logForSrv.error(infoStr, t);
        }
    }

    public static final void _error(final Logger _log, final ServerCfg sc, final String infoStr, final Throwable t) {
        _log.error(infoStr, t);
        _error(sc, infoStr, t);
    }
}
