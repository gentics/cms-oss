package com.gentics.contentnode.tests.etc;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.rest.util.MiscUtils;
import com.gentics.contentnode.testutils.DBTestContext;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the {@link MiscUtils#getMatchingSystemUsers(String, List)} method.
 */
@RunWith(Parameterized.class)
public class GetMatchingSystemUserTest {

	private static final Integer[] ALL_USERS = new Integer[] { 1, 3, 24, 25, 26, 27, 28, 29, 30, 31 };
	private static final Integer[] SYSTEM = new Integer[] { 1 };
	private static final Integer[] NODE = new Integer[] { 3 };
	private static final Integer[] ADMINS = new Integer[] { 1, 3 };
	private static final Integer[] EDITOR = new Integer[] { 26 };


	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	@Parameters(name = "{index}: pattern {0}, user IDs {1}, expected result IDs {2}")
	public static Collection<Object[]> data() {
		return Arrays.asList(
			// Nothing when pattern and IDs are null or empty.
			new Object[] { null, null, null },
			new Object[] { null, new Integer[0], null },
			// Wildcard pattern
			new Object[] { "%", null, ALL_USERS },
			// Pattern only
			new Object[] { "Node", null, ADMINS},
			// IDs only
			new Object[] { null, ADMINS, ADMINS },
			new Object[] { null, EDITOR, EDITOR },
			// Pattern and matching IDs
			new Object[] { "node", ADMINS, ADMINS },
			// Empty intersection between pattern and IDs results
			new Object[] { "Node", EDITOR, new Integer[0] },
			// Stricter pattern than IDs
			new Object[] { ".Node", ADMINS, SYSTEM },
			// Stricter IDs than pattern
			new Object[] { "node", NODE, NODE }
		);
	}

	@Parameter
	public String pattern;

	@Parameter(1)
	public Integer[] ids;

	@Parameter(2)
	public Integer[] expectedResultIds;

	@Test
	public void testGetMatchingSystemUser() throws NodeException {
		List<SystemUser> users = MiscUtils.getMatchingSystemUsers(pattern, ids == null ? null : Arrays.asList(ids));

		if (expectedResultIds == null) {
			assertThat(users)
				.as("Empty result")
				.isNull();

			return;
		}

		assertThat(users.stream().map(SystemUser::getId).collect(Collectors.toSet()))
			.as("Result user IDs")
			.containsExactlyInAnyOrder(expectedResultIds);
	}
}
