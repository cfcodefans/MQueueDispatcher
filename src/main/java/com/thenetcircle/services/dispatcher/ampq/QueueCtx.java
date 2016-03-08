package com.thenetcircle.services.dispatcher.ampq;

import java.util.Objects;

import org.apache.log4j.Logger;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;
import com.thenetcircle.services.dispatcher.entity.QueueCfg;
import com.thenetcircle.services.dispatcher.entity.ServerCfg;
import static com.thenetcircle.services.dispatcher.log.ConsumerLoggers.*;

class QueueCtx implements ShutdownListener {
	public ConsumerActor			ca;
	public Channel					ch;
	public NamedConnection			nc;
	public QueueCfg					qc;
	protected static final Logger	log	= Logger.getLogger(QueueCtx.class);

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
		final Object reasonObj = cause.getReason();
		final ServerCfg sc = qc.getServerCfg();

		if ((reasonObj instanceof AMQP.Connection.Close)) {
			AMQP.Connection.Close close = (AMQP.Connection.Close) reasonObj;
			if (AMQP.CONNECTION_FORCED == close.getReplyCode() && "OK".equals(close.getReplyText())) {
				final String infoStr = String.format("\n close connection to server: \n\t %s", sc);
				log.error(infoStr);
				_info(sc, infoStr);
				return;
			}
		}

		if ((reasonObj instanceof AMQP.Channel.Close)) {
			AMQP.Channel.Close close = (AMQP.Channel.Close) reasonObj;
			if (AMQP.CONNECTION_FORCED == close.getReplyCode() && "OK".equals(close.getReplyText())) {
				final String infoStr = String.format("\n close connection to server: \n\t %s", sc);
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

		MQueueMgr.instance().stopQueue(qc);
		MQueueMgr.instance().reconnActor.addReconnect(qc);
	}
}