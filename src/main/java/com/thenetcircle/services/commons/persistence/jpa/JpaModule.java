package com.thenetcircle.services.commons.persistence.jpa;

import java.io.FileInputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.thenetcircle.services.commons.MiscUtils;


@ApplicationScoped
public class JpaModule {
	
	private static final String EXTERNAL_JPA_PROPERTIES = "external_jpa_properties";
	
	static EntityManagerFactory emf = null;
	final static String UN = "mqueue-dispatcher";
	protected static final Log log = LogFactory.getLog(JpaModule.class.getName());
	private static JpaModule instance; 
	
	private static ThreadLocal<EntityManager> ems = new ThreadLocal<EntityManager>();
	
	@PostConstruct
	public void init() {
		log.info(MiscUtils.invocationInfo());
		log.info("loading EntityManagerFactory......\n");
		
		emf = Persistence.createEntityManagerFactory(UN, getExternalPersistenceCfgs());
		
		log.info("\nEntityManagerFactory is loaded......\n");
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static Map getExternalPersistenceCfgs() {
		final String pathStr = System.getProperty(EXTERNAL_JPA_PROPERTIES);
		if (StringUtils.isBlank(pathStr)) {
			return Collections.EMPTY_MAP;
		}
		
		try {
			final Properties p = new Properties();
			p.load(new FileInputStream(pathStr));
			
			final HashMap settings = new HashMap(p);
			
			log.info("\nhave loaded data source\n" + settings + "\n\n");
			
			return settings;
		} catch (Exception e) {
			log.error("failed to load EXTERNAL_JPA_PROPERTIES: " + pathStr, e);
		}
		
		return Collections.EMPTY_MAP;
	}
	
	@Produces 
//	@PersistenceContext
	public static EntityManager getEntityManager() {
		final EntityManager em = ems.get();
		if (em != null && em.isOpen()) {
			return em;
		}
		
		log.info("creating EntityManager for Thread: " + Thread.currentThread().getName());
		ems.remove();
		
		log.info(MiscUtils.invocationInfo());
		if (emf == null || !emf.isOpen()) {
			emf = Persistence.createEntityManagerFactory(UN, getExternalPersistenceCfgs());
		}
		
		final EntityManager newEM = emf.createEntityManager();
		ems.set(newEM);
		
		log.info("created new EntityManager for Thread: " + Thread.currentThread().getName());
		
		return newEM;
	}
	
	@PreDestroy
	public void destory() {
		log.info(MiscUtils.invocationInfo());
		log.info("\n\nclosing EntityManagerFactory......\n");
		if (emf != null && emf.isOpen()) {
			emf.close();
		}
		emf = null;
		log.info("\n\nEntityManagerFactory is closed......\n");
	}
	
	@Produces
	public static JpaModule instance() {
		if (instance == null) {
			instance = new JpaModule();
			instance.init();
		}
		return instance;
	}
}
