package com.gentics.contentnode.tests.publish.mesh;

import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.assertNewJobs;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.cleanMesh;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.crResource;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.createMeshCR;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.jobs;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.newJobs;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartTypeId;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.TagmapEntry.AttributeType;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.parttype.CheckboxPartType;
import com.gentics.contentnode.object.parttype.ShortTextPartType;
import com.gentics.contentnode.tests.category.MeshTest;
import com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.Job;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.contentnode.testutils.mesh.MeshContext;
import com.gentics.contentnode.testutils.mesh.MeshTestRule;
import com.gentics.mesh.core.rest.job.JobResponse;
import com.gentics.mesh.core.rest.job.JobType;

@GCNFeature(set = { Feature.MESH_CONTENTREPOSITORY, Feature.INSTANT_CR_PUBLISHING })
@Category(MeshTest.class)
public class MeshPublishMicroschemaUpdateTest {
	@ClassRule
	public static DBTestContext context = new DBTestContext();

	@ClassRule
	public static MeshContext mesh = new MeshContext();

	private static Node node;

	/**
	 * Schema prefix
	 */
	public final static String SCHEMA_PREFIX = "test";

	public final static String projectName = "Node";

	@Rule
	public MeshTestRule meshTestRule = new MeshTestRule(mesh);

	@BeforeClass
	public static void setupOnce() throws NodeException {
		Trx.operate(t -> {
			for (Node n : t.getObjects(Node.class, DBUtils.select("SELECT id FROM node", DBUtils.IDS))) {
				n.delete(true);
			}
		});

		node = Trx.supply(() -> createNode("node.com", projectName, PublishTarget.CONTENTREPOSITORY));
	}

	private Integer crId;

	private Integer constructId;

	@Before
	public void setup() throws NodeException {
		crId = createMeshCR(mesh, SCHEMA_PREFIX);
		Trx.operate(t -> update(t.getObject(ContentRepository.class, crId), cr -> {
			cr.setProjectPerNode(true);
			cr.setInstantPublishing(true);

			cr.addEntry("bla", "bla", Page.TYPE_PAGE, 0, AttributeType.micronode, false, false, false, false, false);
		}));

		constructId = Trx.supply(() -> createConstruct(node, ShortTextPartType.class, "construct", "part"));

		Trx.operate(() -> update(node, update -> {
			update.setPublishDir("/prefix");
			update.setContentrepositoryId(crId);
		}));
	}

	@After
	public void tearDown() throws NodeException {
		cleanMesh(mesh.client());

		if (constructId != null) {
			Trx.operate(t -> update(t.getObject(Construct.class, constructId), Construct::delete));
			constructId = null;
		}

		if (crId != null) {
			Trx.operate(t -> update(t.getObject(ContentRepository.class, crId), ContentRepository::delete));
			crId = null;
		}
	}

	@Test
	public void testAddPart() throws Exception {
		// repair CR (which will generate schemas) and assert that no migration jobs are generated
		List<JobResponse> initialJobs = jobs(mesh.client());
		ContentNodeRESTUtils.assertResponseOK(crResource.repair(Integer.toString(crId), 0));
		assertThat(newJobs(mesh.client(), initialJobs)).isEmpty();

		// update the construct (add a part)
		Trx.operate(t -> update(t.getObject(Construct.class, constructId), cr -> {
			cr.getParts().add(create(Part.class, part -> {
				part.setEditable(1);
				part.setHidden(false);
				part.setKeyname("newpart");
				part.setName("newpart", 1);
				part.setPartTypeId(getPartTypeId(CheckboxPartType.class));
				part.setDefaultValue(t.createObject(Value.class));

			}, false));
		}));

		// repair again
		ContentNodeRESTUtils.assertResponseOK(crResource.repair(Integer.toString(crId), 0));
		assertNewJobs(mesh.client(), initialJobs, new Job(JobType.microschema, SCHEMA_PREFIX + "_construct", projectName, "1.0", "2.0"));
	}
}
