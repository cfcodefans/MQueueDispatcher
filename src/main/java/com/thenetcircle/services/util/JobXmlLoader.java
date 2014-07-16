package com.thenetcircle.services.util;

import java.io.FileInputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

import org.apache.commons.lang3.StringUtils;

import com.thenetcircle.comsumerdispatcher.config.DispatcherConfig;
import com.thenetcircle.services.dispatcher.entity.ExchangeCfg;
import com.thenetcircle.services.dispatcher.entity.QueueCfg;

public class JobXmlLoader {
	static Logger log = Logger.getLogger(JobXmlLoader.class.getSimpleName());

	public static void main(String[] args) {
		if (args.length == 0 || StringUtils.isBlank(args[0])) {
			log.log(Level.SEVERE, "useage: job.xml persistence.properties");
			return;
		}
		
		final String jobXmlPath = args[0];
		final String propertiesPath = args[1];
		
		try {
			init(propertiesPath);
			loadCfgsIntoDatabase(jobXmlPath);
		} catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
		clean();
	}

	final static String UN = "job_xml_loader";
	static EntityManagerFactory emf = null;
	static EntityManager em = null;

	public static void init(final String propertiesPath) throws Exception {
		Properties p = new Properties();
		p.load(new FileInputStream(propertiesPath));
		
		emf = Persistence.createEntityManagerFactory(UN, new HashMap(p));
		em = emf.createEntityManager();
	}

	public static void loadCfgsIntoDatabase(String jobXmlPath) throws Exception {
		final DispatcherConfig dispatcherConfig = DispatcherConfig.getInstance();
		dispatcherConfig.loadConfig(jobXmlPath);

		log.info("configuration is loaded");

		Collection<QueueCfg> queueCfgs = DispatcherConfig.dispatcherJobsToQueueCfgs(dispatcherConfig.getAllJobs());

		log.info(queueCfgs.size() + " queues are loaded");
		
		startTransaction();
		for (final QueueCfg qc : queueCfgs) {
			em.persist(qc.getServerCfg());
			em.persist(qc.getDestCfg());
			for (final ExchangeCfg ec : qc.getExchanges()) {
				em.persist(ec);
			}
			em.persist(qc);
		}
		commit();
		
		log.info(em.createQuery("select count(id) from ServerCfg sc").getSingleResult() + " servers are loaded");
		log.info(em.createQuery("select count(id) from QueueCfg qc").getSingleResult() + " queues are loaded");
	}

	public static void clean() {
		if (em != null && em.isOpen()) {
			em.close();
		}
		if (emf != null && emf.isOpen()) {
			emf.close();
		}
	}

	public static void startTransaction() {
		final EntityTransaction tx = em.getTransaction();
		if (tx.isActive()) return;
		tx.begin();
	}

	public static void commit() {
		final EntityTransaction tx = em.getTransaction();
		if (!tx.isActive()) return;
		tx.commit();
	}

}
