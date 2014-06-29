package com.thenetcircle.services.dispatcher.dao;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.persistence.EntityManager;

import com.thenetcircle.services.common.BaseDao;
import com.thenetcircle.services.dispatcher.entity.ExchangeCfg;
import com.thenetcircle.services.dispatcher.entity.QueueCfg;
import com.thenetcircle.services.dispatcher.entity.ServerCfg;

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
	
	public List<ExchangeCfg> findAll() {
		return query("select ec from ExchangeCfg ec " +
				" LEFT JOIN FETCH ec.queues " +
				" LEFT JOIN FETCH ec.serverCfg ");
	}

	public List<ExchangeCfg> findExchangesByServer(ServerCfg find) {
		return query("select ec from ExchangeCfg ec where ec.serverCfg=1?", find);
	}
}
