package com.gentics.contentnode.tests.publish;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.ClassRule;

import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.TagmapEntry;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.lib.content.GenticsContentAttribute;

/**
 * Sandbox helper to encapsulate setup and teardown of a test sandbox
 */
@GCNFeature(set = { Feature.MULTICHANNELLING })
public class AbstractInstantPublishingTestSandboxTest {

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	/**
	 * Id of the pageurl construct
	 */
	public final static int PAGEURL_CONSTRUCT_ID = 9;

	/**
	 * Map of datasources per node/channel
	 */
	protected static Map<Integer, Datasource> datasources = new HashMap<Integer, Datasource>();

	@BeforeClass
	public static void setupOnce() throws Exception {
		// disable publishing of all other nodes
		testContext.getDBSQLUtils().executeQueryManipulation("UPDATE node SET disable_publish = 1, publish_contentmap = 0, publish_fs = 0");

		NodePreferences prefs = testContext.getContext().getNodeConfig().getDefaultPreferences();
		prefs.setFeature("instant_cr_publishing", true);
		prefs.setFeature("tag_image_resizer", false);
	}

	/**
	 * Restore the snapshot
	 * 
	 * @throws Exception
	 */
	public void restoreSnapshot() throws Exception {
		// disable publishing of all other nodes
		testContext.getDBSQLUtils().executeQueryManipulation("UPDATE node SET disable_publish = 1, publish_contentmap = 0, publish_fs = 0");

		// remove all existing logcmd entries
		testContext.getDBSQLUtils().executeQueryManipulation("DELETE FROM logcmd");

		// remove all registered publish handlers.
		testContext.getDBSQLUtils().executeQueryManipulation("DELETE FROM cr_publish_handler");

		testContext.getContext().clearNodeObjectCache();
	}

	/**
	 * Create a non-multichanneling datasource for the given node
	 * @param node node
	 * @return handle properties
	 * @throws Exception
	 */
	public static Map<String, String> createDatasource(Node node) throws Exception {
		return createDatasource(node, false);
	}

	/**
	 * Create a datasource for the given node
	 * @param node node
	 * @param mccr whether to return a multichannelling datasource
	 * @return handle properties
	 * @throws Exception
	 */
	public static Map<String, String> createDatasource(Node node, boolean mccr) throws Exception {
		Map<String, String> handleProps = new HashMap<String, String>();

		handleProps.put("type", "jdbc");
		handleProps.put("driverClass", "org.hsqldb.jdbcDriver");
		handleProps.put("url", "jdbc:hsqldb:mem:" + node.getHostname()+new Object().hashCode());
		handleProps.put("username", "sa");
		handleProps.put("shutDownCommand", "SHUTDOWN");

		Map<String, String> dsProps = new HashMap<String, String>();

		dsProps.put("sanitycheck2", "true");
		dsProps.put("autorepair2", "true");

		Datasource datasource;
		if (mccr) {
			dsProps.put("multichannelling", "true");
			datasource = PortalConnectorFactory.createMultichannellingDatasource(handleProps, dsProps);
		} else {
			datasource = PortalConnectorFactory.createDatasource(handleProps, dsProps);
		}

		assertNotNull("Check whether datasource was created", datasource);

		datasources.put(ObjectTransformer.getInteger(node.getId(), null), datasource);

		return handleProps;
	}

	/**
	 * Create a new Node
	 * 
	 * @param hostName
	 *            hostname
	 * @param name
	 *            node name
	 * @param instantPublishing true to use instant publishing into the contentrepository
	 * @return node
	 * @throws Exception
	 */
	public static Node createNode(String hostName, String name, boolean instantPublishing, boolean publishStartPage) throws Exception {
		return createNode(hostName, name, instantPublishing, publishStartPage, null);
	}

	/**
	 * Create a new Node, which might be a channel of the given node
	 * 
	 * @param hostName
	 *            hostname
	 * @param name
	 *            node name
	 * @param instantPublishing true to use instant publishing into the contentrepository
	 * @param master master node to create a channel, null to create a master node
	 * @return node
	 * @throws Exception
	 */
	public static Node createNode(String hostName, String name, boolean instantPublishing, boolean publishStartPage, Node master) throws Exception {
		return createNode(hostName, name, instantPublishing, publishStartPage, master, false);
	}

	/**
	 * Create a new Node, which might be a channel of the given node
	 * 
	 * @param hostName
	 *            hostname
	 * @param name
	 *            node name
	 * @param instantPublishing true to use instant publishing into the contentrepository
	 * @param master master node to create a channel, null to create a master node
	 * @return node
	 * @throws Exception
	 */
	public static Node createNode(String hostName, String name, boolean instantPublishing, Node master) throws Exception {
		return createNode(hostName, name, instantPublishing, false, master, false);
	}


	/**
	 * Create a new Node, which might be a channel of the given node
	 *
	 * @param hostName
	 *            hostname
	 * @param name
	 *            node name
	 * @param mccr
	 *            whether a multichannelling contentrepository should be created
	 * @param instantPublishing true to use instant publishing into the contentrepository
	 * @param master master node to create a channel, null to create a master node
	 * @return node
	 * @throws Exception
	 */
	public static Node createNode(String hostName, String name, boolean instantPublishing, boolean publishStartPage, Node master, boolean mccr) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		Node node = t.createObject(Node.class);

		node.setHostname(hostName);
		node.setPublishDir("/");
		node.setPublishFilesystem(true);
		Folder rootFolder = t.createObject(Folder.class);

		if (master != null) {
			rootFolder.setChannelMaster(master.getFolder());
		}
		node.setFolder(rootFolder);
		rootFolder.setName(name);
		rootFolder.setPublishDir("/");

		node.save();
		t.commit(false);
		if (Feature.PUBLISH_FOLDER_STARTPAGE.isActivated()) {
			if (publishStartPage) {
				node.activateFeature(Feature.PUBLISH_FOLDER_STARTPAGE);
			} else {
				node.deactivateFeature(Feature.PUBLISH_FOLDER_STARTPAGE);
			}
		}

		// create a datasource
		Map<String, String> handleProperties = createDatasource(node, mccr);
		ContentRepository cr = t.createObject(ContentRepository.class);
		cr.setMultichannelling(mccr);

		cr.setName(name);
		cr.setInstantPublishing(instantPublishing);
		
		for (Integer i : new Integer[]{10002, 10007, 10008}) {
			TagmapEntry tme = t.createObject(TagmapEntry.class);
			tme.setMapname("node_id");
			tme.setTagname("node.id");
			tme.setAttributeTypeId(1);
			tme.setStatic(true);
			tme.setOptimized(true);
			tme.setObject(i);
			cr.getEntries().add(tme);
		}
		cr.save();

		// activate publishing into the contentrepository
		node.setPublishContentmap(true);
		node.setContentrepositoryId(cr.getId());
		node.save();
		t.commit(false);

		// update the cr to point to the hsql database
		DBUtils.executeUpdate("UPDATE contentrepository SET dbtype = ?, url = ?, username = ?, password = ? WHERE id = ?",
				new Object[] { "hsql", handleProperties.get("url"), "sa", "", cr.getId()});

		return t.getObject(Node.class, node.getId());
	}

	/**
	 * Create a folder
	 * @param mother mother folder
	 * @param name name of the folder
	 * @return folder
	 * @throws Exception
	 */
	public Folder createFolder(Folder mother, String name) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		Folder folder = t.createObject(Folder.class);

		folder.setMotherId(mother.getId());
		folder.setName(name);
		folder.setPublishDir("/");
		folder.save();
		t.commit(false);

		return folder;
	}

	/**
	 * Create the template
	 * 
	 * @param folder
	 *            folder where to create the template
	 * @return template
	 * @throws Exception
	 */
	public Template createTemplate(Folder folder) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// create a template
		Template template = t.createObject(Template.class);

		template.setName("Template");
		template.setMlId(1);
		template.setSource("Page [<node page.name>]");

		template.setFolderId(folder.getId());
		template.save();
		t.commit(false);

		return template;
	}

	/**
	 * Create a page
	 * @param folder folder
	 * @param template template
	 * @param name name
	 * @return page
	 * @throws Exception
	 */
	public Page createPage(Folder folder, Template template, String name) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		Page page = t.createObject(Page.class);

		page.setName(name);
		page.setTemplateId(template.getId());
		page.setFolderId(folder.getId());
		page.save();
		t.commit(false);

		return page;
	}

	/**
	 * Create a file
	 * @param folder folder
	 * @param name file name
	 * @param contents file contents
	 * @return created file
	 * @throws Exception
	 */
	public File createFile(Folder folder, String name, byte[] contents) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		File file = t.createObject(File.class);

		file.setFolderId(folder.getId());
		file.setName(name);
		file.setFileStream(new ByteArrayInputStream(contents));
		file.save();
		t.commit(false);

		return file;
	}

	/**
	 * Create the object property definition for "startpage"
	 * @throws Exception
	 */
	public void createStartpageObjectProperty() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		ObjectTagDefinition objDef = t.createObject(ObjectTagDefinition.class);

		objDef.setName("Startpage", 1);
		objDef.setTargetType(Folder.TYPE_FOLDER);
		ObjectTag tag = objDef.getObjectTag();

		tag.setName("object.startpage");
		tag.setConstructId(PAGEURL_CONSTRUCT_ID);
		tag.setObjType(Folder.TYPE_FOLDER);
		objDef.save();
		t.commit(false);
	}

	/**
	 * Create the startpage tagmap entry for the content repository of the given node
	 * @param node node
	 * @throws Exception
	 */
	public void createStartpageTagmapEntry(Node node) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		ContentRepository cr = node.getContentRepository();

		if (cr == null) {
			throw new NodeException("Cannot create tagmap entry for startpage: " + node + " has no contentrepository assigned");
		}

		// get an editable instance
		cr = t.getObject(ContentRepository.class, cr.getId(), true);
		List<TagmapEntry> entries = cr.getEntries();
		TagmapEntry startpageEntry = t.createObject(TagmapEntry.class);

		startpageEntry.setObject(Folder.TYPE_FOLDER);
		startpageEntry.setAttributeTypeId(GenticsContentAttribute.ATTR_TYPE_OBJ);
		startpageEntry.setTargetType(Page.TYPE_PAGE);
		startpageEntry.setTagname("object.startpage.url.target.id");
		startpageEntry.setMapname("startpage");
		entries.add(startpageEntry);
		cr.save();
		t.commit(false);
	}

	/**
	 * Create the <code>name</code> tagmap entry for the content repository
	 * of the given node.
	 *
	 * @param node The contentrespository's node
	 * @param objType The object type for the entry
	 * @param objName The object name for the entry
	 */
	public void createNameTagmapEntry(Node node, int objType, String objName) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		ContentRepository cr = t.getObject(node.getContentRepository(), true);
		List<TagmapEntry> entries = cr.getEntries();
		TagmapEntry nameEntry = t.createObject(TagmapEntry.class);

		nameEntry.setObject(objType);
		nameEntry.setAttributeTypeId(GenticsContentAttribute.ATTR_TYPE_TEXT);
		nameEntry.setTagname(objName + ".name");
		nameEntry.setMapname("name");
		nameEntry.setOptimized(true);

		entries.add(nameEntry);
		cr.save();
		t.commit(false);
	}

	/**
	 * Assert existence/nonexistence of an object in the contentrepository of the given node
	 * @param node node
	 * @param object object
	 * @param expected true if the object is expected to exist
	 * @throws Exception
	 */
	public void assertInContentrepository(Node node, NodeObject object, boolean expected) throws Exception {
		Datasource datasource = datasources.get(node.getId());

		assertNotNull(node + " has no datasource", datasource);
		int ttype = ObjectTransformer.getInt(object.getTType(), 0);

		if (ttype == ImageFile.TYPE_IMAGE) {
			ttype = File.TYPE_FILE;
		}
		Resolvable crObject = PortalConnectorFactory.getContentObject(ttype + "." + object.getId(), datasource);

		if (expected) {
			assertNotNull(object + " must exist in cr of " + node, crObject);
		} else {
			assertNull(object + " must not exist in cr of " + node, crObject);
		}
	}

	/**
	 * Assert that the object exists in the contentrepository of the given node and has the given attribute set like expected
	 * @param node node
	 * @param object object
	 * @param attribute attribute name
	 * @param expectedValue expected attribute value
	 * @throws Exception
	 */
	public void assertInContentrepository(Node node, NodeObject object, String attribute, String expectedValue) throws Exception {
		Datasource datasource = datasources.get(node.getId());

		assertNotNull(node + " has no datasource", datasource);
		int ttype = ObjectTransformer.getInt(object.getTType(), 0);

		if (ttype == ImageFile.TYPE_IMAGE) {
			ttype = File.TYPE_FILE;
		}
		Resolvable crObject = PortalConnectorFactory.getContentObject(ttype + "." + object.getId(), datasource);

		assertNotNull(object + " must exist in cr of " + node, crObject);
		assertEquals("Check attribute value", expectedValue, ObjectTransformer.getString(crObject.get(attribute), null));
	}

}
