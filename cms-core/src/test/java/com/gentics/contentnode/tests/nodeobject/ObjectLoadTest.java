package com.gentics.contentnode.tests.nodeobject;

import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for loading objects
 */
public class ObjectLoadTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	/**
	 * Test loading inexistent objects
	 * @throws NodeException
	 */
	@Test
	public void testLoadInexistent() throws NodeException {
		for (int timestamp : Arrays.asList(-1, 1234567)) {
			List<Value> values = Trx.supply(t -> t.getObjects(Value.class, Arrays.asList(4711, 1234567), timestamp));
			assertThat(values).as("Value List").isEmpty();
		}
	}
}
