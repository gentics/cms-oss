package com.gentics.contentnode.tests.nodeobject;

import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.Builder.update;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for filtered property substitution of node settings
 */
@RunWith(Parameterized.class)
public class NodePropertySubstitutionTest {
	protected final static String PROPERTY_VALUE = "substituted value";

	@ClassRule
	public static DBTestContext context = new DBTestContext();

	private static Node node;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		context.getContext().getTransaction().commit();

		node = supply(() -> ContentNodeTestDataUtils.createNode());
	}

	@Parameters(name = "{index}: property {0}, host {1}, previewurl {2}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		data.add(new Object[] { "INVALID_PROPERTY", false, false });
		data.add(new Object[] { "NODE_DB_PASSWORD", false, false });
		data.add(new Object[] { "NODE_HOST_TEST", true, false });
		data.add(new Object[] { "NODE_PREVIEWURL_TEST", false, true });
		return data;
	}

	@Parameter(0)
	public String propertyName;

	@Parameter(1)
	public boolean validForHost;

	@Parameter(2)
	public boolean validForPreviewUrl;

	protected String valueToSet;

	@Before
	public void setup() {
		System.setProperty(propertyName, PROPERTY_VALUE);
		valueToSet = String.format("${sys:%s}", propertyName);
	}

	@Test
	public void testHost() throws NodeException {
		node = update(node, update -> {
			update.setHostnameProperty(valueToSet);
		}).build();

		assertThat(execute(Node::getEffectiveHostname, node)).as("Effective Hostname")
				.isEqualTo(validForHost ? PROPERTY_VALUE : valueToSet);
	}

	@Test
	public void testPreviewUrl() throws NodeException {
		node = update(node, update -> {
			update.setMeshPreviewUrlProperty(valueToSet);
		}).build();

		assertThat(execute(Node::getEffectiveMeshPreviewUrl, node)).as("Effective Preview URL")
				.isEqualTo(validForPreviewUrl ? PROPERTY_VALUE : valueToSet);
	}
}
