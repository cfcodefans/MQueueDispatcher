package com.thenetcircle.services.dispatcher.failsafe;

import com.thenetcircle.services.dispatcher.IMessageActor;

public interface IFailsafe extends IMessageActor {

	public static enum StorageType {
		sql, mongodb, orientdb, neo4j;
	}
}
