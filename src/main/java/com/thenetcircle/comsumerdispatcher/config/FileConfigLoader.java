package com.thenetcircle.comsumerdispatcher.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.xml.sax.SAXException;

public class FileConfigLoader implements ConfigLoader {
	private static Log _logger = LogFactory.getLog(FileConfigLoader.class);

	protected String filePath;
	protected Document doc;

	public FileConfigLoader(String filePath) {
		// if (!validateConfig(filePath)) {
		// System.exit(-1);
		// }

		this.filePath = filePath;
		SAXReader reader = new SAXReader();
		try {
			_logger.info("loading conf file from " + filePath);
			Map map = new HashMap();
			map.put("tnc", "tnc");
			reader.getDocumentFactory().setXPathNamespaceURIs(map);
			doc = reader.read(filePath);
		} catch (DocumentException e) {
			_logger.error("[File Cofing Loader] error while loading: " + e, e);
		}
	}

	private boolean validateConfig(String filePath) {
		SchemaFactory factory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
		try {
			InputStream resourceAsStream = FileConfigLoader.class.getResourceAsStream("/job.xsd");
			Schema schema = factory.newSchema(new StreamSource(resourceAsStream));
			Validator validator = schema.newValidator();
			Source source = new StreamSource(filePath);
			validator.validate(source);

			_logger.info(filePath + " is valid");
			return true;
		} catch (SAXException e) {
			_logger.error(filePath + " is not valid because ");
			_logger.error(e.getMessage());
		} catch (IOException e) {
			_logger.error("can't load file", e);
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	public List<DispatcherJob> loadAllJobs() {
		Node reqPre = doc.selectSingleNode("//tnc:jobs/tnc:conf/tnc:request-pre");
		String defReqPre = reqPre.getText();

		reqPre = doc.selectSingleNode("//tnc:jobs/tnc:conf/tnc:request-host");
		String defReqHost = null;
		if (reqPre != null)
			defReqHost = reqPre.getText();

		reqPre = doc.selectSingleNode("//tnc:jobs/tnc:conf/tnc:request-count");

		int defCount = Integer.valueOf(reqPre.getText());
		reqPre = doc.selectSingleNode("//tnc:jobs/tnc:conf/tnc:request-timeout");

		int defTimeout = Integer.valueOf(reqPre.getText());
		reqPre = doc.selectSingleNode("//tnc:jobs/tnc:conf/tnc:encoding");

		String defEncoding = null;
		if (reqPre != null)
			defEncoding = reqPre.getText();

		reqPre = doc.selectSingleNode("//tnc:jobs/tnc:conf/tnc:redis");
		if (reqPre != null) {

		}

		List<DispatcherJob> allJobs = null;
		List<Element> list = doc.selectNodes("//tnc:jobs/tnc:job");
		if (null != list && !list.isEmpty()) {
			allJobs = new ArrayList<DispatcherJob>();
			for (Iterator<Element> iter = list.iterator(); iter.hasNext();) {
				Element element = iter.next();
				DispatcherJob je = new DispatcherJob();
				je.setDefaultUrl(defReqPre);
				je.setDefaultUrlHost(defReqHost);
				je.setDefaultCount(defCount);
				je.setDefaultTimeout(defTimeout);
				je.setDefaultEncoding(defEncoding);

				QueueConf qc = DispatcherConfig.getInstance().getServers().get(element.attributeValue("server"));
				je.setFetcherQConf(qc);
				element.accept(new CustomerVistor(je));
				allJobs.add(je);
			}
		}
		return allJobs;
	}

	public MonitorConf loadJmxConfig() {
		MonitorConf monitorConf = new MonitorConf();
		Element monitor = (Element) doc.selectNodes("//tnc:jobs/tnc:monitor").get(0);
		monitorConf.setJmxRmiHost(monitor.attributeValue("rHost"));
		monitorConf.setJmxRmiPort(Integer.valueOf(monitor.attributeValue("rPort")));
		monitorConf.setJmxHttpHost(monitor.attributeValue("httpHost"));
		monitorConf.setJmxHttpPort(Integer.valueOf(monitor.attributeValue("httpPort")));
		return monitorConf;
	}

	@SuppressWarnings("unchecked")
	public Map<String, QueueConf> loadServers() {
		List<Element> serverNodes = doc.selectNodes("//tnc:jobs/tnc:servers/tnc:queueserver");
		Map<String, QueueConf> servers = null;
		if (null != serverNodes && !serverNodes.isEmpty()) {
			servers = new HashMap<String, QueueConf>();
			for (Iterator<Element> iter = serverNodes.iterator(); iter.hasNext();) {
				Element qs = iter.next();

				// changed by fan@thenetcircle.com for issue:
				// http://sylvester:8001/issues/18938
				// QueueConf qc = new QueueConf(qs.attributeValue("name"),
				// qs.attributeValue("host"),
				// Integer.valueOf(qs.attributeValue("port")),
				// qs.attributeValue("userName"),
				// qs.attributeValue("password"),
				// qs.attributeValue("vhost"));
				QueueConf qc = new QueueConf(qs.attributeValue("name"), qs.attributeValue("host"), Integer.valueOf(qs.attributeValue("port")), qs.attributeValue("userName"),
						qs.attributeValue("password"), qs.attributeValue("vhost"), qs.attributeValue("logFileName"), qs.attributeValue("maxFileSize"));
				qc.setRedisHost(qs.attributeValue("redisHost"));
				qc.setRedisPort(qs.attributeValue("redisPort"));

				servers.put(qs.attributeValue("name"), qc);
			}
		}
		return servers;
	}
}
