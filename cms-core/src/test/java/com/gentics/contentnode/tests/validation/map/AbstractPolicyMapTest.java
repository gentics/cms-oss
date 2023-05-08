package com.gentics.contentnode.tests.validation.map;

import java.io.InputStream;
import java.net.URI;

import junit.framework.TestCase;

import com.gentics.contentnode.validation.map.Policy;
import com.gentics.contentnode.validation.map.PolicyGroup;

public abstract class AbstractPolicyMapTest extends TestCase {
	public static final String PASS_THROUGH_POLICY_URI = "http://example.com/passThrough";
	public static final String MINIMAL_POLICY_MAP = "policy-map.minimal.xml";
	public static final String CUSTOM_POLICY_MAP = "policy-map.custom.xml";
	// part id of a <part localId=""> in the custom policy map
	public static final int CUSTOM_PART_TYPE_ID = 1234;
	// local node id of a <node localId=""> in the custom policy map
	public static final int CUSTOM_NODE_ID = 4321;
	// number of policies in the "PolicyGroup" group in the custom policy map 
	// this does not include the <default> policy sepcification, as that one is redundant
	// if there is a <policy> specification with the same name in the group.
	public static final int CUSTOM_NUM_POLICIES_IN_GROUP = 2;
	
	public static InputStream getCustomPolicyMapAsStream() {
		return AbstractPolicyMapTest.class.getResourceAsStream(CUSTOM_POLICY_MAP);
	}

	public static InputStream getMinimalPolicyMapAsStream() {
		return AbstractPolicyMapTest.class.getResourceAsStream(MINIMAL_POLICY_MAP);
	}
	
	/**
	 * Asserts that the policy group "PolicyGroup" in the custom policy map is
	 * structured correctly.
	 */
	protected void assertCustomPolicyGroup(PolicyGroup group) throws Exception {
		assertEquals("number of policies in test group doesn't match", CUSTOM_NUM_POLICIES_IN_GROUP, group.getPolicies().size());
		assertPolicyInGroup(group.getDefaultPolicy(), group);
		// first one should be passThrough
		assertPassThrough(group.getPolicies().get(0).getURI());
	}
	
	protected void assertPassThrough(URI uri) throws Exception {
		assertEquals("doesn't match passThrough Policy URI,", new URI(PASS_THROUGH_POLICY_URI), uri);
	}
	
	protected void assertPolicyInGroup(Policy policy, PolicyGroup group) {
		for (Policy p : group.getPolicies()) {
			if (p.getURI().equals(policy.getURI())) {
				return;
			}
		}
		assertFalse("policy not contained in group,", true);
	}
}
