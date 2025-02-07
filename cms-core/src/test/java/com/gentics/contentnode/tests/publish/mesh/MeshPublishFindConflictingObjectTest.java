package com.gentics.contentnode.tests.publish.mesh;

import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.createMeshCR;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFolder;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;
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
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
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
import com.gentics.mesh.util.UUIDUtil;
import com.gentics.testutils.GenericTestUtils;

/**
 * Test cases for {@link MeshPublisher#getNodeObject(com.gentics.contentnode.publish.mesh.MeshPublisher.MeshProject, int, String, String)}
 */
@GCNFeature(set = { Feature.MESH_CONTENTREPOSITORY })
@RunWith(value = Parameterized.class)
@Category(MeshTest.class)
public class MeshPublishFindConflictingObjectTest {
	/**
	 * Name of the mesh project
	 */
	public final static String MESH_PROJECT_NAME = "testproject";

	@ClassRule
	public static DBTestContext context = new DBTestContext();

	@ClassRule
	public static MeshContext mesh = new MeshContext();

	private static Node node;

	private static Folder folder;

	private static Integer crId;

	private static Template template;

	private static ContentLanguage de;

	@Rule
	public MeshTestRule meshTestRule = new MeshTestRule(mesh);

	@Parameters(name = "{index}: type {0}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (int objType : Arrays.asList(Folder.TYPE_FOLDER, Page.TYPE_PAGE, File.TYPE_FILE, ImageFile.TYPE_IMAGE)) {
			data.add(new Object[] { objType });
		}
		return data;
	}

	/**
	 * Setup static test data
	 * 
	 * @throws NodeException
	 */
	@BeforeClass
	public static void setupOnce() throws Exception {
		node = Trx.supply(() -> createNode("node", "Node", PublishTarget.CONTENTREPOSITORY));
		folder = Trx.supply(() -> createFolder(node.getFolder(), "Folder"));
		crId = createMeshCR(mesh, MESH_PROJECT_NAME);
		de = Trx.supply(t -> {
			Optional<Integer> deId = DBUtils.select("SELECT id FROM contentgroup WHERE code = ?", ps -> {
				ps.setString(1, "de");
			}, DBUtils.IDS).stream().findFirst();
			assertThat(deId).as("ContentLanguage 'de'").isPresent();
			return t.getObject(ContentLanguage.class, deId.get());
		});

		Trx.operate(() -> update(node, n -> {
			n.setContentrepositoryId(crId);
		}));

		template = Trx.supply(() -> createTemplate(node.getFolder(), "Template"));
	}

	/**
	 * Object type
	 */
	@Parameter(0)
	public int objType;

	/**
	 * Test loading with an inexistent UUID
	 * @throws Exception
	 */
	@Test
	public void testLoadInexistentObject() throws Exception {
		// publish
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		ContentRepository cr = Trx.supply(t -> t.getObject(ContentRepository.class, crId));
		try (Trx trx = new Trx(); MeshPublisher mp = new MeshPublisher(cr)) {
			mp.checkSchemasAndProjects(false, false);
			String meshUuid = UUIDUtil.randomUUID();
			String meshLanguage = "en";
			Optional<Pair<Integer, NodeObject>> optionalPair = mp.getNodeObject(mp.getProject(node), node.getId(), meshUuid, meshLanguage);
			assertThat(optionalPair).as("Found object").isEmpty();
		}
	}

	/**
	 * Test loading the object after publishing to Mesh
	 * @throws Exception
	 */
	@Test
	public void testLoadObject() throws Exception {
		NodeObject testedObject = null;
		switch (objType) {
		case Folder.TYPE_FOLDER:
			testedObject = Trx.supply(() -> create(Folder.class, f -> {
				f.setMotherId(folder.getId());
				f.setName("Testfolder");
			}));
			break;
		case Page.TYPE_PAGE:
			testedObject = Trx.supply(() -> update(create(Page.class, p -> {
				p.setFolderId(folder.getId());
				p.setTemplateId(template.getId());
				p.setName("Testpage");
				p.setLanguage(de);
			}), p -> p.publish()));
			break;
		case File.TYPE_FILE:
			try (InputStream in = new ByteArrayInputStream("Contents".getBytes())) {
				testedObject = Trx.supply(() -> create(File.class, f -> {
					f.setFolderId(folder.getId());
					f.setName("testfile.txt");
					f.setFileStream(in);
				}));
			}
			break;
		case ImageFile.TYPE_IMAGE:
			try (InputStream in = GenericTestUtils.getPictureResource("blume.jpg")) {
				testedObject = Trx.supply(() -> create(ImageFile.class, f -> {
					f.setFolderId(folder.getId());
					f.setName("blume.jpg");
					f.setFileStream(in);
				}));
			}
			break;
		}

		// publish
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		ContentRepository cr = Trx.supply(t -> t.getObject(ContentRepository.class, crId));
		try (Trx trx = new Trx(); MeshPublisher mp = new MeshPublisher(cr)) {
			mp.checkSchemasAndProjects(false, false);
			String meshUuid = MeshPublisher.getMeshUuid(testedObject);
			String meshLanguage = MeshPublisher.getMeshLanguage(testedObject);
			Optional<Pair<Integer, NodeObject>> optionalPair = mp.getNodeObject(mp.getProject(node), node.getId(), meshUuid, meshLanguage);
			assertThat(optionalPair).as("Found object").isNotEmpty();
			assertThat(optionalPair.get()).as("Found object").isNotNull();
			assertThat(optionalPair.get().getLeft()).as("Object type").isEqualTo(MeshPublisher.normalizeObjType(testedObject.getTType()));
			assertThat(optionalPair.get().getRight()).as("Object").isEqualTo(testedObject);
			trx.success();
		}
	}
}
