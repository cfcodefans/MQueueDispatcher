package com.thenetcircle.services.dispatcher.failsafe;

import java.util.Collection;

import com.thenetcircle.services.dispatcher.ampq.MessageContext;

public interface IFailsafe {
	void handle(final Collection<MessageContext> mcs);

	MessageContext handle(final MessageContext mc);

	MessageContext handover(final MessageContext mc);

	void handover(final Collection<MessageContext> mcs);
}
