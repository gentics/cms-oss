package com.gentics.contentnode.tests.perm;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.perm.PermHandler.setPermissions;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createUserGroup;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.perm.TypePerms;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for the PermHandler
 */
public class PermHandlerTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();
	private static UserGroup group;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		group = supply(() -> createUserGroup("Testgroup", 2));
	}

	/**
	 * Test changing permissions with a pattern
	 * @throws NodeException
	 */
	@Test
	public void testChangeWithPattern() throws NodeException {
		// set initial permissions
		operate(() -> setPermissions(TypePerms.inbox.type(), Arrays.asList(group), "11100000000000000000000000000000"));

		String perm = supply(() -> getPerm(group.getId(), TypePerms.inbox.type()));
		assertThat(perm).isEqualTo("11100000000000000000000000000000");

		operate(() -> setPermissions(TypePerms.inbox.type(), Arrays.asList(group), "0..............................."));
		perm = supply(() -> getPerm(group.getId(), TypePerms.inbox.type()));
		assertThat(perm).isEqualTo("01100000000000000000000000000000");

		operate(() -> setPermissions(TypePerms.inbox.type(), Arrays.asList(group), ".0.............................."));
		perm = supply(() -> getPerm(group.getId(), TypePerms.inbox.type()));
		assertThat(perm).isEqualTo("00100000000000000000000000000000");

		operate(() -> setPermissions(TypePerms.inbox.type(), Arrays.asList(group), "..0............................."));
		perm = supply(() -> getPerm(group.getId(), TypePerms.inbox.type()));
		assertThat(perm).isNull();

		operate(() -> setPermissions(TypePerms.inbox.type(), Arrays.asList(group), "1..............................."));
		perm = supply(() -> getPerm(group.getId(), TypePerms.inbox.type()));
		assertThat(perm).isEqualTo("10000000000000000000000000000000");

		operate(() -> setPermissions(TypePerms.inbox.type(), Arrays.asList(group), ".1.............................."));
		perm = supply(() -> getPerm(group.getId(), TypePerms.inbox.type()));
		assertThat(perm).isEqualTo("11000000000000000000000000000000");

		operate(() -> setPermissions(TypePerms.inbox.type(), Arrays.asList(group), "..1............................."));
		perm = supply(() -> getPerm(group.getId(), TypePerms.inbox.type()));
		assertThat(perm).isEqualTo("11100000000000000000000000000000");
	}

	/**
	 * Get the permission for group and type
	 * @param groupId group ID
	 * @param typeId type ID
	 * @return permissions string (may be null)
	 * @throws NodeException
	 */
	protected String getPerm(int groupId, int typeId) throws NodeException {
		return DBUtils.select("SELECT perm FROM perm WHERE usergroup_id = ? AND o_type = ?", st -> {
			st.setInt(1, group.getId());
			st.setInt(2, TypePerms.inbox.type());
		}, DBUtils.firstString("perm"));
	}
}
