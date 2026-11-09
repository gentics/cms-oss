package com.gentics.contentnode.tests.contentstaging;

import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.object.NodeObject.GlobalId;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponseOK;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static org.assertj.core.api.Assertions.assertThat;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.devtools.PackageSynchronizer;
import com.gentics.contentnode.devtools.SynchronizableNodeObject;
import com.gentics.contentnode.devtools.Synchronizer;
import com.gentics.contentnode.etc.Consumer;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.StageableNodeObject;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.parttype.FileURLPartType;
import com.gentics.contentnode.object.parttype.FolderURLPartType;
import com.gentics.contentnode.object.parttype.ImageURLPartType;
import com.gentics.contentnode.object.parttype.NodePartType;
import com.gentics.contentnode.object.parttype.PageURLPartType;
import com.gentics.contentnode.object.parttype.PartType;
import com.gentics.contentnode.rest.model.staging.StagedContentPackage;
import com.gentics.contentnode.rest.resource.impl.ContentStagingResourceImpl;
import com.gentics.contentnode.tests.devtools.PackageSynchronizerContext;
import com.gentics.contentnode.tests.utils.Builder;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.testutils.GenericTestUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tests for setting missing references after the objects are imported via content staging.
 */
@GCNFeature(set = { Feature.CONTENT_STAGING, Feature.DEVTOOLS, Feature.WASTEBIN })
public class MissingReferencesTest implements ContentStagingMethods {

	private final static String DEVTOOLS_PACKAGE_NAME = "devtoolspackage";
	private final static String CONTENT_PACKAGE_NAME = "contentpackage";

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();
	@Rule
	public PackageSynchronizerContext syncContext = new PackageSynchronizerContext();

	private PackageSynchronizer pack;
	private Node node;
	private Folder nodeRootFolder;
	private Template testTemplate;
	private Template pageTemplate;
	private Construct testConstruct;
	private GlobalId constructGlobalId;
	private GlobalId templateGlobalId;

	@BeforeClass
	public static void setupOnce() throws Exception {

		Transaction transaction = testContext.getContext().getTransaction();

		if (transaction != null) {
			transaction.commit();
		}
	}

	@Before
	public void setup() throws NodeException, IOException {
		Synchronizer.addPackage(DEVTOOLS_PACKAGE_NAME);

		pack = Synchronizer.getPackage(DEVTOOLS_PACKAGE_NAME);

		assertThat(pack)
			.as("Package synchronizer")
			.isNotNull();

		makeClean();

		node = Trx.supply(() -> ContentNodeTestDataUtils.createNode());

		// create a test node
		node = Builder.create(
			Node.class,
			n -> {
				Folder rootFolder = Builder.create(
					Folder.class,
					f -> {
						f.setName("Test Node");
						f.setPublishDir("/");
					})
					.doNotSave()
					.build();

				n.setFolder(rootFolder);
				n.setHostname("test.node");
				n.setPublishDir("/");
				n.getLanguages().add(ContentNodeTestDataUtils.getLanguage("de"));
				n.getLanguages().add(ContentNodeTestDataUtils.getLanguage("en"));
			})
			.build();

		nodeRootFolder = Trx.supply(() -> node.getFolder());

		testConstruct = createConstruct(Arrays.asList(
			Pair.of("page", PageURLPartType.class),
			Pair.of("folder", FolderURLPartType.class),
			Pair.of("file", FileURLPartType.class),
			Pair.of("image", ImageURLPartType.class),
			Pair.of("node", NodePartType.class)));
		constructGlobalId = testConstruct.getGlobalId();

		Construct templateConstruct = createConstruct(Arrays.asList(
			Pair.of("page", PageURLPartType.class),
			Pair.of("folder", FolderURLPartType.class),
			Pair.of("file", FileURLPartType.class),
			Pair.of("image", ImageURLPartType.class),
			Pair.of("node", NodePartType.class)));

		testTemplate = Builder.create(
			Template.class,
			t -> {
				t.setMlId(1);
				t.setName("Template");
				t.setSource("");
				t.addFolder(nodeRootFolder);

				t.getTemplateTags().put(
					"tag",
					create(
						TemplateTag.class,
						tag -> {
							tag.setConstructId(templateConstruct.getConstructId());
							tag.setName("tag");
							},
						false));
			})
			.build();
		templateGlobalId = testTemplate.getGlobalId();

		pageTemplate = Builder.create(
			Template.class,
			t -> {
				t.setMlId(1);
				t.setName("Template");
				t.setSource("");
				t.addFolder(nodeRootFolder);
			})
		.build();
	}

	@After
	public void teardown() throws NodeException {
		Synchronizer.removePackage(DEVTOOLS_PACKAGE_NAME);
	}

	/**
	 * Test if folder reference is correctly restored in a construct.
	 */
	@Test
	public void testConstructFolderReference() throws NodeException {
		Synchronizer.disable();

		Folder folder = Builder.create(
				Folder.class,
				f -> {
					f.setMotherId(nodeRootFolder.getId());
					f.setName("testfolder");
					f.setDescription("This is the staged folder");
				})
			.build();
		GlobalId folderGlobalId = folder.getGlobalId();
		String keyname = "folder";

		updateAndSyncConstructToPackage(keyname, FolderURLPartType.class, folder, p -> p::setTargetFolder);
		syncAndDeleteObject(keyname, folder);
		syncFromPackage(Construct.class, Construct::delete, testConstruct, "construct");

		assertMissingReference(folderGlobalId.toString());

		Folder reloadedFolder = reloadObject(Folder.class, folderGlobalId);

		assertThat(getMissingReferences())
			.as("Missing references")
			.isEmpty();

		testConstruct = Trx.supply(t -> t.getObject(Construct.class, constructGlobalId));

		Trx.operate(() -> checkConstructPart(
			keyname,
			FolderURLPartType.class,
			p -> assertThat(p.getTargetFolder().getId()).as("Imported target folder ID").isEqualTo(reloadedFolder.getId())));
	}

	/**
	 * Test if file reference is correctly restored in a construct.
	 */
	@Test
	public void testConstructFileReference() throws NodeException {
		Synchronizer.disable();

		File file = Builder.create(
			File.class,
			f -> {
				f.setFolderId(nodeRootFolder.getId());
				f.setName("testfile.txt");
				f.setDescription("This is the staged file");
				f.setFileStream(new ByteArrayInputStream("contents".getBytes()));
				f.setForceOnline(true);
			})
			.build();
		GlobalId fileGlobalId = file.getGlobalId();
		String keyname = "file";

		updateAndSyncConstructToPackage(keyname, FileURLPartType.class, file, p -> p::setTargetFile);
		syncAndDeleteObject(keyname, file);
		syncFromPackage(Construct.class, Construct::delete, testConstruct, "construct");

		assertMissingReference(fileGlobalId.toString());

		File reloadedFile = reloadObject(File.class, fileGlobalId);

		assertThat(getMissingReferences())
			.as("Missing references")
			.isEmpty();

		testConstruct = Trx.supply(t -> t.getObject(Construct.class, constructGlobalId));

		Trx.operate(() -> checkConstructPart(
			keyname,
			FileURLPartType.class,
			p -> assertThat(p.getTargetFile().getId()).as("Imported target file ID").isEqualTo(reloadedFile.getId())));
	}

	/**
	 * Test if image reference is correctly restored in a construct.
	 */
	@Test
	public void testConstructImageReference() throws NodeException {
		Synchronizer.disable();

		ImageFile image = Builder.create(
				ImageFile.class,
				i -> {
					i.setFolderId(nodeRootFolder.getId());
					i.setName("testimage.jpg");
					i.setDescription("This is the staged image");
					i.setFileStream(GenericTestUtils.getPictureResource("blume.jpg"));
					i.setForceOnline(true);
				})
			.build();
		GlobalId imageGlobalId = image.getGlobalId();
		String keyname = "image";

		updateAndSyncConstructToPackage(keyname, ImageURLPartType.class, image, p -> p::setTargetImage);
		syncAndDeleteObject(keyname, image);
		syncFromPackage(Construct.class, Construct::delete, testConstruct, "construct");

		assertMissingReference(imageGlobalId.toString());

		ImageFile reloadedImage = reloadObject(ImageFile.class, imageGlobalId);

		assertThat(getMissingReferences())
			.as("Missing references")
			.isEmpty();

		testConstruct = Trx.supply(t -> t.getObject(Construct.class, constructGlobalId));

		Trx.operate(() -> checkConstructPart(
			keyname,
			ImageURLPartType.class,
			p -> assertThat(p.getTargetImage().getId()).as("Imported target image ID").isEqualTo(reloadedImage.getId())));
	}

	/**
	 * Test if page reference is correctly restored in a construct.
	 */
	@Test
	public void testConstructPageReference() throws NodeException {
		Synchronizer.disable();

		Page page = Builder.create(
				Page.class,
				p -> {
					p.setFolderId(nodeRootFolder.getId());
					p.setTemplateId(pageTemplate.getId());
					p.setName("Staged page");
					p.setFilename("staged_page.html");
					p.setDescription("This is the staged page");
				})
			.build();
		GlobalId pageGlobalId = page.getGlobalId();
		String keyname = "page";

		updateAndSyncConstructToPackage(keyname, PageURLPartType.class, page, p -> p::setTargetPage);
		syncAndDeleteObject(keyname, page);
		syncFromPackage(Construct.class, Construct::delete, testConstruct, "construct");

		assertMissingReference(pageGlobalId.toString());

		Page reloadedPage = reloadObject(Page.class, pageGlobalId);

		assertThat(getMissingReferences())
			.as("Missing references")
			.isEmpty();

		testConstruct = Trx.supply(t -> t.getObject(Construct.class, constructGlobalId));

		Trx.operate(() -> checkConstructPart(
			keyname,
			PageURLPartType.class,
			p -> assertThat(p.getTargetPage().getId()).as("Imported target page ID").isEqualTo(reloadedPage.getId())));
	}

	/**
	 * Test if folder reference is correctly restored in a template.
	 */
	@Test
	public void testTemplateFolderReference() throws NodeException {
		Synchronizer.disable();

		Folder folder = Builder.create(
				Folder.class,
				f -> {
					f.setMotherId(nodeRootFolder.getId());
					f.setName("testfolder");
					f.setDescription("This is the staged folder");
				})
			.build();
		GlobalId folderGlobalId = folder.getGlobalId();
		String keyname = "folder";

		updateAndSyncTemplateToPackage(keyname, FolderURLPartType.class, folder, p -> p::setTargetFolder);
		syncAndDeleteObject(keyname, folder);
		syncFromPackage(Template.class, Template::delete, testTemplate, "template");

		assertMissingReference(folderGlobalId.toString());

		Folder reloadedFolder = reloadObject(Folder.class, folderGlobalId);

		assertThat(getMissingReferences())
			.as("Missing references")
			.isEmpty();

		testTemplate = Trx.supply(t -> t.getObject(Template.class, templateGlobalId));

		Trx.operate(() -> checkTemplateTagPart(
			keyname,
			FolderURLPartType.class,
			p -> assertThat(p.getTargetFolder().getId())
				.as("Imported target folder ID")
				.isEqualTo(reloadedFolder.getId())
				.isNotEqualTo(folder.getId())));
	}

	/**
	 * Test if file reference is correctly restored in a template.
	 */
	@Test
	public void testTemplateFileReference() throws NodeException {
		Synchronizer.disable();

		File file = Builder.create(
				File.class,
				f -> {
					f.setFolderId(nodeRootFolder.getId());
					f.setName("testfile.txt");
					f.setDescription("This is the staged file");
					f.setFileStream(new ByteArrayInputStream("contents".getBytes()));
					f.setForceOnline(true);
				})
			.build();
		GlobalId fileGlobalId = file.getGlobalId();
		String keyname = "file";

		updateAndSyncTemplateToPackage(keyname, FileURLPartType.class, file, p -> p::setTargetFile);
		syncAndDeleteObject(keyname, file);
		syncFromPackage(Template.class, Template::delete, testTemplate, "template");

		assertMissingReference(fileGlobalId.toString());

		File reloadedFile = reloadObject(File.class, fileGlobalId);

		assertThat(getMissingReferences())
			.as("Missing references")
			.isEmpty();

		testTemplate = Trx.supply(t -> t.getObject(Template.class, templateGlobalId));

		Trx.operate(() -> checkTemplateTagPart(
			keyname,
			FileURLPartType.class,
			p -> assertThat(p.getTargetFile().getId())
				.as("Imported target file ID")
				.isEqualTo(reloadedFile.getId())
				.isNotEqualTo(file.getId())));
	}

	/**
	 * Test if image reference is correctly restored in a template.
	 */
	@Test
	public void testTemplateImageReference() throws NodeException {
		Synchronizer.disable();

		ImageFile image = Builder.create(
				ImageFile.class,
				i -> {
					i.setFolderId(nodeRootFolder.getId());
					i.setName("testimage.jpg");
					i.setDescription("This is the staged image");
					i.setFileStream(GenericTestUtils.getPictureResource("blume.jpg"));
					i.setForceOnline(true);
				})
			.build();
		GlobalId imageGlobalId = image.getGlobalId();
		String keyname = "image";

		updateAndSyncTemplateToPackage(keyname, ImageURLPartType.class, image, p -> p::setTargetImage);
		syncAndDeleteObject(keyname, image);
		syncFromPackage(Template.class, Template::delete, testTemplate, "template");

		assertMissingReference(imageGlobalId.toString());

		ImageFile reloadedImage = reloadObject(ImageFile.class, imageGlobalId);

		assertThat(getMissingReferences())
			.as("Missing references")
			.isEmpty();

		testTemplate = Trx.supply(t -> t.getObject(Template.class, templateGlobalId));

		Trx.operate(() -> checkTemplateTagPart(
			keyname,
			ImageURLPartType.class,
			p -> assertThat(p.getTargetImage().getId())
				.as("Imported target image ID")
				.isEqualTo(reloadedImage.getId())
				.isNotEqualTo(image.getId())));
	}

	/**
	 * Test if page reference is correctly restored in a template.
	 */
	@Test
	public void testTemplatePageReference() throws NodeException {
		Synchronizer.disable();

		Page page = Builder.create(
				Page.class,
				p -> {
					p.setFolderId(nodeRootFolder.getId());
					p.setTemplateId(pageTemplate.getId());
					p.setName("Staged page");
					p.setFilename("staged_page.html");
					p.setDescription("This is the staged page");
				})
			.build();
		GlobalId pageGlobalId = page.getGlobalId();
		String keyname = "page";

		updateAndSyncTemplateToPackage(keyname, PageURLPartType.class, page, p -> p::setTargetPage);
		syncAndDeleteObject(keyname, page);
		syncFromPackage(Template.class, Template::delete, testTemplate, "template");

		assertMissingReference(pageGlobalId.toString());

		Page reloadedPage = reloadObject(Page.class, pageGlobalId);

		assertThat(getMissingReferences())
			.as("Missing references")
			.isEmpty();

		testTemplate = Trx.supply(t -> t.getObject(Template.class, templateGlobalId));

		Trx.operate(() -> checkTemplateTagPart(
			keyname,
			PageURLPartType.class,
			p -> assertThat(p.getTargetPage().getId())
				.as("Imported target page ID")
				.isEqualTo(reloadedPage.getId())
				.isNotEqualTo(page.getId())));
	}

	/**
	 * Create the test construct and add the specified URL parts.
	 * @param partInfos Pairs of keyname and part types.
	 * @return The created construct
	 */
	private Construct createConstruct(List<Pair<String, Class<? extends PartType>>> partInfos) throws NodeException {
		return Builder.create(
			Construct.class,
			c -> {
				c.setName("Test Construct", 1);
				c.setKeyword("testconstruct");
				c.setAutoEnable(true);

				List<Part> parts = c.getParts();

				for (Pair<String, Class<? extends PartType>> partInfo : partInfos) {
					parts.add(Builder.create(
						Part.class,
						p -> {
							p.setEditable(1);
							p.setHidden(false);
							p.setKeyname(partInfo.getLeft());
							p.setDefaultValue(Builder.create(Value.class, v -> {}).doNotSave().build());
							p.setPartTypeId(ContentNodeTestDataUtils.getPartTypeId(partInfo.getRight()));
						})
						.doNotSave()
						.build());
				}
			})
			.build();
	}

	/**
	 * Set the reference for "keyname" to "obj" and synchronize the construct to the file system.
	 * @param keyname The parts keyname.
	 * @param unusedButNeededForGenerics The tagpart class.
	 * @param obj The object to be set as reference.
	 * @param setter The actual setter method.
	 * @param <T> The tagpart type.
	 * @param <U> The object type.
	 */
	private <T, U> void updateAndSyncConstructToPackage(String keyname, Class<T> unusedButNeededForGenerics, U obj, Function<T, Consumer<U>> setter) throws NodeException {
		testConstruct = Builder.update(
			testConstruct,
			c -> {
				for (Part part : c.getParts()) {
					if (!part.getKeyname().equals(keyname)) {
						continue;
					}

					setter.apply((T) part.getPartType(part.getDefaultValue())).accept(obj);
				}
			})
			.build();

		Trx.operate(() -> pack.synchronize(testConstruct, true));
		Trx.operate(() -> assertThat(pack.getObjects(Construct.class)).as("Constructs in package").hasSize(1));
	}

	/**
	 * Set the reference for "keyname" to "obj" and synchronize the template to the file system.
	 * @param keyname The parts keyname.
	 * @param unusedButNeededForGenerics The tagpart class.
	 * @param obj The object to be set as reference.
	 * @param setter The actual setter method.
	 * @param <T> The tagpart type.
	 * @param <U> The object type.
	 */
	private <T, U> void updateAndSyncTemplateToPackage(String keyname, Class<T> unusedButNeededForGenerics, U obj, Function<T, Consumer<U>> setter) throws NodeException {
		testTemplate = Builder.update(
				testTemplate,
				t -> {
					T partType = t.getTemplateTags().get("tag").getTagValues().getByKeyname(keyname).getPartType();

					setter.apply(partType).accept(obj);
				})
			.build();

		Trx.operate(() -> pack.synchronize(testTemplate, true));
		Trx.operate(() -> assertThat(pack.getObjects(Template.class)).as("Templates in package").hasSize(1));
	}

	/**
	 * Deletes the given object and then syncs it again from the filesystem.
	 * @param objClass The object type to sync.
	 * @param deleteMethod The method to delete the object before the sync.
	 * @param obj The object to delete and sync.
	 * @param desc Test description for the object (should be it's type, i.e. construct or template).
	 * @param <T> The object type to sync.
	 */
	private <T extends SynchronizableNodeObject> void syncFromPackage(Class<T> objClass, Consumer<T> deleteMethod, T obj, String desc) throws NodeException {
		Trx.operate(() -> ContentNodeTestDataUtils.update(obj, o -> deleteMethod.accept(o)));
		Trx.operate(() -> assertThat(pack.syncAllFromFilesystem(objClass))
			.as(String.format("Synchronized %s from FS", desc))
			.isEqualTo(1));
	}

	/**
	 * Gets the part with the given {@code keyname} from the test construct and applied the given assertion.
	 * @param keyname The parts keyname.
	 * @param unusedButNeededForGenerics The tagpart class.
	 * @param assertion The assertion to apply.
	 * @param <T> The tagpart type
	 */
	private <T extends PartType> void checkConstructPart(String keyname, Class<T> unusedButNeededForGenerics, Consumer<T> assertion) throws NodeException {
		for (Part part : testConstruct.getParts()) {
			if (!part.getKeyname().equals(keyname)) {
				continue;
			}

			assertion.accept((T) part.getPartType(part.getDefaultValue()));
		}
	}

	/**
	 * Gets the part with the given {@code keyname} from the test template and applied the given assertion.
	 * @param keyname The parts keyname.
	 * @param unusedButNeededForGenerics The tagpart class.
	 * @param assertion The assertion to apply.
	 * @param <T> The tagpart type
	 */	private <T extends PartType> void checkTemplateTagPart(String keyname, Class<T> unusedButNeededForGenerics, Consumer<T> assertion) throws NodeException {
		for (Value value : testTemplate.getTemplateTag("tag").getValues()) {
			if (!value.getPart().getKeyname().equals(keyname)) {
				continue;
			}

			assertion.accept((T) value.getPartType(true));
		}
	}

	/**
	 * Syncs the given object to a content package and deletes the object afterwards.
	 * @param type The object type (used for the resource endpoint, i.e. "file", "folder", ...).
	 * @param object The object to sync and delete.
	 * @param <T> The objects type.
	 */
	private <T extends StageableNodeObject> void syncAndDeleteObject(String type, T object) throws NodeException {
		ContentStagingResourceImpl resource = new ContentStagingResourceImpl();
		StagedContentPackage contentPackage = new StagedContentPackage();

		contentPackage.setName(CONTENT_PACKAGE_NAME);

		assertResponseOK(resource.create(contentPackage));
		assertResponseOK(resource.add(CONTENT_PACKAGE_NAME, type, object.getGlobalId().toString(), false));

		Trx.consume(o -> o.delete(true), object);
	}

	/**
	 * Imports the content package and reloads the object with the given global ID.
	 * @param objClass The objects type.
	 * @param globalId The objects global ID.
	 * @return The reloaded object.
	 * @param <T> The objects type.
	 */
	private <T extends StageableNodeObject> T reloadObject(Class<T> objClass, GlobalId globalId) throws NodeException {
		ContentStagingResourceImpl resource = new ContentStagingResourceImpl();

		assertResponseOK(resource.importIntoCms(CONTENT_PACKAGE_NAME, 0, false));

		return supply(t -> t.getObject(objClass, globalId));
	}

	private void assertMissingReference(String expectedMissing) throws NodeException {
		Set<String> missingReferences = getMissingReferences();

		assertThat(missingReferences.size())
			.as("Number of missing references")
			.isEqualTo(1);

		assertThat(missingReferences.stream().findFirst().get())
			.as("Missing reference")
			.isEqualTo(expectedMissing);
	}

	private Set<String> getMissingReferences() throws NodeException {
		return Trx.supply(() -> DBUtils.select(
			"SELECT * FROM missingreference",
			rs -> {
				Set<String> missingReferences = new HashSet<>();

				while (rs.next()) {
					missingReferences.add(rs.getString("target_uuid"));
				}

				return missingReferences;
			}));
	}
}
