/*
 * @author tobiassteiner
 * @date Jan 31, 2011
 * @version $Id: GenericConfigurableInputChannel.java,v 1.1.2.1 2011-02-10 13:43:32 tobiassteiner Exp $
 */
package com.gentics.contentnode.validation.map.inputchannels;

import java.net.URI;

import com.gentics.contentnode.validation.map.Policy;
import com.gentics.contentnode.validation.map.PolicyGroup;
import com.gentics.contentnode.validation.map.PolicyMap;

/**
 * An input channel that can be configured, and uses the global default
 * policy if unconfigured.
 */
public class GenericConfigurableInputChannel extends ConfigurableInputChannel {
    
	public GenericConfigurableInputChannel(URI policyURI) {
		super(policyURI);
	}

	@Override
	public Policy getDefaultPolicy(PolicyMap map) {
		return map.getDefaultPolicy();
	}
    
	@Override
	public PolicyGroup getPolicyGroup(PolicyMap map) {
		return map.getDefaultPolicyGroup();
	}
}
