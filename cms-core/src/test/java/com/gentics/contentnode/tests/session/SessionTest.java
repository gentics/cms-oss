package com.gentics.contentnode.tests.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Vector;

import org.junit.Test;

import com.gentics.contentnode.factory.Session;

/**
 * Test cases for sessions
 */
public class SessionTest {

	/**
	 * Number of session secrects to be created
	 */
	public final static int NUM_SECRETS = 10000;

	/**
	 * This test will create 10000 session secrets and will check whether they
	 * are all 15 characters long and all different from each other
	 * 
	 * @throws Exception
	 */
	@Test
	public void testSecretCreation() throws Exception {
		Vector<String> secrets = new Vector<String>(NUM_SECRETS);

		for (int i = 0; i < NUM_SECRETS; ++i) {
			String secret = Session.createSessionSecret();

			assertEquals("Check length of session secret", 15, secret.length());
			assertFalse("Check if secret was created before", secrets.contains(secret));
			secrets.add(secret);
		}
	}
}
