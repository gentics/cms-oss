package com.gentics.contentnode.tests.rest.group;

import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.NODE_GROUP_ID;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createSystemUser;
import static org.assertj.core.api.Assertions.assertThat;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.rest.model.User;
import com.gentics.contentnode.rest.resource.impl.GroupResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.EmbedParameterBean;
import com.gentics.contentnode.testutils.DBTestContext;
import java.util.Collections;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

public class GroupResourceImplTest {

	@ClassRule
	public static DBTestContext context = new DBTestContext();

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

}

