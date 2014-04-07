package com.thenetcircle.services.dispatcher.failsafe;

import com.thenetcircle.services.dispatcher.ampq.QueueCfg;

public class Failsafes {
	public static IFailSafe getFailsafe(final QueueCfg qc) {
		if (qc == null) return null;
	}
}
