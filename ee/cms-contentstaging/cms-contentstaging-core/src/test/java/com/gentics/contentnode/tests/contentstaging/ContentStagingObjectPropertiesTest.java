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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.object.ContentFile;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.LocalizableNodeObject;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.NodeObject.GlobalId;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.ObjectTagContainer;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.parttype.ShortTextPartType;
import com.gentics.contentnode.rest.model.response.ContentPackageListResponse;
import com.gentics.contentnode.rest.model.response.ContentPackageLoadResponse;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.staging.StagedContentPackage;
import com.gentics.contentnode.rest.resource.impl.ContentStagingResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.ContentPackageListParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.staging.StagingError;
import com.gentics.contentnode.staging.StagingErrorKind;
import com.gentics.contentnode.staging.StagingErrors;
import com.gentics.contentnode.tests.utils.Builder;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.testutils.GenericTestUtils;

import jakarta.ws.rs.core.Response.Status;

/**
 * Test cases for handling object properties with content staging
 */
@GCNFeature(set = { Feature.CONTENT_STAGING })
public class ContentStagingObjectPropertiesTest implements ContentStagingMethods {
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
			ContentPackageListResponse list = resource.list(new FilterParameterBean(), new SortParameterBean(), new PagingParameterBean(), new ContentPackageListParameterBean());
			for (StagedContentPackage pack : list.getItems()) {
				resource.delete(pack.getName());
			}
		});
	}

	/**
	 * Test importing a folder with an object property, that is restricted to other nodes, into an existing node
	 */
	@Test
	public void testImportRestrictedForFolder() throws NodeException, IOException {
		String contentPackageName = "TestPackage";

		// create node to be imported later
		Node testNode = supply(() -> createNode());
		Folder testFolder = execute(n -> createFolder(n.getFolder(), "Test Folder"), testNode);
		GlobalId testFolderId = execute(Folder::getGlobalId, testFolder);
		// the root folder of the node is contained in the package and also has the restricted object property
		GlobalId rootFolderId = execute(n -> n.getFolder().getGlobalId(), testNode);

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

		// delete the folder (the node still exists, because objects in nodes created by the import get the object property assigned)
		consume(o -> o.delete(true), testFolder);

		// restrict object property to other node
		restrictedFolderDef = Builder.update(restrictedFolderDef, upd -> {
			upd.getNodes().add(node);
		}).build();

		// import package must fail, because the restricted object property cannot be imported
		assertImportFailsNotAllowed(testFolderId, Folder.class, rootFolderId);
	}

	/**
	 * Test importing a page with an object property, that is restricted to other nodes, into an existing node
	 */
	@Test
	public void testImportRestrictedForPage() throws NodeException, IOException {
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

		// delete the page (the node still exists, because objects in nodes created by the import get the object property assigned)
		consume(o -> o.delete(true), testPage);

		// restrict object property to other node
		restrictedPageDef = Builder.update(restrictedPageDef, upd -> {
			upd.getNodes().add(node);
		}).build();

		// import package must fail, because the restricted object property cannot be imported
		assertImportFailsNotAllowed(testPageId, Page.class);
	}

	/**
	 * Test importing a file with an object property, that is restricted to other nodes, into an existing node
	 */
	@Test
	public void testImportRestrictedForFile() throws NodeException, IOException {
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

		// delete the file (the node still exists, because objects in nodes created by the import get the object property assigned)
		consume(o -> o.delete(true), testFile);

		// restrict object property to other node
		restrictedFileDef = Builder.update(restrictedFileDef, upd -> {
			upd.getNodes().add(node);
		}).build();

		// import package must fail, because the restricted object property cannot be imported
		assertImportFailsNotAllowed(testFileId, com.gentics.contentnode.object.File.class);
	}

	/**
	 * Test importing an image with an object property, that is restricted to other nodes, into an existing node
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

		// delete the image (the node still exists, because objects in nodes created by the import get the object property assigned)
		consume(o -> o.delete(true), testImage);

		// restrict object property to other node
		restrictedImageDef = Builder.update(restrictedImageDef, upd -> {
			upd.getNodes().add(node);
		}).build();

		// import package must fail, because the restricted object property cannot be imported
		assertImportFailsNotAllowed(testImageId, ImageFile.class);
	}

	/**
	 * Test importing an image with a filled object property, that is not assigned to the target node.
	 * The import must fail (instead of silently dropping the value) and must import the value,
	 * once the object property has been assigned to the target node.
	 * @throws IOException
	 */
	@Test
	public void testImportRestrictedValueForImageAfterAssigning() throws NodeException, IOException {
		String value = "Restricted alt text";

		// create image with a value in the object property, which is not restricted at this time
		// (clearing the nodes would leave the "restricted" flag set, so the restriction is removed explicitly)
		Node testNode = supply(() -> createNode());
		restrictedImageDef = Builder.update(restrictedImageDef, upd -> {
			upd.setRestricted(false);
		}).build();
		byte[] imageData = IOUtils.toByteArray(GenericTestUtils.getPictureResource("blume.jpg"));
		int testFolderId = execute(n -> n.getFolder().getId(), testNode);
		ImageFile testImage = Builder.create(ImageFile.class, create -> {
			create.setFolderId(testFolderId);
			create.setFileStream(new ByteArrayInputStream(imageData));
			create.setName("testimage.jpg");
			// the file type determines whether the object tags for images (and not for files) are used
			create.setFiletype("image/jpeg");
			ObjectTag tag = create.getObjectTag(RESTRICTED_SHORT_NAME);
			assertThat(tag).as("Object tag %s (available: %s)", RESTRICTED_SHORT_NAME, create.getObjectTags().keySet()).isNotNull();
			tag.setEnabled(true);
			tag.getValues().getByKeyname("text").setValueText(value);
		}).build();
		GlobalId testImageId = execute(ImageFile::getGlobalId, testImage);

		// export
		operate(() -> {
			ContentStagingResourceImpl resource = new ContentStagingResourceImpl();
			StagedContentPackage pack = new StagedContentPackage();
			pack.setName(CONTENT_PACKAGE_NAME);
			resource.create(pack);

			ContentPackageLoadResponse response = resource.add(CONTENT_PACKAGE_NAME, "image", testImageId.toString(),
					false);
			assertResponseOK(response);
		});

		// delete the image (the target node still exists)
		consume(o -> o.delete(true), testImage);

		// assign object property only to the other node
		restrictedImageDef = Builder.update(restrictedImageDef, upd -> {
			upd.getNodes().clear();
			upd.getNodes().add(node);
		}).build();

		// import package must fail, because the object property is not assigned to the target node
		assertImportFailsNotAllowed(testImageId, ImageFile.class);

		// assign object property also to the target node
		restrictedImageDef = Builder.update(restrictedImageDef, upd -> {
			upd.getNodes().add(testNode);
		}).build();

		// import package succeeds now and contains the value
		operate(() -> {
			ContentStagingResourceImpl resource = new ContentStagingResourceImpl();
			GenericResponse response = resource.importIntoCms(CONTENT_PACKAGE_NAME, 0, false);
			assertResponseOK(response);
		});
		consume(id -> {
			ImageFile o = TransactionManager.getCurrentTransaction().getObject(ImageFile.class, id);
			assertThat(o).as("Imported image").isNotNull();
			ObjectTag tag = o.getObjectTag(RESTRICTED_SHORT_NAME);
			assertThat(tag).as("Restricted object tag after import").isNotNull();
			assertThat(tag.isEnabled()).as("Restricted object tag enabled").isTrue();
			assertThat(tag.getValues().getByKeyname("text").getValueText()).as("Restricted object tag value").isEqualTo(value);
		}, testImageId);
	}

	/**
	 * Test importing a folder with a filled object property, that is restricted to other nodes, into a node, which is newly created by the import.
	 * The import must succeed and assign the object property to the new node.
	 */
	@Test
	public void testImportRestrictedForFolderIntoNewNode() throws NodeException {
		restrictedFolderDef = unrestrict(restrictedFolderDef);
		Node testNode = supply(() -> createNode());
		Folder testFolder = execute(n -> createFolder(n.getFolder(), "Test Folder"), testNode);

		restrictedFolderDef = assertImportIntoNewNodeAssigns(testNode, testFolder, "folder", Folder.class, restrictedFolderDef);
	}

	/**
	 * Test importing a page with a filled object property, that is restricted to other nodes, into a node, which is newly created by the import.
	 * The import must succeed and assign the object property to the new node.
	 */
	@Test
	public void testImportRestrictedForPageIntoNewNode() throws NodeException {
		restrictedPageDef = unrestrict(restrictedPageDef);
		Node testNode = supply(() -> createNode());
		Page testPage = execute(n -> createPage(n.getFolder(), template, "Test Page"), testNode);

		restrictedPageDef = assertImportIntoNewNodeAssigns(testNode, testPage, "page", Page.class, restrictedPageDef);
	}

	/**
	 * Test importing a file with a filled object property, that is restricted to other nodes, into a node, which is newly created by the import.
	 * The import must succeed and assign the object property to the new node.
	 */
	@Test
	public void testImportRestrictedForFileIntoNewNode() throws NodeException {
		restrictedFileDef = unrestrict(restrictedFileDef);
		Node testNode = supply(() -> createNode());
		com.gentics.contentnode.object.File testFile = execute(n -> createFile(n.getFolder(), "testfile.txt", "Test File Contents".getBytes()), testNode);

		restrictedFileDef = assertImportIntoNewNodeAssigns(testNode, testFile, "file", com.gentics.contentnode.object.File.class, restrictedFileDef);
	}

	/**
	 * Test importing an image with a filled object property, that is restricted to other nodes, into a node, which is newly created by the import.
	 * The import must succeed and assign the object property to the new node.
	 */
	@Test
	public void testImportRestrictedForImageIntoNewNode() throws NodeException, IOException {
		restrictedImageDef = unrestrict(restrictedImageDef);
		Node testNode = supply(() -> createNode());
		byte[] imageData = IOUtils.toByteArray(GenericTestUtils.getPictureResource("blume.jpg"));
		ImageFile testImage = execute(n -> (ImageFile) createImage(n.getFolder(), "testimage.jpg", imageData), testNode);

		restrictedImageDef = assertImportIntoNewNodeAssigns(testNode, testImage, "image", ImageFile.class, restrictedImageDef);
	}

	/**
	 * Remove the restriction of the given object property definition, so that it is visible in newly created nodes
	 * (clearing the nodes would leave the "restricted" flag set)
	 * @param def object property definition
	 * @return updated definition
	 * @throws NodeException
	 */
	protected ObjectTagDefinition unrestrict(ObjectTagDefinition def) throws NodeException {
		return Builder.update(def, upd -> {
			upd.setRestricted(false);
		}).build();
	}

	/**
	 * Fill the restricted object property of the given object, export the object into the content package {@link #CONTENT_PACKAGE_NAME},
	 * delete the node, restrict the object property to the other node and import the package.
	 * Assert that the import succeeds, the node is created again, the object property is assigned to the created node (in addition to the other node)
	 * and the value of the object property is imported.
	 * @param testNode node of the object, which will be deleted before the import
	 * @param object object holding the restricted object property
	 * @param type type of the object for adding it to the content package
	 * @param clazz class of the object
	 * @param def restricted object property definition
	 * @return updated definition
	 * @throws NodeException
	 */
	protected <T extends ObjectTagContainer & LocalizableNodeObject<?>> ObjectTagDefinition assertImportIntoNewNodeAssigns(Node testNode, T object, String type,
			Class<T> clazz, ObjectTagDefinition def) throws NodeException {
		try {
			String value = "Restricted value";
			GlobalId testNodeId = execute(Node::getGlobalId, testNode);
			GlobalId objectId = execute(NodeObject::getGlobalId, object);

			// fill the object property
			Builder.update(object, upd -> {
				ObjectTag tag = upd.getObjectTag(RESTRICTED_SHORT_NAME);
				assertThat(tag).as("Object tag %s (available: %s)", RESTRICTED_SHORT_NAME, upd.getObjectTags().keySet()).isNotNull();
				tag.setEnabled(true);
				tag.getValues().getByKeyname("text").setValueText(value);
			}).build();

			// export
			operate(() -> {
				ContentStagingResourceImpl resource = new ContentStagingResourceImpl();
				StagedContentPackage pack = new StagedContentPackage();
				pack.setName(CONTENT_PACKAGE_NAME);
				resource.create(pack);

				ContentPackageLoadResponse response = resource.add(CONTENT_PACKAGE_NAME, type, objectId.toString(), false);
				assertResponseOK(response);
			});

			// delete node
			consume(Node::delete, testNode);
			Node nodeBeforeImport = supply(t -> t.getObject(Node.class, testNodeId));
			assertThat(nodeBeforeImport).as("Node before import").isNull();

			// restrict object property to other node
			ObjectTagDefinition restrictedDef = Builder.update(def, upd -> {
				upd.getNodes().add(node);
			}).build();
			boolean restrictedBeforeImport = execute(ObjectTagDefinition::isRestricted, restrictedDef);
			assertThat(restrictedBeforeImport).as("Object property restricted before import").isTrue();

			// import package must succeed, because the node is newly created by the import
			operate(() -> importContentPackage(CONTENT_PACKAGE_NAME));

			Node importedNode = supply(t -> t.getObject(Node.class, testNodeId));
			assertThat(importedNode).as("Node after import").isNotNull();

			ObjectTagDefinition reloadedDef = execute(d -> d.reload(), restrictedDef);
			consume(d -> {
				assertThat(d.isRestricted()).as("Object property restricted after import").isTrue();
				assertThat(d.getNodes()).as("Nodes of the object property after import").containsOnly(node, importedNode);
				assertThat(d.isVisibleIn(importedNode)).as("Object property visible in imported node").isTrue();
			}, reloadedDef);

			consume(id -> {
				T imported = TransactionManager.getCurrentTransaction().getObject(clazz, id);
				assertThat(imported).as("Imported object").isNotNull();
				assertThat(imported.getOwningNode()).as("Node of imported object").isEqualTo(importedNode);
				ObjectTag tag = imported.getObjectTag(RESTRICTED_SHORT_NAME);
				assertThat(tag).as("Restricted object tag after import").isNotNull();
				assertThat(tag.isEnabled()).as("Restricted object tag enabled").isTrue();
				assertThat(tag.getValues().getByKeyname("text").getValueText()).as("Restricted object tag value").isEqualTo(value);
			}, objectId);

			return reloadedDef;
		} finally {
			// remove the restriction again, because the setup only clears the nodes (and leaves the "restricted" flag set)
			unrestrict(def);
		}
	}

	/**
	 * Try to import the content package {@link #CONTENT_PACKAGE_NAME} and assert that the import fails because of
	 * {@link StagingErrorKind#NOT_ALLOWED} errors for the restricted object property, exactly one for each of the given
	 * objects, and that the staged object was not imported.
	 * @param objectId global ID of the staged object
	 * @param clazz class of the staged object
	 * @param otherObjectIds global IDs of other objects in the package, which also have the restricted object property
	 * @throws NodeException
	 * @throws IOException
	 */
	protected void assertImportFailsNotAllowed(GlobalId objectId, Class<? extends NodeObject> clazz, GlobalId... otherObjectIds) throws NodeException, IOException {
		List<String> expectedIds = new ArrayList<>();
		expectedIds.add(objectId.toString());
		Arrays.stream(otherObjectIds).map(GlobalId::toString).forEach(expectedIds::add);

		String message = supply(() -> I18NHelper.get("content.staging.import.errors", CONTENT_PACKAGE_NAME, Integer.toString(expectedIds.size())));
		tryImportContentPackage(CONTENT_PACKAGE_NAME, Status.INTERNAL_SERVER_ERROR, ResponseCode.FAILURE,
				"Error while importing", new Message(Message.Type.CRITICAL, message));

		NodeObject importedObject = supply(t -> t.getObject(clazz, objectId));
		assertThat(importedObject).as("Object after failed import").isNull();

		File errorsFile = supply(() -> (File) new ContentStagingResourceImpl().getImportErrors(CONTENT_PACKAGE_NAME).getEntity());
		List<StagingError> errors = new ObjectMapper().readValue(errorsFile, StagingErrors.class).getErrors();
		assertThat(errors).as("Import errors").extracting(StagingError::getGlobalId).containsExactlyInAnyOrderElementsOf(expectedIds);
		for (StagingError error : errors) {
			assertThat(error.getKind()).as("Error kind").isEqualTo(StagingErrorKind.NOT_ALLOWED);
			assertThat(error.getError()).as("Error message").contains(RESTRICTED_LONG_NAME);
		}
	}
}
