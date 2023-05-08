package com.gentics.contentnode.tests.assertj;

import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.assertj.core.api.AbstractAssert;

import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import com.gentics.contentnode.etc.ContentMap;
import com.gentics.contentnode.object.ContentFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.lib.datasource.mccr.MCCRDatasource;

/**
 * Assert for ContentMap
 */
public class ContentMapAssert extends AbstractAssert<ContentMapAssert, ContentMap> {
	/**
	 * Last checked resolvable
	 */
	protected Resolvable current;

	/**
	 * Create an instance
	 * @param actual actual content contentmap
	 */
	public ContentMapAssert(ContentMap actual) {
		super(actual, ContentMapAssert.class);
	}

	/**
	 * Assert that the contentmap contains the object
	 * @param object object
	 * @return fluent API
	 * @throws NodeException
	 */
	public ContentMapAssert contains(NodeObject object) throws NodeException {
		return contains(object, null);
	}

	/**
	 * Assert that the contentmap contains the object in the node (optional)
	 * @param object object
	 * @param node optional node
	 * @return fluent API
	 * @throws NodeException
	 */
	public ContentMapAssert contains(NodeObject object, Node node) throws NodeException {
		checkExistence(object, node, true);
		return this;
	}

	/**
	 * Assert that the contentmap does not contain the object
	 * @param object object
	 * @return fluent API
	 * @throws NodeException
	 */
	public ContentMapAssert doesNotContain(NodeObject object) throws NodeException {
		return doesNotContain(object, null);
	}

	/**
	 * Assert that the contentmap does not contain the object for the node (optional)
	 * @param object object
	 * @param node optional node
	 * @return fluent API
	 * @throws NodeException
	 */
	public ContentMapAssert doesNotContain(NodeObject object, Node node) throws NodeException {
		checkExistence(object, node, false);
		return this;
	}

	/**
	 * Assert that the last checked object has the given attribute value
	 * @param attribute attribute name
	 * @param value attribute value
	 * @return fluent API
	 * @throws NodeException
	 */
	public ContentMapAssert withAttribute(String attribute, Object value) throws NodeException {
		assertThat(current).as("Current object").isNotNull();
		assertThat(current.get(attribute)).as("Value for " + attribute).isEqualTo(value);
		return this;
	}

	/**
	 * Check existence of object in the CR
	 * @param object object
	 * @param node optional node
	 * @param expected true if object is expected, false if not
	 * @throws NodeException
	 */
	protected void checkExistence(NodeObject object, Node node, boolean expected) throws NodeException {
		Resolvable contentObject = getObject(object, node);
		if (expected) {
			assertNotNull(object + " should have been published into the cr " + actual.getName(), contentObject);
		} else {
			assertNull(object + " must not have been published into the cr " + actual.getName(), contentObject);
		}
	}

	/**
	 * Get the object
	 * @param object
	 * @param node
	 * @return
	 * @throws NodeException
	 */
	protected Resolvable getObject(NodeObject object, Node node) throws NodeException {
		int ttype = object.getTType();
		if (ttype == ContentFile.TYPE_IMAGE) {
			ttype = ContentFile.TYPE_FILE;
		}
		String contentId = ttype + "." + object.getId();
		Datasource ds = null;
		if (actual.isMultichannelling()) {
			MCCRDatasource mccrDs = actual.getMCCRDatasource();
			if (node != null) {
				mccrDs.setChannel(node.getId());
			}
			ds = mccrDs;
		} else {
			ds = actual.getDatasource();
		}
		current = PortalConnectorFactory.getContentObject(contentId, ds);
		return current;
	}
}
