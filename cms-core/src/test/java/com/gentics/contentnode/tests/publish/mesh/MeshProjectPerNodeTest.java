package com.gentics.contentnode.tests.publish.mesh;

import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.assertFiles;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.assertFolders;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.assertMeshProject;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.assertObject;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.assertPages;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.cleanMesh;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.createMeshCR;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFile;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFolder;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createObjectPropertyDefinition;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createPage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Consumer;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.TagmapEntry.AttributeType;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.parttype.FileURLPartType;
import com.gentics.contentnode.object.parttype.PageURLPartType;
import com.gentics.contentnode.publish.PublishQueue;
import com.gentics.contentnode.publish.mesh.MeshPublisher;
import com.gentics.contentnode.tests.category.MeshTest;
import com.gentics.contentnode.tests.utils.Builder;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.contentnode.testutils.mesh.MeshContext;
import com.gentics.contentnode.testutils.mesh.MeshTestRule;
import com.gentics.mesh.core.rest.branch.BranchListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.parameter.LinkType;
import com.gentics.mesh.parameter.client.NodeParametersImpl;

/**
 * Test cases for Mesh CR with "projectPerNode" (publish every node into its own Mesh project)
 */
@GCNFeature(set = { Feature.MESH_CONTENTREPOSITORY, Feature.INSTANT_CR_PUBLISHING, Feature.PUB_DIR_SEGMENT })
@Category(MeshTest.class)
public class MeshProjectPerNodeTest {
	/**
	 * Schema prefix
	 */
	public final static String SCHEMA_PREFIX = "test";

	@ClassRule
	public static DBTestContext context = new DBTestContext();

	@ClassRule
	public static MeshContext mesh = new MeshContext();

	private static Node node1;

	private static Node node2;

	private static Node foreignNode;

	private static Integer crId;

	private static Template template;

	private static Integer pageUrlConstructId;

	private static Integer fileUrlConstructId;

	private static Template pageUrlTemplate;

	private static Template fileUrlTemplate;

	@Rule
	public MeshTestRule meshTestRule = new MeshTestRule(mesh);

	/**
	 * Create static test data
	 * @throws Exception
	 */
	@BeforeClass
	public static void setupOnce() throws Exception {
		context.getContext().getTransaction().commit();

		node1 = Trx.supply(() -> createNode("node1", "Node1", PublishTarget.CONTENTREPOSITORY));
		node2 = Trx.supply(() -> update(createNode("node2", "Node2", PublishTarget.CONTENTREPOSITORY), n -> n.setHttps(true)));
		Trx.operate(() -> update(node2.getFolder(), f -> f.setPublishDir("/home2")));
		foreignNode = Trx.supply(() -> createNode("foreign", "Foreign Node", PublishTarget.FILESYSTEM));
		crId = createMeshCR(mesh, SCHEMA_PREFIX);

		Trx.operate(t -> update(t.getObject(ContentRepository.class, crId), cr -> {
			cr.setProjectPerNode(true);
			cr.addEntry("object.startpage.parts.url", "startpage", Folder.TYPE_FOLDER, Page.TYPE_PAGE, AttributeType.link, false, false, false, false, false);
		}));

		for (Node node : Arrays.asList(node1, node2)) {
			Trx.operate(() -> update(node, n -> {
				n.setContentrepositoryId(crId);
				n.setPubDirSegment(true);
			}));
		}

		template = Trx.supply(() -> createTemplate(node1.getFolder(), "Template"));

		pageUrlConstructId = Trx.supply(() -> createConstruct(node1, PageURLPartType.class, "page", "url"));
		fileUrlConstructId = Trx.supply(() -> createConstruct(node1, FileURLPartType.class, "file", "url"));

		pageUrlTemplate = Trx.supply(() -> create(Template.class, t -> {
			t.setName("PageURL Template");
			t.addFolder(node1.getFolder());
			t.getTemplateTags().put("pageurl",
					create(TemplateTag.class, tag -> {
						tag.setConstructId(pageUrlConstructId);
						tag.setEnabled(true);
						tag.setPublic(true);
						tag.setName("pageurl");
					}, false));
			t.setSource("<node pageurl>");
		}));

		fileUrlTemplate = Trx.supply(() -> create(Template.class, t -> {
			t.setName("FileURL Template");
			t.addFolder(node1.getFolder());
			t.getTemplateTags().put("fileurl",
					create(TemplateTag.class, tag -> {
						tag.setConstructId(fileUrlConstructId);
						tag.setEnabled(true);
						tag.setPublic(true);
						tag.setName("fileurl");
					}, false));
			t.setSource("<node fileurl>");
		}));

		Trx.supply(() -> createObjectPropertyDefinition(Folder.TYPE_FOLDER, pageUrlConstructId, "Startpage", "startpage"));
	}

	/**
	 * Clean all data from previous test runs
	 * @throws Exception
	 */
	@Before
	public void setup() throws Exception {
		cleanMesh(mesh.client());

		for (Node node : Arrays.asList(node1, node2)) {
			Trx.operate(() -> {
				for (Folder f : node.getFolder().getChildFolders()) {
					f.delete(true);
				}

				for (File f : node.getFolder().getFilesAndImages()) {
					f.delete(true);
				}

				for (Page p : node.getFolder().getPages()) {
					p.delete(true);
				}
			});
		}

		List<Node> nodes = Arrays.asList(node1, node2, foreignNode);
		Trx.operate(t -> {
			for (Node node : t.getObjects(Node.class, DBUtils.select("SELECT id FROM node", DBUtils.IDS))) {
				if(!nodes.contains(node)) {
					node.delete(true);
				}
			}
		});

		Trx.operate(t -> update(t.getObject(ContentRepository.class, crId), cr -> {
			cr.setProjectPerNode(true);
			cr.setInstantPublishing(false);
		}));
	}

	/**
	 * Test existence of projects and schemas
	 * @throws Exception
	 */
	@Test
	public void testProjectPerNode() throws Exception {
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		ProjectResponse project1 = assertMeshProject(mesh.client(), "Node1", SCHEMA_PREFIX, (String) execute(MeshPublisher::getMeshUuid, node1), true);
		BranchListResponse branches = mesh.client().findBranches(project1.getName()).blockingGet();
		assertThat(branches.getData()).as(String.format("Branches of project %s", project1.getName())).hasSize(1);
		assertThat(branches.getData().get(0).getHostname()).as(String.format("Hostname of project %s", project1.getName())).isEqualTo("node1");
		assertThat(branches.getData().get(0).getSsl()).as(String.format("SSL of project %s", project1.getName())).isFalse();

		ProjectResponse project2 = assertMeshProject(mesh.client(), "Node2", SCHEMA_PREFIX, (String) execute(MeshPublisher::getMeshUuid, node2), true);
		branches = mesh.client().findBranches(project2.getName()).blockingGet();
		assertThat(branches.getData()).as(String.format("Branches of project %s", project2.getName())).hasSize(1);
		assertThat(branches.getData().get(0).getHostname()).as(String.format("Hostname of project %s", project2.getName())).isEqualTo("node2");
		assertThat(branches.getData().get(0).getSsl()).as(String.format("SSL of project %s", project2.getName())).isTrue();
	}

	/**
	 * Test changing node data
	 * @throws Exception
	 */
	@Test
	public void testChangeNode() throws Exception {
		Node node = Trx.supply(
				() -> update(createNode("beforeUpdate", "Before Update", PublishTarget.CONTENTREPOSITORY), n -> {
					n.setContentrepositoryId(crId);
					n.setPubDirSegment(true);
				}));

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		ProjectResponse project1 = assertMeshProject(mesh.client(), "Before_Update", SCHEMA_PREFIX, (String) execute(MeshPublisher::getMeshUuid, node), true);
		BranchListResponse branches = mesh.client().findBranches(project1.getName()).blockingGet();
		assertThat(branches.getData()).as(String.format("Branches of project %s", project1.getName())).hasSize(1);
		assertThat(branches.getData().get(0).getHostname()).as(String.format("Hostname of project %s", project1.getName())).isEqualTo("beforeUpdate");
		assertThat(branches.getData().get(0).getPathPrefix()).as(String.format("Path prefix of project %s", project1.getName())).isEqualTo("/Content.node/home");
		assertThat(branches.getData().get(0).getSsl()).as(String.format("SSL of project %s", project1.getName())).isFalse();

		// change node
		Trx.operate(() -> {
			update(node, n -> {
				n.setHostname("afterUpdate");
				n.setPublishDir("/after/update");
				n.setHttps(true);
				n.getFolder().setName("After Update");
			});
		});

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		project1 = assertMeshProject(mesh.client(), "After_Update", SCHEMA_PREFIX, (String) execute(MeshPublisher::getMeshUuid, node), true);
		branches = mesh.client().findBranches(project1.getName()).blockingGet();
		assertThat(branches.getData()).as(String.format("Branches of project %s", project1.getName())).hasSize(1);
		assertThat(branches.getData().get(0).getHostname()).as(String.format("Hostname of project %s", project1.getName())).isEqualTo("afterUpdate");
		assertThat(branches.getData().get(0).getSsl()).as(String.format("SSL of project %s", project1.getName())).isTrue();
	}

	/**
	 * Test publishing changes to the root folder
	 * @throws Exception
	 */
	@Test
	public void testRootFolder() throws Exception {
		Page startpage = Trx.supply(() -> update(createPage(node1.getFolder(), template, "Startpage"), Page::publish));
		Trx.operate(() -> update(node1.getFolder(), f -> {
			ObjectTag objectTag = f.getObjectTag("startpage");
			getPartType(PageURLPartType.class, objectTag, "url").setTargetPage(startpage);
		}));

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		String projectName = Trx.supply(() -> node1.getFolder().getName());
		ProjectResponse project = assertMeshProject(mesh.client(), projectName, SCHEMA_PREFIX);
		NodeResponse rootNode = mesh.client().findNodeByUuid(projectName, project.getRootNode().getUuid(), new NodeParametersImpl().setLanguages("en"))
				.toSingle().test().await().assertComplete().values().get(0);

		assertThat(rootNode.getFields().getNodeField("startpage")).isNotNull();
		assertThat(rootNode.getFields().getStringField("pub_dir")).isNull();
	}

	/**
	 * Test publishing folders in the root of Nodes
	 * @throws NodeException
	 */
	@Test
	public void testFoldersInRoot() throws Exception {
		Map<String, Folder> foldersInNodes = new HashMap<>();

		Trx.operate(() -> {
			for (Node node : Arrays.asList(node1, node2)) {
				foldersInNodes.put(node.getFolder().getName(), createFolder(node.getFolder(), "Folder"));
			}
		});

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		for (Map.Entry<String, Folder> entry : foldersInNodes.entrySet()) {
			String projectName = entry.getKey();
			Folder folder = entry.getValue();
			ProjectResponse project = assertMeshProject(mesh.client(), projectName, SCHEMA_PREFIX);
			assertFolders(mesh.client(), project.getName(), SCHEMA_PREFIX, project.getRootNode().getUuid(), folder);
		}
	}

	/**
	 * Test publishing pages in the root of Nodes
	 * @throws Exception
	 */
	@Test
	public void testPagesInRoot() throws Exception {
		Map<String, Page> pagesInNodes = new HashMap<>();

		Trx.operate(() -> {
			for (Node node : Arrays.asList(node1, node2)) {
				pagesInNodes.put(node.getFolder().getName(), update(createPage(node.getFolder(), template, "Page"), Page::publish));
			}
		});

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		for (Map.Entry<String, Page> entry : pagesInNodes.entrySet()) {
			String projectName = entry.getKey();
			Page page = entry.getValue();
			ProjectResponse project = assertMeshProject(mesh.client(), projectName, SCHEMA_PREFIX);
			assertPages(mesh.client(), project.getName(), SCHEMA_PREFIX, project.getRootNode().getUuid(), "en", page);
		}
	}

	/**
	 * Test publishing files in the root of Nodes
	 * @throws Exception
	 */
	@Test
	public void testFilesInRoot() throws Exception {
		Map<String, File> filesInNodes = new HashMap<>();

		Trx.operate(() -> {
			for (Node node : Arrays.asList(node1, node2)) {
				filesInNodes.put(node.getFolder().getName(), createFile(node.getFolder(), "file.txt", "File contents".getBytes()));
			}
		});

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		for (Map.Entry<String, File> entry : filesInNodes.entrySet()) {
			String projectName = entry.getKey();
			File file = entry.getValue();
			ProjectResponse project = assertMeshProject(mesh.client(), projectName, SCHEMA_PREFIX);
			assertFiles(mesh.client(), project.getName(), SCHEMA_PREFIX, project.getRootNode().getUuid(), file);
		}
	}

	/**
	 * Test instant publishing when root folder is changed
	 * @throws Exception
	 */
	@Test
	public void testInstantPublishRootFolder() throws Exception {
		String projectName = Trx.supply(() -> node1.getFolder().getName());

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		ProjectResponse project = assertMeshProject(mesh.client(), projectName, SCHEMA_PREFIX);
		assertFolders(mesh.client(), projectName, SCHEMA_PREFIX, project.getRootNode().getUuid());

		// activate instant publishing
		Trx.operate(t -> update(t.getObject(ContentRepository.class, crId), cr -> cr.setInstantPublishing(true)));

		// modify description of root folder
		Trx.operate(() -> update(node1.getFolder(), f -> f.setDescription("bla")));

		// there should still be no folders in the project
		assertFolders(mesh.client(), projectName, SCHEMA_PREFIX, project.getRootNode().getUuid());
	}

	/**
	 * Test linking to another page in the same node
	 * @throws Exception
	 */
	@Test
	public void testLinkToPageSameNode() throws Exception {
		String projectName = Trx.supply(() -> node1.getFolder().getName());

		Page targetPage = Trx.supply(() -> update(createPage(node1.getFolder(), template, "Target"), Page::publish));
		Page sourcePage = Trx.supply(() -> update(createPage(node1.getFolder(), pageUrlTemplate, "Source"), p -> {
			getPartType(PageURLPartType.class, p.getTag("pageurl"), "url").setTargetPage(targetPage);
			p.publish();
		}));

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		ProjectResponse project = assertMeshProject(mesh.client(), projectName, SCHEMA_PREFIX);
		assertPages(mesh.client(), projectName, SCHEMA_PREFIX, project.getRootNode().getUuid(), "en", sourcePage, targetPage);

		assertObject("", mesh.client(), projectName, sourcePage, true,
				contentAsserter(String.format("{{mesh.link(%s, en, %s)}}", (String) execute(MeshPublisher::getMeshUuid, targetPage), projectName)));
		NodeResponse node = mesh.client().findNodeByUuid(projectName, (String) execute(MeshPublisher::getMeshUuid, sourcePage),
				new NodeParametersImpl().setLanguages("en").setResolveLinks(LinkType.SHORT)).blockingGet();
		assertThat(node.getFields().getStringField("content").getString()).isEqualTo("/Content.node/home/Target.html");
	}

	/**
	 * Test linking to another page in another node
	 * @throws Exception
	 */
	@Test
	public void testLinkToPageCrossNode() throws Exception {
		String projectName = Trx.supply(() -> node1.getFolder().getName());

		Page targetPage = Trx.supply(() -> update(createPage(node2.getFolder(), template, "Target"), Page::publish));
		Page sourcePage = Trx.supply(() -> update(createPage(node1.getFolder(), pageUrlTemplate, "Source"), p -> {
			getPartType(PageURLPartType.class, p.getTag("pageurl"), "url").setTargetPage(targetPage);
			p.publish();
		}));

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		ProjectResponse project = assertMeshProject(mesh.client(), projectName, SCHEMA_PREFIX);
		assertPages(mesh.client(), projectName, SCHEMA_PREFIX, project.getRootNode().getUuid(), "en", sourcePage);

		assertObject("", mesh.client(), projectName, sourcePage, true,
				contentAsserter(String.format("{{mesh.link(%s, en, %s)}}", (String) execute(MeshPublisher::getMeshUuid, targetPage), execute(n -> n.getFolder().getName(), node2))));
		NodeResponse node = mesh.client().findNodeByUuid(projectName, (String) execute(MeshPublisher::getMeshUuid, sourcePage),
				new NodeParametersImpl().setLanguages("en").setResolveLinks(LinkType.SHORT)).blockingGet();
		assertThat(node.getFields().getStringField("content").getString()).isEqualTo("https://node2/Content.node/home2/Target.html");
	}

	/**
	 * Test linking to another page in a foreign node (not publishing into the mesh cr)
	 * @throws Exception
	 */
	@Test
	public void testLinkToPageForeignNode() throws Exception {
		String projectName = Trx.supply(() -> node1.getFolder().getName());

		Page targetPage = Trx.supply(() -> update(createPage(foreignNode.getFolder(), template, "Target"), Page::publish));
		Page sourcePage = Trx.supply(() -> update(createPage(node1.getFolder(), pageUrlTemplate, "Source"), p -> {
			getPartType(PageURLPartType.class, p.getTag("pageurl"), "url").setTargetPage(targetPage);
			p.publish();
		}));

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		ProjectResponse project = assertMeshProject(mesh.client(), projectName, SCHEMA_PREFIX);
		assertPages(mesh.client(), projectName, SCHEMA_PREFIX, project.getRootNode().getUuid(), "en", sourcePage);

		assertObject("", mesh.client(), projectName, sourcePage, true,
				contentAsserter("http://foreign/Content.node/home/Target.html"));
	}

	/**
	 * Test linking to another file in the same node
	 * @throws Exception
	 */
	@Test
	public void testLinkToFileSameNode() throws Exception {
		String projectName = Trx.supply(() -> node1.getFolder().getName());

		File targetFile = Trx.supply(() -> createFile(node1.getFolder(), "targetfile.txt", "Contents of target file".getBytes()));
		Page sourcePage = Trx.supply(() -> update(createPage(node1.getFolder(), fileUrlTemplate, "Source"), p -> {
			getPartType(FileURLPartType.class, p.getTag("fileurl"), "url").setTargetFile(targetFile);
			p.publish();
		}));

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		ProjectResponse project = assertMeshProject(mesh.client(), projectName, SCHEMA_PREFIX);
		assertPages(mesh.client(), projectName, SCHEMA_PREFIX, project.getRootNode().getUuid(), "en", sourcePage);
		assertFiles(mesh.client(), projectName, SCHEMA_PREFIX, project.getRootNode().getUuid(), targetFile);

		assertObject("", mesh.client(), projectName, sourcePage, true,
				contentAsserter(String.format("{{mesh.link(%s, en, %s)}}", (String) execute(MeshPublisher::getMeshUuid, targetFile), execute(n -> n.getFolder().getName(), node1))));
	}

	/**
	 * Test linking to another file in another node
	 * @throws Exception
	 */
	@Test
	public void testLinkToFileCrossNode() throws Exception {
		String projectName = Trx.supply(() -> node1.getFolder().getName());

		File targetFile = Trx.supply(() -> createFile(node2.getFolder(), "targetfile.txt", "Contents of target file".getBytes()));
		Page sourcePage = Trx.supply(() -> update(createPage(node1.getFolder(), fileUrlTemplate, "Source"), p -> {
			getPartType(FileURLPartType.class, p.getTag("fileurl"), "url").setTargetFile(targetFile);
			p.publish();
		}));

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		ProjectResponse project = assertMeshProject(mesh.client(), projectName, SCHEMA_PREFIX);
		assertPages(mesh.client(), projectName, SCHEMA_PREFIX, project.getRootNode().getUuid(), "en", sourcePage);
		assertFiles(mesh.client(), projectName, SCHEMA_PREFIX, project.getRootNode().getUuid());

		assertObject("", mesh.client(), projectName, sourcePage, true,
				contentAsserter(String.format("{{mesh.link(%s, en, %s)}}", (String) execute(MeshPublisher::getMeshUuid, targetFile), execute(n -> n.getFolder().getName(), node2))));
	}

	/**
	 * Test linking to another file in a foreign node (not publishing into the mesh cr)
	 * @throws Exception
	 */
	@Test
	public void testLinkToFileForeignNode() throws Exception {
		String projectName = Trx.supply(() -> node1.getFolder().getName());

		File targetFile = Trx.supply(() -> createFile(foreignNode.getFolder(), "targetfile.txt", "Contents of target file".getBytes()));
		Page sourcePage = Trx.supply(() -> update(createPage(node1.getFolder(), fileUrlTemplate, "Source"), p -> {
			getPartType(FileURLPartType.class, p.getTag("fileurl"), "url").setTargetFile(targetFile);
			p.publish();
		}));

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		ProjectResponse project = assertMeshProject(mesh.client(), projectName, SCHEMA_PREFIX);
		assertPages(mesh.client(), projectName, SCHEMA_PREFIX, project.getRootNode().getUuid(), "en", sourcePage);
		assertFiles(mesh.client(), projectName, SCHEMA_PREFIX, project.getRootNode().getUuid());

		assertObject("", mesh.client(), projectName, sourcePage, true,
				contentAsserter("http://foreign/Content.node/home/targetfile.txt"));
	}

	/**
	 * Test moving a folder from one node to the other
	 * @throws Exception
	 */
	@Test
	public void testMoveFolder() throws Exception {
		String project1Name = Trx.supply(() -> node1.getFolder().getName());
		String project2Name = Trx.supply(() -> node2.getFolder().getName());

		Folder folder = Trx.supply(() -> createFolder(node1.getFolder(), "Folder"));
		Folder subfolder = Trx.supply(() -> createFolder(folder, "Subfolder"));
		File file = Trx.supply(() -> createFile(folder, "testfile.txt", "Testfile contents".getBytes()));
		Page page = Trx.supply(() -> update(createPage(folder, template, "Page"), Page::publish));

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		ProjectResponse project1 = assertMeshProject(mesh.client(), project1Name, SCHEMA_PREFIX);
		ProjectResponse project2 = assertMeshProject(mesh.client(), project2Name, SCHEMA_PREFIX);

		assertFolders(mesh.client(), project1Name, SCHEMA_PREFIX, project1.getRootNode().getUuid(), folder);
		assertFolders(mesh.client(), project1Name, SCHEMA_PREFIX, (String) execute(MeshPublisher::getMeshUuid, folder), subfolder);
		assertFiles(mesh.client(), project1Name, SCHEMA_PREFIX, (String) execute(MeshPublisher::getMeshUuid, folder), file);
		assertPages(mesh.client(), project1Name, SCHEMA_PREFIX, (String) execute(MeshPublisher::getMeshUuid, folder), "en", page);
		assertFolders(mesh.client(), project2Name, SCHEMA_PREFIX, project2.getRootNode().getUuid());

		Trx.operate(() -> update(folder, f -> f.move(node2.getFolder(), 0)));

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		assertFolders(mesh.client(), project1Name, SCHEMA_PREFIX, project1.getRootNode().getUuid());
		assertFolders(mesh.client(), project2Name, SCHEMA_PREFIX, project2.getRootNode().getUuid(), folder);
		assertFolders(mesh.client(), project2Name, SCHEMA_PREFIX, (String) execute(MeshPublisher::getMeshUuid, folder), subfolder);
		assertFiles(mesh.client(), project2Name, SCHEMA_PREFIX, (String) execute(MeshPublisher::getMeshUuid, folder), file);
		assertPages(mesh.client(), project2Name, SCHEMA_PREFIX, (String) execute(MeshPublisher::getMeshUuid, folder), "en", page);
	}

	/**
	 * Test moving a page from one node to the other
	 * @throws Exception
	 */
	@Test
	public void testMovePage() throws Exception {
		String project1Name = Trx.supply(() -> node1.getFolder().getName());
		String project2Name = Trx.supply(() -> node2.getFolder().getName());

		Page page = Trx.supply(() -> update(createPage(node1.getFolder(), template, "Page"), Page::publish));

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		ProjectResponse project1 = assertMeshProject(mesh.client(), project1Name, SCHEMA_PREFIX);
		ProjectResponse project2 = assertMeshProject(mesh.client(), project2Name, SCHEMA_PREFIX);

		assertPages(mesh.client(), project1Name, SCHEMA_PREFIX, project1.getRootNode().getUuid(), "en", page);
		assertPages(mesh.client(), project2Name, SCHEMA_PREFIX, project2.getRootNode().getUuid(), "en");

		Trx.operate(() -> update(page, p -> p.move(node2.getFolder(), 0, true)));

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		assertPages(mesh.client(), project1Name, SCHEMA_PREFIX, project1.getRootNode().getUuid(), "en");
		assertPages(mesh.client(), project2Name, SCHEMA_PREFIX, project2.getRootNode().getUuid(), "en", page);
	}

	/**
	 * Test moving a file from one node to the other
	 * @throws Exception
	 */
	@Test
	public void testMoveFile() throws Exception {
		String project1Name = Trx.supply(() -> node1.getFolder().getName());
		String project2Name = Trx.supply(() -> node2.getFolder().getName());

		File file = Trx.supply(() -> createFile(node1.getFolder(), "testfile.txt", "Testfile contents".getBytes()));

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		ProjectResponse project1 = assertMeshProject(mesh.client(), project1Name, SCHEMA_PREFIX);
		ProjectResponse project2 = assertMeshProject(mesh.client(), project2Name, SCHEMA_PREFIX);

		assertFiles(mesh.client(), project1Name, SCHEMA_PREFIX, project1.getRootNode().getUuid(), file);
		assertFiles(mesh.client(), project2Name, SCHEMA_PREFIX, project2.getRootNode().getUuid());

		Trx.operate(() -> update(file, f -> f.move(node2.getFolder(), 0)));

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		assertFiles(mesh.client(), project1Name, SCHEMA_PREFIX, project1.getRootNode().getUuid());
		assertFiles(mesh.client(), project2Name, SCHEMA_PREFIX, project2.getRootNode().getUuid(), file);
	}

	/**
	 * Create a folder, a page and a file in every node
	 * @param foldersInNodes map that will contain the generated folders
	 * @param pagesInNodes map that will contain the generated pages
	 * @param filesInNodes map that will contain the generated files
	 * @throws NodeException
	 */
	protected void createTestData(Map<Node, Folder> foldersInNodes, Map<Node, Page> pagesInNodes, Map<Node, File> filesInNodes) throws NodeException {
		Trx.operate(() -> {
			for (Node node : Arrays.asList(node1, node2)) {
				foldersInNodes.put(node, createFolder(node.getFolder(), "Folder"));
			}
		});
		Trx.operate(() -> {
			for (Node node : Arrays.asList(node1, node2)) {
				pagesInNodes.put(node, update(createPage(node.getFolder(), template, "Page"), Page::publish));
			}
		});
		Trx.operate(() -> {
			for (Node node : Arrays.asList(node1, node2)) {
				filesInNodes.put(node, createFile(node.getFolder(), "file.txt", "File contents".getBytes()));
			}
		});
	}

	/**
	 * Assert existence of test data in the single project
	 * @param foldersInNodes map of folders
	 * @param pagesInNodes map of pages
	 * @param filesInNodes map of files
	 * @throws NodeException
	 */
	protected void assertSingleProject(Map<Node, Folder> foldersInNodes, Map<Node, Page> pagesInNodes, Map<Node, File> filesInNodes) throws NodeException {
		ProjectResponse singleProject = assertMeshProject(mesh.client(), SCHEMA_PREFIX);
		Trx.operate(() -> assertFolders(mesh.client(), SCHEMA_PREFIX, singleProject.getRootNode().getUuid(), node1.getFolder(), node2.getFolder()));
		Trx.operate(() -> {
			for (Map.Entry<Node, Page> entry : pagesInNodes.entrySet()) {
				Node node = entry.getKey();
				Page page = entry.getValue();
				assertPages(mesh.client(), SCHEMA_PREFIX, MeshPublisher.getMeshUuid(node.getFolder()), "en", page);
			}
			for (Map.Entry<Node, Folder> entry : foldersInNodes.entrySet()) {
				Node node = entry.getKey();
				Folder folder = entry.getValue();
				assertFolders(mesh.client(), SCHEMA_PREFIX, MeshPublisher.getMeshUuid(node.getFolder()), folder);
			}
			for (Map.Entry<Node, File> entry : filesInNodes.entrySet()) {
				Node node = entry.getKey();
				File file = entry.getValue();
				assertFiles(mesh.client(), SCHEMA_PREFIX, MeshPublisher.getMeshUuid(node.getFolder()), file);
			}
		});
	}

	/**
	 * Assert existence of test data in the node specific projects
	 * @param foldersInNodes map of folders
	 * @param pagesInNodes map of pages
	 * @param filesInNodes map of files
	 * @throws NodeException
	 */
	protected void assertMultiProject(Map<Node, Folder> foldersInNodes, Map<Node, Page> pagesInNodes, Map<Node, File> filesInNodes) throws NodeException {
		Trx.operate(() -> {
			for (Map.Entry<Node, Folder> entry : foldersInNodes.entrySet()) {
				Node node = entry.getKey();
				Folder folder = entry.getValue();
				ProjectResponse project = assertMeshProject(mesh.client(), getProjectName(node), SCHEMA_PREFIX);
				assertFolders(mesh.client(), project.getName(), SCHEMA_PREFIX, project.getRootNode().getUuid(), folder);
			}
			for (Map.Entry<Node, Page> entry : pagesInNodes.entrySet()) {
				Node node = entry.getKey();
				Page page = entry.getValue();
				ProjectResponse project = assertMeshProject(mesh.client(), getProjectName(node), SCHEMA_PREFIX);
				assertPages(mesh.client(), project.getName(), SCHEMA_PREFIX, project.getRootNode().getUuid(), "en", page);
			}
			for (Map.Entry<Node, File> entry : filesInNodes.entrySet()) {
				Node node = entry.getKey();
				File file = entry.getValue();
				ProjectResponse project = assertMeshProject(mesh.client(), getProjectName(node), SCHEMA_PREFIX);
				assertFiles(mesh.client(), project.getName(), SCHEMA_PREFIX, project.getRootNode().getUuid(), file);
			}
		});
	}

	/**
	 * Assert that the project with given name exists and is empty
	 * @param name project name
	 * @throws NodeException
	 */
	protected void assertEmptyProject(String name) throws NodeException {
		ProjectResponse project = assertMeshProject(mesh.client(), name, SCHEMA_PREFIX);
		assertPages(mesh.client(), project.getName(), SCHEMA_PREFIX, project.getRootNode().getUuid(), "en");
		assertFolders(mesh.client(), project.getName(), SCHEMA_PREFIX, project.getRootNode().getUuid());
		assertFiles(mesh.client(), project.getName(), SCHEMA_PREFIX, project.getRootNode().getUuid());
	}

	/**
	 * Test switching the option on
	 * @throws Exception
	 */
	@Test
	public void testSwitchOn() throws Exception {
		Trx.operate(t -> update(t.getObject(ContentRepository.class, crId), cr -> cr.setProjectPerNode(false)));

		Map<Node, Folder> foldersInNodes = new HashMap<>();
		Map<Node, Page> pagesInNodes = new HashMap<>();
		Map<Node, File> filesInNodes = new HashMap<>();
		createTestData(foldersInNodes, pagesInNodes, filesInNodes);

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		assertMeshProject(mesh.client(), getProjectName(node1), false);
		assertMeshProject(mesh.client(), getProjectName(node2), false);
		assertSingleProject(foldersInNodes, pagesInNodes, filesInNodes);

		Trx.operate(t -> update(t.getObject(ContentRepository.class, crId), cr -> cr.setProjectPerNode(true)));

		Trx.operate(() -> {
			int[] nodeIds = new int[] { node1.getId(), node2.getId() };
			PublishQueue.dirtPublishedPages(nodeIds, null, 0, 0, PublishQueue.Action.DEPENDENCY);
			PublishQueue.dirtImagesAndFiles(nodeIds, null, 0, 0, PublishQueue.Action.DEPENDENCY);
			PublishQueue.dirtFolders(nodeIds, null, 0, 0, PublishQueue.Action.DEPENDENCY);
		});

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		assertEmptyProject(SCHEMA_PREFIX);
		assertMultiProject(foldersInNodes, pagesInNodes, filesInNodes);
	}

	/**
	 * Test switching the option off
	 * @throws Exception
	 */
	@Test
	public void testSwitchOff() throws Exception {
		Map<Node, Folder> foldersInNodes = new HashMap<>();
		Map<Node, Page> pagesInNodes = new HashMap<>();
		Map<Node, File> filesInNodes = new HashMap<>();
		createTestData(foldersInNodes, pagesInNodes, filesInNodes);

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		assertMeshProject(mesh.client(), SCHEMA_PREFIX, false);
		assertMultiProject(foldersInNodes, pagesInNodes, filesInNodes);

		Trx.operate(t -> update(t.getObject(ContentRepository.class, crId), cr -> cr.setProjectPerNode(false)));

		Trx.operate(() -> {
			int[] nodeIds = new int[] { node1.getId(), node2.getId() };
			PublishQueue.dirtPublishedPages(nodeIds, null, 0, 0, PublishQueue.Action.DEPENDENCY);
			PublishQueue.dirtImagesAndFiles(nodeIds, null, 0, 0, PublishQueue.Action.DEPENDENCY);
			PublishQueue.dirtFolders(nodeIds, null, 0, 0, PublishQueue.Action.DEPENDENCY);
		});

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		for (Node node : Arrays.asList(node1, node2)) {
			assertEmptyProject(getProjectName(node));
		}
		assertSingleProject(foldersInNodes, pagesInNodes, filesInNodes);
	}

	/**
	 * Test instant publishing of a page after the folder was moved into another node
	 * @throws Exception
	 */
	@Test
	public void testInstantPublishingAfterFolderMove() throws Exception {
		// create folder containing page
		Folder folder = Trx.supply(() -> createFolder(node1.getFolder(), "Folder"));
		Page page = Trx.supply(() -> update(createPage(folder, template, "Page"), Page::publish));

		// publish everything
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		// folder and page must exist in mesh
		ProjectResponse project1 = assertMeshProject(mesh.client(), getProjectName(node1), SCHEMA_PREFIX);
		assertMeshProject(mesh.client(), getProjectName(node2), SCHEMA_PREFIX);

		assertFolders(mesh.client(), project1.getName(), SCHEMA_PREFIX, project1.getRootNode().getUuid(), folder);
		assertPages(mesh.client(), project1.getName(), SCHEMA_PREFIX, (String) execute(MeshPublisher::getMeshUuid, folder), "en", page);

		// move folder to other node
		Trx.operate(() -> update(folder, f -> f.move(node2.getFolder(), 0)));

		// folder and page must still exist in mesh
		assertFolders(mesh.client(), project1.getName(), SCHEMA_PREFIX, project1.getRootNode().getUuid(), folder);
		assertPages(mesh.client(), project1.getName(), SCHEMA_PREFIX, (String) execute(MeshPublisher::getMeshUuid, folder), "en", page);

		// activate instant publishing
		Trx.operate(t -> update(t.getObject(ContentRepository.class, crId), cr -> cr.setInstantPublishing(true)));

		// republish page (triggers instant publishing)
		Trx.operate(() -> update(page, Page::publish));

		// folder and page must still exist in mesh
		assertFolders(mesh.client(), project1.getName(), SCHEMA_PREFIX, project1.getRootNode().getUuid(), folder);
		assertPages(mesh.client(), project1.getName(), SCHEMA_PREFIX, (String) execute(MeshPublisher::getMeshUuid, folder), "en", page);
	}

	/**
	 * Test publishing with "projectPerNode" when node name is equal to publish prefix
	 * @throws Exception
	 */
	@Test
	public void testIdenticalNodeAndProjectName() throws Exception {
		Node testNode = Trx.supply(() -> createNode(SCHEMA_PREFIX, SCHEMA_PREFIX, PublishTarget.CONTENTREPOSITORY));
		Trx.operate(() -> update(testNode.getFolder(), f -> f.setPublishDir("/home3")));
		Trx.operate(() -> update(testNode, n -> {
			n.setContentrepositoryId(crId);
			n.setPubDirSegment(true);
		}));

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		Folder folder = Trx.supply(() -> createFolder(testNode.getFolder(), "Folder"));
		Page page = Trx.execute(f -> update(createPage(f, template, "Page"), Page::publish), folder);

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		folder = Trx.execute(f -> update(f, upd -> upd.setName("New Folder")), folder);
		page = Trx.execute(p -> update(p, upd -> upd.setName("New Page")), page);

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}
	}

	/**
	 * Test setting/removing an alternative mesh project name
	 * @throws Exception
	 */
	@Test
	public void testAlternativeMeshProjectName() throws Exception {
		String alternativeMeshProjectName = "other_mesh_project";

		Node testNode = Trx.supply(() -> createNode("testnode", "testnode", PublishTarget.CONTENTREPOSITORY));

		Builder.update(execute(Node::getFolder, testNode), upd -> {
			upd.setPublishDir("/home3");
		}).build();

		testNode = Builder.update(testNode, upd -> {
			upd.setContentrepositoryId(crId);
			upd.setPubDirSegment(true);
			upd.setMeshProjectName(alternativeMeshProjectName);
		}).build();

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		assertMeshProject(mesh.client(), alternativeMeshProjectName, SCHEMA_PREFIX, (String) execute(MeshPublisher::getMeshUuid, testNode), true);
		assertMeshProject(mesh.client(), "testnode", false);

		testNode = Builder.update(testNode, upd -> {
			upd.setMeshProjectName("");
		}).build();

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		assertMeshProject(mesh.client(), alternativeMeshProjectName, false);
		assertMeshProject(mesh.client(), "testnode", SCHEMA_PREFIX, (String) execute(MeshPublisher::getMeshUuid, testNode), true);
	}

	/**
	 * Get an asserter for the expected content
	 * @param expectedContent expected content
	 * @return consumer asserting the expected content
	 */
	protected Consumer<NodeResponse> contentAsserter(String expectedContent) {
		return resp -> {
			assertThat(resp.getFields().getStringField("content").getString()).as("Content of Node " + resp.getUuid()).isEqualTo(expectedContent);
		};
	}

	/**
	 * Get the default project name for the given node
	 * @param node node
	 * @return project name
	 * @throws NodeException
	 */
	protected String getProjectName(Node node) throws NodeException {
		return Trx.supply(() -> node.getFolder().getName());
	}
}
