package com.thenetcircle.services.dispatcher.failsafe;

import com.thenetcircle.services.dispatcher.entity.QueueCfg;

public class Failsafes {
	public static IFailsafe getFailsafe(final QueueCfg qc) {
		if (qc == null || qc.getFailsafeCfg() == null) return null;
		
		final IFailsafe fs = null;
		
		return fs;
	}
}
