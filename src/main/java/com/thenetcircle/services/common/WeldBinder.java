package com.thenetcircle.services.common;

import java.util.Iterator;

import javax.enterprise.inject.Instance;

import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.jboss.weld.environment.se.WeldContainer;
import org.jboss.weld.literal.QualifierLiteral;

public class WeldBinder extends AbstractBinder {

	@Override
	protected void configure() {
//		WeldContainer wc = WeldContext.INSTANCE.getContainer();
//		final CDI<Object> cdi = CDI.current();
		WeldContainer cdi = WeldContext.INSTANCE.getContainer();
//		cdi.getBeanManager().getBeans(Object.class, qualifiers)
		final Instance appInstances = cdi.instance().select(QualifierLiteral.INSTANCE);
		for (Iterator<Object> it = appInstances.iterator(); it.hasNext();) {
			Object obj = it.next();
			System.out.println(obj.getClass());
		}
	}

}
