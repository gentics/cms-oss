package com.gentics.contentnode.tests.nodeobject;

import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.Builder.update;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.rest.model.PortalType;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for the portal type of nodes (set directly or by a property)
 */
@GCNFeature(set = { Feature.MESH_CONTENTREPOSITORY })
public class NodePortalTypeTest {
	protected final static String PROPERTY_NAME = "NODE_PORTALTYPE_TEST";

	protected final static String PROPERTY = "${sys:" + PROPERTY_NAME + "}";

	@ClassRule
	public static DBTestContext context = new DBTestContext();

	private static Node node;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		context.getContext().getTransaction().commit();

		node = supply(() -> ContentNodeTestDataUtils.createNode());
	}

	@After
	public void cleanup() throws NodeException {
		System.clearProperty(PROPERTY_NAME);
		System.clearProperty("OTHER_PORTALTYPE");
		node = update(node, update -> {
			update.setPortalTypeProperty("");
			update.setPortalType(null);
		}).build();
	}

	/**
	 * Test that a directly set portal type is stored
	 * @throws NodeException
	 */
	@Test
	public void testValue() throws NodeException {
		node = update(node, update -> {
			update.setPortalType(PortalType.PHP);
		}).build();

		Node reloaded = reload();
		assertThat(execute(Node::getPortalType, reloaded)).as("Portal type").isEqualTo(PortalType.PHP);
		assertThat(execute(Node::getEffectivePortalType, reloaded)).as("Effective portal type").isEqualTo(PortalType.PHP);
	}

	/**
	 * Test that a node without portal type returns null
	 * @throws NodeException
	 */
	@Test
	public void testNotSet() throws NodeException {
		Node reloaded = reload();
		assertThat(execute(Node::getPortalType, reloaded)).as("Portal type").isNull();
		assertThat(execute(Node::getEffectivePortalType, reloaded)).as("Effective portal type").isNull();
	}

	/**
	 * Test that the property is resolved, ignoring the case of the value
	 * @throws NodeException
	 */
	@Test
	public void testPropertyIgnoresCase() throws NodeException {
		System.setProperty(PROPERTY_NAME, "jAvA");

		node = update(node, update -> {
			update.setPortalTypeProperty(PROPERTY);
		}).build();

		Node reloaded = reload();
		assertThat(execute(Node::getPortalTypeProperty, reloaded)).as("Portal type property").isEqualTo(PROPERTY);
		assertThat(execute(Node::getPortalType, reloaded)).as("Resolved portal type").isEqualTo(PortalType.Java);
		assertThat(execute(Node::getEffectivePortalType, reloaded)).as("Effective portal type").isEqualTo(PortalType.Java);
	}

	/**
	 * Test that a set property overwrites a directly set portal type
	 * @throws NodeException
	 */
	@Test
	public void testPropertyWinsOverValue() throws NodeException {
		System.setProperty(PROPERTY_NAME, "php");

		node = update(node, update -> {
			update.setPortalType(PortalType.Java);
			update.setPortalTypeProperty(PROPERTY);
		}).build();

		assertThat(execute(Node::getPortalType, reload())).as("Resolved portal type").isEqualTo(PortalType.PHP);
	}

	/**
	 * Test that a property, which resolves to no portal type, unsets the portal type, but still allows saving the node
	 * @throws NodeException
	 */
	@Test
	public void testPropertyWithInvalidValue() throws NodeException {
		System.setProperty(PROPERTY_NAME, "Ruby");

		node = update(node, update -> {
			update.setPortalType(PortalType.Java);
			update.setPortalTypeProperty(PROPERTY);
		}).build();

		Node reloaded = reload();
		assertThat(execute(Node::getPortalTypeProperty, reloaded)).as("Portal type property").isEqualTo(PROPERTY);
		assertThat(execute(Node::getPortalType, reloaded)).as("Resolved portal type").isNull();
		assertThat(execute(Node::getEffectivePortalType, reloaded)).as("Effective portal type").isNull();
	}

	/**
	 * Test that a property, which does not start with NODE_PORTALTYPE_, is not substituted
	 * @throws NodeException
	 */
	@Test
	public void testPropertyWithWrongPrefix() throws NodeException {
		System.setProperty("OTHER_PORTALTYPE", "Java");

		node = update(node, update -> {
			update.setPortalTypeProperty("${sys:OTHER_PORTALTYPE}");
		}).build();

		assertThat(execute(Node::getEffectivePortalType, reload())).as("Effective portal type").isNull();
	}

	/**
	 * Test that resolving the property again picks up a changed value (like it is done when the CMS starts)
	 * @throws NodeException
	 */
	@Test
	public void testResolveChangedProperty() throws NodeException {
		System.setProperty(PROPERTY_NAME, "Java");

		node = update(node, update -> {
			update.setPortalTypeProperty(PROPERTY);
		}).build();
		assertThat(execute(Node::getPortalType, reload())).as("Portal type before change").isEqualTo(PortalType.Java);

		System.setProperty(PROPERTY_NAME, "PHP");
		assertThat(execute(Node::getEffectivePortalType, reload())).as("Effective portal type after change").isEqualTo(PortalType.PHP);

		node = update(node, Node::resolvePortalTypeProperty).build();
		assertThat(execute(Node::getPortalType, reload())).as("Stored portal type after resolving").isEqualTo(PortalType.PHP);
	}

	/**
	 * Load the node again (from the database, not from the cache)
	 * @return node
	 * @throws NodeException
	 */
	protected Node reload() throws NodeException {
		return Trx.supply(t -> {
			t.dirtObjectCache(Node.class, node.getId());
			return t.getObject(Node.class, node.getId());
		});
	}
}
