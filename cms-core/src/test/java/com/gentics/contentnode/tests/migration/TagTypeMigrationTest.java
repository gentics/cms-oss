package com.gentics.contentnode.tests.migration;

import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.utils.Builder.update;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.api.rest.ModelBuilderApiHelper;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Consumer;
import com.gentics.contentnode.factory.RenderTypeTrx;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.job.AbstractBackgroundJob;
import com.gentics.contentnode.migration.MigrationHelper;
import com.gentics.contentnode.migration.jobs.TagTypeMigrationJob;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObjectVersion;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Tag;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.page.PageCopyOpResult;
import com.gentics.contentnode.object.page.PageCopyOpResultInfo;
import com.gentics.contentnode.object.parttype.LongHTMLPartType;
import com.gentics.contentnode.object.parttype.handlebars.HandlebarsPartType;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.rest.model.Reference;
import com.gentics.contentnode.rest.model.migration.MigrationPartMapping;
import com.gentics.contentnode.rest.model.migration.MigrationPostProcessor;
import com.gentics.contentnode.rest.model.migration.TagTypeMigrationMapping;
import com.gentics.contentnode.rest.model.request.migration.TagTypeMigrationRequest;
import com.gentics.contentnode.rest.util.ModelBuilder;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.lib.db.SQLExecutor;
import com.gentics.lib.log.NodeLogger;

/*
 * Tests for tag type migration
 */
public class TagTypeMigrationTest {

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	private static final NodeLogger logger = NodeLogger.getNodeLogger(TagTypeMigrationTest.class);

	/**
	 * Tag Type Migration Page type
	 */
	private static final String PAGE = "page";

	/**
	 * Tag Type Migration for templates
	 */
	private static final String TEMPLATE = "template";

	/**
	 * IDs of the templates of the pages, which are copied by {@link #copyPage(int)}
	 */
	private static final List<Integer> COPIED_PAGE_TEMPLATE_IDS = Arrays.asList(70, 73);

	/**
	 * Node for the copies of the pages, that are modified by the tests. Migrations, which handle all pages with the same template
	 * in the same node, will therefore only migrate the copies (and not the original pages)
	 */
	private static Node copyNode;

	/**
	 * IDs of the page copies created by the test
	 */
	private List<Integer> copiedPageIds = new ArrayList<>();

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		copyNode = Trx.supply(() -> ContentNodeTestDataUtils.createNode("migrationcopies", "Migration Copies", PublishTarget.NONE));
		try (Trx trx = new Trx()) {
			Transaction t = trx.getTransaction();
			Folder rootFolder = t.getObject(Folder.class, copyNode.getFolder().getId(), true);
			rootFolder.setTemplates(new ArrayList<>(t.getObjects(Template.class, COPIED_PAGE_TEMPLATE_IDS)));
			rootFolder.save();
			t.commit(false);

			// the user with permissions must be allowed to edit the copies
			PermHandler.setPermissions(Node.TYPE_NODE, copyNode.getFolder().getId(),
					t.getObject(SystemUser.class, DBTestContext.USER_WITH_PERMS).getUserGroups(), PermHandler.FULL_PERM);
			trx.success();
		}
	}

	@Before
	public void setUp() throws Exception {
		DynamicDummyTagTypeMigrationTagPostProcessor.setPostProcessorTestBehavior(DynamicDummyTagTypeMigrationTagPostProcessor.DEFAULT_BEHAVIOUR);
	}

	/**
	 * Delete the page copies created by the test
	 * @throws NodeException
	 */
	@After
	public void tearDown() throws NodeException {
		try (Trx trx = new Trx()) {
			for (int id : copiedPageIds) {
				Page copy = trx.getTransaction().getObject(Page.class, id);
				if (copy != null) {
					copy.delete(true);
				}
			}
			trx.success();
		}
	}

	/**
	 * Create a published copy (like the original page) of the given page in {@link #copyNode}. The copy will be deleted after the test.
	 * @param pageId ID of the page to copy
	 * @return ID of the copy
	 * @throws NodeException
	 */
	protected int copyPage(int pageId) throws NodeException {
		int copyId = 0;
		try (Trx trx = new Trx()) {
			Transaction t = trx.getTransaction();
			Page page = t.getObject(Page.class, pageId);
			PageCopyOpResult result = page.copyTo(null, t.getObject(Folder.class, copyNode.getFolder().getId()), true, null, null);
			for (PageCopyOpResultInfo info : result.getCopyInfos()) {
				copiedPageIds.add(info.getCreatedPageCopy().getId());
				if (info.getSourcePage().getId() == pageId) {
					copyId = info.getCreatedPageCopy().getId();
				}
			}
			trx.success();
		}

		final int createdCopyId = copyId;
		update(Trx.supply(t -> t.getObject(Page.class, createdCopyId)), p -> {}).publish().unlock().build();
		return copyId;
	}

	/**
	 * Tests basic content tag type migration when mapping from one tag type to another with parts in the destination tag type mapping to null
	 * 
	 * @throws Exception
	 */
	@Test
	public void testSingleContentTagNullMigration() throws Exception {

		// Object IDs used during the test
		final Integer PAGE_ID = 38;
		final int FROM_TAGTYPE_ID = 1;
		final int TO_TAGTYPE_ID = 5;

		try (Trx trx = new Trx()) {
			Transaction t = trx.getTransaction();

			// Create valid mapping
			TagTypeMigrationMapping mapping = new TagTypeMigrationMapping();

			mapping.setFromTagTypeId(FROM_TAGTYPE_ID);
			mapping.setToTagTypeId(TO_TAGTYPE_ID);
			MigrationPartMapping partMapping = new MigrationPartMapping();

			partMapping.setFromPartId(21);
			partMapping.setToPartId(null);
			partMapping.setPartMappingType(MigrationPartMapping.NOT_MAPPED_TYPE_FLAG);
			ArrayList<MigrationPartMapping> partMappings = new ArrayList<MigrationPartMapping>();

			partMappings.add(partMapping);
			mapping.setPartMappings(partMappings);

			// Retrieve a page to apply the mapping to
			Page page = (Page) t.getObject(Page.class, PAGE_ID, true);

			// Check that the page contains tags of type FROM_TAGTYPE_ID
			assertTrue(getContentTagCount(page, FROM_TAGTYPE_ID) > 0);

			// Get count of tags with type TO_TAGTYPE_ID
			int toTagTypeCount = getContentTagCount(page, TO_TAGTYPE_ID);

			// Apply the mapping to all content tags in the page
			Map<String, ContentTag> contentTags = page.getContentTags();

			for (Tag tag : contentTags.values()) {
				MigrationHelper.migrateTag(t, logger, tag, mapping);
			}

			page.save();

			// Check that page no longer contains tags of type FROM_TAGTYPE_ID
			assertEquals(getContentTagCount(page, FROM_TAGTYPE_ID), 0);

			// Check that count of tags of type TO_TAGTYPE_ID has increased
			assertTrue(getContentTagCount(page, TO_TAGTYPE_ID) > toTagTypeCount);
		}
	}

	/**
	 * Tests basic content tag type migration when mapping from one tag type to another with non-null mappings
	 * 
	 * @throws Exception
	 */
	@Test
	public void testSingleContentTagNonNullMigration() throws Exception {

		// Object IDs used during the test
		final Integer PAGE_ID = copyPage(45);
		final int FROM_TAGTYPE_ID = 6;
		final int TO_TAGTYPE_ID = 7;

		try (Trx trx = new Trx()) {
			Transaction t = trx.getTransaction();

			// Create valid mapping
			TagTypeMigrationMapping mapping = new TagTypeMigrationMapping();

			mapping.setFromTagTypeId(FROM_TAGTYPE_ID);
			mapping.setToTagTypeId(TO_TAGTYPE_ID);
			MigrationPartMapping partMapping = new MigrationPartMapping();

			partMapping.setFromPartId(21);
			partMapping.setToPartId(9);
			ArrayList<MigrationPartMapping> partMappings = new ArrayList<MigrationPartMapping>();

			partMappings.add(partMapping);
			mapping.setPartMappings(partMappings);

			// Retrieve a page to apply the mapping to
			Page page = (Page) t.getObject(Page.class, PAGE_ID, true);

			// Check that the page contains tags of type FROM_TAGTYPE_ID
			assertTrue(getContentTagCount(page, FROM_TAGTYPE_ID) > 0);

			// Get count of tags with type TO_TAGTYPE_ID
			int toTagTypeCount = getContentTagCount(page, TO_TAGTYPE_ID);

			// Apply the mapping to all content tags in the page
			Map<String, ContentTag> contentTags = page.getContentTags();

			for (Tag tag : contentTags.values()) {
				MigrationHelper.migrateTag(t, logger, tag, mapping);
			}

			page.save();

			// Check that page no longer contains tags of type FROM_TAGTYPE_ID
			assertEquals(getContentTagCount(page, FROM_TAGTYPE_ID), 0);

			// Check that count of tags of type TO_TAGTYPE_ID has increased
			assertTrue(getContentTagCount(page, TO_TAGTYPE_ID) > toTagTypeCount);
		}
	}

	/**
	 * Tests content tag type migration when the source and destination tag types contain different numbers of parts
	 * 
	 * @throws Exception
	 */
	@Test
	public void testMigrationToTagWithFewerParts() throws Exception {

		// Object IDs used during the test
		final Integer PAGE_ID = 62;
		final int FROM_TAGTYPE_ID = 15;
		final int TO_TAGTYPE_ID = 6;

		// Number of values contained in the tag being migrated
		int valueCountBefore = -1;
		int valueCountAfter = -1;
		Page page = null;
		Integer tagId = null;
		Map<String, ContentTag> contentTags = null;

		try (Trx trx = new Trx()) {
			Transaction t = trx.getTransaction();

			// Create valid mapping
			TagTypeMigrationMapping mapping = new TagTypeMigrationMapping();

			mapping.setFromTagTypeId(FROM_TAGTYPE_ID);
			mapping.setToTagTypeId(TO_TAGTYPE_ID);

			// Part mappings
			MigrationPartMapping partMapping1 = new MigrationPartMapping();

			partMapping1.setFromPartId(20);
			partMapping1.setToPartId(9);

			MigrationPartMapping partMapping2 = new MigrationPartMapping();

			partMapping2.setFromPartId(19);
			partMapping2.setToPartId(null);
			partMapping2.setPartMappingType(MigrationPartMapping.NOT_MAPPED_TYPE_FLAG);

			MigrationPartMapping partMapping3 = new MigrationPartMapping();

			partMapping3.setFromPartId(21);
			partMapping3.setToPartId(null);
			partMapping3.setPartMappingType(MigrationPartMapping.NOT_MAPPED_TYPE_FLAG);

			ArrayList<MigrationPartMapping> partMappings = new ArrayList<MigrationPartMapping>();

			partMappings.add(partMapping1);
			partMappings.add(partMapping2);
			partMappings.add(partMapping3);

			mapping.setPartMappings(partMappings);

			// Retrieve a page to apply the mapping to
			page = (Page) t.getObject(Page.class, PAGE_ID, true);

			tagId = -1;

			// Apply the mapping to all content tags in the page
			contentTags = page.getContentTags();

			for (Tag tag : contentTags.values()) {
				Integer constructIdOfCurrentTag = ObjectTransformer.getInteger(tag.getConstruct().getId(), null);

				// Check if the current tag is included in the mapping
				if (mapping.getFromTagTypeId().intValue() == constructIdOfCurrentTag.intValue()) {
					valueCountBefore = tag.getValues().size();
					tagId = ObjectTransformer.getInteger(tag.getId(), null);
					MigrationHelper.migrateTag(t, logger, tag, mapping);
				}
			}

			page.save();
			trx.success();
		}

		try (Trx trx = new Trx(null, DBTestContext.USER_WITH_PERMS)) {
			Transaction t = trx.getTransaction();
			page = (Page) t.getObject(Page.class, PAGE_ID);

			contentTags = page.getContentTags();
			for (Tag tag : contentTags.values()) {
				Integer idOfCurrentTag = ObjectTransformer.getInteger(tag.getId(), null);

				// Check if the current tag is included in the mapping
				if (idOfCurrentTag.intValue() == tagId.intValue()) {
					valueCountAfter = tag.getValues().size();
				}
			}

			assertEquals(2, valueCountBefore);
			assertEquals(1, valueCountAfter);
		}
	}

	/**
	 * Tests content tag type migration when the source and destination tag types contain different numbers of parts
	 * 
	 * @throws Exception
	 */
	@Test
	public void testMigrationToTagWithMoreParts() throws Exception {

		// Object IDs used during the test
		final Integer PAGE_ID = 46;
		final int FROM_TAGTYPE_ID = 6;
		final int TO_TAGTYPE_ID = 8;

		// Number of values contained in the tag being migrated
		int valueCountBefore = -1;
		int valueCountAfter = -1;
		Page page = null;
		Integer tagId = null;
		Map<String, ContentTag> contentTags = null;

		try (Trx trx = new Trx(null, DBTestContext.USER_WITH_PERMS)) {
			Transaction t = trx.getTransaction();

			// Create valid mapping
			TagTypeMigrationMapping mapping = new TagTypeMigrationMapping();

			mapping.setFromTagTypeId(FROM_TAGTYPE_ID);
			mapping.setToTagTypeId(TO_TAGTYPE_ID);

			// Part mappings
			MigrationPartMapping partMapping = new MigrationPartMapping();

			partMapping.setFromPartId(8);
			partMapping.setToPartId(12);

			ArrayList<MigrationPartMapping> partMappings = new ArrayList<MigrationPartMapping>();

			partMappings.add(partMapping);
			mapping.setPartMappings(partMappings);

			// Retrieve a page to apply the mapping to
			page = (Page) t.getObject(Page.class, PAGE_ID, true);

			tagId = -1;

			// Apply the mapping to all content tags in the page
			contentTags = page.getContentTags();

			for (Tag tag : contentTags.values()) {
				Integer constructIdOfCurrentTag = ObjectTransformer.getInteger(tag.getConstruct().getId(), null);

				// Check if the current tag is included in the mapping
				if (mapping.getFromTagTypeId().intValue() == constructIdOfCurrentTag.intValue()) {
					valueCountBefore = tag.getTagValues().size();
					tagId = ObjectTransformer.getInteger(tag.getId(), null);
					MigrationHelper.migrateTag(t, logger, tag, mapping);
				}
			}

			page.save();
			trx.success();
		}

		try (Trx trx = new Trx(null, DBTestContext.USER_WITH_PERMS)) {
			Transaction t = trx.getTransaction();
			page = (Page) t.getObject(Page.class, PAGE_ID);

			contentTags = page.getContentTags();
			for (Tag tag : contentTags.values()) {
				Integer idOfCurrentTag = ObjectTransformer.getInteger(tag.getId(), null);

				// Check if the current tag is included in the mapping
				if (idOfCurrentTag.intValue() == tagId.intValue()) {
					valueCountAfter = tag.getTagValues().size();
				}
			}

			assertEquals(1, valueCountBefore);
			assertEquals(3, valueCountAfter);
		}
	}

	/**
	 * Tests creating a valid tag type migration job and scheduling it
	 * 
	 * @throws Exception
	 */
	@Test
	public void testValidMigrationJob() throws Exception {

		// Object IDs used during the test
		final Integer PAGE_ID = copyPage(45);
		final int FROM_TAGTYPE_ID = 6;
		final int TO_TAGTYPE_ID = 7;

		try (Trx trx = new Trx(null, DBTestContext.USER_WITH_PERMS)) {
			Transaction t = trx.getTransaction();

			// Create valid mapping
			TagTypeMigrationMapping mapping = new TagTypeMigrationMapping();

			mapping.setFromTagTypeId(FROM_TAGTYPE_ID);
			mapping.setToTagTypeId(TO_TAGTYPE_ID);
			MigrationPartMapping partMapping = new MigrationPartMapping();

			partMapping.setFromPartId(21);
			partMapping.setToPartId(9);
			ArrayList<MigrationPartMapping> partMappings = new ArrayList<MigrationPartMapping>();

			partMappings.add(partMapping);
			mapping.setPartMappings(partMappings);

			// Create list of objects to apply mappings to
			ArrayList<Integer> objectList = new ArrayList<Integer>();

			objectList.add(PAGE_ID);

			// Create request object
			TagTypeMigrationRequest request = new TagTypeMigrationRequest();
			ArrayList<TagTypeMigrationMapping> ttmMappingList = new ArrayList<TagTypeMigrationMapping>();

			ttmMappingList.add(mapping);
			request.setMappings(ttmMappingList);
			request.setType(PAGE);
			request.setObjectIds(objectList);
			request.setEnabledPostProcessors(new ArrayList<MigrationPostProcessor>());

			// Create and execute job
			TagTypeMigrationJob job = new TagTypeMigrationJob();
			setJobParameter(job, t, request, objectList, false, false, false);
			assertJobSuccess(job, 10000);
		}
	}

	@Test
	public void testMigrationJobBrokenPostProcessor() throws Exception {

		DynamicDummyTagTypeMigrationTagPostProcessor.setPostProcessorTestBehavior(DynamicDummyTagTypeMigrationTagPostProcessor.THROW_EXCEPTION);
		// Object IDs used during the test
		final Integer PAGE_ID = copyPage(1);
		final int FROM_TAGTYPE_ID = 6;
		final int TO_TAGTYPE_ID = 7;

		try (Trx trx = new Trx(null, DBTestContext.USER_WITH_PERMS)) {
			Transaction t = trx.getTransaction();


			// Create valid mapping
			TagTypeMigrationMapping mapping = new TagTypeMigrationMapping();

			mapping.setFromTagTypeId(FROM_TAGTYPE_ID);
			mapping.setToTagTypeId(TO_TAGTYPE_ID);
			MigrationPartMapping partMapping = new MigrationPartMapping();

			partMapping.setFromPartId(21);
			partMapping.setToPartId(9);
			ArrayList<MigrationPartMapping> partMappings = new ArrayList<MigrationPartMapping>();

			partMappings.add(partMapping);
			mapping.setPartMappings(partMappings);

			// Create list of objects to apply mappings to
			ArrayList<Integer> objectList = new ArrayList<Integer>();

			objectList.add(PAGE_ID);

			// Create request object
			TagTypeMigrationRequest request = new TagTypeMigrationRequest();
			ArrayList<TagTypeMigrationMapping> ttmMappingList = new ArrayList<TagTypeMigrationMapping>();

			ttmMappingList.add(mapping);
			request.setMappings(ttmMappingList);
			request.setType(PAGE);
			request.setObjectIds(objectList);

			// Configure post processor
			MigrationPostProcessor configuredPostProcessor = new MigrationPostProcessor();
			configuredPostProcessor.setClassName(DynamicDummyTagTypeMigrationTagPostProcessor.class.getName());
			configuredPostProcessor.setOrderId(0);
			List<MigrationPostProcessor> processors = new ArrayList<MigrationPostProcessor>();
			processors.add(configuredPostProcessor);
			request.setEnabledPostProcessors(processors);

			// Create and execute job
			TagTypeMigrationJob job = new TagTypeMigrationJob();
			setJobParameter(job, t, request, objectList, false, false, false);
			job.execute(1000, TimeUnit.SECONDS);
			trx.success();
		}

		try (Trx trx = new Trx(null, DBTestContext.USER_WITH_PERMS); RenderTypeTrx rTrx = new RenderTypeTrx(RenderType.EM_LIVEPREVIEW)) {
			Transaction t = trx.getTransaction();
			Page updatedNodePage = t.getObject(Page.class, PAGE_ID);
			String content = ModelBuilderApiHelper.renderPage(ModelBuilder.getPage(updatedNodePage, (Collection<Reference>) null));
			assertFalse("The content should not be modified by the post processor.", content.indexOf("MODIFIED") > 0);
		}
	}

	@Test
	public void testMigrationJobBrokenRuntimeExceptionPostProcessor() throws Exception {

		DynamicDummyTagTypeMigrationTagPostProcessor
				.setPostProcessorTestBehavior(DynamicDummyTagTypeMigrationTagPostProcessor.THROW_RUNTIME_EXCEPTION);
		// Object IDs used during the test
		final Integer PAGE_ID = copyPage(1);
		final int FROM_TAGTYPE_ID = 6;
		final int TO_TAGTYPE_ID = 7;

		try (Trx trx = new Trx(null, DBTestContext.USER_WITH_PERMS)) {
			Transaction t = trx.getTransaction();


			// Create valid mapping
			TagTypeMigrationMapping mapping = new TagTypeMigrationMapping();

			mapping.setFromTagTypeId(FROM_TAGTYPE_ID);
			mapping.setToTagTypeId(TO_TAGTYPE_ID);
			MigrationPartMapping partMapping = new MigrationPartMapping();

			partMapping.setFromPartId(21);
			partMapping.setToPartId(9);
			ArrayList<MigrationPartMapping> partMappings = new ArrayList<MigrationPartMapping>();

			partMappings.add(partMapping);
			mapping.setPartMappings(partMappings);

			// Create list of objects to apply mappings to
			ArrayList<Integer> objectList = new ArrayList<Integer>();

			objectList.add(PAGE_ID);

			// Create request object
			TagTypeMigrationRequest request = new TagTypeMigrationRequest();
			ArrayList<TagTypeMigrationMapping> ttmMappingList = new ArrayList<TagTypeMigrationMapping>();

			ttmMappingList.add(mapping);
			request.setMappings(ttmMappingList);
			request.setType(PAGE);
			request.setObjectIds(objectList);

			// Configure post processor
			MigrationPostProcessor configuredPostProcessor = new MigrationPostProcessor();
			configuredPostProcessor.setClassName(DynamicDummyTagTypeMigrationTagPostProcessor.class.getName());
			configuredPostProcessor.setOrderId(0);
			List<MigrationPostProcessor> processors = new ArrayList<MigrationPostProcessor>();
			processors.add(configuredPostProcessor);
			request.setEnabledPostProcessors(processors);

			// Create and execute job
			TagTypeMigrationJob job = new TagTypeMigrationJob();
			setJobParameter(job, t, request, objectList, false, false, false);
			job.execute(1000, TimeUnit.SECONDS);
			trx.success();
		}

		try (Trx trx = new Trx(null, DBTestContext.USER_WITH_PERMS); RenderTypeTrx rTrx = new RenderTypeTrx(RenderType.EM_LIVEPREVIEW)) {
			Transaction t = trx.getTransaction();
			Page updatedNodePage = t.getObject(Page.class, PAGE_ID);
			String content = ModelBuilderApiHelper.renderPage(ModelBuilder.getPage(updatedNodePage, (Collection<Reference>) null));
			assertFalse("The content should not be modified by the post processor.", content.indexOf("MODIFIED") > 0);
		}
	}

	@Test
	public void testMigrationJobWithTagChangingPostProcessor() throws Exception {
		// Object IDs used during the test
		final Integer PAGE_ID = copyPage(1);
		final int FROM_TAGTYPE_ID = 6;
		final int TO_TAGTYPE_ID = 7;

		try (Trx trx = new Trx(null, DBTestContext.USER_WITH_PERMS)) {
			Transaction t = trx.getTransaction();


			// Create valid mapping
			TagTypeMigrationMapping mapping = new TagTypeMigrationMapping();

			mapping.setFromTagTypeId(FROM_TAGTYPE_ID);
			mapping.setToTagTypeId(TO_TAGTYPE_ID);
			MigrationPartMapping partMapping = new MigrationPartMapping();

			partMapping.setFromPartId(21);
			partMapping.setToPartId(9);
			ArrayList<MigrationPartMapping> partMappings = new ArrayList<MigrationPartMapping>();

			partMappings.add(partMapping);
			mapping.setPartMappings(partMappings);

			// Create list of objects to apply mappings to
			ArrayList<Integer> objectList = new ArrayList<Integer>();

			objectList.add(PAGE_ID);

			// Create request object
			TagTypeMigrationRequest request = new TagTypeMigrationRequest();
			ArrayList<TagTypeMigrationMapping> ttmMappingList = new ArrayList<TagTypeMigrationMapping>();

			ttmMappingList.add(mapping);
			request.setMappings(ttmMappingList);
			request.setType(PAGE);
			request.setObjectIds(objectList);

			// Configure post processor
			MigrationPostProcessor configuredPostProcessor = new MigrationPostProcessor();
			configuredPostProcessor.setClassName(DynamicDummyTagTypeMigrationTagPostProcessor.class.getName());
			configuredPostProcessor.setOrderId(0);
			List<MigrationPostProcessor> processors = new ArrayList<MigrationPostProcessor>();
			processors.add(configuredPostProcessor);
			request.setEnabledPostProcessors(processors);

			// Create and execute job
			TagTypeMigrationJob job = new TagTypeMigrationJob();
			setJobParameter(job, t, request, objectList, false, false, false);
			job.execute(1000, TimeUnit.SECONDS);
			trx.success();
		}

		try (Trx trx = new Trx(null, DBTestContext.USER_WITH_PERMS); RenderTypeTrx rTrx = new RenderTypeTrx(RenderType.EM_LIVEPREVIEW)) {
			Transaction t = trx.getTransaction();
			Page updatedNodePage = t.getObject(Page.class, PAGE_ID);
			String content = ModelBuilderApiHelper.renderPage(ModelBuilder.getPage(updatedNodePage, (Collection<Reference>) null));
			assertTrue("The content should be modified by the post processor.", content.indexOf("MODIFIED") > 0);
		}
	}

	/**
	 * Sets the ttm job parameters
	 * 
	 * @param job ttm Job instance
	 * @param t Transaction
	 * @param request TTM Request
	 * @param objectList list of object ids
	 * @param handlePagesByTemplate true to handle all pages with same template (in same node)
	 * @param handleAllNodes true to handle pages with same template in all nodes
	 * @param preventTriggerEvent true to prevent triggering events
	 */
	private void setJobParameter(TagTypeMigrationJob job, Transaction t, TagTypeMigrationRequest request, ArrayList<Integer> objectList,
			boolean handlePagesByTemplate, boolean handleAllNodes, boolean preventTriggerEvent) {
		job.setRequest(request);
		job.setType(request.getType());
		job.setObjectIds(objectList);
		job.setHandlePagesByTemplate(handlePagesByTemplate);
		job.setHandleAllNodes(handleAllNodes);
		job.setPreventTriggerEvent(preventTriggerEvent);
	}

	@Test
	public void testMigrationJobWithTagDeletingPostProcessor() throws Exception {
		// Object IDs used during the test
		final Integer PAGE_ID = 4;
		final int FROM_TAGTYPE_ID = 6;
		final int TO_TAGTYPE_ID = 7;

		try (Trx trx = new Trx(null, DBTestContext.USER_WITH_PERMS)) {
			Transaction t = trx.getTransaction();


			// Create valid mapping
			TagTypeMigrationMapping mapping = new TagTypeMigrationMapping();

			mapping.setFromTagTypeId(FROM_TAGTYPE_ID);
			mapping.setToTagTypeId(TO_TAGTYPE_ID);
			MigrationPartMapping partMapping = new MigrationPartMapping();

			partMapping.setFromPartId(21);
			partMapping.setToPartId(9);
			ArrayList<MigrationPartMapping> partMappings = new ArrayList<MigrationPartMapping>();

			partMappings.add(partMapping);
			mapping.setPartMappings(partMappings);

			// Create list of objects to apply mappings to
			ArrayList<Integer> objectList = new ArrayList<Integer>();

			objectList.add(PAGE_ID);

			// Create request object
			TagTypeMigrationRequest request = new TagTypeMigrationRequest();
			ArrayList<TagTypeMigrationMapping> ttmMappingList = new ArrayList<TagTypeMigrationMapping>();

			ttmMappingList.add(mapping);
			request.setMappings(ttmMappingList);
			request.setType(PAGE);
			request.setObjectIds(objectList);

			// Configure post processor
			MigrationPostProcessor configuredPostProcessor = new MigrationPostProcessor();
			configuredPostProcessor.setClassName(DynamicDummyTagTypeMigrationTagPostProcessor.class.getName());
			configuredPostProcessor.setOrderId(0);
			List<MigrationPostProcessor> processors = new ArrayList<MigrationPostProcessor>();
			processors.add(configuredPostProcessor);
			request.setEnabledPostProcessors(processors);

			// Create and execute job
			TagTypeMigrationJob job = new TagTypeMigrationJob();
			setJobParameter(job, t, request, objectList, false, false, false);
			job.execute(1000, TimeUnit.SECONDS);
			trx.success();
		}

		try (Trx trx = new Trx(null, DBTestContext.USER_WITH_PERMS); RenderTypeTrx rTrx = new RenderTypeTrx(RenderType.EM_LIVEPREVIEW)) {
			Transaction t = trx.getTransaction();
			Page updatedNodePage = t.getObject(Page.class, PAGE_ID);
			String content = ModelBuilderApiHelper.renderPage(ModelBuilder.getPage(updatedNodePage, (Collection<Reference>) null));
			assertTrue("The content should be modified by the post processor.", content.indexOf("MODIFIED") >= 0);
			assertFalse("The tag vtl1 should not exist since it was removed by the post processor.", updatedNodePage.getTags().containsKey("vtl1"));
		}
	}

	private void runMigrationWithSimplePostProcessor(boolean handlePagesByTemplate, boolean preventTriggerEvent) throws Exception {
		// Object IDs used during the test
		final Integer PAGE_ID = copyPage(45);
		final int FROM_TAGTYPE_ID = 6;
		final int TO_TAGTYPE_ID = 7;

		try (Trx trx = new Trx(null, DBTestContext.USER_WITH_PERMS)) {
			Transaction t = trx.getTransaction();


			// Create valid mapping
			TagTypeMigrationMapping mapping = new TagTypeMigrationMapping();

			mapping.setFromTagTypeId(FROM_TAGTYPE_ID);
			mapping.setToTagTypeId(TO_TAGTYPE_ID);
			MigrationPartMapping partMapping = new MigrationPartMapping();

			partMapping.setFromPartId(21);
			partMapping.setToPartId(9);
			ArrayList<MigrationPartMapping> partMappings = new ArrayList<MigrationPartMapping>();

			partMappings.add(partMapping);
			mapping.setPartMappings(partMappings);

			// Create list of objects to apply mappings to
			ArrayList<Integer> objectList = new ArrayList<Integer>();
			objectList.add(PAGE_ID);

			// Create request object
			TagTypeMigrationRequest request = new TagTypeMigrationRequest();
			ArrayList<TagTypeMigrationMapping> ttmMappingList = new ArrayList<TagTypeMigrationMapping>();
			ttmMappingList.add(mapping);
			request.setMappings(ttmMappingList);
			request.setType(PAGE);
			request.setObjectIds(objectList);

			// Configure post processor
			MigrationPostProcessor configuredPostProcessor = new MigrationPostProcessor();
			configuredPostProcessor.setClassName(DummyTagTypeMigrationRenamePostProcessor.class.getName());
			configuredPostProcessor.setOrderId(0);
			List<MigrationPostProcessor> processors = new ArrayList<MigrationPostProcessor>();
			processors.add(configuredPostProcessor);
			request.setEnabledPostProcessors(processors);

			// Create and execute job
			TagTypeMigrationJob job = new TagTypeMigrationJob();
			setJobParameter(job, t, request, objectList, handlePagesByTemplate, false, preventTriggerEvent);
			job.execute(1000, TimeUnit.SECONDS);
			trx.success();
		}

		try (Trx trx = new Trx(null, DBTestContext.USER_WITH_PERMS)) {
			Transaction t = trx.getTransaction();
			Page updatedNodePage = t.getObject(Page.class, PAGE_ID);
			String nameAfter = updatedNodePage.getName();
			assertThat(updatedNodePage).isNotModified().isOnline();

			assertEquals("Page name should change", "migrated page_" + PAGE_ID, nameAfter);
		}
	}

	/**
	 * Tests creating a valid tag type migration job with a post processor and scheduling it
	 * 
	 * @throws Exception
	 */
	@Test
	public void testMigrationJobWithSimplePostProcessor() throws Exception {
		runMigrationWithSimplePostProcessor(false, false);
	}

	@Test
	public void testMigrationJobWithSimplePostProcessorPreventTrigger() throws Exception {
		runMigrationWithSimplePostProcessor(true, true);
	}

	@Test
	public void testMigrationJobForToPublishPagesWithPostProcessor() throws Exception {
		testMigrationPageStatus(Page::publish, page -> {
			assertThat(page).as("Migrated page").isOnline().isNotModified();
		});
	}

	@Test
	public void testMigrationJobForOfflinePagesWithPostProcessor() throws Exception {
		testMigrationPageStatus(Page::takeOffline, page -> {
			assertThat(page).as("Migrated page").isOffline();
		});
	}

	@Test
	public void testMigrationJobForQueuedPages() throws Exception {
		testMigrationPageStatus(p -> p.queuePublish(p.getCreator()), page -> {
			assertThat(page).as("Migrated page").isOnline().hasQueuedPublish(page.getCreator());
		});
	}

	@Test
	public void testMigrationJobForQueuedAtPages() throws Exception {
		int queuedAtTimestamp = 1000;
		AtomicReference<NodeObjectVersion> queuedVersion = new AtomicReference<>();

		testMigrationPageStatus(page -> {
			page.queuePublish(page.getCreator(), queuedAtTimestamp, null);
			queuedVersion.set(page.getTimePubVersionQueue());
			assertThat(queuedVersion.get()).as("Queued Version").isNotNull();
		}, page -> {
			assertThat(page).as("Migrated page").isOnline().hasQueuedPublishAt(page.getCreator(), queuedAtTimestamp, queuedVersion.get().getNumber());
		});
	}

	public void testMigrationPageStatus(Consumer<Page> beforeMigration, Consumer<Page> afterMigration) throws Exception {
		// Object IDs used during the test
		final Integer PAGE_ID = copyPage(45);
		final int FROM_TAGTYPE_ID = 6;
		final int TO_TAGTYPE_ID = 7;

		try (Trx trx = new Trx(null, DBTestContext.USER_WITH_PERMS)) {
			Transaction t = trx.getTransaction();


			Page sourcePage = t.getObject(Page.class, PAGE_ID, true);
			if (beforeMigration != null) {
				beforeMigration.accept(sourcePage);
			}
			sourcePage.unlock();
			t.commit(false);

			// Create valid mapping
			TagTypeMigrationMapping mapping = new TagTypeMigrationMapping();

			mapping.setFromTagTypeId(FROM_TAGTYPE_ID);
			mapping.setToTagTypeId(TO_TAGTYPE_ID);
			MigrationPartMapping partMapping = new MigrationPartMapping();

			partMapping.setFromPartId(21);
			partMapping.setToPartId(9);
			ArrayList<MigrationPartMapping> partMappings = new ArrayList<MigrationPartMapping>();

			partMappings.add(partMapping);
			mapping.setPartMappings(partMappings);

			// Create list of objects to apply mappings to
			ArrayList<Integer> objectList = new ArrayList<Integer>();
			objectList.add(PAGE_ID);

			// Create request object
			TagTypeMigrationRequest request = new TagTypeMigrationRequest();
			ArrayList<TagTypeMigrationMapping> ttmMappingList = new ArrayList<TagTypeMigrationMapping>();
			ttmMappingList.add(mapping);
			request.setMappings(ttmMappingList);
			request.setType(PAGE);
			request.setObjectIds(objectList);

			// Configure post processor
			MigrationPostProcessor configuredPostProcessor = new MigrationPostProcessor();
			configuredPostProcessor.setClassName(DummyTagTypeMigrationRenamePostProcessor.class.getName());
			configuredPostProcessor.setOrderId(0);
			List<MigrationPostProcessor> processors = new ArrayList<MigrationPostProcessor>();
			processors.add(configuredPostProcessor);
			request.setEnabledPostProcessors(processors);

			// Create and execute job
			TagTypeMigrationJob job = new TagTypeMigrationJob();
			setJobParameter(job, t, request, objectList, true, false, true);
			job.execute(1000, TimeUnit.SECONDS);
			trx.success();
		}

		try (Trx trx = new Trx(null, DBTestContext.USER_WITH_PERMS)) {
			Transaction t = trx.getTransaction();
			Page updatedNodePage = t.getObject(Page.class, PAGE_ID);
			String nameAfter = updatedNodePage.getName();
			if (afterMigration != null) {
				afterMigration.accept(updatedNodePage);
			}

			assertEquals("Page name should change", "migrated page_" + PAGE_ID, nameAfter);
		}
	}

	/**
	 * Test migration of non editable parts
	 * 
	 * @throws Exception
	 */
	@Test
	public void testMigrationOfNonEditableParts() throws Exception {
		Construct sourceConstruct = null;
		Construct targetConstruct = null;
		Template template = null;
		Page testPage = null;

		try (Trx trx = new Trx(null, DBTestContext.USER_WITH_PERMS)) {
			Transaction t = trx.getTransaction();
			Node node = ContentNodeTestDataUtils.createNode("Migration Node", "mignode", "/", null, false, false);

			// create source and target constructs
			int sourceConstructId = createHandlebarsConstruct(node, "migrationsource", "source template {{gtx_render cms.tag.parts.text}}");
			int targetConstructId = createHandlebarsConstruct(node, "migrationtarget", "target template {{gtx_render cms.tag.parts.text}}");

			sourceConstruct = t.getObject(Construct.class, sourceConstructId);
			targetConstruct = t.getObject(Construct.class, targetConstructId);

			// create a template
			template = t.createObject(Template.class);
			template.setName("Migration Template");
			template.setSource("<node tag>");
			template.setFolderId(node.getFolder().getId());
			TemplateTag tTag = t.createObject(TemplateTag.class);
			tTag.setConstructId(sourceConstructId);
			tTag.setEnabled(true);
			tTag.setName("tag");
			tTag.setPublic(true);
			template.getTemplateTags().put(tTag.getName(), tTag);
			template.save();
			t.commit(false);

			// create a page
			testPage = t.createObject(Page.class);
			testPage.setFolderId(node.getFolder().getId());
			testPage.setTemplateId(template.getId());
			testPage.setName("Migration Page");
			testPage.getContentTag("tag").getValues().getByKeyname("text").setValueText("page content");
			testPage.save();
			t.commit(false);

			// now do the migration
			TagTypeMigrationMapping mapping = new TagTypeMigrationMapping();
			// migrate from source construct to target construct
			mapping.setFromTagTypeId(sourceConstructId);
			mapping.setToTagTypeId(targetConstructId);

			// generate the part mappings
			ArrayList<MigrationPartMapping> partMappings = new ArrayList<MigrationPartMapping>();
			mapping.setPartMappings(partMappings);

			// migrate source part "text" to target part "text"
			MigrationPartMapping partMapping = new MigrationPartMapping();
			partMapping.setFromPartId(ObjectTransformer.getInt(getPartByKeyname(sourceConstruct, "text").getId(), 0));
			partMapping.setToPartId(ObjectTransformer.getInt(getPartByKeyname(targetConstruct, "text").getId(), 0));
			partMappings.add(partMapping);

			// migrate source part "template" to target part "template"
			partMapping = new MigrationPartMapping();
			partMapping.setFromPartId(ObjectTransformer.getInt(getPartByKeyname(sourceConstruct, "hbs").getId(), 0));
			partMapping.setToPartId(ObjectTransformer.getInt(getPartByKeyname(targetConstruct, "hbs").getId(), 0));
			partMappings.add(partMapping);

			// Create list of objects to apply mappings to
			ArrayList<Integer> objectList = new ArrayList<Integer>(Arrays.asList(ObjectTransformer.getInt(testPage.getId(), 0)));

			// Create request object
			TagTypeMigrationRequest request = new TagTypeMigrationRequest();
			ArrayList<TagTypeMigrationMapping> ttmMappingList = new ArrayList<TagTypeMigrationMapping>();

			ttmMappingList.add(mapping);
			request.setMappings(ttmMappingList);
			request.setType(PAGE);
			request.setObjectIds(objectList);
			request.setEnabledPostProcessors(new ArrayList<MigrationPostProcessor>());

			// Create and execute job
			TagTypeMigrationJob job = new TagTypeMigrationJob();
			setJobParameter(job, t, request, objectList, false, false, false);
			assertJobSuccess(job, 10000);
			trx.success();
		}

		testContext.getContext().clearNodeObjectCache();

		try (Trx trx = new Trx(null, DBTestContext.USER_WITH_PERMS)) {
			Transaction t = trx.getTransaction();
			// check whether the page has been transformed
			testPage = t.getObject(Page.class, testPage.getId());
			ContentTag migratedTag = testPage.getContentTag("tag");
			assertEquals("Check tag construct after migration", targetConstruct, migratedTag.getConstruct());
			assertEquals("Check tag value after migration", "page content", migratedTag.getValues().getByKeyname("text").getValueText());

			// check whether the constructs were modified
			sourceConstruct = t.getObject(Construct.class, sourceConstruct.getId());
			targetConstruct = t.getObject(Construct.class, targetConstruct.getId());
			for (Part part : sourceConstruct.getParts()) {
				if ("hbs".equals(part.getKeyname())) {
					assertEquals("Check part template on source construct after migration", "source template {{gtx_render cms.tag.parts.text}}", part.getDefaultValue()
							.getValueText());
				}
			}
			for (Part part : targetConstruct.getParts()) {
				if ("hbs".equals(part.getKeyname())) {
					assertEquals("Check part template on target construct after migration", "target template {{gtx_render cms.tag.parts.text}}", part.getDefaultValue()
							.getValueText());
				}
			}
		}
	}

	/**
	 * Test Tagtype Migration on a template with a PostProcessor
	 * @throws Exception
	 */
	@Test
	public void testTemplateWithPostProcessor() throws Exception {
		Integer targetConstructId = null;
		Template template = null;

		try (Trx trx = new Trx(null, DBTestContext.USER_WITH_PERMS)) {
			Transaction t = trx.getTransaction();
			Node node = ContentNodeTestDataUtils.createNode("Migration Node", "mignode", "/", null, false, false);

			// create source and target constructs
			Integer sourceConstructId = createHandlebarsConstruct(node, "migrationsource", "source template {{gtx_render cms.tag.parts.text}}");
			targetConstructId = createHandlebarsConstruct(node, "migrationtarget", "target template {{gtx_render cms.tag.parts.text}}");

			Construct sourceConstruct = t.getObject(Construct.class, sourceConstructId);
			Construct targetConstruct = t.getObject(Construct.class, targetConstructId);

			// create a template
			template = t.createObject(Template.class);
			template.setName("Migration Template");
			template.setSource("<node tag>");
			template.setFolderId(node.getFolder().getId());
			TemplateTag tTag = t.createObject(TemplateTag.class);
			tTag.setConstructId(sourceConstructId);
			tTag.setEnabled(true);
			tTag.setName("tag");
			tTag.setPublic(true);
			template.getTemplateTags().put(tTag.getName(), tTag);
			template.save();
			t.commit(false);

			// now do the migration
			TagTypeMigrationMapping mapping = new TagTypeMigrationMapping();
			// migrate from source construct to target construct
			mapping.setFromTagTypeId(sourceConstructId);
			mapping.setToTagTypeId(targetConstructId);

			// generate the part mappings
			ArrayList<MigrationPartMapping> partMappings = new ArrayList<MigrationPartMapping>();
			mapping.setPartMappings(partMappings);

			// migrate source part "text" to target part "text"
			MigrationPartMapping partMapping = new MigrationPartMapping();
			partMapping.setFromPartId(ObjectTransformer.getInt(getPartByKeyname(sourceConstruct, "text").getId(), 0));
			partMapping.setToPartId(ObjectTransformer.getInt(getPartByKeyname(targetConstruct, "text").getId(), 0));
			partMappings.add(partMapping);

			// migrate source part "template" to target part "template"
			partMapping = new MigrationPartMapping();
			partMapping.setFromPartId(ObjectTransformer.getInt(getPartByKeyname(sourceConstruct, "hbs").getId(), 0));
			partMapping.setToPartId(ObjectTransformer.getInt(getPartByKeyname(targetConstruct, "hbs").getId(), 0));
			partMappings.add(partMapping);

			// Create list of objects to apply mappings to
			ArrayList<Integer> objectList = new ArrayList<Integer>(Arrays.asList(ObjectTransformer.getInt(template.getId(), 0)));

			// Create request object
			TagTypeMigrationRequest request = new TagTypeMigrationRequest();
			ArrayList<TagTypeMigrationMapping> ttmMappingList = new ArrayList<TagTypeMigrationMapping>();

			ttmMappingList.add(mapping);
			request.setMappings(ttmMappingList);
			request.setType(TEMPLATE);
			request.setObjectIds(objectList);
			request.setEnabledPostProcessors(new ArrayList<MigrationPostProcessor>(Arrays.asList(new MigrationPostProcessor(EmptyPostProcessor.class.getName(), 0))));

			// Create and execute job
			TagTypeMigrationJob job = new TagTypeMigrationJob();
			setJobParameter(job, t, request, objectList, false, false, false);
			assertJobSuccess(job, 10000);
			trx.success();
		}

		try (Trx trx = new Trx(null, DBTestContext.USER_WITH_PERMS)) {
			Transaction t = trx.getTransaction();

			// check whether the tag was migrated
			template = t.getObject(Template.class, template.getId());
			assertEquals("Check construct ID after migration", targetConstructId, template.getTemplateTag("tag").getConstruct().getId());
		}
	}

	/**
	 * Test migration that does not handle pages having the same template as the selected page
	 * @throws Exception
	 */
	@Test
	public void testHandlePagesByTemplateOff() throws Exception {
		runMigrationByTemplate(false, false);
	}

	/**
	 * Test migration that handles pages having the same template as the selected page in the same node
	 * @throws Exception
	 */
	@Test
	public void testHandlePagesByTemplateSameNode() throws Exception {
		runMigrationByTemplate(true, false);
	}

	/**
	 * Test migration that handles pages having the same template as the selected page across all nodes
	 * @throws Exception
	 */
	@Test
	public void testHandlePagesByTemplateAllNodes() throws Exception {
		runMigrationByTemplate(true, true);
	}

	/**
	 * Test various cases for migration of pages selected by template of a single page
	 * @param handlePagesByTemplate true if pages with same template shall be migrated
	 * @param handleAllNodes true if migration shall be done for all nodes
	 * @throws Exception
	 */
	private void runMigrationByTemplate(boolean handlePagesByTemplate, boolean handleAllNodes) throws Exception {
		Construct sourceConstruct = null;
		Construct targetConstruct = null;
		Page migrationPage1 = null;
		Page migrationPage2 = null;
		Page migrationPage3 = null;

		try (Trx trx = new Trx(null, DBTestContext.USER_WITH_PERMS)) {
			Transaction t = trx.getTransaction();

			// Create two nodes
			Node migrationNode1 = ContentNodeTestDataUtils.createNode("migration1", "Migration Node 1", PublishTarget.NONE);
			Node migrationNode2 = ContentNodeTestDataUtils.createNode("migration2", "Migration Node 2", PublishTarget.NONE);

			// create source and target constructs
			Integer sourceConstructId = createHandlebarsConstruct(migrationNode1, "migrationsource", "source template {{gtx_render cms.tag.parts.text}}");
			Integer targetConstructId = createHandlebarsConstruct(migrationNode1, "migrationtarget", "target template {{gtx_render cms.tag.parts.text}}");

			sourceConstruct = t.getObject(Construct.class, sourceConstructId);
			targetConstruct = t.getObject(Construct.class, targetConstructId);

			// create a template
			Template template = t.createObject(Template.class);
			template.setName("Migration Template");
			template.setSource("<node tag>");
			template.getFolders().addAll(Arrays.asList(migrationNode1.getFolder(), migrationNode2.getFolder()));
			TemplateTag tTag = t.createObject(TemplateTag.class);
			tTag.setConstructId(sourceConstructId);
			tTag.setEnabled(true);
			tTag.setName("tag");
			tTag.setPublic(true);
			template.getTemplateTags().put(tTag.getName(), tTag);
			template.save();
			t.commit(false);

			// create migration source page
			migrationPage1 = ContentNodeTestDataUtils.createPage(migrationNode1.getFolder(), template, "Page selected for migration");
			migrationPage2 = ContentNodeTestDataUtils.createPage(migrationNode1.getFolder(), template, "Other page in same node");
			migrationPage3 = ContentNodeTestDataUtils.createPage(migrationNode2.getFolder(), template, "Other page in other node");

			// now do the migration
			TagTypeMigrationMapping mapping = new TagTypeMigrationMapping();
			// migrate from source construct to target construct
			mapping.setFromTagTypeId(sourceConstructId);
			mapping.setToTagTypeId(targetConstructId);

			// generate the part mappings
			List<MigrationPartMapping> partMappings = new ArrayList<MigrationPartMapping>();
			mapping.setPartMappings(partMappings);

			// migrate source part "text" to target part "text"
			MigrationPartMapping partMapping = new MigrationPartMapping();
			partMapping.setFromPartId(getPartByKeyname(sourceConstruct, "text").getId());
			partMapping.setToPartId(getPartByKeyname(targetConstruct, "text").getId());
			partMappings.add(partMapping);

			// migrate source part "template" to target part "template"
			partMapping = new MigrationPartMapping();
			partMapping.setFromPartId(getPartByKeyname(sourceConstruct, "hbs").getId());
			partMapping.setToPartId(getPartByKeyname(targetConstruct, "hbs").getId());
			partMappings.add(partMapping);

			// Create list of objects to apply mappings to
			ArrayList<Integer> objectList = new ArrayList<Integer>(Arrays.asList(migrationPage1.getId()));

			// Create request object
			TagTypeMigrationRequest request = new TagTypeMigrationRequest();
			ArrayList<TagTypeMigrationMapping> ttmMappingList = new ArrayList<TagTypeMigrationMapping>();

			ttmMappingList.add(mapping);
			request.setMappings(ttmMappingList);
			request.setType(PAGE);
			request.setObjectIds(objectList);
			request.setHandlePagesByTemplate(handlePagesByTemplate);
			request.setHandleAllNodes(handleAllNodes);
			request.setPreventTriggerEvent(false);
			request.setEnabledPostProcessors(new ArrayList<MigrationPostProcessor>());

			// Create and execute job
			TagTypeMigrationJob job = new TagTypeMigrationJob();
			setJobParameter(job, t, request, objectList, handlePagesByTemplate, handleAllNodes, false);
			assertJobSuccess(job, 10000);
			trx.success();
		}

		// check whether the correct pages were migrated

		try (Trx trx = new Trx(null, DBTestContext.USER_WITH_PERMS)) {
			Transaction t = trx.getTransaction();
			migrationPage1 = t.getObject(Page.class, migrationPage1.getId());
			migrationPage2 = t.getObject(Page.class, migrationPage2.getId());
			migrationPage3 = t.getObject(Page.class, migrationPage3.getId());

			Map<Page, Boolean> migratedPages = new HashMap<Page, Boolean>();
			migratedPages.put(migrationPage1, true);
			migratedPages.put(migrationPage2, handlePagesByTemplate);
			migratedPages.put(migrationPage3, handlePagesByTemplate && handleAllNodes);

			for (Map.Entry<Page, Boolean> entry : migratedPages.entrySet()) {
				Page page = entry.getKey();
				boolean expected = entry.getValue();

				ContentTag tag = page.getContentTag("tag");
				assertNotNull(page + " must contain a tag 'tag'", tag);

				if (expected) {
					assertTrue(page + " was expected to be migrated", tag.getConstruct().equals(targetConstruct));
				} else {
					assertTrue(page + " was not expected to be migrated", tag.getConstruct().equals(sourceConstruct));
				}
			}
		}
	}

	/**
	 * Return the number of occurrences of a given construct ID in a given page
	 * 
	 * @param page
	 *            the page to count the occurrences in
	 * @param constructId
	 *            the construct ID to count occurrences of
	 * @return the number of content tags in the given page of the given type
	 * @throws NodeException
	 */
	private int getContentTagCount(Page page, int constructId) throws NodeException {

		int count = 0;

		Map<String, ContentTag> contentTags = page.getContentTags();

		for (Tag tag : contentTags.values()) {
			if (ObjectTransformer.getInt(tag.getConstruct().getId(), 0) == constructId) {
				count++;
			}
		}

		return count;
	}

	/**
	 * Get the part with given keyname from the construct
	 * 
	 * @param construct
	 *            construct (must not be null)
	 * @param partKeyname
	 *            part keyname (must not be null)
	 * @return part or null
	 * @throws NodeException
	 */
	private Part getPartByKeyname(Construct construct, String partKeyname) throws NodeException {
		List<Part> parts = construct.getParts();
		for (Part part : parts) {
			if (partKeyname.equals(part.getKeyname())) {
				return part;
			}
		}

		return null;
	}

	/**
	 * Create a handlebars construct
	 * 
	 * @param node
	 *            node to which the construct shall be assigned
	 * @param keyword
	 *            keyword
	 * @param template
	 *            template
	 * @return id of the construct
	 * @throws NodeException
	 */
	private int createHandlebarsConstruct(Node node, String keyword, String template) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		Construct construct = t.createObject(Construct.class);
		construct.setAutoEnable(true);
		construct.setKeyword(keyword);
		construct.setName(keyword, 1);
		construct.getNodes().add(node);

		// create handlebars part
		Part part = t.createObject(Part.class);
		part.setEditable(0);
		part.setHidden(false);
		part.setKeyname("hbs");
		part.setName("hbs", 1);
		part.setPartTypeId(getPartTypeId(HandlebarsPartType.class));
		part.setDefaultValue(t.createObject(Value.class));
		part.getDefaultValue().setValueText(template);
		construct.getParts().add(part);

		// create editable part
		part = t.createObject(Part.class);
		part.setEditable(1);
		part.setHidden(true);
		part.setKeyname("text");
		part.setName("text", 1);
		part.setPartTypeId(getPartTypeId(LongHTMLPartType.class));
		part.setDefaultValue(t.createObject(Value.class));
		construct.getParts().add(part);

		construct.save();
		t.commit(false);
		return ObjectTransformer.getInt(construct.getId(), 0);
	}

	/**
	 * Get the parttype id for the given parttype class
	 * 
	 * @param clazz
	 *            class
	 * @return parttype id
	 * @throws NodeException
	 *             if the parttype class was not found
	 */
	private <T> int getPartTypeId(final Class<T> clazz) throws NodeException {
		final int[] id = new int[1];
		DBUtils.executeStatement("SELECT id FROM type WHERE javaclass = ?", new SQLExecutor() {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				stmt.setString(1, clazz.getName());
			}

			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				if (rs.next()) {
					id[0] = rs.getInt("id");
				} else {
					fail("Could not find type for " + clazz);
				}
			}
		});

		return id[0];
	}

	/**
	 * Execute the given job and assert that it succeeds (without throwing any exceptions) in the given foreground time
	 * 
	 * @param job
	 *            job to execute
	 * @param foreground
	 *            foreground time (in ms)
	 * @throws NodeException
	 */
	private void assertJobSuccess(AbstractBackgroundJob job, int foreground) throws NodeException {
		AtomicBoolean foregroundFlag = new AtomicBoolean(true);
		job.execute(foreground, TimeUnit.SECONDS, () -> foregroundFlag.set(false));
		assertTrue("Job must finish in foreground within " + foreground + " ms", foregroundFlag.get());
	}
}
