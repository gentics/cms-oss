package com.gentics.contentnode.tests.publish.mesh;

import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.assertContains;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.assertDoesNotContain;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.assertMeshProject;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.cleanMesh;
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
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.LocalizableNodeObject;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.OpResult;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.publish.mesh.MeshPublisher;
import com.gentics.contentnode.tests.category.MeshTest;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.tests.utils.TestedInheritance;
import com.gentics.contentnode.tests.utils.MCTestedType;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.contentnode.testutils.mesh.MeshTestRule;
import com.gentics.contentnode.testutils.mesh.MeshContext;
import com.gentics.mesh.core.rest.project.ProjectResponse;

/**
 * Test cases for moving objects in multichannelling environments
 */
@RunWith(value = Parameterized.class)
@GCNFeature(set = { Feature.MESH_CONTENTREPOSITORY, Feature.MULTICHANNELLING, Feature.PUB_DIR_SEGMENT })
@Category(MeshTest.class)
public class MeshPublishChannelMoveTest {
	@ClassRule
	public static DBTestContext context = new DBTestContext();

	@ClassRule
	public static MeshContext mesh = new MeshContext();

	public final static String projectName = "Node";

	public final static String releaseName = "Channel";

	/**
	 * Schema prefix
	 */
	public final static String SCHEMA_PREFIX = "test";

	private static Node node;

	private static Node channel;

	private static Integer crId;

	@Rule
	public MeshTestRule meshTestRule = new MeshTestRule(mesh);

	@Parameters(name = "{index}: Move {1} {0} from {2} folder to {3} folder")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (MCTestedType type : MCTestedType.values()) {
			for (TestedInheritance inheritance : TestedInheritance.values()) {
				for (TestedInheritance sourceInheritance : TestedInheritance.values()) {
					for (TestedInheritance targetInheritance : TestedInheritance.values()) {
						if (sourceInheritance.canContain(inheritance) && targetInheritance.canContain(inheritance)) {
							data.add(new Object[] { type, inheritance, sourceInheritance, targetInheritance });
						}
					}
				}
			}
		}
		return data;
	}

	@BeforeClass
	public static void setupOnce() throws NodeException {
		node = Trx.supply(() -> update(createNode("node.com", projectName, PublishTarget.NONE), n -> n.setPubDirSegment(true)));
		channel = Trx.supply(() -> createChannel(node, releaseName, "channel.com", "/", PublishTarget.NONE));
		crId = createMeshCR(mesh, SCHEMA_PREFIX);
		Trx.operate(t -> update(t.getObject(ContentRepository.class, crId), cr -> cr.setProjectPerNode(true)));

		Trx.operate(() -> update(node, n -> {
			n.setContentrepositoryId(crId);
			n.setPublishContentmap(true);
		}));

		Trx.operate(() -> update(channel, c -> {
			c.setContentrepositoryId(crId);
			c.setPublishContentmap(true);
		}));
	}

	@Parameter(0)
	public MCTestedType type;

	@Parameter(1)
	public TestedInheritance inheritance;

	@Parameter(2)
	public TestedInheritance sourceInheritance;

	@Parameter(3)
	public TestedInheritance targetInheritance;

	@Before
	public void setup() throws Exception {
		cleanMesh(mesh.client());
		Trx.operate(t -> {
			for (Node n : Arrays.asList(node, channel)) {
				try (ChannelTrx cTrx = new ChannelTrx(n)) {
					for (Folder folder : n.getFolder().getChildFolders()) {
						t.getObject(folder, true).delete(true);
					}
				}
			}
		});
	}

	/**
	 * Test moving an object from source to target folder
	 * @throws Exception
	 */
	@Test
	public void test() throws Exception {
		LocalizableNodeObject<?>[] sourceObjects = Trx.supply(() -> sourceInheritance.create(MCTestedType.folder, node, channel));
		Folder sourceMaster = (Folder)sourceObjects[0];
		Folder sourceChannel = (Folder)sourceObjects[1];
		Folder source = sourceMaster != null ? sourceMaster : sourceChannel;

		LocalizableNodeObject<?>[] targetObjects = Trx.supply(() -> targetInheritance.create(MCTestedType.folder, node, channel));
		Folder targetMaster = (Folder)targetObjects[0];
		Folder targetChannel = (Folder)targetObjects[1];
		Folder target = targetMaster != null ? targetMaster : targetChannel;

		LocalizableNodeObject<?>[] objects = Trx.supply(() -> inheritance.createPublished(type, source, channel));
		LocalizableNodeObject<?> masterObject = objects[0];
		LocalizableNodeObject<?> channelObject = objects[1];
		LocalizableNodeObject<?> object = masterObject != null ? masterObject : channelObject;

		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		ProjectResponse project = assertMeshProject(mesh.client(), projectName, SCHEMA_PREFIX);

		assertData(project, projectName, source, sourceInheritance != TestedInheritance.local, object, inheritance != TestedInheritance.local);
		assertData(project, projectName, target, targetInheritance != TestedInheritance.local, object, false);

		assertData(project, releaseName, source, true, object, true);
		assertData(project, releaseName, target, true, object, false);

		// move object
		int targetChannelId = inheritance == TestedInheritance.local ? channel.getId() : node.getId();
		OpResult moveResult = null;
		if (object instanceof Folder) {
			Folder folder = (Folder)object;
			moveResult = Trx.supply(t -> t.getObject(folder, true).move(target, targetChannelId));
		} else if (object instanceof File) {
			File file = (File)object;
			moveResult = Trx.supply(t -> t.getObject(file, true).move(target, targetChannelId));
		} else if (object instanceof Page) {
			Page page = (Page)object;
			moveResult = Trx.supply(t -> t.getObject(page, true).move(target, targetChannelId, true));
		} else {
			moveResult = OpResult.fail(object.getClass(), String.format("Cannot move object of type %s", object.getClass().getName()));
		}

		assertThat(moveResult.isOK()).as(String.format("Move result: %s", moveResult.getMessages())).isTrue();

		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		assertData(project, projectName, source, sourceInheritance != TestedInheritance.local, object, false);
		assertData(project, projectName, target, targetInheritance != TestedInheritance.local, object, inheritance != TestedInheritance.local);

		assertData(project, releaseName, source, true, object, false);
		assertData(project, releaseName, target, true, object, true);
	}

	/**
	 * Assert correctness of data in the Mesh CR
	 * @param project project
	 * @param releaseName name of the release to check
	 * @param folder folder
	 * @param folderExpected true if the folder is expected to exist, false if it is expected to not exist
	 * @param object object
	 * @param objectExpected true if the object is expected to exist in the folder, false if it is expected to not exist
	 * @throws NodeException
	 */
	protected void assertData(ProjectResponse project, String releaseName, NodeObject folder, boolean folderExpected, NodeObject object, boolean objectExpected)
			throws NodeException {
		Trx.operate(() -> {
			if (folderExpected) {
				assertContains(mesh.client(), project.getName(), releaseName, project.getRootNode().getUuid(), getFolderSchemaName(SCHEMA_PREFIX), "en",
						folder);
				if (objectExpected) {
					assertContains(mesh.client(), project.getName(), releaseName, MeshPublisher.getMeshUuid(folder), getSchemaName(), "en", object);
				} else {
					assertDoesNotContain(mesh.client(), project.getName(), releaseName, MeshPublisher.getMeshUuid(folder), getSchemaName(), "en", object);
				}
			} else {
				assertDoesNotContain(mesh.client(), project.getName(), releaseName, project.getRootNode().getUuid(), getFolderSchemaName(SCHEMA_PREFIX), "en",
						folder);
			}
		});
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
