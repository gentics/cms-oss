package com.gentics.contentnode.tests.publish.mesh;

import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.assertObject;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.createMeshCR;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createChannel;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFile;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFolder;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createPage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.assertPublishCR;

import java.util.Arrays;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.publish.mesh.MeshPublisher;
import com.gentics.contentnode.tests.category.MeshTest;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.contentnode.testutils.mesh.MeshContext;
import com.gentics.contentnode.testutils.mesh.MeshTestRule;

/**
 * Test cases for publishing the Master Node into a MCCR and a Channel into a MeshCR
 */
@GCNFeature(set = { Feature.MESH_CONTENTREPOSITORY, Feature.INSTANT_CR_PUBLISHING, Feature.MULTICHANNELLING,
		Feature.MCCR, Feature.PUB_DIR_SEGMENT })
@Category(MeshTest.class)
public class MeshPublishMCCRTest {
	/**
	 * Name of the mesh project
	 */
	public final static String MESH_PROJECT_NAME = "testproject";

	@ClassRule
	public static DBTestContext context = new DBTestContext();

	@ClassRule
	public static MeshContext mesh = new MeshContext();

	private static Node node;

	private static Node channel1;

	private static Node channel2;

	private static Integer crId;

	private static ContentRepository meshCr;

	private static ContentRepository mccr;

	@Rule
	public MeshTestRule meshTestRule = new MeshTestRule(mesh);

	private static Template template;


	@BeforeClass
	public static void setupOnce() throws NodeException {
		node = supply(() -> update(createNode("node", "Node", PublishTarget.CONTENTREPOSITORY, true, true), n -> n.setPubDirSegment(true)));
		channel1 = supply(() -> createChannel(node, "Channel1", "channel1", "channel1"));
		channel2 = supply(() -> createChannel(node, "Channel2", "channel2", "channel2"));
		crId = createMeshCR(mesh, MESH_PROJECT_NAME);

		template = supply(() -> createTemplate(node.getFolder(), "Template"));

		mccr = supply(() -> node.getContentRepository());
		meshCr = supply(t -> t.getObject(ContentRepository.class, crId));

		operate(() -> {
			update(meshCr, cr -> {
				cr.setProjectPerNode(true);
			});
		});

		// let the channel1 publish into the MCCR
		operate(() -> update(channel1, n -> {
			n.setContentrepositoryId(node.getContentrepositoryId());
			n.setPublishContentmap(true);
		}));

		// let the channel2 publish into the Mesh CR
		operate(() -> update(channel2, n -> {
			n.setContentrepositoryId(crId);
			n.setPublishContentmap(true);
		}));

		channel1 = execute(Node::reload, channel1);
		channel2 = execute(Node::reload, channel2);
	}

	@Test
	public void testPublishProcess() throws Exception {
		String projectName = MeshPublisher.getMeshName(node);

		mccr = supply(() -> update(mccr, cr -> cr.setInstantPublishing(false)));
		meshCr = supply(() -> update(meshCr, cr -> cr.setInstantPublishing(false)));

		Folder folder = supply(() -> createFolder(node.getFolder(), "Folder"));
		File file = supply(() -> createFile(folder, "testfile.txt", "Content".getBytes()));
		Page page = supply(() -> update(createPage(folder, template, "Testpage"), Page::publish));

		// nothing is published yet
		for (NodeObject object : Arrays.asList(folder, file, page)) {
			for (Node n : Arrays.asList(node, channel1, channel2)) {
				assertPublishCR(object, n, false);
				assertObject("Object in MeshCR", mesh.client(), projectName, MeshPublisher.getMeshName(n), object,
						false);
			}
		}

		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		// objects are published into MCCR for node and channel1, but not for channel2
		// and into channel2 for MeshCR
		for (NodeObject object : Arrays.asList(folder, file, page)) {
			for (Node n : Arrays.asList(node, channel1, channel2)) {
				assertPublishCR(object, n, n != channel2);
				assertObject("Object in MeshCR", mesh.client(), projectName, MeshPublisher.getMeshName(n), object,
						n == channel2);
			}
		}
	}

	@Test
	public void testInstantPublishing() throws Exception {
		String projectName = MeshPublisher.getMeshName(node);

		// run publish process to make sure that channels/branches, etc. exist in the CRs
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		mccr = supply(() -> update(mccr, cr -> cr.setInstantPublishing(true)));
		meshCr = supply(() -> update(meshCr, cr -> cr.setInstantPublishing(true)));

		for (Node testNode : Arrays.asList(node, channel1, channel2)) {
			Folder folder = supply(() -> createFolder(node.getFolder(), "Folder", testNode));
			File file = supply(() -> createFile(folder, "testfile.txt", "Content".getBytes(), testNode));
			Page page = supply(() -> update(createPage(folder, template, "Testpage", testNode), Page::publish));

			// objects are published into MCCR for node (not channel1, because instant publishing), but not for channel2
			// and into channel2 for MeshCR
			for (NodeObject object : Arrays.asList(folder, file, page)) {
				for (Node n : Arrays.asList(node, channel1, channel2)) {
					assertPublishCR(object, n, n == testNode && testNode != channel2);
					assertObject("Object in MeshCR", mesh.client(), projectName, MeshPublisher.getMeshName(n), object,
							n == testNode && n == channel2);
				}
			}
		}
	}
}
