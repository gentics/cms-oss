package com.gentics.contentnode.tests.publish.mesh;

import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.assertObject;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.cleanMesh;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.createMeshCR;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.addTagmapEntry;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.clear;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFile;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createPage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getLanguage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Consumer;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.I18nMap;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Node.UrlRenderWay;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.parttype.FileURLPartType;
import com.gentics.contentnode.object.parttype.LongHTMLPartType;
import com.gentics.contentnode.object.parttype.PageURLPartType;
import com.gentics.contentnode.publish.PublishInfo;
import com.gentics.contentnode.publish.mesh.MeshPublisher;
import com.gentics.contentnode.rest.model.PageLanguageCode;
import com.gentics.contentnode.tests.category.MeshTest;
import com.gentics.contentnode.tests.utils.Builder;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.contentnode.testutils.mesh.MeshContext;
import com.gentics.contentnode.testutils.mesh.MeshTestRule;
import com.gentics.lib.content.GenticsContentAttribute;
import com.gentics.mesh.assertj.MeshAssertions;
import com.gentics.mesh.core.rest.node.NodeResponse;

/**
 * Test cases for publishing translated folders to mesh
 */
@GCNFeature(set = { Feature.MESH_CONTENTREPOSITORY, Feature.INSTANT_CR_PUBLISHING, Feature.ATTRIBUTE_DIRTING, Feature.PUB_DIR_SEGMENT })
@RunWith(value = Parameterized.class)
@Category(MeshTest.class)
public class MeshPublishFolderTranslationTest {
	/**
	 * Name of the mesh project
	 */
	public final static String MESH_PROJECT_NAME = "testproject";

	@ClassRule
	public static DBTestContext context = new DBTestContext();

	@ClassRule
	public static MeshContext mesh = new MeshContext();

	private static Node node;

	private static Integer crId;

	private static ContentRepository cr;

	private static Template template;

	private static int htmlConstructId;

	private static int pageUrlConstructId;

	private static int fileUrlConstructId;

	@Rule
	public MeshTestRule meshTestRule = new MeshTestRule(mesh);

	@Parameter(0)
	public boolean instantPublishing;

	@Parameters(name = "{index}: instant {0}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (Boolean instant : Arrays.asList(true, false)) {
			data.add(new Object[] { instant });
		}
		return data;
	}

	/**
	 * Setup static test data
	 * 
	 * @throws NodeException
	 */
	@BeforeClass
	public static void setupOnce() throws Exception {
		context.getContext().getTransaction().commit();
		node = supply(() -> createNode("node", "Node", PublishTarget.CONTENTREPOSITORY, getLanguage("de"),
				getLanguage("en"), getLanguage("fr"), getLanguage("it"), getLanguage("es")));
		crId = createMeshCR(mesh, MESH_PROJECT_NAME);

		cr = supply(t -> t.getObject(ContentRepository.class, crId));

		operate(() -> {
			addTagmapEntry(cr, Page.TYPE_PAGE, GenticsContentAttribute.ATTR_TYPE_TEXT, "page.folder.name", "foldername",
					null, false, false, false, -1, null, null);
			addTagmapEntry(cr, Page.TYPE_PAGE, GenticsContentAttribute.ATTR_TYPE_TEXT, "page.folder.description", "folderdescription",
					null, false, false, false, -1, null, null);
			addTagmapEntry(cr, File.TYPE_FILE, GenticsContentAttribute.ATTR_TYPE_TEXT, "file.folder.name", "foldername",
					null, false, false, false, -1, null, null);
			addTagmapEntry(cr, File.TYPE_FILE, GenticsContentAttribute.ATTR_TYPE_TEXT, "file.folder.description", "folderdescription",
					null, false, false, false, -1, null, null);
		});

		node = update(node, n -> {
			n.setContentrepositoryId(crId);
			n.setPageLanguageCode(PageLanguageCode.PATH);
			n.setPublishDir("base");

			n.setUrlRenderWayFiles(UrlRenderWay.STATIC_WITHOUT_DOMAIN.getValue());
			n.setUrlRenderWayPages(UrlRenderWay.STATIC_WITHOUT_DOMAIN.getValue());
		});

		htmlConstructId = supply(() -> createConstruct(node, LongHTMLPartType.class, "text", "text"));
		fileUrlConstructId = supply(() -> createConstruct(node, FileURLPartType.class, "file", "file"));
		pageUrlConstructId = supply(() -> createConstruct(node, PageURLPartType.class, "page", "page"));

		template = Builder.create(Template.class, create -> {
			create.setName("Template");
			create.addFolder(node.getFolder());

			create.setSource("<node tag>");

			create.getTemplateTags().put("tag", Builder.create(TemplateTag.class, tag -> {
				tag.setConstructId(htmlConstructId);
				tag.setEnabled(true);
				tag.setName("tag");
				tag.setPublic(true);
			}).doNotSave().build());
		}).build();
	}

	@Before
	public void setup() throws NodeException {
		cr = update(cr, c -> {
			c.setInstantPublishing(instantPublishing);
		});
		operate(() -> clear(node));

		node = update(node, update -> {
			update.setPubDirSegment(false);
		});

		cleanMesh(mesh.client());
		operate(() -> assertThat(cr.checkStructure(true)).as("Structure valid").isTrue());

		for (String lang : Arrays.asList("de", "en", "fr", "it", "es")) {
			mesh.client().assignLanguageToProjectByTag(MESH_PROJECT_NAME, lang).blockingAwait();
		}
	}

	/**
	 * Test creating a folder without translations
	 * @throws Exception
	 */
	@Test
	public void testCreate() throws Exception {
		Folder folder = createFolder(false);

		if (!instantPublishing) {
			for (String language : Arrays.asList("de", "en", "fr", "it", "es")) {
				assertObject("Check created folder", mesh.client(), MESH_PROJECT_NAME, null, folder, language, false);
			}

			try (Trx trx = new Trx()) {
				context.publish(false);
				trx.success();
			}
		}

		assertObject("Check created folder", mesh.client(), MESH_PROJECT_NAME, null, folder, "de", false);
		assertObject("Check created folder", mesh.client(), MESH_PROJECT_NAME, null, folder, "en", true,
				doAssert("en", "Generic Name", "Generic Description", "/generic/path/"));
		assertObject("Check created folder", mesh.client(), MESH_PROJECT_NAME, null, folder, "fr", false);
		assertObject("Check created folder", mesh.client(), MESH_PROJECT_NAME, null, folder, "it", false);
		assertObject("Check created folder", mesh.client(), MESH_PROJECT_NAME, null, folder, "es", false);
	}

	/**
	 * Test creating a folder with translations
	 * @throws Exception
	 */
	@Test
	public void testCreateWithTranslations() throws Exception {
		Folder folder = createFolder(true);

		if (!instantPublishing) {
			for (String language : Arrays.asList("de", "en", "fr", "it", "es")) {
				assertObject("Check created folder", mesh.client(), MESH_PROJECT_NAME, null, folder, language, false);
			}

			try (Trx trx = new Trx()) {
				context.publish(false);
				trx.success();
			}
		}

		assertObject("Check created folder", mesh.client(), MESH_PROJECT_NAME, null, folder, "de", true,
				doAssert("de", "Name auf Deutsch", "Generic Description", "/generic/path/"));
		assertObject("Check created folder", mesh.client(), MESH_PROJECT_NAME, null, folder, "en", true,
				doAssert("en", "Name in English", "Description in English", "/generic/path/"));
		assertObject("Check created folder", mesh.client(), MESH_PROJECT_NAME, null, folder, "fr", true,
				doAssert("fr", "Generic Name", "Description en français", "/chemin/"));
		assertObject("Check created folder", mesh.client(), MESH_PROJECT_NAME, null, folder, "it", true,
				doAssert("it", "Generic Name", "Generic Description", "/sentiero/"));
		assertObject("Check created folder", mesh.client(), MESH_PROJECT_NAME, null, folder, "es", false);
	}

	/**
	 * Test updating generic data
	 * @throws Exception
	 */
	@Test
	public void testUpdateGenericData() throws Exception {
		Folder folder = createFolder(true);
		if (!instantPublishing) {
			try (Trx trx = new Trx()) {
				context.publish(false);
				trx.success();
			}
		}

		folder = update(folder, upd -> upd.setName("Updated Generic Name"));

		if (!instantPublishing) {
			try (Trx trx = new Trx()) {
				context.publish(false);
				trx.success();
			}
		}

		assertObject("Check created folder", mesh.client(), MESH_PROJECT_NAME, null, folder, "de", true,
				doAssert("de", "Name auf Deutsch", "Generic Description", "/generic/path/"));
		assertObject("Check created folder", mesh.client(), MESH_PROJECT_NAME, null, folder, "en", true,
				doAssert("en", "Name in English", "Description in English", "/generic/path/"));
		assertObject("Check created folder", mesh.client(), MESH_PROJECT_NAME, null, folder, "fr", true,
				doAssert("fr", "Updated Generic Name", "Description en français", "/chemin/"));
		assertObject("Check created folder", mesh.client(), MESH_PROJECT_NAME, null, folder, "it", true,
				doAssert("it", "Updated Generic Name", "Generic Description", "/sentiero/"));
		assertObject("Check created folder", mesh.client(), MESH_PROJECT_NAME, null, folder, "es", false);
	}

	/**
	 * Test updating translated data
	 * @throws Exception
	 */
	@Test
	public void testUpdateTranslatedData() throws Exception {
		Folder folder = createFolder(true);
		if (!instantPublishing) {
			try (Trx trx = new Trx()) {
				context.publish(false);
				trx.success();
			}
		}

		folder = update(folder, upd -> upd.setNameI18n(new I18nMap().put("de", "Geänderter Name auf Deutsch").put("en", "Name in English")));

		if (!instantPublishing) {
			try (Trx trx = new Trx()) {
				context.publish(false);
				trx.success();
			}
		}

		assertObject("Check created folder", mesh.client(), MESH_PROJECT_NAME, null, folder, "de", true,
				doAssert("de", "Geänderter Name auf Deutsch", "Generic Description", "/generic/path/"));
		assertObject("Check created folder", mesh.client(), MESH_PROJECT_NAME, null, folder, "en", true,
				doAssert("en", "Name in English", "Description in English", "/generic/path/"));
		assertObject("Check created folder", mesh.client(), MESH_PROJECT_NAME, null, folder, "fr", true,
				doAssert("fr", "Generic Name", "Description en français", "/chemin/"));
		assertObject("Check created folder", mesh.client(), MESH_PROJECT_NAME, null, folder, "it", true,
				doAssert("it", "Generic Name", "Generic Description", "/sentiero/"));
		assertObject("Check created folder", mesh.client(), MESH_PROJECT_NAME, null, folder, "es", false);
	}

	@Test
	public void testUpdateAddLanguage() throws Exception {
		Folder folder = createFolder(true);
		if (!instantPublishing) {
			try (Trx trx = new Trx()) {
				context.publish(false);
				trx.success();
			}
		}

		folder = update(folder, upd -> upd.setNameI18n(new I18nMap().put("de", "Name auf Deutsch")
				.put("en", "Name in English").put("es", "Nombre en español")));

		if (!instantPublishing) {
			try (Trx trx = new Trx()) {
				context.publish(false);
				trx.success();
			}
		}

		assertObject("Check created folder", mesh.client(), MESH_PROJECT_NAME, null, folder, "de", true,
				doAssert("de", "Name auf Deutsch", "Generic Description", "/generic/path/"));
		assertObject("Check created folder", mesh.client(), MESH_PROJECT_NAME, null, folder, "en", true,
				doAssert("en", "Name in English", "Description in English", "/generic/path/"));
		assertObject("Check created folder", mesh.client(), MESH_PROJECT_NAME, null, folder, "fr", true,
				doAssert("fr", "Generic Name", "Description en français", "/chemin/"));
		assertObject("Check created folder", mesh.client(), MESH_PROJECT_NAME, null, folder, "it", true,
				doAssert("it", "Generic Name", "Generic Description", "/sentiero/"));
		assertObject("Check created folder", mesh.client(), MESH_PROJECT_NAME, null, folder, "es", true,
				doAssert("es", "Nombre en español", "Generic Description", "/generic/path/"));
	}

	@Test
	public void testUpdateRemoveEnglishTranslations() throws Exception {
		Folder folder = createFolder(true);
		Page page = execute(f -> update(createPage(f, template, "Page"), Page::publish), folder);
		if (!instantPublishing) {
			try (Trx trx = new Trx()) {
				context.publish(false);
				trx.success();
			}
		}

		folder = update(folder, upd -> {
			upd.setNameI18n(new I18nMap().put("de", "Name auf Deutsch"));
			upd.setDescriptionI18n(new I18nMap().put("fr", "Description en français"));
		});

		if (!instantPublishing) {
			try (Trx trx = new Trx()) {
				context.publish(false);
				trx.success();
			}
		}

		assertObject("Check created folder", mesh.client(), MESH_PROJECT_NAME, null, folder, "de", true,
				doAssert("de", "Name auf Deutsch", "Generic Description", "/generic/path/"));
		assertObject("Check created folder", mesh.client(), MESH_PROJECT_NAME, null, folder, "en", true,
				doAssert("en", "Generic Name", "Generic Description", "/generic/path/"));
		assertObject("Check created folder", mesh.client(), MESH_PROJECT_NAME, null, folder, "fr", true,
				doAssert("fr", "Generic Name", "Description en français", "/chemin/"));
		assertObject("Check created folder", mesh.client(), MESH_PROJECT_NAME, null, folder, "it", true,
				doAssert("it", "Generic Name", "Generic Description", "/sentiero/"));
		assertObject("Check created folder", mesh.client(), MESH_PROJECT_NAME, null, folder, "es", false);
		assertObject("Check page", mesh.client(), MESH_PROJECT_NAME, page, true);
	}

	@Test
	public void testUpdateRemoveAdditionalTranslations() throws Exception {
		Folder folder = createFolder(true);
		Page page = execute(f -> update(createPage(f, template, "Page"), Page::publish), folder);
		if (!instantPublishing) {
			try (Trx trx = new Trx()) {
				context.publish(false);
				trx.success();
			}
		}

		folder = update(folder, upd -> {
			upd.setDescriptionI18n(new I18nMap().put("en", "Description in English"));
			upd.setPublishDirI18n(new I18nMap().put("it", "/sentiero/"));
		});

		if (!instantPublishing) {
			try (Trx trx = new Trx()) {
				context.publish(false);
				trx.success();
			}
		}

		assertObject("Check created folder", mesh.client(), MESH_PROJECT_NAME, null, folder, "de", true,
				doAssert("de", "Name auf Deutsch", "Generic Description", "/generic/path/"));
		assertObject("Check created folder", mesh.client(), MESH_PROJECT_NAME, null, folder, "en", true,
				doAssert("en", "Name in English", "Description in English", "/generic/path/"));
		assertObject("Check created folder", mesh.client(), MESH_PROJECT_NAME, null, folder, "fr", false);
		assertObject("Check created folder", mesh.client(), MESH_PROJECT_NAME, null, folder, "it", true,
				doAssert("it", "Generic Name", "Generic Description", "/sentiero/"));
		assertObject("Check created folder", mesh.client(), MESH_PROJECT_NAME, null, folder, "es", false);
		assertObject("Check page", mesh.client(), MESH_PROJECT_NAME, page, true);
	}

	@Test
	public void testRemoveTranslatedFolder() throws Exception {
		Folder folder = createFolder(true);
		Page page = execute(f -> update(createPage(f, template, "Page"), Page::publish), folder);
		if (!instantPublishing) {
			try (Trx trx = new Trx()) {
				context.publish(false);
				trx.success();
			}
		}

		update(folder, Folder::delete);

		if (!instantPublishing) {
			try (Trx trx = new Trx()) {
				context.publish(false);
				trx.success();
			}
		}

		assertObject("Check deleted folder", mesh.client(), MESH_PROJECT_NAME, folder, false);
		assertObject("Check deleted page", mesh.client(), MESH_PROJECT_NAME, page, false);
	}

	@Test
	public void testPageWithLanguage() throws Exception {
		Folder folder = createFolder(true);
		Page page = update(create(Page.class, p -> {
			p.setFolderId(folder.getId());
			p.setTemplateId(template.getId());
			p.setName("Page Name");
			p.setLanguage(getLanguage("fr"));
			p.setFilename("page.html");
		}), Page::publish);

		if (!instantPublishing) {
			assertObject("Page before publish process", mesh.client(), MESH_PROJECT_NAME, page, false);

			try (Trx trx = new Trx()) {
				context.publish(false);
				trx.success();
			}
		}

		assertObject("Check published page", mesh.client(), MESH_PROJECT_NAME, page, true, n -> {
			MeshAssertions.assertThat(n).as("Page").hasStringField(MeshPublisher.FIELD_GTX_URL, "/fr/chemin/page.html");
			MeshAssertions.assertThat(n).as("Page").hasStringField("foldername", "Generic Name");
			MeshAssertions.assertThat(n).as("Page").hasStringField("folderdescription", "Description en français");
		});
	}

	@Test
	public void testPageWithoutLanguage() throws Exception {
		Folder folder = createFolder(true);
		Page page = update(create(Page.class, p -> {
			p.setFolderId(folder.getId());
			p.setTemplateId(template.getId());
			p.setName("Page Name");
			p.setFilename("page.html");
		}), Page::publish);

		if (!instantPublishing) {
			assertObject("Page before publish process", mesh.client(), MESH_PROJECT_NAME, page, false);

			try (Trx trx = new Trx()) {
				context.publish(false);
				trx.success();
			}
		}

		assertObject("Check published page", mesh.client(), MESH_PROJECT_NAME, page, true, n -> {
			MeshAssertions.assertThat(n).as("Page").hasStringField(MeshPublisher.FIELD_GTX_URL, "/generic/path/page.html");
			MeshAssertions.assertThat(n).as("Page").hasStringField("foldername", "Generic Name");
			MeshAssertions.assertThat(n).as("Page").hasStringField("folderdescription", "Generic Description");
		});
	}

	@Test
	public void testFile() throws Exception {
		Folder folder = createFolder(true);
		File file = supply(() -> createFile(folder, "testfile.txt", "Contents".getBytes()));

		if (!instantPublishing) {
			assertObject("File before publish process", mesh.client(), MESH_PROJECT_NAME, file, false);

			try (Trx trx = new Trx()) {
				context.publish(false);
				trx.success();
			}
		}

		assertObject("Check published file", mesh.client(), MESH_PROJECT_NAME, file, true, n -> {
			MeshAssertions.assertThat(n).as("File").hasStringField(MeshPublisher.FIELD_GTX_URL, "/generic/path/testfile.txt");
			MeshAssertions.assertThat(n).as("File").hasStringField("foldername", "Generic Name");
			MeshAssertions.assertThat(n).as("File").hasStringField("folderdescription", "Generic Description");
		});
	}

	@Test
	public void testTranslateOnlyNameWithPubDirSegment() throws Exception {
		// acticate pub_dir_segments
		node = update(node, update -> {
			update.setPubDirSegment(true);
		});

		// create folder (no translations)
		Folder folder = create(Folder.class, f -> {
			f.setMotherId(node.getFolder().getId());
			f.setName("Generic Name");
			f.setPublishDir("path");
		});

		// run publish process (should succeed)
		try (Trx trx = new Trx()) {
			assertThat(context.getContext().publish(false).getReturnCode()).isEqualTo(PublishInfo.RETURN_CODE_SUCCESS);
			trx.success();
		}

		assertObject("Check published folder in english", mesh.client(), MESH_PROJECT_NAME, null, folder, "en", true, n -> {
			MeshAssertions.assertThat(n).as("Folder").hasStringField("name", "Generic Name");
			MeshAssertions.assertThat(n).as("Folder").hasStringField("pub_dir", "path");
		});
		assertObject("Check published folder in german", mesh.client(), MESH_PROJECT_NAME, null, folder, "de", false);

		// update folder (translate name to german)
		folder = update(folder, f -> {
			f.setNameI18n(new I18nMap().put("de", "Name auf Deutsch"));
		});

		// run publish process
		PublishInfo info = null;
		try (Trx trx = new Trx()) {
			info = context.getContext().publish(false);
			trx.success();
		}

		assertObject("Check published folder in english", mesh.client(), MESH_PROJECT_NAME, null, folder, "en", true, n -> {
			MeshAssertions.assertThat(n).as("Folder").hasStringField("name", "Generic Name");
			MeshAssertions.assertThat(n).as("Folder").hasStringField("pub_dir", "path");
		});
		assertObject("Check published folder in german", mesh.client(), MESH_PROJECT_NAME, null, folder, "de", false);

		// publish process should have failed
		assertThat(info.getReturnCode()).as("Publish return code").isEqualTo(PublishInfo.RETURN_CODE_ERROR);
	}

	@Test
	public void testRenderPageUrl() throws Exception {
		Folder folder = Builder.create(Folder.class, f -> {
			f.setMotherId(node.getFolder().getId());
			f.setName("Testfolder");
			f.setPublishDir("/generic/path");

			f.setPublishDirI18n(new I18nMap().put("de", "/german/path").put("en", "/english/path"));
		}).build();

		// create the target page in english
		Page targetPage = Builder.create(Page.class, create -> {
			create.setFolderId(folder.getId());
			create.setTemplateId(template.getId());
			create.setName("Target");
			create.setFilename("target.html");
			create.setLanguage(getLanguage("en"));
		}).publish().build();

		// create a page in german, which renders the page URL
		Page german = Builder.create(Page.class, create -> {
			create.setFolderId(folder.getId());
			create.setTemplateId(template.getId());
			create.setName("German");
			create.setLanguage(getLanguage("de"));

			ContentTag pageTag = create.getContent().addContentTag(pageUrlConstructId);
			getPartType(PageURLPartType.class, pageTag, "page").setTargetPage(targetPage);
			getPartType(LongHTMLPartType.class, create.getContentTag("tag"), "text")
					.setText("<node " + pageTag.getName() + ">");
		}).publish().build();

		// create a page in enlish, which renders the page URL
		Page english = Builder.create(Page.class, create -> {
			create.setFolderId(folder.getId());
			create.setTemplateId(template.getId());
			create.setName("English");
			create.setLanguage(getLanguage("en"));

			ContentTag pageTag = create.getContent().addContentTag(pageUrlConstructId);
			getPartType(PageURLPartType.class, pageTag, "page").setTargetPage(targetPage);
			getPartType(LongHTMLPartType.class, create.getContentTag("tag"), "text")
					.setText("<node " + pageTag.getName() + ">");
		}).publish().build();

		if (!instantPublishing) {
			try (Trx trx = new Trx()) {
				context.publish(false);
				trx.success();
			}
		}

		String expectedPageUrl = "/en/english/path/target.html";
		assertObject("Check published target page", mesh.client(), MESH_PROJECT_NAME, targetPage, true, node -> {
			// assert url
			MeshAssertions.assertThat(node).hasStringField(MeshPublisher.FIELD_GTX_URL, expectedPageUrl);
		});
		assertObject("Check german page", mesh.client(), MESH_PROJECT_NAME, german, true, node -> {
			// assert content
			MeshAssertions.assertThat(node).hasStringField("content", expectedPageUrl);
		});
		assertObject("Check english page", mesh.client(), MESH_PROJECT_NAME, english, true, node -> {
			// assert content
			MeshAssertions.assertThat(node).hasStringField("content", expectedPageUrl);
		});
	}

	@Test
	public void testRenderFileUrl() throws Exception {
		Folder folder = Builder.create(Folder.class, f -> {
			f.setMotherId(node.getFolder().getId());
			f.setName("Testfolder");
			f.setPublishDir("/generic/path");

			f.setPublishDirI18n(new I18nMap().put("de", "/german/path").put("en", "/english/path"));
		}).build();

		// create a file in the folder
		File file = Builder.create(File.class, create -> {
			create.setFolderId(folder.getId());
			create.setName("testfile.txt");
			create.setFiletype("text/raw");
			create.setFileStream(new ByteArrayInputStream("file contents".getBytes()));
		}).build();

		// create a page in german, which renders the file URL
		Page german = Builder.create(Page.class, create -> {
			create.setFolderId(folder.getId());
			create.setTemplateId(template.getId());
			create.setName("German");
			create.setLanguage(getLanguage("de"));

			ContentTag fileTag = create.getContent().addContentTag(fileUrlConstructId);
			getPartType(FileURLPartType.class, fileTag, "file").setTargetFile(file);
			getPartType(LongHTMLPartType.class, create.getContentTag("tag"), "text")
					.setText("<node " + fileTag.getName() + ">");
		}).publish().build();

		// create a page in english, which renders the file URL
		Page english = Builder.create(Page.class, create -> {
			create.setFolderId(folder.getId());
			create.setTemplateId(template.getId());
			create.setName("English");
			create.setLanguage(getLanguage("en"));

			ContentTag fileTag = create.getContent().addContentTag(fileUrlConstructId);
			getPartType(FileURLPartType.class, fileTag, "file").setTargetFile(file);
			getPartType(LongHTMLPartType.class, create.getContentTag("tag"), "text")
					.setText("<node " + fileTag.getName() + ">");
		}).publish().build();

		if (!instantPublishing) {
			try (Trx trx = new Trx()) {
				context.publish(false);
				trx.success();
			}
		}

		String expectedFileUrl = "/generic/path/testfile.txt";
		assertObject("Check published file", mesh.client(), MESH_PROJECT_NAME, file, true, node -> {
			// assert url
			MeshAssertions.assertThat(node).hasStringField(MeshPublisher.FIELD_GTX_URL, expectedFileUrl);
		});
		assertObject("Check german page", mesh.client(), MESH_PROJECT_NAME, german, true, node -> {
			// assert content
			MeshAssertions.assertThat(node).hasStringField("content", expectedFileUrl);
		});
		assertObject("Check english page", mesh.client(), MESH_PROJECT_NAME, english, true, node -> {
			// assert content
			MeshAssertions.assertThat(node).hasStringField("content", expectedFileUrl);
		});
	}

	@Test
	public void testRenderPageUrlWithSegments() throws Exception {
		// activate pub_dir_segments
		node = update(node, update -> {
			update.setPubDirSegment(true);
		});

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		Folder folder = Builder.create(Folder.class, f -> {
			f.setMotherId(node.getFolder().getId());
			f.setName("Testfolder");
			f.setPublishDir("generic");

			f.setPublishDirI18n(new I18nMap().put("de", "german").put("en", "english"));
		}).build();

		// create the target page in english
		Page targetPage = Builder.create(Page.class, create -> {
			create.setFolderId(folder.getId());
			create.setTemplateId(template.getId());
			create.setName("Target");
			create.setFilename("target.html");
			create.setLanguage(getLanguage("en"));
		}).publish().build();

		// create a page in german, which renders the page URL
		Page german = Builder.create(Page.class, create -> {
			create.setFolderId(folder.getId());
			create.setTemplateId(template.getId());
			create.setName("German");
			create.setFilename("german.html");
			create.setLanguage(getLanguage("de"));

			ContentTag pageTag = create.getContent().addContentTag(pageUrlConstructId);
			getPartType(PageURLPartType.class, pageTag, "page").setTargetPage(targetPage);
			getPartType(LongHTMLPartType.class, create.getContentTag("tag"), "text")
					.setText("<node " + pageTag.getName() + ">");
		}).publish().build();

		// create a page in english, which renders the page URL
		Page english = Builder.create(Page.class, create -> {
			create.setFolderId(folder.getId());
			create.setTemplateId(template.getId());
			create.setName("English");
			create.setFilename("english.html");
			create.setLanguage(getLanguage("en"));

			ContentTag pageTag = create.getContent().addContentTag(pageUrlConstructId);
			getPartType(PageURLPartType.class, pageTag, "page").setTargetPage(targetPage);
			getPartType(LongHTMLPartType.class, create.getContentTag("tag"), "text")
					.setText("<node " + pageTag.getName() + ">");
		}).publish().build();

		if (!instantPublishing) {
			try (Trx trx = new Trx()) {
				context.publish(false);
				trx.success();
			}
		}

		String expectedPageUrl = "/en/home/english/target.html";
		assertObject("Check published target page", mesh.client(), MESH_PROJECT_NAME, targetPage, true);
		assertObject("Check german page", mesh.client(), MESH_PROJECT_NAME, german, true, node -> {
			// assert content
			MeshAssertions.assertThat(node).hasStringField("content", expectedPageUrl);
		});
		assertObject("Check english page", mesh.client(), MESH_PROJECT_NAME, english, true, node -> {
			// assert content
			MeshAssertions.assertThat(node).hasStringField("content", expectedPageUrl);
		});
	}

	@Test
	public void testRenderFileUrlWithSegments() throws Exception {
		// acticate pub_dir_segments
		node = update(node, update -> {
			update.setPubDirSegment(true);
		});

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		Folder folder = Builder.create(Folder.class, f -> {
			f.setMotherId(node.getFolder().getId());
			f.setName("Testfolder");
			f.setPublishDir("generic");

			f.setPublishDirI18n(new I18nMap().put("de", "german").put("en", "english"));
		}).build();

		// create a file in the folder
		File file = Builder.create(File.class, create -> {
			create.setFolderId(folder.getId());
			create.setName("testfile.txt");
			create.setFiletype("text/raw");
			create.setFileStream(new ByteArrayInputStream("file contents".getBytes()));
		}).build();

		// create a page in german, which renders the file URL
		Page german = Builder.create(Page.class, create -> {
			create.setFolderId(folder.getId());
			create.setTemplateId(template.getId());
			create.setName("German");
			create.setFilename("german.html");
			create.setLanguage(getLanguage("de"));

			ContentTag fileTag = create.getContent().addContentTag(fileUrlConstructId);
			getPartType(FileURLPartType.class, fileTag, "file").setTargetFile(file);
			getPartType(LongHTMLPartType.class, create.getContentTag("tag"), "text")
					.setText("<node " + fileTag.getName() + ">");
		}).publish().build();

		// create a page in english, which renders the file URL
		Page english = Builder.create(Page.class, create -> {
			create.setFolderId(folder.getId());
			create.setTemplateId(template.getId());
			create.setName("English");
			create.setFilename("english.html");
			create.setLanguage(getLanguage("en"));

			ContentTag fileTag = create.getContent().addContentTag(fileUrlConstructId);
			getPartType(FileURLPartType.class, fileTag, "file").setTargetFile(file);
			getPartType(LongHTMLPartType.class, create.getContentTag("tag"), "text")
					.setText("<node " + fileTag.getName() + ">");
		}).publish().build();

		if (!instantPublishing) {
			try (Trx trx = new Trx()) {
				context.publish(false);
				trx.success();
			}
		}

		String expectedFileUrl = "/home/generic/testfile.txt";
		assertObject("Check published file", mesh.client(), MESH_PROJECT_NAME, file, true);
		assertObject("Check german page", mesh.client(), MESH_PROJECT_NAME, german, true, node -> {
			// assert content
			MeshAssertions.assertThat(node).hasStringField("content", expectedFileUrl);
		});
		assertObject("Check english page", mesh.client(), MESH_PROJECT_NAME, english, true, node -> {
			// assert content
			MeshAssertions.assertThat(node).hasStringField("content", expectedFileUrl);
		});
	}

	/**
	 * Create a folder, optionally with translations in de, en, fr and it (but not es)
	 * @param addTranslations true to add translations
	 * @return folder with translations
	 * @throws NodeException
	 */
	protected Folder createFolder(boolean addTranslations) throws NodeException {
		return create(Folder.class, f -> {
			f.setMotherId(node.getFolder().getId());
			f.setName("Generic Name");
			f.setDescription("Generic Description");
			f.setPublishDir("/generic/path/");
			if (addTranslations) {
				f.setNameI18n(new I18nMap().put("de", "Name auf Deutsch").put("en", "Name in English"));
				f.setDescriptionI18n(new I18nMap().put("en", "Description in English").put("fr", "Description en français"));
				f.setPublishDirI18n(new I18nMap().put("fr", "/chemin/").put("it", "/sentiero/"));
			}
		});
	}

	/**
	 * Create an asserter
	 * @param language language
	 * @param name name
	 * @param description description
	 * @param pubDir publish dir
	 * @return asserter
	 */
	protected Consumer<NodeResponse> doAssert(String language, String name, String description, String pubDir) {
		return n -> {
			MeshAssertions.assertThat(n).as("Folder in " + language).hasStringField("name", name);
			MeshAssertions.assertThat(n).as("Folder in " + language).hasStringField("description", description);
			MeshAssertions.assertThat(n).as("Folder in " + language).hasStringField("pub_dir", pubDir);
			MeshAssertions.assertThat(n).as("Folder in " + language).hasStringField("creator", ".Node Gentics");
		};
	}
}
