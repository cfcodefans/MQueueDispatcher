package com.thenetcircle.services.dispatcher.dao;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.persistence.EntityManager;

import com.thenetcircle.services.common.BaseDao;
import com.thenetcircle.services.dispatcher.entity.ServerCfg;

@Default
@ApplicationScoped
public class ServerCfgDao extends BaseDao<ServerCfg> {
	private static final long serialVersionUID = 1L;
	
	@Override
	public Class<ServerCfg> getEntityClass() {
		return ServerCfg.class;
	}
	
	public ServerCfgDao(final EntityManager em) {
		super(em);
	}
	
	public ServerCfgDao() {
		super();
	}
}