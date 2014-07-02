package com.thenetcircle.services.dispatcher.log;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

import com.thenetcircle.services.dispatcher.entity.ServerCfg;

public class ConsumerLoggers {
	static final Map<String, Logger> serverKeyAndLoggers = new HashMap<String, Logger>();

	public static Logger getLoggerByQueueConf(final ServerCfg sc) {
		final String serverKey = sc.getHost() + "_" + sc.getUserName();
		final Logger logger = serverKeyAndLoggers.get(serverKey);
		if (logger != null) {
			return logger;
		}

		String logFileName = sc.getLogFilePath();
		if (StringUtils.isBlank(logFileName)) {
			logFileName = serverKey;
		}

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

			queueLogAppender.setName(serverKey);
			queueLogAppender.setThreshold(originalLogAppender.getThreshold());

			final Logger _logger = Logger.getLogger(serverKey);
			_logger.setAdditivity(false);// don't inherit root's appender
			_logger.removeAllAppenders();// purge other appenders
			_logger.addAppender(queueLogAppender);// use specified appender

			serverKeyAndLoggers.put(serverKey, _logger);
			return _logger;
		} catch (IOException e) {
			log.error("failed to create logger for serverCfg: \n" + sc, e);
		}
		return logger;
	}

	protected static final Log log = LogFactory.getLog(ConsumerLoggers.class.getName());
}
