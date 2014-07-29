package com.thenetcircle.services.cluster;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.Receiver;
import org.jgroups.View;

import com.thenetcircle.services.common.Jsons;
import com.thenetcircle.services.common.MiscUtils;
import com.thenetcircle.services.dispatcher.ampq.MQueues;
import com.thenetcircle.services.dispatcher.dao.QueueCfgDao;
import com.thenetcircle.services.dispatcher.entity.QueueCfg;
import com.thenetcircle.services.persistence.jpa.JpaModule;

public class JGroupsActor implements Receiver {
	
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
		public Set<Integer> qcIds;
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
			
			ch = new JChannel();
			ch.setReceiver(this);
			ch.connect(CLUSTER_NAME);

			Message msg = new Message();
			msg.setBuffer(String.format("%s joined the %s", ch.getAddress(), CLUSTER_NAME).getBytes());
			ch.send(msg);
		} catch (Exception e) {
			log.error("can't initiate JGroups!!", e);
		}
	}
	
	public synchronized void send(Command cmd) {
		Message msg = new Message();
		String msgStr = Jsons.toString(cmd);
		try {
			ch.send(msg);
		} catch (Exception e) {
			log.error("can't send command: " + msgStr, e);
		}
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
				ch.clearChannelListeners();
				ch.disconnect();
				ch.close();
				ch = null;
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
		final String msgStr = new String(msg.getBuffer());
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
	}

	@Override
	public void getState(OutputStream output) throws Exception {
		log.info(ch.getAddress() + ": " + MiscUtils.invocInfo());
	}

	@Override
	public void setState(InputStream input) throws Exception {
		log.info(ch.getAddress() + ": " + MiscUtils.invocInfo());
	}

	@Override
	public void viewAccepted(View new_view) {
		log.info(ch.getAddress() + ": " + MiscUtils.invocInfo());
		log.info(new_view);
	}

	@Override
	public void suspect(Address suspected_mbr) {
		log.info(ch.getAddress() + ": " + MiscUtils.invocInfo());
	}

	@Override
	public void block() {
		log.info(ch.getAddress() + ": " + MiscUtils.invocInfo());
	}

	@Override
	public void unblock() {
		log.info(ch.getAddress() + ": " + MiscUtils.invocInfo());
	}
}
