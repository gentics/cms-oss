package com.gentics.contentnode.tests.publish.instant;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Vector;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.publish.PublishInfo;
import com.gentics.contentnode.rest.model.request.StartpageRequest;
import com.gentics.contentnode.tests.publish.AbstractInstantPublishingTestSandboxTest;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.lib.datasource.CNWriteableDatasource;

public class InstantPublisherStartFolderPublishFeatureTest extends AbstractInstantPublishingTestSandboxTest {

	private Node node;

	@Before
	public void setupTestData() throws Exception {
		restoreSnapshot();

		NodePreferences prefs = testContext.getContext().getNodeConfig().getDefaultPreferences();
		prefs.setFeature("instant_cr_publishing", true);
		prefs.setFeature("publish_folder_startpage", true);

		testContext.getContext().startTransaction();
		node = createNode("instant", "Instant Publishing Node", true, true);

		// publish once to get the cr structure up to date
		testContext.getContext().startTransaction();
		testContext.publish(false);
	}

	/**
	 * Destroy datasources
	 * 
	 * @throws Exception
	 */
	@After
	public void tearDown() throws Exception {
		PortalConnectorFactory.destroy();
	}

	/**
	 * Publish the trigger page and verify that the startpage is or is not published.
	 * 
	 * @param expectStartPageToBePublished
	 * @throws Exception
	 */
	protected void invokeTest(boolean expectStartPageToBePublished) throws Exception {
		ContentNodeTestDataUtils.createObjectTagDefinition("startpage", Folder.TYPE_FOLDER, PAGEURL_CONSTRUCT_ID);

		Transaction t = TransactionManager.getCurrentTransaction();

		// Create subfolder A
		Folder subfolder = t.createObject(Folder.class);
		subfolder.setName("subfolder_in_node");
		subfolder.setMotherId(node.getFolder().getId());
		subfolder.save();
		t.commit(false);

		// Create common template
		Template template = ContentNodeTestDataUtils.createTemplate(subfolder, "dummy template source", "common-template");

		// Create Page A in folder
		Page startFolderPage = t.createObject(Page.class);
		startFolderPage.setFolderId(subfolder.getId());
		startFolderPage.setTemplateId(template.getId());
		startFolderPage.save();
		t.commit(false);

		startFolderPage.publish();
		t.commit(false);

		// Make Page A the startpage of subfolder A
		ObjectTag subFolderStartPageTag = subfolder.getObjectTags().get("startpage");
		assertNotNull(subFolderStartPageTag);

		// set the startpage
		StartpageRequest request = new StartpageRequest();
		request.setPageId(ObjectTransformer.getInt(startFolderPage.getId(), 0));
		ContentNodeRESTUtils.assertResponseOK(ContentNodeRESTUtils.getFolderResource().setStartpage(ObjectTransformer.getString(subfolder.getId(), null),
				request));
		t.commit(false);

		// Run the publish process and verify that the startpage has been published in the cr
		assertTrue("Publish process failed", testContext.getContext().publish(false).getReturnCode() == PublishInfo.RETURN_CODE_SUCCESS);
		assertInContentrepository(node, startFolderPage, true);

		// Delete the page from the cr and verify that it is gone
		CNWriteableDatasource ds = (CNWriteableDatasource) datasources.get(node.getId());
		Collection<Resolvable> listOfPages = new Vector<Resolvable>();
		Resolvable crObject = PortalConnectorFactory.getContentObject(Page.TYPE_PAGE + "." + startFolderPage.getId(), ds);
		assertNotNull("The resolvable should not be null", crObject);
		listOfPages.add(crObject);
		ds.delete(listOfPages);
		assertInContentrepository(node, startFolderPage, false);

		// Create Page B (triggerPage) in the sub folder
		Page triggerPage = t.createObject(Page.class);
		triggerPage.setTemplateId(template.getId());
		triggerPage.setFolderId(subfolder.getId());
		triggerPage.save();
		t.commit(false);

		// run a publish process
		triggerPage.publish();
		t.commit(false);
		assertTrue("Publish process failed", testContext.getContext().publish(false).getReturnCode() == PublishInfo.RETURN_CODE_SUCCESS);

		// Assert the result
		assertInContentrepository(node, triggerPage, true);
		assertInContentrepository(node, startFolderPage, expectStartPageToBePublished);
	}

	@Test
	public void testInstantPublishFolderStartPage() throws Exception {
		invokeTest(true);
	}

	@Test
	public void testInstantPublishFolderStartPageWithDisabledFeature() throws Exception {
		NodePreferences prefs = testContext.getContext().getNodeConfig().getDefaultPreferences();
		prefs.setFeature("publish_folder_startpage", false);
		invokeTest(false);
	}

	@Test
	public void testInstantPublishFolderStartPageWithDisabledNodeSetting() throws Exception {
		setPublishStartPageForNode(false);
		invokeTest(false);
	}

	/**
	 * Set the publish_startpage feature for the node
	 * 
	 * @param flag
	 * @throws NodeException
	 */
	private void setPublishStartPageForNode(boolean flag) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Node editableNode = t.getObject(Node.class, node.getId(), true);
		if (flag) {
			node.activateFeature(Feature.PUBLISH_FOLDER_STARTPAGE);
		} else {
			node.deactivateFeature(Feature.PUBLISH_FOLDER_STARTPAGE);
		}		editableNode.save();
		t.commit(false);
	}
}
