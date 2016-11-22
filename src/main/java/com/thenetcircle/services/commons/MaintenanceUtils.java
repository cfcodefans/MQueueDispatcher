package com.thenetcircle.services.commons;

import org.apache.commons.lang3.StringUtils;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

/**
 * There is commons-email lib from apache, not really need to reinvent wheels
 * 
 * @author fan
 *
 */
@Deprecated
public class MaintenanceUtils {

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

}
