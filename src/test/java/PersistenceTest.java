import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.thenetcircle.services.commons.Jsons;
import com.thenetcircle.services.commons.MiscUtils;
import com.thenetcircle.services.dispatcher.dao.ExchangeCfgDao;
import com.thenetcircle.services.dispatcher.dao.MessageContextDao;
import com.thenetcircle.services.dispatcher.dao.QueueCfgDao;
import com.thenetcircle.services.dispatcher.dao.ServerCfgDao;
import com.thenetcircle.services.dispatcher.entity.ExchangeCfg;
import com.thenetcircle.services.dispatcher.entity.MessageContext;
import com.thenetcircle.services.dispatcher.entity.MsgResp;
import com.thenetcircle.services.dispatcher.entity.QueueCfg;
import com.thenetcircle.services.dispatcher.entity.ServerCfg;


public class PersistenceTest {

	static Logger log = Logger.getLogger(PersistenceTest.class.getSimpleName());
	
	final static String UN = "mqueue-dispatcher"; 
	static EntityManagerFactory emf = null;
	static EntityManager em = null;
	
	@Test
	public void testPropertiesFile() throws Exception {
		Properties p = new Properties();
		p.putAll(MiscUtils.map(
					"hibernate.ejb.persistenceUnitName", "test",
					"javax.persistence.transactionType", "RESOURCE_LOCAL",
					"javax.persistence.provider", "org.hibernate.jpa.HibernatePersistenceProvider",
					"javax.persistence.jdbc.driver", "org.hsqldb.jdbcDriver",
					"javax.persistence.jdbc.url", "jdbc:hsqldb:hsql://bart:9002/dispatcher",
					"javax.persistence.jdbc.user", "sa",
					"hibernate.dialect", "org.hibernate.dialect.HSQLDialect",
					"hibernate.hbm2ddl.auto", "update",
					"hibernate.show_sql", "true",
					"hibernate.archive.autodetection", "class",
					"hibernate.current_session_context_class", "thread"
				));
		p.store(new FileWriter("load_job_xml.properties"), null);
	}
	
	@BeforeClass
	public static void init() throws Exception {
		Properties p = new Properties();
		p.load(new FileInputStream("load_job_xml.properties"));
		
		emf = Persistence.createEntityManagerFactory("config_loader", 
				new HashMap(p));
		em = emf.createEntityManager();
	}
	
	@Before
	public void startTransaction() {
		final EntityTransaction tx = em.getTransaction();
		if (tx.isActive()) return;
		tx.begin();
	}
	
	@After
	public void commit() {
		final EntityTransaction tx = em.getTransaction();
		if (!tx.isActive()) return;
		tx.commit();
	}
	
	@Test
	public void validateEntities() {
		log.info(String.format("sc %s", em.createQuery("select count(id) from ServerCfg sc").getSingleResult()));
		log.info(String.format("exc %s", em.createQuery("select count(id) from ExchangeCfg ec").getSingleResult()));
		log.info(String.format("qc %s", em.createQuery("select count(id) from QueueCfg qc").getSingleResult()));
		log.info(String.format("mc %s", em.createQuery("select count(mc) from MessageContext mc").getResultList()));
	}
	
	@Test
	public void cleanAll() {
		log.info("remove QueueCfg " + em.createQuery("delete from MessageContext").executeUpdate());
		log.info("remove QueueCfg " + em.createQuery("delete from QueueCfg").executeUpdate());
		log.info("remove QueueCfg " + em.createQuery("delete from ExchangeCfg").executeUpdate());
		log.info("remove QueueCfg " + em.createQuery("delete from ServerCfg").executeUpdate());
		log.info("remove QueueCfg " + em.createQuery("delete from HttpDestinationCfg").executeUpdate());
	}
	
	
//	@Test
//	public void loadCfgsIntoDatabase() throws Exception {
//		final DispatcherConfig dispatcherConfig = DispatcherConfig.getInstance();
//		dispatcherConfig.loadConfig("job.xml");
//		
//		log.info("configuration is loaded");
//		
//		Collection<QueueCfg> queueCfgs = DispatcherConfig.dispatcherJobsToQueueCfgs(dispatcherConfig.getAllJobs());
//		
//		log.info(queueCfgs.size() + " queues are loaded");
////		startTransaction();
//		for (final QueueCfg qc : queueCfgs) {
//			em.persist(qc.getServerCfg());
//			em.persist(qc.getDestCfg());
//			for (final ExchangeCfg ec : qc.getExchanges()) {
//				em.persist(ec);
//			}
//			em.persist(qc);
//		}
//		
//		log.info(em.createQuery("select count(id) from ServerCfg sc").getSingleResult() + " server are loaded");
//	}
	
	@Test
	public void convertToJSON() throws Exception {
		QueueCfgDao qcDao = new QueueCfgDao(em);
		final List<QueueCfg> qcList = qcDao.findAll();
		
		Assert.assertFalse(CollectionUtils.isEmpty(qcList));
		
		QueueCfg qc = qcList.get(0);
		
		try {
			log.info("\n\n" + Jsons.toString(qc) + "\n\n");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testQueries() {
		QueueCfgDao qcDao = new QueueCfgDao(em);
		List<QueueCfg> qcs = qcDao.findAll();
		
		System.out.println(qcs.get(0).getExchanges());
		
		ExchangeCfgDao ecDao = new ExchangeCfgDao(em);
		List<ExchangeCfg> ecs = ecDao.findAll();
		System.out.println(ecs.get(0).getQueues());
	}
	
	@Test
	public void testMsgCtx() {
		QueueCfgDao qcDao = new QueueCfgDao(em);
		QueueCfg qc = qcDao.find(282);
		Assert.assertNotNull(qc);
		
		MessageContext mc = new MessageContext();
		mc.setQueueCfg(qc);
		mc.setResponse(new MsgResp(MsgResp.FAILED, "test"));
	}
	
	@Test
	public void testBinding() {
		QueueCfgDao qcDao = new QueueCfgDao(em);
		List<QueueCfg> qcs = qcDao.findAll();
		
		System.out.println(MiscUtils.toXML(qcs.get(0)));
	}
	
	@Test
	public void testFailedMsgReport() {
		MessageContextDao mcDao = new MessageContextDao(em);
		
		ServerCfgDao scDao = new ServerCfgDao(em); 
		
		List<ServerCfg> scList = scDao.findAll();
		
		Assert.assertTrue(CollectionUtils.isNotEmpty(scList));

		Date end = new Date();
		Date start = DateUtils.addWeeks(end, -1);
		for (final ServerCfg sc : scList) {
			System.out.println(mcDao.queryFailedJobsReport(sc, start, end));
		}
	}
	
	@AfterClass
	public static void clean() {
		em.close();
		emf.close();
	}
}
