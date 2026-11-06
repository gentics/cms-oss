package com.gentics.contentnode.tests.publish.mesh;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.assertContainsOnly;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.assertMeshProject;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.cleanMesh;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.createMeshCR;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.getFileSchemaName;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.getFolderSchemaName;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.getPageSchemaName;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createChannel;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFile;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFolder;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createPage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
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
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.publish.PublishQueue;
import com.gentics.contentnode.publish.PublishQueue.Action;
import com.gentics.contentnode.publish.mesh.MeshPublisher;
import com.gentics.contentnode.tests.category.MeshTest;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.contentnode.testutils.mesh.MeshContext;
import com.gentics.mesh.core.rest.project.ProjectResponse;

/**
 * Test cases for partially publishing nodes/channels into branchs of a Mesh CR
 */
@RunWith(value = Parameterized.class)
@GCNFeature(set = { Feature.MESH_CONTENTREPOSITORY, Feature.MULTICHANNELLING })
@Category(MeshTest.class)
public class MeshPublishPartialChannelTest {
	@ClassRule
	public static DBTestContext context = new DBTestContext();

	@ClassRule
	public static MeshContext mesh = new MeshContext();

	public final static String projectName = "Node";

	public final static String branchName = "Channel";

	private static Node node;

	private static Node channel;

	private static Integer crId;

	private static Template template;

	private static Folder folder;

	private static Page page;

	private static File file;

	/**
	 * Schema prefix
	 */
	public final static String SCHEMA_PREFIX = "test";

	@Parameter(0)
	public boolean masterPages;

	@Parameter(1)
	public boolean masterFiles;

	@Parameter(2)
	public boolean masterFolders;

	@Parameter(3)
	public boolean channelPages;

	@Parameter(4)
	public boolean channelFiles;

	@Parameter(5)
	public boolean channelFolders;

	@Parameters(name = "{index}: master pages {0}, master files {1}, master folders {2}, channel pages {3}, channel files {4}, channel folders {5}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (boolean masterPages : Arrays.asList(true, false)) {
			for (boolean masterFiles : Arrays.asList(true, false)) {
				for (boolean masterFolders : Arrays.asList(true, false)) {
					for (boolean channelPages : Arrays.asList(true, false)) {
						for (boolean channelFiles : Arrays.asList(true, false)) {
							for (boolean channelFolders : Arrays.asList(true, false)) {
								data.add(new Object[] { masterPages, masterFiles, masterFolders, channelPages, channelFiles, channelFolders });
							}
						}
					}
				}
			}
		}
		return data;
	}

	/**
	 * Create node, channels and test data
	 * @throws NodeException
	 */
	@BeforeClass
	public static void setupOnce() throws NodeException {
		operate(t -> {
			for (Node n : t.getObjects(Node.class, DBUtils.select("SELECT id FROM node", DBUtils.IDS))) {
				n.delete(true);
			}
		});

		node = supply(() -> createNode("node.com", projectName, PublishTarget.BOTH));
		channel = supply(() -> createChannel(node, branchName, "channel.com", "/", PublishTarget.BOTH));
		supply(() -> createChannel(node, "other channel", "otherchannel.com", "/", PublishTarget.FILESYSTEM));
		folder = supply(() -> createFolder(node.getFolder(), "Folder"));
		template = supply(() -> createTemplate(node.getFolder(), "Template"));
		page = supply(() -> update(createPage(node.getFolder(), template, "Page"), Page::publish));
		file = supply(() -> createFile(node.getFolder(), "file.txt", "File contents".getBytes()));

		crId = createMeshCR(mesh, SCHEMA_PREFIX);
		operate(t -> update(t.getObject(ContentRepository.class, crId), cr -> cr.setProjectPerNode(true)));

		for (Node n : Arrays.asList(node, channel)) {
			operate(() -> update(n, update -> {
				update.setContentrepositoryId(crId);
			}));
		}
	}

	/**
	 * Prepare test scenario
	 * @throws Exception
	 */
	@Before
	public void setup() throws Exception {
		cleanMesh(mesh.client());

		operate(() -> {
			node = update(node, update -> {
				update.setPublishContentmap(true);
				update.setPublishContentMapPages(masterPages);
				update.setPublishContentMapFiles(masterFiles);
				update.setPublishContentMapFolders(masterFolders);
			});
			channel = update(channel, update -> {
				update.setPublishContentmap(true);
				update.setPublishContentMapPages(channelPages);
				update.setPublishContentMapFiles(channelFiles);
				update.setPublishContentMapFolders(channelFolders);
			});
		});

		operate(() -> {
			DBUtils.executeUpdate("DELETE FROM publishqueue", null);
			DBUtils.executeUpdate("DELETE FROM contentfile_online", null);
			PublishQueue.dirtFolders(new int[] { node.getId(), channel.getId() }, null, 0, 0, Action.MODIFY);
			PublishQueue.dirtImagesAndFiles(new int[] { node.getId(), channel.getId() }, null, 0, 0, Action.MODIFY);
			PublishQueue.dirtPublishedPages(new int[] { node.getId(), channel.getId() }, null, 0, 0, Action.MODIFY);
		});
	}

	/**
	 * Test publishing
	 * @throws Exception
	 */
	@Test
	public void test() throws Exception {
		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		ProjectResponse project = assertMeshProject(mesh.client(), projectName, SCHEMA_PREFIX, MeshPublisher.getMeshUuid(node), true);
		if (masterPages) {
			assertContainsOnly(mesh.client(), projectName, projectName, project.getRootNode().getUuid(), getPageSchemaName(SCHEMA_PREFIX), "en", page);
		} else {
			assertContainsOnly(mesh.client(), projectName, projectName, project.getRootNode().getUuid(), getPageSchemaName(SCHEMA_PREFIX), "en");
		}
		if (masterFiles) {
			assertContainsOnly(mesh.client(), projectName, projectName, project.getRootNode().getUuid(), getFileSchemaName(SCHEMA_PREFIX), "en", file);
		} else {
			assertContainsOnly(mesh.client(), projectName, projectName, project.getRootNode().getUuid(), getFileSchemaName(SCHEMA_PREFIX), "en");
		}
		if (masterFolders) {
			assertContainsOnly(mesh.client(), projectName, projectName, project.getRootNode().getUuid(), getFolderSchemaName(SCHEMA_PREFIX), "en", folder);
		} else {
			assertContainsOnly(mesh.client(), projectName, projectName, project.getRootNode().getUuid(), getFolderSchemaName(SCHEMA_PREFIX), "en");
		}

		if (channelPages) {
			assertContainsOnly(mesh.client(), projectName, branchName, project.getRootNode().getUuid(), getPageSchemaName(SCHEMA_PREFIX), "en", page);
		} else {
			assertContainsOnly(mesh.client(), projectName, branchName, project.getRootNode().getUuid(), getPageSchemaName(SCHEMA_PREFIX), "en");
		}
		if (channelFiles) {
			assertContainsOnly(mesh.client(), projectName, branchName, project.getRootNode().getUuid(), getFileSchemaName(SCHEMA_PREFIX), "en", file);
		} else {
			assertContainsOnly(mesh.client(), projectName, branchName, project.getRootNode().getUuid(), getFileSchemaName(SCHEMA_PREFIX), "en");
		}
		if (channelFolders) {
			assertContainsOnly(mesh.client(), projectName, branchName, project.getRootNode().getUuid(), getFolderSchemaName(SCHEMA_PREFIX), "en", folder);
		} else {
			assertContainsOnly(mesh.client(), projectName, branchName, project.getRootNode().getUuid(), getFolderSchemaName(SCHEMA_PREFIX), "en");
		}
	}
}
