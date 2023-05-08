package com.gentics.contentnode.tests.publish.mesh;

import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.assertMeshProject;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.crResource;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.createMeshCR;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.junit.Assert.fail;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.rest.model.ContentRepositoryModel.Status;
import com.gentics.contentnode.rest.model.response.ContentRepositoryResponse;
import com.gentics.contentnode.tests.category.MeshTest;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.contentnode.testutils.mesh.MeshTestRule;
import com.gentics.contentnode.testutils.mesh.MeshContext;
import com.gentics.mesh.core.rest.user.UserAPITokenResponse;
import com.gentics.mesh.core.rest.user.UserResponse;

/**
 * Test for using an ApiToken for accessing the Mesh CR
 */
@GCNFeature(set = { Feature.MESH_CONTENTREPOSITORY })
@Category(MeshTest.class)
public class MeshPublishApiTokenTest {
	/**
	 * Name of the mesh project
	 */
	public final static String MESH_PROJECT_NAME = "testproject";

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
		UserResponse user = mesh.client().me().blockingGet();
		UserAPITokenResponse apiToken = mesh.client().issueAPIToken(user.getUuid()).blockingGet();

		node = Trx.supply(() -> createNode("node", "Node", PublishTarget.CONTENTREPOSITORY));
		crId = createMeshCR(mesh, MESH_PROJECT_NAME);

		Trx.operate(() -> update(node, n -> {
			n.setContentrepositoryId(crId);
		}));

		Trx.operate(t -> update(t.getObject(ContentRepository.class, crId), cr -> {
			cr.setUsername(null);
			cr.setPassword(apiToken.getToken());
		}));
	}

	/**
	 * Test repairing the CR (which must create the project and the schemas)
	 * @throws Exception
	 */
	@Test
	public void testRepair() throws Exception {
		ContentRepositoryResponse response = crResource.repair(Integer.toString(crId), 0);
		ContentNodeRESTUtils.assertResponseOK(response);
		if (response.getContentRepository().getCheckStatus() != Status.ok) {
			fail(response.getContentRepository().getCheckResult());
		}

		assertMeshProject(mesh.client(), MESH_PROJECT_NAME);
	}
}
