package com.thenetcircle.services.common;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * abstraction for most of JPA operations
 * @author fan
 * @param <T>
 */
@SuppressWarnings("unchecked")
public abstract class BaseDao<T> implements Serializable {//implements IBaseDao<T> {
	private static final long serialVersionUID = 1L;
	private static final Log log = LogFactory.getLog(BaseDao.class.getSimpleName());
	
	public BaseDao() {
		
	}
	
	public BaseDao(final EntityManager _em) {
		this.em = _em;
	}
	
	@PreDestroy
	public void clean() {
		log.info(MiscUtils.invocationInfo());
		if (em.isOpen()) {
			em.close();
		}
	}
	
	@Inject 
//	@PersistenceContext//requires JTA
	protected EntityManager em;

	protected abstract Class<T> getEntityClass();

//	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public T create(final T entity) {
		em.persist(entity);
		return entity;
	}

//	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public T edit(final T entity) {
		return em.merge(entity);
	}

	public T refresh(final T entity) {
		em.refresh(entity);
		return entity;
	}

//	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public T destroy(final T entity) {
		em.remove(em.merge(entity));
		return entity;
	}

	public T find(Object pk) {
		if (pk == null)
			return null;
		return getEntityClass().cast(em.find(getEntityClass(), pk));
	}

	public <E> E findEntity(Class<E> cls, Object pk) {
		if (pk == null || cls == null) {
			return null;
		}
		return cls.cast(em.find(cls, pk));
	}

	public List<T> findAll() {
		return em.createQuery(String.format("select object(o) from %s as o", getEntityClass().getSimpleName())).getResultList();
	}

	public List<T> query(String hql, Object... params) {
		if (StringUtils.isBlank(hql)) {
			return Collections.EMPTY_LIST;
		}
		Query q = em.createQuery(hql);
	
		if (ArrayUtils.isNotEmpty(params)) {
			for (int i = 0; i < params.length; i++) {
				q.setParameter(1 + i, params[i]);
			}
		}
	
		final List<T> resultList = q.getResultList();
		return resultList == null ? Collections.EMPTY_LIST : resultList; 
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.netcircle.paymentsystem.dao.impl.IBaseDao#queryPage(java.lang.String,
	 * int, int, java.lang.Object)
	 */
	
	public List<T> queryPage(String hql, int pageIdx, int pageSize, Object... params) {
		if (StringUtils.isBlank(hql)) {
			return Collections.EMPTY_LIST;
		}
		Query q = em.createQuery(hql);
	
		if (ArrayUtils.isNotEmpty(params)) {
			for (int i = 0; i < params.length; i++) {
				q.setParameter(1 + i, params[i]);
			}
		}
		if (pageIdx >= 0 && pageSize > 0) {
			q.setFirstResult(pageIdx * pageSize);
			q.setMaxResults(pageSize);
		}
		return q.getResultList();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.netcircle.paymentsystem.dao.impl.IBaseDao#queryCount(java.lang.String
	 * , java.lang.Object)
	 */
	
	public long queryCount(String hql, Object... params) {
		if (StringUtils.isBlank(hql)) {
			return 0;
		}
		final List<?> resultList = query(hql, params);
		if (CollectionUtils.isEmpty(resultList)) {
			return 0;
		}
		if (resultList.size() == 1 && resultList.get(0) instanceof Long) {
			return (Long) resultList.get(0);
		}
		return resultList.size();
	}

	public long queryCountWithNamedParams(String hql, Map<String, Object> namedParams) {
		if (StringUtils.isBlank(hql)) {
			return 0;
		}
		final List<?> resultList = queryPageByNamedParams(hql, -1, -1, namedParams);
		if (CollectionUtils.isEmpty(resultList)) {
			return 0;
		}
		if (resultList.size() == 1 && resultList.get(0) instanceof Long) {
			return (Long) resultList.get(0);
		}
		return resultList.size();
	}

	public long queryCountWithPositionalParams(String hql, Map<Integer, Object> positionalParams) {
		if (StringUtils.isBlank(hql)) {
			return 0;
		}
		final List<?> resultList = queryPageByPositionalParams(hql, -1, -1, positionalParams);
		if (CollectionUtils.isEmpty(resultList)) {
			return 0;
		}
		if (resultList.size() == 1 && resultList.get(0) instanceof Long) {
			return (Long) resultList.get(0);
		}
		return resultList.size();
	}

	public List<T> queryPageByNamedParams(String hql, int pageIdx, int pageSize, Map<String, Object> namedParams) {
		if (StringUtils.isBlank(hql)) {
			return Collections.EMPTY_LIST;
		}
		final Query q = em.createQuery(hql);
		if (pageIdx > 0 && pageSize > 0) {
			q.setFirstResult(pageIdx * pageSize);
			q.setMaxResults(pageSize);
		}
	
		if (MapUtils.isNotEmpty(namedParams)) {
			for (Map.Entry<String, Object> namedParam : namedParams.entrySet()) {
				q.setParameter(namedParam.getKey(), namedParam.getValue());
			}
		}
	
		return q.getResultList();
	}

	public List<T> queryPageByPositionalParams(String hql, int pageIdx, int pageSize, Map<Integer, Object> positionalParams) {
		if (StringUtils.isBlank(hql)) {
			return Collections.EMPTY_LIST;
		}
		final Query q = em.createQuery(hql);
		if (pageIdx >= 0 && pageSize > 0) {
			q.setFirstResult(pageIdx * pageSize);
			q.setMaxResults(pageSize);
		}
	
		if (MapUtils.isNotEmpty(positionalParams)) {
			for (Map.Entry<Integer, Object> positionalParam : positionalParams.entrySet()) {
				q.setParameter(positionalParam.getKey(), positionalParam.getValue());
			}
		}
	
		return q.getResultList();
	}

	@SuppressWarnings("rawtypes")
	public List queryEntity(String hql, Object... params) {
		if (StringUtils.isBlank(hql)) {
			return Collections.EMPTY_LIST;
		}
		final Query q = em.createQuery(hql);
	
		if (ArrayUtils.isNotEmpty(params)) {
			for (int i = 0; i < params.length; i++) {
				q.setParameter(1 + i, params[i]);
			}
		}
	
		return q.getResultList();
	}

	@SuppressWarnings("rawtypes")
	public Object findOneEntity(String hql, Object... params) {
		final List result  = queryEntityPage(hql, 0, 1, params);
		return CollectionUtils.isEmpty(result) ? null : result.get(0);
	}

	public T findOne(String hql, Object... params) {
		final List result  = queryEntityPage(hql, 0, 1, params);
		return getEntityClass().cast(CollectionUtils.isEmpty(result) ? null : result.get(0));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.netcircle.paymentsystem.dao.impl.IBaseDao#queryPage(java.lang.String,
	 * int, int, java.lang.Object)
	 */
	@SuppressWarnings("rawtypes")
	public List queryEntityPage(String hql, int pageIdx, int pageSize, Object... params) {
		if (StringUtils.isBlank(hql)) {
			return Collections.EMPTY_LIST;
		}
		Query q = em.createQuery(hql);
	
		if (ArrayUtils.isNotEmpty(params)) {
			for (int i = 0; i < params.length; i++) {
				q.setParameter(1 + i, params[i]);
			}
		}
		if (pageIdx >= 0 && pageSize > 0) {
			q.setFirstResult(pageIdx * pageSize);
			q.setMaxResults(pageSize);
		}
		return q.getResultList();
	}

	@SuppressWarnings("rawtypes")
	public List queryEntityPageByNamedParams(String hql, int pageIdx, int pageSize, Map<String, Object> namedParams) {
		if (StringUtils.isBlank(hql)) {
			return Collections.EMPTY_LIST;
		}
		Query q = em.createQuery(hql);
		if (pageIdx > 0 && pageSize > 0) {
			q.setFirstResult(pageIdx * pageSize);
			q.setMaxResults(pageSize);
		}
	
		if (MapUtils.isNotEmpty(namedParams)) {
			for (Map.Entry<String, Object> namedParam : namedParams.entrySet()) {
				q.setParameter(namedParam.getKey(), namedParam.getValue());
			}
		}
	
		return q.getResultList();
	}

	@SuppressWarnings("rawtypes")
	public List queryEntityPageByPositionalParams(String hql, int pageIdx, int pageSize, Map<Integer, Object> positionalParams) {
		if (StringUtils.isBlank(hql)) {
			return Collections.EMPTY_LIST;
		}
		Query q = em.createQuery(hql);
		if (pageIdx >= 0 && pageSize > 0) {
			q.setFirstResult(pageIdx * pageSize);
			q.setMaxResults(pageSize);
		}
	
		if (MapUtils.isNotEmpty(positionalParams)) {
			for (Map.Entry<Integer, Object> positionalParam : positionalParams.entrySet()) {
				q.setParameter(positionalParam.getKey(), positionalParam.getValue());
			}
		}
	
		return q.getResultList();
	}

	public void beginTransaction() {
		final EntityTransaction transaction = em.getTransaction();
		if (transaction.isActive()) {
			return;
		}
		
		transaction.begin();
	}

	public void endTransaction() {
		final EntityTransaction transaction = em.getTransaction();
		if (!transaction.isActive()) {
			log.error("transaction isn't active, not commit");
			return;
		}
		
		if (transaction.getRollbackOnly()) {
			transaction.rollback();
			return;
		}
		
		transaction.commit();
	}
	
	public List<T> page(int pageIdx, int pageSize) {
		return queryPage(String.format("select object(o) from %s as o", getEntityClass().getSimpleName()), pageIdx, pageSize);
	}
}
