/*
 * @author tobiassteiner
 * @date Dec 18, 2010
 * @version $Id: InputChannel.java,v 1.1.2.1 2011-02-10 13:43:32 tobiassteiner Exp $
 */
package com.gentics.contentnode.validation.map.inputchannels;

import com.gentics.contentnode.validation.map.Policy;
import com.gentics.contentnode.validation.map.PolicyMap;

/**
 * Specifies some method of inputting data into the system. E.g. HTML input fields. 
 */
public interface InputChannel {

	/**
	 * The policy for this channel, which may be a policy configured in the policy-map,
	 * a policy provided during construction (for configurable input channels), or a
	 * default/fallback policy if none is configured.
	 * 
	 * This method must at least return the global default policy of the given map,
	 * if no more-appropriate policy is available. This method may only return null
	 * if the global default policy of the given map is null.
	 */
	Policy getPolicy(PolicyMap map);

	/**
	 * Subclasses should override this method so that reasonable log
	 * messages can be constructed.
	 */
	String toString();
}
