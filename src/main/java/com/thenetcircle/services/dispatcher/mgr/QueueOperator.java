package com.thenetcircle.services.dispatcher.mgr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.rabbitmq.client.AMQP.Queue;
import com.rabbitmq.client.AMQP.Queue.PurgeOk;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.GetResponse;
import com.thenetcircle.services.dispatcher.ampq.MQueues;
import com.thenetcircle.services.dispatcher.entity.ExchangeCfg;
import com.thenetcircle.services.dispatcher.entity.QueueCfg;

public class QueueOperator {

	private static final Log log = LogFactory.getLog(QueueOperator.class.getName());

	private QueueCfg queueCfg;

	public QueueOperator(QueueCfg queueCfg) {
		super();
		this.queueCfg = queueCfg;
	}

	public long getTotalMessageCount() {
		Channel ch = MQueues.instance().getChannel(queueCfg);

		Queue.DeclareOk re = null;
		try {
			re = ch.queueDeclarePassive(queueCfg.getQueueName());
			return re.getMessageCount();
		} catch (IOException e) {
			log.error("failed to check queue: \n\t" + queueCfg.getQueueName(), e);
		}
		return 0;
	}

	public List<byte[]> getMessages(int size) {
		Channel ch = MQueues.instance().getChannel(queueCfg);

		List<byte[]> msgList = new ArrayList<byte[]>(size);
		try {
			for (int i = 0; i < size; i++) {
				GetResponse resp = ch.basicGet(queueCfg.getQueueName(), false);
				ch.basicNack(resp.getEnvelope().getDeliveryTag(), false, true);
				msgList.add(resp.getBody());
			}
		} catch (IOException e) {
			log.error("failed to check queue: \n\t" + queueCfg.getQueueName(), e);
		}

		return msgList;
	}

	public long purge() {
		Channel ch = MQueues.instance().getChannel(queueCfg);
		try {
			PurgeOk re = ch.queuePurge(queueCfg.getQueueName());
			return re.getMessageCount();
		} catch (IOException e) {
			log.error("failed to check queue: \n\t" + queueCfg.getQueueName(), e);
		}
		return -1;
	}
	
	public void sendMessage(final String msgStr) {
		final Channel ch = MQueues.instance().getChannel(queueCfg);
		if (ch == null) {
			log.warn("not channel generated for queue: " + queueCfg.getQueueName());
			return;
		}
		
		try {
			ExchangeCfg ec = queueCfg.getExchanges().iterator().next();
			ch.basicPublish(ec.getExchangeName(), queueCfg.getRouteKey(), null, msgStr.getBytes());
		} catch (IOException e) {
			log.error("failed to send message: \n\t" + msgStr + "\n\tto queue: " + queueCfg.getQueueName(), e);
		}
	}

}
