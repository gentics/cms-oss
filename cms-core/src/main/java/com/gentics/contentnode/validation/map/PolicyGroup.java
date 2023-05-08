/*
 * @author tobiassteiner
 * @date Jan 18, 2011
 * @version $Id: PolicyGroup.java,v 1.1.2.1 2011-02-10 13:43:35 tobiassteiner Exp $
 */
package com.gentics.contentnode.validation.map;

import java.util.List;

import com.gentics.contentnode.validation.map.PolicyMapModel.DefaultPolicyRef;
import com.gentics.contentnode.validation.map.PolicyMapModel.PolicyGroupModel;
import com.gentics.contentnode.validation.map.PolicyMapModel.PolicyRef;
import com.gentics.contentnode.validation.util.jaxb.XmlDerefView;
import com.gentics.contentnode.validation.util.jaxb.XmlFinishedUnmarshalListener;
import com.gentics.contentnode.validation.util.jaxb.XmlFinishedUnmarshalPropagator;

/**
 * This class depends on the {@link XmlFinishedUnmarshalPropagator} to be used,
 * otherwise the _default property will not be initialized. 
 */
public class PolicyGroup extends PolicyGroupModel implements XmlFinishedUnmarshalListener {
	protected List<Policy> policies = new XmlDerefView<Policy, PolicyRef>(policyRefs);
	protected PolicyRef _default;
    
	/**
	 * @return the Policy that was specified as the &lt;default> for this policy group.
	 *   each PolicyGroup must have a default, so this will never be null.
	 */
	public Policy getDefaultPolicy() {
		return _default.getRef();
	}

	/**
	 * @return the policies in this group.
	 */
	public List<Policy> getPolicies() {
		return policies;
	}
    
	public void finishedUnmarshal(Object parent) {
		initDefaultPolicy();
	}
    
	protected void initDefaultPolicy() {
		// there must be exactly one default policy
		boolean found = false;

		for (PolicyRef policy : policyRefs) {
			if (policy instanceof DefaultPolicyRef) {
				if (found) {
					throw new IllegalStateException("More than one default policy within policy group `" + id + "'");
				}
				found = true;
				_default = policy;
			}
		}
		if (!found) {
			throw new IllegalStateException("No default policy for policy group `" + id + "'");
		}
	}
}
