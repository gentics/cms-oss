package com.gentics.contentnode.tests.rest.group;

import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.cleanGroups;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.cleanUsers;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getSystemUsers;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.perm.PermissionStore;
import com.gentics.contentnode.tests.utils.ExceptionChecker;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Abstract class for group edit tests
 */
public abstract class AbstractGroupEditTest {
	@ClassRule
	public static DBTestContext context = new DBTestContext();

	@Rule
	public ExceptionChecker exceptionRule = new ExceptionChecker();

	public final static int SYSTEM_GROUP = 1;

	public final static int NODE_GROUP = 2;

	private static Set<Integer> staticUserIds;

	@BeforeClass
	public static void getStaticUsers() throws NodeException {
		context.getContext().getTransaction().commit();
		staticUserIds = getSystemUsers();
	}

	/**
	 * Clean test data
	 * @throws NodeException
	 */
	@Before
	public void cleanData() throws NodeException {
		cleanGroups();
		cleanUsers(staticUserIds);

		Trx.operate(Transaction::clearNodeObjectCache);
	}

	/**
	 * Check consistency after the test
	 * @throws NodeException
	 */
	@After
	public void checkConsistency() throws NodeException {
		// user_group must not reference inexistent groups
		Set<Integer> ids = Trx.supply(() -> DBUtils.select(
				"SELECT user_group.id FROM user_group LEFT JOIN usergroup ON user_group.usergroup_id = usergroup.id WHERE usergroup.id IS NULL", DBUtils.IDS));
		assertThat(ids).as("user_group entries referencing inexistent groups").isEmpty();

		// user_group must not reference inexistent users
		ids = Trx.supply(() -> DBUtils.select(
				"SELECT user_group.id FROM user_group LEFT JOIN systemuser ON user_group.user_id = systemuser.id WHERE systemuser.id IS NULL", DBUtils.IDS));
		assertThat(ids).as("user_group entries referencing inexistent users").isEmpty();

		// user_group must not reference deactivated users
		ids = Trx.supply(() -> DBUtils.select(
				"SELECT user_group.id FROM user_group LEFT JOIN systemuser ON user_group.user_id = systemuser.id WHERE systemuser.active = 0", DBUtils.IDS));
		assertThat(ids).as("user_group entries referencing deactivated users").isEmpty();

		// deactivated users must not have a login set
		ids = Trx.supply(() -> DBUtils.select(
				"SELECT id FROM systemuser WHERE active = 0 AND login != ''", DBUtils.IDS));
		assertThat(ids).as("deactivated systemusers with non-empty login").isEmpty();

		// active users must all have at least one group
		ids = Trx.supply(() -> DBUtils.select(
				"SELECT systemuser.id FROM systemuser LEFT JOIN user_group ON systemuser.id = user_group.user_id WHERE systemuser.active = 1 AND user_group.id IS NULL", DBUtils.IDS));
		assertThat(ids).as("active systemusers without group").isEmpty();

		StringBuilder output = new StringBuilder();
		boolean permissionStoreConsistent = supply(() -> PermissionStore.getInstance().checkConsistency(true, output, false));
		assertThat(permissionStoreConsistent).as(String.format("PermisionStore consistency result: %s", output.toString())).isTrue();
	}
}
