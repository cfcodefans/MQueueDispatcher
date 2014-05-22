package com.thenetcircle.services.dispatcher.cdi;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.spi.Context;
import javax.enterprise.inject.spi.BeanManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

import com.thenetcircle.services.common.WeldContext;
import com.thenetcircle.services.dispatcher.dao.ExchangeCfgDao;
import com.thenetcircle.services.dispatcher.dao.MessageContextDao;
import com.thenetcircle.services.dispatcher.dao.QueueCfgDao;
import com.thenetcircle.services.dispatcher.dao.ServerCfgDao;

public class WeldBinder extends AbstractBinder {
	
	private static Log log = LogFactory.getLog(WeldBinder.class);

	@Override
	protected void configure() {
		try {
			bind(WeldContext.INSTANCE.getBean(QueueCfgDao.class)).to(QueueCfgDao.class);
			bind(WeldContext.INSTANCE.getBean(ExchangeCfgDao.class)).to(ExchangeCfgDao.class);
			bind(WeldContext.INSTANCE.getBean(ServerCfgDao.class)).to(ServerCfgDao.class);
			bind(WeldContext.INSTANCE.getBean(MessageContextDao.class)).to(MessageContextDao.class);
		} catch (Exception e) {
			log.error("failed to bind", e);
		}
	}

}
