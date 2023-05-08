/*
 * @author tobiassteiner
 * @date Feb 1, 2011
 * @version $Id: GenericInputChannel.java,v 1.1.2.2 2011-02-26 08:57:44 tobiassteiner Exp $
 */
package com.gentics.contentnode.validation.map.inputchannels;

import com.gentics.contentnode.validation.map.Policy;
import com.gentics.contentnode.validation.map.PolicyMap;

/**
 * Any user-inputs not associated with a known channel will
 * use the global default policy.
 * 
 * Using this input channel is obviously not ideal. Instead,
 * each place this class is referenced should eventually
 * feature its own input channel, so that the channel can
 * be configured in the policy map to be as strict as possibly,
 * and to only allow as much input as necessary.
 */
public class GenericInputChannel implements InputChannel {
	public Policy getPolicy(PolicyMap map) {
		return map.getDefaultPolicy();
	}
}
