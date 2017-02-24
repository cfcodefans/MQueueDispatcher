package com.thenetcircle.services.dispatcher.ampq;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;
import com.thenetcircle.services.dispatcher.entity.QueueCfg;
import com.thenetcircle.services.dispatcher.entity.ServerCfg;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Objects;

import static com.thenetcircle.services.dispatcher.log.ConsumerLoggers._info;

class QueueCtx implements ShutdownListener {
	public ConsumerActor			ca;
	public Channel					ch;
	public NamedConnection			nc;
	public QueueCfg					qc;
	protected static final Logger log	= LogManager.getLogger(QueueCtx.class);

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof QueueCtx))
			return false;
		QueueCtx other = (QueueCtx) obj;
		if (qc == null) {
			if (other.qc != null)
				return false;
		} else if (!qc.equals(other.qc))
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(qc);
	}

	public void shutdownCompleted(final ShutdownSignalException cause) {
		// final Object ref = cause.getReference();
		final Object reason = cause.getReason();
		final ServerCfg sc = qc.getServerCfg();

		log.error("shutdown happens!!!", cause);

		if ((reason instanceof AMQP.Connection.Close)) {
			AMQP.Connection.Close close = (AMQP.Connection.Close) reason;
			if (AMQP.CONNECTION_FORCED == close.getReplyCode() && "OK".equals(close.getReplyText())) {
				final String infoStr = String.format("\n close connection to server: \n\t %s", sc);
				log.error(infoStr);
				_info(sc, infoStr);
				return;
			}
		}

		if ((reason instanceof AMQP.Channel.Close)) {
			AMQP.Channel.Close close = (AMQP.Channel.Close) reason;
			if (AMQP.CONNECTION_FORCED == close.getReplyCode() && "OK".equals(close.getReplyText())) {
				final String infoStr = String.format("\n close channel to server: \n\t %s", sc);
				log.error(infoStr);
				_info(sc, infoStr);
				return;
			}
		}

		if (cause.isHardError()) {
			final String infoStr = String.format("\n unexpected shutdown on connection to server: \n\t %s \n\n\t", sc, cause.getCause());
			log.error(infoStr);
			_info(sc, infoStr);
			return;
		}

		MQueueMgr qm = MQueueMgr.instance();
		qm.stopQueue(qc);
	}
}