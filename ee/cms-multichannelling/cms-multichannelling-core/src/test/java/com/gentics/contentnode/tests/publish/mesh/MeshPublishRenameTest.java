package com.gentics.contentnode.tests.publish.mesh;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.publish.mesh.MeshPublisher.getBranchName;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.assertBranch;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.assertMeshProject;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.cleanMesh;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.createMeshCR;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.isChannelBranch;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createChannel;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.junit.After;
import org.junit.Before;
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
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.publish.mesh.MeshPublisher;
import com.gentics.contentnode.tests.category.MeshTest;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.contentnode.testutils.mesh.MeshContext;
import com.gentics.contentnode.testutils.mesh.MeshTestRule;
import com.gentics.testutils.infrastructure.TestEnvironment;

/**
 * Test cases for renaming master and/or channel nodes
 */
@RunWith(value = Parameterized.class)
@GCNFeature(set = { Feature.MESH_CONTENTREPOSITORY, Feature.MULTICHANNELLING })
@Category(MeshTest.class)
public class MeshPublishRenameTest {
	/**
	 * Schema prefix
	 */
	public final static String SCHEMA_PREFIX = "test";

	public final static String NO_VERSION = "";

	public final static String FIRST_VERSION = "1.0";

	public final static String SECOND_VERSION = "2.0";

	public final static List<String> VERSIONS = Arrays.asList(NO_VERSION, FIRST_VERSION, SECOND_VERSION);

	@ClassRule
	public static DBTestContext context = new DBTestContext();

	@ClassRule
	public static MeshContext mesh = new MeshContext();

	@Rule
	public MeshTestRule meshTestRule = new MeshTestRule(mesh);

	@Parameter(0)
	public String initial;

	@Parameter(1)
	public String current;

	@Parameter(2)
	public String updated;

	private String masterName;

	private String channelName;

	private Node master;

	private Node channel;

	private Integer crId;

	public final static String swap(String version) {
		switch (version) {
		case FIRST_VERSION:
			return SECOND_VERSION;
		case SECOND_VERSION:
			return FIRST_VERSION;
		default:
			return version;
		}
	}

	public final static boolean contains(Collection<Object[]> data, Object[] entry) {
		for (Object[] current : data) {
			if (Arrays.equals(current, entry)) {
				return true;
			}
		}
		return false;
	}

	@Parameters(name = "{index}: initial implementation version ''{0}'', current implementation version ''{1}'', updated implementation version ''{2}''")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (String initial : VERSIONS) {
			for (String current : VERSIONS) {
				for (String updated : VERSIONS) {
					Object[] entry = new Object[] { initial, current, updated };
					Object[] swapped = new Object[] {swap(initial), swap(current), swap(updated)};
					if (!Arrays.equals(entry, swapped) && contains(data, swapped)) {
						continue;
					}

					data.add(entry);
				}
			}
		}

		return data;
	}

	/**
	 * Clean all data from previous test runs and create test data
	 * @throws NodeException
	 */
	@Before
	public void setup() throws Exception {
		cleanMesh(mesh.client());

		masterName = "Master_" + TestEnvironment.getRandomHash(5);
		channelName = "Channel_" + TestEnvironment.getRandomHash(5);

		master = supply(() -> createNode("master", masterName, PublishTarget.CONTENTREPOSITORY));
		channel = supply(() -> createChannel(master, channelName, "channel", "/", PublishTarget.CONTENTREPOSITORY));

		crId = createMeshCR(mesh, SCHEMA_PREFIX);

		operate(t -> update(t.getObject(ContentRepository.class, crId), cr -> {
			cr.setVersion(initial);
			cr.setProjectPerNode(true);
		}));

		operate(() -> update(master, n -> {
			n.setContentrepositoryId(crId);
		}));

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		assertMeshProject(mesh.client(), masterName, SCHEMA_PREFIX, MeshPublisher.getMeshUuid(master), initial, true);
		assertBranch(mesh.client(), masterName, isChannelBranch(channel, initial), true, getBranchName(channelName, initial), "channel", null, false);

		if (!Objects.equals(initial, current)) {
			operate(t -> update(t.getObject(ContentRepository.class, crId), cr -> {
				cr.setVersion(current);
			}));

			try (Trx trx = new Trx()) {
				context.publish(false);
				trx.success();
			}

			assertMeshProject(mesh.client(), masterName, SCHEMA_PREFIX, MeshPublisher.getMeshUuid(master), current, true);
			assertBranch(mesh.client(), masterName, isChannelBranch(channel, current), true, getBranchName(channelName, current), "channel", null, false);
		}

		if (!Objects.equals(current, updated)) {
			operate(t -> update(t.getObject(ContentRepository.class, crId), cr -> {
				cr.setVersion(updated);
			}));
		}
	}

	/**
	 * Delete created test data
	 * @throws NodeException
	 */
	@After
	public void tearDown() throws NodeException {
		if (channel != null) {
			consume(Node::delete, channel);
		}
		if (master != null) {
			consume(Node::delete, master);
		}
		if (crId != null) {
			operate(t -> t.getObject(ContentRepository.class, crId).delete());
		}
	}

	/**
	 * Test renaming the master node
	 * @throws Exception
	 */
	@Test
	public void testRenameMaster() throws Exception {
		String newMasterName = "Updated" + masterName;

		update(master, n -> {
			n.getFolder().setName(newMasterName);
		});

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		assertMeshProject(mesh.client(), newMasterName, SCHEMA_PREFIX, MeshPublisher.getMeshUuid(master), updated, true);
		assertBranch(mesh.client(), newMasterName, isChannelBranch(channel, updated), true, getBranchName(channelName, updated), "channel", null, false);
	}

	/**
	 * Test renaming the channel node
	 * @throws Exception
	 */
	@Test
	public void testRenameChannel() throws Exception {
		String newChannelName = "Updated" + channelName;

		update(channel, n -> {
			n.getFolder().setName(newChannelName);
		});

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		assertMeshProject(mesh.client(), masterName, SCHEMA_PREFIX, MeshPublisher.getMeshUuid(master), updated, true);
		assertBranch(mesh.client(), masterName, isChannelBranch(channel, updated), true, getBranchName(newChannelName, updated), "channel", null, false);
	}

	/**
	 * Test renaming the master and channel nodes
	 * @throws Exception
	 */
	@Test
	public void testRenameMasterAndChannel() throws Exception {
		String newMasterName = "Updated" + masterName;
		String newChannelName = "Updated" + channelName;

		update(master, n -> {
			n.getFolder().setName(newMasterName);
		});
		update(channel, n -> {
			n.getFolder().setName(newChannelName);
		});

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		assertMeshProject(mesh.client(), newMasterName, SCHEMA_PREFIX, MeshPublisher.getMeshUuid(master), updated, true);
		assertBranch(mesh.client(), newMasterName, isChannelBranch(channel, updated), true, getBranchName(newChannelName, updated), "channel", null, false);
	}
}
