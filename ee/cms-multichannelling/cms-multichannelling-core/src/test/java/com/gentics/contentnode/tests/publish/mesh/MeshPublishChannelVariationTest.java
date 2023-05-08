package com.gentics.contentnode.tests.publish.mesh;

import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.assertConsistent;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.assertContainsOnly;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.assertFolders;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.assertMeshProject;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.assertObject;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.cleanMesh;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.crResource;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.createMeshCR;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.getFileSchemaName;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.getFolderSchemaName;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.getPageSchemaName;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createChannel;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.junit.After;
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
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.LocalizableNodeObject;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.publish.mesh.MeshPublisher;
import com.gentics.contentnode.rest.model.ContentRepositoryModel.Status;
import com.gentics.contentnode.rest.model.response.ContentRepositoryResponse;
import com.gentics.contentnode.tests.category.MeshTest;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.tests.utils.TestedInheritance;
import com.gentics.contentnode.tests.utils.MCTestedType;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.contentnode.testutils.mesh.MeshContext;
import com.gentics.contentnode.testutils.mesh.MeshTestRule;
import com.gentics.mesh.core.rest.project.ProjectResponse;

/**
 * Variations test for publishing multichannelling structures into Mesh CR
 */
@RunWith(value = Parameterized.class)
@GCNFeature(set = { Feature.MESH_CONTENTREPOSITORY, Feature.MULTICHANNELLING, Feature.INSTANT_CR_PUBLISHING })
@Category(MeshTest.class)
public class MeshPublishChannelVariationTest {
	@ClassRule
	public static DBTestContext context = new DBTestContext();

	@ClassRule
	public static MeshContext mesh = new MeshContext();

	public final static String projectName = "Node";

	public final static String releaseName = "Channel";

	private static Node node;

	private static Node channel;

	private static Integer crId;

	/**
	 * Schema prefix
	 */
	public final static String SCHEMA_PREFIX = "test";

	@Rule
	public MeshTestRule meshTestRule = new MeshTestRule(mesh);

	@BeforeClass
	public static void setupOnce() throws NodeException {
		TransactionManager.getCurrentTransaction().commit();

		Trx.operate(t -> {
			for (Node n : t.getObjects(Node.class, DBUtils.select("SELECT id FROM node", DBUtils.IDS))) {
				n.delete(true);
			}
		});

		Trx.operate(() -> {
			DBUtils.executeUpdate("DELETE FROM dirtqueue", new Object[0]);
			DBUtils.executeUpdate("DELETE FROM publishqueue", new Object[0]);
		});

		node = Trx.supply(() -> createNode("node.com", projectName, PublishTarget.NONE));
		channel = Trx.supply(() -> createChannel(node, releaseName, "channel.com", "/"));
		crId = createMeshCR(mesh, SCHEMA_PREFIX);
		Trx.operate(t -> update(t.getObject(ContentRepository.class, crId), cr -> cr.setProjectPerNode(true)));

		Trx.operate(() -> update(node, n -> {
			n.setContentrepositoryId(crId);
			n.setPublishContentmap(true);
		}));

		Trx.operate(() -> update(channel, n -> {
			n.setContentrepositoryId(crId);
			n.setPublishContentmap(true);
		}));
	}

	@Parameters(name = "{index}: {1} {0} in {2} folder")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (MCTestedType type : MCTestedType.values()) {
			for (TestedInheritance inheritance : TestedInheritance.values()) {
				for (TestedInheritance folderInheritance : TestedInheritance.values()) {
					if (folderInheritance == TestedInheritance.local && inheritance != TestedInheritance.local) {
						continue;
					}
					data.add(new Object[] { type, inheritance, folderInheritance });
				}
			}
		}
		return data;
	}

	@Before
	public void setup() throws Exception {
		cleanMesh(mesh.client());
		Trx.operate(t -> update(t.getObject(ContentRepository.class, crId), cr -> cr.setInstantPublishing(false)));
		Trx.operate(t -> {
			for (Node n : Arrays.asList(node, channel)) {
				try (ChannelTrx cTrx = new ChannelTrx(n)) {
					for (Folder folder : n.getFolder().getChildFolders()) {
						t.getObject(folder, true).delete(true);
					}
				}
			}
		});

		ContentRepositoryResponse response = crResource.repair(Integer.toString(crId), 0);
		ContentNodeRESTUtils.assertResponseOK(response);
		if (response.getContentRepository().getCheckStatus() != Status.ok) {
			fail(response.getContentRepository().getCheckResult());
		}
	}

	@After
	public void tearDown() {
		assertConsistent(mesh.client());
	}

	@Parameter(0)
	public MCTestedType type;

	@Parameter(1)
	public TestedInheritance inheritance;

	@Parameter(2)
	public TestedInheritance folderInheritance;

	/**
	 * Test creating objects
	 * @throws Exception
	 */
	@Test
	public void testCreate() throws Exception {
		LocalizableNodeObject<?>[] folders = Trx.supply(() -> folderInheritance.create(MCTestedType.folder, node, channel));
		Folder masterFolder = (Folder)folders[0];
		Folder channelFolder = (Folder)folders[1];
		if (channelFolder == null && folderInheritance == TestedInheritance.inherited) {
			channelFolder = masterFolder;
		}
		Folder folder = masterFolder != null ? masterFolder : channelFolder;
		LocalizableNodeObject<?>[] objects = Trx.supply(() -> inheritance.createPublished(type, folder, channel));
		LocalizableNodeObject<?> masterObject = objects[0];
		LocalizableNodeObject<?> channelObject = objects[1];
		if (channelObject == null && inheritance == TestedInheritance.inherited) {
			channelObject = masterObject;
		}

		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		assertData(masterFolder, channelFolder, masterObject, channelObject);
	}

	/**
	 * Test creating objects with instant publishing
	 * @throws Exception
	 */
	@Test
	public void testCreateInstant() throws Exception {
		Trx.operate(t -> update(t.getObject(ContentRepository.class, crId), cr -> cr.setInstantPublishing(true)));

		LocalizableNodeObject<?>[] folders = Trx.supply(() -> folderInheritance.create(MCTestedType.folder, node, channel));
		Folder masterFolder = (Folder)folders[0];
		Folder channelFolder = (Folder)folders[1];

		Folder folder = masterFolder != null ? masterFolder : channelFolder;
		LocalizableNodeObject<?>[] objects = Trx.supply(() -> inheritance.createPublished(type, folder, channel));
		LocalizableNodeObject<?> masterObject = objects[0];
		LocalizableNodeObject<?> channelObject = objects[1];

		// if an object was published into the channel, and the folder is inherited, the folder will now be published as well into the channel
		if (channelObject != null && channelFolder == null) {
			channelFolder = masterFolder;
		}

		assertData(masterFolder, channelFolder, masterObject, channelObject);
	}

	/**
	 * Test updating objects
	 * @throws Exception
	 */
	@Test
	public void testUpdate() throws Exception {
		LocalizableNodeObject<?>[] folders = Trx.supply(() -> folderInheritance.create(MCTestedType.folder, node, channel));
		Folder masterFolder = (Folder)folders[0];
		Folder channelFolder = (Folder)folders[1];
		if (channelFolder == null && folderInheritance == TestedInheritance.inherited) {
			channelFolder = masterFolder;
		}
		Folder folder = masterFolder != null ? masterFolder : channelFolder;
		LocalizableNodeObject<?>[] objects = Trx.supply(() -> inheritance.createPublished(type, folder, channel));
		LocalizableNodeObject<?> masterObject = objects[0];
		LocalizableNodeObject<?> channelObject = objects[1];
		if (channelObject == null && inheritance == TestedInheritance.inherited) {
			channelObject = masterObject;
		}

		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		assertData(masterFolder, channelFolder, masterObject, channelObject);

		if (masterObject != null) {
			masterObject = Trx.execute(o -> update(o, updated -> updated.setName(String.format("master-%s", updated.getName()))), masterObject);
		}
		if (channelObject != null) {
			if (!channelObject.equals(masterObject)) {
				channelObject = Trx.execute(o -> update(o, updated -> updated.setName(String.format("channel-%s", updated.getName()))), channelObject);
			} else if (masterObject != null) {
				channelObject = masterObject;
			}
		}

		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		assertData(masterFolder, channelFolder, masterObject, channelObject);
	}

	/**
	 * Test updating objects with instant publishing
	 * @throws Exception
	 */
	@Test
	public void testUpdateInstant() throws Exception {
		Trx.operate(t -> update(t.getObject(ContentRepository.class, crId), cr -> cr.setInstantPublishing(true)));

		LocalizableNodeObject<?>[] folders = Trx.supply(() -> folderInheritance.create(MCTestedType.folder, node, channel));
		Folder masterFolder = (Folder)folders[0];
		Folder channelFolder = (Folder)folders[1];

		Folder folder = masterFolder != null ? masterFolder : channelFolder;
		LocalizableNodeObject<?>[] objects = Trx.supply(() -> inheritance.createPublished(type, folder, channel));
		LocalizableNodeObject<?> masterObject = objects[0];
		LocalizableNodeObject<?> channelObject = objects[1];

		// if an object was published into the channel, and the folder is inherited, the folder will now be published as well into the channel
		if (channelObject != null && channelFolder == null) {
			channelFolder = masterFolder;
		}

		assertData(masterFolder, channelFolder, masterObject, channelObject);

		if (masterObject != null) {
			masterObject = Trx.execute(o -> update(o, updated -> updated.setName(String.format("master-%s", updated.getName()))), masterObject);
		}
		if (channelObject != null) {
			if (!channelObject.equals(masterObject)) {
				channelObject = Trx.execute(o -> update(o, updated -> updated.setName(String.format("channel-%s", updated.getName()))), channelObject);
			} else if (masterObject != null) {
				channelObject = masterObject;
			}
		}

		assertData(masterFolder, channelFolder, masterObject, channelObject);
	}

	/**
	 * Test deleting objects
	 * @throws Exception
	 */
	@Test
	public void testDelete() throws Exception {
		LocalizableNodeObject<?>[] folders = Trx.supply(() -> folderInheritance.create(MCTestedType.folder, node, channel));
		Folder masterFolder = (Folder)folders[0];
		Folder channelFolder = (Folder)folders[1];
		if (channelFolder == null && folderInheritance == TestedInheritance.inherited) {
			channelFolder = masterFolder;
		}
		Folder folder = masterFolder != null ? masterFolder : channelFolder;
		LocalizableNodeObject<?>[] objects = Trx.supply(() -> inheritance.createPublished(type, folder, channel));
		LocalizableNodeObject<?> masterObject = objects[0];
		LocalizableNodeObject<?> channelObject = objects[1];
		if (channelObject == null && inheritance == TestedInheritance.inherited) {
			channelObject = masterObject;
		}

		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		assertData(masterFolder, channelFolder, masterObject, channelObject);

		switch(inheritance) {
		case inherited:
			Trx.consume(o -> o.delete(true), masterObject);
			masterObject = null;
			channelObject = null;
			break;
		case localized:
		case local:
			Trx.consume(o -> {
				try (ChannelTrx cTrx = new ChannelTrx(channel)) {
					o.delete(true);
				}
			}, channelObject);
			channelObject = masterObject;
			break;
		}

		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		assertData(masterFolder, channelFolder, masterObject, channelObject);
	}

	/**
	 * Test deleting objects with instant publishing
	 * @throws Exception
	 */
	@Test
	public void testDeleteInstant() throws Exception {
		Trx.operate(t -> update(t.getObject(ContentRepository.class, crId), cr -> cr.setInstantPublishing(true)));

		LocalizableNodeObject<?>[] folders = Trx.supply(() -> folderInheritance.create(MCTestedType.folder, node, channel));
		Folder masterFolder = (Folder)folders[0];
		Folder channelFolder = (Folder)folders[1];

		Folder folder = masterFolder != null ? masterFolder : channelFolder;
		LocalizableNodeObject<?>[] objects = Trx.supply(() -> inheritance.createPublished(type, folder, channel));
		LocalizableNodeObject<?> masterObject = objects[0];
		LocalizableNodeObject<?> channelObject = objects[1];

		// if an object was published into the channel, and the folder is inherited, the folder will now be published as well into the channel
		if (channelObject != null && channelFolder == null) {
			channelFolder = masterFolder;
		}

		assertData(masterFolder, channelFolder, masterObject, channelObject);

		switch(inheritance) {
		case inherited:
			Trx.consume(o -> o.delete(true), masterObject);
			masterObject = null;
			channelObject = null;
			break;
		case localized:
		case local:
			Trx.consume(o -> {
				try (ChannelTrx cTrx = new ChannelTrx(channel)) {
					o.delete(true);
				}
			}, channelObject);
			channelObject = masterObject;
			break;
		}

		assertData(masterFolder, channelFolder, masterObject, channelObject);
	}

	/**
	 * Assert that the expected objects exist in the project
	 * @param masterFolder master folder
	 * @param channelFolder channel folder
	 * @param masterObject optional master object
	 * @param channelObject optional channel object
	 * @throws NodeException
	 */
	protected void assertData(Folder masterFolder, Folder channelFolder, LocalizableNodeObject<?> masterObject, LocalizableNodeObject<?> channelObject)
			throws NodeException {
		Trx.operate(() -> {
			// check project
			ProjectResponse project = assertMeshProject(mesh.client(), projectName, SCHEMA_PREFIX, MeshPublisher.getMeshUuid(node), true);

			// check "master" release
			assertRelease(project, projectName, masterFolder, masterObject);

			// check "channel" release
			assertRelease(project, releaseName, channelFolder, channelObject);
		});
	}

	/**
	 * Assert objects in the release
	 * @param project project
	 * @param release release name
	 * @param folder expected folder (null for expecting no folder)
	 * @param object expected object in the folder (null for expecting no object)
	 * @throws NodeException
	 */
	protected void assertRelease(ProjectResponse project, String release, Folder folder, LocalizableNodeObject<?> object) throws NodeException {
		if (folder != null) {
			assertFolders(mesh.client(), project.getName(), release, SCHEMA_PREFIX, project.getRootNode().getUuid(), folder);
			String folderDescription = release != null ? String.format("Folder in project %s, release %s", project.getName(), release)
					: String.format("Folder in project %s", project.getName());
			assertObject(folderDescription, mesh.client(), project.getName(), release, folder, true);
			if (object != null) {
				String objectDescription = release != null ? String.format("Object in project %s, release %s", project.getName(), release)
						: String.format("Folder in project %s", project.getName());
				assertContainsOnly(mesh.client(), project.getName(), release, MeshPublisher.getMeshUuid(folder), getSchemaName(), "en", object);
				assertObject(objectDescription, mesh.client(), project.getName(), release, object, true, o -> {
					String nameDesc = String.format("name of %s", objectDescription);
					assertThat(o.getFields().getStringField("name").getString()).as(nameDesc).isEqualTo(object.getName());
				});
			} else {
				assertContainsOnly(mesh.client(), project.getName(), release, MeshPublisher.getMeshUuid(folder), getSchemaName(), "en");
			}
		} else {
			assertFolders(mesh.client(), project.getName(), release, SCHEMA_PREFIX, project.getRootNode().getUuid());
		}
	}

	/**
	 * Get the schema name for the tested object type
	 * @return schema name
	 */
	protected String getSchemaName() {
		switch (type) {
		case folder:
			return getFolderSchemaName(SCHEMA_PREFIX);
		case page:
			return getPageSchemaName(SCHEMA_PREFIX);
		case file:
		case image:
			return getFileSchemaName(SCHEMA_PREFIX);
		default:
			fail("Unknown type " + type);
			return null;
		}
	}
}
