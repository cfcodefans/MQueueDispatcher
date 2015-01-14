package mgr.app;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.thenetcircle.services.commons.MiscUtils;

//@Named("AppBean")
@ApplicationScoped
//@Singleton
public class AppBean {

	public static final String BEAN_NAME = AppBean.class.getSimpleName();
	protected static final Log log = LogFactory.getLog(AppBean.class.getSimpleName());

	public AppBean() {
		log.info(MiscUtils.invocationInfo());
	}
	
	@PostConstruct
	public void init() {
		log.info(MiscUtils.invocationInfo());
	}
}
