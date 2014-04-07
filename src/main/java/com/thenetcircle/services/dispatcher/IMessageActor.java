package com.thenetcircle.services.dispatcher;

import java.util.Collection;

import com.thenetcircle.services.dispatcher.ampq.MessageContext;

public interface IMessageActor {
	MessageContext handover(final MessageContext mc);

	void handover(final Collection<MessageContext> mcs);

	void handle(final Collection<MessageContext> mcs);

	MessageContext handle(final MessageContext mc);
}
