package com.thenetcircle.services.dispatcher.mgr;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.persistence.EntityManager;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.thenetcircle.services.common.MiscUtils;
import com.thenetcircle.services.dispatcher.dao.MessageContextDao;
import com.thenetcircle.services.dispatcher.dao.ServerCfgDao;
import com.thenetcircle.services.dispatcher.entity.ServerCfg;
import com.thenetcircle.services.persistence.jpa.JpaModule;

public class NotificationActor implements Runnable {

	protected static final Log log = LogFactory.getLog(NotificationActor.class.getName());
	
	private Date lastCheckTime = new Date();
	
	@Override
	public void run() {
		final Date now = new Date();
		reportFailedMessages(lastCheckTime, now);
		lastCheckTime = now;
	}

	public void reportFailedMessages(final Date start, final Date end) {
		final Map<String, StringBuilder> mailsAndContents = new HashMap<String, StringBuilder>();
		
		final EntityManager em = JpaModule.getEntityManager();
		
		final ServerCfgDao scDao = new ServerCfgDao(em);
		
		final List<ServerCfg> scList = scDao.findAll();
		
		scDao.close();
		
		final String mailStr = StringUtils.join(new String[] {
				
				"Dear Admins",
				String.format("from %s to %s", start, end),
				"there are :"
				
		}, "\n");
		
		
		final MessageContextDao mcDao = new MessageContextDao(JpaModule.getEntityManager());
		
		for (final ServerCfg sc : scList) {
			final String mails = sc.getMails();
			if (StringUtils.isBlank(mails)) {
				continue;
			}
			
			String contentStr = mcDao.queryFailedJobsReport(sc, start, end);
			if (StringUtils.isEmpty(contentStr)) {
				continue;
			}
				
			StringBuilder content = null;
			
			if (!mailsAndContents.containsKey(mails)) {
				content = new StringBuilder(mailStr);
				mailsAndContents.put(mailStr, content);
			} else {
				content = mailsAndContents.get(mails);
			}
			
			content.append(contentStr).append('\n');
		}
		
		for (final Map.Entry<String, StringBuilder> entry : mailsAndContents.entrySet()) {
			if (StringUtils.isBlank(entry.getKey()) || entry.getValue() == null) {
				continue;
			}
			try {
				MiscUtils.sendMail("localhost", 25, "dispatcher@thenetcircle.com", entry.getKey(), "failed message report", entry.getValue().toString());
			} catch (Exception e) {
				log.error("failed to send report by " + end, e);
			}
		}
	}
	
	private NotificationActor() {
		
	}
	
	private static NotificationActor _instance = new NotificationActor();
	private ScheduledExecutorService ses = null; 
	
	public static NotificationActor instance() {
		return _instance;
	}
	
	public void start() {
		stop();
		ses = Executors.newSingleThreadScheduledExecutor();
		ses.scheduleAtFixedRate(this, 30, 3600, TimeUnit.SECONDS);
	}
	
	public void stop() {
		if (!(ses == null || ses.isTerminated())) {
			ses.shutdownNow();
			ses = null;
		}
	}
	
}
