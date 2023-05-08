/*
 * @author tobiassteiner
 * @date Jan 30, 2011
 * @version $Id: FolderNameInputChannel.java,v 1.1.2.1 2011-02-10 13:43:33 tobiassteiner Exp $
 */
package com.gentics.contentnode.validation.map.inputchannels;

import com.gentics.contentnode.validation.map.NodeInputChannel;
import com.gentics.contentnode.validation.map.Policy;
import com.gentics.contentnode.validation.map.PolicyMap;

public class FolderNameInputChannel extends NodeInputChannel {
    
	public FolderNameInputChannel(int nodeId) {
		super(nodeId);
	}

	@Override
	public Policy getPolicyFromNode(PolicyMap.Node node) {
		return node.getFolderNamePolicy();
	}
}
