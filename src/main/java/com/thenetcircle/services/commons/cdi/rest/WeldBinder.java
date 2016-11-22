package com.thenetcircle.services.commons.cdi.rest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.jboss.weld.literal.AnyLiteral;
import org.jboss.weld.literal.DefaultLiteral;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;

public class WeldBinder extends AbstractBinder {

	private static Log	log	= LogFactory.getLog(WeldBinder.class);

	@Override
	protected void configure() {
		try {
			// bind(getBean(QueueCfgDao.class)).to(QueueCfgDao.class);
			// bind(getBean(ExchangeCfgDao.class)).to(ExchangeCfgDao.class);
			// bind(getBean(ServerCfgDao.class)).to(ServerCfgDao.class);
			// bind(getBean(MessageContextDao.class)).to(MessageContextDao.class);
			// bind(getBean(GeneralDao.class)).to(GeneralDao.class);
			// for (Iterator it = CDI.current().select(DefaultLiteral.INSTANCE).iterator(); it.hasNext();) {
			// log.info(it.next());
			// }

			BeanManager bm = CDI.current().getBeanManager();
			bm.getBeans(Object.class, DefaultLiteral.INSTANCE, AnyLiteral.INSTANCE).forEach(bean -> bindToJerseyContext(bm, bean));
		} catch (Exception e) {
			log.error("failed to bind", e);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void bindToJerseyContext(BeanManager bm, final Bean bean) {
		try {
			Object _ref = bm.getReference(bean, bean.getBeanClass(), bm.createCreationalContext(bean));
			bind(_ref).to(bean.getBeanClass());
		} catch (Exception e) {
			log.warn(String.format("failed to bind: %s \n\t%s", e.getStackTrace()[0], e.getMessage()));
		}
	}

	public static <T> T getBean(Class<T> cls) {
		return CDI.current().select(cls).get();
	}
}