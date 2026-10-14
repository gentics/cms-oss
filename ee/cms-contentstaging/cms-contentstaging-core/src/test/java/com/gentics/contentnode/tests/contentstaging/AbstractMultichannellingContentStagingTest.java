package com.gentics.contentnode.tests.contentstaging;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.utils.Builder.create;
import static com.gentics.contentnode.tests.utils.Builder.update;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponseOK;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createChannel;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.localize;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.assertDirtedObjects;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.assertDirtedObjectsNonStrict;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.NodeObject.GlobalId;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.StageableChanneledNodeObject;
import com.gentics.contentnode.rest.model.request.IdTypeMapRequest;
import com.gentics.contentnode.rest.model.response.StagingStatus;
import com.gentics.contentnode.rest.model.response.StagingStatusResponse;
import com.gentics.contentnode.rest.resource.impl.ContentStagingResourceImpl;

/**
 * Abstract test cases for content staging of channels
 *
 * @param <T>
 */
public abstract class AbstractMultichannellingContentStagingTest<T extends StageableChanneledNodeObject> extends AbstractContentStagingTest<T> {
	/**
	 * Test channel
	 */
	protected Node channel;

	/**
	 * Test subchannel (under channel)
	 */
	protected Node subChannel;

	/**
	 * Test sidechannel (sibling of channel)
	 */
	protected Node sideChannel;

	/**
	 * Map of folders expected to be dirted per node
	 */
	protected Map<Node, Set<Folder>> expectedDirtedFolders = new HashMap<>();

	/**
	 * Map of pages expected to be dirted per node
	 */
	protected Map<Node, Set<Page>> expectedDirtedPages = new HashMap<>();

	/**
	 * Map of files/images expected to be dirted per node
	 */
	protected Map<Node, Set<File>> expectedDirtedFiles = new HashMap<>();

	/**
	 * Create test instance
	 * @param clazz object class
	 * @param type object type
	 */
	protected AbstractMultichannellingContentStagingTest(Class<T> clazz, String type) {
		super(clazz, type);
	}

	@Override
	@Before
	public void setup() throws NodeException, IOException {
		super.setup();
		channel = supply(() -> createChannel(node, "Channel", "channel", "/"));
		subChannel = supply(() -> createChannel(channel, "SubChannel", "subchannel", "/"));
		sideChannel = supply(() -> createChannel(node, "SideChannel", "sidechannel", "/"));
	}

	/**
	 * Test importing a localized object to an existing master object
	 * @throws NodeException
	 */
	@Test
	public void testImportLocalizedObject() throws NodeException {
		// create master
		T masterObject = createMasterObject(supply(() -> node.getFolder()));

		// localize master
		T localizedObject = localizeObject(masterObject);
		String localizedGlobalId = execute(o -> o.getGlobalId().toString(), localizedObject);

		// create package and add object
		String packageName = "TestPackage";
		createContentPackage(packageName);
		assertResponseOK(new ContentStagingResourceImpl().add(packageName, type, localizedGlobalId, false));

		// delete object in CMS
		localizedObject = delete(localizedObject);

		cleanDirtQueueAndPublishQueue();

		// import the package
		importContentPackage(packageName);

		// reload and assert
		localizedObject = supply(t -> t.getObject(clazz, localizedGlobalId));
		consume(this::assertLocalizedObject, masterObject, localizedObject, execute(Node::getFolder, node));

		testContext.waitForDirtqueueWorker();

		expectAllDirted(channel, localizedObject);
		expectAllDirted(subChannel, localizedObject);
		assertObjectsAreDirted(false);
	}

	/**
	 * Test adding a local object from an inherited folder into the content package
	 * and then re-exporting everything.
	 * @throws NodeException
	 */
	@Test
	public void testReexportLocalObjectInInheritedFolder() throws NodeException {
		Folder masterFolder = create(Folder.class, f -> {
			f.setMotherId(node.getFolder().getId());
			f.setName("Master Folder");
		}).build();

		String masterFolderGlobalId = execute(Folder::getGlobalId, masterFolder).toString();

		T localObject = createLocalObject(masterFolder);
		String localObjectGlobalId = execute(NodeObject::getGlobalId, localObject).toString();

		String packageName = "TestPackage";
		createContentPackage(packageName);
		assertResponseOK(new ContentStagingResourceImpl().add(packageName, type, localObjectGlobalId, false));

		// local object should be included in the package
		IdTypeMapRequest request = new IdTypeMapRequest();
		Map<String, List<String>> ids = new HashMap<>();
		ids.computeIfAbsent(type, k -> new ArrayList<>()).add(localObjectGlobalId);
		ids.computeIfAbsent("folder", k -> new ArrayList<>()).add(masterFolderGlobalId);
		request.setIds(ids);
		StagingStatusResponse statusResponse = new ContentStagingResourceImpl().status(packageName, request);
		assertResponseOK(statusResponse);
		assertThat(statusResponse.getStagingStatus()).as("Status info before export")
			.containsEntry(localObjectGlobalId, new StagingStatus().setPackageName(packageName).setIncluded(true))
			.containsEntry(masterFolderGlobalId, new StagingStatus().setPackageName(packageName).setIncluded(false));

		// export (again)
		assertResponseOK(new ContentStagingResourceImpl().exportFromCms(packageName, 0));

		// local object should still be included in the package
		statusResponse = new ContentStagingResourceImpl().status(packageName, request);
		assertResponseOK(statusResponse);
		assertThat(statusResponse.getStagingStatus()).as("Status info after export")
			.containsEntry(localObjectGlobalId, new StagingStatus().setPackageName(packageName).setIncluded(true))
			.containsEntry(masterFolderGlobalId, new StagingStatus().setPackageName(packageName).setIncluded(false));
	}

	/**
	 * Test adding a local object from a local folder into the content package
	 * and then re-exporting everything.
	 * @throws NodeException
	 */
	@Test
	public void testReexportLocalObjectInLocalFolder() throws NodeException {
		Folder localFolder = create(Folder.class, f -> {
			f.setMotherId(node.getFolder().getId());
			f.setName("Channel Folder");
			f.setChannelInfo(channel.getId(), null);
		}).build();

		String localFolderGlobalId = execute(Folder::getGlobalId, localFolder).toString();

		T localObject = createLocalObject(localFolder);
		String localObjectGlobalId = execute(NodeObject::getGlobalId, localObject).toString();

		String packageName = "TestPackage";
		createContentPackage(packageName);
		assertResponseOK(new ContentStagingResourceImpl().add(packageName, type, localObjectGlobalId, false));

		// local folder and object should be included in the package
		IdTypeMapRequest request = new IdTypeMapRequest();
		Map<String, List<String>> ids = new HashMap<>();
		ids.computeIfAbsent(type, k -> new ArrayList<>()).add(localObjectGlobalId);
		ids.computeIfAbsent("folder", k -> new ArrayList<>()).add(localFolderGlobalId);
		request.setIds(ids);
		StagingStatusResponse statusResponse = new ContentStagingResourceImpl().status(packageName, request);
		assertResponseOK(statusResponse);
		assertThat(statusResponse.getStagingStatus()).as("Status info before export")
			.containsEntry(localObjectGlobalId, new StagingStatus().setPackageName(packageName).setIncluded(true))
			.containsEntry(localFolderGlobalId, new StagingStatus().setPackageName(packageName).setIncluded(true));

		// export (again)
		assertResponseOK(new ContentStagingResourceImpl().exportFromCms(packageName, 0));

		// local folder and object should still be included in the package
		statusResponse = new ContentStagingResourceImpl().status(packageName, request);
		assertResponseOK(statusResponse);
		assertThat(statusResponse.getStagingStatus()).as("Status info after export")
			.containsEntry(localObjectGlobalId, new StagingStatus().setPackageName(packageName).setIncluded(true))
			.containsEntry(localFolderGlobalId, new StagingStatus().setPackageName(packageName).setIncluded(true));
	}

	/**
	 * Test importing a local channel object
	 * @throws NodeException
	 */
	@Test
	public void testImportLocalObject() throws NodeException {
		// create object
		T stagedObject = createLocalObject(supply(() -> node.getFolder()));
		String objectGlobalId = execute(f -> f.getGlobalId().toString(), stagedObject);

		// create package and add object
		String packageName = "TestPackage";
		createContentPackage(packageName);
		assertResponseOK(new ContentStagingResourceImpl().add(packageName, type, objectGlobalId, false));

		// delete object in CMS
		stagedObject = delete(stagedObject);

		cleanDirtQueueAndPublishQueue();

		// import the package
		importContentPackage(packageName);

		// reload and assert
		stagedObject = supply(t -> t.getObject(clazz, objectGlobalId));
		consume(this::assertLocalObject, stagedObject, channel, execute(Node::getFolder, node));

		testContext.waitForDirtqueueWorker();

		expectAllDirted(channel, stagedObject);
		expectAllDirted(subChannel, stagedObject);
		assertObjectsAreDirted();
	}

	/**
	 * Test importing a master together with a localized object
	 * @throws NodeException
	 */
	@Test
	public void testImportMasterAndLocalizedObject() throws NodeException {
		// create master
		T masterObject = createMasterObject(supply(() -> node.getFolder()));
		String masterGlobalId = execute(o -> o.getGlobalId().toString(), masterObject);

		// localize master
		T localizedObject = localizeObject(masterObject);
		String localizedGlobalId = execute(o -> o.getGlobalId().toString(), localizedObject);

		// create package and add objects
		String packageName = "TestPackage";
		createContentPackage(packageName);
		assertResponseOK(new ContentStagingResourceImpl().add(packageName, type, localizedGlobalId, false));
		assertResponseOK(new ContentStagingResourceImpl().add(packageName, type, masterGlobalId, false));

		// delete objects in CMS
		localizedObject = delete(localizedObject);
		masterObject = delete(masterObject);

		cleanDirtQueueAndPublishQueue();

		// import the package
		importContentPackage(packageName);

		// reload and assert
		masterObject = supply(t -> t.getObject(clazz, masterGlobalId));
		localizedObject = supply(t -> t.getObject(clazz, localizedGlobalId));
		consume(this::assertLocalizedObject, masterObject, localizedObject, execute(Node::getFolder, node));

		testContext.waitForDirtqueueWorker();

		expectAllDirted(node, masterObject);
		expectAllDirted(sideChannel, masterObject);
		expectAllDirted(channel, localizedObject);
		expectAllDirted(subChannel, localizedObject);
	}

	/**
	 * Test importing objects with different inheritance settings
	 * @throws NodeException
	 */
	@Test
	public void testImportInheritanceSettings() throws NodeException {
		Folder folder = create(Folder.class, f -> {
			f.setMotherId(node.getFolder().getId());
			f.setName("Folder");
			f.setPublishDir("/");
		}).build();

		// create objects with different inheritance settings
		T excludedObject = createExcludedObject(folder);
		GlobalId excludedObjectGlobalId = execute(NodeObject::getGlobalId, excludedObject);

		T disinheritedObject = createDisinheritedObject(folder);
		GlobalId disinheritedObjectGlobalId = execute(NodeObject::getGlobalId, disinheritedObject);

		T defaultDisinheritedObject = createDefaultDisinherited(folder);
		GlobalId defaultDisinheritedObjectGlobalId = execute(NodeObject::getGlobalId, defaultDisinheritedObject);

		// create package and add objects
		String packageName = "TestPackage";
		createContentPackage(packageName);
		assertResponseOK(new ContentStagingResourceImpl().add(packageName, type, excludedObjectGlobalId.toString(), false));
		assertResponseOK(new ContentStagingResourceImpl().add(packageName, type, disinheritedObjectGlobalId.toString(), false));
		assertResponseOK(new ContentStagingResourceImpl().add(packageName, type, defaultDisinheritedObjectGlobalId.toString(), false));

		// delete the folder
		consume(f -> f.delete(true), folder);

		cleanDirtQueueAndPublishQueue();

		// import the package
		importContentPackage(packageName);

		// reload and assert
		excludedObject = supply(t -> t.getObject(clazz, excludedObjectGlobalId));
		disinheritedObject = supply(t -> t.getObject(clazz, disinheritedObjectGlobalId));
		defaultDisinheritedObject = supply(t -> t.getObject(clazz, defaultDisinheritedObjectGlobalId));
		consume(this::assertExcluded, excludedObject);
		consume(this::assertDisinherited, disinheritedObject);
		consume(this::assertDefaultDisinherited, defaultDisinheritedObject);

		testContext.waitForDirtqueueWorker();

		expectAllDirted(node, folder, excludedObject, disinheritedObject, defaultDisinheritedObject);
		expectAllDirted(sideChannel, folder, disinheritedObject, defaultDisinheritedObject);
		expectAllDirted(channel, folder, defaultDisinheritedObject);
		expectAllDirted(subChannel, folder, defaultDisinheritedObject);
		assertObjectsAreDirted();
	}

	/**
	 * Test importing a complete node with channel
	 * @throws NodeException
	 */
	@Test
	public void testImportNodeAndChannel() throws NodeException {
		// create a folder structure
		Folder level1 = create(Folder.class, f -> {
			f.setMotherId(node.getFolder().getId());
			f.setName("Level 1");
			f.setPublishDir("/level1");
		}).build();
		String level1GlobalId = execute(f -> f.getGlobalId().toString(), level1);
		Integer level1Id = execute(Folder::getId, level1);

		Folder level2 = create(Folder.class, f -> {
			f.setMotherId(level1Id);
			f.setName("Overwritten Level 2");
			f.setPublishDir("/level2");
		}).build();
		String level2GlobalId = execute(f -> f.getGlobalId().toString(), level2);

		Folder localizedLevel2 = execute(f -> update(localize(f, channel), upd -> upd.setName("Localized Level 2")).build(), level2);
		String localizedLevel2GlobalId = execute(f -> f.getGlobalId().toString(), localizedLevel2);

		Folder inheritedLevel2 = create(Folder.class, f -> {
			f.setMotherId(level1Id);
			f.setName("Inherited Level 2");
			f.setPublishDir("/level2");
		}).build();
		String inheritedLevel2GlobalId = execute(f -> f.getGlobalId().toString(), inheritedLevel2);

		Folder localLevel2 = create(Folder.class, f -> {
			f.setMotherId(level1Id);
			f.setName("Local level 2");
			f.setPublishDir("/local/level2");
			f.setChannelInfo(channel.getId(), null);
		}).build();
		String localLevel2GlobalId = execute(f -> f.getGlobalId().toString(), localLevel2);

		// create some objects in overwritten folder
		T masterObject1 = createMasterObject(level2);
		String master1GlobalId = execute(o -> o.getGlobalId().toString(), masterObject1);
		T localized1 = localizeObject(masterObject1);
		String localized1GlobalId = execute(o -> o.getGlobalId().toString(), localized1);
		T localObject1 = createLocalObject(level2);
		String local1GlobalId = execute(o -> o.getGlobalId().toString(), localObject1);

		// create some objects in inherited folder
		T masterObject2 = createMasterObject(inheritedLevel2);
		String master2GlobalId = execute(o -> o.getGlobalId().toString(), masterObject2);
		T localized2 = localizeObject(masterObject2);
		String localized2GlobalId = execute(o -> o.getGlobalId().toString(), localized2);
		T localObject2 = createLocalObject(inheritedLevel2);
		String local2GlobalId = execute(o -> o.getGlobalId().toString(), localObject2);

		// create local object in local folder
		T localObject3 = createLocalObject(localLevel2);
		String local3GlobalId = execute(o -> o.getGlobalId().toString(), localObject3);

		String nodeGlobalId = execute(n -> n.getGlobalId().toString(), node);
		String channelGlobalId = execute(c -> c.getGlobalId().toString(), channel);

		// create package and add node and channel recursively
		String packageName = "TestPackage";
		createContentPackage(packageName);
		assertResponseOK(new ContentStagingResourceImpl().add(packageName, "node", nodeGlobalId, true));
		assertResponseOK(new ContentStagingResourceImpl().add(packageName, "node", channelGlobalId, true));

		// delete the channels and node
		consume(f -> f.delete(true), sideChannel);
		consume(f -> f.delete(true), subChannel);
		consume(f -> f.delete(true), channel);
		consume(f -> f.delete(true), node);

		cleanDirtQueueAndPublishQueue();

		// import the package
		importContentPackage(packageName);

		// reload and assert
		node = supply(t -> t.getObject(Node.class, nodeGlobalId));
		channel = supply(t -> t.getObject(Node.class, channelGlobalId));

		level1 = supply(t -> t.getObject(Folder.class, level1GlobalId));
		level2 = supply(t -> t.getObject(Folder.class, level2GlobalId));
		localizedLevel2 = supply(t -> t.getObject(Folder.class, localizedLevel2GlobalId));
		inheritedLevel2 = supply(t -> t.getObject(Folder.class, inheritedLevel2GlobalId));
		localLevel2 = supply(t -> t.getObject(Folder.class, localLevel2GlobalId));

		masterObject1 = supply(t -> t.getObject(clazz, master1GlobalId));
		localized1 = supply(t -> t.getObject(clazz, localized1GlobalId));
		localObject1 = supply(t -> t.getObject(clazz, local1GlobalId));

		masterObject2 = supply(t -> t.getObject(clazz, master2GlobalId));
		localized2 = supply(t -> t.getObject(clazz, localized2GlobalId));
		localObject2 = supply(t -> t.getObject(clazz, local2GlobalId));

		localObject3 = supply(t -> t.getObject(clazz, local3GlobalId));

		consume(f -> assertThat(f).as("Folder level 1").isNotNull().isMaster(), level1);
		consume((m, l) -> assertThat(m).as("Folder level2").isNotNull().isMaster().isLocalizedAs(channel, l), level2, localizedLevel2);
		consume(f -> assertThat(f).as("Inherited folder").isNotNull().isMaster().isInheritedIn(channel), inheritedLevel2);

		consume(this::assertLocalizedObject, masterObject1, localized1, level2);
		consume(this::assertLocalObject, localObject1, channel, level2);

		consume(this::assertLocalizedObject, masterObject2, localized2, inheritedLevel2);
		consume(this::assertLocalObject, localObject2, channel, inheritedLevel2);

		consume(this::assertLocalObject, localObject3, channel, localLevel2);

		testContext.waitForDirtqueueWorker();

		expectAllDirted(node, node, level1, level2, inheritedLevel2, masterObject1, masterObject2);
		expectAllDirted(channel, channel, level1, localizedLevel2, inheritedLevel2, localized1, localized2, localLevel2, localObject1, localObject2, localObject3);
		assertObjectsAreDirted(Arrays.asList(node, channel));
	}

	/**
	 * Test importing an object, which has been moved between channels
	 * @throws NodeException
	 */
	@Test
	public void testMovedBetweenChannels() throws NodeException {
		Folder folder = create(Folder.class, f -> {
			f.setMotherId(nodeRootFolder.getId());
			f.setName("Folder");
			f.setPublishDir("/");
		}).build();

		T stagedObject = createLocalObject(folder);
		GlobalId stagedObjectGlobalId = execute(NodeObject::getGlobalId, stagedObject);

		// create package and add object
		String packageName = "TestPackage";
		createContentPackage(packageName);
		assertResponseOK(new ContentStagingResourceImpl().add(packageName, type, stagedObjectGlobalId.toString(), false));

		// move object into side channel
		stagedObject = update(stagedObject, o -> {
			o.setFolder(sideChannel, folder);
		}).build();
		consume(this::assertLocalObject, stagedObject, sideChannel, folder);

		cleanDirtQueueAndPublishQueue();

		// import the package
		importContentPackage(packageName);

		// reload and assert
		stagedObject = execute(NodeObject::reload, stagedObject);
		consume(this::assertLocalObject, stagedObject, channel, folder);

		testContext.waitForDirtqueueWorker();

		expectAllDirted(channel, stagedObject);
		expectAllDirted(subChannel, stagedObject);

		// right now, when the moved object is a folder, also the parent folder will be dirted
		if (clazz.isAssignableFrom(Folder.class)) {
			expectAllDirted(node, folder);
			expectAllDirted(sideChannel, folder);
			expectAllDirted(channel, folder);
			expectAllDirted(subChannel, folder);
		}

		assertObjectsAreDirted();
	}

	/**
	 * Create a master object for staging
	 * @param folder mother folder
	 * @return object
	 * @throws NodeException
	 */
	protected abstract T createMasterObject(Folder folder) throws NodeException;

	/**
	 * Localize the given master in the channel
	 * @param master master
	 * @return localized copy
	 * @throws NodeException
	 */
	protected abstract T localizeObject(T master) throws NodeException;

	/**
	 * Create a local object for staging in the channel
	 * @param folder mother folder
	 * @return object
	 * @throws NodeException
	 */
	protected abstract T createLocalObject(Folder folder) throws NodeException;

	/**
	 * Create an excluded object in the folder
	 * @param folder folder
	 * @return object
	 * @throws NodeException
	 */
	protected abstract T createExcludedObject(Folder folder) throws NodeException;

	/**
	 * Create an object in the folder, which is disinherited in the channel
	 * @param folder folder
	 * @return object
	 * @throws NodeException
	 */
	protected abstract T createDisinheritedObject(Folder folder) throws NodeException;

	/**
	 * Create an object in the folder, which is disinherited by default
	 * @param folder folder
	 * @return object
	 * @throws NodeException
	 */
	protected abstract T createDefaultDisinherited(Folder folder) throws NodeException;

	/**
	 * Assert that the object in the CMS exists and is a master in the given folder
	 * @param object object to check
	 * @param folder asserted folder
	 * @throws NodeException
	 */
	protected abstract void assertMasterObject(T object, Folder folder) throws NodeException;

	/**
	 * Assert that the given objects are master and localized copy in the given folder
	 * @param masterObject master
	 * @param localizedObject localized copy
	 * @param folder asserted folder
	 * @throws NodeException
	 */
	protected abstract void assertLocalizedObject(T masterObject, T localizedObject, Folder folder) throws NodeException;

	/**
	 * Assert that the object in the CMS exists and is local to the channel in the given folder
	 * @param object object to check
	 * @param channel expected channel
	 * @param folder asserted folder
	 * @throws NodeException
	 */
	protected abstract void assertLocalObject(T object, Node channel, Folder folder) throws NodeException;

	/**
	 * Assert that the object is excluded
	 * @param object object
	 * @throws NodeException
	 */
	protected abstract void assertExcluded(T object) throws NodeException;

	/**
	 * Assert that the object is disinherited in the channel
	 * @param object object
	 * @throws NodeException
	 */
	protected abstract void assertDisinherited(T object) throws NodeException;

	/**
	 * Assert that the object is disinherited by default
	 * @param object object
	 * @throws NodeException
	 */
	protected abstract void assertDefaultDisinherited(T object) throws NodeException;

	/**
	 * Clean dirtqueue and publishqueue
	 * @throws NodeException
	 */
	protected void cleanDirtQueueAndPublishQueue() throws NodeException {
		operate(() -> {
			DBUtils.update("DELETE FROM dirtqueue");
			DBUtils.update("DELETE FROM publishqueue");
		});
	}

	/**
	 * Expect the given objects to be dirted for the node
	 * @param node node
	 * @param objects array of objects expected to be dirted
	 * @throws NodeException
	 */
	protected void expectAllDirted(Node node, NodeObject...objects) throws NodeException {
		for (NodeObject o : objects) {
			expectDirted(node, o);
		}
	}

	/**
	 * Expect the single object to be dirted for the node
	 * @param node node
	 * @param object object expected to be dirted
	 * @throws NodeException
	 */
	protected void expectDirted(Node node, NodeObject object) throws NodeException {
		if (object instanceof Node) {
			Node n = Node.class.cast(object);
			Folder f = null;
			try (Trx trx = new Trx(); ChannelTrx cTrx = new ChannelTrx(n)) {
				f = n.getFolder();
				trx.success();
			}
			expectDirted(node, f);
		} else if (object instanceof Folder) {
			expectedDirtedFolders.computeIfAbsent(node, k -> new HashSet<>()).add(Folder.class.cast(object));
		} else if (object instanceof Page) {
			expectedDirtedPages.computeIfAbsent(node, k -> new HashSet<>()).add(Page.class.cast(object));
		} else if (object instanceof File) {
			expectedDirtedFiles.computeIfAbsent(node, k -> new HashSet<>()).add(File.class.cast(object));
		}
	}

	/**
	 * Assert that exactly the objects, which were passed into
	 * {@link #expectAllDirted(Node, NodeObject...)} or
	 * {@link #expectDirted(Node, NodeObject)} are dirted for the node and all
	 * channels
	 * 
	 * @throws NodeException
	 */
	protected void assertObjectsAreDirted() throws NodeException {
		assertObjectsAreDirted(true);
	}

	/**
	 * Assert that the objects, which were passed into
	 * {@link #expectAllDirted(Node, NodeObject...)} or
	 * {@link #expectDirted(Node, NodeObject)} are dirted for the node and all
	 * channels
	 * 
	 * @param strict true for strict test (exactly the objects are dirted)
	 * @throws NodeException
	 */
	protected void assertObjectsAreDirted(boolean strict) throws NodeException {
		assertObjectsAreDirted(Arrays.asList(node, sideChannel, channel, subChannel), strict);
	}

	/**
	 * Assert that exactly the objects, which were passed into
	 * {@link #expectAllDirted(Node, NodeObject...)} or
	 * {@link #expectDirted(Node, NodeObject)} are dirted for the given list of nodes/channels
	 *
	 * @param nodes checked nodes/channels
	 * @throws NodeException
	 */
	protected void assertObjectsAreDirted(List<Node> nodes) throws NodeException {
		assertObjectsAreDirted(nodes, true);
	}

	/**
	 * Assert that the objects, which were passed into
	 * {@link #expectAllDirted(Node, NodeObject...)} or
	 * {@link #expectDirted(Node, NodeObject)} are dirted for the given list of nodes/channels
	 *
	 * @param nodes checked nodes/channels
	 * @param strict true for strict test (exactly the objects are dirted)
	 * @throws NodeException
	 */
	protected void assertObjectsAreDirted(List<Node> nodes, boolean strict) throws NodeException {
		operate(() -> {
			for (Node dirtNode : nodes) {
				Set<Folder> folderSet = expectedDirtedFolders.getOrDefault(dirtNode, Collections.emptySet());
				if (strict) {
					assertDirtedObjects(Folder.class, dirtNode, (Folder[]) folderSet.toArray(new Folder[folderSet.size()]));
				} else {
					assertDirtedObjectsNonStrict(Folder.class, dirtNode, (Folder[]) folderSet.toArray(new Folder[folderSet.size()]));
				}

				Set<Page> pageSet = expectedDirtedPages.getOrDefault(dirtNode, Collections.emptySet());
				if (strict) {
					assertDirtedObjects(Page.class, dirtNode, (Page[]) pageSet.toArray(new Page[pageSet.size()]));
				} else {
					assertDirtedObjectsNonStrict(Page.class, dirtNode, (Page[]) pageSet.toArray(new Page[pageSet.size()]));
				}

				Set<File> fileSet = expectedDirtedFiles.getOrDefault(dirtNode, Collections.emptySet());
				if (strict) {
					assertDirtedObjects(File.class, dirtNode, (File[]) fileSet.toArray(new File[fileSet.size()]));
				} else {
					assertDirtedObjectsNonStrict(File.class, dirtNode, (File[]) fileSet.toArray(new File[fileSet.size()]));
				}
			}
		});
	}

	protected T delete(T object) throws NodeException {
		String globalId = execute(o -> o.getGlobalId().toString(), object);
		consume(f -> f.delete(true), object);
		object = supply(t -> t.getObject(clazz, globalId));
		assertThat(object).as("Deleted object").isNull();
		return null;
	}
}
