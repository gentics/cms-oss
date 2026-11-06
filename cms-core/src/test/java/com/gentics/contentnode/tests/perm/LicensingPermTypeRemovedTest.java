package com.gentics.contentnode.tests.perm;

import static com.gentics.contentnode.factory.Trx.supply;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.perm.TypePerms;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for the removal of the permission type for the license management (type 142)
 */
public class LicensingPermTypeRemovedTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	/**
	 * Former type of the permission for the license management
	 */
	public final static int LICENSING_TYPE = 142;

	/**
	 * Test that there is no permission type for the license management
	 */
	@Test
	public void testNoPermType() {
		assertThat(Arrays.stream(TypePerms.values()).map(TypePerms::type)).as("Permission types")
				.doesNotContain(LICENSING_TYPE);
	}

	/**
	 * Test that the changelog removed all permission entries for the license management
	 * @throws NodeException
	 */
	@Test
	public void testNoPermEntries() throws NodeException {
		int count = supply(() -> DBUtils.select("SELECT COUNT(*) c FROM perm WHERE o_type = ?",
				st -> st.setInt(1, LICENSING_TYPE), DBUtils.firstInt("c")));
		assertThat(count).as("Number of permission entries for type " + LICENSING_TYPE).isZero();
	}
}
