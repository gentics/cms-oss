package com.gentics.contentnode.tests.publish.mesh;

import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.assertObject;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.cleanMesh;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.createMeshCR;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.getFileSchemaName;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.getPageSchemaName;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.addTagmapEntry;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFile;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFolder;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createObjectPropertyDefinition;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createPage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartTypeId;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
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
import com.gentics.contentnode.etc.Consumer;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.runtime.ConfigurationValue;
import com.gentics.contentnode.tests.category.MeshTest;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.contentnode.testutils.mesh.MeshTestRule;
import com.gentics.contentnode.testutils.mesh.MeshContext;
import com.gentics.lib.content.GenticsContentAttribute;
import com.gentics.lib.image.JavaImageUtils;
import com.gentics.mesh.core.rest.node.NodeResponse;

/**
 * Test cases for instant publishing into Mesh CR
 */
@GCNFeature(set = { Feature.MESH_CONTENTREPOSITORY, Feature.INSTANT_CR_PUBLISHING, Feature.DISABLE_INSTANT_DELETE, Feature.WASTEBIN  })
@RunWith(value = Parameterized.class)
@Category(MeshTest.class)
public class MeshInstantPublishTest {

	/**
	 * Name of the mesh project
	 */
	public final static String MESH_PROJECT_NAME = "testproject";

	@ClassRule
	public static DBTestContext context = new DBTestContext();

	@ClassRule
	public static MeshContext mesh = new MeshContext();

	private static Node node;

	private static Integer crId;

	private static Template template;

	private static Folder folder;

	@Rule
	public MeshTestRule meshTestRule = new MeshTestRule(mesh);

	/**
	 * Asserter for the url
	 */
	private static Consumer<NodeResponse> urlAsserter = node -> {
		if (Arrays.asList(getPageSchemaName(MESH_PROJECT_NAME), getFileSchemaName(MESH_PROJECT_NAME)).contains(node.getSchema().getName())) {
			String expectedUrl = String.format("{{mesh.link(%s, en, %s)}}", node.getUuid(), MeshInstantPublishTest.node.getFolder().getName());
			assertThat(node.getFields().getStringField("url")).as("url field").isNotNull();
			assertThat(node.getFields().getStringField("url").getString()).as("url field value").isEqualTo(expectedUrl);
		}
	};

	private static Construct instantPublishingConstruct;

	/**
	 * Setup static test data
	 *
	 * @throws NodeException
	 */
	@BeforeClass
	public static void setupOnce() throws Exception {
		// register new parttype
		Trx.operate(() -> {
			DBUtils.executeUpdate("INSERT INTO type (id, name, javaclass) VALUES (?, ?, ?)",
					new Object[] { 1000, "InstantPublishing", InstantPublishingPartType.class.getName() });
		});

		node = Trx.supply(() -> createNode("node", "Node", PublishTarget.CONTENTREPOSITORY));
		folder = Trx.supply(() -> createFolder(node.getFolder(), "Folder"));
		crId = createMeshCR(mesh, MESH_PROJECT_NAME);

		// create construct using the new parttype
		instantPublishingConstruct = Trx.supply(() -> create(Construct.class, construct -> {
			construct.setAutoEnable(true);
			construct.setKeyword("instant");
			construct.setName("instant", 1);

			construct.getParts().add(create(Part.class, part -> {
				part.setEditable(1);
				part.setHidden(false);
				part.setKeyname("part");
				part.setName("part", 1);
				part.setPartTypeId(getPartTypeId(InstantPublishingPartType.class));
			}, false));
		}));

		Trx.operate(() -> createObjectPropertyDefinition(Folder.TYPE_FOLDER, instantPublishingConstruct.getId(), "Instant", "object.instant"));
		Trx.operate(() -> createObjectPropertyDefinition(Page.TYPE_PAGE, instantPublishingConstruct.getId(), "Instant", "object.instant"));
		Trx.operate(() -> createObjectPropertyDefinition(File.TYPE_FILE, instantPublishingConstruct.getId(), "Instant", "object.instant"));

		Trx.operate(t -> {
			addTagmapEntry(t.getObject(ContentRepository.class, crId), Page.TYPE_PAGE, GenticsContentAttribute.ATTR_TYPE_TEXT, "page.url", "url", null, false,
					false, false, 0, null, null);
			addTagmapEntry(t.getObject(ContentRepository.class, crId), File.TYPE_FILE, GenticsContentAttribute.ATTR_TYPE_TEXT, "file.url", "url", null, false,
					false, false, 0, null, null);
			addTagmapEntry(t.getObject(ContentRepository.class, crId), Folder.TYPE_FOLDER, GenticsContentAttribute.ATTR_TYPE_TEXT, "folder.object.instant", "instant", null, false,
					false, false, 0, null, null);
			addTagmapEntry(t.getObject(ContentRepository.class, crId), Page.TYPE_PAGE, GenticsContentAttribute.ATTR_TYPE_TEXT, "page.object.instant", "instant", null, false,
					false, false, 0, null, null);
			addTagmapEntry(t.getObject(ContentRepository.class, crId), File.TYPE_FILE, GenticsContentAttribute.ATTR_TYPE_TEXT, "file.object.instant", "instant", null, false,
					false, false, 0, null, null);
		});

		Trx.operate(() -> update(node, n -> {
			n.setContentrepositoryId(crId);
		}));

		template = Trx.supply(() -> createTemplate(node.getFolder(), "Template"));
	}

	@Parameters(name = "{index}: instant {0}, repair {1}, type {2}, corrupted {3}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (boolean instant : Arrays.asList(true, false)) {
			for (boolean repair : Arrays.asList(true, false)) {
				for (int objType : Arrays.asList(Folder.TYPE_FOLDER, Page.TYPE_PAGE, File.TYPE_FILE)) {
					if (objType == File.TYPE_FILE) {
						data.add(new Object[] { instant, repair, objType, true });
					}
					data.add(new Object[] { instant, repair, objType, false });
				}
			}
		}
		return data;
	}

	/**
	 * Flag for instant publishing
	 */
	@Parameter(0)
	public boolean instant;

	/**
	 * Flag for repairing the Mesh CR
	 */
	@Parameter(1)
	public boolean repair;

	/**
	 * Object type
	 */
	@Parameter(2)
	public int objType;

	/**
	 * Should binary data be corrupted?
	 */
	@Parameter(3)
	public boolean corruptedBinary;

	@Before
	public void setup() throws Exception {
		InstantPublishingPartType.doInstantPublish = false;
		cleanMesh(mesh.client());
		Trx.operate(t -> {
			ContentRepository contentRepository = t.getObject(ContentRepository.class, crId);
			update(contentRepository, cr -> {
				cr.setInstantPublishing(instant);
			});
			if (repair) {
				assertThat(contentRepository.checkStructure(true)).as("Structure valid").isTrue();
			}
		});

		node = update(node, n -> {
			n.deactivateFeature(Feature.DISABLE_INSTANT_DELETE);
		});
	}

	/**
	 * Test creating a new object
	 *
	 * @throws NodeException
	 */
	@Test
	public void testCreate() throws NodeException {
		NodeObject object = createTestObject();

		assertObject("after creation", mesh.client(), MESH_PROJECT_NAME, object, repair && instant, urlAsserter);
	}

	/**
	 * Test instant publishing an object while it is handled by the publish process
	 * @throws Exception
	 */
	@Test
	public void testInstantWhilePublishProcess() throws Exception {
		if (repair) {
			Trx.operate(() -> {
				DBUtils.executeUpdate("DELETE FROM dirtqueue", null);
				DBUtils.executeUpdate("DELETE FROM publishqueue", null);
			});
			switch (objType) {
			case Folder.TYPE_FOLDER:
				Trx.operate(() -> create(Folder.class, create -> {
					create.setMotherId(folder.getId());
					create.setName("Folder");
					create.getObjectTag("instant").setEnabled(true);
				}));
				break;
			case Page.TYPE_PAGE:
				Trx.operate(() -> update(create(Page.class, create -> {
					create.setFolderId(folder.getId());
					create.setTemplateId(template.getId());
					create.setName("Testpage");
					create.getObjectTag("instant").setEnabled(true);
				}), Page::publish));
				break;
			case File.TYPE_FILE:
				int fileId = Trx.supply(() -> create(File.class, create -> {
					create.setFolderId(folder.getId());
					create.setFileStream(new ByteArrayInputStream("Testfile contents".getBytes()));
					create.setName("testfile.txt");
					create.getObjectTag("instant").setEnabled(true);
				}).getId());
				if (corruptedBinary) {
					java.io.File targetFile = new java.io.File(new java.io.File(ConfigurationValue.DBFILES_PATH.get()), fileId + ".bin");
					String contents = FileUtils.readFileToString(targetFile, StandardCharsets.UTF_8);
					FileUtils.write(targetFile, contents.substring(contents.length() / 2), StandardCharsets.UTF_8);
				}
				break;
			default:
				fail(String.format("Cannot test unexpected type %d", objType));
			}

			InstantPublishingPartType.doInstantPublish = true;

			Trx.operate(t -> {
				ContentRepository contentRepository = t.getObject(ContentRepository.class, crId);
				update(contentRepository, cr -> {
					cr.setInstantPublishing(true);
				});
			});
			// run publish process
			try (Trx trx = new Trx()) {
				context.publish(false);
				trx.success();
			}
		}
	}

	/**
	 * Test that creating, deleting and creating the same object will not let the following publish process fail, even if disable_instant_delete is active for the node
	 * @throws Exception
	 */
	@Test
	public void testCreateDeleteCreate() throws Exception {
		node = update(node, n -> {
			n.activateFeature(Feature.DISABLE_INSTANT_DELETE);
		});

		// create the test object
		NodeObject object = createTestObject();
		assertObject("after creation", mesh.client(), MESH_PROJECT_NAME, object, repair && instant, urlAsserter);

		// delete object (for good)
		Trx.consume(o -> o.delete(true), object);

		// create the test object again
		object = createTestObject();

		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}
	}

	/**
	 * Test that creating, deleting and creating the same object will not let the following publish process fail, even if disable_instant_delete is active for the node.
	 * This time, the "DELETE" events will be removed before the publish run
	 * @throws Exception
	 */
	@Test
	public void testCreateDeleteCreateWithRemovedEvents() throws Exception {
		node = update(node, n -> {
			n.activateFeature(Feature.DISABLE_INSTANT_DELETE);
		});

		// create the test object
		NodeObject object = createTestObject();
		assertObject("after creation", mesh.client(), MESH_PROJECT_NAME, object, repair && instant, urlAsserter);

		// delete object (for good)
		Trx.consume(o -> o.delete(true), object);

		// wait for the dirt queue worker
		try (Trx trx = new Trx()) {
			context.waitForDirtqueueWorker();
			trx.success();
		}
		// remove all "DELETE" entries from the publishqueue
		Trx.operate(() -> {
			DBUtils.deleteWithPK("publishqueue", "id", "action = ?", new String[] {"DELETE"});
		});

		// create the test object again
		object = createTestObject();

		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}
	}

	/**
	 * Test that creating, deleting (into the wastebin) and creating the same object will not let the following publish process fail, even if disable_instant_delete is active for the node
	 * @throws Exception
	 */
	@Test
	public void testCreateDeleteIntoWastebinCreate() throws Exception {
		node = update(node, n -> {
			n.activateFeature(Feature.DISABLE_INSTANT_DELETE);
		});

		// create the test object
		NodeObject object = createTestObject();
		assertObject("after creation", mesh.client(), MESH_PROJECT_NAME, object, repair && instant, urlAsserter);

		// delete object (into wastebin)
		Trx.consume(o -> o.delete(), object);

		// create the test object again
		object = createTestObject();

		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}
	}

	/**
	 * Test that creating, deleting (into the wastebin) and creating the same object will not let the following publish process fail, even if disable_instant_delete is active for the node.
	 * This time, the "DELETE" events will be removed before the publish run
	 * @throws Exception
	 */
	@Test
	public void testCreateDeleteIntoWastebinCreateWithRemovedEvents() throws Exception {
		node = update(node, n -> {
			n.activateFeature(Feature.DISABLE_INSTANT_DELETE);
		});

		// create the test object
		NodeObject object = createTestObject();
		assertObject("after creation", mesh.client(), MESH_PROJECT_NAME, object, repair && instant, urlAsserter);

		// delete object (into wastebin)
		Trx.consume(o -> o.delete(), object);

		// wait for the dirt queue worker
		try (Trx trx = new Trx()) {
			context.waitForDirtqueueWorker();
			trx.success();
		}
		// remove all "DELETE" entries from the publishqueue
		Trx.operate(() -> {
			DBUtils.deleteWithPK("publishqueue", "id", "action = ?", new String[] {"DELETE"});
		});

		// create the test object again
		object = createTestObject();

		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}
	}

	/**
	 * Create the test object
	 * @return test object
	 * @throws NodeException
	 */
	protected NodeObject createTestObject() throws NodeException {
		NodeObject object = null;
		switch (objType) {
		case Folder.TYPE_FOLDER:
			object = Trx.supply(() -> createFolder(folder, "Testfolder"));
			break;
		case Page.TYPE_PAGE:
			object = Trx.supply(() -> {
				Page page = createPage(folder, template, "Testpage");
				update(page, p -> p.publish());
				return page;
			});
			break;
		case File.TYPE_FILE:
			object = Trx.supply(() -> createFile(folder, "testfile.txt", "Testfile contents".getBytes()));
			break;
		default:
			fail(String.format("Cannot test unexpected type %d", objType));
		}
		return object;
	}
}
