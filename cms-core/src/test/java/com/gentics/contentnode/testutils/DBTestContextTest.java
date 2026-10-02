package com.gentics.contentnode.testutils;

import static org.junit.Assert.assertNotNull;

import org.junit.ClassRule;
import org.junit.Test;

public class DBTestContextTest {

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	@Test
	public void testKram() throws Exception {
		assertNotNull("The url property should have been set", testContext.getConnectionProperties().getProperty("url"));
	}

}
