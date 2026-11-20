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

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.ContentFile;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
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
import com.gentics.contentnode.rest.model.staging.StagedContentPackage;
import com.gentics.contentnode.rest.resource.impl.ContentStagingResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.ContentPackageListParameterBean;
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
				// clearing the nodes leaves the "restricted" flag set, so it is reset explicitly
				upd.setRestricted(false);
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
	 * Test importing a folder with an object property, that is restricted to other nodes, into a node, which is created by the import.
	 * The import must succeed and assign the object property to the created node.
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

		// delete node
		GlobalId testNodeId = execute(Node::getGlobalId, testNode);
		consume(Node::delete, testNode);

		// restrict object property to other node
		restrictedFolderDef = Builder.update(restrictedFolderDef, upd -> {
			upd.getNodes().add(node);
		}).build();

		// import package must succeed and assign the restricted object property to the created node
		restrictedFolderDef = assertImportAssigns(restrictedFolderDef, testNodeId, Folder.class, testFolderId, null, rootFolderId);
	}

	/**
	 * Test importing a page with an object property, that is restricted to other nodes, into a node, which is created by the import.
	 * The import must succeed and assign the object property to the created node.
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

		// delete node
		GlobalId testNodeId = execute(Node::getGlobalId, testNode);
		consume(Node::delete, testNode);

		// restrict object property to other node
		restrictedPageDef = Builder.update(restrictedPageDef, upd -> {
			upd.getNodes().add(node);
		}).build();

		// import package must succeed and assign the restricted object property to the created node
		restrictedPageDef = assertImportAssigns(restrictedPageDef, testNodeId, Page.class, testPageId, null);
	}

	/**
	 * Test importing a file with an object property, that is restricted to other nodes, into a node, which is created by the import.
	 * The import must succeed and assign the object property to the created node.
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

		// delete node
		GlobalId testNodeId = execute(Node::getGlobalId, testNode);
		consume(Node::delete, testNode);

		// restrict object property to other node
		restrictedFileDef = Builder.update(restrictedFileDef, upd -> {
			upd.getNodes().add(node);
		}).build();

		// import package must succeed and assign the restricted object property to the created node
		restrictedFileDef = assertImportAssigns(restrictedFileDef, testNodeId, com.gentics.contentnode.object.File.class, testFileId, null);
	}

	/**
	 * Test importing an image with an object property, that is restricted to other nodes, into a node, which is created by the import.
	 * The import must succeed and assign the object property to the created node.
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
		GlobalId testNodeId = execute(Node::getGlobalId, testNode);
		consume(Node::delete, testNode);

		// restrict object property to other node
		restrictedImageDef = Builder.update(restrictedImageDef, upd -> {
			upd.getNodes().add(node);
		}).build();

		// import package must succeed and assign the restricted object property to the created node
		restrictedImageDef = assertImportAssigns(restrictedImageDef, testNodeId, ImageFile.class, testImageId, null);
	}

	/**
	 * Test importing an image with a filled object property, that is not assigned to the existing target node.
	 * The import must succeed, assign the object property to the target node and import the value
	 * (instead of silently dropping the value).
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

		// import package must succeed, assign the object property to the target node and import the value
		GlobalId testNodeId = execute(Node::getGlobalId, testNode);
		restrictedImageDef = assertImportAssigns(restrictedImageDef, testNodeId, ImageFile.class, testImageId, value);
	}

	/**
	 * Test importing a folder with a filled object property, that is not assigned to the existing target node.
	 * The import must succeed, assign the object property to the target node and import the value.
	 */
	@Test
	public void testImportRestrictedValueForFolderIntoExistingNode() throws NodeException {
		Node testNode = supply(() -> createNode());
		Folder testFolder = execute(n -> createFolder(n.getFolder(), "Test Folder"), testNode);

		restrictedFolderDef = assertImportIntoExistingNodeAssigns(testNode, testFolder, "folder", Folder.class, restrictedFolderDef);
	}

	/**
	 * Test importing a page with a filled object property, that is not assigned to the existing target node.
	 * The import must succeed, assign the object property to the target node and import the value.
	 */
	@Test
	public void testImportRestrictedValueForPageIntoExistingNode() throws NodeException {
		Node testNode = supply(() -> createNode());
		Page testPage = execute(n -> createPage(n.getFolder(), template, "Test Page"), testNode);

		restrictedPageDef = assertImportIntoExistingNodeAssigns(testNode, testPage, "page", Page.class, restrictedPageDef);
	}

	/**
	 * Test importing a file with a filled object property, that is not assigned to the existing target node.
	 * The import must succeed, assign the object property to the target node and import the value.
	 */
	@Test
	public void testImportRestrictedValueForFileIntoExistingNode() throws NodeException {
		Node testNode = supply(() -> createNode());
		com.gentics.contentnode.object.File testFile = execute(n -> createFile(n.getFolder(), "testfile.txt", "Test File Contents".getBytes()), testNode);

		restrictedFileDef = assertImportIntoExistingNodeAssigns(testNode, testFile, "file", com.gentics.contentnode.object.File.class, restrictedFileDef);
	}

	/**
	 * Fill the restricted object property of the given object, export the object into the content package {@link #CONTENT_PACKAGE_NAME},
	 * delete the object (but not its node), restrict the object property to the other node and import the package.
	 * Assert that the import assigns the object property to the node of the object and imports the value.
	 * @param testNode node of the object, which still exists at the time of the import
	 * @param object object holding the restricted object property
	 * @param type type of the object for adding it to the content package
	 * @param clazz class of the object
	 * @param def restricted object property definition
	 * @return updated definition
	 * @throws NodeException
	 */
	protected <T extends ObjectTagContainer> ObjectTagDefinition assertImportIntoExistingNodeAssigns(Node testNode, T object, String type,
			Class<T> clazz, ObjectTagDefinition def) throws NodeException {
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

		// delete the object (the node still exists)
		consume(o -> o.delete(true), object);

		// restrict object property to other node
		ObjectTagDefinition restrictedDef = Builder.update(def, upd -> {
			upd.getNodes().add(node);
		}).build();

		return assertImportAssigns(restrictedDef, testNodeId, clazz, objectId, value);
	}

	/**
	 * Import the content package {@link #CONTENT_PACKAGE_NAME} and assert that it succeeds, that the given restricted object property
	 * is assigned to exactly the other node and the node with given global ID, and that the imported objects have the object tag.
	 * @param def restricted object property definition
	 * @param nodeId global ID of the node, to which the object property must be assigned by the import
	 * @param clazz class of the staged objects
	 * @param objectId global ID of the staged object
	 * @param expectedValue expected value of the object tag of the staged object (null to not check the value)
	 * @param otherObjectIds global IDs of other objects of the same class in the package, which also have the restricted object property
	 * @return reloaded definition
	 * @throws NodeException
	 */
	protected ObjectTagDefinition assertImportAssigns(ObjectTagDefinition def, GlobalId nodeId, Class<? extends ObjectTagContainer> clazz,
			GlobalId objectId, String expectedValue, GlobalId... otherObjectIds) throws NodeException {
		operate(() -> importContentPackage(CONTENT_PACKAGE_NAME));

		Node importedNode = supply(t -> t.getObject(Node.class, nodeId));
		assertThat(importedNode).as("Node after import").isNotNull();

		ObjectTagDefinition reloadedDef = execute(d -> d.reload(), def);
		consume(d -> {
			assertThat(d.isRestricted()).as("Object property restricted after import").isTrue();
			assertThat(d.getNodes()).as("Nodes of the object property after import").containsOnly(node, importedNode);
		}, reloadedDef);

		List<GlobalId> ids = new ArrayList<>();
		ids.add(objectId);
		ids.addAll(Arrays.asList(otherObjectIds));
		for (GlobalId id : ids) {
			consume(globalId -> {
				ObjectTagContainer imported = TransactionManager.getCurrentTransaction().getObject(clazz, globalId);
				assertThat(imported).as("Imported object %s", globalId).isNotNull();
				ObjectTag tag = imported.getObjectTag(RESTRICTED_SHORT_NAME);
				assertThat(tag).as("Restricted object tag of %s after import", globalId).isNotNull();
			}, id);
		}

		if (expectedValue != null) {
			consume(globalId -> {
				ObjectTag tag = TransactionManager.getCurrentTransaction().getObject(clazz, globalId).getObjectTag(RESTRICTED_SHORT_NAME);
				assertThat(tag.isEnabled()).as("Restricted object tag enabled").isTrue();
				assertThat(tag.getValues().getByKeyname("text").getValueText()).as("Restricted object tag value").isEqualTo(expectedValue);
			}, objectId);
		}

		return reloadedDef;
	}
}
