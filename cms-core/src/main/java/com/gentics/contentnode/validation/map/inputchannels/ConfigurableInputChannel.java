/*
 * @author tobiassteiner
 * @date Dec 19, 2010
 * @version $Id: ConfigurableInputChannel.java,v 1.1.2.1 2011-02-10 13:43:33 tobiassteiner Exp $
 */
package com.gentics.contentnode.validation.map.inputchannels;

import java.net.URI;

import com.gentics.contentnode.validation.map.Policy;
import com.gentics.contentnode.validation.map.PolicyGroup;
import com.gentics.contentnode.validation.map.PolicyMap;

abstract public class ConfigurableInputChannel extends FallbackInputChannel {
	protected final URI policyURI;
    
	/**
	 * Configures the given policyURI for this input channel, instead of 
	 * looking up the policy for a specific input channel configured in the
	 * policy map. Passing null instead of a policyURI will leave the
	 * input channel unconfigured, making it always return the default policy.
	 */
	public ConfigurableInputChannel(URI policyURI) {
		this.policyURI = policyURI; 
	}
    
	/**
	 * If this input channel was constructed with a policyURI, it will
	 * look up the policy in the given policy map, otherwise it will return the
	 * default policy.
	 */
	@Override
	public Policy getSpecificPolicy(PolicyMap map) {
		if (null != policyURI) {
			return map.getPolicyByURI(policyURI);
		} else {
			return getDefaultPolicy(map);
		}
	}
    
	/**
	 * If no policy is configured, the default policy is used. If this returns
	 * null, the global default policy is being used
	 * {@link FallbackInputChannel#getFallbackPolicy(PolicyMap)}.
	 */
	public abstract Policy getDefaultPolicy(PolicyMap map);

	/**
	 * Only policies in this policy-group should be used to call the
	 * constructor with. Can be used to display a selection of available
	 * policies to the user.
	 */
	public abstract PolicyGroup getPolicyGroup(PolicyMap map);
    
	@Override
	public String toString() {
		return getClass().getName() + " (" + (null == policyURI ? "unconfigured" : "configured policy: " + policyURI) + ")";
	}
}
