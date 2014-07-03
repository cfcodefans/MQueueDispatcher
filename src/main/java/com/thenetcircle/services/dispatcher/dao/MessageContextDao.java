package com.thenetcircle.services.dispatcher.dao;

import java.util.Date;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.persistence.EntityManager;

import com.thenetcircle.services.common.BaseDao;
import com.thenetcircle.services.dispatcher.entity.MessageContext;
import com.thenetcircle.services.dispatcher.entity.QueueCfg;

@Default
@ApplicationScoped //for java se, only applicatoin scope available
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
}
