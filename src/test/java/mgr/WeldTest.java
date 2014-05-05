package mgr;

import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;

import mgr.app.AppBean;
import mgr.dao.QueueCfgDao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.thenetcircle.services.common.MiscUtils;

public class WeldTest {
	static Weld weld;
	static WeldContainer container;
	static BeanManager bm;
	
	@BeforeClass
	public static void startUp() {
		log.info(MiscUtils.invocationInfo());
		try {
		weld = new Weld();
		container = weld.initialize();
		bm = container.getBeanManager();
		} catch (Exception e) {
			log.error("", e);
			System.exit(-1);
		}
	}
	
	protected static final Log log = LogFactory.getLog(WeldTest.class.getSimpleName());
	
	@Test
	public void testAppBeanWithName() {
		// log.info(bm.getBeans(AppBean.BEAN_NAME));
		log.info(MiscUtils.invocationInfo());
		try {
			log.info(container.instance().select(AppBean.class).get());
			log.info(container.instance().select(AppBean.class).get());
			
			final Set<Bean<?>> beans = bm.getBeans(AppBean.class);
			log.info(beans);
			final Bean<AppBean> b = (Bean<AppBean>) beans.iterator().next();
			final AppBean ab = bm.getContext(ApplicationScoped.class).get(b);
			log.info(ab == null ? "null" : ab);
		} catch (Exception e) {
			log.error("", e);
		}
	}
	
	@Before
	public void preTest() {
		log.info("\n\n");
	}
	
	@Test
	public void testDao() {
		log.info(MiscUtils.invocationInfo());
		
		try {
//			QueueCfgDao qcDao = (QueueCfgDao) bm.getContext(RequestScoped.class).get(bm.getBeans(QueueCfgDao.class).iterator().next());
			QueueCfgDao qcDao = container.instance().select(QueueCfgDao.class).get();
			log.info("QueueCfgDao: " + qcDao);
//			log.info("Entity Class is " + qcDao.getEntityClass());
		} catch (Exception e) {
			log.error("", e);
		}
	}
	
	@AfterClass
	public static void tearDown() {
		log.info(MiscUtils.invocationInfo());
		if (weld == null) {
			return;
		}
		weld.shutdown();
	}
	
	public static void main(String[] args) {
		startUp();
		WeldTest wt = new WeldTest();
		wt.testAppBeanWithName();
		tearDown();
	}
}
