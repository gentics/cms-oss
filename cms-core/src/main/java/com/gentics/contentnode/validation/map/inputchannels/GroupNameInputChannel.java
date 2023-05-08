/*
 * @author tobiassteiner
 * @date Mar 6, 2011
 * @version $Id: GroupNameInputChannel.java,v 1.1.2.1 2011-03-07 18:42:00 tobiassteiner Exp $
 */
package com.gentics.contentnode.validation.map.inputchannels;

import com.gentics.contentnode.validation.map.Policy;
import com.gentics.contentnode.validation.map.PolicyMap;

public class GroupNameInputChannel extends FallbackInputChannel {
	@Override
	public Policy getSpecificPolicy(PolicyMap map) {
		return map.getGroupNamePolicy();
	}
}
