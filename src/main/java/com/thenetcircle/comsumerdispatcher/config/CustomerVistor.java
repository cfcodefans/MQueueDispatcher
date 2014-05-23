package com.thenetcircle.comsumerdispatcher.config;

import java.lang.reflect.InvocationTargetException;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.Attribute;
import org.dom4j.Element;
import org.dom4j.VisitorSupport;

/**
 * this class maps the attributes of pojo with xml node's attribute;
 */
public class CustomerVistor extends VisitorSupport {

	private Object object;

	public CustomerVistor(Object object) {
		this.object = object;
	}

	public void visit(Element node) {
		try {
			BeanUtils.copyProperty(object, node.getName(), node.getText());
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}

	public void visit(Attribute attr) {
		try {
//			if (null == attr.getText() || attr.getText().isEmpty()) {
			if (StringUtils.isEmpty(attr.getText())) {
				BeanUtils.copyProperty(object, attr.getName(), attr.getValue());
			} else {
				BeanUtils.copyProperty(object, attr.getName(), attr.getText());
			}
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}
}
