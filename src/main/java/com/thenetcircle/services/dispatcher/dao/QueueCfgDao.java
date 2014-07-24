package com.thenetcircle.services.dispatcher.dao;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Default;
import javax.persistence.EntityManager;

import org.apache.commons.collections4.CollectionUtils;

import com.thenetcircle.services.common.BaseDao;
import com.thenetcircle.services.dispatcher.entity.ExchangeCfg;
import com.thenetcircle.services.dispatcher.entity.QueueCfg;
import com.thenetcircle.services.dispatcher.entity.ServerCfg;
import com.thenetcircle.services.weld.Transactional;

@Default
@RequestScoped //for java se, only applicatoin scope available
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
	
	public List<QueueCfg> findEnabled() {
		return query("select qc from QueueCfg qc " +
				" LEFT JOIN FETCH qc.exchanges " +
				" LEFT JOIN FETCH qc.serverCfg " +
				" LEFT JOIN FETCH qc.destCfg where qc.enabled=true");
	}
	
	
	public List<QueueCfg> findAll() {
		return query("select qc from QueueCfg qc " +
				" LEFT JOIN FETCH qc.exchanges " +
				" LEFT JOIN FETCH qc.serverCfg " +
				" LEFT JOIN FETCH qc.destCfg");
	}
	
	public QueueCfg find(Integer id) {
		return findOne("select qc from QueueCfg qc " +
				" LEFT JOIN FETCH qc.exchanges " +
				" LEFT JOIN FETCH qc.serverCfg " +
				" LEFT JOIN FETCH qc.destCfg where qc.id=?1 ", id);
	}
	
	@Transactional
	public QueueCfg create(final QueueCfg qc) {
		prepare(qc);
		return super.create(qc);
	}
	
	@Transactional
	public QueueCfg edit(final QueueCfg qc) {
		prepare(qc);
		return super.edit(qc);
	}

	private void prepare(final QueueCfg qc) {
		if (qc.getId() < 0) {
			qc.getDestCfg().setId(-1);
		}

		final ExchangeCfgDao ecDao = new ExchangeCfgDao(em);
		
		if (CollectionUtils.isEmpty(qc.getExchanges())) {
			final ExchangeCfg _ec = QueueCfg.defaultExchange(qc);
			qc.getExchanges().add(_ec);
			_ec.getQueues().add(qc);
		} else {
			final Collection<ExchangeCfg> ecs = new HashSet<ExchangeCfg>(qc.getExchanges());
			qc.getExchanges().clear();
			for (ExchangeCfg ec : ecs) {
				if (ec == null) {
					continue;
				}
				if (ec.getId() < 0) {
					ec = ecDao.create(ec);
				}
				ec.getQueues().add(qc);
				qc.getExchanges().add(ec);
			}
		}
		
		for (ExchangeCfg ec : qc.getExchanges()) {
			ec.getQueues().add(qc);
		}
	}

	public List<QueueCfg> findQueuesByServer(final ServerCfg sc) {
		return super.query("select qc from QueueCfg qc where qc.serverCfg=?1", sc);
	}
}
