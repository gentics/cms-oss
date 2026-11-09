package com.gentics.contentnode.tests.publish.mesh;

import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.assertObject;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.assertVersionBranches;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.cleanMesh;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.createMeshCR;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createChannel;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFolder;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.localize;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.publish.mesh.MeshPublisher;
import com.gentics.contentnode.tests.category.MeshTest;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.contentnode.testutils.mesh.MeshContext;
import com.gentics.contentnode.testutils.mesh.MeshTestRule;
import com.gentics.mesh.assertj.MeshAssertions;
import com.gentics.mesh.core.rest.branch.BranchResponse;

/**
 * Test cases for publishing channels into Mesh CR with implementation versions
 */
@GCNFeature(set = { Feature.MESH_CONTENTREPOSITORY, Feature.MULTICHANNELLING })
@Category(MeshTest.class)
public class MeshPublishChannelVersionTest {
	/**
	 * Schema prefix for Mesh CR
	 */
	public final static String SCHEMA_PREFIX = "versionedproject";

	@ClassRule
	public static DBTestContext context = new DBTestContext();

	@ClassRule
	public static MeshContext mesh = new MeshContext();

	public final static String nodeName = "Node";

	public final static String channelName = "Channel";

	private static Node node;

	private static Node channel;

	private static Integer crId;

	@Rule
	public MeshTestRule meshTestRule = new MeshTestRule(mesh);

	@BeforeClass
	public static void setupOnce() throws NodeException {
		node = Trx.supply(() -> createNode("node.com", nodeName, PublishTarget.CONTENTREPOSITORY));
		channel = Trx.supply(() -> createChannel(node, channelName, "channel.com", "/", PublishTarget.CONTENTREPOSITORY));
		crId = createMeshCR(mesh, SCHEMA_PREFIX);
		Trx.operate(t -> update(t.getObject(ContentRepository.class, crId), cr -> cr.setProjectPerNode(true)));

		Trx.operate(() -> update(node, n -> {
			n.setContentrepositoryId(crId);
		}));
		Trx.operate(() -> update(channel, n -> {
			n.setContentrepositoryId(crId);
		}));
	}

	@Before
	public void setup() throws Exception {
		cleanMesh(mesh.client());
		Trx.operate(t -> {
			for (Node n : Arrays.asList(node, channel)) {
				for (Folder folder : n.getFolder().getChildFolders()) {
					t.getObject(folder, true).delete(true);
				}
			}
		});

		Trx.operate(t -> {
			update(t.getObject(ContentRepository.class, crId), cr -> {
				cr.setVersion(null);
			});
		});
	}

	@Test
	public void testSetVersion() throws Exception {
		// run publish process (no version set)
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		List<BranchResponse> branches = mesh.client().findBranches(nodeName).blockingGet().getData();
		assertThat(branches).as("Branch list").hasSize(2);
		Optional<BranchResponse> nodeBranch = branches.stream().filter(b -> nodeName.equals(b.getName())).findFirst();
		assertThat(nodeBranch).as("Branch for node").isPresent();
		MeshAssertions.assertThat(nodeBranch.get()).as("Branch for node").isOnlyTagged();

		Optional<BranchResponse> channelBranch = branches.stream().filter(b -> channelName.equals(b.getName())).findFirst();
		assertThat(channelBranch).as("Branch for channel").isPresent();
		MeshAssertions.assertThat(channelBranch.get()).as("Branch for channel").isOnlyTagged(MeshPublisher.getMeshUuid(channel));

		// set CR version
		Trx.operate(t -> {
			update(t.getObject(ContentRepository.class, crId), cr -> {
				cr.setVersion("1.0");
			});
		});

		// run publish process again
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		assertVersionBranches(mesh.client(), nodeName, true, channel, "1.0");
	}

	@Test
	public void testUpdateVersion() throws Exception {
		// set CR version
		Trx.operate(t -> {
			update(t.getObject(ContentRepository.class, crId), cr -> {
				cr.setVersion("1.0");
			});
		});
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}
		assertVersionBranches(mesh.client(), nodeName, false, channel, "1.0");

		// update CR version
		Trx.operate(t -> {
			update(t.getObject(ContentRepository.class, crId), cr -> {
				cr.setVersion("2.0");
			});
		});
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		assertVersionBranches(mesh.client(), nodeName, false, channel, "1.0", "2.0");
	}

	@Test
	public void testRemoveVersion() throws Exception {
		// set CR version
		Trx.operate(t -> {
			update(t.getObject(ContentRepository.class, crId), cr -> {
				cr.setVersion("1.0");
			});
		});
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}
		assertVersionBranches(mesh.client(), nodeName, false, channel, "1.0");

		// remove version
		Trx.operate(t -> {
			update(t.getObject(ContentRepository.class, crId), cr -> {
				cr.setVersion(null);
			});
		});
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}
		assertVersionBranches(mesh.client(), nodeName, true, channel, "1.0");
	}

	@Test
	public void testBranchMigration() throws Exception {
		// create folder and localize
		Folder masterFolder = Trx.supply(() -> update(createFolder(node.getFolder(), "Version 1.0 Master"), upd -> {
			upd.setPublishDir("bla");
		}));
		Folder localizedFolder = Trx.execute(f -> update(localize(f, channel), upd -> upd.setName("Version 1.0 Channel")), masterFolder);

		// set CR version
		Trx.operate(t -> {
			update(t.getObject(ContentRepository.class, crId), cr -> {
				cr.setVersion("1.0");
			});
		});
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		assertObject("Check master folder in Version 1.0", mesh.client(), nodeName, MeshPublisher.getBranchName(nodeName, "1.0"), masterFolder, true, meshFolder -> {
			MeshAssertions.assertThat(meshFolder).hasStringField("name", "Version 1.0 Master");
		});
		assertObject("Check channel folder in Version 1.0", mesh.client(), nodeName, MeshPublisher.getBranchName(channel, "1.0"), localizedFolder, true, meshFolder -> {
			MeshAssertions.assertThat(meshFolder).hasStringField("name", "Version 1.0 Channel");
		});

		// update CR version
		Trx.operate(t -> {
			update(t.getObject(ContentRepository.class, crId), cr -> {
				cr.setVersion("2.0");
			});
		});
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}
		assertObject("Check master folder in Version 2.0", mesh.client(), nodeName, MeshPublisher.getBranchName(nodeName, "2.0"), masterFolder, true, meshFolder -> {
			MeshAssertions.assertThat(meshFolder).hasStringField("name", "Version 1.0 Master");
		});
		assertObject("Check channel folder in Version 2.0", mesh.client(), nodeName, MeshPublisher.getBranchName(channel, "2.0"), localizedFolder, true, meshFolder -> {
			MeshAssertions.assertThat(meshFolder).hasStringField("name", "Version 1.0 Channel");
		});
	}

	@Test
	public void testSetVersionCheckAndRepair() throws Exception {
		// run publish process (no version set)
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		// set CR version
		Trx.operate(t -> {
			update(t.getObject(ContentRepository.class, crId), cr -> {
				cr.setVersion("1.0");
			});
		});

		// check CR consistency (will fail, this is expected)
		boolean consistencyCheckResult = Trx.supply(t -> t.getObject(ContentRepository.class, crId).checkStructure(false));
		assertThat(consistencyCheckResult).as("Structure consistency check result").isFalse();

		// repair CR consistency (should succeed)
		boolean consistencyRepairResult = Trx.supply(t -> t.getObject(ContentRepository.class, crId).checkStructure(true));
		assertThat(consistencyRepairResult).as("Structure consistency repair result").isTrue();
	}
}
