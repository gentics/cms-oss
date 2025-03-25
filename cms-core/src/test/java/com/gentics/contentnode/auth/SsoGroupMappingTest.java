package com.gentics.contentnode.auth;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.ServletException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.DBTestContext;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;

import static org.junit.runners.Parameterized.Parameters;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test if group mappings and restrictions are correctly applied for SSO user attributes.
 */
@RunWith(Parameterized.class)
public class SsoGroupMappingTest {

	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> data() throws IOException {
		var data = new ArrayList<Object[]>();

		data.add(new  Object[] {"/auth/auth-attr-single-realm-role"});
		data.add(new  Object[] {"/auth/auth-attr-two-simple-realm-roles"});
		data.add(new  Object[] {"/auth/auth-attr-complex-realm-role"});
		data.add(new  Object[] {"/auth/auth-attr-single-realm-role-group-id"});
		data.add(new  Object[] {"/auth/auth-attr-single-realm-role-group-id-string"});
		data.add(new  Object[] {"/auth/auth-attr-resource-role-single-map"});
		data.add(new  Object[] {"/auth/auth-attr-resource-role-list"});
		data.add(new  Object[] {"/auth/auth-attr-mixed-roles-1"});
		data.add(new  Object[] {"/auth/auth-attr-mixed-roles-2"});

		return data;
	}

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	private static TestSsoFilter filter;
	private static final Map<String, Integer> nodeIds = new HashMap<>();
	private static final Map<String, Integer> groupIds = new HashMap<>();

	@Parameter
	public String testConfigBasePath;

	private final Map<String, Object> attributes = new HashMap<>();
	private final Map<Integer, Set<Integer>> expectedGroupAssignments = new HashMap<>();


	@BeforeClass
	public static void setupOnce() throws ServletException, IOException, NodeException {
		for (var nodeName: Arrays.asList("Node1", "1337")) {
			nodeIds.put(
				nodeName,
				Trx.supply(() -> ContentNodeTestDataUtils.createNode(nodeName + "Host", nodeName, ContentNodeTestDataUtils.PublishTarget.NONE).getId()));
		}

		for (var groupName: Arrays.asList("Group1", "Group2", "4711")) {
			groupIds.put(
				groupName,
				Trx.supply(() -> ContentNodeTestDataUtils.createUserGroup(groupName, ContentNodeTestDataUtils.NODE_GROUP_ID).getId()));
		}

		var mapper = new YAMLMapper();
		var config = mapper.readValue(SsoGroupMappingTest.class.getResourceAsStream("/auth/sso-config-init-groups.yml"), Map.class);

		Trx.operate(() -> NodeConfigRuntimeConfiguration.getPreferences().setPropertyMap(TestFilterConfig.INIT_GROUPS_PARAM, config));

		filter = new TestSsoFilter();
		filter.init(new TestFilterConfig());
	}

	@Before
	public void setup() throws IOException {
		var clazz = SsoGroupMappingTest.class;
		var mapper = new ObjectMapper();

		attributes.clear();
		expectedGroupAssignments.clear();

		attributes.putAll(mapper.readValue(clazz.getResourceAsStream(testConfigBasePath + ".json"), Map.class));

		Map<String, List<String>> groupsFromFile = mapper.readValue(clazz.getResourceAsStream(testConfigBasePath + ".expected.json"), Map.class);

		for (var entry: groupsFromFile.entrySet()) {
			var value = entry.getValue();

			expectedGroupAssignments.put(
				groupIds.get(entry.getKey()),
				value == null ? null : value.stream().map(nodeIds::get).collect(Collectors.toSet()));
		}
	}

	@Test
	public void test() throws NodeException {
		var groupAssignments = filter.getUserGroups(attributes);

		assertThat(groupAssignments)
			.as("Group assignments")
			.containsOnly(expectedGroupAssignments.entrySet().toArray(new Map.Entry[0]));
	}
}
