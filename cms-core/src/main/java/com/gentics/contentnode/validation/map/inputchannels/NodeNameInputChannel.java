/*
 * @author tobiassteiner
 * @date Jan 31, 2011
 * @version $Id: NodeNameInputChannel.java,v 1.1.2.2 2011-03-07 18:42:00 tobiassteiner Exp $
 */
package com.gentics.contentnode.validation.map.inputchannels;

import com.gentics.contentnode.validation.map.Policy;
import com.gentics.contentnode.validation.map.PolicyMap;

public class NodeNameInputChannel extends FallbackInputChannel {
	@Override
	public Policy getSpecificPolicy(PolicyMap map) {
		return map.getNodeNamePolicy();
	}
}
