package com.gentics.contentnode.tests.rest.group;

import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.NODE_GROUP_ID;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createSystemUser;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.assertRequiredPermissions;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.tuple.Triple;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.rest.model.User;
import com.gentics.contentnode.rest.model.request.Permission;
import com.gentics.contentnode.rest.model.response.GroupList;
import com.gentics.contentnode.rest.model.response.GroupLoadResponse;
import com.gentics.contentnode.rest.resource.impl.GroupResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.EmbedParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PermsFilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PermsParameterBean;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.RESTAppContext;
import com.gentics.contentnode.testutils.RESTAppContext.Type;

public class GroupResourceImplTest {

	@ClassRule
	public static DBTestContext context = new DBTestContext();

	/**
	 * REST App context
	 */
	protected static RESTAppContext restContext = new RESTAppContext(Type.jetty);

	@ClassRule
	public static RuleChain chain = RuleChain.outerRule(context).around(restContext);

	private static UserGroup testGroup;
	private static SystemUser testUser;

	@BeforeClass
	public static void setup() throws NodeException {
		testGroup = supply(() -> create(UserGroup.class, group -> {
			group.setName("Test group");
			group.setMotherId(NODE_GROUP_ID);
		}));

		testUser = supply(
				() -> createSystemUser("Test", "Test", null, "tester", "tester",
						Collections.singletonList(testGroup)));
	}

	@Test
	public void givenUserListRequestWithEmbedGroup_shouldHaveGroupsEmbedded() throws NodeException {
		List<User> groupUserList = new GroupResourceImpl().users(String.valueOf(testGroup.getId()),
				null, null, null, null, new EmbedParameterBean().withEmbed("group")).getItems();

		assertThat(groupUserList.get(0).getId())
				.isEqualTo(testUser.getId());
		assertThat(groupUserList.get(0).getGroups().get(0).getId())
				.isEqualTo(testGroup.getId());
	}

	@Test
	public void testFilterOutUnassignableGroups() throws NodeException {
		SystemUser nodeSuperUser = Trx.supply(trx -> trx.getObject(SystemUser.class, 3));
		GroupList allAvailable = Trx.supply(nodeSuperUser, () -> new GroupResourceImpl().list(null, null, null, new PermsParameterBean().setPerms(true), null));
		assertThat(allAvailable)
			.as("Available groups")
			.matches(assignable -> assignable.getPerms().values().stream().anyMatch(perms -> perms.stream().noneMatch(p -> Permission.userassignment.equals(p))), "User unassignable groups provided");

		GroupList onlyAssignable = Trx.supply(nodeSuperUser, () -> new GroupResourceImpl().list(null, null, null, new PermsParameterBean().setPerms(true), new PermsFilterParameterBean().setPermitted(List.of(Permission.userassignment))));
		assertThat(onlyAssignable)
			.as("Assignable groups")
			.matches(assignable -> assignable.getItems().size() < allAvailable.getItems().size(), "User assignable groups are filtered out")
			.matches(assignable -> assignable.getPerms().values().stream().noneMatch(perms -> perms.stream().noneMatch(p -> Permission.userassignment.equals(p))), "Only user assignable groups provided");
	}

	@Test
	public void testPermissionFilterRESTCall() throws NodeException {
		GroupList onlyAssignable = assertRequiredPermissions(testGroup, "tester", "tester", restContext, 
				target -> target.path("group")
					.queryParam("permitted", Permission.userassignment.name())
					.queryParam("perms", Boolean.TRUE.toString().toLowerCase())
					.request().get(GroupList.class), 
				Triple.of(PermHandler.TYPE_ADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(UserGroup.TYPE_GROUPADMIN, 1, PermHandler.PERM_VIEW));

		assertThat(onlyAssignable)
			.as("Assignable groups")
			.matches(assignable -> assignable.getPerms().values().stream().noneMatch(perms -> perms.stream().noneMatch(p -> Permission.userassignment.equals(p))), "Only user assignable groups provided");
	}

	@Test
	public void testGroupPerms() throws NodeException {
		GroupLoadResponse response = assertRequiredPermissions(testGroup, "tester", "tester", restContext, 
				target -> target.path("group").path(testGroup.getId().toString())
					.queryParam("perms", Boolean.TRUE.toString().toLowerCase())
					.request().get(GroupLoadResponse.class), 
				Triple.of(PermHandler.TYPE_ADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(UserGroup.TYPE_GROUPADMIN, 1, PermHandler.PERM_VIEW));

		assertThat(response.getPerms()).as("Group response with permissions").isNotNull().isNotEmpty();
	}
}

