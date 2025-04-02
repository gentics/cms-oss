package com.gentics.contentnode.tests.assertj;

import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.devtools.Synchronizer;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.ObjectTagDefinition;

/**
 * Assertions for Nodes
 */
public class NodeAssert extends AbstractNodeObjectAssert<NodeAssert, Node> {
	/**
	 * Create an instance
	 * @param actual actual node
	 */
	protected NodeAssert(Node actual) {
		super(actual, NodeAssert.class);
	}

	/**
	 * Assert that the node has exactly the given languages assigned (in the given order)
	 * @param languages languages
	 * @return fluent API
	 * @throws NodeException
	 */
	public NodeAssert hasLanguages(ContentLanguage...languages) throws NodeException {
		assertThat(actual.getLanguages()).as(descriptionText() + " languages").containsExactly(languages);
		return myself;
	}

	/**
	 * Assert that the node has exactly the given constructs assigned
	 * @param constructs constructs
	 * @return fluent API
	 * @throws NodeException
	 */
	public NodeAssert hasConstructs(Construct...constructs) throws NodeException {
		assertThat(actual.getConstructs()).as(descriptionText() + " constructs").containsOnly(constructs);
		return myself;
	}

	/**
	 * Assert that the node has exactly the given object properties assigned
	 * @param objectProperties object properties
	 * @return fluent API
	 * @throws NodeException
	 */
	public NodeAssert hasObjectProperties(ObjectTagDefinition...objectProperties) throws NodeException {
		assertThat(actual.getObjectTagDefinitions()).as(descriptionText() + " object properties").containsOnly(objectProperties);
		return myself;
	}

	/**
	 * Assert that the node has the given contentrepository assigned
	 * @param cr contentrepository
	 * @return fluent API
	 * @throws NodeException
	 */
	public NodeAssert hasContentRepository(ContentRepository cr) throws NodeException {
		assertThat(actual.getContentRepository()).as(descriptionText() + " contentrepository").isEqualTo(cr);
		return myself;
	}

	/**
	 * Assert that the node is not a channel
	 * @return fluent API
	 * @throws NodeException
	 */
	public NodeAssert isNoChannel() throws NodeException {
		assertThat(actual.isChannel()).as(descriptionText() + " is channel").isFalse();
		return myself;
	}

	/**
	 * Assert that the node is a channel
	 * @return fluent API
	 * @throws NodeException
	 */
	public NodeAssert isChannel() throws NodeException {
		assertThat(actual.isChannel()).as(descriptionText() + " is channel").isTrue();
		return myself;
	}

	/**
	 * Assert that the node is a channel of the given master
	 * @param master master node
	 * @return fluent API
	 * @throws NodeException
	 */
	public NodeAssert isChannelOf(Node master) throws NodeException {
		assertThat(actual.isChannelOf(master)).as(descriptionText() + " is channel of " + master).isTrue();
		return myself;
	}

	/**
	 * Assert that the node is assigned to exactly the given packages
	 * @param packages package names
	 * @return fluent API
	 * @throws NodeException
	 */
	public NodeAssert hasPackages(String...packages) throws NodeException {
		assertThat(Synchronizer.getPackages(actual)).as(descriptionText() + " packages").containsOnly(packages);
		return myself;
	}

	/**
	 * Assert that the node uses UTF8.
	 * @return fluent API
	 */
	public NodeAssert isUtf8() {
		assertThat(actual.isUtf8()).as(descriptionText() + " uses UTF8").isTrue();

		return myself;
	}

	/**
	 * Assert that the node uses the specified editorversion (0 = LiveEditor, 1 = Aloha).
	 *
	 * @param editorversion The expected editorversion
	 * @return fluent API
	 */
	public NodeAssert hasEditorversion(int editorversion) {
		assertThat(actual.getEditorversion()).as(descriptionText() + " uses editorversion").isEqualTo(editorversion);

		return myself;
	}

	/**
	 * Assert the node having the specified name.
	 * 
	 * @param name
	 * @return
	 */
	public NodeAssert hasName(String name) {
		assertThat(actual.getName()).as("Name of " + descriptionText()).isEqualTo(name);

		return myself;
	}

	/**
	 * Assert the node having the specified name.
	 * 
	 * @param hostname
	 * @return
	 */
	public NodeAssert hasHostname(String hostname) {
		assertThat(actual.getHostname()).as("Hostname of " + descriptionText()).isEqualTo(hostname);

		return myself;
	}

	/**
	 * Assert the node having the HTTPS flag set.
	 * 
	 * @return
	 */
	public NodeAssert isHttps() {
		assertThat(actual.isHttps()).as(descriptionText() + " uses HTTPS").isTrue();

		return myself;
	}

	/**
	 * Assert the node having the HTTPS flag unset.
	 * 
	 * @return
	 */
	public NodeAssert isHttp() {
		assertThat(actual.isHttps()).as(descriptionText() + " uses HTTPS").isFalse();

		return myself;
	}
}
