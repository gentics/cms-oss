package com.gentics.contentnode.tests.publish.attributes;

import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.events.Dependency;
import com.gentics.contentnode.events.DependencyManager;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.parttype.handlebars.HandlebarsPartType;
import com.gentics.contentnode.publish.PublishQueue;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.tests.utils.ContentNodeTestUtils;
import com.gentics.contentnode.tests.utils.CountHelperSource;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.contentnode.testutils.TestHelpersHandlebarsService;
import com.gentics.lib.content.GenticsContentAttribute;

/**
 * Test cases for publishing after attribute specific dirting
 */
@GCNFeature(set={Feature.ATTRIBUTE_DIRTING, Feature.MULTICHANNELLING, Feature.MCCR, Feature.CR_FILESYSTEM_ATTRIBUTES})
public class AttributePublishTest {
	/**
	 * Template for the hbs construct that counts, how often it is rendered
	 */
	public final static String HBS = "value of {{gtx_render cms.tag.name}}: [{{gtx_test_count cms.tag.name}}]";

	/**
	 * Test file contents
	 */
	public final static byte[] FILE_CONTENTS = "Test File Contents".getBytes();

	/**
	 * Changed file contents
	 */
	public final static byte[] CHANGED_FILE_CONTENTS = "Changed Test File Contents".getBytes();

	/**
	 * Test template source
	 */
	public final static String TEMPLATE = "Template of page <node page.name>";

	@BeforeClass
	public static void setupOnce() {
		TestHelpersHandlebarsService.addHelper(CountHelperSource.class);
	}

	@Rule
	public DBTestContext testContext = new DBTestContext();


	@Test
	public void testDependenciesAfterFolderNameChange() throws Exception {
		Node node = null;
		Node channel = null;
		Folder folder = null;

		// create test data
		try (Trx trx = new Trx()) {
			node = ContentNodeTestDataUtils.createNode("testhost", "TestNode", PublishTarget.CONTENTREPOSITORY);
			ContentNodeTestDataUtils.addTagmapEntry(node.getContentRepository(), Folder.TYPE_FOLDER, GenticsContentAttribute.ATTR_TYPE_OBJ, "folder.mother",
					"folder_id", null, false, false, false, Folder.TYPE_FOLDER, null, null);
			channel = ContentNodeTestDataUtils.createChannel(node, "Testchannel", "testchannel", "/", PublishTarget.CONTENTREPOSITORY);
			ContentNodeTestDataUtils.addTagmapEntry(channel.getContentRepository(), Folder.TYPE_FOLDER, GenticsContentAttribute.ATTR_TYPE_OBJ, "folder.mother",
					"folder_id", null, false, false, false, Folder.TYPE_FOLDER, null, null);
			folder = ContentNodeTestDataUtils.createFolder(node.getFolder(), "Folder");
			trx.success();
		}

		// publish into cr (creating the dependencies)
		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		// get the dependencies for the folder
		List<Dependency> expectedDependencies = null;
		try (Trx trx = new Trx()) {
			expectedDependencies = DependencyManager.getDependenciesForObject(folder, null, null);
			trx.success();
		}

		// change the folder name
		try (Trx trx = new Trx()) {
			Transaction t = trx.getTransaction();
			folder = t.getObject(folder, true);
			folder.setName("Changed Folder");
			folder.save();
			folder = t.getObject(folder);
			trx.success();
		}

		// publish again
		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		// assert: dependencies must be unchanged
		List<Dependency> actualDependencies = null;
		try (Trx trx = new Trx()) {
			actualDependencies = DependencyManager.getDependenciesForObject(folder, null, null);
			trx.success();
		}

		assertThat(actualDependencies).as("Dependencies for " + folder)
				.usingElementComparatorOnFields("id", "source", "dependent", "sourceProperty", "storedDepProperties", "channelIds")
				.containsOnlyElementsOf(expectedDependencies);
	}

	@Test
	public void testRenderOnlyDirtedAttributesForFolder() throws Exception {
		Node node = null;
		Node channel = null;
		Folder folder = null;
		Construct construct = null;

		// create test data
		try (Trx trx = new Trx()) {
			// node and channel with crs
			node = ContentNodeTestDataUtils.createNode("testhost", "TestNode", PublishTarget.CONTENTREPOSITORY);
			ContentNodeTestDataUtils.addTagmapEntry(node.getContentRepository(), Folder.TYPE_FOLDER, GenticsContentAttribute.ATTR_TYPE_TEXT, "object.attr1",
					"attr1", null, false, false, false, 0, null, null);
			ContentNodeTestDataUtils.addTagmapEntry(node.getContentRepository(), Folder.TYPE_FOLDER, GenticsContentAttribute.ATTR_TYPE_TEXT, "object.attr2",
					"attr2", null, false, false, false, 0, null, null);
			channel = ContentNodeTestDataUtils.createChannel(node, "Testchannel", "testchannel", "/", PublishTarget.CONTENTREPOSITORY);
			ContentNodeTestDataUtils.addTagmapEntry(channel.getContentRepository(), Folder.TYPE_FOLDER, GenticsContentAttribute.ATTR_TYPE_TEXT, "object.attr1",
					"attr1", null, false, false, false, 0, null, null);
			ContentNodeTestDataUtils.addTagmapEntry(channel.getContentRepository(), Folder.TYPE_FOLDER, GenticsContentAttribute.ATTR_TYPE_TEXT, "object.attr2",
					"attr2", null, false, false, false, 0, null, null);

			// construct that counts when rendered
			int hbsConstructId = ContentNodeTestDataUtils.createConstruct(node, HandlebarsPartType.class, "hbs", "hbs");
			construct = trx.getTransaction().getObject(Construct.class, hbsConstructId, true);
			construct.getParts().stream().filter(part -> "hbs".equals(part.getKeyname())).forEach(part -> {
				try {
					part.getDefaultValue().setValueText(HBS);
				} catch (Exception e) {
					e.printStackTrace();
					fail("Setup of construct failed");
				}
			});
			construct.save();
			construct = trx.getTransaction().getObject(construct);

			// Object Property Definitions
			ContentNodeTestDataUtils.createObjectPropertyDefinition(Folder.TYPE_FOLDER, construct.getId(), "Attribute1", "object.attr1");
			ContentNodeTestDataUtils.createObjectPropertyDefinition(Folder.TYPE_FOLDER, construct.getId(), "Attribute2", "object.attr2");

			folder = ContentNodeTestDataUtils.createFolder(node.getFolder(), "Folder");
			folder = trx.getTransaction().getObject(folder, true);
			folder.getObjectTag("attr1").setEnabled(true);
			folder.getObjectTag("attr2").setEnabled(true);
			folder.save();
			folder = trx.getTransaction().getObject(folder);
			trx.success();
		}

		// publish into cr (creating the dependencies)
		CountHelperSource.reset();
		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		// every attribute must be rendered for node and channel
		assertThat(CountHelperSource.get("object.attr1")).as("Count for object.attr1").isEqualTo(2);
		assertThat(CountHelperSource.get("object.attr2")).as("Count for object.attr2").isEqualTo(2);

		// get the dependencies
		List<Dependency> expectedDependencies = null;
		try (Trx trx = new Trx()) {
			expectedDependencies = DependencyManager.getDependenciesForObject(folder, null, null);
			trx.success();
		}

		// dirt attr1 for folder in node
		try (Trx trx = new Trx()) {
			PublishQueue.dirtObject(folder, PublishQueue.Action.DEPENDENCY, node.getId(), "attr1");
			trx.success();
		}

		// publish into cr (creating the dependencies)
		CountHelperSource.reset();
		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		assertThat(CountHelperSource.get("object.attr1")).as("Count for object.attr1").isEqualTo(1);
		assertThat(CountHelperSource.get("object.attr2")).as("Count for object.attr2").isEqualTo(0);

		// assert that dependencies did not change
		List<Dependency> actualDependencies = null;
		try (Trx trx = new Trx()) {
			actualDependencies = DependencyManager.getDependenciesForObject(folder, null, null);
			trx.success();
		}

		assertThat(actualDependencies).as("Dependencies for " + folder)
				.usingElementComparatorOnFields("id", "source", "dependent", "sourceProperty", "storedDepProperties", "channelIds")
				.containsOnlyElementsOf(expectedDependencies);

		// assert attributes in cr
		assertThat(node.getContentMap()).as("Node CR").contains(folder).withAttribute("attr1", "value of object.attr1: []")
				.withAttribute("attr2", "value of object.attr2: []");
		assertThat(channel.getContentMap()).as("Channel CR").contains(folder).withAttribute("attr1", "value of object.attr1: []")
				.withAttribute("attr2", "value of object.attr2: []");
	}

	@Test
	public void testRenderOnlyDirtedAttributesForFile() throws Exception {
		Node node = null;
		Node channel = null;
		File file = null;
		Construct construct = null;

		// create test data
		try (Trx trx = new Trx()) {
			// node and channel with crs
			node = ContentNodeTestDataUtils.createNode("testhost", "TestNode", PublishTarget.CONTENTREPOSITORY);
			ContentNodeTestDataUtils.addTagmapEntry(node.getContentRepository(), File.TYPE_FILE, GenticsContentAttribute.ATTR_TYPE_TEXT, "object.attr1",
					"attr1", null, false, false, false, 0, null, null);
			ContentNodeTestDataUtils.addTagmapEntry(node.getContentRepository(), File.TYPE_FILE, GenticsContentAttribute.ATTR_TYPE_TEXT, "object.attr2",
					"attr2", null, false, false, false, 0, null, null);
			channel = ContentNodeTestDataUtils.createChannel(node, "Testchannel", "testchannel", "/", PublishTarget.CONTENTREPOSITORY);
			ContentNodeTestDataUtils.addTagmapEntry(channel.getContentRepository(), File.TYPE_FILE, GenticsContentAttribute.ATTR_TYPE_TEXT, "object.attr1",
					"attr1", null, false, false, false, 0, null, null);
			ContentNodeTestDataUtils.addTagmapEntry(channel.getContentRepository(), File.TYPE_FILE, GenticsContentAttribute.ATTR_TYPE_TEXT, "object.attr2",
					"attr2", null, false, false, false, 0, null, null);

			// construct that counts when rendered
			int hbsConstructId = ContentNodeTestDataUtils.createConstruct(node, HandlebarsPartType.class, "hbs", "hbs");
			construct = trx.getTransaction().getObject(Construct.class, hbsConstructId, true);
			construct.getParts().stream().filter(part -> "hbs".equals(part.getKeyname())).forEach(part -> {
				try {
					part.getDefaultValue().setValueText(HBS);
				} catch (Exception e) {
					e.printStackTrace();
					fail("Setup of construct failed");
				}
			});
			construct.save();
			construct = trx.getTransaction().getObject(construct);

			// Object Property Definitions
			ContentNodeTestDataUtils.createObjectPropertyDefinition(File.TYPE_FILE, construct.getId(), "Attribute1", "object.attr1");
			ContentNodeTestDataUtils.createObjectPropertyDefinition(File.TYPE_FILE, construct.getId(), "Attribute2", "object.attr2");

			file = ContentNodeTestDataUtils.createFile(node.getFolder(), "testfile.txt", FILE_CONTENTS);
			file = trx.getTransaction().getObject(file, true);
			file.getObjectTag("attr1").setEnabled(true);
			file.getObjectTag("attr2").setEnabled(true);
			file.save();
			file = trx.getTransaction().getObject(file);
			trx.success();
		}

		// publish into cr (creating the dependencies)
		CountHelperSource.reset();
		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		// every attribute must be rendered for node and channel
		assertThat(CountHelperSource.get("object.attr1")).as("Count for object.attr1").isEqualTo(2);
		assertThat(CountHelperSource.get("object.attr2")).as("Count for object.attr2").isEqualTo(2);

		// get the dependencies
		List<Dependency> expectedDependencies = null;
		try (Trx trx = new Trx()) {
			expectedDependencies = DependencyManager.getDependenciesForObject(file, null, null);
			trx.success();
		}

		// dirt attr1 for file in node
		try (Trx trx = new Trx()) {
			PublishQueue.dirtObject(file, PublishQueue.Action.DEPENDENCY, node.getId(), "attr1");
			trx.success();
		}

		// publish into cr (creating the dependencies)
		CountHelperSource.reset();
		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		assertThat(CountHelperSource.get("object.attr1")).as("Count for object.attr1").isEqualTo(1);
		assertThat(CountHelperSource.get("object.attr2")).as("Count for object.attr2").isEqualTo(0);

		// assert that dependencies did not change
		List<Dependency> actualDependencies = null;
		try (Trx trx = new Trx()) {
			actualDependencies = DependencyManager.getDependenciesForObject(file, null, null);
			trx.success();
		}

		assertThat(actualDependencies).as("Dependencies for " + file)
				.usingElementComparatorOnFields("id", "source", "dependent", "sourceProperty", "storedDepProperties", "channelIds")
				.containsOnlyElementsOf(expectedDependencies);

		// assert attributes in cr
		assertThat(node.getContentMap()).as("Node CR").contains(file).withAttribute("attr1", "value of object.attr1: []")
				.withAttribute("attr2", "value of object.attr2: []").withAttribute("binarycontent", FILE_CONTENTS);
		assertThat(channel.getContentMap()).as("Channel CR").contains(file).withAttribute("attr1", "value of object.attr1: []")
				.withAttribute("attr2", "value of object.attr2: []").withAttribute("binarycontent", FILE_CONTENTS);
	}

	@Test
	public void testRenderOnlyDirtedAttributesForPage() throws Exception {
		Node node = null;
		Node channel = null;
		Page page = null;
		Construct construct = null;

		// create test data
		try (Trx trx = new Trx()) {
			// node and channel with crs
			node = ContentNodeTestDataUtils.createNode("testhost", "TestNode", PublishTarget.CONTENTREPOSITORY);
			ContentNodeTestDataUtils.addTagmapEntry(node.getContentRepository(), Page.TYPE_PAGE, GenticsContentAttribute.ATTR_TYPE_TEXT, "object.attr1",
					"attr1", null, false, false, false, 0, null, null);
			ContentNodeTestDataUtils.addTagmapEntry(node.getContentRepository(), Page.TYPE_PAGE, GenticsContentAttribute.ATTR_TYPE_TEXT, "object.attr2",
					"attr2", null, false, false, false, 0, null, null);
			channel = ContentNodeTestDataUtils.createChannel(node, "Testchannel", "testchannel", "/", PublishTarget.CONTENTREPOSITORY);
			ContentNodeTestDataUtils.addTagmapEntry(channel.getContentRepository(), Page.TYPE_PAGE, GenticsContentAttribute.ATTR_TYPE_TEXT, "object.attr1",
					"attr1", null, false, false, false, 0, null, null);
			ContentNodeTestDataUtils.addTagmapEntry(channel.getContentRepository(), Page.TYPE_PAGE, GenticsContentAttribute.ATTR_TYPE_TEXT, "object.attr2",
					"attr2", null, false, false, false, 0, null, null);

			// construct that counts when rendered
			int hbsConstructId = ContentNodeTestDataUtils.createConstruct(node, HandlebarsPartType.class, "hbs", "hbs");
			construct = trx.getTransaction().getObject(Construct.class, hbsConstructId, true);
			construct.getParts().stream().filter(part -> "hbs".equals(part.getKeyname())).forEach(part -> {
				try {
					part.getDefaultValue().setValueText(HBS);
				} catch (Exception e) {
					e.printStackTrace();
					fail("Setup of construct failed");
				}
			});
			construct.save();
			construct = trx.getTransaction().getObject(construct);

			// Object Property Definitions
			ContentNodeTestDataUtils.createObjectPropertyDefinition(Page.TYPE_PAGE, construct.getId(), "Attribute1", "object.attr1");
			ContentNodeTestDataUtils.createObjectPropertyDefinition(Page.TYPE_PAGE, construct.getId(), "Attribute2", "object.attr2");

			page = ContentNodeTestDataUtils.createTemplateAndPage(node.getFolder(), "Testpage");
			Template template = trx.getTransaction().getObject(page.getTemplate(), true);
			template.setSource(TEMPLATE);
			template.save();

			page = trx.getTransaction().getObject(page, true);
			page.getObjectTag("attr1").setEnabled(true);
			page.getObjectTag("attr2").setEnabled(true);
			page.save();
			page.publish();
			page = trx.getTransaction().getObject(page);
			trx.success();
		}

		// publish into cr (creating the dependencies)
		CountHelperSource.reset();
		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		// every attribute must be rendered for node and channel
		assertThat(CountHelperSource.get("object.attr1")).as("Count for object.attr1").isEqualTo(2);
		assertThat(CountHelperSource.get("object.attr2")).as("Count for object.attr2").isEqualTo(2);

		// get the dependencies
		List<Dependency> expectedDependencies = null;
		try (Trx trx = new Trx()) {
			expectedDependencies = DependencyManager.getDependenciesForObject(page, null, null);
			trx.success();
		}

		// dirt attr1 for page in node
		try (Trx trx = new Trx()) {
			PublishQueue.dirtObject(page, PublishQueue.Action.DEPENDENCY, node.getId(), "attr1");
			trx.success();
		}

		// publish into cr (creating the dependencies)
		CountHelperSource.reset();
		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		assertThat(CountHelperSource.get("object.attr1")).as("Count for object.attr1").isEqualTo(1);
		assertThat(CountHelperSource.get("object.attr2")).as("Count for object.attr2").isEqualTo(0);

		// assert that dependencies did not change
		List<Dependency> actualDependencies = null;
		try (Trx trx = new Trx()) {
			actualDependencies = DependencyManager.getDependenciesForObject(page, null, null);
			trx.success();
		}

		assertThat(actualDependencies).as("Dependencies for " + page)
				.usingElementComparatorOnFields("id", "source", "dependent", "sourceProperty", "storedDepProperties", "channelIds")
				.containsOnlyElementsOf(expectedDependencies);

		// assert attributes in cr
		assertThat(node.getContentMap()).as("Node CR").contains(page).withAttribute("attr1", "value of object.attr1: []")
				.withAttribute("attr2", "value of object.attr2: []").withAttribute("content", "Template of page " + page.getName());
		assertThat(channel.getContentMap()).as("Channel CR").contains(page).withAttribute("attr1", "value of object.attr1: []")
				.withAttribute("attr2", "value of object.attr2: []").withAttribute("content", "Template of page " + page.getName());
	}

	@Test
	public void testDirtPageName() throws Exception {
		dirtPageName(true);
	}

	@Test
	@GCNFeature(unset={Feature.ATTRIBUTE_DIRTING})
	public void testDirtPageNameNoFeature() throws Exception {
		dirtPageName(false);
	}

	/**
	 * Dirt the name of a page and check whether the content is rendered
	 * @param feature true if the feature is activated, false if not
	 * @throws Exception
	 */
	protected void dirtPageName(boolean feature) throws Exception {
		String tagName = "tag";
		Node node = null;
		Node channel = null;
		Page page = null;
		Construct construct = null;

		// create test data
		try (Trx trx = new Trx()) {
			// node and channel with crs
			node = ContentNodeTestDataUtils.createNode("testhost", "TestNode", PublishTarget.CONTENTREPOSITORY);
			channel = ContentNodeTestDataUtils.createChannel(node, "Testchannel", "testchannel", "/", PublishTarget.CONTENTREPOSITORY);

			// construct that counts when rendered
			int hbsConstructId = ContentNodeTestDataUtils.createConstruct(node, HandlebarsPartType.class, "hbs", "hbs");
			construct = trx.getTransaction().getObject(Construct.class, hbsConstructId, true);
			construct.getParts().stream().filter(part -> "hbs".equals(part.getKeyname())).forEach(part -> {
				try {
					part.getDefaultValue().setValueText(HBS);
				} catch (Exception e) {
					e.printStackTrace();
					fail("Setup of construct failed");
				}
			});
			construct.save();
			construct = trx.getTransaction().getObject(construct);

			// page with template
			page = ContentNodeTestDataUtils.createTemplateAndPage(node.getFolder(), "Testpage");

			// let template render construct
			Template template = trx.getTransaction().getObject(page.getTemplate(), true);
			template.setSource("<node " + tagName + ">");
			template.getTemplateTags().put(tagName, ContentNodeTestDataUtils.createTemplateTag(construct.getId(), tagName, false, false));
			template.save();

			// publish page
			page = trx.getTransaction().getObject(page, true);
			page.publish();
			page = trx.getTransaction().getObject(page);
			trx.success();
		}

		// publish into cr (creating the dependencies)
		// every attribute must be rendered for node and channel
		try (Trx trx = new Trx(); AutoCloseable asserter = CountHelperSource.asserter(tagName, 2)) {
			testContext.publish(false);
			trx.success();
		}

		// get the dependencies
		List<Dependency> expectedDependencies = null;
		try (Trx trx = new Trx()) {
			expectedDependencies = DependencyManager.getDependenciesForObject(page, null, null);
			trx.success();
		}

		// dirt name for page in node
		try (Trx trx = new Trx()) {
			PublishQueue.dirtObject(page, PublishQueue.Action.DEPENDENCY, node.getId(), "name");
			trx.success();
		}

		// publish into cr
		try (Trx trx = new Trx(); AutoCloseable asserter = CountHelperSource.asserter(tagName, feature ? 0 : 1)) {
			testContext.publish(false);
			trx.success();
		}

		// assert that dependencies did not change
		List<Dependency> actualDependencies = null;
		try (Trx trx = new Trx()) {
			actualDependencies = DependencyManager.getDependenciesForObject(page, null, null);
			trx.success();
		}

		assertThat(actualDependencies).as("Dependencies for " + page)
				.usingElementComparatorOnFields("id", "source", "dependent", "sourceProperty", "storedDepProperties", "channelIds")
				.containsOnlyElementsOf(expectedDependencies);

		// assert attributes in cr
		assertThat(node.getContentMap()).as("Node CR").contains(page).withAttribute("name", page.getName()).withAttribute("content", "value of tag: []");
		assertThat(channel.getContentMap()).as("Channel CR").contains(page).withAttribute("name", page.getName()).withAttribute("content", "value of tag: []");
	}

	@Test
	public void testDirtPageContent() throws Exception {
		String tagName = "tag";
		Node node = null;
		Node channel = null;
		Page page = null;
		Construct construct = null;

		// create test data
		try (Trx trx = new Trx()) {
			// node and channel with crs
			node = ContentNodeTestDataUtils.createNode("testhost", "TestNode", PublishTarget.CONTENTREPOSITORY);
			channel = ContentNodeTestDataUtils.createChannel(node, "Testchannel", "testchannel", "/", PublishTarget.CONTENTREPOSITORY);

			// construct that counts when rendered
			int hbsConstructId = ContentNodeTestDataUtils.createConstruct(node, HandlebarsPartType.class, "hbs", "hbs");
			construct = trx.getTransaction().getObject(Construct.class, hbsConstructId, true);
			construct.getParts().stream().filter(part -> "hbs".equals(part.getKeyname())).forEach(part -> {
				try {
					part.getDefaultValue().setValueText(HBS);
				} catch (Exception e) {
					e.printStackTrace();
					fail("Setup of construct failed");
				}
			});
			construct.save();
			construct = trx.getTransaction().getObject(construct);

			// page with template
			page = ContentNodeTestDataUtils.createTemplateAndPage(node.getFolder(), "Testpage");

			// let template render construct
			Template template = trx.getTransaction().getObject(page.getTemplate(), true);
			template.setSource("<node " + tagName + ">");
			template.getTemplateTags().put(tagName, ContentNodeTestDataUtils.createTemplateTag(construct.getId(), tagName, false, false));
			template.save();

			// publish page
			page = trx.getTransaction().getObject(page, true);
			page.publish();
			page = trx.getTransaction().getObject(page);
			trx.success();
		}

		// publish into cr (creating the dependencies)
		CountHelperSource.reset();
		CountHelperSource.render("first published content");
		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		// assert correct rendering
		assertThat(node.getContentMap()).contains(page).withAttribute("content", "value of tag: [first published content]")
				.withAttribute("name", page.getName());
		assertThat(channel.getContentMap()).contains(page).withAttribute("content", "value of tag: [first published content]")
				.withAttribute("name", page.getName());

		// dirt content of page for node
		try (Trx trx = new Trx()) {
			PublishQueue.dirtObject(page, PublishQueue.Action.DEPENDENCY, node.getId(), "content");
			trx.success();
		}

		// publish into cr
		CountHelperSource.reset();
		CountHelperSource.render("second published content");
		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		// assert
		assertThat(CountHelperSource.get(tagName)).as("Count for " + tagName).isEqualTo(1);

		// assert correct rendering
		assertThat(node.getContentMap()).contains(page).withAttribute("content", "value of tag: [second published content]")
				.withAttribute("name", page.getName());
		assertThat(channel.getContentMap()).contains(page).withAttribute("content", "value of tag: [first published content]")
				.withAttribute("name", page.getName());
	}

	@Test
	public void testDirtFileBinaryContent() throws Exception {
		Node node = null;
		Node channel = null;
		File file = null;

		// create test data
		try (Trx trx = new Trx()) {
			// node and channel with crs
			node = ContentNodeTestDataUtils.createNode("testhost", "TestNode", PublishTarget.CONTENTREPOSITORY);
			channel = ContentNodeTestDataUtils.createChannel(node, "Testchannel", "testchannel", "/", PublishTarget.CONTENTREPOSITORY);

			file = ContentNodeTestDataUtils.createFile(node.getFolder(), "testfile.txt", FILE_CONTENTS);
			trx.success();
		}

		// publish into cr (creating the dependencies)
		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		// get the dependencies
		List<Dependency> expectedDependencies = null;
		try (Trx trx = new Trx()) {
			expectedDependencies = DependencyManager.getDependenciesForObject(file, null, null);
			trx.success();
		}

		// change binary content of file
		try (Trx trx = new Trx()) {
			file = trx.getTransaction().getObject(file, true);
			file.setFileStream(new ByteArrayInputStream(CHANGED_FILE_CONTENTS));
			file.save();
			trx.success();
		}

		// publish into cr
		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		// assert that dependencies did not change
		List<Dependency> actualDependencies = null;
		try (Trx trx = new Trx()) {
			actualDependencies = DependencyManager.getDependenciesForObject(file, null, null);
			trx.success();
		}

		assertThat(actualDependencies).as("Dependencies for " + file)
				.usingElementComparatorOnFields("id", "source", "dependent", "sourceProperty", "storedDepProperties", "channelIds")
				.containsOnlyElementsOf(expectedDependencies);

		// assert attributes in cr
		assertThat(node.getContentMap()).as("Node CR").contains(file).withAttribute("name", file.getFilename())
				.withAttribute("binarycontent", CHANGED_FILE_CONTENTS);
		assertThat(channel.getContentMap()).as("Channel CR").contains(file).withAttribute("name", file.getFilename())
				.withAttribute("binarycontent", CHANGED_FILE_CONTENTS);
	}

	@Test
	public void testPagePublishHandler() throws Exception {
		Node node = null;
		Node channel = null;
		Page page = null;

		// create test data
		try (Trx trx = new Trx()) {
			// node and channel with crs
			node = ContentNodeTestDataUtils.createNode("testhost", "TestNode", PublishTarget.CONTENTREPOSITORY);
			channel = ContentNodeTestDataUtils.createChannel(node, "Testchannel", "testchannel", "/", PublishTarget.CONTENTREPOSITORY);

			// add publish handlers
			ContentNodeTestDataUtils.addPublishHandler(node.getContentRepository(), AttributeCollectingPublishHandler.class);
			ContentNodeTestDataUtils.addPublishHandler(channel.getContentRepository(), AttributeCollectingPublishHandler.class);

			page = ContentNodeTestDataUtils.createTemplateAndPage(node.getFolder(), "Testpage");
			Template template = trx.getTransaction().getObject(page.getTemplate(), true);
			template.setSource(TEMPLATE);
			template.save();

			page = trx.getTransaction().getObject(page, true);
			page.publish();
			page = trx.getTransaction().getObject(page);
			trx.success();
		}

		// publish into cr (creating the dependencies)
		AttributeCollectingPublishHandler.reset();
		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		// assert
		assertThat(AttributeCollectingPublishHandler.get(page)).as("Attributes of " + page + " when creating").containsOnly("name", "content", "node_id",
				"publishtimestamp");

		// dirt name for page in node
		try (Trx trx = new Trx()) {
			PublishQueue.dirtObject(page, PublishQueue.Action.DEPENDENCY, node.getId(), "name");
			trx.success();
		}

		// publish into cr
		AttributeCollectingPublishHandler.reset();
		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		// name must have been available, but not content
		assertThat(AttributeCollectingPublishHandler.get(page)).as("Attributes of " + page + " when updating").containsOnly("name");
	}

	@Test
	public void testFilePublishHandler() throws Exception {
		Node node = null;
		Node channel = null;
		File file = null;

		// create test data
		try (Trx trx = new Trx()) {
			// node and channel with crs
			node = ContentNodeTestDataUtils.createNode("testhost", "TestNode", PublishTarget.CONTENTREPOSITORY);
			channel = ContentNodeTestDataUtils.createChannel(node, "Testchannel", "testchannel", "/", PublishTarget.CONTENTREPOSITORY);

			// add publish handlers
			ContentNodeTestDataUtils.addPublishHandler(node.getContentRepository(), AttributeCollectingPublishHandler.class);
			ContentNodeTestDataUtils.addPublishHandler(channel.getContentRepository(), AttributeCollectingPublishHandler.class);

			file = ContentNodeTestDataUtils.createFile(node.getFolder(), "testfile.txt", FILE_CONTENTS);
			trx.success();
		}

		// publish into cr (creating the dependencies)
		AttributeCollectingPublishHandler.reset();
		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		// assert
		assertThat(AttributeCollectingPublishHandler.get(file)).as("Attributes of " + file + " when creating").containsOnly("name", "binarycontent", "node_id");

		// dirt name for file in node
		try (Trx trx = new Trx()) {
			PublishQueue.dirtObject(file, PublishQueue.Action.DEPENDENCY, node.getId(), "name");
			trx.success();
		}

		// publish into cr
		AttributeCollectingPublishHandler.reset();
		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		// name must have been available, but not binarycontent
		assertThat(AttributeCollectingPublishHandler.get(file)).as("Attributes of " + file + " when updating").containsOnly("name");
	}

	@Test
	public void testFilePublishHandlerCache() throws Exception {
		Node node = null;
		File file = null;

		// create test data
		try (Trx trx = new Trx()) {
			// node and channel with crs
			node = ContentNodeTestDataUtils.createNode("testhost", "TestNode MCCR", PublishTarget.CONTENTREPOSITORY, true, false);
			ContentNodeTestDataUtils.createChannel(node, "Testchannel MCCR", "testchannel", "/", PublishTarget.CONTENTREPOSITORY);

			// set basepath
			ContentNodeTestDataUtils.update(node.getContentRepository(), cr -> cr.setBasepath(System.getProperty("java.io.tmpdir")));

			// add non optimized tagmap entry
			ContentNodeTestDataUtils.addTagmapEntry(node.getContentRepository(), File.TYPE_FILE, GenticsContentAttribute.ATTR_TYPE_TEXT, "file.name", "test",
					null, false, false, false, 0, null, null);
			// add non optimized filesystem tagmap entry
			ContentNodeTestDataUtils.addTagmapEntry(node.getContentRepository(), File.TYPE_FILE, GenticsContentAttribute.ATTR_TYPE_TEXT_LONG, "file.name", "testfs",
					null, false, true, false, 0, null, null);

			// add publish handlers
			ContentNodeTestDataUtils.addPublishHandler(node.getContentRepository(), CacheTestingPublishHandler.class);

			file = ContentNodeTestDataUtils.createFile(node.getFolder(), "testfile.txt", FILE_CONTENTS);
			trx.success();
		}

		// publish into cr (creating the dependencies)
		CacheTestingPublishHandler.reset();
		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		// dirt name for file in node
		try (Trx trx = new Trx()) {
			PublishQueue.dirtObject(file, PublishQueue.Action.DEPENDENCY, node.getId(), "name");
			trx.success();
		}

		// publish into cr
		CacheTestingPublishHandler.reset();
		CacheTestingPublishHandler.set("name", "binarycontent", "node_id", "test", "testfs");
		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		// no additional SQL Statements allowed
		assertThat(CacheTestingPublishHandler.get().getCount()).as("SQL Count (" + CacheTestingPublishHandler.get().getLoggedStatements() + ")").isEqualTo(0);
	}

	@Test
	public void testFolderPublishHandler() throws Exception {
		Node node = null;
		Node channel = null;
		Folder folder = null;

		// create test data
		try (Trx trx = new Trx()) {
			// node and channel with crs
			node = ContentNodeTestDataUtils.createNode("testhost", "TestNode", PublishTarget.CONTENTREPOSITORY);
			channel = ContentNodeTestDataUtils.createChannel(node, "Testchannel", "testchannel", "/", PublishTarget.CONTENTREPOSITORY);

			// add publish handlers
			ContentNodeTestDataUtils.addPublishHandler(node.getContentRepository(), AttributeCollectingPublishHandler.class);
			ContentNodeTestDataUtils.addPublishHandler(channel.getContentRepository(), AttributeCollectingPublishHandler.class);

			folder = ContentNodeTestDataUtils.createFolder(node.getFolder(), "Testfolder");
			trx.success();
		}

		// publish into cr (creating the dependencies)
		AttributeCollectingPublishHandler.reset();
		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		// assert
		assertThat(AttributeCollectingPublishHandler.get(folder)).as("Attributes of " + folder + " when creating").containsOnly("name", "node_id");

		// dirt name for file in node
		try (Trx trx = new Trx()) {
			PublishQueue.dirtObject(folder, PublishQueue.Action.DEPENDENCY, node.getId(), "name");
			trx.success();
		}

		// publish into cr
		AttributeCollectingPublishHandler.reset();
		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		// name must have been available, but not binarycontent
		assertThat(AttributeCollectingPublishHandler.get(folder)).as("Attributes of " + folder + " when updating").containsOnly("name");
	}

	@Test
	public void testFolderPublishHandlerCache() throws Exception {
		Node node = null;
		Folder folder = null;

		// create test data
		try (Trx trx = new Trx()) {
			// node and channel with crs
			node = ContentNodeTestDataUtils.createNode("testhost", "TestNode MCCR", PublishTarget.CONTENTREPOSITORY, true, false);
			ContentNodeTestDataUtils.createChannel(node, "Testchannel MCCR", "testchannel", "/", PublishTarget.CONTENTREPOSITORY);

			// set basepath
			ContentNodeTestDataUtils.update(node.getContentRepository(), cr -> cr.setBasepath(System.getProperty("java.io.tmpdir")));

			// add non optimized tagmap entry
			ContentNodeTestDataUtils.addTagmapEntry(node.getContentRepository(), Folder.TYPE_FOLDER, GenticsContentAttribute.ATTR_TYPE_TEXT, "folder.name",
					"test", null, false, false, false, 0, null, null);
			// add non optimized filesystem tagmap entry
			ContentNodeTestDataUtils.addTagmapEntry(node.getContentRepository(), Folder.TYPE_FOLDER, GenticsContentAttribute.ATTR_TYPE_TEXT_LONG, "folder.name", "testfs",
					null, false, true, false, 0, null, null);

			// add publish handlers
			ContentNodeTestDataUtils.addPublishHandler(node.getContentRepository(), CacheTestingPublishHandler.class);

			folder = ContentNodeTestDataUtils.createFolder(node.getFolder(), "Testfolder");
			trx.success();
		}

		// publish into cr (creating the dependencies)
		CacheTestingPublishHandler.reset();
		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		// dirt name for file in node
		try (Trx trx = new Trx()) {
			PublishQueue.dirtObject(folder, PublishQueue.Action.DEPENDENCY, node.getId(), "name");
			trx.success();
		}

		// publish into cr
		CacheTestingPublishHandler.reset();
		CacheTestingPublishHandler.set("name", "node_id", "test", "testfs");
		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		// no additional SQL Statements allowed
		assertThat(CacheTestingPublishHandler.get().getCount()).as("SQL Count (" + CacheTestingPublishHandler.get().getLoggedStatements() + ")").isEqualTo(0);
	}

	@Test
	public void testPublishChangedPubdirToFilesystem() throws Exception {
		Node node = null;
		Page page = null;

		// create test data
		try (Trx trx = new Trx()) {
			node = ContentNodeTestDataUtils.createNode("testhost", "TestNode", PublishTarget.FILESYSTEM);
			page = ContentNodeTestDataUtils.createTemplateAndPage(node.getFolder(), "TestPage");
			trx.getTransaction().getObject(page, true).publish();
			trx.success();
		}

		// publish
		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		// assert
		try (Trx trx = new Trx()) {
			ContentNodeTestUtils.assertPublishFS(testContext.getPubDir(), page, node, true);
			trx.success();
		}

		// change folder pubdir
		try (Trx trx = new Trx()) {
			Folder editable = trx.getTransaction().getObject(node.getFolder(), true);
			editable.setPublishDir("/modified");
			editable.save();
			trx.success();
		}

		// publish
		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		// assert
		try (Trx trx = new Trx()) {
			ContentNodeTestUtils.assertPublishFS(testContext.getPubDir(), page, node, true);
			trx.success();
		}
	}

	@Test
	public void testPublishMovedPageToFilesystem() throws Exception {
		Node node = null;
		Page page = null;
		Folder folder1 = null;
		Folder folder2 = null;

		// create test data
		try (Trx trx = new Trx()) {
			node = ContentNodeTestDataUtils.createNode("testhost", "TestNode", PublishTarget.FILESYSTEM);
			int rootFolderId = node.getFolder().getId();
			folder1 = ContentNodeTestDataUtils.create(Folder.class, f -> {
				f.setMotherId(rootFolderId);
				f.setName("Folder1");
				f.setPublishDir("/folder1");
			});
			folder2 = ContentNodeTestDataUtils.create(Folder.class, f -> {
				f.setMotherId(rootFolderId);
				f.setName("Folder2");
				f.setPublishDir("/folder2");
			});

			page = ContentNodeTestDataUtils.createTemplateAndPage(folder1, "TestPage");
			trx.getTransaction().getObject(page, true).publish();

			trx.success();
		}

		// publish
		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		// assert
		try (Trx trx = new Trx()) {
			ContentNodeTestUtils.assertPublishFS(testContext.getPubDir(), page, node, true);
			trx.success();
		}

		// move page
		try (Trx trx = new Trx()) {
			assertThat(page.move(folder2, 0, false).isOK()).isTrue();
			page = trx.getTransaction().getObject(page);
			trx.success();
		}

		// publish
		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		// assert
		try (Trx trx = new Trx()) {
			ContentNodeTestUtils.assertPublishFS(testContext.getPubDir(), page, node, true);
			trx.success();
		}
	}
}
