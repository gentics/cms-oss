/*
 * @author tobiassteiner
 * @date Jan 31, 2011
 * @version $Id: FileDescriptionInputChannel.java,v 1.1.2.2 2011-02-26 08:57:44 tobiassteiner Exp $
 */
package com.gentics.contentnode.validation.map.inputchannels;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.validation.map.NodeInputChannel;
import com.gentics.contentnode.validation.map.Policy;
import com.gentics.contentnode.validation.map.PolicyMap;

public class FileDescriptionInputChannel extends NodeInputChannel {

	public FileDescriptionInputChannel(int nodeId) {
		super(nodeId);
	}
    
	public FileDescriptionInputChannel(Node node) {
		this(ObjectTransformer.getInt(node.getId(), 0));
	}
    
	public Policy getPolicyFromNode(PolicyMap.Node node) {
		return node.getFileDescriptionPolicy();
	}
}
