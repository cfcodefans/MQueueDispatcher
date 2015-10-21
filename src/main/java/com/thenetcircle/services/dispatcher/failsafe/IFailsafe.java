package com.thenetcircle.services.dispatcher.failsafe;

import com.thenetcircle.services.commons.actor.IActor;
import com.thenetcircle.services.dispatcher.entity.MessageContext;

public interface IFailsafe extends IActor<MessageContext> {

	public static enum StorageType {
		sql, mongodb, orientdb, neo4j;
	}
}
