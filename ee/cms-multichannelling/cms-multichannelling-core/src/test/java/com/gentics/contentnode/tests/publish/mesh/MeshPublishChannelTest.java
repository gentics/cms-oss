package com.gentics.contentnode.tests.publish.mesh;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.assertBranch;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.assertContainsOnly;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.assertFolders;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.assertMeshProject;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.assertObject;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.cleanMesh;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.crResource;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.createMeshCR;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.getPageSchemaName;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.isChannelBranch;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createChannel;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFile;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFolder;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createPage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getLanguage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.localize;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.publish.PublishQueue;
import com.gentics.contentnode.publish.PublishQueue.Action;
import com.gentics.contentnode.publish.mesh.MeshPublisher;
import com.gentics.contentnode.rest.model.ContentRepositoryModel.Status;
import com.gentics.contentnode.rest.model.response.ContentRepositoryResponse;
import com.gentics.contentnode.tests.category.MeshTest;
import com.gentics.contentnode.tests.utils.Builder;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.contentnode.testutils.mesh.MeshContext;
import com.gentics.contentnode.testutils.mesh.MeshTestRule;
import com.gentics.mesh.assertj.MeshAssertions;
import com.gentics.mesh.core.rest.branch.BranchResponse;
import com.gentics.mesh.core.rest.project.ProjectResponse;

/**
 * Test cases for publishing Channels into a Mesh CR
 */
@GCNFeature(set = { Feature.MESH_CONTENTREPOSITORY, Feature.MULTICHANNELLING, Feature.INSTANT_CR_PUBLISHING })
@Category(MeshTest.class)
public class MeshPublishChannelTest {
	@ClassRule
	public static DBTestContext context = new DBTestContext();

	@ClassRule
	public static MeshContext mesh = new MeshContext();

	public final static String projectName = "Node";

	public final static String branchName = "Channel";

	private static Node node;

	private static Node channel;

	private static Integer crId;

	/**
	 * Schema prefix
	 */
	public final static String SCHEMA_PREFIX = "test";

	private static Template template;

	@Rule
	public MeshTestRule meshTestRule = new MeshTestRule(mesh);

	@BeforeClass
	public static void setupOnce() throws NodeException {
		context.getContext().getTransaction().commit();
		Trx.operate(t -> {
			for (Node n : t.getObjects(Node.class, DBUtils.select("SELECT id FROM node", DBUtils.IDS))) {
				n.delete(true);
			}
		});

		node = Trx.supply(() -> createNode("node.com", projectName, PublishTarget.CONTENTREPOSITORY, getLanguage("de")));
		channel = Trx.supply(() -> createChannel(node, branchName, "channel.com", "/", PublishTarget.CONTENTREPOSITORY));
		template = Trx.supply(() -> createTemplate(node.getFolder(), "Template"));

		crId = createMeshCR(mesh, SCHEMA_PREFIX);
		Trx.operate(t -> update(t.getObject(ContentRepository.class, crId), cr -> cr.setProjectPerNode(true)));

		for (Node n : Arrays.asList(node, channel)) {
			Trx.operate(() -> update(n, update -> {
				update.setPublishDir("/prefix");
				update.setContentrepositoryId(crId);
			}));
		}
	}

	@Before
	public void setup() throws Exception {
		cleanMesh(mesh.client());
		Trx.operate(t -> {
			update(t.getObject(ContentRepository.class, crId), cr -> {
				cr.setInstantPublishing(false);
			});
			for (Node n : Arrays.asList(node, channel)) {
				update(n, update -> {
					update.setPublishDir("/prefix");
					update.setContentrepositoryId(crId);
					update.setMeshProjectName("");
				});
				try (ChannelTrx cTrx = new ChannelTrx(n)) {
					for (Folder folder : n.getFolder().getChildFolders()) {
						t.getObject(folder, true).delete(true);
					}
				}
			}
		});
	}

	@Test
	public void testBranches() throws Exception {
		Trx.operate(() -> {
			PublishQueue.dirtFolders(new int[] {node.getId()}, null, 0, 0, Action.MODIFY);
			PublishQueue.dirtFolders(new int[] {channel.getId()}, null, 0, 0, Action.MODIFY);
		});

		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		assertMeshProject(mesh.client(), projectName, SCHEMA_PREFIX, MeshPublisher.getMeshUuid(node), true);
		assertBranch(mesh.client(), projectName, isChannelBranch(channel), true, branchName, "channel.com", "/prefix", false);
	}

	/**
	 * Test publish process when one channel is fully published and only a file is published in the other channel
	 * @throws Exception
	 */
	@Test
	public void testPartialChannelDirting() throws Exception {
		ContentNodeRESTUtils.assertResponseOK(crResource.repair(Integer.toString(crId), 0));

		ProjectResponse project = assertMeshProject(mesh.client(), projectName, SCHEMA_PREFIX, MeshPublisher.getMeshUuid(node), true);
		assertBranch(mesh.client(), projectName, isChannelBranch(channel), true, branchName, "channel.com", null, false);

		Folder folder = Trx.supply(() -> createFolder(node.getFolder(), "Folder"));
		Page page = Trx.supply(() -> update(createPage(folder, template, "Page"), Page::publish));

		Trx.operate(() -> DBUtils.executeUpdate("DELETE FROM dirtqueue", null));

		Trx.operate(() -> {
			PublishQueue.dirtFolders(new int[] {channel.getId()}, null, 0, 0, Action.MODIFY);
			PublishQueue.dirtPublishedPages(new int[] {channel.getId()}, null, 0, 0, Action.MODIFY);
		});

		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		// nothing should have been published into the "master" branch
		assertFolders(mesh.client(), projectName, projectName, SCHEMA_PREFIX, project.getRootNode().getUuid());
		// folder and file should have been published into the "channel" branch
		assertFolders(mesh.client(), projectName, branchName, SCHEMA_PREFIX, project.getRootNode().getUuid(), folder);
		assertContainsOnly(mesh.client(), projectName, branchName, supply(() -> MeshPublisher.getMeshUuid(folder)), getPageSchemaName(SCHEMA_PREFIX), "en", page);

		// now only dirt the page for the node (we expect the folder also to be published into the node)
		Trx.operate(() -> {
			PublishQueue.dirtPublishedPages(new int[] {node.getId()}, null, 0, 0, Action.MODIFY);
		});

		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}
		assertFolders(mesh.client(), projectName, projectName, SCHEMA_PREFIX, project.getRootNode().getUuid(), folder);
		assertContainsOnly(mesh.client(), projectName, projectName, supply(() -> MeshPublisher.getMeshUuid(folder)), getPageSchemaName(SCHEMA_PREFIX), "en", page);
	}

	/**
	 * Test dirting a folder in one channel, that has been published into another channel
	 * @throws Exception
	 */
	@Test
	public void testAddFolderToChannel() throws Exception {
		ContentNodeRESTUtils.assertResponseOK(crResource.repair(Integer.toString(crId), 0));

		ProjectResponse project = assertMeshProject(mesh.client(), projectName, SCHEMA_PREFIX, MeshPublisher.getMeshUuid(node), true);
		assertBranch(mesh.client(), projectName, isChannelBranch(channel), true, branchName, "channel.com", null, false);

		Folder folder = Trx.supply(() -> createFolder(node.getFolder(), "Folder"));

		Trx.operate(() -> DBUtils.executeUpdate("DELETE FROM dirtqueue", null));

		Trx.operate(() -> {
			PublishQueue.dirtFolders(new int[] {channel.getId()}, null, 0, 0, Action.MODIFY);
		});

		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		// nothing should have been published into the "master" branch
		assertFolders(mesh.client(), projectName, projectName, SCHEMA_PREFIX, project.getRootNode().getUuid());
		// folder and file should have been published into the "channel" branch
		assertFolders(mesh.client(), projectName, branchName, SCHEMA_PREFIX, project.getRootNode().getUuid(), folder);

		// now dirt the folder for the master
		Trx.operate(() -> {
			PublishQueue.dirtFolders(new int[] {node.getId()}, null, 0, 0, Action.MODIFY);
		});

		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}
		assertFolders(mesh.client(), projectName, projectName, SCHEMA_PREFIX, project.getRootNode().getUuid(), folder);
	}

	@Test
	public void testRepairLatestBranch() throws Exception {
		// repair CR to initially create project and branches
		ContentRepositoryResponse response = crResource.repair(Integer.toString(crId), 0);
		ContentNodeRESTUtils.assertResponseOK(response);
		if (response.getContentRepository().getCheckStatus() != Status.ok) {
			fail(response.getContentRepository().getCheckResult());
		}
		assertMeshProject(mesh.client(), projectName, SCHEMA_PREFIX, MeshPublisher.getMeshUuid(node), true);
		BranchResponse channelBranch = assertBranch(mesh.client(), projectName, isChannelBranch(channel), true, branchName, "channel.com", null, false);

		// forcibly set channel branch to be latest
		mesh.client().setLatestBranch(projectName, channelBranch.getUuid()).blockingAwait();

		// check on CR must fail now
		response = crResource.check(Integer.toString(crId), 0);
		ContentNodeRESTUtils.assertResponseOK(response);
		if (response.getContentRepository().getCheckStatus() != Status.error) {
			fail(response.getContentRepository().getCheckResult());
		}

		// repair now
		response = crResource.repair(Integer.toString(crId), 0);
		ContentNodeRESTUtils.assertResponseOK(response);
		if (response.getContentRepository().getCheckStatus() != Status.ok) {
			fail(response.getContentRepository().getCheckResult());
		}

		assertMeshProject(mesh.client(), projectName, SCHEMA_PREFIX, MeshPublisher.getMeshUuid(node), true);
		assertBranch(mesh.client(), projectName, isChannelBranch(channel), true, branchName, "channel.com", null, false);
	}

	/**
	 * Test publishing only the channel into the MeshCR
	 * @throws Exception
	 */
	@Test
	public void testChannelOnly() throws Exception {
		Trx.operate(() -> {
			update(node, update -> {
				update.setContentrepositoryId(0);
			});
		});

		Folder folder = Trx.supply(() -> createFolder(node.getFolder(), "Folder"));

		Trx.operate(() -> DBUtils.executeUpdate("DELETE FROM dirtqueue", null));

		Trx.operate(() -> {
			PublishQueue.dirtFolders(new int[] {channel.getId()}, null, 0, 0, Action.MODIFY);
		});

		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		assertMeshProject(mesh.client(), projectName, SCHEMA_PREFIX, MeshPublisher.getMeshUuid(node), true);
		assertBranch(mesh.client(), projectName, isChannelBranch(node), false, null, null, null, false);
		assertBranch(mesh.client(), projectName, isChannelBranch(channel), true, branchName, "channel.com", null, false);
	}

	/**
	 * Test setting the language "de" to a page which had no language before (was published as "en"), in the master node (inherited in the channel)
	 * @throws Exception
	 */
	@Test
	public void testSetPageLanguageInMaster() throws Exception {
		Page page = supply(() -> createPage(node.getFolder(), template, "Page"));
		update(page, Page::publish);

		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		assertObject("Check page in master branch", mesh.client(), projectName, page, true, n -> {
			assertThat(n.getAvailableLanguages()).as("Available languages").containsOnlyKeys("en");
		});
		assertObject("Check page in channel branch", mesh.client(), projectName, supply(() -> MeshPublisher.getBranchName(channel, null)), page, true, n -> {
			assertThat(n.getAvailableLanguages()).as("Available languages").containsOnlyKeys("en");
		});

		page = update(page, upd -> {
			upd.setLanguage(getLanguage("de"));
		});

		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		assertObject("Check page in master branch", mesh.client(), projectName, page, true, n -> {
			assertThat(n.getAvailableLanguages()).as("Available languages").containsOnlyKeys("de");
		});
		assertObject("Check page in channel branch", mesh.client(), projectName, supply(() -> MeshPublisher.getBranchName(channel, null)), page, true, n -> {
			assertThat(n.getAvailableLanguages()).as("Available languages").containsOnlyKeys("de");
		});
	}

	/**
	 * Test setting the language "de" to a page which had no language before (was published as "en"), in the channel
	 * @throws Exception
	 */
	@Test
	public void testSetPageLanguageInChannel() throws Exception {
		Page page = supply(() -> createPage(node.getFolder(), template, "Page", channel));
		update(page, Page::publish);

		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		assertObject("Check page in master branch", mesh.client(), projectName, page, false);
		assertObject("Check page in channel branch", mesh.client(), projectName, supply(() -> MeshPublisher.getBranchName(channel, null)), page, true, n -> {
			assertThat(n.getAvailableLanguages()).as("Available languages").containsOnlyKeys("en");
		});

		page = update(page, upd -> {
			upd.setLanguage(getLanguage("de"));
		});

		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		assertObject("Check page in master branch", mesh.client(), projectName, page, false);
		assertObject("Check page in channel branch", mesh.client(), projectName, supply(() -> MeshPublisher.getBranchName(channel, null)), page, true, n -> {
			assertThat(n.getAvailableLanguages()).as("Available languages").containsOnlyKeys("de");
		});
	}

	/**
	 * Test setting the language "de" to a page which had no language before (was published as "en") in the master node (inherited in the channel)
	 * with instant publishing
	 * @throws Exception
	 */
	@Test
	public void testSetPageLanguageInMasterInstantPublishing() throws Exception {
		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		operate(t -> {
			update(t.getObject(ContentRepository.class, crId), cr -> {
				cr.setInstantPublishing(true);
			});
		});
		Page page = supply(() -> createPage(node.getFolder(), template, "Page"));
		update(page, Page::publish);

		assertObject("Check page in master branch", mesh.client(), projectName, page, true, n -> {
			assertThat(n.getAvailableLanguages()).as("Available languages").containsOnlyKeys("en");
		});
		// instant publishing will not publish the page into the channel
		assertObject("Check page in channel branch", mesh.client(), projectName, supply(() -> MeshPublisher.getBranchName(channel, null)), page, false);

		page = update(page, upd -> {
			upd.setLanguage(getLanguage("de"));
		});

		assertObject("Check page in master branch", mesh.client(), projectName, page, true, n -> {
			assertThat(n.getAvailableLanguages()).as("Available languages").containsOnlyKeys("de");
		});
		assertObject("Check page in channel branch", mesh.client(), projectName, supply(() -> MeshPublisher.getBranchName(channel, null)), page, false);
	}

	/**
	 * Test setting the language "de" to a page which had no language before (was published as "en") in the channel
	 * with instant publishing
	 * @throws Exception
	 */
	@Test
	public void testSetPageLanguageInChannelInstantPublishing() throws Exception {
		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		operate(t -> {
			update(t.getObject(ContentRepository.class, crId), cr -> {
				cr.setInstantPublishing(true);
			});
		});
		Page page = supply(() -> createPage(node.getFolder(), template, "Page", channel));
		update(page, Page::publish);

		assertObject("Check page in master branch", mesh.client(), projectName, page, false);
		assertObject("Check page in channel branch", mesh.client(), projectName, supply(() -> MeshPublisher.getBranchName(channel, null)), page, true, n -> {
			assertThat(n.getAvailableLanguages()).as("Available languages").containsOnlyKeys("en");
		});

		page = update(page, upd -> {
			upd.setLanguage(getLanguage("de"));
		});

		assertObject("Check page in master branch", mesh.client(), projectName, page, false);
		assertObject("Check page in channel branch", mesh.client(), projectName, supply(() -> MeshPublisher.getBranchName(channel, null)), page, true, n -> {
			assertThat(n.getAvailableLanguages()).as("Available languages").containsOnlyKeys("de");
		});
	}

	/**
	 * Test localizing a folder, which contains objects (objects must not be removed from the branch in Mesh)
	 * @throws NodeException
	 */
	@Test
	public void testLocalizeFolder() throws Exception {
		Folder masterFolder = supply(() -> createFolder(node.getFolder(), "Testfolder"));
		Folder subFolder = supply(() -> createFolder(masterFolder, "Subfolder"));
		File testFile = supply(() -> createFile(masterFolder, "testfile.txt", "File contents".getBytes()));
		Page testPage = supply(() -> update(createPage(masterFolder, template, "Testpage"), Page::publish));

		String branchName = supply(() -> MeshPublisher.getBranchName(channel, null));

		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		List<NodeObject> masterObjects = Arrays.asList(masterFolder, subFolder, testFile, testPage);
		List<NodeObject> channelObjects = Arrays.asList(masterFolder, subFolder, testFile, testPage);
		for (NodeObject o : masterObjects) {
			assertObject("Check " + o + " in master branch", mesh.client(), projectName, o, true);
		}
		for (NodeObject o : channelObjects) {
			assertObject("Check " + o + " in channel branch", mesh.client(), projectName, branchName, o, true);
		}

		// localize the master folder
		Folder localFolder = supply(() -> localize(masterFolder, channel));

		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		channelObjects = Arrays.asList(localFolder, subFolder, testFile, testPage);
		for (NodeObject o : masterObjects) {
			assertObject("Check " + o + " in master branch", mesh.client(), projectName, o, true);
		}
		for (NodeObject o : channelObjects) {
			assertObject("Check " + o + " in channel branch", mesh.client(), projectName, branchName, o, true);
		}

		// unlocalize the folder
		consume(Folder::delete, localFolder);

		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		channelObjects = Arrays.asList(masterFolder, subFolder, testFile, testPage);
		for (NodeObject o : masterObjects) {
			assertObject("Check " + o + " in master branch", mesh.client(), projectName, o, true);
		}
		for (NodeObject o : channelObjects) {
			assertObject("Check " + o + " in channel branch", mesh.client(), projectName, branchName, o, true);
		}
	}

	/**
	 * Test moving a local folder into a new inherited parent folder within the same publish process
	 * without instant publishing
	 * @throws Exception
	 */
	@Test
	public void testMoveLocalFolderIntoNewInheritedParent() throws Exception {
		// create inherited folder and local subfolder
		Folder folder1 = supply(() -> createFolder(node.getFolder(), "Folder #1"));
		Folder subFolder = supply(() -> createFolder(folder1, "Subfolder", channel));

		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		// create new inherited folder
		Folder folder2 = supply(() -> createFolder(node.getFolder(), "Folder #2"));
		// move local subfolder into new inherited folder
		subFolder = update(subFolder, f -> f.move(folder2, channel.getId()));

		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}
	}

	/**
	 * Test setting/removing alternative mesh project names
	 * @throws Exception
	 */
	@Test
	public void testAlternativeMeshProjectName() throws Exception {
		// 1. Check default behaviour
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		assertMeshProject(mesh.client(), projectName, SCHEMA_PREFIX, MeshPublisher.getMeshUuid(node), true);
		assertBranch(mesh.client(), projectName, isChannelBranch(channel), true, branchName, "channel.com", "/prefix", false);

		// 2. Change mesh project name for node and channel
		String alternativeMasterMeshProjectName = "alternative_node";
		String alternativeChannelMeshProjectName = "alternative_channel";

		node = Builder.update(node, upd -> {
			upd.setMeshProjectName(alternativeMasterMeshProjectName);
		}).build();

		channel = Builder.update(channel, upd -> {
			upd.setMeshProjectName(alternativeChannelMeshProjectName);
		}).build();

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		assertMeshProject(mesh.client(), projectName, false);
		assertMeshProject(mesh.client(), alternativeMasterMeshProjectName, SCHEMA_PREFIX, MeshPublisher.getMeshUuid(node), true);
		assertBranch(mesh.client(), alternativeMasterMeshProjectName, isChannelBranch(channel), true, alternativeChannelMeshProjectName, "channel.com", "/prefix", false);

		// 3. Reset to default behaviour
		node = Builder.update(node, upd -> {
			upd.setMeshProjectName("");
		}).build();

		channel = Builder.update(channel, upd -> {
			upd.setMeshProjectName("");
		}).build();

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		assertMeshProject(mesh.client(), alternativeMasterMeshProjectName, false);
		assertMeshProject(mesh.client(), projectName, SCHEMA_PREFIX, MeshPublisher.getMeshUuid(node), true);
		assertBranch(mesh.client(), projectName, isChannelBranch(channel), true, branchName, "channel.com", "/prefix", false);
	}

	@Test
	public void testRenameInheritedFile() throws Exception {
		// create folder
		Folder folder = Builder.create(Folder.class, create -> {
			create.setMotherId(node.getFolder().getId());
			create.setName("Testfolder");
			create.setPublishDir("/publish/path");
		}).build();

		// create file in master
		File inheritedFile = supply(() -> createFile(folder, "testfile.txt", "File contents".getBytes()));

		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		assertObject("Check original file in master", mesh.client(), projectName, projectName, inheritedFile, true,
				n -> {
					MeshAssertions.assertThat(n).hasStringField("name", "testfile.txt");
					MeshAssertions.assertThat(n).hasStringField("gtx_url", "/publish/path/testfile.txt");
				});
		assertObject("Check original file in branch", mesh.client(), projectName, branchName, inheritedFile, true,
				n -> {
					MeshAssertions.assertThat(n).hasStringField("name", "testfile.txt");
					MeshAssertions.assertThat(n).hasStringField("gtx_url", "/publish/path/testfile.txt");
				});

		// change filename
		inheritedFile = update(inheritedFile, upd -> {
			upd.setName("modified.txt");
		});

		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		assertObject("Check modified file in master", mesh.client(), projectName, projectName, inheritedFile, true,
				n -> {
					MeshAssertions.assertThat(n).hasStringField("name", "modified.txt");
					MeshAssertions.assertThat(n).hasStringField("gtx_url", "/publish/path/modified.txt");
				});
		assertObject("Check modified file in branch", mesh.client(), projectName, branchName, inheritedFile, true,
				n -> {
					MeshAssertions.assertThat(n).hasStringField("name", "modified.txt");
					MeshAssertions.assertThat(n).hasStringField("gtx_url", "/publish/path/modified.txt");
				});
	}

	@Test
	public void testRenameInheritedPage() throws Exception {
		// create folder
		Folder folder = Builder.create(Folder.class, create -> {
			create.setMotherId(node.getFolder().getId());
			create.setName("Testfolder");
			create.setPublishDir("/publish/path");
		}).build();

		// create and publish page in master
		Page inheritedPage = Builder.create(Page.class, create -> {
			create.setFolder(node, folder);
			create.setTemplateId(template.getId());
			create.setName("Page");
			create.setFilename("page.html");
		}).publish().build();

		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		assertObject("Check original page in master", mesh.client(), projectName, projectName, inheritedPage, true,
				n -> {
					MeshAssertions.assertThat(n).hasStringField("filename", "page.html");
					MeshAssertions.assertThat(n).hasStringField("gtx_url", "/publish/path/page.html");
				});
		assertObject("Check original page in branch", mesh.client(), projectName, branchName, inheritedPage, true,
				n -> {
					MeshAssertions.assertThat(n).hasStringField("filename", "page.html");
					MeshAssertions.assertThat(n).hasStringField("gtx_url", "/publish/path/page.html");
				});

		// change filename
		inheritedPage = Builder.update(inheritedPage, update -> {
			update.setFilename("modified.html");
		}).publish().build();

		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		assertObject("Check modified page in master", mesh.client(), projectName, projectName, inheritedPage, true,
				n -> {
					MeshAssertions.assertThat(n).hasStringField("filename", "modified.html");
					MeshAssertions.assertThat(n).hasStringField("gtx_url", "/publish/path/modified.html");
				});
		assertObject("Check modified page in branch", mesh.client(), projectName, branchName, inheritedPage, true,
				n -> {
					MeshAssertions.assertThat(n).hasStringField("filename", "modified.html");
					MeshAssertions.assertThat(n).hasStringField("gtx_url", "/publish/path/modified.html");
				});
	}
}
