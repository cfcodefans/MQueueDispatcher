import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.thenetcircle.comsumerdispatcher.config.DispatcherConfig;
import com.thenetcircle.comsumerdispatcher.config.QueueConf;
import com.thenetcircle.services.dispatcher.entity.ExchangeCfg;
import com.thenetcircle.services.dispatcher.entity.QueueCfg;
import com.thenetcircle.services.dispatcher.entity.ServerCfg;


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
		em.createQuery("select count(id) from ServerCfg sc").getSingleResult();
		em.createQuery("select count(id) from ExchangeCfg ec").getSingleResult();
		em.createQuery("select count(id) from QueueCfg qc").getSingleResult();
		em.createQuery("select mc from MessageContext mc").getResultList();
	}
	
	
	
	@Test
	public void loadCfgsIntoDatabase() throws Exception {
		final DispatcherConfig dispatcherConfig = DispatcherConfig.getInstance();
		dispatcherConfig.loadConfig("job.xml");
		
		log.info("configuration is loaded");
		
//		final List<QueueConf> qcs = new ArrayList<QueueConf>(dispatcherConfig.getServers().values());
		
		Collection<QueueCfg> queueCfgs = DispatcherConfig.dispatcherJobsToQueueCfgs(dispatcherConfig.getAllJobs());
		
		log.info(queueCfgs.size() + " queues are loaded");
		startTransaction();
		for (final QueueCfg qc : queueCfgs) {
			em.persist(qc.getServerCfg());
			em.persist(qc.getDestCfg());
			for (final ExchangeCfg ec : qc.getExchanges()) {
//				em.persist(ec.getServerCfg());
				em.persist(ec);
			}
			em.persist(qc);
		}
//		commit();
		
		log.info(em.createQuery("select count(id) from ServerCfg sc").getSingleResult() + " server are loaded");
	}
	
	@AfterClass
	public static void clean() {
		em.close();
		emf.close();
	}
}
