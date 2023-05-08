/*
 * @author tobiassteiner
 * @date Jan 31, 2011
 * @version $Id: FallbackInputChannel.java,v 1.1.2.2 2011-03-07 18:42:00 tobiassteiner Exp $
 */
package com.gentics.contentnode.validation.map.inputchannels;

import com.gentics.contentnode.validation.map.Policy;
import com.gentics.contentnode.validation.map.PolicyMap;

/**
 * Splits {@link InputChannel#getPolicy(PolicyMap)} between
 * {@link #getSpecificPolicy(PolicyMap)} and {@link #getFallbackPolicy(PolicyMap)}.
 */
abstract public class FallbackInputChannel implements InputChannel {

	/**
	 * @return the specific policy for this input channel, or null,
	 *   if there is no specific policy configured in the given map.
	 */
	abstract public Policy getSpecificPolicy(PolicyMap map);

	/**
	 * @return either the specific policy, or if it is null, the
	 *   fallback policy.
	 */
	public Policy getPolicy(PolicyMap map) {
		Policy policy = getSpecificPolicy(map);

		if (null == policy) {
			policy = getFallbackPolicy(map);
		}
		return policy;
	}
    
	/**
	 * @return a fallback policy, which must be at least the
	 *   global default policy in the given policy map. This method
	 *   may return null only if the global default policy in the
	 *   given map is null.
	 */
	protected Policy getFallbackPolicy(PolicyMap map) {
		return map.getDefaultPolicy();
	}
}
