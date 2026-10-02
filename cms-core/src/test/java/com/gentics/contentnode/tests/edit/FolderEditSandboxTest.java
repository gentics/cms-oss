/**
 *
 */
package com.gentics.contentnode.tests.edit;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.Builder.create;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getFolderResource;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.AbstractFolder;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.parttype.ShortTextPartType;
import com.gentics.contentnode.object.parttype.handlebars.HandlebarsPartType;
import com.gentics.contentnode.publish.PublishQueue;
import com.gentics.contentnode.rest.model.request.FolderSaveRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.resource.FolderResource;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.LoaderHelperSource;
import com.gentics.contentnode.testutils.TestHelpersHandlebarsService;

/**
 * Testcase for editing folders
 * TODO: migrate all tests to use the REST API (if possible)
 * @author norbert
 */
public class FolderEditSandboxTest extends AbstractEditSandboxTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	/**
	 * ID of the handlebars construct
	 */
	protected static int hbsConstructId;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();
		TestHelpersHandlebarsService.addHelper(LoaderHelperSource.class);

		Node node = supply(t -> t.getObject(Node.class, NODE_ID));
		hbsConstructId = supply(() -> createConstruct(node, HandlebarsPartType.class, "hbs", "hbs"));
	}

	/**
	 * constant for the id of the folder, which contains the edited folder
	 */
	public final static int MOTHER_FOLDER_ID = 5;

	/**
	 * constant for the id of the folder, which contains the folders with the pages depending on the edited folder
	 */
	public final static int TARGET_MOTHER_FOLDER_ID = 9;

	/**
	 * constant for the template id
	 */
	public final static int TEMPLATE_ID = 70;

	/**
	 * Name of the tag rendered by the template
	 */
	public final static String HTML_TAGNAME = "htmltag";

	/**
	 * Name of the part of the tag rendered by the template
	 */
	public final static String HTML_PARTNAME = "html";

	/**
	 * constant for the new folder's name
	 */
	public final static String NEW_FOLDER_NAME = "This is the new folder name";

	/**
	 * constant for the new folder's description
	 */
	public final static String NEW_FOLDER_DESCRIPTION = "This is the new folder description";

	/**
	 * constant for the new folder's pub_dir
	 */
	public final static String NEW_FOLDER_PUB_DIR = "/new/folder/pub_dir/";

	/**
	 * constant for the new folder's name_de (object property)
	 */
	public final static String NEW_FOLDER_NAME_DE = "Deutscher Name des neuen Ordners";

	/**
	 * constant for the new folder's name_en (object property)
	 */
	public final static String NEW_FOLDER_NAME_EN = "New English Name";

	/**
	 * Folder which is edited by the test
	 */
	protected Folder editedFolder;

	/**
	 * Objects created by the test case, in order of creation
	 */
	private List<NodeObject> createdObjects = new ArrayList<>();

	/**
	 * Create the folder, which is edited by the test. The folder has the object property name_en set, but not name_de
	 * @throws NodeException
	 */
	@Before
	public void setup() throws NodeException {
		editedFolder = created(create(Folder.class, f -> {
			f.setMotherId(MOTHER_FOLDER_ID);
			f.setName("Testfolder");
			f.setDescription("Description of the Testfolder");
			f.setPublishDir("/");

			ObjectTag nameEn = f.getObjectTags().get("name_en");
			nameEn.getValues().iterator().next().setValueText("Testfolder in English");
			nameEn.setEnabled(true);
		}).build());

		// a new folder gets object tags for all object properties, so remove name_de again
		operate(t -> {
			ObjectTag nameDe = t.getObject(editedFolder).getObjectTag("name_de");
			t.getObject(ObjectTag.class, nameDe.getId(), true).delete();
		});
	}

	/**
	 * Delete the objects created by the test case (in reverse order of creation) and wait for the dirtqueue worker to handle the deletion events
	 * @throws Exception
	 */
	@After
	public void tearDown() throws Exception {
		Collections.reverse(createdObjects);
		for (NodeObject object : createdObjects) {
			if (object.getId() == null) {
				continue;
			}
			try (Trx trx = new Trx()) {
				NodeObject toDelete = trx.getTransaction().getObject(object);
				if (toDelete != null) {
					toDelete.delete(true);
				}
				trx.success();
			}
		}
		testContext.waitForDirtqueueWorker();
	}

	/**
	 * Register the given object for deletion after the test case
	 * @param object created object
	 * @return object
	 */
	protected <T extends NodeObject> T created(T object) {
		createdObjects.add(object);
		return object;
	}

	/**
	 * Create a new transaction for the system user
	 * @return transaction
	 * @throws NodeException
	 */
	protected Trx trxAsSystemUser() throws NodeException {
		return new Trx(supply(t -> t.getObject(SystemUser.class, 1)));
	}

	/**
	 * Create a folder containing a page, which renders the given property of the edited folder, and publish the page (which will create the dependencies)
	 * @param property rendered property of the edited folder
	 * @return ID of the page
	 * @throws Exception
	 */
	protected int createDependentPage(String property) throws Exception {
		Folder targetFolder = created(create(Folder.class, f -> {
			f.setMotherId(TARGET_MOTHER_FOLDER_ID);
			f.setName("Target-Folders[Testfolder]");
			f.setPublishDir("/");
			f.setTemplates(Collections.singletonList(TransactionManager.getCurrentTransaction().getObject(Template.class, TEMPLATE_ID)));
		}).build());

		Page page = create(Page.class, p -> {
			p.setFolderId(targetFolder.getId());
			p.setTemplateId(TEMPLATE_ID);
			p.setName("Target[Testfolder]." + property);

			ContentTag hbsTag = p.getContent().addContentTag(hbsConstructId);
			getPartType(HandlebarsPartType.class, hbsTag, "hbs")
					.setText("{{#with (gtx_test_folder %d)}}{{gtx_render %s}}{{/with}}".formatted(editedFolder.getId(), property));
			ContentTag htmlTag = p.getContentTag(HTML_TAGNAME);
			htmlTag.getValues().getByKeyname(HTML_PARTNAME).setValueText("<node " + hbsTag.getName() + ">");
			htmlTag.setEnabled(true);
		}).publish().unlock().build();

		// handle the events of creating the test data, then publish the page to build the dependencies
		testContext.waitForDirtqueueWorker();
		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		return page.getId();
	}

	/**
	 * Test creation of a new folder
	 * @throws Exception
	 */
	@Test
	public void testCreateNewFolder() throws Exception {
		// Create a new transaction
		try (Trx trx = trxWithPermissions()) {
			Transaction t = trx.getTransaction();

			// get the template
			Template template = (Template) t.getObject(Template.class, TEMPLATE_ID);

			assertEquals("Check that template exists", true, template != null);

			// create a new folder
			Folder newFolder = (Folder) t.createObject(Folder.class);

			newFolder.setMotherId(editedFolder.getId());
			newFolder.setName(NEW_FOLDER_NAME);
			newFolder.setDescription(NEW_FOLDER_DESCRIPTION);
			newFolder.setPublishDir(NEW_FOLDER_PUB_DIR);
			newFolder.setTemplates(Collections.singletonList(template));

			ObjectTag nameDe = newFolder.getObjectTags().get("name_de");

			assertEquals("Check that name_de for new folder exists", true, nameDe != null);

			nameDe.getValues().iterator().next().setValueText(NEW_FOLDER_NAME_DE);
			nameDe.setEnabled(true);

			// save the folder
			newFolder.save();
			t.commit(false);

			// check whether the folder object has an id now
			assertEquals("Check that folder has an id now", false, AbstractFolder.isEmptyId(newFolder.getId()));

			// now check whether the folder exists
			ResultSet folderRes = testContext.getDBSQLUtils().executeQuery("SELECT * FROM folder WHERE id = " + newFolder.getId());

			assertEquals("Check number of new folders", 1, testContext.getDBSQLUtils().getSize(folderRes));

			// check folder data
			folderRes.first();
			assertEquals("Check mother of new folder", editedFolder.getId().intValue(), folderRes.getInt("mother"));
			assertEquals("Check name of new folder", NEW_FOLDER_NAME, folderRes.getString("name"));
			assertEquals("Check description of new folder", NEW_FOLDER_DESCRIPTION, folderRes.getString("description"));
			assertEquals("Check pub_dir of new folder", NEW_FOLDER_PUB_DIR, folderRes.getString("pub_dir"));

			// check linked templates
			ResultSet linkedTemplates = testContext.getDBSQLUtils().executeQuery("SELECT template_id FROM template_folder WHERE folder_id = " + newFolder.getId());

			assertEquals("Check number of linked templates", 1, testContext.getDBSQLUtils().getSize(linkedTemplates));
			linkedTemplates.first();
			assertEquals("Check id of linked template", TEMPLATE_ID, linkedTemplates.getInt("template_id"));

			// check set object property
			ResultSet objectProp = testContext.getDBSQLUtils().executeQuery(
					"SELECT * FROM objtag LEFT JOIN value ON objtag.id = value.objtag_id WHERE objtag.name = 'object.name_de' AND objtag.obj_type = 10002 AND objtag.obj_id = "
							+ newFolder.getId());

			assertEquals("Check # of values for objprop name_de", 1, testContext.getDBSQLUtils().getSize(objectProp));
			objectProp.first();
			assertEquals("Check whether objprop name_de is enabled", 1, objectProp.getInt("enabled"));
			assertEquals("Check value of objprop name_de", NEW_FOLDER_NAME_DE, objectProp.getString("value_text"));

			// check the unset object property
			objectProp = testContext.getDBSQLUtils().executeQuery(
					"SELECT * FROM objtag LEFT JOIN value ON objtag.id = value.objtag_id WHERE objtag.name = 'object.name_en' AND objtag.obj_type = 10002 AND objtag.obj_id = "
							+ newFolder.getId());
			assertEquals("Check # of values for objprop name_en", 1, testContext.getDBSQLUtils().getSize(objectProp));
			objectProp.first();
			assertEquals("Check whether objprop name_en is disabled", 0, objectProp.getInt("enabled"));

			// check that the channelset_id of the folder was set
			assertTrue("Channelset ID must be set", folderRes.getInt("channelset_id") != 0);
			trx.success();
		}
	}

	/**
	 * Tests filename sanitize.
	 * @throws Exception
	 */
	@Test
	public void testSanitizeFilename() throws Exception {
		ResultSet folder = createAndSaveFolder("casa äëï");

		folder.first();

		assertEquals("/casa-aeei/", folder.getString("pub_dir"));
	}

	/**
	 * Tests leading/trailing spaces removal
	 * @throws Exception
	 */
	@Test
	public void testLeadingTrailingSpaces() throws Exception {
		ResultSet folder = createAndSaveFolder("  leadingtrailing  ");
		folder.first();

		assertEquals("/leadingtrailing/", folder.getString("pub_dir"));
	}

	/**
	 * Creates and saves a folder.
	 * @param string
	 * @return
	 * @throws Exception
	 */
	private ResultSet createAndSaveFolder(String pubDir) throws Exception {
		// Create a new transaction
		try (Trx trx = trxWithPermissions()) {
			Transaction t = trx.getTransaction();

			// get the template
			Template template = (Template) t.getObject(Template.class, TEMPLATE_ID);

			// create a new folder
			Folder newFolder = (Folder) t.createObject(Folder.class);

			newFolder.setMotherId(editedFolder.getId());
			newFolder.setName(NEW_FOLDER_NAME);
			newFolder.setDescription(NEW_FOLDER_DESCRIPTION);
			newFolder.setPublishDir(pubDir);
			newFolder.setTemplates(Collections.singletonList(template));

			ObjectTag nameDe = newFolder.getObjectTags().get("name_de");

			assertNotNull("Object Tag name_de must exist", nameDe);

			nameDe.getValues().iterator().next().setValueText(NEW_FOLDER_NAME_DE);
			nameDe.setEnabled(true);

			// save the folder
			newFolder.save();
			t.commit(false);
			trx.success();

			// now check whether the folder exists
			return testContext.getDBSQLUtils().executeQuery(
					"SELECT * FROM folder WHERE id = " + newFolder.getId());
		}
	}

	/**
	 * Test updating a folder's name
	 * @throws Exception
	 */
	@Test
	public void testUpdateFolderName() throws Exception {
		try (Trx trx = new Trx()) {
			FolderResource folderResource = getFolderResource();
			FolderSaveRequest request = new FolderSaveRequest();
			com.gentics.contentnode.rest.model.Folder folder = new com.gentics.contentnode.rest.model.Folder();

			folder.setId(editedFolder.getId());
			folder.setName(NEW_FOLDER_NAME);
			request.setFolder(folder);
			folderResource.save(Integer.toString(editedFolder.getId()), request);
			TransactionManager.getCurrentTransaction().commit(false);

			// now check whether the folder exists
			ResultSet folderRes = testContext.getDBSQLUtils().executeQuery("SELECT * FROM folder WHERE id = " + folder.getId());

			assertEquals("Check number of updated folders", 1, testContext.getDBSQLUtils().getSize(folderRes));

			// check folder data
			folderRes.first();
			assertEquals("Check updated name of folder", NEW_FOLDER_NAME, folderRes.getString("name"));
			trx.success();
		}
	}

	/**
	 * Test updating a folder's description
	 * @throws Exception
	 */
	@Test
	public void testUpdateFolderDescription() throws Exception {
		try (Trx trx = new Trx()) {
			FolderResource folderResource = getFolderResource();
			FolderSaveRequest request = new FolderSaveRequest();
			com.gentics.contentnode.rest.model.Folder folder = new com.gentics.contentnode.rest.model.Folder();

			folder.setId(editedFolder.getId());
			folder.setDescription(NEW_FOLDER_DESCRIPTION);
			request.setFolder(folder);
			folderResource.save(Integer.toString(editedFolder.getId()), request);
			TransactionManager.getCurrentTransaction().commit(false);

			// now check whether the folder exists
			ResultSet folderRes = testContext.getDBSQLUtils().executeQuery("SELECT * FROM folder WHERE id = " + folder.getId());

			assertEquals("Check number of updated folders", 1, testContext.getDBSQLUtils().getSize(folderRes));

			// check folder data
			folderRes.first();
			assertEquals("Check updated description of folder", NEW_FOLDER_DESCRIPTION, folderRes.getString("description"));
			trx.success();
		}
	}

	/**
	 * Test updating a folder's pub_dir
	 * @throws Exception
	 */
	@Test
	public void testUpdateFolderPubDir() throws Exception {
		try (Trx trx = new Trx()) {
			FolderResource folderResource = getFolderResource();
			FolderSaveRequest request = new FolderSaveRequest();
			com.gentics.contentnode.rest.model.Folder folder = new com.gentics.contentnode.rest.model.Folder();

			folder.setId(editedFolder.getId());
			folder.setPublishDir(NEW_FOLDER_PUB_DIR);
			request.setFolder(folder);
			folderResource.save(Integer.toString(editedFolder.getId()), request);
			TransactionManager.getCurrentTransaction().commit(false);

			// now check whether the folder exists
			ResultSet folderRes = testContext.getDBSQLUtils().executeQuery("SELECT * FROM folder WHERE id = " + folder.getId());

			assertEquals("Check number of updated folders", 1, testContext.getDBSQLUtils().getSize(folderRes));

			// check folder data
			folderRes.first();
			assertEquals("Check updated pub_dir of folder", NEW_FOLDER_PUB_DIR, folderRes.getString("pub_dir"));
			trx.success();
		}
	}

	/**
	 * Test creating a folder's object property
	 * @throws Exception
	 */
	@Test
	public void testCreateFolderObjectProperty() throws Exception {
		// Create a new transaction
		try (Trx trx = trxWithPermissions()) {
			Transaction t = trx.getTransaction();

			// get the folder
			Folder folder = (Folder) t.getObject(Folder.class, editedFolder.getId(), true);
			ObjectTag nameDe = folder.getObjectTags().get("name_de");

			// check whether the object property is really not yet set
			assertEquals("Check whether objprop name_de has no id", true, ObjectTag.isEmptyId(nameDe.getId()));

			nameDe.getValues().iterator().next().setValueText(NEW_FOLDER_NAME_DE);
			nameDe.setEnabled(true);
			folder.save();
			t.commit(false);

			// check set object property
			ResultSet objectProp = testContext.getDBSQLUtils().executeQuery(
					"SELECT * FROM objtag LEFT JOIN value ON objtag.id = value.objtag_id WHERE objtag.name = 'object.name_de' AND objtag.obj_type = 10002 AND objtag.obj_id = "
							+ folder.getId());

			assertEquals("Check # of values for objprop name_de", 1, testContext.getDBSQLUtils().getSize(objectProp));
			objectProp.first();
			assertEquals("Check whether objprop name_de is enabled", 1, objectProp.getInt("enabled"));
			assertEquals("Check value of objprop name_de", NEW_FOLDER_NAME_DE, objectProp.getString("value_text"));
			trx.success();
		}
	}

	/**
	 * Test updating a folder's object property
	 * @throws Exception
	 */
	@Test
	public void testUpdateFolderObjectProperty() throws Exception {
		// Create a new transaction
		try (Trx trx = trxWithPermissions()) {
			Transaction t = trx.getTransaction();

			// get the folder
			Folder folder = (Folder) t.getObject(Folder.class, editedFolder.getId(), true);
			ObjectTag nameEn = folder.getObjectTags().get("name_en");

			nameEn.getValues().iterator().next().setValueText(NEW_FOLDER_NAME_EN);
			folder.save();
			t.commit(false);

			// check set object property
			ResultSet objectProp = testContext.getDBSQLUtils().executeQuery(
					"SELECT * FROM objtag LEFT JOIN value ON objtag.id = value.objtag_id WHERE objtag.name = 'object.name_en' AND objtag.obj_type = 10002 AND objtag.obj_id = "
							+ folder.getId());

			assertEquals("Check # of values for objprop name_en", 1, testContext.getDBSQLUtils().getSize(objectProp));
			objectProp.first();
			assertEquals("Check whether objprop name_en is enabled", 1, objectProp.getInt("enabled"));
			assertEquals("Check value of objprop name_en", NEW_FOLDER_NAME_EN, objectProp.getString("value_text"));
			trx.success();
		}
	}

	/**
	 * Test dirting after folder's name is changed
	 * @throws Exception
	 */
	@Test
	public void testDirtPageByName() throws Exception {
		int pageDependsName = createDependentPage("name");

		int dirtedPages = 0;

		// Create a new transaction
		try (Trx trx = trxWithPermissions()) {
			Transaction t = trx.getTransaction();

			dirtedPages = PublishQueue.countDirtedObjects(Page.class, false, null);

			// get the folder for editing
			Folder folder = (Folder) t.getObject(Folder.class, editedFolder.getId(), true);

			// change the folder's name
			folder.setName("This is the new folder name");
			folder.save();
			trx.success();
		}

		// wait until no more entry in dirtqueue exists (all events have been handled)
		testContext.waitForDirtqueueWorker();
		try (Trx trx = trxWithPermissions()) {
			testContext.checkDirtedPages(dirtedPages, new int[] { pageDependsName});
			trx.success();
		}
	}

	/**
	 * Test dirting after folder's description is changed
	 * @throws Exception
	 */
	@Test
	public void testDirtPageByDescription() throws Exception {
		int pageDependsDescription = createDependentPage("description");

		int dirtedPagesBeforeModification = 0;

		// Create a new transaction
		try (Trx trx = trxWithPermissions()) {
			Transaction t = trx.getTransaction();

			dirtedPagesBeforeModification = PublishQueue.countDirtedObjects(Page.class, false, null);

			// get the folder for editing
			Folder folder = (Folder) t.getObject(Folder.class, editedFolder.getId(), true);

			// change the folder's description
			folder.setDescription("This is the new folder description");
			folder.save();
			trx.success();
		}

		// wait until no more entry in dirtqueue exists (all events have been
		// handled)
		testContext.waitForDirtqueueWorker();
		try (Trx trx = trxWithPermissions()) {
			testContext.checkDirtedPages(dirtedPagesBeforeModification, new int[] { pageDependsDescription});
			trx.success();
		}
	}

	/**
	 * Test dirting after folder's pub_dir is changed
	 * @throws Exception
	 */
	@Test
	public void testDirtPageByPubDir() throws Exception {
		int pageDependsPubDir = createDependentPage("pub_dir");

		int dirtedPagesBeforeModification = 0;

		// Create a new transaction
		try (Trx trx = trxWithPermissions()) {
			Transaction t = trx.getTransaction();

			dirtedPagesBeforeModification = PublishQueue.countDirtedObjects(Page.class, false, null);

			// get the folder for editing
			Folder folder = (Folder) t.getObject(Folder.class, editedFolder.getId(), true);

			// change the folder's pub_dir
			folder.setPublishDir("/new_publish_dir");
			folder.save();
			trx.success();
		}

		// wait until no more entry in dirtqueue exists (all events have been
		// handled)
		testContext.waitForDirtqueueWorker();
		try (Trx trx = trxWithPermissions()) {
			testContext.checkDirtedPages(dirtedPagesBeforeModification, new int[] { pageDependsPubDir});
			trx.success();
		}
	}

	/**
	 * Test dirting after folder's object property is changed
	 * @throws Exception
	 */
	@Test
	public void testDirtPageByObjectProperty() throws Exception {
		int pageDependsNameEn = createDependentPage("object.name_en");

		int dirtedPagesBeforeModification = 0;

		// Create a new transaction
		try (Trx trx = trxWithPermissions()) {
			Transaction t = trx.getTransaction();

			dirtedPagesBeforeModification = PublishQueue.countDirtedObjects(Page.class, false, null);

			// get the folder for editing
			Folder folder = (Folder) t.getObject(Folder.class, editedFolder.getId(), true);
			// change the folder's object property
			ObjectTag nameEn = folder.getObjectTag("name_en");
			Value value = nameEn.getValues().iterator().next();

			value.setValueText("Modified English Name");
			folder.save();
			trx.success();
		}

		// wait until no more entry in dirtqueue exists (all events have been handled)
		testContext.waitForDirtqueueWorker();
		try (Trx trx = trxWithPermissions()) {
			testContext.checkDirtedPages(dirtedPagesBeforeModification, new int[] { pageDependsNameEn});
			trx.success();
		}
	}

	/**
	 * Test dirting after folder's object property is created
	 * @throws Exception
	 */
	@Test
	@Ignore("Fails because the necessary dependency is not written for handlebars. See SUP-20138")
	public void testDirtPageByNewObjectProperty() throws Exception {
		int pageDependsNameDe = createDependentPage("object.name_de");

		int dirtedPagesBeforeModification = 0;

		// Create a new transaction
		try (Trx trx = trxWithPermissions()) {
			Transaction t = trx.getTransaction();

			dirtedPagesBeforeModification = PublishQueue.countDirtedObjects(Page.class, false, null);

			// get the folder for editing
			Folder folder = (Folder) t.getObject(Folder.class, editedFolder.getId(), true);
			// change the folder's object property
			ObjectTag nameDe = folder.getObjectTag("name_de");

			// check that the objecttag is really new
			assertEquals("Check that objecttag name_de is really new", true, ObjectTag.isEmptyId(nameDe.getId()));

			// enabled the object tag
			nameDe.setEnabled(true);

			Value value = nameDe.getValues().iterator().next();

			value.setValueText("Deutscher Name");
			folder.save();
			trx.success();
		}

		// wait until no more entry in dirtqueue exists (all events have been handled)
		testContext.waitForDirtqueueWorker();
		try (Trx trx = trxWithPermissions()) {
			testContext.checkDirtedPages(dirtedPagesBeforeModification, new int[] { pageDependsNameDe});
			trx.success();
		}
	}

	/**
	 * Test deleting a folder (with all objects included in the folder)
	 * @throws Exception
	 */
	@Test
	public void testDeleteFolder() throws Exception {
		Folder toDelete = null;
		Template delTemplate = null;
		Page delPage = null;
		File delFile = null;
		Folder delFolder = null;
		Page delSubPage = null;
		File delSubFile = null;

		try (Trx trx = new Trx()) {
			Transaction t = trx.getTransaction();

			toDelete = t.createObject(Folder.class);

			toDelete.setMotherId(editedFolder.getId());
			toDelete.setName("Folder to delete");
			toDelete.setPublishDir("/");
			toDelete.save();
			t.commit(false);

			// create a template in the folder
			delTemplate = t.createObject(Template.class);

			delTemplate.setMlId(1);
			delTemplate.setName("Template to delete");
			delTemplate.setSource("This is the template source");
			delTemplate.getFolders().add(toDelete);
			delTemplate.save();
			t.commit(false);

			// create a page in the folder
			delPage = t.createObject(Page.class);

			delPage.setFilename("page.html");
			delPage.setFolderId(toDelete.getId());
			delPage.setName("Page to delete");
			delPage.setTemplateId(delTemplate.getId());
			delPage.save();
			t.commit(false);

			// create a file in the folder
			delFile = t.createObject(File.class);

			delFile.setFileStream(new ByteArrayInputStream(new byte[] { 1, 2, 3}));
			delFile.setFolderId(toDelete.getId());
			delFile.setName("file.bin");
			delFile.save();
			t.commit(false);

			// create a folder in the folder
			delFolder = t.createObject(Folder.class);

			delFolder.setMotherId(toDelete.getId());
			delFolder.setName("Subfolder to delete");
			delFolder.setPublishDir("/");
			delFolder.save();
			t.commit(false);

			// create a page in the subfolder
			delSubPage = t.createObject(Page.class);

			delSubPage.setFilename("subpage.html");
			delSubPage.setFolderId(delFolder.getId());
			delSubPage.setName("Subpage to delete");
			delSubPage.setTemplateId(delTemplate.getId());
			delSubPage.save();
			t.commit(false);

			// create a file in the subfolder
			delSubFile = t.createObject(File.class);

			delSubFile.setFileStream(new ByteArrayInputStream(new byte[] { 1, 2, 3}));
			delSubFile.setFolderId(delFolder.getId());
			delSubFile.setName("subfile.bin");
			delSubFile.save();
			t.commit(false);

			// check that all created objects exist
			assertNotNull("Check that " + toDelete + " exists", t.getObject(Folder.class, toDelete.getId()));
			assertNotNull("Check that " + delTemplate + " exists", t.getObject(Template.class, delTemplate.getId()));
			assertNotNull("Check that " + delPage + " exists", t.getObject(Page.class, delPage.getId()));
			assertNotNull("Check that " + delFile + " exists", t.getObject(File.class, delFile.getId()));
			assertNotNull("Check that " + delFolder + " exists", t.getObject(Folder.class, delFolder.getId()));
			assertNotNull("Check that " + delSubPage + " exists", t.getObject(Page.class, delSubPage.getId()));
			assertNotNull("Check that " + delSubFile + " exists", t.getObject(File.class, delSubFile.getId()));
			trx.success();
		}

		// start a new transaction
		try (Trx trx = new Trx()) {
			// now delete the folder
			FolderResource folderResource = getFolderResource();

			folderResource.delete(ObjectTransformer.getString(toDelete.getId(), null), null, null);
			trx.success();
		}

		// start a new transaction
		try (Trx trx = new Trx()) {
			Transaction t = trx.getTransaction();

			// now check that everything is gone
			assertNull("Check that " + toDelete + " was deleted", t.getObject(Folder.class, toDelete.getId()));
			assertNull("Check that " + delTemplate + " was deleted", t.getObject(Template.class, delTemplate.getId()));
			assertNull("Check that " + delPage + " was deleted", t.getObject(Page.class, delPage.getId()));
			assertNull("Check that " + delFile + " was deleted", t.getObject(File.class, delFile.getId()));
			assertNull("Check that " + delFolder + " was deleted", t.getObject(Folder.class, delFolder.getId()));
			assertNull("Check that " + delSubPage + " was deleted", t.getObject(Page.class, delSubPage.getId()));
			assertNull("Check that " + delSubFile + " was deleted", t.getObject(File.class, delSubFile.getId()));
		}
	}

	/**
	 * Test passing object properties to subfolders recursively
	 * @throws Exception
	 */
	@Test
	public void testSetRecursiveObjectProperties() throws Exception {
		Folder rootFolder = null;
		List<Folder> folders = null;

		try (Trx trx = trxAsSystemUser()) {
			Transaction t = trx.getTransaction();

			Node node = ContentNodeTestDataUtils.createNode("test", "Test", PublishTarget.NONE);
			int constructId = ContentNodeTestDataUtils.createConstruct(node, ShortTextPartType.class, "construct", "part");
			// objects are deleted in reverse order: first the node (with the folders and their object tags), then the object property, then the construct
			created(t.getObject(Construct.class, constructId));
			created(ContentNodeTestDataUtils.createObjectPropertyDefinition(Folder.TYPE_FOLDER, constructId, "Test OE", "test"));
			created(node);
			rootFolder = node.getFolder();
			Folder folder1 = ContentNodeTestDataUtils.createFolder(rootFolder, "Folder 1");
			Folder folder11 = ContentNodeTestDataUtils.createFolder(folder1, "Folder 1.1");
			Folder folder12 = ContentNodeTestDataUtils.createFolder(folder1, "Folder 1.2");
			Folder folder2 = ContentNodeTestDataUtils.createFolder(rootFolder, "Folder 2");
			Folder folder21 = ContentNodeTestDataUtils.createFolder(folder2, "Folder 2.1");
			Folder folder22 = ContentNodeTestDataUtils.createFolder(folder2, "Folder 2.2");
			folders = Arrays.asList(rootFolder, folder1, folder11, folder12, folder2, folder21, folder22);

			// set the OE for the root folder
			rootFolder = t.getObject(Folder.class, rootFolder.getId(), true);
			ObjectTag tag = rootFolder.getObjectTag("test");
			assertNotNull(rootFolder + " must have 'object.test'", tag);
			tag.getValues().getByKeyname("part").setValueText("Content of the OE");
			tag.setEnabled(true);
			rootFolder.save();
			t.commit(false);
			rootFolder = t.getObject(Folder.class, rootFolder.getId());
			trx.success();
		}

		try (Trx trx = trxAsSystemUser()) {
			Transaction t = trx.getTransaction();

			FolderResource folderResource = getFolderResource();
			FolderSaveRequest request = new FolderSaveRequest();
			com.gentics.contentnode.rest.model.Folder f = new com.gentics.contentnode.rest.model.Folder();
			f.setId(ObjectTransformer.getInt(rootFolder.getId(), 0));
			request.setFolder(f);
			request.setTagsToSubfolders(Arrays.asList("test"));
			request.setForegroundTime(60);
			GenericResponse response = folderResource.save(ObjectTransformer.getString(rootFolder.getId(), ""), request);
			t.commit(false);
			ContentNodeRESTUtils.assertResponseOK(response);

			for (Folder folder : folders) {
				folder = t.getObject(Folder.class, folder.getId());
				ObjectTag objectTag = folder.getObjectTag("test");
				assertNotNull(folder + " must have 'object.test'", objectTag);

				assertTrue("'object.test' must be enabled for " + folder, objectTag.isEnabled());
				assertEquals("Check value of 'object.test' for " + folder, "Content of the OE", objectTag.getValues().getByKeyname("part").getValueText());
			}

			// now disable the OE recursively
			rootFolder = t.getObject(Folder.class, rootFolder.getId(), true);
			ObjectTag tag = rootFolder.getObjectTag("test");
			assertNotNull(rootFolder + " must have 'object.test'", tag);
			tag.setEnabled(false);
			rootFolder.save();
			t.commit(false);
			rootFolder = t.getObject(Folder.class, rootFolder.getId());
			trx.success();
		}

		try (Trx trx = trxAsSystemUser()) {
			Transaction t = trx.getTransaction();

			FolderResource folderResource = getFolderResource();
			FolderSaveRequest request = new FolderSaveRequest();
			com.gentics.contentnode.rest.model.Folder f = new com.gentics.contentnode.rest.model.Folder();
			f.setId(ObjectTransformer.getInt(rootFolder.getId(), 0));
			request.setFolder(f);
			request.setTagsToSubfolders(Arrays.asList("test"));
			request.setForegroundTime(60);
			GenericResponse response = folderResource.save(ObjectTransformer.getString(rootFolder.getId(), ""), request);
			t.commit(false);
			ContentNodeRESTUtils.assertResponseOK(response);

			for (Folder folder : folders) {
				folder = t.getObject(Folder.class, folder.getId());
				ObjectTag objectTag = folder.getObjectTag("test");
				assertNotNull(folder + " must have 'object.test'", objectTag);
				assertFalse("'object.test' must be disabled for " + folder, objectTag.isEnabled());
			}
			trx.success();
		}
	}

	/**
	 * Test passing object properties to subfolders recursively
	 * @throws Exception
	 */
	@Test
	public void testSetRecursiveInTagObjectProperties() throws Exception {
		try (Trx trx = trxAsSystemUser()) {
			Transaction t = trx.getTransaction();

			Node node = ContentNodeTestDataUtils.createNode("test", "Test", PublishTarget.NONE);
			int constructId = ContentNodeTestDataUtils.createConstruct(node, ShortTextPartType.class, "construct", "part");
			// objects are deleted in reverse order: first the node (with the folders and their object tags), then the object property, then the construct
			created(t.getObject(Construct.class, constructId));
			created(ContentNodeTestDataUtils.createObjectPropertyDefinition(Folder.TYPE_FOLDER, constructId, "Test OE", "test"));
			created(node);
			Folder rootFolder = node.getFolder();
			Folder folder1 = ContentNodeTestDataUtils.createFolder(rootFolder, "Folder 1");
			Folder folder11 = ContentNodeTestDataUtils.createFolder(folder1, "Folder 1.1");
			Folder folder12 = ContentNodeTestDataUtils.createFolder(folder1, "Folder 1.2");
			List<Folder> folders = Arrays.asList(rootFolder, folder1, folder11, folder12);

			// set the OE for the root folder
			rootFolder = t.getObject(Folder.class, rootFolder.getId(), true);
			ObjectTag tag = rootFolder.getObjectTag("test");
			assertNotNull(rootFolder + " must have 'object.test'", tag);
			tag.getValues().getByKeyname("part").setValueText("Content of the OE");
			tag.setEnabled(true);
			rootFolder.save();
			t.commit(false);

			ContentNodeTestDataUtils.createInTagObjectTag(
					tag, ObjectTransformer.getInt(tag.getConstructId(), 0), "intag", rootFolder);

			rootFolder = t.getObject(Folder.class, rootFolder.getId());

			FolderResource folderResource = getFolderResource();
			FolderSaveRequest request = new FolderSaveRequest();
			com.gentics.contentnode.rest.model.Folder f = new com.gentics.contentnode.rest.model.Folder();
			f.setId(ObjectTransformer.getInt(rootFolder.getId(), 0));
			request.setFolder(f);
			request.setTagsToSubfolders(Arrays.asList("test", "intag"));
			request.setForegroundTime(60);
			GenericResponse response = folderResource.save(ObjectTransformer.getString(rootFolder.getId(), ""), request);
			t.commit(false);
			ContentNodeRESTUtils.assertResponseOK(response);

			for (Folder folder : folders) {
				folder = t.getObject(Folder.class, folder.getId());
				ObjectTag testObjectTag = folder.getObjectTag("test");
				assertNotNull(folder + " must have 'object.test'", testObjectTag);
				assertEquals("Check value of 'object.test' for " + folder, "Content of the OE",
						testObjectTag.getValues().getByKeyname("part").getValueText());

				ObjectTag inTagobjectTag = folder.getObjectTag("intag");
				assertNotNull(folder + " must have 'object.intag'", inTagobjectTag);
				assertTrue("object.intag must be an intag", inTagobjectTag.isIntag());
				assertTrue("The container of object.intag must belong to the same folder",
						testObjectTag.getNodeObject().getId().equals(inTagobjectTag.getNodeObject().getId()));
			}
			trx.success();
		}
	}
}
