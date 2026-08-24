package com.gentics.contentnode.tests.multichannelling;

import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getFolderResource;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.rest.model.response.TemplateListResponse;
import com.gentics.contentnode.rest.resource.parameter.EditableParameterBean;
import com.gentics.contentnode.rest.resource.parameter.InFolderParameterBean;
import com.gentics.contentnode.rest.resource.parameter.LegacyFilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.LegacyPagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.LegacySortParameterBean;
import com.gentics.contentnode.rest.resource.parameter.TemplateListParameterBean;
import com.gentics.contentnode.rest.resource.parameter.WastebinParameterBean;

/**
 * Template tests for multichannelling support.
 *
 * @author Antoniy Chonkov <antoniy@web-teh.net>
 */
public class TemplateChannelSandboxTest extends AbstractMultichannellingSandboxTest {

	/**
	 * Test inheritance and localizing of templates.
	 *
	 * @throws NodeException
	 */
	@Test
	public void testTemplateInheritanceAndLocalizing() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		// Get the master node.
		Node masterNode = t.getObject(Node.class, MASTER_ID);

		// Get the root folder from the master node.
		Folder masterNodeRootFolder = masterNode.getFolder();

		// Create test folder.
		Folder testFolder = t.createObject(Folder.class);

		testFolder.setName(C.file_channel_test.test_folder_name);
		testFolder.setMotherId(masterNodeRootFolder.getId());

		testFolder.save();
		t.commit(false);

		// Create first test template in the root folder.
		Template newTemplateFirst = t.createObject(Template.class);

		newTemplateFirst.setDescription(C.template_channel_test.first_template_description);
		newTemplateFirst.setName(C.template_channel_test.first_template_name);
		newTemplateFirst.setSource(C.template_channel_test.template_content);
		newTemplateFirst.setMlId(1);
		newTemplateFirst.setFolderId(testFolder.getId());
		newTemplateFirst.save();

		// Create a second test template in the root folder.
		Template newTemplateSecond = t.createObject(Template.class);

		newTemplateSecond.setDescription(C.template_channel_test.second_template_description);
		newTemplateSecond.setName(C.template_channel_test.second_template_name);
		newTemplateSecond.setSource(C.template_channel_test.template_content);
		newTemplateSecond.setMlId(1);
		newTemplateSecond.setFolderId(testFolder.getId());

		// Create channel set id if there is no id set yet
		Integer channelSetId = newTemplateSecond.getChannelSetId();

		newTemplateSecond.save();
		t.commit(false);

		List<Template> templates = new ArrayList<Template>();

		templates.add(newTemplateFirst);
		templates.add(newTemplateSecond);
		testFolder.setTemplates(templates);
		testFolder.save();

		// Create a local copy of the second template and set it's channel info
		// - a.k.a. make a
		// localized copy in the specified channel.
		Template localCopy = (Template) newTemplateSecond.copy();

		localCopy.setChannelInfo(channelId, channelSetId);

		localCopy.save();
		t.commit(false);

		// Execute rest request (without http) and retrieve all templates for
		// the test folder.
		String folderId = Integer.toString(ObjectTransformer.getInt(testFolder.getId(), -1));
		TemplateListResponse templateListResponse = getFolderResource().getTemplates(
				folderId,
				new InFolderParameterBean(),
				new TemplateListParameterBean().setFolderNodeId(Integer.valueOf(channelId)),
				new LegacyFilterParameterBean(),
				new LegacySortParameterBean().setSortOrder("desc"),
				new LegacyPagingParameterBean(),
				new EditableParameterBean(),
				new WastebinParameterBean());

		// assertEquals("Check the reponse code", ResponseCode.OK,
		// templateListResponse.getResponseInfo().getResponseCode());
		assertNotNull("Check if the template list is null", templateListResponse.getTemplates());
		assertTrue("Check if the template list contains elements", templateListResponse.getTemplates().size() == 2);

		// Need these to check if we tested all the test cases below.
		boolean isFirstChecked = false;
		boolean isSecondChecked = false;

		for (com.gentics.contentnode.rest.model.Template template : templateListResponse.getTemplates()) {
			assertEquals("Check if template folder id is the same", testFolder.getId(), template.getFolderId());

			// Test if the first template has the correct data and is not
			// inherited
			if (template.getId().equals(newTemplateFirst.getId())) {
				assertEquals("Check if template name is the same", C.template_channel_test.first_template_name, template.getName());
				assertEquals("Check if template description is the same", C.template_channel_test.first_template_description, template.getDescription());
				assertTrue("Check if the template is inherited", template.isInherited());
				isFirstChecked = true;
			}

			// Test if the second template has the correct data and is n
			if (template.getId().equals(localCopy.getId())) {
				assertEquals("Check if template name is the same", C.template_channel_test.second_template_name, template.getName());
				assertEquals("Check if template description is the same", C.template_channel_test.second_template_description, template.getDescription());
				assertTrue("Check if the template is inherited", !template.isInherited());
				isSecondChecked = true;
			}
		}

		assertTrue("Check if the test went through both templates", (isFirstChecked && isSecondChecked));
	}

	/**
	 * Test creating new image in a channel. Check that it's not in the master
	 * node.
	 *
	 * @throws NodeException
	 */
	@Test
	public void testTemplateAsLocalObjectInChannel() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		// Retrieve master node.
		Node node = t.getObject(Node.class, MASTER_ID);
		Folder masterNodeFolder = node.getFolder();

		// Create test folder for our the new image.
		Folder testFolder = t.createObject(Folder.class);

		testFolder.setName(C.file_channel_test.test_folder_name);
		testFolder.setMotherId(masterNodeFolder.getId());

		testFolder.save();
		t.commit(false);

		// Create test template in the channel.
		Template newTemplateFirst = t.createObject(Template.class);

		newTemplateFirst.setName(C.template_channel_test.first_template_name);
		newTemplateFirst.setDescription(C.template_channel_test.first_template_description);
		newTemplateFirst.setSource(C.template_channel_test.template_content);
		newTemplateFirst.setMlId(1);
		newTemplateFirst.setFolderId(testFolder.getId());

		// Set channel info for the new template.
		Integer channelSetId = newTemplateFirst.getChannelSetId();
		newTemplateFirst.setChannelInfo(channelId, channelSetId);

		newTemplateFirst.save();
		t.commit(false);

		List<Template> templates = new ArrayList<Template>();

		templates.add(newTemplateFirst);
		testFolder.setTemplates(templates);

		testFolder.save();
		t.commit(false);

		// Get all templates for our test folder in the channel we are working
		// in.
		String folderId = Integer.toString(ObjectTransformer.getInt(testFolder.getId(), -1));
		TemplateListResponse templateListResponse = getFolderResource().getTemplates(folderId,
				new InFolderParameterBean(),
				new TemplateListParameterBean().setFolderNodeId(Integer.valueOf(channelId)),
				new LegacyFilterParameterBean(),
				new LegacySortParameterBean().setSortOrder("desc"),
				new LegacyPagingParameterBean(),
				new EditableParameterBean(),
				new WastebinParameterBean());

		assertNotNull("Check if the template response is null", templateListResponse);
		// assertEquals("Check the reponse code", ResponseCode.OK,
		// templateListResponse.getResponseInfo().getResponseCode());
		assertNotNull("Check if the template list is null", templateListResponse.getTemplates());
		assertTrue("Check if the template list contains elements", templateListResponse.getTemplates().size() == 1);
		assertEquals("Check if template folder id is the same", testFolder.getId(), templateListResponse.getTemplates().get(0).getFolderId());
		assertEquals("Check if template name is the same", C.template_channel_test.first_template_name, templateListResponse.getTemplates().get(0).getName());
		assertEquals("Check if template description is the same", C.template_channel_test.first_template_description, templateListResponse.getTemplates()
				.get(0).getDescription());
		assertTrue("Check if the template is inherited", !templateListResponse.getTemplates().get(0).isInherited());

		// Get all files for test folder in the master node.
		templateListResponse = getFolderResource().getTemplates(folderId,
				new InFolderParameterBean(),
				new TemplateListParameterBean(),
				new LegacyFilterParameterBean(),
				new LegacySortParameterBean().setSortOrder("desc"),
				new LegacyPagingParameterBean(),
				new EditableParameterBean(),
				new WastebinParameterBean());
		assertNotNull("Check if the template response is null", templateListResponse);
		// assertEquals("Check the reponse code", ResponseCode.OK,
		// templateListResponse.getResponseInfo().getResponseCode());
		assertTrue("Check if the image list contains elements", templateListResponse.getTemplates().size() == 0);
	}
}
