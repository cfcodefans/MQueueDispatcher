package mgr.dao;

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
}
