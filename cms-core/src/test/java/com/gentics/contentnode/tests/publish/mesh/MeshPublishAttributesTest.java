package com.gentics.contentnode.tests.publish.mesh;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.assertObject;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.cleanMesh;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.createMeshCR;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.clear;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFile;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.publish.mesh.MeshPublisher;
import com.gentics.contentnode.tests.category.MeshTest;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.contentnode.testutils.mesh.MeshContext;
import com.gentics.contentnode.testutils.mesh.MeshTestRule;

/**
 * Test cases for publishing into a Mesh CR with attribute dirting
 */
@GCNFeature(set = { Feature.MESH_CONTENTREPOSITORY, Feature.ATTRIBUTE_DIRTING })
@Category(MeshTest.class)
public class MeshPublishAttributesTest {
	/**
	 * Name of the mesh project
	 */
	public final static String MESH_PROJECT_NAME = "testproject";

	@ClassRule
	public static DBTestContext context = new DBTestContext();

	@ClassRule
	public static MeshContext mesh = new MeshContext();

	private static Map<String, ContentLanguage> languages;

	private static Node node;

	private static Integer crId;

	@Rule
	public MeshTestRule meshTestRule = new MeshTestRule(mesh);

	/**
	 * Setup static test data
	 * 
	 * @throws NodeException
	 */
	@BeforeClass
	public static void setupOnce() throws Exception {
		languages = Trx.supply(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			return t.getObjects(ContentLanguage.class, DBUtils.select("SELECT id FROM contentgroup", DBUtils.IDS)).stream()
					.collect(Collectors.toMap(ContentLanguage::getCode, Function.identity()));
		});
		node = supply(() -> createNode("node", "Node", PublishTarget.CONTENTREPOSITORY, languages.get("de"), languages.get("en")));
		crId = createMeshCR(mesh, MESH_PROJECT_NAME);

		node = update(node, n -> {
			n.setContentrepositoryId(crId);
		});
	}

	@Before
	public void setup() throws Exception {
		cleanMesh(mesh.client());
		operate(() -> clear(node));
	}

	/**
	 * Test that the binarycontent is republished when the filename changes
	 * @throws Exception
	 */
	@Test
	public void testRepublishBinaryOnFilenameChange() throws Exception {
		String oldFilename = "testfile.txt";
		String newFilename = "modified-testfile.txt";
		// create file
		File file = supply(() -> createFile(node.getFolder(), oldFilename, "Test file data".getBytes()));

		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		// assert that file is published and name and binarycontent.fileName are correct
		assertObject("", mesh.client(), MeshPublisher.getMeshProjectName(node), file, true, node -> {
			assertThat(node.getFields().getStringField("name")).as("name").isNotNull().hasFieldOrPropertyWithValue("string", oldFilename);
			assertThat(node.getFields().getBinaryField("binarycontent")).as("binarycontent").isNotNull().hasFieldOrPropertyWithValue("fileName", oldFilename);
		});

		// change the filename
		file = update(file, f -> f.setName(newFilename));

		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		// assert that file is published and name and binarycontent.fileName are correct
		assertObject("", mesh.client(), MeshPublisher.getMeshProjectName(node), file, true, node -> {
			assertThat(node.getFields().getStringField("name")).as("name").isNotNull().hasFieldOrPropertyWithValue("string", newFilename);
			assertThat(node.getFields().getBinaryField("binarycontent")).as("binarycontent").isNotNull().hasFieldOrPropertyWithValue("fileName", newFilename);
		});
	}

	/**
	 * Test that the binarycontent is republished when the filetype changes
	 * @throws Exception
	 */
	@Test
	public void testRepublishBinaryOnFiletypeChange() throws Exception {
		String oldFiletype = "text/plain";
		String newFiletype = "text/html";
		// create file
		File file = supply(() -> update(createFile(node.getFolder(), "testfile.txt", "Test file data".getBytes()), upd -> {
			upd.setFiletype(oldFiletype);
		}));

		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		// assert that file is published and name and binarycontent.fileName are correct
		assertObject("", mesh.client(), MeshPublisher.getMeshProjectName(node), file, true, node -> {
			assertThat(node.getFields().getStringField("mimetype")).as("mimetype").isNotNull().hasFieldOrPropertyWithValue("string", oldFiletype);
			assertThat(node.getFields().getBinaryField("binarycontent")).as("binarycontent").isNotNull().hasFieldOrPropertyWithValue("mimeType", oldFiletype);
		});

		// change the filename
		file = update(file, f -> f.setFiletype(newFiletype));

		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		// assert that file is published and name and binarycontent.fileName are correct
		assertObject("", mesh.client(), MeshPublisher.getMeshProjectName(node), file, true, node -> {
			assertThat(node.getFields().getStringField("mimetype")).as("mimetype").isNotNull().hasFieldOrPropertyWithValue("string", newFiletype);
			assertThat(node.getFields().getBinaryField("binarycontent")).as("binarycontent").isNotNull().hasFieldOrPropertyWithValue("mimeType", newFiletype);
		});
	}
}
