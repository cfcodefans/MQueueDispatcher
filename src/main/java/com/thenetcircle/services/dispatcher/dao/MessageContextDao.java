package com.thenetcircle.services.dispatcher.dao;

import java.util.Date;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Default;
import javax.persistence.EntityManager;

import org.apache.commons.lang3.StringUtils;

import com.thenetcircle.services.common.BaseDao;
import com.thenetcircle.services.dispatcher.entity.MessageContext;
import com.thenetcircle.services.dispatcher.entity.QueueCfg;
import com.thenetcircle.services.dispatcher.entity.ServerCfg;

@Default
@RequestScoped //for java se, only applicatoin scope available
public class MessageContextDao extends BaseDao<MessageContext> {
	private static final long serialVersionUID = 1L;
	
	@Override
	public Class<MessageContext> getEntityClass() {
		return MessageContext.class;
	}

	public MessageContextDao() {
		
	}
	
	public MessageContextDao(final EntityManager em) {
		super(em);
	}
	
	public List<MessageContext> queryFailedJobs(final QueueCfg qc, final Date start, final Date end) {
		String hql = "select mc from MessageContext mc where mc.queueCfg=?1 ";
		final Date now = new Date();
		if (start != null && start.before(now)) {
			hql = hql + " and mc.timestamp > " + start.getTime();
		}
		
		if (end != null && end.before(now)) {
			hql = hql + " and mc.timestamp < " + end.getTime();
		}
		
		hql = hql + " order by mc.timestamp desc";
		
		return queryPage(hql, 0, 1000, qc);
	}
	
	public List<MessageContext> findAll() {
		return queryPage("select mc from MessageContext mc left join fetch mc.queueCfg order by mc.timestamp desc", 0, 1000);
	}
	
	public String queryFailedJobsReport(final ServerCfg sc, final Date start, final Date end) {
		if (sc == null) {
			return null;
		}
		
		String hql = "select mc.queueCfg.name as name, "
				+ " mc.queueCfg.retryLimit as retryLimit, "
				+ " mc.response.statusCode as statusCode, "
				+ " count(mc.response.statusCode) as count, "
				+ " mc.response.responseStr as response "
				+ " from MessageContext mc where mc.queueCfg.serverCfg=?1 ";
		final Date now = new Date();
		if (start != null && start.before(now)) {
			hql = hql + " and mc.timestamp > " + start.getTime();
		}
		
		if (end != null && end.before(now)) {
			hql = hql + " and mc.timestamp < " + end.getTime();
		}
		
		hql = hql + " and mc.failTimes > mc.queueCfg.retryLimit group by mc.queueCfg.id, mc.response.statusCode";
		
		final List<Object[]> resultList = queryEntity(hql, sc);
		
		final StringBuilder sb = new StringBuilder();
		
		for (final Object[] row : resultList) {
			for (int i = 0; i < row.length; i++) {
				if (i == row.length - 1) {
					sb.append(StringUtils.substring(String.valueOf(row[i]), 0, 20)).append("......\n");
				} else {
					sb.append(row[i]).append('\t');
				}
			}
		}
		
		return sb.toString();
	}
}
