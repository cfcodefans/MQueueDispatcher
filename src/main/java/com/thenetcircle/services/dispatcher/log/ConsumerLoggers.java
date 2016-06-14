package com.thenetcircle.services.dispatcher.log;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

import com.thenetcircle.services.dispatcher.entity.ServerCfg;

public class ConsumerLoggers {
	static final Map<String, Logger> serverKeyAndLoggers = new HashMap<String, Logger>();

	public static synchronized void updateLoggerByServerCfg(final ServerCfg sc) {
		//TODO
	}
	
	public static Logger getLoggerByServerCfg(final ServerCfg sc) {
		final String serverKey = sc.getLogFilePath();
		Logger logger = serverKeyAndLoggers.get(serverKey);
		if (logger != null) {
			return logger;
		}

		logger = createLogger(sc);
		serverKeyAndLoggers.put(serverKey, logger);
		return logger;
	}

	private static Logger createLogger(final ServerCfg sc) {
		String logFileName = sc.getLogFilePath();

		final String maxLogSize = sc.getMaxFileSize();

		RollingFileAppender originalLogAppender = (RollingFileAppender) Logger.getRootLogger().getAppender("consumerDispatcherLog");
		PatternLayout srcLayout = (PatternLayout) originalLogAppender.getLayout();
		RollingFileAppender queueLogAppender = new RollingFileAppender();
		queueLogAppender.setEncoding(originalLogAppender.getEncoding());

		try {
			queueLogAppender.setFile(logFileName, originalLogAppender.getAppend(), originalLogAppender.getBufferedIO(), originalLogAppender.getBufferSize());

			queueLogAppender.setErrorHandler(originalLogAppender.getErrorHandler());
			queueLogAppender.setImmediateFlush(originalLogAppender.getImmediateFlush());

			final PatternLayout pl = new PatternLayout(srcLayout.getConversionPattern());
			queueLogAppender.setLayout(pl);
			queueLogAppender.setMaxBackupIndex(originalLogAppender.getMaxBackupIndex());
			queueLogAppender.setMaxFileSize(maxLogSize);

			final String logName = sc.getHost() + "/" + sc.getVirtualHost();
			queueLogAppender.setName(logName);
			queueLogAppender.setThreshold(originalLogAppender.getThreshold());

			final Logger _logger = Logger.getLogger(logName);
			_logger.setAdditivity(false);// don't inherit root's appender
			_logger.removeAllAppenders();// purge other appenders
			_logger.addAppender(queueLogAppender);// use specified appender

			return _logger;
		} catch (IOException e) {
			log.error("failed to create logger for serverCfg: \n" + sc, e);
		}
		return null;
	}

	protected static final Log log = LogFactory.getLog(ConsumerLoggers.class.getName());
	
	public static final void _info(final ServerCfg sc, final String infoStr) {
		final Logger logForSrv = getLoggerByServerCfg(sc);
		if (logForSrv != null) {
			logForSrv.info(infoStr);
		}
	}
	
	public static final void _error(final ServerCfg sc, final String infoStr) {
		final Logger logForSrv = getLoggerByServerCfg(sc);
		if (logForSrv != null) {
			logForSrv.error(infoStr);
		}
	}

	public static final void _error(final ServerCfg sc, final String infoStr, final Throwable t) {
		final Logger logForSrv = getLoggerByServerCfg(sc);
		if (logForSrv != null) {
			logForSrv.error(infoStr, t);
		}
	}
}
