package com.gentics.contentnode.tests.publish;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.etc.Feature;

/**
 * Tests concerning the deletion of objects during instant publishing,
 * also in regards to the disable_instant_delete feature
 * @author escitalopram
 *
 */
@RunWith(Parameterized.class)
public class InstantPublishingDeleteTest extends AbstractInstantPublishingTestSandboxTest {
	private boolean disableInstantDelete;

	public InstantPublishingDeleteTest(boolean disableInstantDelete) {
		this.disableInstantDelete=disableInstantDelete;
	}

	/*
	 * test if disable_instant_delete actually prevents objects from being deleted in instant publishing
	 */
	@Test
	public void testInstantDelete() throws Exception {

		// set disable_instant_delete if applicable
		testContext.getContext().getNodeConfig().getDefaultPreferences().setFeature("disable_instant_delete", disableInstantDelete);

		// create node objects
		Node n = createNode("test", "test", true, false);
		if (disableInstantDelete) {
			n = testContext.getContext().getTransaction().getObject(Node.class, n.getId(), true);
			n.activateFeature(Feature.DISABLE_INSTANT_DELETE);
			n.save();
		}

		// publish once to get the cr structure up to date
		testContext.getContext().startTransaction();
		testContext.publish(false);

		Folder folder = createFolder(n.getFolder(), "test");
		File file = createFile(n.getFolder(), "testfile", new byte[] { 1, 2, 3, 4, 5 });
		Template template = createTemplate(folder);
		Page page = createPage(folder, template, "testpage");
		page.publish();

		// must do a full commit because InstantPublisher.handleInstantPublishing()
		// will not handle it if it is already listed as published in the transaction
		testContext.getContext().startTransaction();

		// instant publishing doesn't work if these fail
		assertInContentrepository(n, file, true);
		assertInContentrepository(n, folder, true);
		assertInContentrepository(n, page, true);

		// delete objects
		file.delete();
		page.delete();
		folder.delete();

		testContext.getContext().startTransaction();

		// check existence of objects
		assertInContentrepository(n, file, disableInstantDelete);
		assertInContentrepository(n, folder, disableInstantDelete);
		assertInContentrepository(n, page, disableInstantDelete);
	}

	/**
	 * try the test with disable_instant_delete both on and off
	 */
	@Parameters
	public static Collection<Object[]> parameters() {
		return Arrays.asList(new Object[][] { { Boolean.TRUE }, { Boolean.FALSE } });
	}

	/**
	 * Setup the sandbox
	 * @throws Exception
	 */
	@BeforeClass
	public static void setUpOnce() throws Exception {
		// activate the feature instant publishing
		testContext.getContext().getNodeConfig().getDefaultPreferences().setFeature("instant_cr_publishing", true);
	}

	/**
	 * restore snapshot
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {
		restoreSnapshot();
		testContext.getContext().startTransaction();
	}
}
