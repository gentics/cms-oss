package com.gentics.contentnode.tests.parttype;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Datasource;
import com.gentics.contentnode.object.DatasourceEntry;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.NodeObjectVersion;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.parttype.DatasourcePartType;
import com.gentics.contentnode.object.parttype.LongHTMLPartType;
import com.gentics.contentnode.publish.PublishQueue;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.render.RendererFactory;
import com.gentics.contentnode.render.TemplateRenderer;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.lib.db.SQLExecutor;

/**
 * Test cases for the DatasourcePartType (32)
 * All test cases test, whether the following things work:
 * <ol>
 * <li>Every tag must have its own exclusive Datasource instance</li>
 * <li>When removing a tag, the Datasource instance must be removed as well</li>
 * <li>Dirting the page must also dirt the Datasource instance</li>
 * <li>The Datasource instance must be versioned with the page</li>
 * <li>Rendering a versioned page must render the versioned datasource instance</li>
 * <li>Dirting must work</li>
 * </ol>
 */
public class DatasourcePartTypeTest {
	/**
	 * Name of the templatetag
	 */
	private static final String TEMPLATETAG_NAME = "datasource";

	/**
	 * Name of the velocity templatetag
	 */
	private static final String VELOCITY_NAME = "velocity";

	/**
	 * Keyword of the construct
	 */
	private static final String CONSTRUCT_KEYWORD = "construct";

	/**
	 * Keyword of the part
	 */
	private static final String PART_KEYWORD = "part";

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	/**
	 * Node for the test data
	 */
	protected static Node node;

	/**
	 * Datasource construct
	 */
	protected static Construct datasourceConstruct;

	/**
	 * Velocity construct
	 */
	protected static Construct velocityConstruct;

	/**
	 * Object Property Definition
	 */
	protected static ObjectTagDefinition objProp;

	/**
	 * Template
	 */
	protected static Template template;

	@BeforeClass
	public static void setupOnce() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		node = ContentNodeTestDataUtils.createNode("testnode", "Test Node", PublishTarget.NONE);

		// create a velocity construct
		int velocityConstructId = ContentNodeTestDataUtils.createVelocityConstruct(node, "Velocity", "vtl");
		velocityConstruct = t.getObject(Construct.class, velocityConstructId);

		// create a datasource construct
		int datasourceConstructId = ContentNodeTestDataUtils.createConstruct(node, DatasourcePartType.class, CONSTRUCT_KEYWORD, PART_KEYWORD);
		datasourceConstruct = t.getObject(Construct.class, datasourceConstructId, true);
		DatasourcePartType datasourcePartType = ContentNodeTestDataUtils.getPartType(DatasourcePartType.class, datasourceConstruct, PART_KEYWORD);
		Datasource datasource = datasourcePartType.getDatasource();
		assertNotNull("Datasource of the construct must not be null", datasource);

		List<DatasourceEntry> entries = datasource.getEntries();
		for (String value : Arrays.asList("one", "two", "three")) {
			DatasourceEntry entry = t.createObject(DatasourceEntry.class);
			entry.setKey(value);
			entry.setValue(value);
			entries.add(entry);
		}
		datasourceConstruct.save();
		t.commit(false);
		datasourceConstruct = t.getObject(Construct.class, datasourceConstruct.getId());

		assertDatasourceEntryKeys(Arrays.asList("one", "two", "three"),
				ContentNodeTestDataUtils.getPartType(DatasourcePartType.class, datasourceConstruct, PART_KEYWORD).getDatasource().getEntries());

		// create an object property definition for the datasource part type
		objProp = createObjectProperty(Folder.TYPE_FOLDER, "Test OE", "object.testoe", datasourceConstructId);

		// create a template
		template = t.createObject(Template.class);
		template.setMlId(1);
		template.setName("Template");
		template.setSource("<node " + VELOCITY_NAME + ">");
		template.addFolder(node.getFolder());

		TemplateTag templateTag = t.createObject(TemplateTag.class);
		templateTag.setConstructId(datasourceConstruct.getId());
		templateTag.setEnabled(true);
		templateTag.setName(TEMPLATETAG_NAME);
		templateTag.setPublic(true);
		template.getTemplateTags().put(templateTag.getName(), templateTag);

		templateTag = t.createObject(TemplateTag.class);
		templateTag.setConstructId(velocityConstruct.getId());
		templateTag.setEnabled(true);
		templateTag.setName(VELOCITY_NAME);
		templateTag.setPublic(true);
		ContentNodeTestDataUtils.getPartType(LongHTMLPartType.class, templateTag, "template").getValueObject().setValueText("$!{cms.page.tags." + TEMPLATETAG_NAME + "}");
		template.getTemplateTags().put(templateTag.getName(), templateTag);

		template.save();
		t.commit(false);
		template = t.getObject(Template.class, template.getId());
	}

	/**
	 * Test creating a contenttag from a construct
	 * Expected behaviour: the tag must have its own, exclusive Datasource instance
	 */
	@Test
	public void testCreateContentTagFromConstruct() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		Template template = t.createObject(Template.class);
		template.setMlId(1);
		template.setName("Template");
		template.setSource("Dummy template");
		template.addFolder(node.getFolder());
		template.save();
		t.commit(false);

		Page page = t.createObject(Page.class);
		page.setFolderId(node.getFolder().getId());
		page.setTemplateId(template.getId());
		String contentTagName = page.getContent().addContentTag(ObjectTransformer.getInt(datasourceConstruct.getId(), 0)).getName();
		page.save();
		t.commit(false);
		page = t.getObject(Page.class, page.getId());

		assertDatasourcesAreCopies(
			ContentNodeTestDataUtils.getPartType(DatasourcePartType.class, datasourceConstruct, PART_KEYWORD).getDatasource(),
			ContentNodeTestDataUtils.getPartType(DatasourcePartType.class, page.getContentTag(contentTagName), PART_KEYWORD).getDatasource());
	}

	/**
	 * Test creating a templatetag from a construct
	 * Expected behaviour: the tag must have its own, exclusive Datasource instance
	 */
	@Test
	public void testCreateTemplateTagFromConstruct() throws Exception {
		assertDatasourcesAreCopies(
				ContentNodeTestDataUtils.getPartType(DatasourcePartType.class, datasourceConstruct, PART_KEYWORD).getDatasource(),
				ContentNodeTestDataUtils.getPartType(DatasourcePartType.class, template.getTemplateTag(TEMPLATETAG_NAME), PART_KEYWORD).getDatasource());
	}

	/**
	 * Test creating an objecttag from a construct
	 * Expected behaviour: the tag must have its own, exclusive Datasource instance
	 */
	@Test
	public void testCreateObjectTagFromConstruct() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		Folder folder = t.getObject(Folder.class, node.getFolder().getId(), true);
		String oeKeyword = objProp.getObjectTag().getName().substring("object.".length());
		ObjectTag objectTag = folder.getObjectTag(oeKeyword);
		objectTag.setEnabled(true);
		folder.save();
		t.commit(false);
		folder = t.getObject(Folder.class, folder.getId());

		assertDatasourcesAreCopies(
				ContentNodeTestDataUtils.getPartType(DatasourcePartType.class, datasourceConstruct, PART_KEYWORD).getDatasource(),
				ContentNodeTestDataUtils.getPartType(DatasourcePartType.class, folder.getObjectTag(oeKeyword), PART_KEYWORD).getDatasource());
	}

	/**
	 * Test creating a contenttag as copy of a templatetag
	 * Expected behaviour: the tag must have its own, exclusive Datasource instance
	 */
	@Test
	public void testCreateContentTagFromTemplateTag() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		Page page = t.createObject(Page.class);
		page.setFolderId(node.getFolder().getId());
		page.setTemplateId(template.getId());
		page.save();
		t.commit(false);
		page = t.getObject(Page.class, page.getId());

		assertDatasourcesAreCopies(
			ContentNodeTestDataUtils.getPartType(DatasourcePartType.class, template.getTemplateTag(TEMPLATETAG_NAME), PART_KEYWORD).getDatasource(),
			ContentNodeTestDataUtils.getPartType(DatasourcePartType.class, page.getContentTag(TEMPLATETAG_NAME), PART_KEYWORD).getDatasource());
	}

	/**
	 * Test creating a copy of the page
	 * Expected behaviour: the copied tag must have its own, exclusive Datasource instance
	 */
	@Test
	public void testCopyPage() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		Page page = t.createObject(Page.class);
		page.setFolderId(node.getFolder().getId());
		page.setTemplateId(template.getId());
		page.save();
		t.commit(false);
		page = t.getObject(Page.class, page.getId());

		Page copy = (Page)page.copy();
		copy.save();
		t.commit(false);
		copy = t.getObject(Page.class, copy.getId());

		assertDatasourcesAreCopies(
				ContentNodeTestDataUtils.getPartType(DatasourcePartType.class, page.getContentTag(TEMPLATETAG_NAME), PART_KEYWORD).getDatasource(),
				ContentNodeTestDataUtils.getPartType(DatasourcePartType.class, copy.getContentTag(TEMPLATETAG_NAME), PART_KEYWORD).getDatasource());
	}

	/**
	 * Test creating a page version with modifications in the Datasource
	 * Expected behaviour: The Datasource instance must be versioned
	 */
	@Test
	public void testCreatePageVersion() throws Exception {
		int createTS = 1;

		Transaction t = testContext.startTransaction(createTS);

		Page page = t.createObject(Page.class);
		page.setFolderId(node.getFolder().getId());
		page.setTemplateId(template.getId());
		page.save();
		t.commit(false);

		final int datasourceId = ContentNodeTestDataUtils.getPartType(DatasourcePartType.class, page.getContentTag(TEMPLATETAG_NAME), PART_KEYWORD).getDatasourceId();
		final List<Integer> timestamps = new ArrayList<Integer>();

		DBUtils.executeStatement("SELECT nodeversiontimestamp FROM datasource_nodeversion WHERE id = ? ORDER BY nodeversiontimestamp ASC", new SQLExecutor() {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				stmt.setInt(1, datasourceId); // id = ?
			}

			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				while (rs.next()) {
					timestamps.add(rs.getInt("nodeversiontimestamp"));
				}
			}
		});

		assertEquals("Incorrect number of versions found", 1, timestamps.size());
		assertEquals("Version has incorrect timestamp", createTS, timestamps.get(0).intValue());

		for (VersionedContent test : Arrays.asList(new VersionedContent(createTS, "onetwothree"))) {
			test.assertContent(page);
		}
	}

	/**
	 * Test restoring a page version with modifications in the Datasource
	 * Expected behaviour: The Datasource instance must be restored
	 */
	@Test
	public void testRestorePageVersionAfterUpdate() throws Exception {
		int createTS = 1;
		int updateTS = createTS + 1;
		int checkTS = updateTS + 1;
		int restoreTS = checkTS + 1;
		int checkAfterRestoreTS = restoreTS + 1;

		Transaction t = testContext.startTransaction(createTS);

		Page page = t.createObject(Page.class);
		page.setFolderId(node.getFolder().getId());
		page.setTemplateId(template.getId());
		page.save();
		t.commit(false);

		t = testContext.startTransaction(updateTS);
		page = t.getObject(Page.class, page.getId(), true);
		Datasource datasource = ContentNodeTestDataUtils.getPartType(DatasourcePartType.class, page.getContentTag(TEMPLATETAG_NAME), PART_KEYWORD).getDatasource();
		assertNotNull("Datasource must not be null", datasource);
		List<DatasourceEntry> entries = datasource.getEntries();
		entries.remove(1);
		Collections.reverse(entries);

		DatasourceEntry newEntry = t.createObject(DatasourceEntry.class);
		newEntry.setDsid(99);
		newEntry.setKey("four");
		newEntry.setValue("four");
		entries.add(1, newEntry);
		page.save();
		t.commit(false);

		t = testContext.startTransaction(checkTS);
		page = t.getObject(Page.class, page.getId());
		assertDatasourceEntryKeys(Arrays.asList("three", "four", "one"),
				ContentNodeTestDataUtils.getPartType(DatasourcePartType.class, page.getContentTag(TEMPLATETAG_NAME), PART_KEYWORD).getDatasource().getEntries());

		t = testContext.startTransaction(restoreTS);
		page = t.getObject(Page.class, page.getId(), true);
		NodeObjectVersion[] pageVersions = page.getVersions();
		page.restoreVersion(pageVersions[1], false);
		t.commit(false);

		t = testContext.startTransaction(checkAfterRestoreTS);
		page = t.getObject(Page.class, page.getId());
		assertDatasourcesAreCopies(
				ContentNodeTestDataUtils.getPartType(DatasourcePartType.class, datasourceConstruct, PART_KEYWORD).getDatasource(),
				ContentNodeTestDataUtils.getPartType(DatasourcePartType.class, page.getContentTag(TEMPLATETAG_NAME), PART_KEYWORD).getDatasource());
		assertDatasourceEntryKeys(Arrays.asList("one", "two", "three"),
				ContentNodeTestDataUtils.getPartType(DatasourcePartType.class, page.getContentTag(TEMPLATETAG_NAME), PART_KEYWORD).getDatasource().getEntries());

		List<VersionedContent> tests = Arrays.asList(
				new VersionedContent(createTS, "onetwothree"),
				new VersionedContent(updateTS, "threefourone"),
				new VersionedContent(checkTS, "threefourone"),
				new VersionedContent(restoreTS, "onetwothree"),
				new VersionedContent(checkAfterRestoreTS, "onetwothree")
		);
		for (VersionedContent test : tests) {
			test.assertContent(page);
		}
	}

	/**
	 * Test restoring a page version after the tag containing the datasource instances has been removed
	 * Expected behaviour: The Datasource instance must be restored
	 */
	@Test
	public void testRestorePageVersionAfterDelete() throws Exception {
		int createTS = 1;
		int updateTS = createTS + 1;
		int checkTS = updateTS + 1;
		int restoreTS = checkTS + 1;
		int checkAfterRestoreTS = restoreTS + 1;

		Transaction t = testContext.startTransaction(createTS);

		Page page = t.createObject(Page.class);
		page.setFolderId(node.getFolder().getId());
		page.setTemplateId(template.getId());
		page.save();
		t.commit(false);

		t = testContext.startTransaction(updateTS);
		page = t.getObject(Page.class, page.getId(), true);
		Datasource datasource = ContentNodeTestDataUtils.getPartType(DatasourcePartType.class, page.getContentTag(TEMPLATETAG_NAME), PART_KEYWORD).getDatasource();
		assertNotNull("Datasource must not be null", datasource);
		Integer datasourceId = datasource.getId();

		page.getContent().getContentTags().remove(TEMPLATETAG_NAME);
		page.save();
		t.commit(false);

		t = testContext.startTransaction(checkTS);
		assertNull("Datasource must be deleted when tag is deleted", t.getObject(Datasource.class, datasourceId));

		t = testContext.startTransaction(restoreTS);
		page = t.getObject(Page.class, page.getId(), true);
		NodeObjectVersion[] pageVersions = page.getVersions();
		page.restoreVersion(pageVersions[pageVersions.length - 1], false);
		t.commit(false);

		t = testContext.startTransaction(checkAfterRestoreTS);
		page = t.getObject(Page.class, page.getId());
		assertDatasourcesAreCopies(
				ContentNodeTestDataUtils.getPartType(DatasourcePartType.class, datasourceConstruct, PART_KEYWORD).getDatasource(),
				ContentNodeTestDataUtils.getPartType(DatasourcePartType.class, page.getContentTag(TEMPLATETAG_NAME), PART_KEYWORD).getDatasource());
		assertDatasourceEntryKeys(Arrays.asList("one", "two", "three"),
				ContentNodeTestDataUtils.getPartType(DatasourcePartType.class, page.getContentTag(TEMPLATETAG_NAME), PART_KEYWORD).getDatasource().getEntries());

		List<VersionedContent> tests = Arrays.asList(
				new VersionedContent(createTS, "onetwothree"),
				new VersionedContent(updateTS, ""),
				new VersionedContent(checkTS, ""),
				new VersionedContent(restoreTS, "onetwothree"),
				new VersionedContent(checkAfterRestoreTS, "onetwothree")
		);
		for (VersionedContent test : tests) {
			test.assertContent(page);
		}
	}

	/**
	 * Test restoring a page version after a tag with a datasource instance has been created
	 * Expected behaviour: The Datasource instance must be removed
	 */
	@Test
	public void testRestorePageVersionAfterCreate() throws Exception {
		int createTS = 1;
		int updateTS = createTS + 1;
		int checkTS = updateTS + 1;
		int restoreTS = checkTS + 1;
		int checkAfterRestoreTS = restoreTS + 1;

		Transaction t = testContext.startTransaction(createTS);

		Page page = t.createObject(Page.class);
		page.setFolderId(node.getFolder().getId());
		page.setTemplateId(template.getId());
		page.save();
		t.commit(false);

		t = testContext.startTransaction(updateTS);
		page = t.getObject(Page.class, page.getId(), true);
		String contentTagName = page.getContent().addContentTag(ObjectTransformer.getInt(datasourceConstruct.getId(), 0)).getName();
		page.save();
		t.commit(false);

		t = testContext.startTransaction(checkTS);
		page = t.getObject(Page.class, page.getId());
		assertDatasourcesAreCopies(
				ContentNodeTestDataUtils.getPartType(DatasourcePartType.class, datasourceConstruct, PART_KEYWORD).getDatasource(),
				ContentNodeTestDataUtils.getPartType(DatasourcePartType.class, page.getContentTag(contentTagName), PART_KEYWORD).getDatasource());
		int datasourceId = ContentNodeTestDataUtils.getPartType(DatasourcePartType.class, page.getContentTag(contentTagName), PART_KEYWORD).getDatasourceId();

		t = testContext.startTransaction(restoreTS);
		page = t.getObject(Page.class, page.getId(), true);
		NodeObjectVersion[] pageVersions = page.getVersions();
		page.restoreVersion(pageVersions[1], false);
		t.commit(false);

		t = testContext.startTransaction(checkAfterRestoreTS);
		page = t.getObject(Page.class, page.getId());
//		assertNull("Datasource must be deleted while restoring", t.getObject(Datasource.class, datasourceId));

		List<VersionedContent> tests = Arrays.asList(
				new VersionedContent(createTS, ""),
				new VersionedContent(updateTS, "onetwothree"),
				new VersionedContent(checkTS, "onetwothree"),
				new VersionedContent(restoreTS, ""),
				new VersionedContent(checkAfterRestoreTS, "")
		);
		String template = "<node " + contentTagName + ">";
		for (VersionedContent test : tests) {
			test.assertContent(page, template);
		}
	}

	/**
	 * Test dirting
	 * @throws Exception
	 */
	@Test
	public void testDirting() throws Exception {
		int createTS = 1;
		int publishTS = createTS + 1;
		int modifyTS = publishTS + 1;
		int checkTS = modifyTS + 1;

		Transaction t = testContext.startTransaction(createTS);

		Page sourcePage = t.createObject(Page.class);
		sourcePage.setFolderId(node.getFolder().getId());
		sourcePage.setTemplateId(template.getId());
		sourcePage.save();
		sourcePage.publish();
		t.commit(false);

		Page targetPage = t.createObject(Page.class);
		targetPage.setFolderId(node.getFolder().getId());
		targetPage.setTemplateId(template.getId());
		ContentNodeTestDataUtils.getPartType(LongHTMLPartType.class, targetPage.getContentTag(VELOCITY_NAME), "template").getValueObject()
				.setValueText("#foreach($page in $cms.folder.pages)$page.tags." + TEMPLATETAG_NAME + "#end");
		targetPage.save();
		targetPage.publish();
		t.commit(false);

		testContext.publish(publishTS);

		t = testContext.startTransaction(modifyTS);
		sourcePage = t.getObject(Page.class, sourcePage.getId(), true);
		Datasource datasource = ContentNodeTestDataUtils.getPartType(DatasourcePartType.class, sourcePage.getContentTag(TEMPLATETAG_NAME), PART_KEYWORD).getDatasource();
		Collections.reverse(datasource.getEntries());
		sourcePage.save();
		sourcePage.publish();
		t.commit(false);

		testContext.waitForDirtqueueWorker();
		t = testContext.startTransaction(checkTS);
		List<Integer> dirtedPages = PublishQueue.getDirtedObjectIds(Page.class, false, node);
		assertTrue("Source page must be dirted", dirtedPages.contains(sourcePage.getId()));
		assertTrue("Target page must be dirted", dirtedPages.contains(targetPage.getId()));
	}

	/**
	 * Assert that the given datasources are different instances, but are copies of each other
	 * @param orig original datasource
	 * @param copy assumed copy of the datasource
	 * @throws Exception
	 */
	protected static void assertDatasourcesAreCopies(Datasource orig, Datasource copy) throws Exception {
		assertNotEquals("Datasources must not be the same", orig, copy);
		assertEquals("Datasource Names must be equal", orig.getName(), copy.getName());
		assertEquals("Datasource types must be equal", orig.getSourceType(), copy.getSourceType());
		List<DatasourceEntry> origEntries = orig.getEntries();
		List<DatasourceEntry> copyEntries = copy.getEntries();
		assertEquals("Datasources must have the same number of entries", origEntries.size(), copyEntries.size());

		for (int i = 0; i < origEntries.size(); i++) {
			assertDatasourceEntriesAreCopies(origEntries.get(i), copyEntries.get(i));
		}
	}

	/**
	 * Assert that the given datasource entries are different instances, but are copies of each other
	 * @param origEntry original entry
	 * @param copyEntry assumed copy of the entry
	 * @throws Exception
	 */
	protected static void assertDatasourceEntriesAreCopies(DatasourceEntry origEntry, DatasourceEntry copyEntry) throws Exception {
		assertNotEquals("Entries must not be the same", origEntry, copyEntry);
		assertEquals("Entries must have identical dsid", origEntry.getDsid(), copyEntry.getDsid());
		assertEquals("Entries must have identical key", origEntry.getKey(), copyEntry.getKey());
		assertEquals("Entries must have identical sortorder", origEntry.getSortOrder(), copyEntry.getSortOrder());
		assertEquals("Entries must have identical value", origEntry.getValue(), copyEntry.getValue());
	}

	/**
	 * Assert the keys of the list of entries
	 * @param expectedKeys expected keys
	 * @param entries actual entries
	 * @throws Exception
	 */
	protected static void assertDatasourceEntryKeys(List<String> expectedKeys, List<DatasourceEntry> entries) throws Exception {
		assertEquals("Number of entries does not match", expectedKeys.size(), entries.size());
		for (int i = 0; i < expectedKeys.size(); i++) {
			assertEquals("Key #" + i + " does not match", expectedKeys.get(i), entries.get(i).getKey());
		}
	}

	/**
	 * Create an object property definition
	 * @param targetType target type
	 * @param name human readable name
	 * @param keyword keyword (including object. prefix)
	 * @param constructId construct id
	 * @return object property definition
	 * @throws Exception
	 */
	protected static ObjectTagDefinition createObjectProperty(int targetType, String name, String keyword, int constructId) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		ObjectTagDefinition objProp = t.createObject(ObjectTagDefinition.class);
		objProp.setTargetType(targetType);
		objProp.setName(name, 1);
		ObjectTag objectTag = objProp.getObjectTag();
		objectTag.setConstructId(constructId);
		objectTag.setEnabled(true);
		objectTag.setName(keyword);
		objectTag.setObjType(targetType);
		objProp.save();
		t.commit(false);

		return t.getObject(ObjectTagDefinition.class, objProp.getId());
	}

	/**
	 * Class for expected rendered content for a page version
	 */
	protected static class VersionedContent {
		/**
		 * Timestamp of the page version
		 */
		protected int timestamp;

		/**
		 * Expected page content
		 */
		protected String content;

		/**
		 * Create an instance
		 * @param timestamp timestamp of the page version
		 * @param content expected rendered content
		 */
		public VersionedContent(int timestamp, String content) {
			this.timestamp = timestamp;
			this.content = content;
		}

		/**
		 * Render the version {@link #timestamp} of the given page and assert equality with the expected page content
		 * @param page page to render
		 * @throws Exception
		 */
		public void assertContent(Page page) throws Exception {
			Transaction t = TransactionManager.getCurrentTransaction();
			Page versionedPage = t.getObject(Page.class, page.getId(), timestamp);
			String renderedContent = versionedPage.render(new RenderResult());

			assertEquals("Rendered content @" + timestamp + " does not match", content, renderedContent);
		}

		/**
		 * Render the version {@link #timestamp} of the given page, using the given template and assert equality with the expected page content
		 * @param page page to render
		 * @param template rendered template
		 * @throws Exception
		 */
		public void assertContent(Page page, String template) throws Exception {
			Transaction t = TransactionManager.getCurrentTransaction();
			RenderType renderType = t.getRenderType();
			renderType.setHandleDependencies(false);

			Page versionedPage = t.getObject(Page.class, page.getId(), timestamp);

			renderType.setLanguage(page.getLanguage());
            TemplateRenderer renderer = RendererFactory.getRenderer(renderType
                    .getDefaultRenderer());

            // push the page onto the rendertype stack
            renderType.push(versionedPage);
            try {
                String renderedContent = renderer.render(new RenderResult(), template);
                assertEquals("Rendered content @" + timestamp + " does not match", content, renderedContent);
            } finally {
                renderType.pop();
            }
		}
	}
}
