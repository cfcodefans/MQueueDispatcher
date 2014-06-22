package com.thenetcircle.services.rest.ajax;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;

import org.glassfish.jersey.server.model.Parameter;
import org.glassfish.jersey.server.model.Parameter.Source;

@XmlRootElement
@SuppressWarnings("rawtypes")
public class ParamMetaData implements Serializable {

	private static final long serialVersionUID = 1L;
	public Source source;
	public String sourceName;
	public Class rawType;
	public Class type;

	public static ParamMetaData build(Parameter param) {
		ParamMetaData pmd = new ParamMetaData();
		
		pmd.source = param.getSource();
		pmd.sourceName = param.getSourceName();
		pmd.rawType = param.getRawType();
		pmd.type = (Class) param.getType();
		
		return pmd;
	}

	@Override
	public String toString() {
		return "ParamMetaData [source=" + source + ", sourceName=" + sourceName + ", rawType=" + rawType + ", type=" + type + "]";
	}
	
}
