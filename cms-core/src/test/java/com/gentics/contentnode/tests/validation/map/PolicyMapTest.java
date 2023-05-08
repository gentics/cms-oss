package com.gentics.contentnode.tests.validation.map;

import java.io.File;
import java.io.InputStream;

import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.IOUtils;

import com.gentics.contentnode.validation.map.Policy;
import com.gentics.contentnode.validation.map.PolicyGroup;
import com.gentics.contentnode.validation.map.PolicyMap;
import com.gentics.testutils.fs.FileUtils;

public class PolicyMapTest extends AbstractPolicyMapTest {
	
	public void testLoadDefault() throws Exception {
		PolicyMap map = PolicyMap.loadDefault();
		// the default policy map must have all defaults set - ideally this
		// would be checked by a schema, but we musn't require this, so that
		// the user can selectively overwrite settings in the default map.
		Policy globalDefaultPolicy = map.getDefaultPolicy();

		assertNotNull("the default policy map must have a global default policy", globalDefaultPolicy);
		PolicyGroup defaultGroup = map.getDefaultPolicyGroup();

		assertNotNull("the default policy map must have a default policy group", defaultGroup);
		PolicyMap.Node node = map.getDefaultNode();

		assertNotNull("the default policy map must have a default node config", node);
		Policy nodeDefaultPolicy = node.getDefaultPolicy(); 

		assertNotNull("the default policy map must have a default node config with a default policy", nodeDefaultPolicy);
	}
	
	/**
	 * The minimal policy doesn't have anything defined, just some minimal
	 * structure to keep the schema happy. 
	 */
	public void testMinimalPolicy() throws Exception {
		InputStream stream = getMinimalPolicyMapAsStream();
		PolicyMap map;

		try {
			map = PolicyMap.load(new StreamSource(stream));
		} finally {
			stream.close();
		}
		
		Policy globalDefaultPolicy = map.getDefaultPolicy();

		assertNull("there should not be a default policy,", globalDefaultPolicy);
		PolicyGroup defaultGroup = map.getDefaultPolicyGroup();

		assertNull("there should not be a default policy group,", defaultGroup);
		PolicyMap.Node node = map.getDefaultNode();

		assertNotNull("there should be a default node config,", node);
		assertNull("there shouldn't be a default policy for the default node config,", node.getDefaultPolicy());
		assertEquals("there shouldn't be any policies defined,", 0, map.getPolicies().size());
	}

	/**
	 * The custom policy will have some random definitions that we can test for.
	 */
	public void testCustomPolicy() throws Exception {
		InputStream stream = getCustomPolicyMapAsStream();
		PolicyMap map;

		try {
			map = PolicyMap.load(new StreamSource(stream));
		} finally {
			stream.close();
		}

		Policy globalDefaultPolicy = map.getDefaultPolicy();

		assertPassThrough(globalDefaultPolicy.getURI());
		
		PolicyGroup defaultGroup = map.getDefaultPolicyGroup();

		assertPassThrough(defaultGroup.getDefaultPolicy().getURI());
		assertCustomPolicyGroup(defaultGroup);
		
		PolicyMap.Node defaultNode = map.getDefaultNode();

		assertPassThrough(defaultNode.getDefaultPolicy().getURI());

		PolicyMap.PartType part = map.getPartTypeById(CUSTOM_PART_TYPE_ID);

		assertNotNull("there should be a part with a local id,", part);
	}
	
	public void testResolvingRelativeUris() throws Exception {
		// a new temporary working folder in which we create the various
		// files referenced in the policy map (so we can test whether
		// reading them back-in works).
		File tmpDir = FileUtils.newTmpDir();
		
		InputStream customPolicyMapStream = getCustomPolicyMapAsStream();
		// copy the custom policy map to a temporary file
		File policyMap;

		try {
			policyMap = FileUtils.newFileWithContents(tmpDir, "policy-map.custom.xml", customPolicyMapStream);
		} finally {
			customPolicyMapStream.close();
		}
		
		// load the policy map from the temporary file
		PolicyMap map = PolicyMap.load(policyMap.toURI());
		
		// write something to another temporary file
		String someText = "<testing>";
		File someFile = FileUtils.newFileWithContents(tmpDir, "anti-samy-policy.xml", someText);
		
		// try to resolve and read the temporary file we
		// just wrote to, through the policy map.
		// Note: we only pass the name of the tempfile, so the path
		// must be resolved by the URI we provided PolicyMap.load() with.
		InputStream someFileStream = map.getLocationAsStream(someFile.getName());
		String testing;

		try {
			testing = IOUtils.toString(someFileStream);
		} finally {
			someFileStream.close();
		}
		assertEquals(someText, testing);
	}
}
