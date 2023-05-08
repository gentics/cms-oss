package com.gentics.contentnode.tests.publish.mesh;

import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.cleanMesh;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.createMeshCR;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getFileResource;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getPageResource;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Disinheritable;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.publish.PublishQueue;
import com.gentics.contentnode.publish.mesh.MeshPublisher;
import com.gentics.contentnode.rest.model.ContentRepositoryModel;
import com.gentics.contentnode.rest.model.response.ContentRepositoryResponse;
import com.gentics.contentnode.rest.resource.ContentRepositoryResource;
import com.gentics.contentnode.rest.resource.impl.ContentRepositoryResourceImpl;
import com.gentics.contentnode.tests.category.MeshTest;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.contentnode.testutils.mesh.MeshContext;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.parameter.LinkType;
import com.gentics.mesh.parameter.client.NodeParametersImpl;


@RunWith(Parameterized.class)
@GCNFeature(set = {Feature.MESH_CONTENTREPOSITORY, Feature.PUB_DIR_SEGMENT, Feature.LIVE_URLS})
@Category(MeshTest.class)
public class MeshPublishLiveUrlTest {

	/**
	 * Name of the mesh project
	 */
	public final static String MESH_PROJECT_NAME = "testproject";

	@ClassRule
	public static DBTestContext context = new DBTestContext();

	@ClassRule
	public static MeshContext mesh = new MeshContext();

	private static List<Node> nodes = new ArrayList<>();

	private static Integer crId;

	private static Template template;

	@Parameterized.Parameter(0)
	public boolean projectPerNode;

	@Parameterized.Parameters(name = "projectPerNode: {0}")
	public static Iterable<Object[]> data() {
		return Arrays.asList(
			new Object[] {false},
			new Object[] {true}
		);
	}

	/**
	 * Setup static test data
	 *
	 * @throws NodeException
	 */
	@BeforeClass
	public static void setupOnce() throws Exception {
		context.getContext().getTransaction().commit();

		crId = createMeshCR(mesh, MESH_PROJECT_NAME);

		createNodes(2);

		template = Trx.supply(() -> createTemplate(nodes.get(0).getFolder(), "Template"));
		for (Node node : nodes) {
			Folder folder = Trx.supply(node::getFolder);
			update(folder, f -> {
				f.setTemplates(Stream.concat(
					f.getTemplates().stream(),
					Stream.of(template)
				).collect(Collectors.toList()));
			});
		}
	}

	/**
	 * Creates some nodes and sets the CR and the node and folder publish dir.
	 * @param amount
	 * @throws NodeException
	 */
	private static void createNodes(int amount) throws NodeException {
		for (int i = 1; i <= amount; i++) {
			int index = i;
			Node node = Trx.supply(() -> createNode("node", "node" + index, ContentNodeTestDataUtils.PublishTarget.CONTENTREPOSITORY));

			update(node, n -> {
				n.setPubDirSegment(true);
				n.setPublishDir("nodePublishdir" + index);
				n.setContentrepositoryId(crId);
			});

			Folder folder = Trx.supply(node::getFolder);

			update(folder, f -> {
				f.setPublishDir("nodeFolderPublishDir" + index);
			});

			nodes.add(node);
		}
	}

	@Before
	public void setup() throws Exception {
		cleanMesh(mesh.client());
		Trx.operate(t -> {
			for (Node node : nodes) {
				for (Folder folder : node.getFolder().getChildFolders()) {
					t.getObject(folder, true).delete(true);
				}
			}
		});
		setProjectPerNode(projectPerNode);
	}

	/**
	 * Publish some objects and assert that the live url equals the Mesh webroot path
	 *
	 * @throws Exception
	 */
	@Test
	public void testLiveUrl() throws Exception {
		setupAndPublishData().forEach(this::assertPath);
	}

	/**
	 * Creates and publishes a page and a file for each node.
	 * @return A list of all created objects.
	 * @throws Exception
	 */
	private List<Disinheritable> setupAndPublishData() throws Exception {
		List<Disinheritable> objects = new ArrayList<>();

		for (int i = 0; i < nodes.size(); i++) {
			int index = i;
			Node node = nodes.get(i);

			Page page = create(Page.class, p -> {
				p.setTemplateId(template.getId());
				p.setFolderId(node.getFolder().getId());
				p.setName("Page" + (index + 1));
			});
			objects.add(page);

			File file = create(File.class, f -> {
				f.setFolderId(node.getFolder().getId());
				f.setName("File" + (index + 1));
				f.setMd5("abc");
				f.setFileStream(fromString("abc"));
			});
			objects.add(file);

			Trx.operate(() -> {
				PublishQueue.undirtObjects(new int[]{node.getId()}, Folder.TYPE_FOLDER, null, 0, 0);
				PublishQueue.undirtObjects(new int[]{node.getId()}, File.TYPE_FILE, null, 0, 0);
				PublishQueue.undirtObjects(new int[]{node.getId()}, Page.TYPE_PAGE, null, 0, 0);
			});
			Trx.operate(t -> t.getObject(page, true).publish());
		}

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}
		return objects;
	}

	/**
	 * Uses the rest api to set the {@link ContentRepositoryModel#setProjectPerNode(Boolean)} property.
	 * @param projectPerNode
	 */
	private void setProjectPerNode(boolean projectPerNode) {
		ContentRepositoryResource contentRepositoryResource = new ContentRepositoryResourceImpl();
		ContentRepositoryModel model = new ContentRepositoryModel();
		model.setProjectPerNode(projectPerNode);
		ContentRepositoryResponse response = null;
		try {
			response = contentRepositoryResource.update(crId.toString(), model);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		ContentNodeRESTUtils.assertResponseOK(response);
	}

	/**
	 * Asserts that the live url of the object matches the webroot path in mesh.
	 * @param object
	 */
	private void assertPath(Disinheritable object) {
		NodeResponse meshNode;
		try {
			String projectName = Trx.supply(() -> MeshPublisher.getMeshProjectName(object.getNode()));
			meshNode = mesh.client().findNodeByUuid(
				projectName,
				Trx.supply(() -> MeshPublisher.getMeshUuid(object)),
				new NodeParametersImpl().setResolveLinks(LinkType.SHORT)
			).blockingGet();
			String meshNodePath = meshNode.getPath();

			// The branch host is only set with the projectPerNode feature
			String liveUrl = projectPerNode
				? fetchLiveUrl(object)
				: stripOrigin(fetchLiveUrl(object));

			assertEquals("Paths must be equal", meshNodePath, liveUrl);
		} catch (NodeException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Uses the REST Api to fetch the live url from a page or a folder.
	 * @param object
	 * @return
	 * @throws NodeException
	 */
	private String fetchLiveUrl(NodeObject object) throws NodeException {
		if (object instanceof Page) {
			return Trx.supply(() -> getPageResource()
				.load(object.getId().toString(), false, false, false, false, false, false, false, false, false, false, null, null).getPage()
				.getLiveUrl());
		} else if (object instanceof File) {
			return Trx.supply(() -> getFileResource()
				.load(object.getId().toString(), false, false, null, null).getFile()
				.getLiveUrl());
		} else {
			throw new RuntimeException("Cannot fetch live URL for object " + object);
		}
	}

	/**
	 * Creates an {@link InputStream} from a string.
	 * @param str
	 * @return
	 */
	private InputStream fromString(String str) {
		return new ByteArrayInputStream(str.getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * Strips the origin of an url. Returns the input if the origin is already stripped.
	 * @param url
	 * @return
	 */
	private String stripOrigin(String url) {
		try {
			if (!url.contains("://")) {
				return url;
			} else {
				return new URL(url).getPath();
			}
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}
}
