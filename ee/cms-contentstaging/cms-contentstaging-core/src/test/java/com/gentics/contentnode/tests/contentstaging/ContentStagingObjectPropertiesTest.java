package com.gentics.contentnode.tests.contentstaging;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponseOK;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.clear;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFile;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFolder;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createImage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createObjectPropertyDefinition;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createPage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.ContentFile;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject.GlobalId;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.parttype.ShortTextPartType;
import com.gentics.contentnode.rest.model.response.ContentPackageListResponse;
import com.gentics.contentnode.rest.model.response.ContentPackageLoadResponse;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.staging.StagedContentPackage;
import com.gentics.contentnode.rest.resource.impl.ContentStagingResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.tests.utils.Builder;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.testutils.GenericTestUtils;

/**
 * Test cases for handling object properties with content staging
 */
@GCNFeature(set = { Feature.CONTENT_STAGING })
public class ContentStagingObjectPropertiesTest {
	public final static String RESTRICTED_SHORT_NAME = "restricted_property";

	public final static String RESTRICTED_LONG_NAME = "object." + RESTRICTED_SHORT_NAME;

	public final static String UNRESTRICTED_SHORT_NAME = "unrestricted_property";

	public final static String UNRESTRICTED_LONG_NAME = "object." + UNRESTRICTED_SHORT_NAME;

	public final static String CONTENT_PACKAGE_NAME = "TestPackage";

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();
	private static Node node;
	private static int constructId;
	private static Template template;
	private static ObjectTagDefinition restrictedFolderDef;
	private static ObjectTagDefinition restrictedPageDef;
	private static ObjectTagDefinition restrictedImageDef;
	private static ObjectTagDefinition restrictedFileDef;
	private static ObjectTagDefinition unrestrictedFolderDef;
	private static ObjectTagDefinition unrestrictedPageDef;
	private static ObjectTagDefinition unrestrictedImageDef;
	private static ObjectTagDefinition unrestrictedFileDef;

	@BeforeClass
	public static void setupOnce() throws Exception {
		testContext.getContext().getTransaction().commit();

		operate(t -> {
			for (Node n : t.getObjects(Node.class, DBUtils.select("SELECT id FROM node", DBUtils.IDLIST))) {
				n.delete();
			}
		});

		operate(t -> {
			for (ObjectTagDefinition def : t.getObjects(ObjectTagDefinition.class,
					DBUtils.select("SELECT id FROM objtag WHERE obj_id = 0", DBUtils.IDLIST))) {
				def.delete(true);
			}
		});

		node = supply(() -> createNode());
		constructId = supply(() -> createConstruct(node, ShortTextPartType.class, "text", "text"));

		template = supply(() -> createTemplate(node.getFolder(), "Template"));

		restrictedFolderDef = supply(() -> createObjectPropertyDefinition(Folder.TYPE_FOLDER, constructId, "Restricted Object Property", RESTRICTED_SHORT_NAME));
		restrictedPageDef = supply(() -> createObjectPropertyDefinition(Page.TYPE_PAGE, constructId, "Restricted Object Property", RESTRICTED_SHORT_NAME));
		restrictedImageDef = supply(() -> createObjectPropertyDefinition(ContentFile.TYPE_IMAGE, constructId, "Restricted Object Property", RESTRICTED_SHORT_NAME));
		restrictedFileDef = supply(() -> createObjectPropertyDefinition(ContentFile.TYPE_FILE, constructId, "Restricted Object Property", RESTRICTED_SHORT_NAME));
		unrestrictedFolderDef = supply(() -> createObjectPropertyDefinition(Folder.TYPE_FOLDER, constructId, "Unrestricted Object Property", UNRESTRICTED_SHORT_NAME));
		unrestrictedPageDef = supply(() -> createObjectPropertyDefinition(Page.TYPE_PAGE, constructId, "Unrestricted Object Property", UNRESTRICTED_SHORT_NAME));
		unrestrictedImageDef = supply(() -> createObjectPropertyDefinition(ContentFile.TYPE_IMAGE, constructId, "Unrestricted Object Property", UNRESTRICTED_SHORT_NAME));
		unrestrictedFileDef = supply(() -> createObjectPropertyDefinition(ContentFile.TYPE_FILE, constructId, "Unrestricted Object Property", UNRESTRICTED_SHORT_NAME));
	}

	@Before
	public void setup() throws NodeException {
		operate(() -> clear(node));

		operate(t -> {
			for (Node n : t.getObjects(Node.class, DBUtils.select("SELECT id FROM node", DBUtils.IDLIST))) {
				if (!node.equals(n)) {
					n.delete();
				}
			}
		});

		List<ObjectTagDefinition> usedDefs = Arrays.asList(restrictedFolderDef, restrictedPageDef, restrictedImageDef,
				restrictedFileDef, unrestrictedFolderDef, unrestrictedPageDef, unrestrictedFileDef,
				unrestrictedImageDef);

		operate(t -> {
			for (ObjectTagDefinition def : t.getObjects(ObjectTagDefinition.class,
					DBUtils.select("SELECT id FROM objtag WHERE obj_id = 0", DBUtils.IDLIST))) {
				if (!usedDefs.contains(def)) {
					def.delete(true);
				}
			}
		});

		for (ObjectTagDefinition prop : usedDefs) {
			Builder.update(prop, upd -> {
				upd.getNodes().clear();
			}).build();
		}

		operate(() -> {
			ContentStagingResourceImpl resource = new ContentStagingResourceImpl();
			ContentPackageListResponse list = resource.list(new FilterParameterBean(), new SortParameterBean(), new PagingParameterBean());
			for (StagedContentPackage pack : list.getItems()) {
				resource.delete(pack.getName());
			}
		});
	}

	/**
	 * Test importing a folder with an object property, that is restricted to other nodes
	 */
	@Test
	public void testImportRestrictedForFolder() throws NodeException {
		String contentPackageName = "TestPackage";

		// create node to be imported later
		Node testNode = supply(() -> createNode());
		Folder testFolder = execute(n -> createFolder(n.getFolder(), "Test Folder"), testNode);
		GlobalId testFolderId = execute(Folder::getGlobalId, testFolder);

		// assert
		consume(f -> {
			f = f.reload();
			Set<String> objectTagNames = f.getObjectTags().values().stream().map(ObjectTag::getName)
					.collect(Collectors.toSet());
			assertThat(objectTagNames).as("Object Tags before export").containsOnly(RESTRICTED_LONG_NAME, UNRESTRICTED_LONG_NAME);
		}, testFolder);

		// export
		operate(() -> {
			ContentStagingResourceImpl resource = new ContentStagingResourceImpl();
			StagedContentPackage pack = new StagedContentPackage();
			pack.setName(contentPackageName);
			resource.create(pack);

			ContentPackageLoadResponse response = resource.add(contentPackageName, "folder", testFolderId.toString(),
					false);
			assertResponseOK(response);
		});

		// delete node
		consume(Node::delete, testNode);

		// restrict object property to other node
		restrictedFolderDef = Builder.update(restrictedFolderDef, upd -> {
			upd.getNodes().add(node);
		}).build();

		// import package
		operate(() -> {
			ContentStagingResourceImpl resource = new ContentStagingResourceImpl();
			GenericResponse response = resource.importIntoCms(contentPackageName, 0, false);
			assertResponseOK(response);
		});

		// assert
		consume(id -> {
			Folder f = TransactionManager.getCurrentTransaction().getObject(Folder.class, id);
			Set<String> objectTagNames = f.getObjectTags().values().stream().map(ObjectTag::getName)
					.collect(Collectors.toSet());
			assertThat(objectTagNames).as("Object Tags after import").containsOnly(UNRESTRICTED_LONG_NAME);
		}, testFolderId);
	}

	/**
	 * Test importing a page with an object property, that is restricted to other nodes
	 */
	@Test
	public void testImportRestrictedForPage() throws NodeException {
		String contentPackageName = "TestPackage";

		// create node to be imported later
		Node testNode = supply(() -> createNode());
		Page testPage = execute(n -> createPage(n.getFolder(), template, "Test Page"), testNode);
		GlobalId testPageId = execute(Page::getGlobalId, testPage);

		// assert
		consume(o -> {
			o = o.reload();
			Set<String> objectTagNames = o.getObjectTags().values().stream().map(ObjectTag::getName)
					.collect(Collectors.toSet());
			assertThat(objectTagNames).as("Object Tags before export").containsOnly(RESTRICTED_LONG_NAME, UNRESTRICTED_LONG_NAME);
		}, testPage);

		// export
		operate(() -> {
			ContentStagingResourceImpl resource = new ContentStagingResourceImpl();
			StagedContentPackage pack = new StagedContentPackage();
			pack.setName(contentPackageName);
			resource.create(pack);

			ContentPackageLoadResponse response = resource.add(contentPackageName, "page", testPageId.toString(),
					false);
			assertResponseOK(response);
		});

		// delete node
		consume(Node::delete, testNode);

		// restrict object property to other node
		restrictedPageDef = Builder.update(restrictedPageDef, upd -> {
			upd.getNodes().add(node);
		}).build();

		// import package
		operate(() -> {
			ContentStagingResourceImpl resource = new ContentStagingResourceImpl();
			GenericResponse response = resource.importIntoCms(contentPackageName, 0, false);
			assertResponseOK(response);
		});

		// assert
		consume(id -> {
			Page o = TransactionManager.getCurrentTransaction().getObject(Page.class, id);
			Set<String> objectTagNames = o.getObjectTags().values().stream().map(ObjectTag::getName)
					.collect(Collectors.toSet());
			assertThat(objectTagNames).as("Object Tags after import").containsOnly(UNRESTRICTED_LONG_NAME);
		}, testPageId);
	}

	/**
	 * Test importing a file with an object property, that is restricted to other nodes
	 */
	@Test
	public void testImportRestrictedForFile() throws NodeException {
		String contentPackageName = "TestPackage";

		// create node to be imported later
		Node testNode = supply(() -> createNode());
		com.gentics.contentnode.object.File testFile = execute(n -> createFile(n.getFolder(), "testfile.txt", "Test File Contents".getBytes()), testNode);
		GlobalId testFileId = execute(com.gentics.contentnode.object.File::getGlobalId, testFile);

		// assert
		consume(o -> {
			o = o.reload();
			Set<String> objectTagNames = o.getObjectTags().values().stream().map(ObjectTag::getName)
					.collect(Collectors.toSet());
			assertThat(objectTagNames).as("Object Tags before export").containsOnly(RESTRICTED_LONG_NAME, UNRESTRICTED_LONG_NAME);
		}, testFile);

		// export
		operate(() -> {
			ContentStagingResourceImpl resource = new ContentStagingResourceImpl();
			StagedContentPackage pack = new StagedContentPackage();
			pack.setName(contentPackageName);
			resource.create(pack);

			ContentPackageLoadResponse response = resource.add(contentPackageName, "file", testFileId.toString(),
					false);
			assertResponseOK(response);
		});

		// delete node
		consume(Node::delete, testNode);

		// restrict object property to other node
		restrictedFileDef = Builder.update(restrictedFileDef, upd -> {
			upd.getNodes().add(node);
		}).build();

		// import package
		operate(() -> {
			ContentStagingResourceImpl resource = new ContentStagingResourceImpl();
			GenericResponse response = resource.importIntoCms(contentPackageName, 0, false);
			assertResponseOK(response);
		});

		// assert
		consume(id -> {
			com.gentics.contentnode.object.File o = TransactionManager.getCurrentTransaction()
					.getObject(com.gentics.contentnode.object.File.class, id);
			Set<String> objectTagNames = o.getObjectTags().values().stream().map(ObjectTag::getName)
					.collect(Collectors.toSet());
			assertThat(objectTagNames).as("Object Tags after import").containsOnly(UNRESTRICTED_LONG_NAME);
		}, testFileId);
	}

	/**
	 * Test importing an image with an object property, that is restricted to other nodes
	 * @throws IOException 
	 */
	@Test
	public void testImportRestrictedForImage() throws NodeException, IOException {
		String contentPackageName = "TestPackage";

		// create node to be imported later
		Node testNode = supply(() -> createNode());
		byte[] imageData = IOUtils.toByteArray(GenericTestUtils.getPictureResource("blume.jpg"));
		com.gentics.contentnode.object.File testImage = execute(n -> createImage(n.getFolder(), "testfile.txt", imageData), testNode);
		GlobalId testImageId = execute(com.gentics.contentnode.object.File::getGlobalId, testImage);

		// assert
		consume(o -> {
			o = o.reload();
			Set<String> objectTagNames = o.getObjectTags().values().stream().map(ObjectTag::getName)
					.collect(Collectors.toSet());
			assertThat(objectTagNames).as("Object Tags before export").containsOnly(RESTRICTED_LONG_NAME, UNRESTRICTED_LONG_NAME);
		}, testImage);

		// export
		operate(() -> {
			ContentStagingResourceImpl resource = new ContentStagingResourceImpl();
			StagedContentPackage pack = new StagedContentPackage();
			pack.setName(contentPackageName);
			resource.create(pack);

			ContentPackageLoadResponse response = resource.add(contentPackageName, "image", testImageId.toString(),
					false);
			assertResponseOK(response);
		});

		// delete node
		consume(Node::delete, testNode);

		// restrict object property to other node
		restrictedImageDef = Builder.update(restrictedImageDef, upd -> {
			upd.getNodes().add(node);
		}).build();

		// import package
		operate(() -> {
			ContentStagingResourceImpl resource = new ContentStagingResourceImpl();
			GenericResponse response = resource.importIntoCms(contentPackageName, 0, false);
			assertResponseOK(response);
		});

		// assert
		consume(id -> {
			ImageFile o = TransactionManager.getCurrentTransaction().getObject(ImageFile.class, id);
			Set<String> objectTagNames = o.getObjectTags().values().stream().map(ObjectTag::getName)
					.collect(Collectors.toSet());
			assertThat(objectTagNames).as("Object Tags after import").containsOnly(UNRESTRICTED_LONG_NAME);
		}, testImageId);
	}
}
