package com.thenetcircle.services.rest;

import java.util.Iterator;

import javax.enterprise.inject.spi.CDI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.jboss.weld.literal.DefaultLiteral;

import com.thenetcircle.services.dispatcher.dao.ExchangeCfgDao;
import com.thenetcircle.services.dispatcher.dao.MessageContextDao;
import com.thenetcircle.services.dispatcher.dao.QueueCfgDao;
import com.thenetcircle.services.dispatcher.dao.ServerCfgDao;

public class WeldBinder extends AbstractBinder {
	
	private static Log log = LogFactory.getLog(WeldBinder.class);

	@Override
	protected void configure() {
		try {
			bind(getBean(QueueCfgDao.class)).to(QueueCfgDao.class);
			bind(getBean(ExchangeCfgDao.class)).to(ExchangeCfgDao.class);
			bind(getBean(ServerCfgDao.class)).to(ServerCfgDao.class);
			bind(getBean(MessageContextDao.class)).to(MessageContextDao.class);
			
			for (Iterator it = CDI.current().select(DefaultLiteral.INSTANCE).iterator(); it.hasNext();) {
				log.info(it.next());
			}
		} catch (Exception e) {
			log.error("failed to bind", e);
		}
	}

	private <T> T getBean(Class<T> cls) {
		return CDI.current().select(cls).get();
	}

}