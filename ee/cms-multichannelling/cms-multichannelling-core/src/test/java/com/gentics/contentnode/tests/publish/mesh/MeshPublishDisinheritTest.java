package com.gentics.contentnode.tests.publish.mesh;

import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.Builder.update;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.assertObject;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.cleanMesh;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.createMeshCR;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createChannel;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;

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
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.LocalizableNodeObject;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.publish.PublishQueue;
import com.gentics.contentnode.tests.category.MeshTest;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.tests.utils.MCTestedType;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.contentnode.testutils.mesh.MeshContext;
import com.gentics.contentnode.testutils.mesh.MeshTestRule;

/**
 * Test cases for disinheriting/excluding objects in Channels when publishing into a Mesh CR
 */
@RunWith(value = Parameterized.class)
@GCNFeature(set = { Feature.MESH_CONTENTREPOSITORY, Feature.MULTICHANNELLING, Feature.INSTANT_CR_PUBLISHING, Feature.PUB_DIR_SEGMENT })
@Category(MeshTest.class)
public class MeshPublishDisinheritTest {
	@ClassRule
	public static DBTestContext context = new DBTestContext();

	@ClassRule
	public static MeshContext mesh = new MeshContext();

	public final static String SCHEMA_PREFIX = "test";

	public final static String projectName = "Node";

	public final static String channelName = "Channel";

	public final static String subChannelName = "SubChannel";

	public final static String sideChannelName = "SideChannel";

	private static Node node;

	private static Node channel;

	private static Node subChannel;

	private static Node sideChannel;

	private static Template template;

	@Rule
	public MeshTestRule meshTestRule = new MeshTestRule(mesh);

	@BeforeClass
	public static void setupOnce() throws NodeException {
		context.getContext().getTransaction().commit();

		operate(t -> {
			for (Node n : t.getObjects(Node.class, DBUtils.select("SELECT id FROM node", DBUtils.IDS))) {
				n.delete(true);
			}
		});

		operate(() -> {
			DBUtils.executeUpdate("DELETE FROM dirtqueue", new Object[0]);
			DBUtils.executeUpdate("DELETE FROM publishqueue", new Object[0]);
		});

		node = supply(() -> createNode("node.com", projectName, PublishTarget.NONE));
		channel = supply(() -> createChannel(node, channelName, "channel.com", "/"));
		subChannel = supply(() -> createChannel(channel, subChannelName, "sub.channel.com", "/"));
		sideChannel = supply(() -> createChannel(node, sideChannelName, "side.channel.com", "/"));

		int crId = createMeshCR(mesh, SCHEMA_PREFIX);
		update(supply(t -> t.getObject(ContentRepository.class, crId)), upd -> {
			upd.setProjectPerNode(true);
		}).build();

		operate(() -> update(node, upd -> {
			upd.setContentrepositoryId(crId);
			upd.setPublishContentmap(true);
		}).build());

		for (Node n : Arrays.asList(channel, subChannel, sideChannel)) {
			operate(() -> update(n, upd -> {
				upd.setContentrepositoryId(crId);
				upd.setPublishContentmap(true);
			}).build());
		}

		template = supply(() -> createTemplate(node.getFolder(), "Template"));
	}

	@Parameters(name = "{index}: test: {0}, segments: {1}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (MCTestedType type : MCTestedType.values()) {
			for (Boolean segments : Arrays.asList(true, false)) {
				data.add(new Object[] {type, segments});
			}
		}
		return data;
	}

	/**
	 * Type of tested object
	 */
	@Parameter(0)
	public MCTestedType type;

	/**
	 * Flag to use publish directory segments
	 */
	@Parameter(1)
	public boolean segments;

	/**
	 * Tested object
	 */
	private LocalizableNodeObject<?> object;

	/**
	 * Setup the test
	 * @throws Exception
	 */
	@Before
	public void setup() throws Exception {
		// set the flag for "publish directory segments"
		for (Node n : Arrays.asList(node, channel, subChannel, sideChannel)) {
			operate(() -> update(n, upd -> {
				upd.setPubDirSegment(segments);
			}).build());
		}

		// create and publish the tested object
		object = supply(() -> type.create(node.getFolder(), template));
		object = execute(obj -> type.publish(obj), object);

		// dirt all
		operate(() -> {
			int[] nodeIds = new int[] {node.getId(), channel.getId(), subChannel.getId(), sideChannel.getId()};
			// dirt all pages
			PublishQueue.dirtPublishedPages(nodeIds, null, 0, 0, PublishQueue.Action.DEPENDENCY);
			// dirt images and files
			PublishQueue.dirtImagesAndFiles(nodeIds, null, 0, 0, PublishQueue.Action.DEPENDENCY);
			// dirt folders
			PublishQueue.dirtFolders(nodeIds, null, 0, 0, PublishQueue.Action.DEPENDENCY);
		});

		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		// assert that the object exists in all branches
		assertObject(projectName, mesh.client(), projectName, null, object, true);
		for (String branchName : Arrays.asList(channelName, subChannelName, sideChannelName)) {
			assertObject(branchName, mesh.client(), projectName, branchName, object, true);
		}
	}

	@After
	public void tearDown() throws NodeException {
		operate(() -> {
			for (Node n : Arrays.asList(node, channel, subChannel, sideChannel)) {
				ContentNodeTestDataUtils.clear(n);
			}
		});

		cleanMesh(mesh.client());
	}

	/**
	 * Test that excluding an object from inheritance removes it from all branches (except the "main" branch)
	 * @throws Exception
	 */
	@Test
	public void testExclude() throws Exception {
		// exclude tested object
		operate(() -> type.exclude(object));

		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		// assert
		assertObject(projectName, mesh.client(), projectName, null, object, true);
		for (String branchName : Arrays.asList(channelName, subChannelName, sideChannelName)) {
			assertObject(branchName, mesh.client(), projectName, branchName, object, false);
		}
	}

	/**
	 * Test that disinheriting an object from a channel removes it from the branches for the channel (and subchannel), but not from the other branches
	 * @throws Exception
	 */
	@Test
	public void testDisinherit() throws Exception {
		// disinherit tested object from channel (and subchannel)
		operate(() -> type.disinherit(object, channel));

		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		// assert
		assertObject(projectName, mesh.client(), projectName, null, object, true);
		assertObject(channelName, mesh.client(), projectName, channelName, object, false);
		assertObject(subChannelName, mesh.client(), projectName, subChannelName, object, false);
		assertObject(sideChannelName, mesh.client(), projectName, sideChannelName, object, true);
	}

	/**
	 * Test localizing an object
	 * @throws Exception
	 */
	@Test
	public void testLocalize() throws Exception {
		// localize the object in the channel
		LocalizableNodeObject<?> localizedObject = supply(() -> type.localize(object, channel));
		localizedObject = execute(obj -> type.publish(obj), localizedObject);

		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		// assert
		assertObject(projectName, mesh.client(), projectName, null, object, true);
		assertObject(channelName, mesh.client(), projectName, channelName, localizedObject, true);
		assertObject(subChannelName, mesh.client(), projectName, subChannelName, localizedObject, true);
		assertObject(sideChannelName, mesh.client(), projectName, sideChannelName, object, true);
	}
}
