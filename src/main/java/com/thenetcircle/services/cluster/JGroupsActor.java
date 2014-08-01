package com.thenetcircle.services.cluster;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;

import com.thenetcircle.services.common.Jsons;
import com.thenetcircle.services.common.MiscUtils;
import com.thenetcircle.services.dispatcher.ampq.MQueues;
import com.thenetcircle.services.dispatcher.dao.QueueCfgDao;
import com.thenetcircle.services.dispatcher.entity.QueueCfg;
import com.thenetcircle.services.persistence.jpa.JpaModule;

public class JGroupsActor extends ReceiverAdapter {
	private static final String EXTERNAL_JGRP_PROPERTIES = "jgroup_settings";
	
	public static enum CommandType {
		stop {
			@Override
			public void execute(Set<QueueCfg> qcs) {
				for (final QueueCfg qc : qcs) {
					MQueues.instance().removeQueueCfg(qc);
				}
			}
		},
		restart {
			@Override
			public void execute(Set<QueueCfg> qcs) {
				for (final QueueCfg qc : qcs) {
					MQueues.instance().updateQueueCfg(qc);
				}
			}
		};

		public abstract void execute(Set<QueueCfg> qcs);
	}
	
	public static class Command {
		public Set<Integer> qcIds = new HashSet<Integer>();
		public CommandType commandType;
		
		public void execute(Set<QueueCfg> qcs) {
			commandType.execute(qcs);
		}
	}
	
	private static final String CLUSTER_NAME = "mqueue_dispatcher_cluster";
	private JChannel ch = null;
	protected static final Log log = LogFactory.getLog(JGroupsActor.class.getSimpleName());

	public synchronized void start() {
		try {
			stop();
			
			ch = createChannel();
			
			ch.setReceiver(this);
			ch.connect(CLUSTER_NAME);
			log.info("join the jgroup cluster...");

//			Message msg = new Message();
//			msg.setBuffer(String.format("%s joined the %s", ch.getAddress(), CLUSTER_NAME).getBytes());
//			ch.send(msg);
		} catch (Exception e) {
			log.error("can't initiate JGroups!!", e);
		}
	}

	private JChannel createChannel() throws Exception {
		final String cfgPathStr = System.getProperty(EXTERNAL_JGRP_PROPERTIES);
		if (StringUtils.isBlank(cfgPathStr)) {
			log.error("JGroup cluster Configuration file is not set: " + cfgPathStr);
			return new JChannel();
		}
		
		final File cfgFile = new File(cfgPathStr);
		if (!(cfgFile.exists() && cfgFile.isFile())) {
			log.error("JGroup cluster Configuration file is invalide: " + cfgPathStr);
			return new JChannel();
		}
		
		log.info(String.format("loading JGroup cluster from jgroup_settings file [%s]......", cfgPathStr));
		return new JChannel(cfgFile);
	}
	
	public synchronized void send(final Command cmd) {
		final String msgStr = Jsons.toString(cmd);
		log.info("sending command to JGroup: \n\t" + msgStr);
		try {
			ch.send(new Message(null, msgStr));
		} catch (Exception e) {
			log.error("can't send command: " + msgStr, e);
		}
	}
	
	public synchronized void stopQueues(final QueueCfg... qcs) {
		if (ArrayUtils.isEmpty(qcs)) {
			return;
		}
		
		final Command stopCmd = new Command();
		for (final QueueCfg qc : qcs) {
			stopCmd.qcIds.add(qc.getId());
		}
		stopCmd.commandType = CommandType.stop;
		send(stopCmd);
	}
	
	public synchronized void restartQueues(final QueueCfg... qcs) {
		if (ArrayUtils.isEmpty(qcs)) {
			return;
		}
		
		final Command stopCmd = new Command();
		for (final QueueCfg qc : qcs) {
			stopCmd.qcIds.add(qc.getId());
		}
		stopCmd.commandType = CommandType.restart;
		send(stopCmd);
	}

	private JGroupsActor() {

	}

	private static JGroupsActor instance = new JGroupsActor();

	public static JGroupsActor instance() {
		return instance;
	}

	public synchronized void stop() {
		try {
			if (ch != null) {
				log.info("leave the jgroup cluster...");
				ch.clearChannelListeners();
				ch.disconnect();
				ch.close();
				ch = null;
				log.info("left the jgroup cluster...");
			}
		} catch (Exception e) {
			log.error("can't stop JGroups!!", e);
		}
	}

	@Override
	public void receive(Message msg) {
		if (msg.getSrc().equals(ch.getAddress())) {
			return;
		}
		
		log.info(ch.getAddress() + ": " + MiscUtils.invocInfo());
		final byte[] buf = msg.getBuffer();
		if (ArrayUtils.isEmpty(buf)) {
			return;
		}
		
		final String msgStr = (new String(buf)).trim();
		log.info(msgStr);
		
		Command cmd = Jsons.read(msgStr, Command.class);
		if (cmd == null) {
			log.error("can't execute command: " + msgStr);
			return;
		}
		
		final QueueCfgDao qcDao = new QueueCfgDao(JpaModule.getEntityManager());
		final Set<QueueCfg> qcs = new HashSet<QueueCfg>();
		for (final Integer id : cmd.qcIds) {
			final QueueCfg qc = qcDao.find(id);
			if (qc == null)
				continue;
			qcs.add(qc);
		}
		
		cmd.execute(qcs);
		qcDao.clean();
	}

	@Override
	public void getState(final OutputStream output) throws Exception {
		log.info(ch.getAddress() + ": " + MiscUtils.invocInfo());
	}

	@Override
	public void setState(final InputStream input) throws Exception {
		log.info(ch.getAddress() + ": " + MiscUtils.invocInfo());
	}

	@Override
	public void viewAccepted(final View new_view) {
		log.info(ch.getAddress() + ": " + MiscUtils.invocInfo());
		log.info(new_view);
	}

	@Override
	public void suspect(final Address suspected_mbr) {
//		log.info(ch.getAddress() + ": " + MiscUtils.invocInfo());
	}

	@Override
	public void block() {
//		log.info(ch.getAddress() + ": " + MiscUtils.invocInfo());
	}

	@Override
	public void unblock() {
//		log.info(ch.getAddress() + ": " + MiscUtils.invocInfo());
	}
}
