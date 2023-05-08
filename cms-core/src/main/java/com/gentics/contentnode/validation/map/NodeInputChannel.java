/*
 * @author tobiassteiner
 * @date Dec 19, 2010
 * @version $Id: NodeInputChannel.java,v 1.1.2.1 2011-02-10 13:43:35 tobiassteiner Exp $
 */
package com.gentics.contentnode.validation.map;

import com.gentics.contentnode.validation.map.inputchannels.InputChannel;

/**
 * Represents channels of node-bound input (e.g. folder names).
 */
public class NodeInputChannel implements InputChannel {
    
	private final int localId;

	public NodeInputChannel(int nodeId) {
		if (1 > nodeId) {
			throw new IllegalArgumentException("Invalid node id `" + nodeId + "'");
		}
		this.localId = nodeId;
	}
    
	public Policy getPolicy(PolicyMap map) {
		// either the node with the given localId...
		PolicyMap.Node node = map.getNodeById(localId);

		// ...or the default node
		if (null == node) {
			node = map.getDefaultNode();
		}
		if (null == node) {
			// return the global default policy as ultimate fallback 
			return map.getDefaultPolicy();
		}
		// either input channel specific policy...
		Policy policy = getPolicyFromNode(node);

		if (null == policy) {
			// ...or return the node default policy as fallback
			return node.getDefaultPolicy();
		}
		return policy;
	}

	/**
	 * Should be overridden by subclasses to return the specific
	 * policy ref that the input channel represents
	 * @return the default implementation will always use the
	 *   default policy for the node.
	 */
	public Policy getPolicyFromNode(PolicyMap.Node node) {
		return node.getDefaultPolicy();
	}
    
	@Override
	public String toString() {
		return getClass().getName() + " (localId: " + localId + ")";
	}
}
