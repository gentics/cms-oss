package com.gentics.contentnode.tests.publish.mesh;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.TagmapEntry;
import com.gentics.contentnode.tests.category.MeshTest;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.contentnode.testutils.mesh.MeshContext;
import com.gentics.contentnode.testutils.mesh.MeshTestRule;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.createMeshCR;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.assertj.core.api.Assertions.assertThat;

@GCNFeature(set = { Feature.MESH_CONTENTREPOSITORY, Feature.INSTANT_CR_PUBLISHING })
@Category(MeshTest.class)
public class MicroNodeCrRepairTest {

	public final static String SCHEMA_PREFIX = "test";

	@ClassRule
	public final static DBTestContext context = new DBTestContext();

	@ClassRule
	public final static MeshContext mesh = new MeshContext();

	@Rule
	public MeshTestRule meshTestRule = new MeshTestRule(mesh);

	private static Node node;
	private static Integer crId;

	/**
	 * Create static test data
	 * @throws Exception
	 */
	@BeforeClass
	public static void setupOnce() throws Exception {
		node = Trx.supply(() -> createNode("node1", "Node1", ContentNodeTestDataUtils.PublishTarget.CONTENTREPOSITORY));
		crId = createMeshCR(mesh, SCHEMA_PREFIX);

		Trx.operate(t -> update(t.getObject(ContentRepository.class, crId), cr -> {
			cr.setProjectPerNode(true);
			cr.setInstantPublishing(true);
		}));

		Trx.operate(() -> update(node, n -> n.setContentrepositoryId(crId)));
	}

	@Before
	public void setup() throws NodeException {
		Trx.operate(t -> update(t.getObject(ContentRepository.class, crId), cr -> {
			cr.getEntries().add(create(TagmapEntry.class, entry -> {
				entry.setObject(Page.TYPE_PAGE);
				entry.setAttributeTypeId(TagmapEntry.AttributeType.micronode.getType());
				entry.setTagname("page.tags");
				entry.setMapname("tags");
				entry.setMultivalue(true);
			}, false));
		}));
	}

	@Test
	public void testRepair() throws NodeException {
		boolean repaired = Trx.supply(t -> t.getObject(ContentRepository.class, crId).checkStructure(true));

		assertThat(repaired)
			.as("CR is valid")
			.isTrue();

		Trx.operate(t -> update(t.getObject(ContentRepository.class, crId), cr -> {
			cr.getEntries().add(create(TagmapEntry.class, entry -> {
				entry.setObject(Page.TYPE_PAGE);
				entry.setAttributeTypeId(TagmapEntry.AttributeType.micronode.getType());
				entry.setTagname("cms.tags.dummy");
				entry.setMapname("dummy");
				entry.setMultivalue(false);
			}, false));
		}));

		repaired = Trx.supply(t -> t.getObject(ContentRepository.class, crId).checkStructure(true));

		assertThat(repaired)
			.as("Updated CR is still valid")
			.isTrue();

	}
}
