package com.gentics.contentnode.tests.perm;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.PermHandler.ObjectPermission;
import com.gentics.contentnode.testutils.Creator;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for checking permissions on user (groups)
 */
@RunWith(value = Parameterized.class)
public class UserPermissionCheckTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();
	private static UserGroup superGroupWithPerm;
	private static UserGroup subGroupWithPerm;

	private static UserGroup superGroupWithoutPerm;
	private static UserGroup subGroupWithoutPerm;

	private static UserGroup otherGroup;
	private static SystemUser checkUser;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		UserGroup nodeGroup = Trx.supply(() -> TransactionManager.getCurrentTransaction().getObject(UserGroup.class, 2));
		assertThat(nodeGroup).as("Node Group").isNotNull();

		// Create super group and sub group
		superGroupWithPerm = Trx.supply(() -> Creator.createUsergroup("SupergroupWithPerm", "", nodeGroup));
		subGroupWithPerm = Trx.supply(() -> Creator.createUsergroup("SubgroupWithPerm", "", superGroupWithPerm));

		superGroupWithoutPerm = Trx.supply(() -> Creator.createUsergroup("SupergroupWithoutPerm", "", nodeGroup));
		subGroupWithoutPerm = Trx.supply(() -> Creator.createUsergroup("SubgroupWithoutPerm", "", superGroupWithoutPerm));

		otherGroup = Trx.supply(() -> Creator.createUsergroup("OthergroupWithPerm", "", nodeGroup));

		checkUser = Trx.supply(() -> Creator.createUser("check", "check", "check", "check", "check", Arrays.asList(superGroupWithPerm, superGroupWithoutPerm)));

		// Grant permission to edit Users to both groups
		Trx.operate(() -> {
			PermHandler.setPermissions(PermHandler.TYPE_ADMIN, Arrays.asList(superGroupWithPerm, subGroupWithPerm), PermHandler.FULL_PERM);
			PermHandler.setPermissions(SystemUser.TYPE_USERADMIN, Arrays.asList(superGroupWithPerm, subGroupWithPerm), PermHandler.FULL_PERM);
			PermHandler.setPermissions(UserGroup.TYPE_GROUPADMIN, Arrays.asList(superGroupWithPerm, subGroupWithPerm), PermHandler.FULL_PERM);
		});
	}

	/**
	 * Get the test parameters
	 * @return collection of test parameter sets
	 */
	@Parameters(name = "{index}: perm \"{0}\", superWith: \"{1}\", subWith: \"{2}\", superWithout: \"{3}\", subWithout: \"{4}\", other: \"{5}\"")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (ObjectPermission perm : Arrays.asList(ObjectPermission.view, ObjectPermission.edit, ObjectPermission.delete)) {
			for (boolean superWith : Arrays.asList(true, false)) {
				for (boolean subWith : Arrays.asList(true, false)) {
					for (boolean superWithout : Arrays.asList(true, false)) {
						for (boolean subWithout : Arrays.asList(true, false)) {
							for (boolean other : Arrays.asList(true, false)) {
								if (superWith || superWithout || subWith || subWithout || other) {
									data.add(new Object[] {perm, superWith, subWith, superWithout, subWithout, other});
								}
							}
						}
					}
				}
			}
		}
		return data;
	}

	@Parameter(0)
	public ObjectPermission perm;

	@Parameter(1)
	public boolean superWith;

	@Parameter(2)
	public boolean subWith;

	@Parameter(3)
	public boolean superWithout;

	@Parameter(4)
	public boolean subWithout;

	@Parameter(5)
	public boolean other;

	/**
	 * Clean data before test run
	 * @throws NodeException
	 */
	@Before
	public void cleanData() throws NodeException {
		Trx.operate(() -> {
			DBUtils.executeUpdate("DELETE FROM user_group WHERE user_id NOT IN (?, ?, ?)", new Object[] {1, 2, checkUser.getId()});
			DBUtils.executeUpdate("DELETE FROM systemuser WHERE id NOT IN (?, ?, ?)", new Object[] {1, 2, checkUser.getId()});
		});
	}

	@Test
	public void test() throws NodeException {
		SystemUser testedUser = Trx.supply(() -> Creator.createUser("tested", "tested", "tested", "tested", "", getGroups()));
		boolean expected = false;

		switch (perm) {
		case edit:
		case delete:
			expected = subWith && !superWith && !superWithout && !subWithout && !other;
			break;
		default:
			expected = superWith || subWith;
			break;
		}

		try (Trx trx = new Trx(null, checkUser.getId())) {
			assertThat(perm.checkObject(testedUser)).as("Permission").isEqualTo(expected);
		}
	}

	/**
	 * Get tested groups
	 * @return list of groups
	 */
	protected List<UserGroup> getGroups() {
		List<UserGroup> groups = new ArrayList<>();
		if (superWith) {
			groups.add(superGroupWithPerm);
		}
		if (subWith) {
			groups.add(subGroupWithPerm);
		}
		if (superWithout) {
			groups.add(superGroupWithoutPerm);
		}
		if (subWithout) {
			groups.add(subGroupWithoutPerm);
		}
		if (other) {
			groups.add(otherGroup);
		}
		return groups;
	}
}
