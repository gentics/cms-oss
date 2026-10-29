package com.gentics.contentnode.tests.devtools;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.devtools.DevToolTestUtils.getGlobalId;
import static com.gentics.contentnode.tests.devtools.DevToolTestUtils.getStructureFile;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFolder;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createObjectPropertyDefinition;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.fillOverview;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Arrays;

import jakarta.ws.rs.WebApplicationException;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.devtools.MainPackageSynchronizer;
import com.gentics.contentnode.devtools.PackageObject;
import com.gentics.contentnode.devtools.PackageSynchronizer;
import com.gentics.contentnode.devtools.Synchronizer;
import com.gentics.contentnode.devtools.TemplateSynchronizer;
import com.gentics.contentnode.devtools.model.DefaultValueModel;
import com.gentics.contentnode.devtools.model.OverviewItemModel;
import com.gentics.contentnode.devtools.model.OverviewModel;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Overview;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Tag;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.parttype.HTMLPartType;
import com.gentics.contentnode.object.parttype.OverviewPartType;
import com.gentics.contentnode.object.parttype.ShortTextPartType;
import com.gentics.contentnode.rest.model.Overview.ListType;
import com.gentics.contentnode.rest.model.Overview.OrderBy;
import com.gentics.contentnode.rest.model.Overview.OrderDirection;
import com.gentics.contentnode.rest.model.Overview.SelectType;
import com.gentics.contentnode.rest.resource.devtools.PackageResource;
import com.gentics.contentnode.rest.resource.impl.devtools.PackageResourceImpl;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.testutils.infrastructure.TestEnvironment;

/**
 * Special test cases for synchronizing templates
 *
 */
@GCNFeature(set = { Feature.DEVTOOLS, Feature.MULTICHANNELLING })
public class TemplateSyncTest {
	/**
	 * Name of the testpackage
	 */
	public final static String PACKAGE_NAME = "testpackage";

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	@Rule
	public PackageSynchronizerContext syncContext = new PackageSynchronizerContext();

	private static Integer overviewConstructId;

	protected static Node node;

	protected static Node channel;

	protected PackageSynchronizer pack;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		Transaction transaction = testContext.getContext().getTransaction();
		if (transaction != null) {
			transaction.commit();
		}

		node = Trx.supply(() -> ContentNodeTestDataUtils.createNode());
		channel = Trx.supply(() -> ContentNodeTestDataUtils.createChannel(node, "Channel", "channel", "/"));

		Integer constructId = Trx.supply(() -> createConstruct(node, HTMLPartType.class, "construct", "part"));
		Trx.supply(() -> createObjectPropertyDefinition(Template.TYPE_TEMPLATE, constructId, "Test OE", "test"));

		overviewConstructId = supply(() -> createConstruct(node, OverviewPartType.class, "overview", "ds"));
	}

	@Before
	public void setup() throws NodeException {
		Synchronizer.addPackage(PACKAGE_NAME);

		pack = Synchronizer.getPackage(PACKAGE_NAME);
		assertThat(pack).as("package synchronizer").isNotNull();
	}

	@After
	public void teardown() throws NodeException {
		Synchronizer.removePackage(PACKAGE_NAME);
	}

	/**
	 * Test creating a localized copy of a template that is part of a package
	 * @throws Exception
	 */
	@Test
	public void testLocalizeAdded() throws Exception {
		String templateName = TestEnvironment.getRandomHash(10);

		// create master template
		Template master = Trx.supply(() -> ContentNodeTestDataUtils.createTemplate(node.getFolder(), templateName));

		// add to package
		Trx.operate(() -> pack.synchronize(master, true));

		// localize template
		Template localized = Trx.execute(t -> ContentNodeTestDataUtils.localize(t, channel), master);

		// check that localized copy is also synchronized
		Trx.operate(() -> {
			File masterStructure = getStructureFile(pack.getPackagePath().toFile(), master);
			assertThat(masterStructure).as("Structure file of master").exists().isFile();
			assertThat(getGlobalId(masterStructure)).as("Sync'ed master globalId").isEqualTo(master.getGlobalId().toString());

			File localizedStructure = getStructureFile(pack.getPackagePath().toFile(), localized);
			assertThat(localizedStructure).as("Structure file of localized").exists().isFile().isNotEqualTo(masterStructure);
			assertThat(getGlobalId(localizedStructure)).as("Sync'ed localized globalId").isEqualTo(localized.getGlobalId().toString());
		});
	}

	/**
	 * Test adding a template that has a localized copy
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testAddWithLocalizedCopy() throws Exception {
		String templateName = TestEnvironment.getRandomHash(10);

		// create master and localized copy
		Template master = Trx.supply(() -> ContentNodeTestDataUtils.createTemplate(node.getFolder(), templateName));
		Template localized = Trx.execute(t -> ContentNodeTestDataUtils.localize(t, channel), master);

		// add master (using the REST API)
		try (Trx trx = new Trx()) {
			PackageResource resource = new PackageResourceImpl();
			resource.addTemplate(PACKAGE_NAME, master.getGlobalId().toString());
			trx.success();
		}
		Trx.consume(m -> assertThat(pack.getObjects(Template.class)).as("Templates in package").containsOnly(new PackageObject<>(m)), master);

		// check that localized copy is also synchronized
		Trx.operate(() -> {
			File masterStructure = getStructureFile(pack.getPackagePath().toFile(), master);
			assertThat(masterStructure).as("Structure file of master").exists().isFile();
			assertThat(getGlobalId(masterStructure)).as("Sync'ed master globalId").isEqualTo(master.getGlobalId().toString());

			File localizedStructure = getStructureFile(pack.getPackagePath().toFile(), localized);
			assertThat(localizedStructure).as("Structure file of localized").exists().isFile().isNotEqualTo(masterStructure);
			assertThat(getGlobalId(localizedStructure)).as("Sync'ed localized globalId").isEqualTo(localized.getGlobalId().toString());
		});
	}

	/**
	 * Test adding a localized copy of a template to a package
	 * @throws Exception
	 */
	@Test(expected = WebApplicationException.class)
	public void testAddLocalizedCopy() throws Exception {
		String templateName = TestEnvironment.getRandomHash(10);

		// create master and localized copy
		Template master = Trx.supply(() -> ContentNodeTestDataUtils.createTemplate(node.getFolder(), templateName));
		Template localized = Trx.execute(t -> ContentNodeTestDataUtils.localize(t, channel), master);

		// add master (using the REST API)
		try (Trx trx = new Trx()) {
			PackageResource resource = new PackageResourceImpl();
			resource.addTemplate(PACKAGE_NAME, localized.getGlobalId().toString());
			trx.success();
		}
	}

	/**
	 * Test adding a channel local template to a package
	 * @throws Exception
	 */
	@Test
	public void testAddLocalTemplate() throws Exception {
		String templateName = TestEnvironment.getRandomHash(10);

		// create master and localized copy
		Template localMaster = Trx.supply(() -> ContentNodeTestDataUtils.createTemplate(node.getFolder(), templateName, channel));

		// add master (using the REST API)
		try (Trx trx = new Trx()) {
			PackageResource resource = new PackageResourceImpl();
			resource.addTemplate(PACKAGE_NAME, localMaster.getGlobalId().toString());
			trx.success();
		}

		// check that local template is synchronized
		Trx.operate(() -> {
			File masterStructure = getStructureFile(pack.getPackagePath().toFile(), localMaster);
			assertThat(masterStructure).as("Structure file of master").exists().isFile();
			assertThat(getGlobalId(masterStructure)).as("Sync'ed master globalId").isEqualTo(localMaster.getGlobalId().toString());
		});
	}

	/**
	 * Test adding different templates with the same name into the same package
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testAddNameConflict() throws Exception {
		String folderName = TestEnvironment.getRandomHash(10);
		String templateName = TestEnvironment.getRandomHash(10);
		Folder folder = Trx.supply(() -> ContentNodeTestDataUtils.createFolder(node.getFolder(), folderName));

		// create two templates with the same name
		Template template1 = Trx.supply(() -> ContentNodeTestDataUtils.createTemplate(node.getFolder(), templateName));
		Template template2 = Trx.supply(() -> ContentNodeTestDataUtils.createTemplate(folder, templateName));

		Trx.operate(() -> assertThat(template1).as("Template 1").hasName(templateName));
		Trx.operate(() -> assertThat(template2).as("Template 2").hasName(templateName));

		Trx.operate(() -> {
			pack.synchronize(template1, true);
			try {
				pack.synchronize(template2, true);
				fail(String.format("Adding %s to the package was expected to fail, but succeeded", template2));
			} catch (NodeException e) {
				// this is expected behaviour
			}
		});

		Trx.operate(() -> assertThat(pack.getObjects(Template.class)).as("Templates in package").containsOnly(new PackageObject<>(template1)));
	}

	/**
	 * Test name conflict when template in the package is renamed
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testRenameNameConflict() throws Exception {
		String folderName = TestEnvironment.getRandomHash(10);
		String templateName1 = TestEnvironment.getRandomHash(10);
		String templateName2 = TestEnvironment.getRandomHash(10);
		Folder folder = Trx.supply(() -> ContentNodeTestDataUtils.createFolder(node.getFolder(), folderName));

		// create two templates with the different names
		Template template1 = Trx.supply(() -> ContentNodeTestDataUtils.createTemplate(node.getFolder(), templateName1));
		Template template2 = Trx.supply(() -> ContentNodeTestDataUtils.createTemplate(folder, templateName2));

		Trx.consume(t -> assertThat(t).as("Template 1").hasName(templateName1), template1);
		Trx.consume(t -> assertThat(t).as("Template 2").hasName(templateName2), template2);

		// add both templates to the package
		Trx.consume((t1, t2) -> {
			pack.synchronize(t1, true);
			pack.synchronize(t2, true);
		}, template1, template2);

		// both templates must be in the package now
		Trx.consume(
				(t1, t2) -> assertThat(pack.getObjects(Template.class)).as("Templates in package").containsOnly(new PackageObject<>(t1),
						new PackageObject<>(t2)), template1, template2);

		// rename one template so that they have identical names
		template2 = ContentNodeTestDataUtils.update(template2, t -> t.setName(templateName1));

		Trx.consume(t -> assertThat(t).as("Template 1").hasName(templateName1), template1);
		Trx.consume(t -> assertThat(t).as("Template 2").hasName(templateName1), template2);

		// still both templates must be in the package now
		Trx.consume(
				(t1, t2) -> assertThat(pack.getObjects(Template.class)).as("Templates in package").containsOnly(new PackageObject<>(t1),
						new PackageObject<>(t2)), template1, template2);

		// template2 must still be synchronized to the old path (with the old name)
		File packageDir = new File(syncContext.getPackagesRoot(), PACKAGE_NAME);
		assertThat(packageDir).as("package directory").exists().isDirectory();
		File templatesDir = new File(packageDir, MainPackageSynchronizer.TEMPLATES_DIR);
		assertThat(templatesDir).as("templates directory").exists().isDirectory();

		File template1Dir = new File(templatesDir, templateName1);
		assertThat(template1Dir).as(String.format("directory for %s", template1)).exists().isDirectory();
		assertThat(getGlobalId(template1Dir)).as(String.format("Global ID found for", templateName1)).isEqualTo(template1.getGlobalId().toString());

		File template2Dir = new File(templatesDir, templateName2);
		assertThat(template2Dir).as(String.format("directory for %s", template2)).exists().isDirectory();
		assertThat(getGlobalId(template2Dir)).as(String.format("Global ID found for", templateName2)).isEqualTo(template2.getGlobalId().toString());
	}

	/**
	 * Test sync when object property is changed
	 * @throws NodeException
	 */
	@Test
	public void testChangeObjectProperty() throws Exception {
		String templateName = TestEnvironment.getRandomHash(10);

		// create template
		Template template = Trx.supply(() -> ContentNodeTestDataUtils.createTemplate(node.getFolder(), templateName));

		// add to package
		Trx.operate(() -> pack.synchronize(template, true));

		File packageDir = new File(syncContext.getPackagesRoot(), PACKAGE_NAME);
		File templatesDir = new File(packageDir, MainPackageSynchronizer.TEMPLATES_DIR);
		File templateDir = new File(templatesDir, template.getName());
		File objectPropertiesDir = new File(templateDir, TemplateSynchronizer.OBJECTTAGS_DIR);
		File objectPropDir = new File(objectPropertiesDir, "object.test");
		File partFile = new File(objectPropDir, "part.part.html");
		assertThat(partFile).as("Part File").exists().isFile().hasContent("");

		// change object property
		Trx.operate(() -> update(template.getObjectTag("test"), tag -> {
			tag.getValues().getByKeyname("part").setValueText("bla");
		}));

		assertThat(partFile).as("Part File").exists().isFile().hasContent("bla");
	}

	/**
	 * Test that template is unlocked when synchronization from FS fails
	 * @throws NodeException
	 */
	@Test
	public void testSyncError() throws NodeException {
		Synchronizer.disable();

		// create construct
		Construct construct = supply(t -> t.getObject(Construct.class, createConstruct(node, ShortTextPartType.class, "short", "short")));
		final int constructId = execute(Construct::getId, construct);

		// create template using the construct
		Template template = supply(() -> create(Template.class, t -> {
			t.setName("Template");
			t.setSource("");
			t.addFolder(node.getFolder());
			t.getTemplateTags().put("tag", create(TemplateTag.class, tag -> {
				tag.setConstructId(constructId);
				tag.setName("tag");
			}, false));
		}));
		consume(Template::unlock, template);

		assertThat(execute(Template::isLocked, template)).as("Template is locked").isFalse();

		consume(t -> pack.synchronize(t, true), template);

		// delete the templatetag
		consume(t -> update(t, upd -> {
			upd.getTemplateTags().clear();
		}), template);

		// delete the construct
		consume(Construct::delete, construct);

		// sync from FS (should fail)
		try {
			operate(() -> pack.syncAllFromFilesystem(Template.class));
			fail("Sync was expected to fail");
		} catch (NodeException expected) {
		}

		assertThat(execute(Template::isLocked, template)).as("Template is locked").isFalse();
		consume(t -> {
			int locked = DBUtils.select("SELECT locked FROM template WHERE id = ?", st -> {
				st.setInt(1, t.getId());
			}, rs -> {
				if (rs.next()) {
					return rs.getInt("locked");
				} else {
					return -1;
				}
			});

			assertThat(locked).as("Lock timestamp of template").isEqualTo(0);
		}, template);
	}

	/**
	 * Test syncing a channel local template from the filesystem.
	 * @throws Exception
	 */
	@Test
	public void testSyncLocalTemplateFromFilesystem() throws Exception {
		String templateName = TestEnvironment.getRandomHash(10);

		// create master and localized copy
		Template localMaster = Trx.supply(() -> ContentNodeTestDataUtils.createTemplate(node.getFolder(), templateName, channel));

		// add master (using the REST API)
		try (Trx trx = new Trx()) {
			PackageResource resource = new PackageResourceImpl();
			resource.addTemplate(PACKAGE_NAME, localMaster.getGlobalId().toString());
			trx.success();
		}

		// check that local template is synchronized
		try {
			operate(() -> pack.syncAllFromFilesystem(Template.class));
		} catch (NodeException e) {
			fail("Synchronization must succeed, but failed with: " + e.getMessage());
		}
	}

	@Test
	public void testSyncOverviewTag() throws Exception {
		String templateName = TestEnvironment.getRandomHash(10);
		String tagName = "overview";
		String globalRootFolderId = supply(() -> node.getFolder().getGlobalId().toString());

		Folder folder = supply(() -> createFolder(node.getFolder(), "Folder"));
		String globalFolderId = supply(() -> folder.getGlobalId().toString());

		Template template = supply(() -> create(Template.class, tmpl -> {
			tmpl.setFolderId(node.getFolder().getId());
			tmpl.setName(templateName);
			tmpl.setSource("");

			tmpl.getTags().put(tagName, create(TemplateTag.class, tag -> {
				tag.setConstructId(overviewConstructId);
				tag.setEnabled(true);
				tag.setName(tagName);
			}, false));
			fillOverview(tmpl.getTag(tagName), "ds", "<node page.name>", Page.class, Overview.SELECTIONTYPE_FOLDER, 3, Overview.ORDER_CDATE, Overview.ORDERWAY_ASC, true, Arrays.asList(node.getFolder()));
		}));

		// add to package
		consume(tmpl -> pack.synchronize(tmpl, true), template);

		// assert overview in package
		File packageDir = new File(syncContext.getPackagesRoot(), PACKAGE_NAME);
		File templatesDir = new File(packageDir, MainPackageSynchronizer.TEMPLATES_DIR);
		File templateDir = new File(templatesDir, template.getName());
		File tagsDir = new File(templateDir, TemplateSynchronizer.TEMPLATETAGS_DIR);
		File tagDir = new File(tagsDir, tagName);
		File partFile = new File(tagDir, "part.ds.json");
		assertThat(partFile).as("Part File").exists().isFile();

		ObjectMapper mapper = new ObjectMapper();
		DefaultValueModel value = mapper.readValue(partFile, DefaultValueModel.class);

		OverviewModel expected = new OverviewModel()
				.setSource("<node page.name>")
				.setListType(ListType.PAGE)
				.setSelectType(SelectType.FOLDER)
				.setMaxItems(3)
				.setOrderBy(OrderBy.CDATE)
				.setOrderDirection(OrderDirection.ASC)
				.setRecursive(true)
				.setSelection(Arrays.asList(new OverviewItemModel().setId(globalRootFolderId)));
		assertThat(value).as("Tag value").isNotNull();
		assertThat(value.getOverview()).as("Overview in tag value").isNotNull()
			.isEqualToComparingFieldByFieldRecursively(expected);

		// change overview in package
		value.getOverview()
			.setSource("<node folder.name>")
			.setListType(ListType.FOLDER)
			.setSelectType(SelectType.MANUAL)
			.setMaxItems(5)
			.setOrderBy(OrderBy.ALPHABETICALLY)
			.setOrderDirection(OrderDirection.DESC)
			.setRecursive(false)
			.setSelection(Arrays.asList(new OverviewItemModel().setId(globalFolderId), new OverviewItemModel().setId(globalRootFolderId)));

		mapper.writeValue(partFile, value);

		// sync from filesystem
		assertThat(supply(() -> pack.syncAllFromFilesystem(Template.class))).as("Synchronized templates").isEqualTo(1);

		// reload and assert
		template = execute(Template::reload, template);
		consume(tmpl -> {
			Tag tag = tmpl.getTag(tagName);
			Overview overview = getPartType(OverviewPartType.class, tag, "ds").getOverview();
			assertThat(overview).as("Overview").isNotNull()
				.hasFieldOrPropertyWithValue("template", "<node folder.name>")
				.hasFieldOrPropertyWithValue("objectClass", Folder.class)
				.hasFieldOrPropertyWithValue("selectionType", Overview.SELECTIONTYPE_SINGLE)
				.hasFieldOrPropertyWithValue("maxObjects", 5)
				.hasFieldOrPropertyWithValue("orderKind", Overview.ORDER_NAME)
				.hasFieldOrPropertyWithValue("orderWay", Overview.ORDERWAY_DESC);
			assertThat(overview.doRecursion()).isEqualTo(false);
			assertThat(overview.getOverviewEntries().stream().map(entry -> entry.getObjectId())).as("Overview entries").containsExactly(node.getFolder().getId(), folder.getId());
		}, template);
	}

	@Test
	public void testSyncEmptyOverview() throws Exception {
		String templateName = TestEnvironment.getRandomHash(10);
		String tagName = "overview";

		Template template = supply(() -> create(Template.class, tmpl -> {
			tmpl.setFolderId(node.getFolder().getId());
			tmpl.setName(templateName);
			tmpl.setSource("");

			tmpl.getTags().put(tagName, create(TemplateTag.class, tag -> {
				tag.setConstructId(overviewConstructId);
				tag.setEnabled(true);
				tag.setName(tagName);
			}, false));
		}));

		// add to package
		consume(tmpl -> pack.synchronize(tmpl, true), template);

		// assert overview in package
		File packageDir = new File(syncContext.getPackagesRoot(), PACKAGE_NAME);
		File templatesDir = new File(packageDir, MainPackageSynchronizer.TEMPLATES_DIR);
		File templateDir = new File(templatesDir, template.getName());
		File tagsDir = new File(templateDir, TemplateSynchronizer.TEMPLATETAGS_DIR);
		File tagDir = new File(tagsDir, tagName);
		File partFile = new File(tagDir, "part.ds.json");
		assertThat(partFile).as("Part File").exists().isFile();

		ObjectMapper mapper = new ObjectMapper();
		DefaultValueModel value = mapper.readValue(partFile, DefaultValueModel.class);

		OverviewModel expected = new OverviewModel();
		assertThat(value).as("Tag value").isNotNull();
		assertThat(value.getOverview()).as("Overview in tag value").isNotNull()
			.isEqualToComparingFieldByFieldRecursively(expected);

		// sync from filesystem
		assertThat(supply(() -> pack.syncAllFromFilesystem(Template.class))).as("Synchronized templates").isEqualTo(1);
	}
}
