/*
 * @author tobiassteiner
 * @date Dec 19, 2010
 * @version $Id: TagPartInputChannel.java,v 1.1.2.1 2011-02-10 13:43:32 tobiassteiner Exp $
 */
package com.gentics.contentnode.validation.map.inputchannels;

import java.net.URI;

import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.validation.map.Policy;
import com.gentics.contentnode.validation.map.PolicyGroup;
import com.gentics.contentnode.validation.map.PolicyMap;

/**
 * Represents input that happens for a tag part.
 * TODO: this should be a TagType input channel and the
 *   constructor shouldn't accept a Part instance.
 **/
public class TagPartInputChannel extends ConfigurableInputChannel {
    
	protected final int partTypeId;

	/**
	 * TagParts need not be handled individually - rather, only the type id
	 * need be known, and, if available, the policyURI (which may be configured
	 * individually for each part).
	 * @param partTypeId the part type id for this tag part
	 * @param policyURI the policyURI for this {@link ConfigurableInputChannel},
	 *   or null, if not yet configured.
	 * @see ConfigurableInputChannel 
	 */
	public TagPartInputChannel(int partTypeId, URI policyURI) {
		super(policyURI);
		if (1 > partTypeId) {
			throw new IllegalArgumentException("Invalid part type ID `" + partTypeId + "'");
		}
		this.partTypeId = partTypeId;
	}
    
	public TagPartInputChannel(Part part) {
		this(part.getPartTypeId(), part.getPolicyURI());
	}

	@Override
	public Policy getDefaultPolicy(PolicyMap map) {
		PolicyGroup group = getPolicyGroup(map);

		if (null != group) {
			return group.getDefaultPolicy();
		}
		return null;
	}
    
	@Override
	public PolicyGroup getPolicyGroup(PolicyMap map) {
		PolicyMap.PartType part = map.getPartTypeById(partTypeId);

		if (null != part) {
			return part.getPolicyGroup();
		}
		return map.getDefaultPolicyGroup();
	}
    
	@Override
	public String toString() {
		return super.toString() + " (partType: " + partTypeId + ")";
	}
}
