package com.thenetcircle.services.dispatcher.dao;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.persistence.EntityManager;

import com.thenetcircle.services.common.BaseDao;
import com.thenetcircle.services.dispatcher.entity.MessageContext;

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
	
	public List<MessageContext> queryFailedJobs() {
		return null;
	}
}
