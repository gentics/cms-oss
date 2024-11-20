package com.gentics.contentnode.tests.publish.mesh;

import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.assertObject;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.cleanMesh;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.crResource;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.createMeshCR;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createChannel;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFolder;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.util.Arrays;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.FeatureClosure;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.parttype.FileURLPartType;
import com.gentics.contentnode.publish.mesh.MeshPublisher;
import com.gentics.contentnode.rest.model.ContentRepositoryModel.Status;
import com.gentics.contentnode.rest.model.response.ContentRepositoryResponse;
import com.gentics.contentnode.tests.category.MeshTest;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.contentnode.testutils.mesh.MeshContext;
import com.gentics.contentnode.testutils.mesh.MeshTestRule;

/**
 * Test cases for publishing pages into a mesh CR linking to files in different folders, when contentfile_auto_offline is used
 */
@GCNFeature(set = { Feature.MESH_CONTENTREPOSITORY, Feature.CONTENTFILE_AUTO_OFFLINE, Feature.INSTANT_CR_PUBLISHING, Feature.PUB_DIR_SEGMENT, Feature.MULTICHANNELLING })
@Category(MeshTest.class)
public class MeshPublishAutoofflineCrossNodeTest {
	/**
	 * Name of the mesh project
	 */
	public final static String MESH_PROJECT_NAME = "testproject";

	@ClassRule
	public static DBTestContext context = new DBTestContext();

	@ClassRule
	public static MeshContext mesh = new MeshContext();

	private static Node node;

	private static Node channel;

	private static Integer crId;

	private static Node otherNode;

	private static Node otherPublishedNode;

	private static Integer constructId;

	private static Template template;

	@Rule
	public MeshTestRule meshTestRule = new MeshTestRule(mesh);

	/**
	 * Setup static test data
	 * 
	 * @throws NodeException
	 */
	@BeforeClass
	public static void setupOnce() throws Exception {
		context.getContext().getTransaction().commit();
		// delete all old nodes
		operate(t -> {
			for (Node node : t.getObjects(Node.class, DBUtils.select("SELECT id FROM node", DBUtils.IDS))) {
				node.delete(true);
			}
		});
		operate(() -> DBUtils.executeUpdate("DELETE FROM dirtqueue", null));
		operate(() -> DBUtils.executeUpdate("DELETE FROM publishqueue", null));

		node = supply(() -> createNode("node", "Node", PublishTarget.CONTENTREPOSITORY));
		channel = supply(() -> createChannel(node, "Channel", "channel", "/", PublishTarget.CONTENTREPOSITORY));
		crId = createMeshCR(mesh, MESH_PROJECT_NAME);
		operate(t -> update(t.getObject(ContentRepository.class, crId), upd -> {
			upd.setProjectPerNode(true);
			upd.setInstantPublishing(true);
		}));

		node = execute(Node::reload, node);
		channel = execute(Node::reload, channel);

		node = supply(() -> update(node, n -> {
			n.setPubDirSegment(true);
			n.setContentrepositoryId(crId);
			n.getFolder().setPublishDir("node");
		}));

		channel = supply(() -> update(channel, n -> {
			n.setPubDirSegment(true);
			n.setContentrepositoryId(crId);
			n.getFolder().setPublishDir("channel");
		}));

		otherNode = supply(() -> createNode("other", "Other", PublishTarget.NONE));

		otherPublishedNode = supply(() -> createNode("otherpublished", "Other Published", PublishTarget.CONTENTREPOSITORY));

		otherPublishedNode = supply(() -> update(otherPublishedNode, n -> {
			n.setPubDirSegment(true);
			n.setContentrepositoryId(crId);
			n.getFolder().setPublishDir("other_published");
			n.activateFeature(Feature.CONTENTFILE_AUTO_OFFLINE);
		}));

		constructId = supply(() -> createConstruct(node, FileURLPartType.class, "fileurl", "url"));

		template = supply(() -> create(Template.class, t -> {
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

	/**
	 * Clean data
	 * @throws Exception
	 */
	@Before
	public void setup() throws Exception {
		cleanMesh(mesh.client());

		try (FeatureClosure fc = new FeatureClosure(Feature.INSTANT_CR_PUBLISHING, false)) {
			for (Node n : Arrays.asList(node, channel, otherNode, otherPublishedNode)) {
				operate(() -> {
					for (Folder f : n.getFolder().getChildFolders()) {
						f.delete(true);
					}
					for (Page p : n.getFolder().getPages()) {
						p.delete(true);
					}
					for (File f : n.getFolder().getFilesAndImages()) {
						f.delete(true);
					}
				});
			}
		}

		ContentRepositoryResponse response = crResource.repair(Integer.toString(crId), 0);
		ContentNodeRESTUtils.assertResponseOK(response);
		if (response.getContentRepository().getCheckStatus() != Status.ok) {
			fail(response.getContentRepository().getCheckResult());
		}
	}

	/**
	 * Test page in master, using a file from the foreign node
	 * @throws NodeException
	 */
	@Test
	public void testLinkMaster() throws NodeException {
		Folder targetFolder = supply(() -> createFolder(otherNode.getFolder(), "Folder"));
		File targetFile = supply(() -> create(File.class, f -> {
			f.setFolderId(targetFolder.getId());
			f.setName("testfile.txt");
			f.setFileStream(new ByteArrayInputStream("Testfile contents".getBytes()));
		}));

		Page page = supply(() -> create(Page.class, p -> {
			p.setTemplateId(template.getId());
			p.setFolderId(node.getFolder().getId());
			getPartType(FileURLPartType.class, p.getContentTag("tag"), "url").setTargetFile(targetFile);
		}));

		String projectName = Trx.supply(() -> MeshPublisher.getMeshProjectName(node));

		update(page, Page::publish);
		operate(() -> assertObject("after instant publishing", mesh.client(), projectName, page, true));
		operate(() -> assertObject("after instant publishing", mesh.client(), projectName, targetFile, false));
		operate(() -> assertObject("after instant publishing", mesh.client(), projectName, targetFolder, false));
		operate(() -> assertObject("after instant publishing", mesh.client(), projectName, otherNode.getFolder(), false));
	}

	/**
	 * Test page in channel, using a file from the foreign node
	 * @throws NodeException
	 */
	@Test
	public void testLinkChannel() throws NodeException {
		Folder targetFolder = supply(() -> createFolder(otherNode.getFolder(), "Folder"));
		File targetFile = supply(() -> create(File.class, f -> {
			f.setFolderId(targetFolder.getId());
			f.setName("testfile.txt");
			f.setFileStream(new ByteArrayInputStream("Testfile contents".getBytes()));
		}));

		Page page = supply(() -> create(Page.class, p -> {
			p.setTemplateId(template.getId());
			p.setFolderId(node.getFolder().getId());
			p.setChannelInfo(channel.getId(), null);
			getPartType(FileURLPartType.class, p.getContentTag("tag"), "url").setTargetFile(targetFile);
		}));

		String projectName = Trx.supply(() -> MeshPublisher.getMeshProjectName(node));

		update(page, Page::publish);
		operate(() -> assertObject("after instant publishing", mesh.client(), projectName, MeshPublisher.getMeshName(channel), page, true));
		operate(() -> assertObject("after instant publishing", mesh.client(), projectName, MeshPublisher.getMeshName(channel), targetFile, false));
		operate(() -> assertObject("after instant publishing", mesh.client(), projectName, MeshPublisher.getMeshName(channel), targetFolder, false));
		operate(() -> assertObject("after instant publishing", mesh.client(), projectName, MeshPublisher.getMeshName(channel), otherNode.getFolder(), false));
	}

	/**
	 * Test page in master, using a file from the foreign node, which also publishes into the CR
	 * @throws NodeException
	 */
	@Test
	public void testLinkMasterOtherPublished() throws NodeException {
		Folder targetFolder = supply(() -> createFolder(otherPublishedNode.getFolder(), "Folder"));
		File targetFile = supply(() -> create(File.class, f -> {
			f.setFolderId(targetFolder.getId());
			f.setName("testfile.txt");
			f.setFileStream(new ByteArrayInputStream("Testfile contents".getBytes()));
		}));

		Page page = supply(() -> create(Page.class, p -> {
			p.setTemplateId(template.getId());
			p.setFolderId(node.getFolder().getId());
			getPartType(FileURLPartType.class, p.getContentTag("tag"), "url").setTargetFile(targetFile);
		}));

		String projectName = Trx.supply(() -> MeshPublisher.getMeshProjectName(node));
		String otherProjectName = Trx.supply(() -> MeshPublisher.getMeshProjectName(otherPublishedNode));

		update(page, Page::publish);
		operate(() -> assertObject("after instant publishing", mesh.client(), projectName, page, true));
		operate(() -> assertObject("after instant publishing", mesh.client(), projectName, targetFile, false));
		operate(() -> assertObject("after instant publishing", mesh.client(), projectName, targetFolder, false));
		operate(() -> assertObject("after instant publishing", mesh.client(), projectName, otherPublishedNode.getFolder(), false));

		// file should have been published into the other project
		operate(() -> assertObject("after instant publishing", mesh.client(), otherProjectName, targetFile, true));
		operate(() -> assertObject("after instant publishing", mesh.client(), otherProjectName, targetFolder, true));
	}

	/**
	 * Test page in channel, using a file from the foreign node, which also publishes into the CR
	 * @throws NodeException
	 */
	@Test
	public void testLinkChannelOtherPublished() throws NodeException {
		Folder targetFolder = supply(() -> createFolder(otherPublishedNode.getFolder(), "Folder"));
		File targetFile = supply(() -> create(File.class, f -> {
			f.setFolderId(targetFolder.getId());
			f.setName("testfile.txt");
			f.setFileStream(new ByteArrayInputStream("Testfile contents".getBytes()));
		}));

		Page page = supply(() -> create(Page.class, p -> {
			p.setTemplateId(template.getId());
			p.setFolderId(node.getFolder().getId());
			p.setChannelInfo(channel.getId(), null);
			getPartType(FileURLPartType.class, p.getContentTag("tag"), "url").setTargetFile(targetFile);
		}));

		String projectName = Trx.supply(() -> MeshPublisher.getMeshProjectName(node));
		String otherProjectName = Trx.supply(() -> MeshPublisher.getMeshProjectName(otherPublishedNode));

		update(page, Page::publish);
		operate(() -> assertObject("after instant publishing", mesh.client(), projectName, MeshPublisher.getMeshName(channel), page, true));
		operate(() -> assertObject("after instant publishing", mesh.client(), projectName, MeshPublisher.getMeshName(channel), targetFile, false));
		operate(() -> assertObject("after instant publishing", mesh.client(), projectName, MeshPublisher.getMeshName(channel), targetFolder, false));
		operate(() -> assertObject("after instant publishing", mesh.client(), projectName, MeshPublisher.getMeshName(channel), otherPublishedNode.getFolder(), false));

		// file should have been published into the other project
		operate(() -> assertObject("after instant publishing", mesh.client(), otherProjectName, targetFile, true));
		operate(() -> assertObject("after instant publishing", mesh.client(), otherProjectName, targetFolder, true));
	}
}
