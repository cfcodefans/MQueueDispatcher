package com.thenetcircle.services.dispatcher.dao;

import javax.enterprise.context.RequestScoped;
import javax.persistence.EntityManager;

import com.thenetcircle.services.commons.persistence.jpa.CdiBaseDao;

//@Default
//@Transactional
@RequestScoped
public class GeneralDao  extends CdiBaseDao<Object> {
	private static final long serialVersionUID = 1L;
	
	@Override
	public Class<Object> getEntityClass() {
		return Object.class;
	}
	
	public GeneralDao(final EntityManager em) {
		super(em);
	}
	
	public GeneralDao() {
		super();
	}
}