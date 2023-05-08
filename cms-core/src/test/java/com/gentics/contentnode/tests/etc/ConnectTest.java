package com.gentics.contentnode.tests.etc;

import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.gentics.contentnode.tests.category.PreflightTest;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Preflight Tests. This test class is execute before all other tests are started.
 * If any test fails, no other tests will be started
 */
@Category(PreflightTest.class)
public class ConnectTest {
	@Rule
	public DBTestContext testContext = new DBTestContext(false).setMaxWait(15);

	/**
	 * Empty test case (currently, we just check, whether DBTestContext can be started)
	 */
	@Test
	public void test() {
	}
}
