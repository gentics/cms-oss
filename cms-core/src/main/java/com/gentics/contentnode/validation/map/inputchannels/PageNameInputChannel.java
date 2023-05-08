/*
 * @author tobiassteiner
 * @date Jan 30, 2011
 * @version $Id: PageNameInputChannel.java,v 1.1.2.2 2011-02-26 08:57:44 tobiassteiner Exp $
 */
package com.gentics.contentnode.validation.map.inputchannels;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.validation.map.NodeInputChannel;
import com.gentics.contentnode.validation.map.Policy;
import com.gentics.contentnode.validation.map.PolicyMap;

public class PageNameInputChannel extends NodeInputChannel {

	public PageNameInputChannel(int nodeId) {
		super(nodeId);
	}
    
	public PageNameInputChannel(Node node) {
		this(ObjectTransformer.getInt(node.getId(), 0));
	}

	@Override
	public Policy getPolicyFromNode(PolicyMap.Node node) {
		return node.getPageNamePolicy();
	}
}
