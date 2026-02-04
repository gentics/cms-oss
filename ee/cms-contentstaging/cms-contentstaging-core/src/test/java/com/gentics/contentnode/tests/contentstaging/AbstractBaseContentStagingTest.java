package com.gentics.contentnode.tests.contentstaging;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.utils.Builder.create;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponseOK;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.assertRequiredPermissions;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.perm;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import com.gentics.contentnode.object.ContentFile;
import com.gentics.contentnode.tests.assertj.GCNAssertions;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.WastebinFilter;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.NodeObject.GlobalId;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.StageableNodeObject;
import com.gentics.contentnode.perm.TypePerms;
import com.gentics.contentnode.rest.model.perm.PermType;
import com.gentics.contentnode.rest.model.response.StagingResponse;
import com.gentics.contentnode.rest.model.response.StagingStatus;
import com.gentics.contentnode.rest.resource.impl.ContentStagingResourceImpl;
import com.gentics.contentnode.runtime.ConfigurationValue;

/**
 * Abstract class for base content staging tests
 *
 * @param <T> type of the staged object
 */
public abstract class AbstractBaseContentStagingTest<T extends StageableNodeObject> extends AbstractContentStagingTest<T> {
	/**
	 * Create test instance
	 * @param clazz object class
	 * @param type object type
	 */
	protected AbstractBaseContentStagingTest(Class<T> clazz, String type) {
		super(clazz, type);
	}

	/**
	 * Test importing the object as new object (in the root folder)
	 * @throws NodeException
	 */
	@Test
	public void testImportNewObject() throws NodeException {
		// create object
		T stagedObject = createObject(nodeRootFolder, 1);
		String objectGlobalId = execute(f -> f.getGlobalId().toString(), stagedObject);

		// create package and add object
		String packageName = "TestPackage";
		createContentPackage(packageName);
		assertRequiredPermissions(testGroup, testUser, new ContentStagingResourceImpl(), "add",
				new Class<?>[] { String.class, String.class, String.class, Boolean.TYPE },
				new Object[] { packageName, type, objectGlobalId, false },
				perm(TypePerms.admin, PermType.read),
				perm(TypePerms.contentadmin, PermType.read),
				perm(TypePerms.contentstagingadmin, PermType.read),
				perm(TypePerms.contentstagingadmin, PermType.modifycontent),
				perm(nodeRootFolder, PermType.read),
				readPerm(stagedObject));

		// delete object in CMS
		consume(f -> f.delete(true), stagedObject);
		stagedObject = supply(t -> t.getObject(clazz, objectGlobalId));
		assertThat(stagedObject).as("Deleted object").isNull();

		// import the package
		assertRequiredPermissions(testGroup, testUser, new ContentStagingResourceImpl(), "importIntoCms",
				new Class<?>[] { String.class, Long.TYPE, Boolean.TYPE }, new Object[] { packageName, 0, false },
				perm(TypePerms.admin, PermType.read),
				perm(TypePerms.contentadmin, PermType.read),
				perm(TypePerms.contentstagingadmin, PermType.read),
				perm(nodeRootFolder, PermType.read),
				perm(nodeRootFolder, PermType.updatefolder),
				readItemPerm(nodeRootFolder),
				createPerm(nodeRootFolder));

		// reload and assert
		stagedObject = supply(t -> t.getObject(clazz, objectGlobalId));
		consume(this::assertStagedObject, stagedObject, nodeRootFolder);
	}

	/**
	 * Test importing the object as updated object (in the root folder)
	 * @throws NodeException
	 */
	@Test
	public void testImportUpdatedObject() throws NodeException {
		// create object
		T stagedObject = createObject(nodeRootFolder, 1);
		String objectGlobalId = execute(f -> f.getGlobalId().toString(), stagedObject);

		// create package and add object
		String packageName = "TestPackage";
		createContentPackage(packageName);
		assertRequiredPermissions(testGroup, testUser, new ContentStagingResourceImpl(), "add",
				new Class<?>[] { String.class, String.class, String.class, Boolean.TYPE },
				new Object[] { packageName, type, objectGlobalId, false },
				perm(TypePerms.admin, PermType.read),
				perm(TypePerms.contentadmin, PermType.read),
				perm(TypePerms.contentstagingadmin, PermType.read),
				perm(TypePerms.contentstagingadmin, PermType.modifycontent),
				perm(nodeRootFolder, PermType.read),
				readPerm(stagedObject));

		// update object in CMS
		stagedObject = updateObject(stagedObject, 2);

		// import the package
		assertRequiredPermissions(testGroup, testUser, new ContentStagingResourceImpl(), "importIntoCms",
				new Class<?>[] { String.class, Long.TYPE, Boolean.TYPE }, new Object[] { packageName, 0, false },
				perm(TypePerms.admin, PermType.read),
				perm(TypePerms.contentadmin, PermType.read),
				perm(TypePerms.contentstagingadmin, PermType.read),
				perm(nodeRootFolder, PermType.read),
				perm(nodeRootFolder, PermType.updatefolder),
				readPerm(stagedObject),
				updatePerm(stagedObject));

		// reload and assert
		stagedObject = supply(t -> t.getObject(clazz, objectGlobalId));
		consume(this::assertStagedObject, stagedObject, nodeRootFolder);
	}

	/**
	 * Test importing a folder structure
	 * @throws NodeException
	 */
	@Test
	public void testImportInFolderStructure() throws NodeException {
		// create a folder structure
		Folder level1 = create(Folder.class, f -> {
			f.setMotherId(nodeRootFolder.getId());
			f.setName("Level 1");
			f.setPublishDir("/level1");
		}).build();
		String level1GlobalId = execute(f -> f.getGlobalId().toString(), level1);
		Integer level1Id = execute(Folder::getId, level1);

		Folder level2 = create(Folder.class, f -> {
			f.setMotherId(level1Id);
			f.setName("Level 2");
			f.setPublishDir("/level2");
		}).build();
		String level2GlobalId = execute(f -> f.getGlobalId().toString(), level2);

		T stagedObject = createObject(level2, 1);
		String objectGlobalId = execute(f -> f.getGlobalId().toString(), stagedObject);

		// create package and add object
		String packageName = "TestPackage";
		createContentPackage(packageName);
		assertRequiredPermissions(testGroup, testUser, new ContentStagingResourceImpl(), "add",
				new Class<?>[] { String.class, String.class, String.class, Boolean.TYPE },
				new Object[] { packageName, type, objectGlobalId, false },
				perm(TypePerms.admin, PermType.read),
				perm(TypePerms.contentadmin, PermType.read),
				perm(TypePerms.contentstagingadmin, PermType.read),
				perm(TypePerms.contentstagingadmin, PermType.modifycontent),
				perm(nodeRootFolder, PermType.read),
				perm(level1, PermType.read),
				perm(level2, PermType.read),
				readPerm(stagedObject));

		// delete the folder structure
		consume(f -> f.delete(true), level1);
		stagedObject = supply(t -> t.getObject(clazz, objectGlobalId));
		assertThat(stagedObject).as("Deleted object").isNull();

		// import the package
		assertRequiredPermissions(testGroup, testUser, new ContentStagingResourceImpl(), "importIntoCms",
				new Class<?>[] { String.class, Long.TYPE, Boolean.TYPE }, new Object[] { packageName, 0, false },
				perm(TypePerms.admin, PermType.read),
				perm(TypePerms.contentadmin, PermType.read),
				perm(TypePerms.contentstagingadmin, PermType.read),
				perm(nodeRootFolder, PermType.read),
				perm(nodeRootFolder, PermType.updatefolder),
				perm(nodeRootFolder, PermType.create),
				createPerm(nodeRootFolder),
				readItemPerm(nodeRootFolder));

		// reload and assert
		stagedObject = supply(t -> t.getObject(clazz, objectGlobalId));
		level1 = supply(t -> t.getObject(Folder.class, level1GlobalId));
		level2 = supply(t -> t.getObject(Folder.class, level2GlobalId));
		assertThat(level1).as("Level 1 Folder").isNotNull();
		assertThat(level2).as("Level 2 Folder").isNotNull();
		consume(this::assertStagedObject, stagedObject, level2);

		try (Trx trx = new Trx()) {
			assertThat(stagedObject.getParentObject()).as("Parent folder").isEqualTo(level2);
			assertThat(level2.getParentObject()).as("Grandparent folder").isEqualTo(level1);
			trx.success();
		}
	}

	/**
	 * Test importing an object (as new) which contains a reference to an existing object
	 * @throws NodeException
	 */
	@Test
	public void testImportWithReference() throws NodeException {
		Page referencedPage = create(Page.class, p -> {
			p.setFolderId(nodeRootFolder.getId());
			p.setTemplateId(template.getId());
			p.setName("Referenced page");
		}).build();

		T stagedObject = createObject(nodeRootFolder, 1);
		String objectGlobalId = execute(f -> f.getGlobalId().toString(), stagedObject);

		setPageReference(stagedObject, referencedPage);

		// create package and add object
		String packageName = "TestPackage";
		createContentPackage(packageName);
		assertResponseOK(new ContentStagingResourceImpl().add(packageName, type, objectGlobalId, false));

		// delete object in CMS
		consume(f -> f.delete(true), stagedObject);
		stagedObject = supply(t -> t.getObject(clazz, objectGlobalId));
		assertThat(stagedObject).as("Deleted object").isNull();

		// import the package
		importContentPackage(packageName);

		// reload and assert
		stagedObject = supply(t -> t.getObject(clazz, objectGlobalId));
		consume(this::assertPageReference, stagedObject, referencedPage);
	}

	/**
	 * Test importing an object (as new) which contains a reference to a not existing object
	 * @throws NodeException
	 */
	@Test
	public void testImportWithUnresolvableReference() throws NodeException {
		Page referencedPage = create(Page.class, p -> {
			p.setFolderId(nodeRootFolder.getId());
			p.setTemplateId(template.getId());
			p.setName("Referenced page");
		}).build();

		T stagedObject = createObject(nodeRootFolder, 1);
		String objectGlobalId = execute(f -> f.getGlobalId().toString(), stagedObject);

		setPageReference(stagedObject, referencedPage);

		// create package and add object
		String packageName = "TestPackage";
		createContentPackage(packageName);
		assertResponseOK(new ContentStagingResourceImpl().add(packageName, type, objectGlobalId, false));

		// delete object in CMS
		consume(f -> f.delete(true), stagedObject);
		stagedObject = supply(t -> t.getObject(clazz, objectGlobalId));
		assertThat(stagedObject).as("Deleted object").isNull();

		// delete referenced page in the CMS
		consume(p -> p.delete(true), referencedPage);

		// import the package
		importContentPackage(packageName);

		// reload and assert
		stagedObject = supply(t -> t.getObject(clazz, objectGlobalId));
		consume(this::assertPageReference, stagedObject, null);
	}

	/**
	 * Test importing an object with its folder, where the folder references the object
	 * @throws NodeException
	 */
	@Test
	public void testImportReferencedFromFolder() throws NodeException {
		Folder level1 = create(Folder.class, f -> {
			f.setMotherId(nodeRootFolder.getId());
			f.setName("Level 1");
			f.setPublishDir("/level1");
		}).build();
		String level1GlobalId = execute(f -> f.getGlobalId().toString(), level1);

		T stagedObject = createObject(level1, 1);
		String objectGlobalId = execute(f -> f.getGlobalId().toString(), stagedObject);

		// modify the folder to reference the object
		level1 = setForeignReference(level1, stagedObject);

		// create package and add object
		String packageName = "TestPackage";
		createContentPackage(packageName);
		assertResponseOK(new ContentStagingResourceImpl().add(packageName, type, objectGlobalId, false));

		// delete the folder structure
		consume(f -> f.delete(true), level1);
		stagedObject = supply(t -> t.getObject(clazz, objectGlobalId));
		assertThat(stagedObject).as("Deleted object").isNull();

		// import the package
		importContentPackage(packageName);

		// reload and assert
		stagedObject = supply(t -> t.getObject(clazz, objectGlobalId));
		level1 = supply(t -> t.getObject(Folder.class, level1GlobalId));
		consume(this::assertForeignReference, level1, stagedObject);
	}

	/**
	 * Test import an object, which has been put into the wastebin
	 * @throws NodeException
	 */
	@Test
	public void testImportInWastebin() throws NodeException {
		T object = createObject(nodeRootFolder, 1);
		GlobalId objectGlobalId = execute(NodeObject::getGlobalId, object);

		// create package and add object
		String packageName = "TestPackage";
		createContentPackage(packageName);
		assertResponseOK(new ContentStagingResourceImpl().add(packageName, type, objectGlobalId.toString(), false));

		// modify object
		object = updateObject(object, 2);

		// delete object into wastebin
		consume(NodeObject::delete, object);
		try (Trx trx = new Trx(); WastebinFilter wbf = WastebinFilter.get(true, node)) {
			object = object.reload();

			// We cannot use hasFieldOrPropertyWithValue("deleted", true) here, because NodeObject have both a
			// isDeleted() and a getDeleted() method, and hasFieldOrPropertyWithValue() uses the wrong getter that does
			// not yield a boolean but a timestamp.
			assertThat(object).as("Object exists").isNotNull();
			assertThat(object.isDeleted()).as("Object is in wastebin").isTrue();
			trx.success();
		}

		// import the package
		importContentPackage(packageName);

		// reload and assert
		object = supply(t -> t.getObject(clazz, objectGlobalId));
		consume(this::assertStagedObject, object, nodeRootFolder);
	}

	/**
	 * Test importing an object into a folder, which has been put into the wastebin
	 * @throws NodeException
	 */
	@Test
	public void testImportIntoFolderInWastebin() throws NodeException {
		Folder folder = create(Folder.class, f -> {
			f.setMotherId(nodeRootFolder.getId());
			f.setName("Folder");
			f.setPublishDir("/");
		}).build();

		T object = createObject(folder, 1);
		GlobalId objectGlobalId = execute(NodeObject::getGlobalId, object);

		// create package and add object
		String packageName = "TestPackage";
		createContentPackage(packageName);
		assertResponseOK(new ContentStagingResourceImpl().add(packageName, type, objectGlobalId.toString(), false));

		// delete object (not into wastebin)
		consume(o -> o.delete(true), object);

		// delete folder (into wastebin)
		consume(NodeObject::delete, folder);

		try (Trx trx = new Trx(); WastebinFilter wbf = WastebinFilter.get(true, node)) {
			object = object.reload();
			assertThat(object).as("Deleted object").isNull();
			folder = folder.reload();
			assertThat(folder).as("Folder in wastebin").isNotNull().isDeleted(true);
			trx.success();
		}

		// import the package
		importContentPackage(packageName);

		// reload and assert
		object = supply(t -> t.getObject(clazz, objectGlobalId));
		folder = execute(NodeObject::reload, folder);
		consume(f -> {
			assertThat(f).as("Restored folder").isNotNull();
		}, folder);
		consume(this::assertStagedObject, object, folder);
	}

	/**
	 * Test importing a moved object
	 * @throws NodeException
	 */
	@Test
	public void testImportMovedObject() throws NodeException {
		// create source and target folder
		Folder sourceFolder = create(Folder.class, f -> {
			f.setMotherId(nodeRootFolder.getId());
			f.setName("Source Folder");
			f.setPublishDir("/source");
		}).build();
		GlobalId sourceFolderGlobalId = execute(NodeObject::getGlobalId, sourceFolder);

		Folder targetFolder = create(Folder.class, f -> {
			f.setMotherId(nodeRootFolder.getId());
			f.setName("Target Folder");
			f.setPublishDir("/target");
		}).build();

		// create object
		T stagedObject = createObject(sourceFolder, 1);
		String objectGlobalId = execute(f -> f.getGlobalId().toString(), stagedObject);

		// create package and add object
		String packageName = "TestPackage";
		createContentPackage(packageName);
		assertResponseOK(new ContentStagingResourceImpl().add(packageName, type, objectGlobalId.toString(), false));

		// move object to target folder
		moveObject(stagedObject, targetFolder);

		// delete the source folder
		consume(f -> f.delete(true), sourceFolder);

		// import the package
		importContentPackage(packageName);

		// reload and assert
		sourceFolder = supply(t -> t.getObject(Folder.class, sourceFolderGlobalId));
		stagedObject = supply(t -> t.getObject(clazz, objectGlobalId));
		consume(this::assertStagedObject, stagedObject, sourceFolder);
	}

	/**
	 * Test importing objects which reference each other
	 * @throws NodeException
	 */
	@Test
	public void testImportReferencingEachOther() throws NodeException {
		// create two objects
		T object1 = createObject(nodeRootFolder, 1);
		GlobalId object1GlobalId = execute(NodeObject::getGlobalId, object1);

		T object2 = createObject(nodeRootFolder, 1);
		GlobalId object2GlobalId = execute(NodeObject::getGlobalId, object2);

		// let them reference each other
		setReference(object1, object2);
		setReference(object2, object1);

		// create package and add objects
		String packageName = "TestPackage";
		createContentPackage(packageName);
		assertResponseOK(new ContentStagingResourceImpl().add(packageName, type, object1GlobalId.toString(), false));
		assertResponseOK(new ContentStagingResourceImpl().add(packageName, type, object2GlobalId.toString(), false));

		// delete objects
		consume(f -> f.delete(true), object1);
		consume(f -> f.delete(true), object2);

		// import
		importContentPackage(packageName);

		// reload and assert
		object1 = supply(t -> t.getObject(clazz, object1GlobalId));
		object2 = supply(t -> t.getObject(clazz, object2GlobalId));
		consume(this::assertReference, object1, object2);
		consume(this::assertReference, object2, object1);
	}

	/**
	 * Test getting the staging info for the object
	 * @throws NodeException
	 */
	@Test
	public void testStagingInfo() throws NodeException {
		// create object
		T stagedObject = createObject(nodeRootFolder, 1);
		String objectGlobalId = execute(f -> f.getGlobalId().toString(), stagedObject);

		// create package and add object
		String packageName = "TestPackage";
		createContentPackage(packageName);

		StagingResponse<String> stagingResponse = getStagingResponse(packageName, nodeRootFolder);
		assertThat(stagingResponse.getStagingStatus()).as("Staging status").isNotNull().containsOnly(
				entry(objectGlobalId, new StagingStatus().setPackageName(packageName).setIncluded(false)));

		assertResponseOK(new ContentStagingResourceImpl().add(packageName, type, objectGlobalId, false));

		stagingResponse = getStagingResponse(packageName, nodeRootFolder);
		assertThat(stagingResponse.getStagingStatus()).as("Staging status").isNotNull()
				.containsOnly(entry(objectGlobalId, new StagingStatus().setPackageName(packageName).setIncluded(true)));
	}

	/**
	 * Test getting the staging info for the object using the legacy method
	 * @throws NodeException
	 */
	@Test
	public void testLegacyStagingInfo() throws NodeException {
		// create object
		T stagedObject = createObject(nodeRootFolder, 1);
		String objectGlobalId = execute(f -> f.getGlobalId().toString(), stagedObject);

		// create package and add object
		String packageName = "TestPackage";
		createContentPackage(packageName);

		StagingResponse<String> stagingResponse = getLegacyStagingResponse(packageName, nodeRootFolder);
		assertThat(stagingResponse.getStagingStatus()).as("Staging status").isNotNull().containsOnly(
				entry(objectGlobalId, new StagingStatus().setPackageName(packageName).setIncluded(false)));

		assertResponseOK(new ContentStagingResourceImpl().add(packageName, type, objectGlobalId, false));

		stagingResponse = getLegacyStagingResponse(packageName, nodeRootFolder);
		assertThat(stagingResponse.getStagingStatus()).as("Staging status").isNotNull()
				.containsOnly(entry(objectGlobalId, new StagingStatus().setPackageName(packageName).setIncluded(true)));
	}

	/**
	 * Test refreshing an export where an object in the content package was deleted in the CMS
	 * @throws NodeException
	 * @throws IOException
	 */
	@Test
	public void testExportDeletedObject() throws NodeException, IOException {
		// create object
		T stagedObject = createObject(nodeRootFolder, 1);
		String objectGlobalId = execute(f -> f.getGlobalId().toString(), stagedObject);

		// create package and add object
		String packageName = "TestPackage";
		createContentPackage(packageName);
		assertResponseOK(new ContentStagingResourceImpl().add(packageName, type, objectGlobalId, false));
		assertThat(containedGlobalIds(packageName)).as("Contained global IDs").contains(objectGlobalId);

		// delete object in CMS
		consume(f -> f.delete(true), stagedObject);
		stagedObject = supply(t -> t.getObject(clazz, objectGlobalId));
		assertThat(stagedObject).as("Deleted object").isNull();

		// export again (must not fail and must remove object in content package)
		assertResponseOK(new ContentStagingResourceImpl().exportFromCms(packageName, 0));
		assertThat(containedGlobalIds(packageName)).as("Contained global IDs").doesNotContain(objectGlobalId);
	}

	/**
	 * Test refreshing an export where an object in the content package was moved to another folder in the CMS, and the original folder was then deleted
	 * @throws NodeException
	 * @throws IOException
	 */
	@Test
	public void testExportMovedObjectsFromDeletedFolder() throws NodeException, IOException {
		// create source folder
		Folder source = create(Folder.class, f -> {
			f.setMotherId(nodeRootFolder.getId());
			f.setName("Source folder");
		}).build();
		String sourceGlobalId = execute(f -> f.getGlobalId().toString(), source);

		// create target folder
		Folder target = create(Folder.class, f -> {
			f.setMotherId(nodeRootFolder.getId());
			f.setName("Target folder");
		}).build();
		String targetGlobalId = execute(f -> f.getGlobalId().toString(), target);

		// create object in source folder
		T stagedObject = createObject(source, 1);
		String objectGlobalId = execute(f -> f.getGlobalId().toString(), stagedObject);

		// create package and add object
		String packageName = "TestPackage";
		createContentPackage(packageName);
		assertResponseOK(new ContentStagingResourceImpl().add(packageName, type, objectGlobalId, false));

		assertThat(containedGlobalIds(packageName)).as("Contained global IDs").contains(sourceGlobalId, objectGlobalId).doesNotContain(targetGlobalId);

		// move staged object to target folder
		moveObject(stagedObject, target);

		// delete source folder
		consume(f -> f.delete(true), source);
		source = supply(t -> t.getObject(Folder.class, sourceGlobalId));
		assertThat(source).as("Deleted source folder").isNull();
		stagedObject = supply(t -> t.getObject(clazz, objectGlobalId));
		assertThat(stagedObject).as("Moved staged object").isNotNull();

		// export again
		assertResponseOK(new ContentStagingResourceImpl().exportFromCms(packageName, 0));

		// assert
		assertThat(containedGlobalIds(packageName)).as("Contained global IDs").contains(targetGlobalId, objectGlobalId).doesNotContain(sourceGlobalId);
	}

	/**
	 * Get the global IDs of objects contained in the content package
	 * @param packageName content package name
	 * @return set of global IDs
	 * @throws IOException
	 */
	protected Set<String> containedGlobalIds(String packageName) throws IOException {
		Path root = Paths.get(ConfigurationValue.CONTENT_PACKAGES_PATH.get());
		Path packageRoot = root.resolve(packageName);
		Set<String> foundGlobalIds = new HashSet<>();
		Files.walk(packageRoot).forEach(path -> {
			if (Files.isRegularFile(path)) {
				ContentStagingResourceImpl.getObjectId(path).ifPresent(id -> foundGlobalIds.add(id));
			}
		});

		return foundGlobalIds;
	}

	/**
	 * Create an object for staging
	 * @param folder mother folder
	 * @param timestamp timestamp of creation
	 * @return object
	 * @throws NodeException
	 */
	protected abstract T createObject(Folder folder, int timestamp) throws NodeException;

	/**
	 * Update the object in the CMS
	 * @param object object to be updated
	 * @param timestamp modification timestamp
	 * @return updated object
	 * @throws NodeException
	 */
	protected abstract T updateObject(T object, int timestamp) throws NodeException;

	/**
	 * Move the object into the target folder
	 * @param object object to move
	 * @param targetFolder target folder
	 * @return moved object
	 * @throws NodeException
	 */
	protected abstract T moveObject(T object, Folder targetFolder) throws NodeException;

	/**
	 * Set a reference to the other object
	 * @param object object to be changed
	 * @param referenced referenced object
	 * @return changed object
	 * @throws NodeException
	 */
	protected abstract T setReference(T object, T referenced) throws NodeException;

	/**
	 * Assert that the object in the CMS exists and has the properties with which it was created
	 * @param object object to check
	 * @param parent parent folder
	 * @throws NodeException
	 */
	protected abstract void assertStagedObject(T object, Folder parent) throws NodeException;

	/**
	 * Change the folder to reference the object
	 * @param folder folder to update
	 * @param object referenced object
	 * @return changed folder
	 * @throws NodeException
	 */
	protected abstract Folder setForeignReference(Folder folder, T object) throws NodeException;

	/**
	 * Assert that the folder references the object
	 * @param folder folder with reference
	 * @param object referenced object
	 * @throws NodeException
	 */
	protected abstract void assertForeignReference(Folder folder, T object) throws NodeException;

	/**
	 * Assert that the object references the other object
	 * @param object object with reference
	 * @param referenced referenced object
	 * @throws NodeException
	 */
	protected abstract void assertReference(T object, T referenced) throws NodeException;

	/**
	 * Get the staging response for objects of tested type in the folder
	 * @param packageName package name
	 * @param folder folder
	 * @return staging response
	 * @throws NodeException
	 */
	protected abstract StagingResponse<String> getStagingResponse(String packageName, Folder folder) throws NodeException;

	/**
	 * Get the staging response for objects of tested type in the folder using the legacy method
	 * @param packageName package name
	 * @param folder folder
	 * @return staging response
	 * @throws NodeException
	 */
	protected abstract StagingResponse<String> getLegacyStagingResponse(String packageName, Folder folder) throws NodeException;
}
