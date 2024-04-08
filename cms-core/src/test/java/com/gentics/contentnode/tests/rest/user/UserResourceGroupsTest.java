package com.gentics.contentnode.tests.rest.user;

import static com.gentics.contentnode.factory.Trx.supply;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.BeforeClass;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.rest.model.Group;
import com.gentics.contentnode.rest.model.response.AbstractListResponse;
import com.gentics.contentnode.rest.resource.UserResource;
import com.gentics.contentnode.rest.resource.impl.UserResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PermsParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.tests.rest.AbstractListSortAndFilterTest;
import com.gentics.contentnode.tests.utils.Builder;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;

/**
 * Sorting and filtering tests for {@link UserResource#groups(String, FilterParameterBean, SortParameterBean, PagingParameterBean, PermsParameterBean)}
 */
public class UserResourceGroupsTest extends AbstractListSortAndFilterTest<Group> {
	protected static SystemUser user;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		AbstractListSortAndFilterTest.setupOnce();

		UserGroup nodeGroup = supply(t -> t.getObject(UserGroup.class, ContentNodeTestDataUtils.NODE_GROUP_ID));
		List<SystemUser> members = supply(() -> nodeGroup.getMembers());
		assertThat(members).as("Members of node group").isNotEmpty();
		user = members.get(0);
	}

	@Parameters(name = "{index}: sortBy {0}, ascending {2}, filter {3}")
	public static Collection<Object[]> data() {
		List<Pair<String, Function<Group, String>>> attributes = Arrays.asList(
				Pair.of("id", group -> AbstractListSortAndFilterTest.addLeadingZeros(group.getId())),
				Pair.of("name", Group::getName)
		);
		return data(attributes, attributes);
	}

	@Override
	protected Group createItem() throws NodeException {
		UserGroup userGroup = Builder.create(UserGroup.class, g -> {
			g.setMotherId(ContentNodeTestDataUtils.NODE_GROUP_ID);
			g.setName(randomStringGenerator.generate(5, 10));
			g.setDescription(randomStringGenerator.generate(10, 20));
		}).build();

		user = Builder.update(user, u -> {
			u.getUserGroups().add(userGroup);
		}).build();

		return Trx.execute(UserGroup.TRANSFORM2REST, userGroup);
	}

	@Override
	protected AbstractListResponse<Group> getResult(SortParameterBean sort, FilterParameterBean filter, PagingParameterBean paging) throws NodeException {
		return new UserResourceImpl().groups(Integer.toString(user.getId()), filter, sort, paging, new PermsParameterBean());
	}
}
