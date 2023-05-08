package com.gentics.contentnode.factory;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Node;

/**
 * Autocloseable that sets the currently published node in
 * the current transaction.
 */
public class PublishedNodeTrx implements AutoCloseable {

	/**
	 * Convenience constructor.
	 * @see #PublishedNodeTrx(Integer)
	 * @param node The node being published.
	 * @throws TransactionException When <code>node</code> is <code>null</code>.
	 */
	public PublishedNodeTrx(Node node) throws TransactionException {
		this(node == null ? null : node.getId());
	}

	/**
	 * Set the ID of the node that is currently being published in the
	 * current transaction.
	 *
	 * @param nodeId The ID of the node currently being published.
	 * @throws TransactionException When <code>nodeId</code> is <code>null</code>
	 *		or less than or equal to 0.
	 */
	public PublishedNodeTrx(Integer nodeId) throws TransactionException {
		if (ObjectTransformer.getInt(nodeId, 0) <= 0) {
			throw new TransactionException("Published node ID may not be " + nodeId);
		}

		Transaction t = TransactionManager.getCurrentTransaction();
		Integer cur = t.getPublishedNodeId();

		if (ObjectTransformer.getInt(cur, 0) > 0) {
			throw new TransactionException(
				"Cannot set published node ID to " + nodeId + " because it is already set to " + cur);
		}

		TransactionManager.getCurrentTransaction().setPublishedNodeId(nodeId);
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.AutoCloseable#close()
	 */
	@Override
	public void close() throws NodeException {
		TransactionManager.getCurrentTransaction().setPublishedNodeId(null);
	}
}
