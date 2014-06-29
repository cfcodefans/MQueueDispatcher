import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.thenetcircle.comsumerdispatcher.config.DispatcherConfig;
import com.thenetcircle.services.common.Jsons;
import com.thenetcircle.services.common.MiscUtils;
import com.thenetcircle.services.dispatcher.dao.ExchangeCfgDao;
import com.thenetcircle.services.dispatcher.dao.QueueCfgDao;
import com.thenetcircle.services.dispatcher.entity.ExchangeCfg;
import com.thenetcircle.services.dispatcher.entity.QueueCfg;


public class PersistenceTest {

	static Logger log = Logger.getLogger(PersistenceTest.class.getSimpleName());
	
	final static String UN = "mqueue-dispatcher"; 
	static EntityManagerFactory emf = null;
	static EntityManager em = null;
	
	@BeforeClass
	public static void init() {
		emf = Persistence.createEntityManagerFactory(UN);
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
	
	
	@Test
	public void loadCfgsIntoDatabase() throws Exception {
		final DispatcherConfig dispatcherConfig = DispatcherConfig.getInstance();
		dispatcherConfig.loadConfig("job.xml");
		
		log.info("configuration is loaded");
		
		Collection<QueueCfg> queueCfgs = DispatcherConfig.dispatcherJobsToQueueCfgs(dispatcherConfig.getAllJobs());
		
		log.info(queueCfgs.size() + " queues are loaded");
//		startTransaction();
		for (final QueueCfg qc : queueCfgs) {
			em.persist(qc.getServerCfg());
			em.persist(qc.getDestCfg());
			for (final ExchangeCfg ec : qc.getExchanges()) {
				em.persist(ec);
			}
			em.persist(qc);
		}
		
		log.info(em.createQuery("select count(id) from ServerCfg sc").getSingleResult() + " server are loaded");
	}
	
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
	public void testBinding() {
		QueueCfgDao qcDao = new QueueCfgDao(em);
		List<QueueCfg> qcs = qcDao.findAll();
		
		System.out.println(MiscUtils.toXML(qcs.get(0)));
	}
	
	@AfterClass
	public static void clean() {
		em.close();
		emf.close();
	}
}
