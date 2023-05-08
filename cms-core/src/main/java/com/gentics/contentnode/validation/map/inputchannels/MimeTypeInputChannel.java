/*
 * @author tobiassteiner
 * @date Mar 6, 2011
 * @version $Id: MimeTypeInputChannel.java,v 1.1.2.1 2011-03-07 18:42:00 tobiassteiner Exp $
 */
package com.gentics.contentnode.validation.map.inputchannels;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.validation.map.NodeInputChannel;
import com.gentics.contentnode.validation.map.Policy;
import com.gentics.contentnode.validation.map.PolicyMap;

public class MimeTypeInputChannel extends NodeInputChannel {

	public MimeTypeInputChannel(int nodeId) {
		super(nodeId);
	}
    
	public MimeTypeInputChannel(Node node) {
		this(ObjectTransformer.getInt(node.getId(), 0));
	}
    
	public Policy getPolicyFromNode(PolicyMap.Node node) {
		return node.getMimeTypePolicy();
	}
}
