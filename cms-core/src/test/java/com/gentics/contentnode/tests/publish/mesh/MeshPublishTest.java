package com.gentics.contentnode.tests.publish.mesh;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.assertFolders;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.assertMeshProject;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.assertObject;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.assertPages;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.cleanMesh;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.crResource;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.createMeshCR;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createObjectPropertyDefinition;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.assertResponseCodeOk;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.OpResult;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.parttype.PageURLPartType;
import com.gentics.contentnode.publish.PublishInfo;
import com.gentics.contentnode.publish.PublishQueue;
import com.gentics.contentnode.publish.PublishQueue.Action;
import com.gentics.contentnode.publish.mesh.MeshPublisher;
import com.gentics.contentnode.rest.model.ContentRepositoryModel;
import com.gentics.contentnode.rest.model.ContentRepositoryModel.PasswordType;
import com.gentics.contentnode.rest.model.ContentRepositoryModel.Status;
import com.gentics.contentnode.rest.model.request.PageOfflineRequest;
import com.gentics.contentnode.rest.model.TagmapEntryListResponse;
import com.gentics.contentnode.rest.model.TagmapEntryModel;
import com.gentics.contentnode.rest.model.response.ContentRepositoryResponse;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;
import com.gentics.contentnode.tests.category.MeshTest;
import com.gentics.contentnode.tests.utils.Builder;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.contentnode.testutils.mesh.MeshContext;
import com.gentics.contentnode.testutils.mesh.MeshTestRule;
import com.gentics.mesh.core.rest.node.field.BinaryField;
import com.gentics.mesh.core.rest.node.field.NodeField;
import com.gentics.mesh.core.rest.node.field.StringField;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.testutils.GenericTestUtils;

/**
 * Test cases for publishing into Mesh CR
 */
@GCNFeature(set = { Feature.MESH_CONTENTREPOSITORY, Feature.INSTANT_CR_PUBLISHING, Feature.PUB_DIR_SEGMENT })
@Category(MeshTest.class)
public class MeshPublishTest {
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

	private static Integer contentEntryId;

	private static Integer pageUrlConstructId;

	private static Template template;

	private static Map<String, ContentLanguage> languages;

	private static String meshCrUrl;

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
		languages = Trx.supply(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			return t.getObjects(ContentLanguage.class, DBUtils.select("SELECT id FROM contentgroup", DBUtils.IDS)).stream()
					.collect(Collectors.toMap(ContentLanguage::getCode, Function.identity()));
		});
		node = supply(() -> createNode("node", "Node", PublishTarget.CONTENTREPOSITORY, languages.get("de"), languages.get("en")));
		crId = createMeshCR(mesh, MESH_PROJECT_NAME);

		meshCrUrl = supply(t -> t.getObject(ContentRepository.class, crId).getEffectiveUrl());

		TagmapEntryListResponse entriesResponse = crResource.listEntries(Integer.toString(crId), false, null, null, null);
		assertResponseCodeOk(entriesResponse);
		contentEntryId = entriesResponse.getItems().stream().filter(entry -> "content".equals(entry.getMapname())).findFirst()
				.orElseThrow(() -> new Exception("Could not find tagmap 'content'")).getId();

		Trx.operate(() -> update(node, n -> {
			n.setContentrepositoryId(crId);
		}));

		template = Trx.supply(() -> createTemplate(node.getFolder(), "Template"));

		pageUrlConstructId = Trx.supply(() -> createConstruct(node, PageURLPartType.class, "pageurl", "url"));
		Trx.operate(() -> createObjectPropertyDefinition(Folder.TYPE_FOLDER, pageUrlConstructId, "Startpage", "startpage"));
	}

	@Before
	public void setup() throws Exception {
		cleanMesh(mesh.client());
		Trx.operate(t -> {
			for (Folder folder : node.getFolder().getChildFolders()) {
				t.getObject(folder, true).delete(true);
			}
		});

		// deactivate pub_dir_segment for node
		node = update(node, n -> {
			n.setPubDirSegment(false);
		});

		// change tagmap entry "content" back to default (having no tagname)
		TagmapEntryModel contentEntry = new TagmapEntryModel();
		contentEntry.setTagname("");
		crResource.updateEntry(Integer.toString(crId), Integer.toString(contentEntryId), contentEntry);

		// disable instant publishing and set username/password and URL
		ContentRepositoryModel crModel = new ContentRepositoryModel();
		crModel.setInstantPublishing(false);
		crModel.setUsername("admin");
		crModel.setPassword("admin");
		crModel.setPasswordType(PasswordType.value);
		crModel.setUrl(meshCrUrl);
		crResource.update(Integer.toString(crId), crModel);
	}

	/**
	 * Test repairing the CR (which must create the project and the schemas)
	 * @throws Exception
	 */
	@Test
	public void testRepair() throws Exception {
		ContentRepositoryResponse response = crResource.repair(Integer.toString(crId), 0);
		ContentNodeRESTUtils.assertResponseOK(response);
		if (response.getContentRepository().getCheckStatus() != Status.ok) {
			fail(response.getContentRepository().getCheckResult());
		}

		assertMeshProject(mesh.client(), MESH_PROJECT_NAME);
	}

	/**
	 * Test empty publish process
	 * @throws Exception
	 */
	@Test
	public void testEmptyPublish() throws Exception {
		Trx.operate(() -> {
			PublishQueue.dirtFolders(new int[] {node.getId()}, null, 0, 0, Action.MODIFY);
		});

		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		assertMeshProject(mesh.client(), MESH_PROJECT_NAME);
		ProjectResponse project = mesh.client().findProjectByName(MESH_PROJECT_NAME).blockingGet();
		assertFolders(mesh.client(), MESH_PROJECT_NAME, project.getRootNode().getUuid(), Trx.supply(() -> node.getFolder()));
	}

	/**
	 * Test publishing a folder (expect mother folder to also be published)
	 * @throws Exception
	 */
	@Test
	public void testPublishFolder() throws Exception {
		Trx.operate(() -> {
			PublishQueue.undirtObjects(new int[] {node.getId()}, Folder.TYPE_FOLDER, null, 0, 0);
			PublishQueue.undirtObjects(new int[] {node.getId()}, File.TYPE_FILE, null, 0, 0);
			PublishQueue.undirtObjects(new int[] {node.getId()}, Page.TYPE_PAGE, null, 0, 0);
		});

		Folder folder = Trx.supply(() -> {
			return create(Folder.class, f -> {
				f.setMotherId(node.getFolder().getId());
				f.setName("Testfolder");
			});
		});

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		operate(() -> {
			ProjectResponse project = mesh.client().findProjectByName(MESH_PROJECT_NAME).blockingGet();
			assertFolders(mesh.client(), MESH_PROJECT_NAME, project.getRootNode().getUuid(), Trx.supply(() -> node.getFolder()));
			assertFolders(mesh.client(), MESH_PROJECT_NAME, MeshPublisher.getMeshUuid(node.getFolder()), folder);
		});
	}

	/**
	 * Test publishing a subfolder
	 * @throws Exception
	 */
	@Test
	public void testPublishSubfolder() throws Exception {
		Folder folderLevel1 = Trx.supply(() -> {
			return create(Folder.class, f -> {
				f.setMotherId(node.getFolder().getId());
				f.setName("Testfolder");
			});
		});

		Folder folderLevel2 = Trx.supply(() -> {
			return create(Folder.class, f -> {
				f.setMotherId(folderLevel1.getId());
				f.setName("Testfolder");
			});
		});

		Trx.operate(() -> {
			DBUtils.executeUpdate("DELETE FROM dirtqueue", null);
			DBUtils.executeUpdate("DELETE FROM publishqueue", null);
		});

		Folder folderLevel3 = Trx.supply(() -> {
			return create(Folder.class, f -> {
				f.setMotherId(folderLevel2.getId());
				f.setName("Testfolder");
			});
		});

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		operate(() -> {
			ProjectResponse project = mesh.client().findProjectByName(MESH_PROJECT_NAME).blockingGet();
			assertFolders(mesh.client(), MESH_PROJECT_NAME, project.getRootNode().getUuid(), Trx.supply(() -> node.getFolder()));
			assertFolders(mesh.client(), MESH_PROJECT_NAME, MeshPublisher.getMeshUuid(folderLevel2), folderLevel3);
		});
	}

	/**
	 * Test publishing a page without language
	 * @throws Exception
	 */
	@Test
	public void testPublishPageWithoutLanguage() throws Exception {
		Folder folder = Trx.supply(() -> {
			return create(Folder.class, f -> {
				f.setMotherId(node.getFolder().getId());
				f.setName("Testfolder");
			});
		});

		Page page = Trx.supply(() -> {
			return create(Page.class, p -> {
				p.setTemplateId(template.getId());
				p.setFolderId(folder.getId());
				p.setName("Page");
			});
		});

		Trx.operate(() -> {
			PublishQueue.undirtObjects(new int[] {node.getId()}, Folder.TYPE_FOLDER, null, 0, 0);
			PublishQueue.undirtObjects(new int[] {node.getId()}, File.TYPE_FILE, null, 0, 0);
			PublishQueue.undirtObjects(new int[] {node.getId()}, Page.TYPE_PAGE, null, 0, 0);
		});

		Trx.operate(t -> t.getObject(page, true).publish());

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		assertPages(mesh.client(), MESH_PROJECT_NAME, Trx.supply(() -> MeshPublisher.getMeshUuid(folder)), "en", page);
	}

	/**
	 * Test publishing a page in "en" and "de"
	 * @throws Exception
	 */
	@Test
	public void testPublishPageWithLanguages() throws Exception {
		Folder folder = Trx.supply(() -> {
			return create(Folder.class, f -> {
				f.setMotherId(node.getFolder().getId());
				f.setName("Testfolder");
			});
		});

		Page enPage = Trx.supply(() -> {
			return create(Page.class, p -> {
				p.setTemplateId(template.getId());
				p.setFolderId(folder.getId());
				p.setName("Page");
				p.setLanguage(languages.get("en"));
			});
		});

		Page dePage = Trx.supply(() -> {
			Page p = (Page) enPage.copy();
			p.setLanguage(languages.get("de"));
			p.setFilename(null);
			p.save();
			p.unlock();
			return TransactionManager.getCurrentTransaction().getObject(p);
		});

		Trx.operate(() -> {
			PublishQueue.undirtObjects(new int[] {node.getId()}, Folder.TYPE_FOLDER, null, 0, 0);
			PublishQueue.undirtObjects(new int[] {node.getId()}, File.TYPE_FILE, null, 0, 0);
			PublishQueue.undirtObjects(new int[] {node.getId()}, Page.TYPE_PAGE, null, 0, 0);
		});

		Trx.operate(t -> {
			t.getObject(enPage, true).publish();
			t.getObject(dePage, true).publish();
		});

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		assertPages(mesh.client(), MESH_PROJECT_NAME, Trx.supply(() -> MeshPublisher.getMeshUuid(folder)), "en", enPage);
		assertPages(mesh.client(), MESH_PROJECT_NAME, Trx.supply(() -> MeshPublisher.getMeshUuid(folder)), "de", dePage);
	}

	@Test
	public void testTakeLanguagesOffline() throws Exception {
		Folder folder = Trx.supply(() -> {
			return create(Folder.class, f -> {
				f.setMotherId(node.getFolder().getId());
				f.setName("Testfolder");
			});
		});

		Page enPage = Trx.supply(() -> {
			return create(Page.class, p -> {
				p.setTemplateId(template.getId());
				p.setFolderId(folder.getId());
				p.setName("Page");
				p.setLanguage(languages.get("en"));
			});
		});

		Page dePage = Trx.supply(() -> {
			Page p = (Page) enPage.copy();
			p.setLanguage(languages.get("de"));
			p.setFilename(null);
			p.save();
			p.unlock();
			return TransactionManager.getCurrentTransaction().getObject(p);
		});

		Trx.operate(() -> {
			PublishQueue.undirtObjects(new int[] {node.getId()}, Folder.TYPE_FOLDER, null, 0, 0);
			PublishQueue.undirtObjects(new int[] {node.getId()}, File.TYPE_FILE, null, 0, 0);
			PublishQueue.undirtObjects(new int[] {node.getId()}, Page.TYPE_PAGE, null, 0, 0);
		});

		Trx.operate(t -> {
			t.getObject(enPage, true).publish();
			t.getObject(dePage, true).publish();
		});

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}
		assertPages(mesh.client(), MESH_PROJECT_NAME, Trx.supply(() -> MeshPublisher.getMeshUuid(folder)), "en", enPage);
		assertPages(mesh.client(), MESH_PROJECT_NAME, Trx.supply(() -> MeshPublisher.getMeshUuid(folder)), "de", dePage);

		consume(p -> {
			PageOfflineRequest request = new PageOfflineRequest();
			request.setAlllang(false);
			GenericResponse response = new PageResourceImpl().takeOffline(String.valueOf(p.getId()), request);
			assertResponseCodeOk(response);
		}, dePage);


		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}
		assertPages(mesh.client(), MESH_PROJECT_NAME, Trx.supply(() -> MeshPublisher.getMeshUuid(folder)), "en", enPage);
		assertPages(mesh.client(), MESH_PROJECT_NAME, Trx.supply(() -> MeshPublisher.getMeshUuid(folder)), "de"); /// removed
	}

	/**
	 * Test setting the page language after it has been published
	 * @throws Exception
	 */
	@Test
	public void testSetPageLanguage() throws Exception {
		Folder folder = Trx.supply(() -> {
			return create(Folder.class, f -> {
				f.setMotherId(node.getFolder().getId());
				f.setName("Testfolder");
			});
		});

		// create and publish a page without language
		Page page = Trx.supply(() -> {
			return create(Page.class, p -> {
				p.setTemplateId(template.getId());
				p.setFolderId(folder.getId());
				p.setName("Page");
			});
		});

		Trx.operate(() -> {
			PublishQueue.undirtObjects(new int[] {node.getId()}, Folder.TYPE_FOLDER, null, 0, 0);
			PublishQueue.undirtObjects(new int[] {node.getId()}, File.TYPE_FILE, null, 0, 0);
			PublishQueue.undirtObjects(new int[] {node.getId()}, Page.TYPE_PAGE, null, 0, 0);
		});

		Trx.operate(t -> {
			t.getObject(page, true).publish();
		});

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		// page must be "en" in mesh
		assertPages(mesh.client(), MESH_PROJECT_NAME, Trx.supply(() -> MeshPublisher.getMeshUuid(folder)), "en", page);
		assertPages(mesh.client(), MESH_PROJECT_NAME, Trx.supply(() -> MeshPublisher.getMeshUuid(folder)), "de");

		// set the page language to "de"
		Trx.supply(() -> update(page, p -> {
			p.setLanguage(languages.get("de"));
			p.publish();
		}));

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		// page must be "de" in mesh now
		assertPages(mesh.client(), MESH_PROJECT_NAME, Trx.supply(() -> MeshPublisher.getMeshUuid(folder)), "de", page);
		assertPages(mesh.client(), MESH_PROJECT_NAME, Trx.supply(() -> MeshPublisher.getMeshUuid(folder)), "en");
	}

	/**
	 * Test changing the page language after it has been published
	 * @throws Exception
	 */
	@Test
	public void testChangePageLanuage() throws Exception {
		Folder folder = Trx.supply(() -> {
			return create(Folder.class, f -> {
				f.setMotherId(node.getFolder().getId());
				f.setName("Testfolder");
			});
		});

		// create and publish a page in "de"
		Page page = Trx.supply(() -> {
			return create(Page.class, p -> {
				p.setTemplateId(template.getId());
				p.setFolderId(folder.getId());
				p.setLanguage(languages.get("de"));
				p.setName("Page");
			});
		});

		Trx.operate(() -> {
			PublishQueue.undirtObjects(new int[] {node.getId()}, Folder.TYPE_FOLDER, null, 0, 0);
			PublishQueue.undirtObjects(new int[] {node.getId()}, File.TYPE_FILE, null, 0, 0);
			PublishQueue.undirtObjects(new int[] {node.getId()}, Page.TYPE_PAGE, null, 0, 0);
		});

		Trx.operate(t -> {
			t.getObject(page, true).publish();
		});

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		// page must be "de" in mesh
		assertPages(mesh.client(), MESH_PROJECT_NAME, Trx.supply(() -> MeshPublisher.getMeshUuid(folder)), "de", page);
		assertPages(mesh.client(), MESH_PROJECT_NAME, Trx.supply(() -> MeshPublisher.getMeshUuid(folder)), "en");

		// change the page language to "en"
		Trx.supply(() -> update(page, p -> {
			p.setLanguage(languages.get("en"));
			p.publish();
		}));

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		// page must be "en" in mesh now
		assertPages(mesh.client(), MESH_PROJECT_NAME, Trx.supply(() -> MeshPublisher.getMeshUuid(folder)), "en", page);
		assertPages(mesh.client(), MESH_PROJECT_NAME, Trx.supply(() -> MeshPublisher.getMeshUuid(folder)), "de");
	}

	/**
	 * Test checking the contentrepository, when many schemas have been created and assigned to the project
	 * @throws Exception
	 */
	@Test
	public void testCheckManySchemas() throws Exception {
		// Repair the CR
		ContentRepositoryResponse response = crResource.repair(Integer.toString(crId), 0);
		ContentNodeRESTUtils.assertResponseOK(response);
		if (response.getContentRepository().getCheckStatus() != Status.ok) {
			fail(response.getContentRepository().getCheckResult());
		}

		// create 100 schemas and assign to project
		for (int i = 0; i < 100; i++) {
			SchemaResponse schemaResponse = mesh.client().createSchema(
					new SchemaCreateRequest().setNoIndex((i % 2) == 0).setName(String.format("DummySchema_%d", i))).blockingGet();
			mesh.client().assignSchemaToProject(MESH_PROJECT_NAME, schemaResponse.getUuid()).blockingGet();
		}

		// check CR
		response = crResource.check(Integer.toString(crId), 0);
		ContentNodeRESTUtils.assertResponseOK(response);
		if (response.getContentRepository().getCheckStatus() != Status.ok) {
			fail(response.getContentRepository().getCheckResult());
		}
	}

	/**
	 * Test publishing a folder with a startpage into an empty Mesh CR instance
	 * @throws Exception
	 */
	@Test
	public void testPublishFolderWithStartpage() throws Exception {
		Trx.operate(() -> {
			PublishQueue.undirtObjects(new int[] {node.getId()}, Folder.TYPE_FOLDER, null, 0, 0);
			PublishQueue.undirtObjects(new int[] {node.getId()}, File.TYPE_FILE, null, 0, 0);
			PublishQueue.undirtObjects(new int[] {node.getId()}, Page.TYPE_PAGE, null, 0, 0);
		});

		Folder folder = Trx.supply(() -> {
			return create(Folder.class, f -> {
				f.setMotherId(node.getFolder().getId());
				f.setName("Testfolder");
			});
		});

		Page page = Trx.supply(() -> {
			return create(Page.class, p -> {
				p.setTemplateId(template.getId());
				p.setFolderId(folder.getId());
				p.setName("Page");
			});
		});

		Trx.operate(() -> update(folder, f -> {
			getPartType(PageURLPartType.class, f.getObjectTag("startpage"), "url").setTargetPage(page);
		}));
		Trx.operate(t -> t.getObject(page, true).publish());

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		operate(() -> {
			ProjectResponse project = mesh.client().findProjectByName(MESH_PROJECT_NAME).blockingGet();
			assertFolders(mesh.client(), MESH_PROJECT_NAME, project.getRootNode().getUuid(), Trx.supply(() -> node.getFolder()));
			assertFolders(mesh.client(), MESH_PROJECT_NAME, MeshPublisher.getMeshUuid(node.getFolder()), folder);
			assertPages(mesh.client(), MESH_PROJECT_NAME, MeshPublisher.getMeshUuid(folder), MeshPublisher.getMeshLanguage(page), page);

			assertObject("Check folder startpage", mesh.client(), MESH_PROJECT_NAME, folder, true, meshFolder -> {
				NodeField startPageField = meshFolder.getFields().getNodeField("startpage");
				assertThat(startPageField).as("Startpage").isNotNull();
				assertThat(startPageField.getUuid()).as("Startpage").isEqualTo(MeshPublisher.getMeshUuid(page));
			});
		});
	}

	/**
	 * Test publishing a folder with a startpage into an empty Mesh CR instance
	 * @throws Exception
	 */
	@Test
	public void testPublishFolderWithExternalStartpage() throws Exception {
		Trx.operate(() -> {
			PublishQueue.undirtObjects(new int[] {node.getId()}, Folder.TYPE_FOLDER, null, 0, 0);
			PublishQueue.undirtObjects(new int[] {node.getId()}, File.TYPE_FILE, null, 0, 0);
			PublishQueue.undirtObjects(new int[] {node.getId()}, Page.TYPE_PAGE, null, 0, 0);
		});

		Folder folder = Trx.supply(() -> {
			return create(Folder.class, f -> {
				f.setMotherId(node.getFolder().getId());
				f.setName("Testfolder");
			});
		});

		Page page = Trx.supply(() -> {
			return create(Page.class, p -> {
				p.setTemplateId(template.getId());
				p.setFolderId(folder.getId());
				p.setName("Page");
			});
		});

		Trx.operate(() -> update(folder, f -> {
			getPartType(PageURLPartType.class, f.getObjectTag("startpage"), "url").setExternalTarget("https://gentics.com");
		}));
		Trx.operate(t -> t.getObject(page, true).publish());

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		operate(() -> {
			ProjectResponse project = mesh.client().findProjectByName(MESH_PROJECT_NAME).blockingGet();
			assertFolders(mesh.client(), MESH_PROJECT_NAME, project.getRootNode().getUuid(), Trx.supply(() -> node.getFolder()));
			assertFolders(mesh.client(), MESH_PROJECT_NAME, MeshPublisher.getMeshUuid(node.getFolder()), folder);
			assertPages(mesh.client(), MESH_PROJECT_NAME, MeshPublisher.getMeshUuid(folder), MeshPublisher.getMeshLanguage(page), page);

			assertObject("Check folder startpage URL", mesh.client(), MESH_PROJECT_NAME, folder, true, meshFolder -> {
				StringField startPageField = meshFolder.getFields().getStringField("startpageurl");
				assertThat(startPageField).as("Startpage").isNotNull().matches(field -> "https://gentics.com".equals(field.getString()));
			});
		});
	}

	/**
	 * Test deleting a page and renaming another to the filename of the deleted page (in the same publish process)
	 * @throws Exception
	 */
	@Test
	public void testPageFilenameConflictDelete() throws Exception {
		Folder folder = Trx.supply(() -> {
			return create(Folder.class, f -> {
				f.setMotherId(node.getFolder().getId());
				f.setName("Testfolder");
			});
		});

		// create published pages "page.html" and "otherpage.html"
		Page page = Trx.supply(() -> {
			return create(Page.class, p -> {
				p.setTemplateId(template.getId());
				p.setFolderId(folder.getId());
				p.setName("Page");
				p.setFilename("page.html");
			});
		});
		Trx.consume(upd -> update(upd, Page::publish), page);
		Page otherpage = Trx.supply(() -> {
			return create(Page.class, p -> {
				p.setTemplateId(template.getId());
				p.setFolderId(folder.getId());
				p.setName("Other Page");
				p.setFilename("otherpage.html");
			});
		});
		Trx.consume(upd -> update(upd, Page::publish), otherpage);

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		// assert page existence and filenames
		assertObject("Check page", mesh.client(), MESH_PROJECT_NAME, page, true, meshPage -> {
			assertThat(meshPage.getFields().getStringField("filename").getString()).isEqualTo("page.html");
		});
		assertObject("Check otherpage", mesh.client(), MESH_PROJECT_NAME, otherpage, true, meshPage -> {
			assertThat(meshPage.getFields().getStringField("filename").getString()).isEqualTo("otherpage.html");
		});

		// delete "page.html"
		Trx.operate(t -> t.getObject(page, true).delete());

		// rename "otherpage.html" to "page.html"
		otherpage = Trx.execute(p -> update(p, upd -> {
			upd.setFilename("page.html");
		}), otherpage);
		Trx.consume(upd -> update(upd, Page::publish), otherpage);

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		// assert again
		assertObject("Check page", mesh.client(), MESH_PROJECT_NAME, page, false);
		assertObject("Check otherpage", mesh.client(), MESH_PROJECT_NAME, otherpage, true, meshPage -> {
			assertThat(meshPage.getFields().getStringField("filename").getString()).isEqualTo("page.html");
		});
	}

	/**
	 * Test moving contents from a folder and then deleting that folder
	 * @throws Exception
	 */
	@Test
	public void testMoveContentsIntoOtherNode() throws Exception {
		String otherMeshProjectName = "othertestproject";

		// create other unattended node
		Node otherNode = supply(() -> createNode("otherNode", "OtherNode", PublishTarget.CONTENTREPOSITORY, languages.get("de"), languages.get("en")));
		Integer otherCrId = createMeshCR(mesh, otherMeshProjectName);

		try {
			TagmapEntryListResponse entriesResponse = crResource.listEntries(Integer.toString(otherCrId), false, null, null, null);
			assertResponseCodeOk(entriesResponse);

			Trx.operate(() -> update(otherNode, n -> {
				n.setContentrepositoryId(otherCrId);
			}));

			Trx.operate(trx -> {
				template.getNodes().add(otherNode);
				trx.getObject(Construct.class, pageUrlConstructId).getNodes().add(otherNode);
			});

			// create folder1 containing a page
			Folder folder1 = Trx.supply(() -> {
				return create(Folder.class, f -> {
					f.setMotherId(node.getFolder().getId());
					f.setName("Testfolder 1");
					f.setPublishDir("folder1");
				});
			});

			Page page = Trx.supply(() -> {
				return create(Page.class, p -> {
					p.setTemplateId(template.getId());
					p.setFolderId(folder1.getId());
					p.setName("Page");
				});
			});
			Trx.consume(upd -> update(upd, Page::publish), page);

			// create folder2 (empty)
			Folder folder2 = Trx.supply(() -> {
				return create(Folder.class, f -> {
					f.setMotherId(otherNode.getFolder().getId());
					f.setName("Testfolder 2");
					f.setPublishDir("folder2");
				});
			});

			// publish
			try (Trx trx = new Trx()) {
				context.publish(false);
				trx.success();
			}

			// assert
			operate(() -> {
				assertObject("Check folder 1", mesh.client(), MESH_PROJECT_NAME, folder1, true);
				assertObject("Check folder 2", mesh.client(), otherMeshProjectName, folder2, true);
				assertObject("Check page", mesh.client(), MESH_PROJECT_NAME, page, true, meshPage -> {
					assertThat(meshPage.getParentNode().getUuid()).as("Parent node Uuid").isEqualTo(MeshPublisher.getMeshUuid(folder1));
				});
			});

			// move page to folder2
			Trx.operate(() -> {
				OpResult result = page.move(folder2, 0, true);
				assertThat(result.isOK()).isTrue();
			});

			// publish
			try (Trx trx = new Trx()) {
				context.publish(false);
				trx.success();
			}

			// assert
			operate(() -> {
				assertObject("Check folder 1", mesh.client(), MESH_PROJECT_NAME, folder1, true);
				assertObject("Check folder 2", mesh.client(), otherMeshProjectName, folder2, true);
				assertObject("Check page", mesh.client(), otherMeshProjectName, page, true, meshPage -> {
					assertThat(meshPage.getParentNode().getUuid()).as("Parent node Uuid").isEqualTo(MeshPublisher.getMeshUuid(folder2));
				});
			});
		} finally {
			operate(() -> otherNode.delete(true));
		}		
	}

	/**
	 * Test moving contents from a folder and then deleting that folder
	 * @throws Exception
	 */
	@Test
	public void testMoveContentsAndDeleteFolder() throws Exception {
		// create folder1 containing a page
		Folder folder1 = Trx.supply(() -> {
			return create(Folder.class, f -> {
				f.setMotherId(node.getFolder().getId());
				f.setName("Testfolder 1");
				f.setPublishDir("folder1");
			});
		});

		Page page = Trx.supply(() -> {
			return create(Page.class, p -> {
				p.setTemplateId(template.getId());
				p.setFolderId(folder1.getId());
				p.setName("Page");
			});
		});
		Trx.consume(upd -> update(upd, Page::publish), page);

		// create folder2 (empty)
		Folder folder2 = Trx.supply(() -> {
			return create(Folder.class, f -> {
				f.setMotherId(node.getFolder().getId());
				f.setName("Testfolder 2");
				f.setPublishDir("folder2");
			});
		});

		// publish
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		// assert
		operate(() -> {
			assertObject("Check folder 1", mesh.client(), MESH_PROJECT_NAME, folder1, true);
			assertObject("Check folder 2", mesh.client(), MESH_PROJECT_NAME, folder2, true);
			assertObject("Check page", mesh.client(), MESH_PROJECT_NAME, page, true, meshPage -> {
				assertThat(meshPage.getParentNode().getUuid()).as("Parent node Uuid").isEqualTo(MeshPublisher.getMeshUuid(folder1));
			});
		});

		// move page to folder2
		Trx.operate(() -> {
			OpResult result = page.move(folder2, 0, true);
			assertThat(result.isOK()).isTrue();
		});

		// delete folder1
		Trx.consume(upd -> update(upd, Folder::delete), folder1);

		// publish
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		// assert
		operate(() -> {
			assertObject("Check folder 1", mesh.client(), MESH_PROJECT_NAME, folder1, false);
			assertObject("Check folder 2", mesh.client(), MESH_PROJECT_NAME, folder2, true);
			assertObject("Check page", mesh.client(), MESH_PROJECT_NAME, page, true, meshPage -> {
				assertThat(meshPage.getParentNode().getUuid()).as("Parent node Uuid").isEqualTo(MeshPublisher.getMeshUuid(folder2));
			});
		});
	}

	/**
	 * Test data consistency check and repair
	 * @throws Exception
	 */
	@Test
	public void testDataCheck() throws Exception {
		Trx.operate(() -> {
			PublishQueue.undirtObjects(new int[] {node.getId()}, Folder.TYPE_FOLDER, null, 0, 0);
			PublishQueue.undirtObjects(new int[] {node.getId()}, File.TYPE_FILE, null, 0, 0);
			PublishQueue.undirtObjects(new int[] {node.getId()}, Page.TYPE_PAGE, null, 0, 0);
		});

		// create a folder, that will be published
		Folder folder = Trx.supply(() -> {
			return create(Folder.class, f -> {
				f.setMotherId(node.getFolder().getId());
				f.setName("Testfolder");
			});
		});

		// publish
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		// check data consistency
		Pair<Boolean, String> result = Trx.supply(t -> {
			try (MeshPublisher mp = new MeshPublisher(t.getObject(ContentRepository.class, crId))) {
				StringBuilder stringBuilder = new StringBuilder();
				boolean consistent = mp.checkDataConsistency(false, stringBuilder);
				return Pair.of(consistent, stringBuilder.toString());
			}
		});
		assertThat(result.getKey()).as("Check result after publish").isTrue();
		assertObject("Check folder after publish", mesh.client(), MESH_PROJECT_NAME, folder, true);

		// delete folder
		update(folder, Folder::delete);
	}

	/**
	 * Test publishing page.name as "content" with normal publish run
	 * @throws Exception
	 */
	@Test
	public void testPublishDataAsContent() throws Exception {
		// change tagmap entry "content" to publish the page.name instead
		TagmapEntryModel contentEntry = new TagmapEntryModel();
		contentEntry.setTagname("page.name");
		crResource.updateEntry(Integer.toString(crId), Integer.toString(contentEntryId), contentEntry);

		// create and publish a page
		Page page = Trx.supply(() -> {
			return create(Page.class, p -> {
				p.setTemplateId(template.getId());
				p.setFolderId(node.getFolder().getId());
				p.setName("Page");
			});
		});

		Trx.operate(() -> {
			PublishQueue.undirtObjects(new int[] {node.getId()}, Folder.TYPE_FOLDER, null, 0, 0);
			PublishQueue.undirtObjects(new int[] {node.getId()}, File.TYPE_FILE, null, 0, 0);
			PublishQueue.undirtObjects(new int[] {node.getId()}, Page.TYPE_PAGE, null, 0, 0);
		});

		Trx.operate(t -> {
			t.getObject(page, true).publish();
		});

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		assertObject("Check page", mesh.client(), MESH_PROJECT_NAME, page, true, meshPage -> {
			StringFieldImpl contentField = meshPage.getFields().getStringField("content");
			assertThat(contentField).as("Content Field").isNotNull();
			assertThat(contentField.getString()).as("Published content").isEqualTo(page.getName());
		});
	}

	/**
	 * Test publishing page.name as "content" with instant publishing
	 * @throws Exception
	 */
	@Test
	public void testInstantPublishDataAsContent() throws Exception {
		// activate instant publishing
		ContentRepositoryModel contentRepositoryModel = new ContentRepositoryModel();
		contentRepositoryModel.setInstantPublishing(true);
		crResource.update(Integer.toString(crId), contentRepositoryModel);

		// change tagmap entry "content" to publish the page.name instead
		TagmapEntryModel contentEntry = new TagmapEntryModel();
		contentEntry.setTagname("page.name");
		crResource.updateEntry(Integer.toString(crId), Integer.toString(contentEntryId), contentEntry);

		// repair contentrepository, so that instant publishing is possible
		ContentRepositoryResponse response = crResource.repair(Integer.toString(crId), 0);
		ContentNodeRESTUtils.assertResponseOK(response);

		// create and publish a page
		Page page = Trx.supply(() -> {
			return create(Page.class, p -> {
				p.setTemplateId(template.getId());
				p.setFolderId(node.getFolder().getId());
				p.setName("Page");
			});
		});

		Trx.operate(() -> {
			PublishQueue.undirtObjects(new int[] {node.getId()}, Folder.TYPE_FOLDER, null, 0, 0);
			PublishQueue.undirtObjects(new int[] {node.getId()}, File.TYPE_FILE, null, 0, 0);
			PublishQueue.undirtObjects(new int[] {node.getId()}, Page.TYPE_PAGE, null, 0, 0);
		});

		Trx.operate(t -> {
			t.getObject(page, true).publish();
		});

		assertObject("Check page", mesh.client(), MESH_PROJECT_NAME, page, true, meshPage -> {
			StringFieldImpl contentField = meshPage.getFields().getStringField("content");
			assertThat(contentField).as("Content Field").isNotNull();
			assertThat(contentField.getString()).as("Published content").isEqualTo(page.getName());
		});
	}

	/**
	 * Test setting the focal point for images
	 * @throws Exception
	 */
	@Test
	public void testFocalPointPublishing() throws Exception {
		Folder folder = Trx.supply(() -> {
			return create(Folder.class, f -> {
				f.setMotherId(node.getFolder().getId());
				f.setName("Testfolder");
			});
		});

		// create an image with focal point
		ImageFile image = null;
		try (InputStream inputStream = GenericTestUtils.getPictureResource("blume.jpg")) {
			image = Trx.supply(() -> {
				return create(ImageFile.class, img -> {
					img.setFolderId(folder.getId());
					img.setFileStream(inputStream);
					img.setName("testimage.jpg");
					img.setFpX(0.7f);
					img.setFpY(0.3f);
				});
			});
		}

		Trx.operate(() -> {
			PublishQueue.undirtObjects(new int[] {node.getId()}, Folder.TYPE_FOLDER, null, 0, 0);
			PublishQueue.undirtObjects(new int[] {node.getId()}, File.TYPE_FILE, null, 0, 0);
			PublishQueue.undirtObjects(new int[] {node.getId()}, Page.TYPE_PAGE, null, 0, 0);
		});

		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		// assert image
		assertObject("Image after creation", mesh.client(), MESH_PROJECT_NAME, image, true, node -> {
			assertThat(node.getVersion())
				.as("Created node version")
				.endsWith(".0");

			BinaryField binaryField = node.getFields().getBinaryField("binarycontent");
			assertThat(binaryField).as("Binary Field").isNotNull();
			assertThat(binaryField.getFocalPoint()).as("Focal Point").isNotNull().hasFieldOrPropertyWithValue("x", 0.7f).hasFieldOrPropertyWithValue("y", 0.3f);
		});

		// update focal point
		update(image, upd -> {
			upd.setFpX(0.9f);
			upd.setFpY(0.1f);
		});

		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		// assert image
		assertObject("Image after update", mesh.client(), MESH_PROJECT_NAME, image, true, node -> {
			assertThat(node.getVersion())
				.as("Updated node version")
				.endsWith(".0");

			BinaryField binaryField = node.getFields().getBinaryField("binarycontent");
			assertThat(binaryField).as("Binary Field").isNotNull();
			assertThat(binaryField.getFocalPoint()).as("Focal Point").isNotNull().hasFieldOrPropertyWithValue("x", 0.9f).hasFieldOrPropertyWithValue("y", 0.1f);
		});
	}

	/**
	 * Test recovering from a filename conflict
	 * @throws Exception
	 */
	@Test
	public void testRecoverFilenameConflict() throws Exception {
		Folder folder = Builder.create(Folder.class, f -> {
			f.setMotherId(node.getFolder().getId());
			f.setName("Testfolder");
		}).build();

		Page pageA = Builder.create(Page.class, p -> {
			p.setTemplateId(template.getId());
			p.setFolderId(folder.getId());
			p.setName("Page A");
			p.setFilename("page_a.html");
		}).publish().build();

		Page pageB = Builder.create(Page.class, p -> {
			p.setTemplateId(template.getId());
			p.setFolderId(folder.getId());
			p.setName("Page B");
			p.setFilename("page_b.html");
		}).publish().build();

		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		assertObject("Original page A", mesh.client(), MESH_PROJECT_NAME, pageA, true, node -> {
			StringFieldImpl filenameField = node.getFields().getStringField("filename");
			assertThat(filenameField).as("Filename field").isNotNull();
			assertThat(filenameField.getString()).as("Filename").isEqualTo("page_a.html");
		});
		assertObject("Original page B", mesh.client(), MESH_PROJECT_NAME, pageB, true, node -> {
			StringFieldImpl filenameField = node.getFields().getStringField("filename");
			assertThat(filenameField).as("Filename field").isNotNull();
			assertThat(filenameField.getString()).as("Filename").isEqualTo("page_b.html");
		});

		// change filename of pageB to something different
		pageB = Builder.update(pageB, upd -> upd.setFilename("page_c.html")).publish().build();
		// change filename of pageA to the former filename of pageB
		pageA = Builder.update(pageA, upd -> upd.setFilename("page_b.html")).publish().build();

		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		assertObject("Updated page A", mesh.client(), MESH_PROJECT_NAME, pageA, true, node -> {
			StringFieldImpl filenameField = node.getFields().getStringField("filename");
			assertThat(filenameField).as("Filename field").isNotNull();
			assertThat(filenameField.getString()).as("Filename").isEqualTo("page_b.html");
		});
		assertObject("Updated page B", mesh.client(), MESH_PROJECT_NAME, pageB, true, node -> {
			StringFieldImpl filenameField = node.getFields().getStringField("filename");
			assertThat(filenameField).as("Filename field").isNotNull();
			assertThat(filenameField.getString()).as("Filename").isEqualTo("page_c.html");
		});
	}

	/**
	 * Test that publishing with an unrecoverable filename conflict (two objects swapping filenames) fails
	 * @throws Exception
	 */
	@Test
	public void testUnrecoverableFilenameConflict() throws Exception {
		Folder folder = Builder.create(Folder.class, f -> {
			f.setMotherId(node.getFolder().getId());
			f.setName("Testfolder");
		}).at(1).build();

		Page pageA = Builder.create(Page.class, p -> {
			p.setTemplateId(template.getId());
			p.setFolderId(folder.getId());
			p.setName("Page A");
			p.setFilename("page_a.html");
		}).at(2).publish().build();

		Page pageB = Builder.create(Page.class, p -> {
			p.setTemplateId(template.getId());
			p.setFolderId(folder.getId());
			p.setName("Page B");
			p.setFilename("page_b.html");
		}).at(3).publish().build();

		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		assertObject("Original page A", mesh.client(), MESH_PROJECT_NAME, pageA, true, node -> {
			StringFieldImpl filenameField = node.getFields().getStringField("filename");
			assertThat(filenameField).as("Filename field").isNotNull();
			assertThat(filenameField.getString()).as("Filename").isEqualTo("page_a.html");
		});
		assertObject("Original page B", mesh.client(), MESH_PROJECT_NAME, pageB, true, node -> {
			StringFieldImpl filenameField = node.getFields().getStringField("filename");
			assertThat(filenameField).as("Filename field").isNotNull();
			assertThat(filenameField.getString()).as("Filename").isEqualTo("page_b.html");
		});

		// change filename of pageB to something different
		pageB = Builder.update(pageB, upd -> upd.setFilename("page_c.html")).at(4).publish().build();
		// change filename of pageA to the former filename of pageB
		pageA = Builder.update(pageA, upd -> upd.setFilename("page_b.html")).at(5).publish().build();
		// change filename of pageB to the former filename of pageA
		pageB = Builder.update(pageB, upd -> upd.setFilename("page_a.html")).at(6).publish().build();

		// run publish process (we expect it to fail)
		try (Trx trx = new Trx()) {
			assertThat(context.getContext().publish(false).getReturnCode()).as("Publish process return code").isEqualTo(PublishInfo.RETURN_CODE_ERROR);
			trx.success();
		}
	}

	/**
	 * Test that the publish process will fail (but not hang) if the dirted page
	 * cannot be published, because its also dirted folder cannot be published due
	 * to a conflict
	 * @throws Exception
	 */
	@Test(timeout = 60_000)
	public void testConflictInDependency() throws Exception {
		// activate pub_dir_segment for the node (helps to create a conflict)
		node = update(node, n -> {
			n.setPubDirSegment(true);
		});

		// create folder with conflicting pub dir segment
		Folder conflicting = Builder.create(Folder.class, f -> {
			f.setMotherId(node.getFolder().getId());
			f.setName("Conflict");
			f.setPublishDir("conflict");
		}).build();

		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		// change folder in CMS, so that we can create another folder with the conflict
		Builder.update(conflicting, f -> {
			f.setPublishDir("different");
		}).build();
		// "undirt" the updated folder, so that it will not be handled by the publish process
		context.waitForDirtqueueWorker();
		operate(() -> PublishQueue.undirtObjects(new int[] {node.getId()}, Folder.TYPE_FOLDER, null, -1, -1));

		// create new folder with conflicting publish dir
		Folder newFolder = Builder.create(Folder.class, f -> {
			f.setMotherId(node.getFolder().getId());
			f.setName("New Folder");
			f.setPublishDir("conflict");
		}).build();

		// create a page in the folder
		Builder.create(Page.class, p -> {
			p.setTemplateId(template.getId());
			p.setFolderId(newFolder.getId());
			p.setName("Page");
			p.setFilename("page.html");
		}).publish().build();

		// run publish process, which is expected to fail
		try (Trx trx = new Trx()) {
			assertThat(context.getContext().publish(false, true, System.currentTimeMillis(), false).getReturnCode())
					.isEqualTo(PublishInfo.RETURN_CODE_ERROR);
			trx.success();
		}
	}

	/**
	 * Test that the nodes written to Mesh are "published"
	 * @throws Exception
	 */
	@Test
	public void testMeshNodeIsPublished() throws Exception {
		Trx.operate(() -> {
			PublishQueue.undirtObjects(new int[] {node.getId()}, Folder.TYPE_FOLDER, null, 0, 0);
			PublishQueue.undirtObjects(new int[] {node.getId()}, File.TYPE_FILE, null, 0, 0);
			PublishQueue.undirtObjects(new int[] {node.getId()}, Page.TYPE_PAGE, null, 0, 0);
		});

		Folder folder = Trx.supply(() -> {
			return create(Folder.class, f -> {
				f.setMotherId(node.getFolder().getId());
				f.setName("Testfolder");
			});
		});

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		operate(() -> {
			assertObject("Published folder", mesh.client(), MESH_PROJECT_NAME, folder, true, node -> {
				assertThat(node.getVersion()).as("Node Version").endsWith(".0");
				assertThat(node.getAvailableLanguages()).as("Available languages").containsKey("en");
				assertThat(node.getAvailableLanguages().get("en")).as("English version").hasFieldOrPropertyWithValue("published", true);
			});
		});
	}

	/**
	 * Test that the {@link MeshPublisher} uses property substitution for the username/password and URL when connecting to Mesh
	 * @throws Exception
	 */
	@Test
	public void testPropertySubstitution() throws Exception {
		// update username, password and URL to a system property
		ContentRepositoryModel crModel = new ContentRepositoryModel();
		crModel.setUsernameProperty("${sys:CR_USERNAME_TEST}");
		crModel.setPasswordProperty("${sys:CR_PASSWORD_TEST}");
		crModel.setPasswordType(PasswordType.property);
		crModel.setUrlProperty("${sys:CR_URL_TEST}");
		crResource.update(Integer.toString(crId), crModel);

		// set the system properties
		System.setProperty("CR_USERNAME_TEST", "admin");
		System.setProperty("CR_PASSWORD_TEST", "admin");
		System.setProperty("CR_URL_TEST", meshCrUrl);

		// repair the CR
		ContentRepositoryResponse response = crResource.repair(Integer.toString(crId), 0);
		ContentNodeRESTUtils.assertResponseOK(response);
		if (response.getContentRepository().getCheckStatus() != Status.ok) {
			fail(response.getContentRepository().getCheckResult());
		}

		assertMeshProject(mesh.client(), MESH_PROJECT_NAME);
	}
}
