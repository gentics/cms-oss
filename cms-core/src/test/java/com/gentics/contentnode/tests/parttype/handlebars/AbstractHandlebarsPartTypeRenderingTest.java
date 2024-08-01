package com.gentics.contentnode.tests.parttype.handlebars;

import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.utils.Builder.create;
import static com.gentics.contentnode.tests.utils.Builder.update;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.NODE_GROUP_ID;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getLanguage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartTypeId;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.BeforeClass;
import org.junit.ClassRule;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.ContentNodeDate;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.Datasource;
import com.gentics.contentnode.object.Datasource.SourceType;
import com.gentics.contentnode.object.DatasourceEntry;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.Overview;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.object.parttype.CheckboxPartType;
import com.gentics.contentnode.object.parttype.DatasourcePartType;
import com.gentics.contentnode.object.parttype.FileURLPartType;
import com.gentics.contentnode.object.parttype.FolderURLPartType;
import com.gentics.contentnode.object.parttype.HTMLPartType;
import com.gentics.contentnode.object.parttype.ImageURLPartType;
import com.gentics.contentnode.object.parttype.MultiSelectPartType;
import com.gentics.contentnode.object.parttype.NodePartType;
import com.gentics.contentnode.object.parttype.OverviewPartType;
import com.gentics.contentnode.object.parttype.PageURLPartType;
import com.gentics.contentnode.object.parttype.SingleSelectPartType;
import com.gentics.contentnode.object.parttype.handlebars.HandlebarsPartType;
import com.gentics.contentnode.tests.utils.Builder;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.testutils.GenericTestUtils;

/**
 * Abstract base class for handlebars rendering tests
 */
public abstract class AbstractHandlebarsPartTypeRenderingTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	final static int creationTimestamp = (int) (System.currentTimeMillis() / 1000) - 2 * 86400;

	final static int editTimestamp = (int) (System.currentTimeMillis() / 1000) - 86400;

	final static int publishTimestamp = (int) (System.currentTimeMillis() / 1000);

	final static String creationdate = new ContentNodeDate(creationTimestamp).toString();

	final static String editdate = new ContentNodeDate(editTimestamp).toString();

	final static String publishdate = new ContentNodeDate(publishTimestamp).toString();

	static SystemUser creator;

	static SystemUser editor;

	static SystemUser publisher;

	static Node node;

	static Folder rootFolder;

	static Folder homeFolder;

	static Folder testFolder;

	static Folder subFolder;

	static File testFile;

	static ImageFile testImage;

	static Construct handlebarsConstruct;

	static Template template;

	static Page testPage;

	static Page englishPage;

	static Construct testConstruct;

	static Construct overviewConstruct;

	static Construct datasourceConstruct;

	static Datasource datasource;

	static Construct singleSelectConstruct;

	static Construct multiSelectConstruct;

	static Construct urlsConstruct;

	static Construct checkboxConstruct;

	static Construct nodeConstruct;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		ContentLanguage german = getLanguage("de");
		ContentLanguage english = getLanguage("en");

		UserGroup nodeGroup = supply(t -> t.getObject(UserGroup.class, NODE_GROUP_ID));

		creator = create(SystemUser.class, u -> {
			u.setActive(true);
			u.setLogin("creatoruser");
			u.setFirstname("Creator-First");
			u.setLastname("Creator-Last");
			u.setDescription("This is the creator user");
			u.getUserGroups().add(nodeGroup);
		}).at(creationTimestamp).build();

		creator = Builder.update(creator, c -> {
			c.setEmail("creator@no.where");
		}).at(editTimestamp).build();

		editor = create(SystemUser.class, u -> {
			u.setActive(true);
			u.setLogin("editoruser");
			u.setFirstname("Editor-First");
			u.setLastname("Editor-Last");
			u.setDescription("This is the editor user");
			u.getUserGroups().add(nodeGroup);
		}).at(creationTimestamp).build();

		editor = Builder.update(editor, c -> {
			c.setEmail("editor@no.where");
		}).at(editTimestamp).build();

		publisher = create(SystemUser.class, u -> {
			u.setActive(true);
			u.setLogin("publisheruser");
			u.setFirstname("Publisher-First");
			u.setLastname("Publisher-Last");
			u.setDescription("This is the publiser user");
			u.getUserGroups().add(nodeGroup);
		}).at(creationTimestamp).build();

		publisher = Builder.update(publisher, c -> {
			c.setEmail("publisher.where");
		}).at(editTimestamp).build();

		node = create(Node.class, n -> {
			Folder root = create(Folder.class, f -> {
				f.setName("Test Node");
				f.setPublishDir("/");
			}).doNotSave().build();
			n.setFolder(root);
			n.setHostname("test.node.hostname");
			n.setPublishDir("/node/pub/dir");
			n.setBinaryPublishDir("/node/pub/dir/bin");
			n.getLanguages().add(german);
			n.getLanguages().add(english);
		}).as(creator).build();

		rootFolder = execute(Node::getFolder, node);

		testConstruct = create(Construct.class, c -> {
			c.setAutoEnable(true);
			c.setKeyword("test_construct");
			c.setName("test_construct", 1);

			c.getParts().add(create(Part.class, p -> {
				p.setPartTypeId(getPartTypeId(HTMLPartType.class));
				p.setEditable(1);
				p.setHidden(false);
				p.setKeyname("html");
			}).doNotSave().build());
		}).build();

		create(ObjectTagDefinition.class, oe -> {
			oe.setTargetType(Folder.TYPE_FOLDER);
			oe.setName("First Object Property", 1);
			ObjectTag objectTag = oe.getObjectTag();

			objectTag.setConstructId(testConstruct.getConstructId());
			objectTag.setEnabled(true);
			objectTag.setName("object.firstfolder");
			objectTag.setObjType(Folder.TYPE_FOLDER);
		}).build();

		create(ObjectTagDefinition.class, oe -> {
			oe.setTargetType(Folder.TYPE_FOLDER);
			oe.setName("Second Object Property", 1);
			ObjectTag objectTag = oe.getObjectTag();

			objectTag.setConstructId(testConstruct.getConstructId());
			objectTag.setEnabled(true);
			objectTag.setName("object.secondfolder");
			objectTag.setObjType(Folder.TYPE_FOLDER);
		}).build();

		create(ObjectTagDefinition.class, oe -> {
			oe.setTargetType(Folder.TYPE_FOLDER);
			oe.setName("Common Object Property", 1);
			ObjectTag objectTag = oe.getObjectTag();

			objectTag.setConstructId(testConstruct.getConstructId());
			objectTag.setEnabled(true);
			objectTag.setName("object.common");
			objectTag.setObjType(Folder.TYPE_FOLDER);
		}).build();

		create(ObjectTagDefinition.class, oe -> {
			oe.setTargetType(Page.TYPE_PAGE);
			oe.setName("First Object Property", 1);
			ObjectTag objectTag = oe.getObjectTag();

			objectTag.setConstructId(testConstruct.getConstructId());
			objectTag.setEnabled(true);
			objectTag.setName("object.firstpage");
			objectTag.setObjType(Page.TYPE_PAGE);
		}).build();

		create(ObjectTagDefinition.class, oe -> {
			oe.setTargetType(Page.TYPE_PAGE);
			oe.setName("Second Object Property", 1);
			ObjectTag objectTag = oe.getObjectTag();

			objectTag.setConstructId(testConstruct.getConstructId());
			objectTag.setEnabled(true);
			objectTag.setName("object.secondpage");
			objectTag.setObjType(Page.TYPE_PAGE);
		}).build();

		create(ObjectTagDefinition.class, oe -> {
			oe.setTargetType(Page.TYPE_PAGE);
			oe.setName("Common Object Property", 1);
			ObjectTag objectTag = oe.getObjectTag();

			objectTag.setConstructId(testConstruct.getConstructId());
			objectTag.setEnabled(true);
			objectTag.setName("object.common");
			objectTag.setObjType(Page.TYPE_PAGE);
		}).build();

		create(ObjectTagDefinition.class, oe -> {
			oe.setTargetType(Template.TYPE_TEMPLATE);
			oe.setName("First Object Property", 1);
			ObjectTag objectTag = oe.getObjectTag();

			objectTag.setConstructId(testConstruct.getConstructId());
			objectTag.setEnabled(true);
			objectTag.setName("object.firsttemplate");
			objectTag.setObjType(Template.TYPE_TEMPLATE);
		}).build();

		create(ObjectTagDefinition.class, oe -> {
			oe.setTargetType(Template.TYPE_TEMPLATE);
			oe.setName("Second Object Property", 1);
			ObjectTag objectTag = oe.getObjectTag();

			objectTag.setConstructId(testConstruct.getConstructId());
			objectTag.setEnabled(true);
			objectTag.setName("object.secondtemplate");
			objectTag.setObjType(Template.TYPE_TEMPLATE);
		}).build();

		create(ObjectTagDefinition.class, oe -> {
			oe.setTargetType(Template.TYPE_TEMPLATE);
			oe.setName("Common Object Property", 1);
			ObjectTag objectTag = oe.getObjectTag();

			objectTag.setConstructId(testConstruct.getConstructId());
			objectTag.setEnabled(true);
			objectTag.setName("object.common");
			objectTag.setObjType(Template.TYPE_TEMPLATE);
		}).build();

		homeFolder = create(Folder.class, f -> {
			f.setMotherId(node.getFolder().getId());
			f.setName("Home");
			f.setPublishDir("/home");
		}).as(creator).build();

		testFolder = create(Folder.class, f -> {
			f.setMotherId(homeFolder.getId());
			f.setName("Testfolder");
			f.setPublishDir("/test");
		}).at(creationTimestamp).as(creator).build();

		testFolder = update(testFolder, f -> {
			f.setDescription("This is the Testfolder");

			getPartType(HTMLPartType.class, f.getObjectTag("firstfolder"), "html").setText("Contents of first from folder");
			f.getObjectTag("firstfolder").setEnabled(true);
			getPartType(HTMLPartType.class, f.getObjectTag("secondfolder"), "html").setText("Contents of second from folder");
			f.getObjectTag("secondfolder").setEnabled(true);
			getPartType(HTMLPartType.class, f.getObjectTag("common"), "html").setText("Contents of common from folder");
			f.getObjectTag("common").setEnabled(true);
		}).at(editTimestamp).as(editor).build();

		subFolder = create(Folder.class, f -> {
			f.setMotherId(testFolder.getId());
			f.setName("Subfolder");
		}).build();

		testFile = create(File.class, f -> {
			f.setFolderId(testFolder.getId());
			f.setName("testfile.txt");
			f.setFileStream(new ByteArrayInputStream("contents".getBytes()));
		}).at(creationTimestamp).as(creator).build();

		testFile = update(testFile, f -> {
			f.setDescription("This is the test file");
		}).at(editTimestamp).as(editor).build();

		testImage = create(ImageFile.class, f -> {
			f.setFolderId(testFolder.getId());
			f.setName("blume.jpg");
			f.setFileStream(GenericTestUtils.getPictureResource("blume.jpg"));
		}).at(creationTimestamp).as(creator).build();

		testImage = update(testImage, f -> {
			f.setDescription("This is the test image");
		}).at(editTimestamp).as(editor).build();

		handlebarsConstruct = create(Construct.class, c -> {
			c.setAutoEnable(true);
			c.setKeyword("construct_with_handlebars");
			c.setName("Construct with Handlebars", 1);

			c.getParts().add(create(Part.class, p -> {
				p.setPartTypeId(getPartTypeId(HandlebarsPartType.class));
				p.setEditable(1);
				p.setHidden(false);
				p.setKeyname("hb");
				p.setName("Handlebars", 1);
			}).doNotSave().build());
		}).build();

		overviewConstruct = create(Construct.class, c -> {
			c.setAutoEnable(true);
			c.setKeyword("overview_construct");
			c.setName("overview_construct", 1);

			c.getParts().add(create(Part.class, p -> {
				p.setPartTypeId(getPartTypeId(OverviewPartType.class));
				p.setEditable(1);
				p.setHidden(false);
				p.setKeyname("overview");
			}).doNotSave().build());
		}).build();

		datasourceConstruct = create(Construct.class, c -> {
			c.setAutoEnable(true);
			c.setKeyword("datasource_construct");
			c.setName("datasource_construct", 1);

			c.getParts().add(create(Part.class, p -> {
				p.setPartTypeId(getPartTypeId(DatasourcePartType.class));
				p.setEditable(1);
				p.setHidden(false);
				p.setKeyname("datasource");
			}).doNotSave().build());
		}).build();

		datasource = create(Datasource.class, d -> {
			d.setName("Colors");
			d.setSourceType(SourceType.staticDS);

			List<DatasourceEntry> entries = d.getEntries();
			entries.add(create(DatasourceEntry.class, ds -> {
				ds.setDsid(1);
				ds.setKey("red");
				ds.setValue("Rot");
			}).doNotSave().build());
			entries.add(create(DatasourceEntry.class, ds -> {
				ds.setDsid(2);
				ds.setKey("green");
				ds.setValue("Grün");
			}).doNotSave().build());
			entries.add(create(DatasourceEntry.class, ds -> {
				ds.setDsid(3);
				ds.setKey("blue");
				ds.setValue("Blau");
			}).doNotSave().build());
		}).build();

		singleSelectConstruct = create(Construct.class, c -> {
			c.setAutoEnable(true);
			c.setKeyword("single_select_construct");
			c.setName("single_select_construct", 1);

			c.getParts().add(create(Part.class, p -> {
				p.setPartTypeId(getPartTypeId(SingleSelectPartType.class));
				p.setEditable(1);
				p.setHidden(false);
				p.setKeyname("single");
				p.setInfoInt(datasource.getId());
			}).doNotSave().build());
		}).build();

		multiSelectConstruct = create(Construct.class, c -> {
			c.setAutoEnable(true);
			c.setKeyword("multi_select_construct");
			c.setName("multi_select_construct", 1);

			c.getParts().add(create(Part.class, p -> {
				p.setPartTypeId(getPartTypeId(MultiSelectPartType.class));
				p.setEditable(1);
				p.setHidden(false);
				p.setKeyname("multi");
				p.setInfoInt(datasource.getId());
			}).doNotSave().build());
		}).build();

		urlsConstruct = create(Construct.class, c -> {
			c.setAutoEnable(true);
			c.setKeyword("urls_construct");
			c.setName("urls_construct", 1);

			c.getParts().add(create(Part.class, p -> {
				p.setPartTypeId(getPartTypeId(PageURLPartType.class));
				p.setEditable(1);
				p.setHidden(false);
				p.setKeyname("page");
			}).doNotSave().build());
			c.getParts().add(create(Part.class, p -> {
				p.setPartTypeId(getPartTypeId(PageURLPartType.class));
				p.setEditable(1);
				p.setHidden(false);
				p.setKeyname("extpage");
			}).doNotSave().build());
			c.getParts().add(create(Part.class, p -> {
				p.setPartTypeId(getPartTypeId(FolderURLPartType.class));
				p.setEditable(1);
				p.setHidden(false);
				p.setKeyname("folder");
			}).doNotSave().build());
			c.getParts().add(create(Part.class, p -> {
				p.setPartTypeId(getPartTypeId(FileURLPartType.class));
				p.setEditable(1);
				p.setHidden(false);
				p.setKeyname("file");
			}).doNotSave().build());
			c.getParts().add(create(Part.class, p -> {
				p.setPartTypeId(getPartTypeId(ImageURLPartType.class));
				p.setEditable(1);
				p.setHidden(false);
				p.setKeyname("image");
			}).doNotSave().build());
		}).build();

		checkboxConstruct = create(Construct.class, c -> {
			c.setAutoEnable(true);
			c.setKeyword("checkbox_construct");
			c.setName("checkbox_construct", 1);

			c.getParts().add(create(Part.class, p -> {
				p.setPartTypeId(getPartTypeId(CheckboxPartType.class));
				p.setEditable(1);
				p.setHidden(false);
				p.setKeyname("check1");
			}).doNotSave().build());
			c.getParts().add(create(Part.class, p -> {
				p.setPartTypeId(getPartTypeId(CheckboxPartType.class));
				p.setEditable(1);
				p.setHidden(false);
				p.setKeyname("check2");
			}).doNotSave().build());
		}).build();

		nodeConstruct = create(Construct.class, c -> {
			c.setAutoEnable(true);
			c.setKeyword("node_construct");
			c.setName("node_construct", 1);

			c.getParts().add(create(Part.class, p -> {
				p.setPartTypeId(getPartTypeId(NodePartType.class));
				p.setEditable(1);
				p.setHidden(false);
				p.setKeyname("node");
			}).doNotSave().build());
		}).build();

		template = create(Template.class, t -> {
			t.setFolderId(node.getFolder().getId());
			t.setMlId(1);
			t.setName("Test Template");
			t.setSource("<node testtag>");

			t.getTemplateTags().put("testtag", create(TemplateTag.class, tag -> {
				tag.setConstructId(handlebarsConstruct.getId());
				tag.setEnabled(true);
				tag.setName("testtag");
				tag.setPublic(true);
			}).doNotSave().build());
		}).at(creationTimestamp).as(creator).unlock().build();

		template = update(template, t -> {
			t.setDescription("This is the template");

			getPartType(HTMLPartType.class, t.getObjectTag("firsttemplate"), "html").setText("Contents of first from template");
			t.getObjectTag("firsttemplate").setEnabled(true);
			getPartType(HTMLPartType.class, t.getObjectTag("secondtemplate"), "html").setText("Contents of second from template");
			t.getObjectTag("secondtemplate").setEnabled(true);
			getPartType(HTMLPartType.class, t.getObjectTag("common"), "html").setText("Contents of common from template");
			t.getObjectTag("common").setEnabled(true);

		}).at(editTimestamp).as(editor).unlock().build();

		testPage = create(Page.class, p -> {
			p.setFolder(node, testFolder);
			p.setTemplateId(template.getId());
			p.setName("Test Page");
			p.setPriority(42);
			p.setLanguage(german);
		}).at(creationTimestamp).as(creator).unlock().build();

		testPage = update(testPage, p -> {
			p.setDescription("This is the test page");

			getPartType(HTMLPartType.class, p.getObjectTag("firstpage"), "html").setText("Contents of first from page");
			p.getObjectTag("firstpage").setEnabled(true);
			getPartType(HTMLPartType.class, p.getObjectTag("secondpage"), "html").setText("Contents of second from page");
			p.getObjectTag("secondpage").setEnabled(true);
			getPartType(HTMLPartType.class, p.getObjectTag("common"), "html").setText("Contents of common from page");
			p.getObjectTag("common").setEnabled(true);

			// add an overview tag
			ContentTag overviewTag = p.getContent().addContentTag(overviewConstruct.getId());
			ContentNodeTestDataUtils.fillOverview(overviewTag, "overview", "<node name><br>", Folder.class,
					Overview.SELECTIONTYPE_SINGLE, 0, Overview.ORDER_NAME, Overview.ORDERWAY_ASC, false,
					Arrays.asList(rootFolder, homeFolder, testFolder, subFolder));

			// add a datasource tag
			ContentTag datasourceTag = p.getContent().addContentTag(datasourceConstruct.getId());
			List<DatasourceEntry> items = getPartType(DatasourcePartType.class, datasourceTag, "datasource").getItems();
			items.add(create(DatasourceEntry.class, ds -> {
				ds.setDsid(1);
				ds.setKey("one");
				ds.setValue("Eins");
			}).doNotSave().build());
			items.add(create(DatasourceEntry.class, ds -> {
				ds.setDsid(2);
				ds.setKey("two");
				ds.setValue("Zwei");
			}).doNotSave().build());
			items.add(create(DatasourceEntry.class, ds -> {
				ds.setDsid(3);
				ds.setKey("three");
				ds.setValue("Drei");
			}).doNotSave().build());

			// add single select tag
			ContentTag singleSelectTag = p.getContent().addContentTag(singleSelectConstruct.getId());
			getPartType(SingleSelectPartType.class, singleSelectTag, "single").setSelected(datasource.getEntries().get(1));

			// add multi select tag
			ContentTag multiSelectTag = p.getContent().addContentTag(multiSelectConstruct.getId());
			getPartType(MultiSelectPartType.class, multiSelectTag, "multi").setSelected(datasource.getEntries().get(0), datasource.getEntries().get(2));

			// add urls tag
			ContentTag urlsTag = p.getContent().addContentTag(urlsConstruct.getId());
			getPartType(PageURLPartType.class, urlsTag, "page").setTargetPage(testPage);
			getPartType(PageURLPartType.class, urlsTag, "page").setNode(node);
			getPartType(PageURLPartType.class, urlsTag, "extpage").setExternalTarget("https://www.gentics.com/");
			getPartType(FolderURLPartType.class, urlsTag, "folder").setTargetFolder(homeFolder);
			getPartType(FolderURLPartType.class, urlsTag, "folder").setNode(node);
			getPartType(FileURLPartType.class, urlsTag, "file").setTargetFile(testFile);
			getPartType(FileURLPartType.class, urlsTag, "file").setNode(node);
			getPartType(ImageURLPartType.class, urlsTag, "image").setTargetImage(testImage);
			getPartType(ImageURLPartType.class, urlsTag, "image").setNode(node);

			// add a checkbox tag
			ContentTag checkboxTag = p.getContent().addContentTag(checkboxConstruct.getId());
			getPartType(CheckboxPartType.class, checkboxTag, "check1").setChecked(true);
			getPartType(CheckboxPartType.class, checkboxTag, "check2").setChecked(false);

			// add a node tag
			ContentTag nodeTag = p.getContent().addContentTag(nodeConstruct.getId());
			getPartType(NodePartType.class, nodeTag, "node").setNode(node);
		}).at(editTimestamp).as(editor).unlock().build();

		englishPage = create(Page.class, p -> {
			p.setFolder(node, testFolder);
			p.setTemplateId(template.getId());
			p.setName("English Test Page");
			p.setPriority(42);
			p.setLanguage(english);
			p.setContentsetId(testPage.getContentsetId());
		}).at(creationTimestamp).as(creator).unlock().publish().build();
	}

	/**
	 * Get the generic test cases, which can be used both in handlebars templates and in js helpers.
	 * @return list of generic test cases
	 */
	public static List<Object[]> getGenericTestCases() {
		return Arrays.asList(
			// node properties
			new Object[] { "cms.node.https", "false", Arrays.asList(Pair.of("testPage", "node"), Pair.of("node", "https")) },
			new Object[] { "cms.node.host", "test.node.hostname", Arrays.asList(Pair.of("testPage", "node"), Pair.of("node", "host"))},
			new Object[] { "cms.node.folder.name", "Test Node", Arrays.asList(Pair.of("testPage", "node"), Pair.of("node", "folder"), Pair.of("rootFolder", "name")) },
			new Object[] { "cms.node.path", "/node/pub/dir", Arrays.asList(Pair.of("testPage", "node"), Pair.of("node", "path")) },
			new Object[] { "cms.node.pub_dir", "/node/pub/dir", Arrays.asList(Pair.of("testPage", "node"), Pair.of("node", "pub_dir")) },
			new Object[] { "cms.node.pub_dir_bin", "/node/pub/dir/bin", Arrays.asList(Pair.of("testPage", "node"), Pair.of("node", "pub_dir_bin")) },

			// folder properties
			new Object[] { "cms.folder.name", "Testfolder", Arrays.asList(Pair.of("testPage", "folder"), Pair.of("testFolder", "name")) },
			new Object[] { "cms.folder.description", "This is the Testfolder", Arrays.asList(Pair.of("testPage", "folder"), Pair.of("testFolder", "description"))},
			new Object[] { "cms.folder.node.host", "test.node.hostname", Arrays.asList(Pair.of("testPage", "folder"), Pair.of("testFolder", "node"), Pair.of("node", "host")) },
			new Object[] { "cms.folder.parent.name", "Home", Arrays.asList(Pair.of("testPage", "folder"), Pair.of("testFolder", "parent"), Pair.of("homeFolder", "name")) },
			new Object[] { "cms.folder.path", "/test/", Arrays.asList(Pair.of("testPage", "folder"), Pair.of("testFolder", "path")) },
			new Object[] { "cms.folder.creator.firstname", "Creator-First", Arrays.asList(Pair.of("testPage", "folder"), Pair.of("testFolder", "creator"), Pair.of("creator", "firstname")) },
			new Object[] { "cms.folder.creator.lastname", "Creator-Last", Arrays.asList(Pair.of("testPage", "folder"), Pair.of("testFolder", "creator"), Pair.of("creator", "lastname")) },
			new Object[] { "cms.folder.creator.login", "creatoruser", Arrays.asList(Pair.of("testPage", "folder"), Pair.of("testFolder", "creator"), Pair.of("creator", "login")) },
			new Object[] { "cms.folder.creator.email", "creator@no.where", Arrays.asList(Pair.of("testPage", "folder"), Pair.of("testFolder", "creator"), Pair.of("creator", "email")) },
			new Object[] { "cms.folder.creator.active", "1", Arrays.asList(Pair.of("testPage", "folder"), Pair.of("testFolder", "creator"), Pair.of("creator", "active")) },
			new Object[] { "cms.folder.creator.creationdate", creationdate, Arrays.asList(Pair.of("testPage", "folder"), Pair.of("testFolder", "creator"), Pair.of("creator", "creationdate")) },
			new Object[] { "cms.folder.creator.creationtimestamp", Integer.toString(creationTimestamp), Arrays.asList(Pair.of("testPage", "folder"), Pair.of("testFolder", "creator"), Pair.of("creator", "creationtimestamp")) },
			new Object[] { "cms.folder.creator.editdate", editdate, Arrays.asList(Pair.of("testPage", "folder"), Pair.of("testFolder", "creator"), Pair.of("creator", "editdate")) },
			new Object[] { "cms.folder.creator.edittimestamp", Integer.toString(editTimestamp), Arrays.asList(Pair.of("testPage", "folder"), Pair.of("testFolder", "creator"), Pair.of("creator", "edittimestamp")) },
			new Object[] { "cms.folder.creator.description", "This is the creator user", Arrays.asList(Pair.of("testPage", "folder"), Pair.of("testFolder", "creator"), Pair.of("creator", "description")) },
			new Object[] { "cms.folder.editor.firstname", "Editor-First", Arrays.asList(Pair.of("testPage", "folder"), Pair.of("testFolder", "editor"), Pair.of("editor", "firstname")) },
			new Object[] { "cms.folder.creationtimestamp", Integer.toString(creationTimestamp), Arrays.asList(Pair.of("testPage", "folder"), Pair.of("testFolder", "creationtimestamp")) },
			new Object[] { "cms.folder.creationdate", creationdate, Arrays.asList(Pair.of("testPage", "folder"), Pair.of("testFolder", "creationdate")) },
			new Object[] { "cms.folder.edittimestamp", Integer.toString(editTimestamp), Arrays.asList(Pair.of("testPage", "folder"), Pair.of("testFolder", "edittimestamp")) },
			new Object[] { "cms.folder.editdate", editdate, Arrays.asList(Pair.of("testPage", "folder"), Pair.of("testFolder", "editdate")) },
			new Object[] { "cms.folder.object.secondfolder.parts.html.text", "Contents of second from folder", null },
			new Object[] { "cms.folder.object.common.parts.html.text", "Contents of common from folder", null },
			new Object[] { "cms.folder.ismaster", "true", Arrays.asList(Pair.of("testPage", "folder"), Pair.of("testFolder", "ismaster")) },
			new Object[] { "cms.folder.inherited", "false", Arrays.asList(Pair.of("testPage", "folder"), Pair.of("testFolder", "inherited")) },

			// page properties
			new Object[] { "cms.page.url", "/node/pub/dir/test/Test-Page.de.html", null },
			new Object[] { "cms.page.name", "Test Page", null },
			new Object[] { "cms.page.filename", "Test-Page.de.html", null },
			new Object[] { "cms.page.description", "This is the test page", null },
			new Object[] { "cms.page.priority", "42", null },
			new Object[] { "cms.page.creator.firstname", "Creator-First", null },
			new Object[] { "cms.page.creationtimestamp", Integer.toString(creationTimestamp), null },
			new Object[] { "cms.page.creationdate", creationdate, null },
			new Object[] { "cms.page.editor.firstname", "Editor-First", null },
			new Object[] { "cms.page.edittimestamp", Integer.toString(editTimestamp), null },
			new Object[] { "cms.page.editdate", editdate, null },
			new Object[] { "cms.page.publisher.firstname", "Publisher-First", null },
			new Object[] { "cms.page.publishtimestamp", Integer.toString(publishTimestamp), null },
			new Object[] { "cms.page.publishdate", publishdate, null },
			new Object[] { "cms.page.language.code", "de", null },
			new Object[] { "cms.page.language.name", "Deutsch", null },
			new Object[] { "cms.page.languageset.pages.de.name", "Test Page", null },
			new Object[] { "cms.page.languageset.pages.en.name", "English Test Page", null },
			new Object[] { "cms.page.online", "true", null },
			new Object[] { "cms.page.version.number", "1.0", null },
			new Object[] { "cms.page.version.date", publishdate, null },
			new Object[] { "cms.page.version.editor.firstname", "Publisher-First", null },
			new Object[] { "cms.page.version.major", "true", null },
			new Object[] { "cms.page.object.firstfolder.parts.html.text", "Contents of first from folder", null },
			new Object[] { "cms.page.object.secondpage.parts.html.text", "Contents of second from page", null },
			new Object[] { "cms.page.object.firsttemplate.parts.html.text", "Contents of first from template", null },
			new Object[] { "cms.page.object.common.parts.html.text", "Contents of common from page", null },
			new Object[] { "cms.page.ismaster", "true", null },
			new Object[] { "cms.page.inherited", "false", null },

			// template properties
			new Object[] { "cms.page.template.name", "Test Template", null },
			new Object[] { "cms.page.template.ml.extension", "html", null },
			new Object[] { "cms.page.template.ml.name", "HTML", null },
			new Object[] { "cms.page.template.ml.contenttype", "text/html", null },
			new Object[] { "cms.page.template.ml.excludeFromPublishing", "false", null },
			new Object[] { "cms.page.template.object.firsttemplate.parts.html.text", "Contents of first from template", null },
			new Object[] { "cms.page.template.object.common.parts.html.text", "Contents of common from template", null }
		);
	}

	/**
	 * Assert that the rendered testPage has the expected dependencies
	 * @param expectedDependencies expected dependencies
	 * @throws NodeException
	 */
	protected void assertDependencies(List<Pair<String, String>> expectedDependencies) throws NodeException {
		if (CollectionUtils.isNotEmpty(expectedDependencies)) {
			operate(() -> {
				for (Pair<String, String> dep : expectedDependencies) {
					// the left part denotes the field name of the test class containing the object from which the test page depends
					String fieldName = dep.getLeft();
					// the right part denotes the property
					String property = dep.getRight();
					try {
						Field field = AbstractHandlebarsPartTypeRenderingTest.class.getDeclaredField(fieldName);
						NodeObject nodeObject = ObjectTransformer.get(NodeObject.class, field.get(null));
						assertThat(testPage).dependsOn(nodeObject, property, 0);
					} catch (Exception e) {
						throw new NodeException(e);
					}
				}
			});
		}
	}
}
