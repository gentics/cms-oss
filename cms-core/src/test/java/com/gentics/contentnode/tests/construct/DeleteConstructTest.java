package com.gentics.contentnode.tests.construct;

import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getTemplateResource;
import static org.junit.Assert.assertNull;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.parttype.ShortTextPartType;
import com.gentics.contentnode.rest.model.request.FileSaveRequest;
import com.gentics.contentnode.rest.model.request.FolderSaveRequest;
import com.gentics.contentnode.rest.model.request.PageSaveRequest;
import com.gentics.contentnode.rest.model.request.TemplateSaveRequest;
import com.gentics.contentnode.rest.model.response.FileLoadResponse;
import com.gentics.contentnode.rest.model.response.FolderLoadResponse;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.PageLoadResponse;
import com.gentics.contentnode.rest.model.response.TemplateLoadResponse;
import com.gentics.contentnode.rest.resource.TemplateResource;
import com.gentics.contentnode.rest.resource.impl.FileResourceImpl;
import com.gentics.contentnode.rest.resource.impl.FolderResourceImpl;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.tests.utils.ContentNodeTestUtils;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test inconsistent tags and object tags where the construct is gone
 */
public class DeleteConstructTest {

	@Rule
	public DBTestContext testContext = new DBTestContext();

	protected Node node = null;
	protected int constructId = 0;


	@Before
	public void setUp() throws Exception {
		try (Trx trx = new Trx()) {
			node = ContentNodeTestDataUtils.createNode("node", "Node", PublishTarget.BOTH);
			constructId = ContentNodeTestDataUtils.createConstruct(
					node, ShortTextPartType.class, "shorttext", "text");
		}
	}

	/**
	 * Test pages with inconsistent tags
	 */
	@Test
	public void pageWithMissingConstructTest() throws Exception {
		Page page = null;

		try (Trx trx = new Trx()) {
			Transaction t = trx.getTransaction();

			ContentNodeTestDataUtils.createObjectPropertyDefinition(
					Page.TYPE_PAGE, constructId, "pageproperty", "pageproperty");
			page = ContentNodeTestDataUtils.createTemplateAndPage(node.getFolder(), "Test");
			page = t.getObject(page, true);
			ContentTag testedTag = page.getContent().addContentTag(constructId);
			testedTag.setName("testtag");
			page.save();

			trx.success();
		}

		deleteConstruct();

		try (Trx trx = new Trx()) {
			Transaction t = trx.getTransaction();

			// Load
			PageResourceImpl pageResource = new PageResourceImpl();
			pageResource.setTransaction(t);
			PageLoadResponse response = pageResource.load(page.getId().toString(),
					false, true, true, true, true, false, false, true, false, false, 0, null);

			Map<String, com.gentics.contentnode.rest.model.Tag> tags = response.getPage().getTags();
			assertNull("Tag must not exist", tags.get("testtag"));
			assertNull("Object tag must not exist", tags.get("object.pageproperty"));
			ContentNodeTestUtils.assertResponseCodeOk(response);

			// Save
			PageSaveRequest pageSaveRequest = new PageSaveRequest();
			pageSaveRequest.setPage(response.getPage());
			GenericResponse pageSaveResponse = pageResource.save(page.getId().toString(), pageSaveRequest);
			ContentNodeTestUtils.assertResponseCodeOk(pageSaveResponse);

			page.publish();
			trx.success();
		}

		testContext.publish(false);
	}

	/**
	 * Test templates with inconsistent tags
	 */
	@Test
	public void templateWithMissingConstructTest() throws Exception {
		Template template = null;

		try (Trx trx = new Trx()) {
			ContentNodeTestDataUtils.createObjectPropertyDefinition(
					Template.TYPE_TEMPLATE, constructId, "templateproperty", "templateproperty");
			TemplateTag templateTag = ContentNodeTestDataUtils.createTemplateTag(constructId, "templatetag", false, true);
			template = ContentNodeTestDataUtils.createTemplate(node.getFolder(), "<node templatetag>", "Test",
					templateTag);
			template.save();

			trx.success();
		}

		deleteConstruct();

		// A Session is required for the TemplateSaveJob
		try (Trx trx = new Trx(ContentNodeTestDataUtils.createSession(), true)) {
			// Load
			TemplateResource templateResource = getTemplateResource();
			TemplateLoadResponse response = templateResource.load(template.getId().toString(), node.getId());

			assertNull("Tag must not exist", response.getTemplate().getTemplateTags().get("templatetag"));
			assertNull("Object tag must not exist", response.getTemplate().getObjectTags().get("object.templateproperty"));
			ContentNodeTestUtils.assertResponseCodeOk(response);

			// Save
			TemplateSaveRequest templateSaveRequest = new TemplateSaveRequest();
			templateSaveRequest.setTemplate(response.getTemplate());
			GenericResponse pageSaveResponse = templateResource.update(template.getId().toString(), templateSaveRequest);
			ContentNodeTestUtils.assertResponseCodeOk(pageSaveResponse);
		}
	}

	/**
	 * Test folders with inconsistent tags
	 */
	@Test
	public void folderWithMissingConstructTest() throws Exception {
		Folder folder = null;

		try (Trx trx = new Trx()) {
			ContentNodeTestDataUtils.createObjectPropertyDefinition(
					Folder.TYPE_FOLDER, constructId, "folderproperty", "folderproperty");
			folder = ContentNodeTestDataUtils.createFolder(node.getFolder(), "Test");

			trx.success();
		}

		deleteConstruct();

		try (Trx trx = new Trx()) {
			Transaction t = trx.getTransaction();

			// Load
			FolderResourceImpl folderResource = new FolderResourceImpl();
			folderResource.setTransaction(t);
			FolderLoadResponse response = folderResource.load(folder.getId().toString(), false, false, false, 0, null);
			ContentNodeTestUtils.assertResponseCodeOk(response);

			assertNull("Object tag must not exist", response.getFolder().getTags().get("object.folderproperty"));

			// Save
			FolderSaveRequest folderSaveRequest = new FolderSaveRequest();
			folderSaveRequest.setFolder(response.getFolder());
			GenericResponse folderSaveResponse = folderResource.save(folder.getId().toString(), folderSaveRequest);
			ContentNodeTestUtils.assertResponseCodeOk(folderSaveResponse);
		}

		testContext.publish(false);
	}

	/**
	 * Test files with inconsistent tags
	 */
	@Test
	public void fileWithMissingConstructTest() throws Exception {
		File file = null;

		try (Trx trx = new Trx()) {
			ContentNodeTestDataUtils.createObjectPropertyDefinition(
					File.TYPE_FILE, constructId, "fileproperty", "fileproperty");
			file = ContentNodeTestDataUtils.createFile(node.getFolder(), "Test", "content".getBytes());

			trx.success();
		}

		deleteConstruct();

		try (Trx trx = new Trx()) {
			Transaction t = trx.getTransaction();

			// Load
			FileResourceImpl fileResource = new FileResourceImpl();
			fileResource.setTransaction(t);
			FileLoadResponse response = fileResource.load(file.getId().toString(), false, false, 0, null);
			ContentNodeTestUtils.assertResponseCodeOk(response);

			assertNull("Object tag must not exist", response.getFile().getTags().get("object.fileproperty"));

			// Save
			FileSaveRequest fileSaveRequest = new FileSaveRequest();
			fileSaveRequest.setFile(response.getFile());
			GenericResponse fileSaveResponse = fileResource.save(file.getId(), fileSaveRequest);
			ContentNodeTestUtils.assertResponseCodeOk(fileSaveResponse);
		}

		testContext.publish(false);
	}

	/**
	 * Delete  a construct
	 * @throws NodeException
	 * @throws SQLException
	 */
	private void deleteConstruct() throws NodeException, SQLException {
		try (Trx trx = new Trx()) {
			Transaction t = trx.getTransaction();

			// Use a query, because the normal .delete() way doesn't allow deletion of still referenced constructs
			PreparedStatement preparedStatement = t.prepareUpdateStatement("DELETE FROM construct WHERE id = ?");
			preparedStatement.setInt(1, constructId);
			preparedStatement.executeUpdate();
			t.dirtObjectCache(t.getObject(Construct.class, constructId).getObjectInfo().getObjectClass(), constructId, true);

			trx.success();
		}
	}
}
