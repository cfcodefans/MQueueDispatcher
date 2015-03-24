package com.thenetcircle.services.dispatcher.dao;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Default;
import javax.persistence.EntityManager;

import com.thenetcircle.services.commons.persistence.jpa.BaseDao;
import com.thenetcircle.services.commons.persistence.jpa.cdi.Transactional;
import com.thenetcircle.services.dispatcher.entity.ExchangeCfg;
import com.thenetcircle.services.dispatcher.entity.QueueCfg;
import com.thenetcircle.services.dispatcher.entity.ServerCfg;

@Default
@RequestScoped //for java se, only applicatoin scope available
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
				" LEFT JOIN FETCH ec.serverCfg order by ec.id desc");
	}

	public List<ExchangeCfg> findExchangesByServer(ServerCfg find) {
		return query("select ec from ExchangeCfg ec where ec.serverCfg=?1", find);
	}
	
	public List<ExchangeCfg> findExchangesByServer(int serverCfgId) {
		return query("select ec from ExchangeCfg ec left join fetch ec.serverCfg where ec.serverCfg.id=?1", serverCfgId);
	}
	
	@Transactional
	public ExchangeCfg update(final ExchangeCfg ec) {
		final ExchangeCfg _ec = find(ec.getId());
		final Set<QueueCfg> qcs = new HashSet<QueueCfg>(_ec.getQueues());

		_ec.getQueues().clear();
		
		qcs.forEach(qc -> {
			qc.getExchanges().remove(_ec);
			qc.getExchanges().add(ec);
			em.merge(qc);
		});
		ec.getQueues().addAll(qcs);
		
//		_ec.setAutoDelete(ec.isAutoDelete());
//		_ec.setDurable(ec.isDurable());
//		_ec.setEnabled(ec.isEnabled());
//		_ec.setExchangeName(ec.getExchangeName());
//		_ec.setType(ec.getType());
//		_ec.setVersion(ec.getVersion());
//		
		final ExchangeCfg edited = super.edit(ec);
		return edited;
	}
}
