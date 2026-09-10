package com.gentics.contentnode.tests.migration;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.Builder.update;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createChannel;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createObjectTagDefinition;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createPage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplateTag;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.localize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.MultichannellingFactory;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.migration.jobs.TagTypeMigrationJob;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.parttype.HTMLPartType;
import com.gentics.contentnode.rest.model.migration.MigrationPartMapping;
import com.gentics.contentnode.rest.model.migration.MigrationPostProcessor;
import com.gentics.contentnode.rest.model.migration.TagTypeMigrationMapping;
import com.gentics.contentnode.rest.model.request.migration.TagTypeMigrationRequest;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for the global tagtype migration
 */
@GCNFeature(set = Feature.MULTICHANNELLING)
@RunWith(value = Parameterized.class)
public class GlobalTagTypeMigrationTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	/**
	 * Get the test parameters
	 * 
	 * @return collection of test parameter sets
	 */
	@Parameters(name = "{index}: pages {0}, templates {1}, object properties {2}, nodes {3}, postprocessor {4}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (boolean pages : Arrays.asList(true, false)) {
			for (boolean templates : Arrays.asList(true, false)) {
				for (boolean objTagDefs : Arrays.asList(true, false)) {
					if (!pages && !templates && !objTagDefs) {
						continue;
					}
					for (List<?> restriction : Arrays.asList(Collections.emptyList(), Arrays.asList(NodeRestriction.master), Arrays.asList(NodeRestriction.channel), Arrays.asList(NodeRestriction.master, NodeRestriction.channel))) {
						for (boolean postProcessor : Arrays.asList(true, false)) {
							data.add(new Object[] {pages, templates, objTagDefs, restriction, postProcessor});
						}
					}
				}
			}
		}
		return data;
	}

	/**
	 * Test variation setting for migrating pages
	 */
	protected boolean pages;

	/**
	 * Test variation setting for migrating templates
	 */
	protected boolean templates;

	/**
	 * Test variation setting for migrating object property definitions
	 */
	protected boolean objTagDefs;

	/**
	 * Test variation setting for node restrictions
	 */
	protected List<NodeRestriction> restriction;

	/**
	 * Test variation setting for application of a post processor
	 */
	protected boolean postProcessor;

	/**
	 * Test data objects
	 */
	private Node master;
	private Node channel;
	private Node other;
	private Construct sourceConstruct;
	private Construct targetConstruct;
	private ObjectTagDefinition oeAllNodes;
	private ObjectTagDefinition oeMaster;
	private ObjectTagDefinition oeOther;
	private ObjectTagDefinition oeMasterOther;
	private Template masterTemplate;
	private Template otherTemplate;
	private Template bothTemplate;
	private Page masterPage;
	private Template channelTemplate;
	private Page channelPage;
	private Page otherPage;

	/**
	 * Map for objects, that are expected to be migrated
	 */
	private Map<NodeObject, Boolean> expectMigration;

	/**
	 * Create a test instance
	 * @param pages true to migrate pages
	 * @param templates true to migrate templates
	 * @param objTagDefs true to migrate obj tag definitions
	 * @param restriction node restrictions
	 * @param postProcessor true to add a post processor
	 */
	public GlobalTagTypeMigrationTest(boolean pages, boolean templates, boolean objTagDefs, List<NodeRestriction> restriction, boolean postProcessor) {
		this.pages = pages;
		this.templates = templates;
		this.objTagDefs = objTagDefs;
		this.restriction = restriction;
		this.postProcessor = postProcessor;
	}

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();
	}

	/**
	 * Create test data
	 * @throws Exception
	 */
	@Before
	public void setUpTestData() throws Exception {
		LoggingPostProcessor.reset();

		// create nodes
		master = supply(() -> createNode("master", "Master Node", PublishTarget.NONE));
		channel = supply(() -> createChannel(master, "Channel", "channel", "/", PublishTarget.NONE));
		other = supply(() -> createNode("other", "Other Node", PublishTarget.NONE));

		// create constructs
		sourceConstruct = supply(t -> t.getObject(Construct.class, createConstruct(master, HTMLPartType.class, "migratefrom", "part")));
		targetConstruct = supply(t -> t.getObject(Construct.class, createConstruct(master, HTMLPartType.class, "migrateto", "part")));

		// create obj tag defs
		oeAllNodes = supply(() -> createObjectTagDefinition("tomigrate_all", Folder.TYPE_FOLDER, sourceConstruct.getId()));
		oeMaster = supply(() -> createObjectTagDefinition("tomigrate_master", Folder.TYPE_FOLDER, sourceConstruct.getId(), master));
		oeOther = supply(() -> createObjectTagDefinition("tomigrate_other", Folder.TYPE_FOLDER, sourceConstruct.getId(), other));
		oeMasterOther = supply(() -> createObjectTagDefinition("tomigrate_master_other", Folder.TYPE_FOLDER, sourceConstruct.getId(), master, other));

		// edit the root folders to create the ObjectTags
		operate(t -> {
			for (Folder f : Arrays.asList(master.getFolder(), channel.getFolder(), other.getFolder())) {
				f = t.getObject(Folder.class, f.getId(), true);
				f.save();
			}
		});

		// create templates
		masterTemplate = supply(() -> createTemplate(master.getFolder(), "Master Template", "Master Template",
				createTemplateTag(sourceConstruct.getId(), "migrate1", false, false),
				createTemplateTag(sourceConstruct.getId(), "migrate2", true, false)));
		otherTemplate = supply(() -> createTemplate(other.getFolder(), "Other Template", "Other Template",
				createTemplateTag(sourceConstruct.getId(), "migrate1", false, false),
				createTemplateTag(sourceConstruct.getId(), "migrate2", true, false)));
		bothTemplate = supply(() -> createTemplate(master.getFolder(), "Both Template", "Both Template",
				createTemplateTag(sourceConstruct.getId(), "migrate1", false, false),
				createTemplateTag(sourceConstruct.getId(), "migrate2", true, false)));

		bothTemplate = update(bothTemplate, tmpl -> {
			tmpl.addFolder(other.getFolder());
		}).build();

		channelTemplate = supply(() -> localize(masterTemplate, channel));

		// create pages
		masterPage = supply(() -> createPage(master.getFolder(), masterTemplate, "Master Page"));

		masterPage = update(masterPage, p -> {
			p.getContent().addContentTag(sourceConstruct.getId()).setName("migratefrom1");
		}).build();

		channelPage = supply(() -> ContentNodeTestDataUtils.localize(masterPage, channel));

		otherPage = supply(() -> createPage(other.getFolder(), otherTemplate, "Other Page"));

		otherPage = update(otherPage, p -> {
			p.getContent().addContentTag(sourceConstruct.getId()).setName("migratefrom1");
		}).build();
	}

	/**
	 * Create test data
	 * @throws Exception
	 */
	@After
	public void cleanupTestData() throws Exception {
		operate(() -> {
			other.delete();
			channel.delete();
			master.delete();

			oeAllNodes.delete();
			oeMasterOther.delete();
			oeOther.delete();
			oeMaster.delete();

			targetConstruct.delete();
			sourceConstruct.delete();
		});
	}

	/**
	 * Test performing a global tag migration
	 * @throws Exception
	 */
	@Test
	public void testMigration() throws Exception {
		// Create mapping
		TagTypeMigrationMapping mapping = new TagTypeMigrationMapping();
		mapping.setFromTagTypeId(sourceConstruct.getId());
		mapping.setToTagTypeId(targetConstruct.getId());
		MigrationPartMapping partMapping = new MigrationPartMapping();

		partMapping.setFromPartId(supply(() -> sourceConstruct.getParts().get(0).getId()));
		partMapping.setToPartId(supply(() -> targetConstruct.getParts().get(0).getId()));
		ArrayList<MigrationPartMapping> partMappings = new ArrayList<MigrationPartMapping>();

		partMappings.add(partMapping);
		mapping.setPartMappings(partMappings);

		// create request
		TagTypeMigrationRequest request = new TagTypeMigrationRequest();
		request.setType("global");
		ArrayList<TagTypeMigrationMapping> mappings = new ArrayList<TagTypeMigrationMapping>();
		mappings.add(mapping);
		request.setMappings(mappings);
		request.setHandleGlobalPages(pages);
		request.setHandleGlobalTemplates(templates);
		request.setHandleGlobalObjTagDefs(objTagDefs);
		request.setRestrictedNodeIds(new ArrayList<Integer>());
		for (NodeRestriction r : restriction) {
			switch (r) {
			case master:
				request.getRestrictedNodeIds().add(master.getId());
				break;
			case channel:
				request.getRestrictedNodeIds().add(channel.getId());
				break;
			case other:
				request.getRestrictedNodeIds().add(other.getId());
				break;
			}
		}
		if (postProcessor) {
			request.setEnabledPostProcessors(Arrays.asList(new MigrationPostProcessor(LoggingPostProcessor.class.getName(), 0)));
		}

		// run the job and check whether it succeeded in foreground
		TagTypeMigrationJob job = new TagTypeMigrationJob();
		setJobParameter(job, request);
		AtomicBoolean foreground = new AtomicBoolean(true);
		operate(() -> {
			job.execute(60, TimeUnit.SECONDS, () -> foreground.set(false));
		});
		assertTrue("TagTypeMigrationJob must finish in foreground", foreground.get());

		operate(() -> {
			// determine, which objects are expected to be migrated
			expectMigration = new HashMap<>();
			expectMigration.put(masterTemplate, templates && isHandled(NodeRestriction.master));
			expectMigration.put(channelTemplate, templates && isHandled(NodeRestriction.channel));
			expectMigration.put(otherTemplate, templates && isHandled(NodeRestriction.other));
			expectMigration.put(bothTemplate, templates && isHandled(NodeRestriction.master, NodeRestriction.other));

			expectMigration.put(masterPage, pages && isHandled(NodeRestriction.master));
			expectMigration.put(channelPage, pages && isHandled(NodeRestriction.channel));
			expectMigration.put(otherPage, pages && isHandled(NodeRestriction.other));

			expectMigration.put(oeAllNodes, objTagDefs);
			expectMigration.put(oeMaster, objTagDefs && isHandled(NodeRestriction.master));
			expectMigration.put(oeMasterOther, objTagDefs && isHandled(NodeRestriction.master, NodeRestriction.other));
			expectMigration.put(oeOther, objTagDefs && isHandled(NodeRestriction.other));

			// make assertions about what has been migrated
			for (Template template : Arrays.asList(masterTemplate, channelTemplate, otherTemplate, bothTemplate)) {
				assertMigrated(template);
			}

			for (Page page : Arrays.asList(masterPage, channelPage, otherPage)) {
				assertMigrated(page);
			}

			for (ObjectTagDefinition objTagDef : Arrays.asList(oeAllNodes, oeMaster, oeMasterOther, oeOther)) {
				assertMigrated(objTagDef);
			}

			// make assertions about post processing
			for (NodeObject o : Arrays.asList(masterTemplate, channelTemplate, otherTemplate, bothTemplate, masterPage, channelPage, otherPage)) {
				assertEquals("Check whether " + o + " was post processed", postProcessor && expectMigration.get(o), LoggingPostProcessor.isProcessed(o));
			}
			for (Folder f : Arrays.asList(master.getFolder(), channel.getFolder(), other.getFolder())) {
				boolean hasMigratedObjectTag = false;

				for (ObjectTag tag : f.getObjectTags().values()) {
					if (ObjectTransformer.getBoolean(expectMigration.get(tag.getDefinition()), false)) {
						hasMigratedObjectTag = true;
						break;
					}
				}

				assertEquals("Check whether " + f + " was post processed", postProcessor && hasMigratedObjectTag, LoggingPostProcessor.isProcessed(f));
			}
		});
	}

	/**
	 * Sets the ttm job parameters
	 * 
	 * @param job ttm Job instance
	 * @param request TTM Request
	 */
	private void setJobParameter(TagTypeMigrationJob job, TagTypeMigrationRequest request) throws NodeException {
		job.setRequest(request);
		job.setType(request.getType());
		job.setObjectIds(request.getObjectIds() != null ? new ArrayList<>(request.getObjectIds()) : null);
		job.setHandlePagesByTemplate(request.isHandlePagesByTemplate());
		job.setHandleAllNodes(request.isHandleAllNodes());
		job.setPreventTriggerEvent(request.isPreventTriggerEvent());
	}

	/**
	 * Check whether any of the given nodes shall be handled
	 * @param nodes optional list of NodeRestrictions
	 * @return true if any node shall be handled, false if not
	 */
	private boolean isHandled(NodeRestriction...nodes) {
		if (restriction.isEmpty()) {
			return true;
		}
		for (NodeRestriction node : nodes) {
			if (restriction.contains(node)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Assert that the page has been migrated or not migrated
	 * @param page page
	 * @throws NodeException
	 */
	private void assertMigrated(Page page) throws NodeException {
		boolean expected = expectMigration.get(page);
		Transaction t = TransactionManager.getCurrentTransaction();
		page = t.getObject(Page.class, page.getId());
		ContentTag tag = page.getContentTag("migratefrom1");
		assertNotNull("Tag 'migratefrom1' must exist in page " + page, tag);
		if (expected) {
			assertTrue(page + " was expected to be migrated", targetConstruct.equals(tag.getConstruct()));
		} else {
			assertTrue(page + " was not expected to be migrated", sourceConstruct.equals(tag.getConstruct()));
		}
	}

	/**
	 * Assert that the template has been migrated or not migrated
	 * @param template template
	 * @throws NodeException
	 */
	private void assertMigrated(Template template) throws NodeException {
		boolean expected = expectMigration.get(template);

		Transaction t = TransactionManager.getCurrentTransaction();
		template = t.getObject(Template.class, template.getId());
		TemplateTag migrate1 = template.getTemplateTag("migrate1");
		assertNotNull("Tag 'migrate1' must exist in template " + template, migrate1);
		TemplateTag migrate2 = template.getTemplateTag("migrate2");
		assertNotNull("Tag 'migrate2' must exist in template " + template, migrate2);

		if (expected) {
			assertTrue(template + " was expected to be migrated", targetConstruct.equals(migrate1.getConstruct()));
			assertTrue(template + " was expected to be migrated", targetConstruct.equals(migrate2.getConstruct()));
		} else {
			assertTrue(template + " was not expected to be migrated", sourceConstruct.equals(migrate1.getConstruct()));
			assertTrue(template + " was not expected to be migrated", sourceConstruct.equals(migrate2.getConstruct()));
		}

		for (Page page : template.getPages()) {
			// check whether page really uses the given template (or a localized copy)
			Node pageChannel = page.getChannel();
			if (pageChannel != null) {
				if (!template.equals(MultichannellingFactory.getChannelVariant(page.getTemplate(), channel))) {
					continue;
				}
			}

			ContentTag tag = page.getContentTag("migrate2");
			assertNotNull("Tag 'migrate2' must exist in " + page, tag);
			if (expected || expectMigration.get(page)) {
				assertTrue(page + " was expected to be migrated", targetConstruct.equals(tag.getConstruct()));
			} else {
				assertTrue(page + " was not expected to be migrated", sourceConstruct.equals(tag.getConstruct()));
			}
		}
	}

	/**
	 * Assert that the objTagDef has been migrated or not migrated
	 * @param objTagDef objTagDef
	 * @throws NodeException
	 */
	private void assertMigrated(ObjectTagDefinition objTagDef) throws NodeException {
		boolean expected = expectMigration.get(objTagDef);

		Transaction t = TransactionManager.getCurrentTransaction();
		objTagDef = t.getObject(ObjectTagDefinition.class, objTagDef.getId());
		List<Node> objTagDefNodes = objTagDef.getNodes();

		if (expected) {
			assertTrue(objTagDef + " was expected to be migrated", targetConstruct.equals(objTagDef.getObjectTag().getConstruct()));
		} else {
			assertTrue(objTagDef + " was not expected to be migrated", sourceConstruct.equals(objTagDef.getObjectTag().getConstruct()));
		}

		String name = objTagDef.getObjectTag().getName();
		if (name.startsWith("object.")) {
			name = name.substring("object.".length());
		}
		for (Folder f : Arrays.asList(master.getFolder(), channel.getFolder(), other.getFolder())) {
			if (!objTagDefNodes.isEmpty() && !objTagDefNodes.contains(f.getOwningNode())) {
				continue;
			}
			ObjectTag tag = f.getObjectTag(name);
			assertNotNull(f + " is expected to have object tag " + name, tag);
			if (expected) {
				assertTrue("Object property" + objTagDef.getObjectTag().getName() + " of " + f + " was expected to be migrated",
						targetConstruct.equals(tag.getConstruct()));
			} else {
				assertTrue("Object property" + objTagDef.getObjectTag().getName() + " of " + f + " was not expected to be migrated",
						sourceConstruct.equals(tag.getConstruct()));
			}
		}
	}

	/**
	 * Enumeration of possible node restrictions
	 */
	public static enum NodeRestriction {
		master, channel, other
	}
}
