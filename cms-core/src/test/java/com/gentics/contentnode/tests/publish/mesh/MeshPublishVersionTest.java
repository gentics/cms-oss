package com.gentics.contentnode.tests.publish.mesh;

import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.assertMeshProject;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.assertObject;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.assertSchema;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.cleanMesh;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.createMeshCR;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.getFolderSchemaName;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFolder;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
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
import com.gentics.contentnode.object.TagmapEntry.AttributeType;
import com.gentics.contentnode.publish.mesh.MeshPublisher;
import com.gentics.contentnode.tests.category.MeshTest;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.contentnode.testutils.mesh.MeshTestRule;
import com.gentics.contentnode.testutils.mesh.MeshContext;
import com.gentics.mesh.assertj.MeshAssertions;
import com.gentics.mesh.core.rest.branch.BranchResponse;

/**
 * Test cases for publishing into Mesh CR with implementation versions
 */
@GCNFeature(set = { Feature.MESH_CONTENTREPOSITORY })
@Category(MeshTest.class)
public class MeshPublishVersionTest {
	/**
	 * Name of the mesh project
	 */
	public final static String MESH_PROJECT_NAME = "versionedproject";

	@ClassRule
	public static DBTestContext context = new DBTestContext();

	@ClassRule
	public static MeshContext mesh = new MeshContext();

	private static Node node;

	private static Integer crId;

	@Rule
	public MeshTestRule meshTestRule = new MeshTestRule(mesh);

	/**
	 * Setup static test data
	 * 
	 * @throws NodeException
	 */
	@BeforeClass
	public static void setupOnce() throws Exception {
		node = Trx.supply(() -> createNode("node", "Node", PublishTarget.CONTENTREPOSITORY));
		crId = createMeshCR(mesh, MESH_PROJECT_NAME);

		Trx.operate(() -> update(node, n -> {
			n.setContentrepositoryId(crId);
		}));
	}

	@Before
	public void setup() throws Exception {
		cleanMesh(mesh.client());
		Trx.operate(t -> {
			for (Folder folder : node.getFolder().getChildFolders()) {
				t.getObject(folder, true).delete(true);
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
		assertMeshProject(mesh.client(), MESH_PROJECT_NAME);

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

		// there should be two branches now
		List<BranchResponse> branches = mesh.client().findBranches(MESH_PROJECT_NAME).blockingGet().getData();
		assertThat(branches).as("Branch list").hasSize(2);

		// default branch (not tagged)
		assertBranch(branches, MESH_PROJECT_NAME, false);

		// version 1.0 branch
		assertBranch(branches, MESH_PROJECT_NAME + "_1.0", true, "1.0", MeshPublisher.LATEST_TAG);
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

		List<BranchResponse> branches = mesh.client().findBranches(MESH_PROJECT_NAME).blockingGet().getData();
		assertThat(branches).as("Branch list").hasSize(2);
		assertBranch(branches, MESH_PROJECT_NAME, false);
		assertBranch(branches, MESH_PROJECT_NAME + "_1.0", true, "1.0", MeshPublisher.LATEST_TAG);

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
		branches = mesh.client().findBranches(MESH_PROJECT_NAME).blockingGet().getData();
		assertThat(branches).as("Branch list").hasSize(3);
		assertBranch(branches, MESH_PROJECT_NAME, false);
		assertBranch(branches, MESH_PROJECT_NAME + "_1.0", false, "1.0");
		assertBranch(branches, MESH_PROJECT_NAME + "_2.0", true, "2.0", MeshPublisher.LATEST_TAG);
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

		List<BranchResponse> branches = mesh.client().findBranches(MESH_PROJECT_NAME).blockingGet().getData();
		assertThat(branches).as("Branch list").hasSize(2);
		assertBranch(branches, MESH_PROJECT_NAME, false);
		assertBranch(branches, MESH_PROJECT_NAME + "_1.0", true, "1.0", MeshPublisher.LATEST_TAG);

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
		branches = mesh.client().findBranches(MESH_PROJECT_NAME).blockingGet().getData();
		assertThat(branches).as("Branch list").hasSize(2);
		assertBranch(branches, MESH_PROJECT_NAME, true);
		assertBranch(branches, MESH_PROJECT_NAME + "_1.0", false, "1.0", MeshPublisher.LATEST_TAG);
	}

	@Test
	public void testBranchMigration() throws Exception {
		// create folder
		Folder folder = Trx.supply(() -> createFolder(node.getFolder(), "Version 1.0"));

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

		assertObject("Check folder in Version 1.0", mesh.client(), MESH_PROJECT_NAME, MeshPublisher.getBranchName(MESH_PROJECT_NAME, "1.0"), folder, true, meshFolder -> {
			MeshAssertions.assertThat(meshFolder).hasStringField("name", "Version 1.0");
		});

		// update CR version
		Trx.operate(t -> {
			update(t.getObject(ContentRepository.class, crId), cr -> {
				cr.setVersion("2.0");
			});
		});
		// update folder name
		folder = Trx.execute(f -> update(f, upd -> upd.setName("Version 2.0")), folder);

		// publish
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		assertObject("Check folder in Version 1.0", mesh.client(), MESH_PROJECT_NAME, MeshPublisher.getBranchName(MESH_PROJECT_NAME, "1.0"), folder, true, meshFolder -> {
			MeshAssertions.assertThat(meshFolder).hasStringField("name", "Version 1.0");
		});
		assertObject("Check folder in Version 2.0", mesh.client(), MESH_PROJECT_NAME, MeshPublisher.getBranchName(MESH_PROJECT_NAME, "2.0"), folder, true, meshFolder -> {
			MeshAssertions.assertThat(meshFolder).hasStringField("name", "Version 2.0");
		});

		// update CR version
		Trx.operate(t -> {
			update(t.getObject(ContentRepository.class, crId), cr -> {
				cr.setVersion("3.0");
			});
		});
		// publish
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		assertObject("Check folder in Version 1.0", mesh.client(), MESH_PROJECT_NAME, MeshPublisher.getBranchName(MESH_PROJECT_NAME, "1.0"), folder, true, meshFolder -> {
			MeshAssertions.assertThat(meshFolder).hasStringField("name", "Version 1.0");
		});
		assertObject("Check folder in Version 2.0", mesh.client(), MESH_PROJECT_NAME, MeshPublisher.getBranchName(MESH_PROJECT_NAME, "2.0"), folder, true, meshFolder -> {
			MeshAssertions.assertThat(meshFolder).hasStringField("name", "Version 2.0");
		});
		assertObject("Check folder in Version 3.0", mesh.client(), MESH_PROJECT_NAME, MeshPublisher.getBranchName(MESH_PROJECT_NAME, "3.0"), folder, true, meshFolder -> {
			MeshAssertions.assertThat(meshFolder).hasStringField("name", "Version 2.0");
		});
	}

	@Test
	public void testSchemaVersion() throws Exception {
		// create folder
		Folder folder = Trx.supply(() -> createFolder(node.getFolder(), "Version 1.0"));

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

		// initial branch should use version 1.0 of schema
		assertSchema(mesh.client(), MESH_PROJECT_NAME, MeshPublisher.getBranchName(MESH_PROJECT_NAME, null), getFolderSchemaName(MESH_PROJECT_NAME), info -> {
			assertThat(info).as("Schema in initial branch").hasFieldOrPropertyWithValue("version", "1.0");
		});

		// update cr (which will be published onto a new schema version) and set new version
		Trx.operate(t -> {
			update(t.getObject(ContentRepository.class, crId), cr -> {
				cr.addEntry("folder.name", "newtagmapentry", Folder.TYPE_FOLDER, 0, AttributeType.text, false, false, false, false, false);
				cr.setVersion("2.0");
			});
		});
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		// initial branch should still use version 1.0 of schema
		assertSchema(mesh.client(), MESH_PROJECT_NAME, MeshPublisher.getBranchName(MESH_PROJECT_NAME, null), getFolderSchemaName(MESH_PROJECT_NAME), info -> {
			assertThat(info).as("Schema in initial branch").hasFieldOrPropertyWithValue("version", "1.0");
		});
		// new branch should use version 2.0 of schema
		assertSchema(mesh.client(), MESH_PROJECT_NAME, MeshPublisher.getBranchName(MESH_PROJECT_NAME, "2.0"), getFolderSchemaName(MESH_PROJECT_NAME), info -> {
			assertThat(info).as("Schema in new branch").hasFieldOrPropertyWithValue("version", "2.0");
		});

		// folder in initial branch should have schema version 1.0
		assertObject("Folder in initial branch", mesh.client(), MESH_PROJECT_NAME, MeshPublisher.getBranchName(MESH_PROJECT_NAME, "1.0"), folder, true, node -> {
			assertThat(node.getSchema()).as("Schema of node in initial branch").hasFieldOrPropertyWithValue("version", "1.0");
		});
		// folder in new branch should have schema version 2.0
		assertObject("Folder in new branch", mesh.client(), MESH_PROJECT_NAME, MeshPublisher.getBranchName(MESH_PROJECT_NAME, "2.0"), folder, true,node -> {
			assertThat(node.getSchema()).as("Schema of node in new branch").hasFieldOrPropertyWithValue("version", "2.0");
		});
	}

	/**
	 * Make assertions about existence and nature of a branch
	 * @param branches list of branches
	 * @param branchName expected branch name
	 * @param latest expected "latest" flag
	 * @param tags expected tags (empty means "expected to have no tags")
	 */
	protected void assertBranch(List<BranchResponse> branches, String branchName, boolean latest, String... tags) {
		Optional<BranchResponse> optionalBranch = branches.stream()
				.filter(b -> StringUtils.equals(b.getName(), branchName)).findFirst();
		assertThat(optionalBranch).as("Branch " + branchName).isNotEmpty();
		MeshAssertions.assertThat(optionalBranch.get()).as("Branch " + branchName).isOnlyTagged(tags);
		if (latest) {
			MeshAssertions.assertThat(optionalBranch.get()).as("Branch " + branchName).isLatest();
		} else {
			MeshAssertions.assertThat(optionalBranch.get()).as("Branch " + branchName).isNotLatest();
		}
	}
}
