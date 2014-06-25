package com.thenetcircle.services.dispatcher.dao;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.persistence.EntityManager;

import com.thenetcircle.services.common.BaseDao;
import com.thenetcircle.services.dispatcher.entity.QueueCfg;

@Default
@ApplicationScoped //for java se, only applicatoin scope available
public class QueueCfgDao extends BaseDao<QueueCfg> {
	private static final long serialVersionUID = 1L;
	
	@Override
	public Class<QueueCfg> getEntityClass() {
		return QueueCfg.class;
	}

	public QueueCfgDao() {
		
	}
	
	public QueueCfgDao(final EntityManager em) {
		super(em);
	}
	
	public List<QueueCfg> findAll() {
		return query("select qc from QueueCfg qc " +
				" LEFT JOIN FETCH qc.exchanges " +
				" LEFT JOIN FETCH qc.serverCfg " +
				" LEFT JOIN FETCH qc.destCfg");
	}
}
