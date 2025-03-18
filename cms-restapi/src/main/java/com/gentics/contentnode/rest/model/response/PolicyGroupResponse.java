/*
 * @author tobiassteiner
 * @date Jan 18, 2011
 * @version $Id: PolicyGroupResponse.java,v 1.1.2.3 2011-02-10 13:43:41 tobiassteiner Exp $
 */
package com.gentics.contentnode.rest.model.response;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The policies in the group can be assumed to be in the same
 * order defined in the policy map.
 */
@XmlRootElement(name = "policyGroup")
@XmlType(propOrder = { "policies" })
public class PolicyGroupResponse {
	@XmlElementWrapper(name = "policies")
	@XmlElement(name = "policy")
	public List<GroupPolicyResponse> policies = new ArrayList<GroupPolicyResponse>();

	public PolicyGroupResponse() {}

	public static class GroupPolicyResponse extends PolicyResponse {
		@XmlAttribute(name = "default")
		@JsonProperty("default")
		public Boolean _default;

		public GroupPolicyResponse() {}
	}
}
