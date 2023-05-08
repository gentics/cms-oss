/*
 * @author tobiassteiner
 * @date Jan 30, 2011
 * @version $Id: FolderDescriptionInputChannel.java,v 1.1.2.1 2011-02-10 13:43:32 tobiassteiner Exp $
 */
package com.gentics.contentnode.validation.map.inputchannels;

import com.gentics.contentnode.validation.map.NodeInputChannel;
import com.gentics.contentnode.validation.map.Policy;
import com.gentics.contentnode.validation.map.PolicyMap;

public class FolderDescriptionInputChannel extends NodeInputChannel {

	public FolderDescriptionInputChannel(int nodeId) {
		super(nodeId);
	}

	@Override
	public Policy getPolicyFromNode(PolicyMap.Node node) {
		return node.getFolderDescriptionPolicy();
	}
}
