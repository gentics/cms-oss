package com.gentics.contentnode.tests.rest.group;

import static com.gentics.contentnode.factory.Trx.supply;

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
import com.gentics.contentnode.rest.model.User;
import com.gentics.contentnode.rest.model.response.AbstractListResponse;
import com.gentics.contentnode.rest.resource.GroupResource;
import com.gentics.contentnode.rest.resource.impl.GroupResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.EmbedParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PermsParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.tests.rest.AbstractListSortAndFilterTest;
import com.gentics.contentnode.tests.utils.Builder;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;

/**
 * Sorting and filtering tests for {@link GroupResource#users(String, FilterParameterBean, SortParameterBean, PagingParameterBean, PermsParameterBean)}
 */
public class GroupResourceUsersTest extends AbstractListSortAndFilterTest<User> {
	protected static UserGroup nodeGroup;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		AbstractListSortAndFilterTest.setupOnce();

		nodeGroup = supply(t -> t.getObject(UserGroup.class, ContentNodeTestDataUtils.NODE_GROUP_ID));
	}

	@Parameters(name = "{index}: sortBy {0}, ascending {2}, filter {3}")
	public static Collection<Object[]> data() {
		List<Pair<String, Function<User, String>>> attributes = Arrays.asList(
				Pair.of("id", user -> AbstractListSortAndFilterTest.addLeadingZeros(user.getId())),
				Pair.of("login", User::getLogin),
				Pair.of("firstName", User::getFirstName),
				Pair.of("lastName", User::getLastName),
				Pair.of("email", User::getEmail)
		);
		return data(attributes, attributes);
	}

	@Override
	protected User createItem() throws NodeException {
		SystemUser systemUser = Builder.create(SystemUser.class, u -> {
			u.setLogin(randomStringGenerator.generate(5, 10));
			u.setFirstname(randomStringGenerator.generate(5, 10));
			u.setLastname(randomStringGenerator.generate(5, 10));
			u.setDescription(randomStringGenerator.generate(10, 20));
			u.setEmail(randomStringGenerator.generate(5, 10));
			u.setActive(true);
			u.getUserGroups().add(nodeGroup);
		}).build();

		return Trx.execute(SystemUser.TRANSFORM2REST, systemUser);
	}

	@Override
	protected AbstractListResponse<User> getResult(SortParameterBean sort, FilterParameterBean filter, PagingParameterBean paging) throws NodeException {
		return new GroupResourceImpl().users(Integer.toString(ContentNodeTestDataUtils.NODE_GROUP_ID), filter, sort, paging, new PermsParameterBean(), new EmbedParameterBean());
	}
}
