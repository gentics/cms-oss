package com.gentics.contentnode.tests.publish.mesh;

import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.assertObject;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.cleanMesh;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.createMeshCR;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
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
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.object.FileOnlineStatus;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.parttype.FileURLPartType;
import com.gentics.contentnode.publish.mesh.MeshPublisher;
import com.gentics.contentnode.tests.category.MeshTest;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.contentnode.testutils.mesh.MeshTestRule;
import com.gentics.contentnode.testutils.mesh.MeshContext;

/**
 * Test cases for auto offline when publishing into Mesh CR
 */
@GCNFeature(set = { Feature.MESH_CONTENTREPOSITORY, Feature.CONTENTFILE_AUTO_OFFLINE, Feature.INSTANT_CR_PUBLISHING, Feature.PUB_DIR_SEGMENT })
@RunWith(value = Parameterized.class)
@Category(MeshTest.class)
public class MeshPublishAutoofflineTest {
	/**
	 * Name of the mesh project
	 */
	public final static String MESH_PROJECT_NAME = "testproject";

	/**
	 * Name of the other mesh project
	 */
	public final static String OTHER_MESH_PROJECT_NAME = "other";

	@ClassRule
	public static DBTestContext context = new DBTestContext();

	@ClassRule
	public static MeshContext mesh = new MeshContext();

	private static Node node;

	private static Integer crId;

	private static Template template;

	private static Integer constructId;

	private static Node otherNode;

	private static Integer otherCrId;

	@Rule
	public MeshTestRule meshTestRule = new MeshTestRule(mesh);

	/**
	 * Setup static test data
	 * 
	 * @throws NodeException
	 */
	@BeforeClass
	public static void setupOnce() throws Exception {
		// delete all old nodes
		Trx.operate(t -> {
			for (Node node : t.getObjects(Node.class, DBUtils.select("SELECT id FROM node", DBUtils.IDS))) {
				node.delete(true);
			}
		});
		Trx.operate(() -> DBUtils.executeUpdate("DELETE FROM dirtqueue", null));
		Trx.operate(() -> DBUtils.executeUpdate("DELETE FROM publishqueue", null));

		node = Trx.supply(() -> createNode("node", "Node", PublishTarget.CONTENTREPOSITORY));
		crId = createMeshCR(mesh, MESH_PROJECT_NAME);

		node = Trx.supply(() -> update(node, n -> {
			n.setPubDirSegment(true);
			n.setContentrepositoryId(crId);
			n.getFolder().setPublishDir("node");
		}));

		otherNode = Trx.supply(() -> createNode("other", "Other", PublishTarget.CONTENTREPOSITORY));
		otherCrId = createMeshCR(mesh, OTHER_MESH_PROJECT_NAME);

		otherNode = Trx.supply(() -> update(otherNode, n -> {
			n.setPubDirSegment(true);
			n.setContentrepositoryId(otherCrId);
			n.getFolder().setPublishDir("other");
		}));

		constructId = Trx.supply(() -> createConstruct(node, FileURLPartType.class, "fileurl", "url"));

		template = Trx.supply(() -> create(Template.class, t -> {
			t.setName("Template");
			t.setFolderId(node.getFolder().getId());
			t.setSource("<node tag>");
			TemplateTag templateTag = create(TemplateTag.class, tt -> {
				tt.setConstructId(constructId);
				tt.setEnabled(true);
				tt.setName("tag");
				tt.setPublic(true);
			}, false);
			t.getTemplateTags().put("tag", templateTag);
		}));
	}

	@Parameters(name = "{index}: feature {0}, force {1}, dependency {2}, crossnode {3}, instant {4}, projectPerNode {5}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (boolean feature : Arrays.asList(true, false)) {
			for (boolean force : Arrays.asList(true, false)) {
				for (boolean dependency : Arrays.asList(true, false)) {
					for (boolean crossnode : Arrays.asList(true, false)) {
						for (boolean instant : Arrays.asList(true, false)) {
							for (boolean projectPerNode : Arrays.asList(true, false)) {
								data.add(new Object[] { feature, force, dependency, crossnode, instant, projectPerNode });
							}
						}
					}
				}
			}
		}
		return data;
	}

	/**
	 * Flag for feature enabled for node
	 */
	@Parameter(0)
	public boolean feature;

	/**
	 * Flag for forcing file online
	 */
	@Parameter(1)
	public boolean force;

	/**
	 * Flag for creating dependency
	 */
	@Parameter(2)
	public boolean dependency;

	/**
	 * Flag for crossnode dependency
	 */
	@Parameter(3)
	public boolean crossnode;

	/**
	 * Flag for instant publishing
	 */
	@Parameter(4)
	public boolean instant;

	/**
	 * Flag for "project per node"
	 */
	@Parameter(5)
	public boolean projectPerNode;

	@Before
	public void setup() throws Exception {
		cleanMesh(mesh.client());
	}

	/**
	 * Test publishing
	 * @throws Exception
	 */
	@Test
	public void testPublish() throws Exception {
		for (Node n : Arrays.asList(node, otherNode)) {
			if (feature) {
				Trx.operate(() -> n.activateFeature(Feature.CONTENTFILE_AUTO_OFFLINE));
			} else {
				Trx.operate(() -> n.deactivateFeature(Feature.CONTENTFILE_AUTO_OFFLINE));
			}
		}

		Trx.operate(t -> {
			for (ContentRepository cr : t.getObjects(ContentRepository.class, Arrays.asList(crId, otherCrId))) {
				update(cr, upd -> {
					upd.setInstantPublishing(instant);
					upd.setProjectPerNode(projectPerNode);
				});
			}
		});

		String projectName = Trx.supply(() -> MeshPublisher.getMeshProjectName(node));

		File file = Trx.supply(() -> create(File.class, f -> {
			f.setFolderId(node.getFolder().getId());
			f.setName("testfile.txt");
			f.setFileStream(new ByteArrayInputStream("Testfile contents".getBytes()));
			f.setForceOnline(force);
		}));

		Node depNode = crossnode ? otherNode : node;
		Page page = Trx.supply(() -> create(Page.class, p -> {
			p.setTemplateId(template.getId());
			p.setFolderId(depNode.getFolder().getId());
			getPartType(FileURLPartType.class, p.getContentTag("tag"), "url").setTargetFile(file);
		}));

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		// test online status of file
		Trx.operate(() -> assertThat(FileOnlineStatus.isOnline(file)).as("Online status").isEqualTo(!feature || force));
		assertObject("after publish process", mesh.client(), projectName, file, !feature || force);

		// when testing with dependency, we add it now
		if (dependency) {
			Trx.operate(t -> t.getObject(page, true).publish());

			if (!instant) {
				try (Trx trx = new Trx()) {
					context.publish(false);
					trx.success();
				}
			}

			// test online status of file
			// if the file publishes into another node (crossnode), it will only be online, if published by a regular publish process
			// or forced online (during instant publishing) or the feature is off (during instant publishing)
			// if the file is published into the same node, the file will be online, if forced, the featured is off or the page has
			// a dependency on the file
			boolean expectFile = crossnode ? (!instant || force || !feature) : (!feature || force || dependency);
			Trx.operate(() -> assertThat(FileOnlineStatus.isOnline(file)).as("Online status").isEqualTo(expectFile));
			assertObject("after adding dependency", mesh.client(), projectName, file, expectFile);

			// remove dependency
			Trx.operate(t -> t.getObject(page, true).takeOffline());

			try (Trx trx = new Trx()) {
				context.publish(false);
				trx.success();
			}

			// test online status of file
			Trx.operate(() -> assertThat(FileOnlineStatus.isOnline(file)).as("Online status").isEqualTo(!feature || force));
			assertObject("after removing dependency", mesh.client(), projectName, file, !feature || force);
		}
	}
}
