package com.gentics.contentnode.tests.edit;

import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.parttype.HTMLPartType;
import com.gentics.contentnode.rest.model.request.TemplateSaveRequest;
import com.gentics.contentnode.rest.resource.impl.TemplateResourceImpl;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.lib.db.SQLExecutor;

/**
 * Test cases for editing templates
 */
public class TemplateEditSandboxTest {

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	/**
	 * Folder id for creation of new templates
	 */
	public final static int FOLDER_ID = 7;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();
	}

	/**
	 * Tests leading/trailing spaces removal.
	 * @throws NodeException
	 */
	@Test
	public void testLeadingTrailingSpace() throws NodeException {
		final int mlId = 1;
		final String name = "  leadingtrailing  ";
		final String source = " Leading and trailing spaces ";

		Template newTemplate = create(Template.class, tmpl -> {
			tmpl.setFolderId(FOLDER_ID);
			tmpl.setMlId(mlId);
			tmpl.setName(name);
			tmpl.setSource(source);
		});
	
		assertEquals(newTemplate.getName(), "leadingtrailing");
		assertEquals(newTemplate.getSource(), " Leading and trailing spaces ");
	}

	/**
	 * Test creation of a new template
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCreateNewTemplate() throws Exception {
		final int mlId = 1;
		final String name = "New Template";
		final String source = "Template source";

		Template newTemplate = create(Template.class, tmpl -> {
			tmpl.setFolderId(FOLDER_ID);
			tmpl.setMlId(mlId);
			tmpl.setName(name);
			tmpl.setSource(source);
		});

		final int templateId = ObjectTransformer.getInt(newTemplate.getId(), 0);

		assertTrue("Template must have an ID now", templateId != 0);

		// check stored data
		operate(() -> {
			DBUtils.executeStatement("SELECT * FROM template WHERE id = ?", new SQLExecutor() {
				@Override
				public void prepareStatement(PreparedStatement stmt) throws SQLException {
					stmt.setInt(1, templateId);
				}

				@Override
				public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
					if (rs.next()) {
						assertEquals("Check stored ml_id", mlId, rs.getInt("ml_id"));
						assertEquals("Check stored name", name, rs.getString("name"));
						assertEquals("Check stored source", source, rs.getString("ml"));
						assertTrue("Template must have a channelset_id", rs.getInt("channelset_id") != 0);
					} else {
						fail("Did not find template in the DB");
					}
				}
			});
		});
	}

	/**
	 * Test whether the template can be linked to additional folders
	 * 
	 * @throws Exception
	 */
	@Test
	public void testTemplateFolderLinking() throws Exception {
		final int mlId = 1;
		final String name = "New Template";
		final String source = "Template source";
		final int LINKED_FOLDER_ID = 20;

		Folder folder = supply(t -> t.getObject(Folder.class, LINKED_FOLDER_ID));

		create(Template.class, tmpl -> {
			tmpl.setFolderId(FOLDER_ID);
			tmpl.setMlId(mlId);
			tmpl.setName(name);
			tmpl.setSource(source);
			tmpl.addFolder(folder);
		});

		Folder reloadedFolder = execute(Folder::reload, folder);

		operate(() -> {
			boolean found = false;
			for (Template template : reloadedFolder.getTemplates()) {
				if (name.equalsIgnoreCase(template.getName())) {
					found = true;
					break;
				}
			}
			assertTrue("The template with name {" + name + "} should be linked to the folder {" + LINKED_FOLDER_ID + "}", found);
		});
	}

	/**
	 * Tests loading of pages of a template
	 * 
	 * @throws Exception
	 */
	@Test
	public void testGetPagesForTemplate() throws Exception {
		final int TEMPLATE_ID = 70;
		Template template = supply(t -> t.getObject(Template.class, TEMPLATE_ID));

		assertNotNull("There should be an template with id {" + TEMPLATE_ID + "}", template);
		List<Page> pages = execute(Template::getPages, template);

		assertTrue("There should be at least one page that uses this template.", pages.size() > 0);
	}

	/**
	 * Test changing tags via REST call
	 * @throws Exception
	 */
	@Test
	public void testChangeTags() throws Exception {
		SystemUser user = supply(t -> t.getObject(SystemUser.class, 1));
		Node node = supply(user, () -> createNode("testnode", "Test Node", PublishTarget.NONE));
		Integer construct1Id = supply(user, () -> createConstruct(node, HTMLPartType.class, "construct1", "html"));
		Integer construct2Id = supply(user, () -> createConstruct(node, HTMLPartType.class, "construct2", "html"));

		Template template = supply(user, () -> create(Template.class, tmpl -> {
			tmpl.setFolderId(FOLDER_ID);
			tmpl.setName("Test Template");
			tmpl.setMlId(1);
			tmpl.setSource("<node content><node content2>");

			for (String name : Arrays.asList("content", "content2")) {
				TemplateTag templateTag = create(TemplateTag.class, tTag -> {
					tTag.setConstructId(construct1Id);
					tTag.setEnabled(true);
					tTag.setMandatory(false);
					tTag.setName(name);
					tTag.setPublic(false);
				}, false);
				tmpl.getTemplateTags().put(templateTag.getName(), templateTag);
			}
		}));

		com.gentics.contentnode.rest.model.Template restTemplate = new com.gentics.contentnode.rest.model.Template();
		Map<String, com.gentics.contentnode.rest.model.TemplateTag> templateTags = new HashMap<String, com.gentics.contentnode.rest.model.TemplateTag>();
		com.gentics.contentnode.rest.model.TemplateTag tag = new com.gentics.contentnode.rest.model.TemplateTag();
		tag.setName("content");
		tag.setEditableInPage(true);
		tag.setMandatory(true);
		tag.setConstructId(construct2Id);
		templateTags.put("content", tag);
		restTemplate.setTemplateTags(templateTags);
		TemplateSaveRequest req = new TemplateSaveRequest(restTemplate);

		operate(user, () -> new TemplateResourceImpl().update(ObjectTransformer.getString(template.getId(), null), req));

		operate(t -> {
			Template reloaded = template.reload();
			TemplateTag templateTag = reloaded.getTemplateTag("content");
			assertTrue("Template Tag 'content' should be editable in page now", templateTag.isPublic());
			assertTrue("Template Tag 'content' should be mandatory now", templateTag.getMandatory());
			assertEquals("Check construct ID of Template Tag 'content'", construct2Id, templateTag.getConstruct().getId());

			templateTag = reloaded.getTemplateTag("content2");
			assertFalse("Template Tag 'content2' should not be editable in page", templateTag.isPublic());
			assertFalse("Template Tag 'content2' should not be mandatory", templateTag.getMandatory());
			assertEquals("Check construct ID of Template Tag 'content2'", construct1Id, templateTag.getConstruct().getId());
		});
	}
}
