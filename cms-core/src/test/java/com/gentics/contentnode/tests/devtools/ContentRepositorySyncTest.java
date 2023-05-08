package com.gentics.contentnode.tests.devtools;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createContentRepository;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.devtools.PackageSynchronizer;
import com.gentics.contentnode.devtools.Synchronizer;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject.GlobalId;
import com.gentics.contentnode.rest.resource.impl.devtools.PackageResourceImpl;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for synchronization of CR's
 */
@GCNFeature(set = { Feature.DEVTOOLS })
public class ContentRepositorySyncTest {
	/**
	 * Name of the testpackage
	 */
	public final static String PACKAGE_NAME = "testpackage";

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	@Rule
	public PackageSynchronizerContext syncContext = new PackageSynchronizerContext();

	protected PackageSynchronizer pack;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		Transaction transaction = testContext.getContext().getTransaction();
		if (transaction != null) {
			transaction.commit();
		}
	}

	@Before
	public void setup() throws NodeException {
		Synchronizer.addPackage(PACKAGE_NAME);

		pack = Synchronizer.getPackage(PACKAGE_NAME);
		assertThat(pack).as("package synchronizer").isNotNull();
	}

	@After
	public void teardown() throws NodeException {
		Synchronizer.removePackage(PACKAGE_NAME);
	}

	@Test
	public void testAddCRWithAssignedNode() throws NodeException {
		Synchronizer.disable();

		// create a node
		Node node = supply(() -> createNode());

		// assign the node to the package
		consume(n -> Synchronizer.addPackage(n, PACKAGE_NAME), node);

		// create a CR
		ContentRepository cr = supply(() -> createContentRepository("Test CR", false, false, "url"));
		GlobalId crGlobalId = execute(ContentRepository::getGlobalId, cr);

		// add the CR to the package
		new PackageResourceImpl().addContentRepository(PACKAGE_NAME, crGlobalId.toString());

		// reload node
		node = execute(Node::reload, node);

		// check whether CR is assigned to node now and still has an editor
		try (Trx trx = new Trx()) {
			assertThat(node.getContentRepository()).as("Content Repository of Node").isEqualTo(cr);
			assertThat(node.getEditor()).as("Last editor of node").isNotNull();
			trx.success();
		}
	}
}
