package com.thenetcircle.services.dispatcher.failsafe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.thenetcircle.services.dispatcher.entity.MessageContext;
import com.thenetcircle.services.dispatcher.entity.QueueCfg;

public interface IFailedMessageManagment {
	public static class Criterion {
		private Pair<Date, Date> timeRange = new MutablePair<Date, Date>();
		private String msgPattern = null;
		private List<QueueCfg> queues = new ArrayList<QueueCfg>();
		private String resultPattern = null;

		public Pair<Date, Date> getTimeRange() {
			return timeRange;
		}

		public void setTimeRange(Pair<Date, Date> timeRange) {
			this.timeRange = timeRange;
		}

		public String getMsgPattern() {
			return msgPattern;
		}

		public void setMsgPattern(String msgPattern) {
			this.msgPattern = msgPattern;
		}

		public List<QueueCfg> getQueues() {
			return queues;
		}

		public void setQueues(List<QueueCfg> queues) {
			this.queues = queues;
		}

		public String getResultPattern() {
			return resultPattern;
		}

		public void setResultPattern(String resultPattern) {
			this.resultPattern = resultPattern;
		}
	}
	
	void retry(final Criterion c);
	void retry(final Collection<MessageContext> messages, final QueueCfg qc);
	Collection<MessageContext> query(final Criterion c);
	
}
