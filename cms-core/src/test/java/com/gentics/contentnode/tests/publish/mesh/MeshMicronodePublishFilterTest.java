package com.gentics.contentnode.tests.publish.mesh;

import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.cleanMesh;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.createMeshCR;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.microschemas;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.After;
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
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.TagmapEntry;
import com.gentics.contentnode.object.TagmapEntry.AttributeType;
import com.gentics.contentnode.object.parttype.ShortTextPartType;
import com.gentics.contentnode.tests.category.MeshTest;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.contentnode.testutils.mesh.MeshContext;
import com.gentics.contentnode.testutils.mesh.MeshTestRule;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;

/**
 * Test cases for micronode filter
 */
@GCNFeature(set = { Feature.MESH_CONTENTREPOSITORY, Feature.INSTANT_CR_PUBLISHING })
@Category(MeshTest.class)
public class MeshMicronodePublishFilterTest {
	/**
	 * Schema prefix
	 */
	public final static String SCHEMA_PREFIX = "test";

	@ClassRule
	public final static DBTestContext context = new DBTestContext();

	@ClassRule
	public final static MeshContext mesh = new MeshContext();

	private static Node node;

	private static Node foreignNode;

	private static Integer crId;

	@Rule
	public MeshTestRule meshTestRule = new MeshTestRule(mesh);

	@BeforeClass
	public static void setupOnce() throws Exception {
		node = Trx.supply(() -> createNode("node1", "Node1", PublishTarget.CONTENTREPOSITORY));
		foreignNode = Trx.supply(() -> createNode());
		crId = createMeshCR(mesh, SCHEMA_PREFIX);

		Trx.operate(t -> update(t.getObject(ContentRepository.class, crId), cr -> {
			cr.setProjectPerNode(true);
			cr.setInstantPublishing(true);
		}));

		Trx.operate(() -> update(node, n -> {
			n.setContentrepositoryId(crId);
		}));

		Trx.supply(() -> createConstruct(node, ShortTextPartType.class, "one", "text"));
		Trx.supply(() -> createConstruct(node, ShortTextPartType.class, "two", "text"));
		Trx.supply(() -> createConstruct(node, ShortTextPartType.class, "three", "text"));
		Trx.supply(() -> createConstruct(node, ShortTextPartType.class, "four", "text"));
		Trx.supply(() -> createConstruct(node, ShortTextPartType.class, "five", "text"));
		Trx.supply(() -> createConstruct(foreignNode, ShortTextPartType.class, "foreign", "text"));
	}

	/**
	 * Clean all data from previous test runs
	 * @throws NodeException
	 */
	@After
	public void tearDown() throws NodeException {
		cleanMesh(mesh.client());

		Trx.operate(t -> {
			update(t.getObject(ContentRepository.class, crId), cr -> {
				cr.getEntries().removeIf(entry -> entry.getAttributetype() == AttributeType.micronode);
			});
		});
	}

	@Test
	public void testNoFilter() throws NodeException, InterruptedException {
		test("", "test_one", "test_two", "test_three", "test_four", "test_five");
	}

	@Test
	public void testBlacklist() throws NodeException, InterruptedException {
		test("-three, -five", "test_one", "test_two", "test_four");
	}

	@Test
	public void testWhitelist() throws NodeException, InterruptedException {
		test("one, three, four", "test_one", "test_three", "test_four");
	}

	@Test
	public void testForeign() throws NodeException, InterruptedException {
		test("foreign");
	}

	@Test
	public void testBlackWhitelist() throws NodeException, InterruptedException {
		test("-four, four", "test_one", "test_two", "test_three", "test_five");
	}

	@Test
	public void testWhiteBlacklist() throws NodeException, InterruptedException {
		test("five, -five", "test_five");
	}

	/**
	 * Test filtering
	 * @param filter filter
	 * @param expectedMicroschemaNames list of expected microschema names
	 * @throws NodeException
	 * @throws InterruptedException
	 */
	protected void test(String filter, String...expectedMicroschemaNames) throws NodeException, InterruptedException {
		Trx.operate(t -> update(t.getObject(ContentRepository.class, crId), cr -> {
			cr.getEntries().add(create(TagmapEntry.class, entry -> {
				entry.setObject(Page.TYPE_PAGE);
				entry.setAttributeTypeId(AttributeType.micronode.getType());
				entry.setTagname("page.tags");
				entry.setMapname("tags");
				entry.setMultivalue(true);
				entry.setMicronodeFilter(filter);
			}, false));
		}));

		boolean repaired = Trx.supply(t -> {
			return t.getObject(ContentRepository.class, crId).checkStructure(true);
		});

		if (!repaired) {
			String checkResult = Trx.supply(t -> {
				return t.getObject(ContentRepository.class, crId).getCheckResult();
			});
			fail(String.format("CR Check failed. %s", checkResult));
		}

		List<MicroschemaResponse> microschemas = microschemas(mesh.client()).test().await().assertComplete().values();
		assertThat(microschemas.stream().map(MicroschemaResponse::getName).collect(Collectors.toList())).as("Microschemas")
				.containsOnly(expectedMicroschemaNames);
	}
}
