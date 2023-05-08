package com.gentics.contentnode.factory;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Node;

/**
 * {@link AutoCloseable} implementation that will set the given channel into the current transaction when created and
 * reset the channel in {@link #close()}.
 */
public class ChannelTrx implements AutoCloseable {
	/**
	 * The transaction that was active when this object is created
	 */
	Transaction t = null;

	/**
	 * Create an instance that sets null as current channel
	 * @throws NodeException
	 */
	public ChannelTrx() throws NodeException {
		this(0);
	}

	/**
	 * Create an instance that sets the given id as channel into the current transaction
	 * @param channelId channel ID
	 * @throws NodeException
	 */
	public ChannelTrx(Integer channelId) throws NodeException {
		this.t = TransactionManager.getCurrentTransaction();
		t.setChannelId(ObjectTransformer.getInteger(channelId, 0) > 0 ? channelId : null);
	}

	/**
	 * Create an instance, if channel is not null, the channel will be set into the transaction
	 * @param channel channel (may be null)
	 * @throws NodeException
	 */
	public ChannelTrx(Node channel) throws NodeException {
		this(channel != null ? channel.getId() : null);
	}

	/**
	 * Reset the channel, if it was set in the constructor
	 */
	@Override
	public void close() throws NodeException {
		t.resetChannel();
	}
}
