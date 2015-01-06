package com.thenetcircle.services.commons;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.ws.rs.core.MultivaluedMap;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.iterators.ObjectArrayIterator;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.apache.commons.lang3.event.EventUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import com.fasterxml.jackson.databind.ObjectMapper;

public class MiscUtils {
	public static final int AVAILABLE_PROCESSORS = Runtime.getRuntime().availableProcessors();

	public static long getPropertyNumber(String name, long defaultValule) {
		String str = System.getProperty(name);
		
		if (StringUtils.isNumeric(str)) {
			return Long.parseLong(str);
		}
		return defaultValule;
	}
	
	public static String invocationInfo() {
		StackTraceElement[] ste = Thread.currentThread().getStackTrace();
		int i = 2;
		return String.format("%s\t%s.%s", ste[i].getFileName(), ste[i].getClassName(), ste[i].getMethodName());
	}
	
	public static String invocInfo() {
		StackTraceElement[] ste = Thread.currentThread().getStackTrace();
		int i = 2;
		return String.format("%s\t%s.%s", ste[i].getFileName(), StringUtils.substringAfterLast(ste[i].getClassName(), ".") , ste[i].getMethodName());
	}

	public static String invocationInfo(final int i) {
		StackTraceElement[] ste = Thread.currentThread().getStackTrace();
		return String.format("%s\t%s.%s", ste[i].getFileName(), ste[i].getClassName(), ste[i].getMethodName());
	}

	public static String byteCountToDisplaySize(long size) {
		String displaySize;
		if (size / 1073741824L > 0L) {
			displaySize = String.valueOf(size / 1073741824L) + " GB";
		} else {
			if (size / 1048576L > 0L) {
				displaySize = String.valueOf(size / 1048576L) + " MB";
			} else {
				if (size / 1024L > 0L)
					displaySize = String.valueOf(size / 1024L) + " KB";
				else
					displaySize = String.valueOf(size) + " bytes";
			}
		}
		return displaySize;
	}

	public static long getProcessId() {
		// Note: may fail in some JVM implementations
		// therefore fallback has to be provided

		// something like '<pid>@<hostname>', at least in SUN / Oracle JVMs
		final String jvmName = ManagementFactory.getRuntimeMXBean().getName();
		final int index = jvmName.indexOf('@');
		String pidStr = jvmName.substring(0, index);

		if (index < 1 || !NumberUtils.isNumber(pidStr)) {
			// part before '@' empty (index = 0) / '@' not found (index = -1)
			return 0;
		}

		return Long.parseLong(pidStr);
	}

	public static class LoopingArrayIterator<E> extends ObjectArrayIterator<E> {
		@SafeVarargs
		public LoopingArrayIterator(final E... array) {
			super(array, 0, array.length);
		}

		public LoopingArrayIterator(final E array[], final int start) {
			super(array, start, array.length);
		}

		public E loop() {		
			final E[] array = this.getArray();
			loopIdx.compareAndSet(array.length, 0);
//			System.out.println(array.getClass().getName() + ".index: " + loopIdx.get());
			return array[loopIdx.getAndIncrement() % array.length];
		}
		
		private AtomicInteger loopIdx = new AtomicInteger();
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Map map(Object...keyAndVals) {
		return MapUtils.putAll(new HashMap(), keyAndVals);
	}

	public static long HOST_HASH = System.currentTimeMillis(); 
	
	static {
		try {
			HOST_HASH = InetAddress.getLocalHost().getHostAddress().hashCode();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}
	
//	private static long IDX = 0;
	
	public static long uniqueLong() {
		return Math.abs(UUID.randomUUID().hashCode());
	}
	
	public static ThreadFactory namedThreadFactory(final String name) {
		return new BasicThreadFactory.Builder().namingPattern(name + "_%d").build();
	}
	
	public static void sendMail(final String smtpHost, final int smtpPort, final String from, final String toAddrs, final String subject, final String content) throws Exception {
		final Properties props = new Properties();
		props.put("mail.smtp.host", smtpHost);
		props.put("mail.smtp.port", String.valueOf(smtpPort));
		
		Session session = Session.getDefaultInstance(props);
		
		Message msg = new MimeMessage(session);
		
		msg.setFrom(new InternetAddress(from));
		
		final List<InternetAddress> toAddrList = new LinkedList<InternetAddress>(); 
		for (final String toAddr : StringUtils.split(toAddrs, ",")) {
			toAddrList.add(new InternetAddress(toAddr));
		}
		
		msg.setRecipients(Message.RecipientType.TO, toAddrList.toArray(new InternetAddress[0]));
		
		msg.setSubject(subject);
		msg.setText(content);
		
		Transport.send(msg);
	}

	public static String lineNumber(final String str) {
		if (str == null) {
			return null;
		}
	
		final StringReader sr = new StringReader(str);
		final BufferedReader br = new BufferedReader(sr);
	
		final StringBuilder sb = new StringBuilder(0);
		long lineNumber = 0;
		try {
			for (String line = br.readLine(); line != null; line = br.readLine()) {
				sb.append(++lineNumber).append("\t").append(line).append('\n');
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	
		return sb.toString();
	}

	private static final Log log = LogFactory.getLog(EventUtils.class.getName());


	public static NameValuePair[] getParamPairs(Map<String, ?> paramMap) {
		List<NameValuePair> pairList = new ArrayList<NameValuePair>();
	
		for (Map.Entry<String, ?> en : paramMap.entrySet()) {
			pairList.add(new BasicNameValuePair(en.getKey(), String.valueOf(en.getValue())));
		}
	
		return pairList.toArray(new NameValuePair[0]);
	}

	public static String mapToJson(Map<String, String> paramMap) {
		if (MapUtils.isEmpty(paramMap)) {
			return "{}";
		}
	
		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.writeValueAsString(paramMap);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return paramMap.toString();
		}
	}

	public static String toXML(final Object bean) {
			final StringWriter sw = new StringWriter();
			
			try {
				JAXBContext jc = JAXBContext.newInstance(bean.getClass());
				Marshaller m = jc.createMarshaller();		
				m.setProperty(Marshaller.JAXB_FRAGMENT, true);
				m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
	//			marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-16");
				m.marshal(bean, sw);
			} catch (Exception e) {
				log.error(bean, e);
			}
			return sw.toString();
		}

	public static <T> T toObj(final String xmlStr, final Class<T> cls) {
		if (StringUtils.isBlank(xmlStr) || cls == null) {
			return null;
		}
		
		try {
			JAXBContext jc = JAXBContext.newInstance(cls);
			Unmarshaller um = jc.createUnmarshaller();
			return um.unmarshal(new StreamSource(new StringReader(xmlStr)), cls).getValue();
		} catch (JAXBException e) {
			log.error(xmlStr, e);
		}
		
		return null;
	}

	public static Map<String, String> extractParams(MultivaluedMap<String, String> params) {
		Map<String, String> paramsMap = new HashMap<String, String>();
		for (String key : params.keySet()) {
			paramsMap.put(key, params.getFirst(key));
		}
		return paramsMap;
	}

	public static Map<String, String[]> toParamMap(MultivaluedMap<String, String> params) {
		Map<String, String[]> paramsMap = new HashMap<String, String[]>();
		for (String key : params.keySet()) {
			final List<String> valList = params.get(key);
			paramsMap.put(key, valList.toArray(new String[0]));
		}
		return paramsMap;
	}

	public static Map<String, String> extractParams(Map<String, String[]> params) {
		Map<String, String> paramsMap = new HashMap<String, String>();
		for (String key : params.keySet()) {
			final String[] vals = params.get(key);
			paramsMap.put(key, ArrayUtils.isEmpty(vals) ? null : vals[0]);
		}
		return paramsMap;
	}

	public static String generate(final String text) {
		final StringBuffer sb = new StringBuffer();
		try {
			final byte[] intext = text.getBytes();
			final MessageDigest md5 = MessageDigest.getInstance("MD5");
			final byte[] md5rslt = md5.digest(intext);
			for (int i = 0; i < md5rslt.length; i++) {
				final int val = 0xff & md5rslt[i];
				if (val < 16) {
					sb.append("0");
				}
				sb.append(Integer.toHexString(val));
			}
		} catch (final Exception e) {
			log.error(e, e);
		}
		return sb.toString();
	}
}
