package com.thenetcircle.services.dispatcher.dao;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.persistence.EntityManager;

import com.thenetcircle.services.common.BaseDao;
import com.thenetcircle.services.dispatcher.entity.ExchangeCfg;

@Default
@ApplicationScoped //for java se, only applicatoin scope available
public class ExchangeCfgDao extends BaseDao<ExchangeCfg> {
	private static final long serialVersionUID = 1L;
	
	@Override
	public Class<ExchangeCfg> getEntityClass() {
		return ExchangeCfg.class;
	}

	public ExchangeCfgDao() {
		
	}
	
	public ExchangeCfgDao(final EntityManager em) {
		super(em);
	}
}
