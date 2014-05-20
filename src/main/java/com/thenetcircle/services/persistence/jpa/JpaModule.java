package com.thenetcircle.services.persistence.jpa;

import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.PersistenceContext;

import com.thenetcircle.services.common.MiscUtils;


@ApplicationScoped
public class JpaModule {
	
	static EntityManagerFactory emf = null;
	final static String UN = "mqueue-dispatcher";
	static Logger log = Logger.getLogger(JpaModule.class.getSimpleName());
	private static JpaModule instance; 
	
	@PostConstruct
	public void init() {
		log.info(MiscUtils.invocationInfo());
		log.info("loading EntityManagerFactory......\n");
		emf = Persistence.createEntityManagerFactory(UN);
		log.info("\nEntityManagerFactory is loaded......\n");
	}
	
	@Produces 
//	@PersistenceContext
	public static EntityManager getEntityManager() {
		log.info(MiscUtils.invocationInfo());
		if (emf == null) {
			emf = Persistence.createEntityManagerFactory(UN);
		}
		return emf.createEntityManager();
	}
	
	@PreDestroy
	public void destory() {
		log.info(MiscUtils.invocationInfo());
		log.info("closing EntityManagerFactory......\n");
		if (emf != null && emf.isOpen()) {
			emf.close();
		}
		log.info("\nEntityManagerFactory is closed......\n");
	}
	
	public static JpaModule instance() {
		if (instance == null) {
			instance = new JpaModule();
			instance.init();
		}
		return instance;
	}
}
