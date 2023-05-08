/*
 * @author tobiassteiner
 * @date Jan 30, 2011
 * @version $Id: PageLanguageInputChannel.java,v 1.1.2.1 2011-03-07 18:42:00 tobiassteiner Exp $
 */
package com.gentics.contentnode.validation.map.inputchannels;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.validation.map.NodeInputChannel;
import com.gentics.contentnode.validation.map.Policy;
import com.gentics.contentnode.validation.map.PolicyMap;

public class PageLanguageInputChannel extends NodeInputChannel {

	public PageLanguageInputChannel(int nodeId) {
		super(nodeId);
	}
    
	public PageLanguageInputChannel(Node node) {
		this(ObjectTransformer.getInt(node.getId(), 0));
	}

	@Override
	public Policy getPolicyFromNode(PolicyMap.Node node) {
		return node.getPageLanguagePolicy();
	}
}
