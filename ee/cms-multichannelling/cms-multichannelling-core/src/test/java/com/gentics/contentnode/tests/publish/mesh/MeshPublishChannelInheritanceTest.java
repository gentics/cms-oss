package com.gentics.contentnode.tests.publish.mesh;

import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.assertObject;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.createMeshCR;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createChannel;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFolder;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
import com.gentics.contentnode.object.Disinheritable;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.publish.mesh.MeshPublisher;
import com.gentics.contentnode.tests.category.MeshTest;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.tests.utils.TestedMCRestriction;
import com.gentics.contentnode.tests.utils.MCTestedType;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.contentnode.testutils.mesh.MeshContext;
import com.gentics.contentnode.testutils.mesh.MeshTestRule;

/**
 * Test cases for multichannelling inheritance settings
 */
@RunWith(value = Parameterized.class)
@GCNFeature(set = { Feature.MESH_CONTENTREPOSITORY, Feature.MULTICHANNELLING, Feature.PUB_DIR_SEGMENT })
@Category(MeshTest.class)
public class MeshPublishChannelInheritanceTest {
	@ClassRule
	public static DBTestContext context = new DBTestContext();

	@ClassRule
	public static MeshContext mesh = new MeshContext();

	private static Node node;

	private static Node channel1;

	private static Node channel11;

	private static Node channel12;

	private static Node channel2;

	private static Node channel21;

	private static Node channel22;

	private static Integer crId;

	private static Set<Node> disinherited = new HashSet<>();

	/**
	 * Schema prefix
	 */
	public final static String SCHEMA_PREFIX = "test";

	public final static String projectName = "Node";

	public final static String release1Name = "Channel 1";

	public final static String release11Name = "Channel 1 1";

	public final static String release12Name = "Channel 1 2";

	public final static String release2Name = "Channel 2";

	public final static String release21Name = "Channel 2 1";

	public final static String release22Name = "Channel 2 2";

	/**
	 * Map of objects
	 */
	protected static Map<MCTestedType, Map<TestedMCRestriction, Disinheritable<?>>> objectMap = new HashMap<>();

	private static Folder folder;

	private static Template template;

	@Rule
	public MeshTestRule meshTestRule = new MeshTestRule(mesh);

	@BeforeClass
	public static void setupOnce() throws Exception {
		Trx.operate(t -> {
			for (Node n : t.getObjects(Node.class, DBUtils.select("SELECT id FROM node", DBUtils.IDS))) {
				n.delete(true);
			}
		});

		Trx.operate(() -> {
			DBUtils.executeUpdate("DELETE FROM dirtqueue", new Object[0]);
			DBUtils.executeUpdate("DELETE FROM publishqueue", new Object[0]);
		});

		node = Trx.supply(() -> update(createNode("node.com", projectName, PublishTarget.NONE), n -> n.setPubDirSegment(true)));
		channel1 = Trx.supply(() -> createChannel(node, release1Name, "channel1.com", "/"));
		channel11 = Trx.supply(() -> createChannel(channel1, release11Name, "channel11.com", "/"));
		channel12 = Trx.supply(() -> createChannel(channel1, release12Name, "channel12.com", "/"));
		channel2 = Trx.supply(() -> createChannel(node, release2Name, "channel2.com", "/"));
		channel21 = Trx.supply(() -> createChannel(channel2, release21Name, "channel21.com", "/"));
		channel22 = Trx.supply(() -> createChannel(channel2, release22Name, "channel22.com", "/"));

		disinherited.add(channel1);
		disinherited.add(channel21);

		crId = createMeshCR(mesh, SCHEMA_PREFIX);
		Trx.operate(t -> update(t.getObject(ContentRepository.class, crId), cr -> cr.setProjectPerNode(true)));

		Trx.operate(() -> update(node, n -> {
			n.setContentrepositoryId(crId);
			n.setPublishContentmap(true);
		}));

		for (Node channel : Arrays.asList(channel1, channel11, channel12, channel2, channel21, channel22)) {
			Trx.operate(() -> update(channel, n -> {
				n.setContentrepositoryId(crId);
				n.setPublishContentmap(true);
			}));
		}

		folder = Trx.supply(() -> createFolder(node.getFolder(), "Folder"));
		template = Trx.supply(() -> createTemplate(node.getFolder(), "Template"));

		// create all sorts of objects with inheritance settings
		for (MCTestedType type : MCTestedType.values()) {
			for (TestedMCRestriction restriction : TestedMCRestriction.values()) {
				objectMap.computeIfAbsent(type, key -> new HashMap<>()).put(restriction, Trx.supply(() -> {
					Disinheritable<?> object = (Disinheritable<?>) type.publish(type.create(folder, template));
					restriction.set(object, disinherited.toArray(new Node[disinherited.size()]));
					return object;
				}));
			}
		}

		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}
	}

	@Parameters(name = "{index}: {1} {0}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (MCTestedType type : MCTestedType.values()) {
			for (TestedMCRestriction restriction : TestedMCRestriction.values()) {
				data.add(new Object[] { type, restriction });
			}
		}
		return data;
	}

	@Parameter(0)
	public MCTestedType type;

	@Parameter(1)
	public TestedMCRestriction restriction;

	@Test
	public void test() throws NodeException {
		Disinheritable<?> testedObject = objectMap.get(type).get(restriction);

		assertObject("", mesh.client(), projectName, projectName, testedObject, true);

		for (Node channel : Arrays.asList(channel1, channel11, channel12, channel2, channel21, channel22)) {
			assertObject("", mesh.client(), projectName, MeshPublisher.getMeshName(channel), testedObject, expect(channel));
		}
	}

	/**
	 * Check whether the object is expected to be published into the given channel
	 * @param channel channel
	 * @return true iff the object is expected
	 * @throws NodeException
	 */
	protected boolean expect(Node channel) throws NodeException {
		switch (restriction) {
		case inherited:
			return true;
		case excluded:
			return false;
		case disinherited:
			return Trx.supply(() -> {
				for (Node disinheritChannel : disinherited) {
					if (channel.equals(disinheritChannel)) {
						return false;
					} else if (channel.isChannelOf(disinheritChannel)) {
						return false;
					}
				}
				return true;
			});
		default:
			throw new NodeException(String.format("Unknown restriction %s", restriction));
		}
	}
}
