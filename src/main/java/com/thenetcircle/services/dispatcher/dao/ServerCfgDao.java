package com.thenetcircle.services.dispatcher.dao;

import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Default;
import javax.persistence.EntityManager;

import com.thenetcircle.services.commons.persistence.jpa.BaseDao;
import com.thenetcircle.services.dispatcher.entity.ServerCfg;

@Default
@RequestScoped
//@Transactional
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
	
	public List<ServerCfg> findAll() {
		return super.query("select sc from ServerCfg sc where sc.enabled=true");
	}
}
