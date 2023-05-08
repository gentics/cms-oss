/*
 * @author johannes2
 * @date 15.04.2008
 * @version $Id: CRSyncObjectManagementManagerTest.java,v 1.3 2010-09-28 17:08:09 norbert Exp $
 */
package com.gentics.node.tests.crsync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import com.gentics.lib.content.DatatypeHelper;
import com.gentics.lib.content.GenticsContentAttribute;
import com.gentics.lib.content.GenticsContentFactory;
import com.gentics.lib.content.GenticsContentObject;
import com.gentics.lib.datasource.CNWriteableDatasource;
import com.gentics.lib.datasource.SQLHandle;
import com.gentics.lib.datasource.object.ObjectAttributeBean;
import com.gentics.lib.datasource.object.ObjectManagementException;
import com.gentics.lib.datasource.object.ObjectManagementManager;
import com.gentics.lib.datasource.object.ObjectTypeBean;
import com.gentics.lib.datasource.object.ObjectManagementManager.ObjectTypeDiff;
import com.gentics.lib.datasource.object.ObjectManagementManager.TypeDiff;
import com.gentics.lib.db.DB;
import com.gentics.lib.db.DB.ColumnDefinition;
import com.gentics.lib.db.DBHandle;
import com.gentics.lib.db.ResultProcessor;
import com.gentics.lib.db.SimpleResultProcessor;
import com.gentics.lib.log.NodeLogger;
import com.gentics.testutils.GenericTestUtils;
import com.gentics.testutils.database.JDBCMalformedURLException;
import com.gentics.testutils.database.SQLUtilException;
import com.gentics.testutils.database.SQLUtils;
import com.gentics.testutils.database.SQLUtilsFactory;
import com.gentics.testutils.database.TestDatabase;
import com.gentics.testutils.database.variations.AbstractSingleVariationDatabaseTest;
import com.gentics.testutils.database.variations.TestDatabaseVariationConfigurations;

/**
 * This test tests ObjectManagementManager class. Coverage around 67,1%.
 *
 * @author johannes2
 */
@Category(BaseLibTest.class)
public class CRSyncObjectManagementManagerTest extends AbstractSingleVariationDatabaseTest {

	static DBCon dbcon = null;

	static CNWriteableDatasource wds = null;

	static NodeLogger logger;

	static DBHandle dbh = null;

	static Properties databaseSettings;

	private static SQLUtils sqlUtils;

	public CRDatabaseUtils crdbutils;

	@BeforeClass
	public static void setupOnce() throws IOException, ClassNotFoundException, SQLException, JDBCMalformedURLException, SQLUtilException {
		logger = NodeLogger.getNodeLogger(CRSyncObjectManagementManagerTest.class);
	}

	/**
	 * Get the test parameters
	 * @return test parameters
	 * @throws JDBCMalformedURLException
	 */
	@Parameters(name = "{index}: singleDBTest: {0}")
	public static Collection<Object[]> data() throws JDBCMalformedURLException {
		return Arrays.asList(getData(TestDatabaseVariationConfigurations.BASIC));
	}

	/**
	 * Create an instance with the given test parameters
	 * @param testDatabase test database
	 */
	public CRSyncObjectManagementManagerTest(TestDatabase testDatabase) {
		super(testDatabase);
	}

	@Before
	public void setUp() throws Exception {
		logger.info("Setup");
		TestDatabase testDatabase = getTestDatabase();

		sqlUtils = SQLUtilsFactory.getSQLUtils(testDatabase);

		// clean DB

		// CRDatabaseUtils will be used to verify the changed data
		crdbutils = new CRDatabaseUtils(sqlUtils);
		// connect to database
		sqlUtils.connectDatabase();
		testDatabase.setRandomDatabasename(CRSyncObjectManagementManagerTest.class.getSimpleName());
		databaseSettings = testDatabase.getSettings();
		sqlUtils.createCRDatabase(getClass());

		Map<String, String> dsProperties = new HashMap<String, String>();

		dsProperties.put("attribute.path", "/tmp");

		wds = (CNWriteableDatasource) PortalConnectorFactory.createWriteableDatasource(databaseSettings, dsProperties);

		assertNotNull("Datasource must exist", wds);

		dbcon = new DBCon(databaseSettings.getProperty("driverClass"), databaseSettings.getProperty("url"), databaseSettings.getProperty("username"),
				databaseSettings.getProperty("passwd"));

		prepareContentRepository();
	}

	@After
	public void tearDown() throws Exception {
		PortalConnectorFactory.destroy();
		if (dbcon != null) {
			dbcon.closeCON();
		}

		if (sqlUtils != null) {
			sqlUtils.removeDatabase();
			sqlUtils.disconnectDatabase();
		}
	}

	/**
	 * Clean the CNRepository and get the DBHandle from the datasource.
	 *
	 * @throws Exception
	 */
	private void prepareContentRepository() throws Exception {
		emtpyTables(dbcon);
		dbh = GenticsContentFactory.getHandle(wds);
	}

	// Test api begin

	/**
	 * This method compares record from database with reference object type a
	 *
	 * @param row
	 *            ArrayList that contains the columns of that row
	 * @param compareObjectType
	 *            ObjectTypeBean that should be compared with database content
	 */
	public void checkObjectTypeBean(Map<String, Object> row, ObjectTypeBean compareObjectType) {
		assertEquals("Chekc name of object type", compareObjectType.getName(), row.get("name"));
		assertEquals("Check type of object type", compareObjectType.getType(), ObjectTransformer.getInteger(row.get("type"), null));
		assertEquals("Check exclude versioning of object type", compareObjectType.getExcludeVersioning(),
				ObjectTransformer.getBoolean(row.get("exclude_versioning"), null));
	}

	/**
	 * This method compare two object type beans.
	 *
	 * @param referenceObjectType
	 * @param compareObjectType
	 */
	public void checkObjectTypeBean(ObjectTypeBean referenceObjectType, ObjectTypeBean compareObjectType) {

		// name
		assertTrue("expected name of object type: '" + referenceObjectType.getName() + "' but was '" + compareObjectType.getName() + "'",
				referenceObjectType.getName().equalsIgnoreCase(compareObjectType.getName()));

		// type
		assertTrue("expected type of object type: '" + referenceObjectType.getType() + "' but was '" + compareObjectType.getType() + "'",
				referenceObjectType.getType().intValue() == compareObjectType.getType().intValue());

		// id
		assertTrue("expected id of object type: '" + referenceObjectType.getId() + "' but was '" + compareObjectType.getId() + "'",
				referenceObjectType.getId().equalsIgnoreCase(compareObjectType.getId()));

		// exclude versioning
		assertTrue(
				"expected exclude versioning of object type: '" + referenceObjectType.getExcludeVersioning() + "' but was '"
				+ compareObjectType.getExcludeVersioning() + "'",
				referenceObjectType.getExcludeVersioning() == compareObjectType.getExcludeVersioning());

	}

	/**
	 * This method is used to compare an ArrayList that contains each column as elements of a database record with an object attribute bean.
	 *
	 * @param row
	 *            ArrayList with column elements
	 * @param compareObjectAttribute
	 */
	public void checkAttribute(Map<String, Object> row, ObjectAttributeBean compareObjectAttribute) {

		// attribute name
		assertEquals("Check name of attribute " + compareObjectAttribute.getName(), compareObjectAttribute.getName(),
				ObjectTransformer.getString(row.get("name"), null));

		// attribute type
		assertEquals("Check type of attribute " + compareObjectAttribute.getName(), compareObjectAttribute.getAttributetype(),
				ObjectTransformer.getInt(row.get("attributetype"), -1));

		// optimized
		assertEquals("Check optimized of attribute " + compareObjectAttribute.getName(), compareObjectAttribute.getOptimized(),
				ObjectTransformer.getBoolean(row.get("optimized"), null));

		// quick name
		assertEquals("Check quickname of attribute " + compareObjectAttribute.getName(), compareObjectAttribute.getQuickname(),
				ObjectTransformer.getString(row.get("quickname"), ""));

		// multi value
		assertEquals("Check multivalue of attribute " + compareObjectAttribute.getName(), compareObjectAttribute.getMultivalue(),
				ObjectTransformer.getBoolean(row.get("multivalue"), null));

		// linked object type
		assertEquals("Check linked object type of attribute " + compareObjectAttribute.getName(), compareObjectAttribute.getLinkedobjecttype(),
				ObjectTransformer.getInt(row.get("linkedobjecttype"), -1));

		// foreign link attribute
		assertEquals("Check foreign link attribute of attribute " + compareObjectAttribute.getName(), compareObjectAttribute.getForeignlinkattribute(),
				ObjectTransformer.getString(row.get("foreignlinkattribute"), ""));

		// foreign link attribute rule
		assertEquals("Check foreign link attribute rule of attribute " + compareObjectAttribute.getName(), compareObjectAttribute.getForeignlinkattributerule(),
				ObjectTransformer.getString(row.get("foreignlinkattributerule"), ""));
	}

	/**
	 * This method compares the referenceObjectAttribute with the record which was fetched from database. Furthermore it compares the
	 * sourceObjectAttribute (an Attribute which should be gathered by using loadAttribtes method from ObjectManagementManager class) with
	 * the reference.
	 *
	 * @param referenceObjectAttribute
	 * @param compareObjectAttribute
	 */
	public void checkAttribute(ObjectAttributeBean referenceObjectAttribute, ObjectAttributeBean compareObjectAttribute) {

		// attribute name
		assertTrue("expected name of attribute '" + referenceObjectAttribute.getName() + "' but was '" + compareObjectAttribute.getName() + "'",
				referenceObjectAttribute.getName().equalsIgnoreCase(compareObjectAttribute.getName()));

		// attribute type
		assertTrue("expected name of type " + referenceObjectAttribute.getAttributetype() + "' but was '" + compareObjectAttribute.getAttributetype() + "'",
				referenceObjectAttribute.getAttributetype() == compareObjectAttribute.getAttributetype());

		// optimized
		assertTrue("expected name of optimized " + referenceObjectAttribute.getOptimized() + "' but was '" + compareObjectAttribute.getOptimized() + "'",
				referenceObjectAttribute.getOptimized() == compareObjectAttribute.getOptimized());

		// quick name
		assertTrue("expected name of quickname " + referenceObjectAttribute.getQuickname() + "' but was '" + compareObjectAttribute.getQuickname() + "'",
				referenceObjectAttribute.getQuickname().equalsIgnoreCase(compareObjectAttribute.getQuickname()));

		// multi value
		assertTrue("expected name of multivalue " + referenceObjectAttribute.getMultivalue() + "' but was '" + compareObjectAttribute.getMultivalue() + "'",
				referenceObjectAttribute.getMultivalue() == compareObjectAttribute.getMultivalue());

		// linked object type
		assertTrue(
				"expected name of linked object type " + referenceObjectAttribute.getLinkedobjecttype() + "' but was '" + compareObjectAttribute.getLinkedobjecttype()
				+ "'",
				referenceObjectAttribute.getLinkedobjecttype() == compareObjectAttribute.getLinkedobjecttype());

		// foreign link attribute
		assertTrue(
				"expected name of foreign link attribute " + referenceObjectAttribute.getForeignlinkattribute() + "' but was '"
				+ compareObjectAttribute.getForeignlinkattribute() + "'",
				referenceObjectAttribute.getForeignlinkattribute().equalsIgnoreCase(compareObjectAttribute.getForeignlinkattribute()));

		// foreign link attribute rule
		assertTrue(
				"expected name of foreign link attribute rule " + referenceObjectAttribute.getForeignlinkattributerule() + "' but was '"
				+ compareObjectAttribute.getForeignlinkattributerule() + "'",
				referenceObjectAttribute.getForeignlinkattributerule().equalsIgnoreCase(compareObjectAttribute.getForeignlinkattributerule()));

	}

	/**
	 * This method validates the number of records in all tables of the CR database.
	 *
	 * @param nContentAttribute
	 * @param nContentAttributeNodeVersion
	 * @param nContentAttributeType
	 * @param nContentMap
	 * @param nContentMapNodeversion
	 * @param nContentObject
	 * @param nContentStatus
	 */
	public void checkDbRowCount(int nContentAttribute, int nContentAttributeNodeVersion, int nContentAttributeType, int nContentMap,
			int nContentMapNodeversion, int nContentObject, int nContentStatus) {
		assertTrue("Number of rows should be '" + nContentAttribute + "' but was: " + crdbutils.getCountTable(CRDatabaseUtils.CONTENTATTRIBUTE),
				crdbutils.getCountTable(CRDatabaseUtils.CONTENTATTRIBUTE) == nContentAttribute);

		assertTrue(
				"Number of rows should be '" + nContentAttributeNodeVersion + "' but was: " + crdbutils.getCountTable(CRDatabaseUtils.CONTENTATTRIBUTE_NODEVERSION),
				crdbutils.getCountTable(CRDatabaseUtils.CONTENTATTRIBUTE_NODEVERSION) == nContentAttributeNodeVersion);

		assertTrue("Number of rows should be '" + nContentAttributeType + "' but was: " + crdbutils.getCountTable(CRDatabaseUtils.CONTENTATTRIBUTETYPE),
				crdbutils.getCountTable(CRDatabaseUtils.CONTENTATTRIBUTETYPE) == nContentAttributeType);

		assertTrue("Number of rows should be '" + nContentMap + "' but was: " + crdbutils.getCountTable(CRDatabaseUtils.CONTENTMAP),
				crdbutils.getCountTable(CRDatabaseUtils.CONTENTMAP) == nContentMap);

		assertTrue("Number of rows should be '" + nContentMapNodeversion + "' but was: " + crdbutils.getCountTable(CRDatabaseUtils.CONTENTMAP_NODEVERSION),
				crdbutils.getCountTable(CRDatabaseUtils.CONTENTMAP_NODEVERSION) == nContentMapNodeversion);

		assertTrue("Number of rows should be '" + nContentObject + "' but was: " + crdbutils.getCountTable(CRDatabaseUtils.CONTENTOBJECT),
				crdbutils.getCountTable(CRDatabaseUtils.CONTENTOBJECT) == nContentObject);

		assertTrue("Number of rows should be '" + nContentStatus + "' but was: " + crdbutils.getCountTable(CRDatabaseUtils.CONTENTSTATUS),
				crdbutils.getCountTable(CRDatabaseUtils.CONTENTSTATUS) == nContentStatus);
	}

	// Test api end

	// Tests begin

	/**
	 * Test of 'ObjectManagementManager.createNewObject()'. Execute 'createNewObject()' and verify if the object was created.
	 */
	@Test
	public void testCreateNewObject() throws Exception {

		// CreateNewObject()
		assertTrue(ObjectManagementManager.createNewObject(dbh, "1000" + GenticsContentObject.OBJ_TYPE_FOLDER, "folder1"));
		assertTrue(ObjectManagementManager.createNewObject(dbh, "1001" + GenticsContentObject.OBJ_TYPE_FOLDER, "folder2"));
		assertTrue(ObjectManagementManager.createNewObject(dbh, "1003" + GenticsContentObject.OBJ_TYPE_FOLDER, "folder3"));

		// Verfiy that the object was created
		Map<String, Object> values = crdbutils.getRecordContentobject("type", "100010002");

		assertTrue(values != null);
		assertTrue("folder1".equalsIgnoreCase(ObjectTransformer.getString(values.get("name"), null)));

		values = crdbutils.getRecordContentobject("type", "100110002");
		assertTrue(values != null);
		assertTrue("folder2".equalsIgnoreCase(ObjectTransformer.getString(values.get("name"), null)));

		values = crdbutils.getRecordContentobject("type", "100310002");
		assertTrue(values != null);
		assertTrue("folder3".equalsIgnoreCase(ObjectTransformer.getString(values.get("name"), null)));

		checkDbRowCount(0, 0, 0, 0, 0, 3, 0);
	}

	/**
	 * DONE Test of 'ObjectManagementManager.loadObjectTypes()'. Execute multiple times 'createNewObject()' and run though the returned
	 * collection.
	 */
	@Test
	public void testLoadObjectTypes() throws Exception {
		// create expected object types
		Map<String, String> expected = new HashMap<String, String>();

		expected.put("10001002", "folder1");
		expected.put("10011002", "folder2");

		for (Map.Entry<String, String> entry : expected.entrySet()) {
			// createNewObject()
			assertTrue("Object type " + entry.getKey() + " could not be created", ObjectManagementManager.createNewObject(dbh, entry.getKey(), entry.getValue()));
		}

		// loadObjectTypes()
		Collection<ObjectTypeBean> objtypes = ObjectManagementManager.loadObjectTypes(wds, false);

		assertEquals("loadObjectTypes() returned collection that didn't contain number of objects as expected.", expected.size(), objtypes.size());

		// make a map of loaded object types
		Map<String, ObjectTypeBean> objTypeMap = new HashMap<String, ObjectTypeBean>();

		for (ObjectTypeBean objType : objtypes) {
			objTypeMap.put(objType.toString(), objType);
		}

		// check that the expected types were found
		for (Map.Entry<String, String> entry : expected.entrySet()) {
			assertTrue("Obj Type " + entry.getKey() + " not found", objTypeMap.containsKey(entry.getKey()));
			assertEquals("Check name of obj type " + entry.getKey(), entry.getValue(), objTypeMap.get(entry.getKey()).getName());
		}

		checkDbRowCount(0, 0, 0, 0, 0, 2, 0);
	}

	/**
	 * Done SQL Test of 'ObjectManagementManager.saveAttributeType()'. Execute 'saveAttributeType()' and verify that the attribute was
	 * created. Case 1 - Execute 'saveAttributeType()' verify that the attribute was saved.
	 */
	@Test
	public void testSaveAttributeTypeCase1() throws Exception {

		// saveAttributeType()
		ObjectAttributeBean referenceObjectAttribute = new ObjectAttributeBean("name2", GenticsContentAttribute.ATTR_TYPE_TEXT, true, "quick_name", false,
				GenticsContentObject.OBJ_TYPE_FOLDER, 0, null, null, null, false, false);

		try {
			// saveAttributeType()
			ObjectManagementManager.saveAttributeType(wds, referenceObjectAttribute, true);
		} catch (ObjectManagementException e) {
			logger.info("Exception:", e);
			assertTrue(e.toString(), false);
		}

		// check attribute
		Collection<ObjectAttributeBean> attributeTypes = ObjectManagementManager.loadAttributeTypes(dbh);

		assertTrue("expected number of types is 1 but was " + attributeTypes.size(), attributeTypes.size() == 1);

		// get record from database
		Map<String, Object> row = crdbutils.getRecordContentattributetype("name", referenceObjectAttribute.getName());

		assertTrue(row != null);
		assertTrue("expected number of rows is 1 but was " + crdbutils.getCountTable(CRDatabaseUtils.CONTENTATTRIBUTETYPE),
				crdbutils.getCountTable(CRDatabaseUtils.CONTENTATTRIBUTETYPE) == 1);
		assertFalse("getRecordContentattribute is null - no record was found", row == null);
		assertTrue("expected number of colums is 11 but was " + row.size(), row.size() == 11);

		// Check attribute with object fetched via api and with values fetched
		// via stand alone db utils
		ObjectAttributeBean databaseObjectAttribute = (((ObjectAttributeBean) attributeTypes.toArray()[0]));

		checkAttribute(row, referenceObjectAttribute);
		checkAttribute(referenceObjectAttribute, databaseObjectAttribute);

		checkDbRowCount(0, 0, 1, 0, 0, 0, 0);

	}

	/**
	 * Save an attribute and verify that a subsequent update is successful.
	 */
	private void internalUpdateOptimizedAttributeType(boolean excludeVersioningFrom, boolean excludeVersioningTo, boolean forceStructureChangeFrom,
			boolean forceStructureChangeTo, int attributeTypeFrom, int attributeTypeTo) throws Exception {
		String quickName = "quick_name";

		ObjectAttributeBean referenceObjectAttribute = new ObjectAttributeBean("name2", attributeTypeFrom, true, quickName, false,
				GenticsContentObject.OBJ_TYPE_FOLDER, 0, null, null, null, excludeVersioningFrom, false);

		try {
			ObjectManagementManager.saveAttributeType(wds, referenceObjectAttribute, forceStructureChangeFrom);
		} catch (ObjectManagementException e) {
			logger.info("Exception:", e);
			assertTrue(e.toString(), false);
		}

		referenceObjectAttribute = new ObjectAttributeBean("name2", attributeTypeTo, true, quickName, false, GenticsContentObject.OBJ_TYPE_FOLDER, 6, null, null,
				null, excludeVersioningTo, false);

		try {
			ObjectManagementManager.saveAttributeType(wds, referenceObjectAttribute, forceStructureChangeTo);
		} catch (ObjectManagementException e) {
			logger.info("Exception:", e);
			assertTrue(e.toString(), false);
		}

		// check attribute
		Collection<ObjectAttributeBean> attributeTypes = ObjectManagementManager.loadAttributeTypes(dbh);

		assertTrue("expected number of types is 1 but was " + attributeTypes.size(), attributeTypes.size() == 1);

		// get record from database
		Map<String, Object> row = crdbutils.getRecordContentattributetype("name", referenceObjectAttribute.getName());

		assertTrue(row != null);
		assertTrue("expected number of rows is 1 but was " + crdbutils.getCountTable(CRDatabaseUtils.CONTENTATTRIBUTETYPE),
				crdbutils.getCountTable(CRDatabaseUtils.CONTENTATTRIBUTETYPE) == 1);
		assertFalse("getRecordContentattribute is null - no record was found", row == null);
		assertTrue("expected number of colums is 11 but was " + row.size(), row.size() == 11);

		// Check attribute with object fetched via api and with values fetched
		// via stand alone db utils
		ObjectAttributeBean databaseObjectAttribute = (((ObjectAttributeBean) attributeTypes.toArray()[0]));

		checkAttribute(row, referenceObjectAttribute);
		checkAttribute(referenceObjectAttribute, databaseObjectAttribute);

		checkDbRowCount(0, 0, 1, 0, 0, 0, 0);

		// check whether quick column datatype is correct
		ColumnDefinition colDef = DatatypeHelper.getQuickColumnDefinition(dbh, DB.getDatabaseProductName(dbh), attributeTypeTo, quickName);
		boolean columnCheck = DB.checkColumn(dbh, colDef);

		assertTrue("Quick column definition is incorrect after updating the attribute", columnCheck);
	}

	/**
	 * Done SQL Test of 'ObjectManagementManager.saveAttributeType()'. Execute 'saveAttributeType()' and verify that the attribute was
	 * created. Case 2 - Execute 'saveAttributeType()' again and verify that the attribute was updated.
	 */
	@Test
	public void testSaveAttributeTypeCase2() throws Exception {
		internalUpdateOptimizedAttributeType(false, false, // excludeVersioning
				true, true, // foceStructureChange
				GenticsContentAttribute.ATTR_TYPE_TEXT, GenticsContentAttribute.ATTR_TYPE_TEXT);
	}

	/**
	 * Done SQL Test of 'ObjectManagementManager.saveAttributeType()'. Execute 'saveAttributeType()' and verify that the attribute was
	 * created. Case 3 - Execute 'saveAttributeType()' again and verify that the attribute was updated. Exclude versioning true
	 */
	@Test
	public void testSaveAttributeTypeCase3() throws Exception {
		internalUpdateOptimizedAttributeType(false, true, // excludeVersioning
				true, false, // forceStructureChange
				GenticsContentAttribute.ATTR_TYPE_TEXT, GenticsContentAttribute.ATTR_TYPE_TEXT);
	}

	/**
	 * Test changing the type of an optimized attribute.
	 */
	@Test
	public void testChangeAttributeTypeCase1() throws Exception {
		internalUpdateOptimizedAttributeType(false, false, // excludeVersioning
				true, true, // forceStrctureChange
				GenticsContentAttribute.ATTR_TYPE_TEXT, GenticsContentAttribute.ATTR_TYPE_TEXT_LONG);
	}

	/**
	 * Test changing the type of an optimized attribute - reversed from case1.
	 */
	@Test
	public void testChangeAttributeTypeCase2() throws Exception {
		internalUpdateOptimizedAttributeType(false, false, // excludeVersioning
				true, true, // forceStrctureChange
				GenticsContentAttribute.ATTR_TYPE_TEXT_LONG, GenticsContentAttribute.ATTR_TYPE_TEXT);
	}

	/**
	 * Test of 'ObjectManagementManager.saveObjectType()'. Execute 'saveObjectType()' and verify that the objecttype was created. Execute
	 * 'saveObjectType()' again and verify that the objecttype was updated. Case 1 - saving of one object in empty CR
	 */
	@Test
	public void testSaveObjectTypeCase1() throws Exception {

		// define object type
		ObjectTypeBean referenceObjectType = new ObjectTypeBean(null, "newObjectPageType", false);

		// saveObjectType()
		try {
			assertTrue("method returned false but should return true", ObjectManagementManager.saveObjectType(wds, referenceObjectType));
		} catch (Exception e) {
			logger.info("Exception:", e);
			logger.info("", e);
		}

		// fetch data for verification
		Map<String, Object> row = crdbutils.getRecordContentobject("name", referenceObjectType.getName());
		Collection<ObjectTypeBean> col = ObjectManagementManager.loadObjectTypes(dbh);
		ObjectTypeBean databaseObjectType = col.iterator().next();

		// verify data
		checkObjectTypeBean(row, referenceObjectType);
		checkObjectTypeBean(referenceObjectType, databaseObjectType);

		checkDbRowCount(0, 0, 0, 0, 0, 1, 0);
	}

	/**
	 * Test of 'ObjectManagementManager.saveObjectType()'. Execute 'saveObjectType()' and verify that the object type was created. Execute
	 * 'saveObjectType()' again and verify that the object type was updated. Case 2 - saving of two object in empty CR
	 */
	@Test
	public void testSaveObjectTypeCase2() throws Exception {

		// define object type
		ObjectTypeBean referenceObjectType = new ObjectTypeBean(null, "newObjectPageType", false);

		// saveObjectType()
		try {
			assertTrue("method returned false but should return true", ObjectManagementManager.saveObjectType(wds, referenceObjectType));
		} catch (Exception e) {
			logger.info("Exception:", e);
			logger.info("", e);
		}

		// fetch data for verification
		Map<String, Object> row = crdbutils.getRecordContentobject("name", referenceObjectType.getName());
		Collection<ObjectTypeBean> col = ObjectManagementManager.loadObjectTypes(wds, false);
		ObjectTypeBean databaseObjectType = col.iterator().next();

		// verify data
		checkObjectTypeBean(row, referenceObjectType);
		checkObjectTypeBean(referenceObjectType, databaseObjectType);

		// second object
		referenceObjectType = new ObjectTypeBean(null, "newObjectPageType2", false);

		// saveObjectType()
		try {
			assertTrue("method returned false but should return true", ObjectManagementManager.saveObjectType(wds, referenceObjectType));
		} catch (Exception e) {
			logger.info("Exception:", e);
			logger.info("", e);
		}

		// verify data
		row = crdbutils.getRecordContentobject("name", referenceObjectType.getName());
		checkObjectTypeBean(row, referenceObjectType);

		col = ObjectManagementManager.loadObjectTypes(wds, false);
		databaseObjectType = null;
		for (ObjectTypeBean type : col) {
			if (referenceObjectType.getName().equals(type.getName())) {
				databaseObjectType = type;
				break;
			}
		}
		assertNotNull("Saved object type not found", databaseObjectType);
		checkObjectTypeBean(referenceObjectType, databaseObjectType);

		checkDbRowCount(0, 0, 0, 0, 0, 2, 0);
	}

	/**
	 * Test of 'ObjectManagementManager.saveObjectType()'. Execute 'saveObjectType()' and verify that the object type was created. Execute
	 * 'saveObjectType()' again and verify that the object type was updated. Case 3 - saving one object in empty CR - saving the same object
	 * in CR. Update should occur.
	 */
	@Test
	public void testSaveObjectTypeCase3() throws Exception {

		// define object type
		// Integer type = new Integer(GenticsContentObject.OBJ_TYPE_PAGE);
		ObjectTypeBean referenceObjectType = new ObjectTypeBean(null, "newObjectPageType", false);

		// saveObjectType()
		try {
			assertTrue("method returned false but should return true", ObjectManagementManager.saveObjectType(wds, referenceObjectType));
		} catch (Exception e) {
			logger.info("Exception:", e);
			logger.info("", e);
		}

		// fetch data for verification
		Map<String, Object> row = crdbutils.getRecordContentobject("name", referenceObjectType.getName());
		Collection<ObjectTypeBean> col = ObjectManagementManager.loadObjectTypes(dbh);
		ObjectTypeBean databaseObjectType = col.iterator().next();

		// verify data
		checkObjectTypeBean(row, referenceObjectType);
		checkObjectTypeBean(referenceObjectType, databaseObjectType);

		// second object
		referenceObjectType = new ObjectTypeBean(new Integer(1), "newObjectPageType2", true);

		// saveObjectType()
		try {
			assertTrue("method returned false but should return true", ObjectManagementManager.saveObjectType(wds, referenceObjectType));
		} catch (Exception e) {
			logger.info("Exception:", e);
			logger.info("", e);
		}

		// fetch data for verification
		row = crdbutils.getRecordContentobject("name", referenceObjectType.getName());
		col = ObjectManagementManager.loadObjectTypes(dbh);
		databaseObjectType = ((ObjectTypeBean) col.toArray()[0]);

		// verify data
		checkObjectTypeBean(row, referenceObjectType);
		checkObjectTypeBean(referenceObjectType, databaseObjectType);

		checkDbRowCount(0, 0, 0, 0, 0, 1, 0);

	}

	/**
	 * Test of 'ObjectManagementManager.getConflictingObjectTypes()'. Creates object types that are conflicting and executes
	 * 'getConflictingObjectTypes()' to fetch all conflicting object types. Case 1 - execute method on empty CR
	 */
	@Test
	public void testGetConflictingObjectTypesCase1() {

		ObjectTypeBean targetObjectType = new ObjectTypeBean(null, "newObjectPageType", false);

		// getConflictingObjectTypes()
		Collection<ObjectTypeBean> conflicting = ObjectManagementManager.getConflictingObjectTypes(dbh, targetObjectType);

		// verify collection
		assertTrue("collection should be 0 but was " + conflicting.size(), conflicting.size() == 0);

		checkDbRowCount(0, 0, 0, 0, 0, 0, 0);

	}

	/**
	 * Test of 'ObjectManagementManager.getConflictingObjectTypes()'. Creates object types that are conflicting and executes
	 * 'getConflictingObjectTypes()' to fetch all conflicting object types. Case 2 - Not conflicting
	 */
	@Test
	public void testGetConflictingObjectTypesCase2() {

		Integer type = new Integer(GenticsContentObject.OBJ_TYPE_PAGE);
		ObjectTypeBean objectType = new ObjectTypeBean(type, "newObjectPageType", false);

		// CreateNewObject
		assertTrue(ObjectManagementManager.createNewObject(dbh, "1000" + GenticsContentObject.OBJ_TYPE_FOLDER, "folder1"));

		// CreateNewObject
		assertTrue(ObjectManagementManager.createNewObject(dbh, "1001" + GenticsContentObject.OBJ_TYPE_FOLDER, "folder2"));

		// getConflictingObjectTypes()
		Collection<ObjectTypeBean> conflicting = ObjectManagementManager.getConflictingObjectTypes(dbh, objectType);

		// verify collection
		assertTrue("collection should be 0 but was " + conflicting.size(), conflicting.size() == 0);

		checkDbRowCount(0, 0, 0, 0, 0, 2, 0);

	}

	/**
	 * Test of 'ObjectManagementManager.getConflictingObjectTypes()'. Creates object types that are conflicting and executes
	 * 'getConflictingObjectTypes()' to fetch all conflicting object types. Case 3 - Conflicting
	 */
	@Test
	public void testGetConflictingObjectTypesCase3() throws Exception {

		ObjectTypeBean objectType = new ObjectTypeBean(null, "folder1", true);

		objectType.setType(new Integer(100110002));

		// CreateNewObject
		assertTrue(ObjectManagementManager.createNewObject(dbh, "1000" + GenticsContentObject.OBJ_TYPE_FOLDER, "folder1"));

		// CreateNewObject
		assertTrue(ObjectManagementManager.createNewObject(dbh, "1001" + GenticsContentObject.OBJ_TYPE_FOLDER, "folder1"));

		// getConflictingObjectTypes()
		Collection<ObjectTypeBean> conflicting = ObjectManagementManager.getConflictingObjectTypes(dbh, objectType);

		assertTrue("expect one conflict but found " + conflicting.size(), conflicting.size() == 1);

		ObjectTypeBean conflictingObjectType = conflicting.iterator().next();

		Map<String, Object> row = crdbutils.getRecordContentobject("type", "100110002");

		checkObjectTypeBean(row, conflictingObjectType);

		// checkObjectTypeBean(row, objectType);
		// checkObjectTypeBean(objectType, conflictingObjectType);

		checkDbRowCount(0, 0, 0, 0, 0, 2, 0);

	}

	/**
	 * Test of 'ObjectManagementManager.getConflictingObjectTypes()'. Creates object types that are conflicting and executes
	 * 'getConflictingObjectTypes()' to fetch all conflicting object types. Case 4 - Conflicting
	 */
	@Test
	public void testGetConflictingObjectTypesCase4() throws Exception {

		ObjectTypeBean objectType = new ObjectTypeBean(null, "folder1", true);

		objectType.setType(new Integer(100110002));
		objectType.setOldType(new Integer(245235));

		// CreateNewObject
		assertTrue(ObjectManagementManager.createNewObject(dbh, "1000" + GenticsContentObject.OBJ_TYPE_FOLDER, "folder1"));

		// CreateNewObject
		assertTrue(ObjectManagementManager.createNewObject(dbh, "1001" + GenticsContentObject.OBJ_TYPE_FOLDER, "folder1"));

		// getConflictingObjectTypes()
		Collection<ObjectTypeBean> conflicting = ObjectManagementManager.getConflictingObjectTypes(dbh, objectType);

		assertTrue("expect one conflict but found " + conflicting.size(), conflicting.size() == 1);

		ObjectTypeBean conflictingObjectType = conflicting.iterator().next();

		Map<String, Object> row = crdbutils.getRecordContentobject("type", "100110002");

		checkObjectTypeBean(row, conflictingObjectType);

		// checkObjectTypeBean(row, objectType);
		// checkObjectTypeBean(objectType, conflictingObjectType);

		checkDbRowCount(0, 0, 0, 0, 0, 2, 0);

	}

	/**
	 * Test of 'ObjectManagementManager.getLinkingAttributes()'. Creates an attribute that references to an object type and uses
	 * 'getLinkingAttriutes()' to get the collection of attributes that link to that object type. Case 1 - execute method on empty CR
	 */
	@Test
	public void testGetLinkingAttributesCase1() {

		// create object type
		Integer type = new Integer(GenticsContentObject.OBJ_TYPE_PAGE);
		ObjectTypeBean objectType1 = new ObjectTypeBean(type, "newObjectPageType", false);

		// getLinkingAttributes
		Collection<ObjectAttributeBean> linking = ObjectManagementManager.getLinkingAttributes(dbh, objectType1);

		assertTrue(linking.size() == 0);

		checkDbRowCount(0, 0, 0, 0, 0, 0, 0);
	}

	/**
	 * Test of 'ObjectManagementManager.getLinkingAttributes()'. Creates an attribute that references to an object type and uses
	 * 'getLinkingAttriutes()' to get the collection of attributes that link to that object type. Case 2 - create object and attributes,
	 * link them by, execute method to get linking attributes
	 */
	@Test
	public void testGetLinkingAttributesCase2() throws Exception {

		// create attribute & save attribute type
		ObjectAttributeBean objectAttribute = new ObjectAttributeBean("name2", GenticsContentAttribute.ATTR_TYPE_TEXT, true, "quick_name", false, 1, 1, "1", "1",
				"1", false, false);

		try {
			assertTrue(ObjectManagementManager.saveAttributeType(wds, objectAttribute, false));
		} catch (Exception e) {
			logger.info("Exception:", e);
		}

		// load attribute types (Collection)
		Collection<ObjectAttributeBean> objectAttributes = ObjectManagementManager.loadAttributeTypes(dbh);

		assertTrue(objectAttributes.size() == 1);

		// create object type & save object type
		ObjectTypeBean objectType = new ObjectTypeBean(null, "newObjectPageType", false);

		try {
			assertTrue(ObjectManagementManager.saveObjectType(wds, objectType));
		} catch (Exception e) {
			logger.info("Exception:", e);
		}

		// load object types (Collection)
		Collection<ObjectTypeBean> objectTypes = ObjectManagementManager.loadObjectTypes(dbh);

		assertTrue(objectTypes.size() == 1);

		// getLinkingAttributes from defined object type
		ObjectTypeBean objectType1 = new ObjectTypeBean(new Integer(0), "newObjectPageType", false);
		Collection<ObjectAttributeBean> linking = ObjectManagementManager.getLinkingAttributes(dbh, objectType1);

		// verify collection - no linking attributes
		assertTrue(linking.size() == 1);

		Map<String, Object> row = crdbutils.getRecordContentattributetype("name", "name2");
		ObjectAttributeBean link = ((ObjectAttributeBean) linking.toArray()[0]);

		this.checkAttribute(row, link);

		checkDbRowCount(0, 0, 1, 0, 0, 1, 0);

	}

	/**
	 * Test of 'ObjectManagementManager.isAttributeValid()'. Creates object attributes and verifies them by using 'isAttributeValid()'. Case
	 * 1 - create valid object attribute
	 */
	@Test
	public void testIsAttributeValidCase1() {

		// create proper objectAttribute
		ObjectAttributeBean objectAttribute = new ObjectAttributeBean("new attribute", GenticsContentAttribute.ATTR_TYPE_TEXT, true, "other attribute", false,
				GenticsContentObject.OBJ_TYPE_FOLDER, 0, null, null, null, false, false);

		// isAttributeValid()
		assertTrue(ObjectManagementManager.isAttributeValid(objectAttribute));

		checkDbRowCount(0, 0, 0, 0, 0, 0, 0);

	}

	/**
	 * Test of 'ObjectManagementManager.isAttributeValid()'. Creates object attributes and verifies them by using 'isAttributeValid()'. Case
	 * 2 - create invalid object attribute
	 */
	@Test
	public void testIsAttributeValidCase2() {

		// create invalid objectAttribute
		ObjectAttributeBean objectAttribute = new ObjectAttributeBean("new attribute", GenticsContentAttribute.ATTR_TYPE_TEXT, true, "other attribute", false,
				GenticsContentObject.OBJ_TYPE_FOLDER, 0, null, null, null, false, false);

		assertFalse(ObjectManagementManager.isAttributeValid(null));

		objectAttribute.setName("");
		assertFalse(ObjectManagementManager.isAttributeValid(objectAttribute));

		objectAttribute.setName("obj_type");
		assertFalse(ObjectManagementManager.isAttributeValid(objectAttribute));

		objectAttribute.setName("contentid");
		assertFalse(ObjectManagementManager.isAttributeValid(objectAttribute));

		objectAttribute.setName("mother_obj_id");
		assertFalse(ObjectManagementManager.isAttributeValid(objectAttribute));

		objectAttribute.setName("motherid");
		assertFalse(ObjectManagementManager.isAttributeValid(objectAttribute));

		objectAttribute.setName("mother_obj_type");
		assertFalse(ObjectManagementManager.isAttributeValid(objectAttribute));

		objectAttribute.setName("updatetimestamp");
		assertFalse(ObjectManagementManager.isAttributeValid(objectAttribute));

		objectAttribute.setName("obj_id");
		assertFalse(ObjectManagementManager.isAttributeValid(objectAttribute));

		objectAttribute.setName("versionTimestamp");
		assertFalse(ObjectManagementManager.isAttributeValid(objectAttribute));

		checkDbRowCount(0, 0, 0, 0, 0, 0, 0);

	}

	/**
	 * Test of 'ObjectManagementManager.getConflictingAttributes()'. Executes 'getConflictingAttributes()' and verifies the returned
	 * collection. Case 1 - execute method on empty CR. (ATTRIBUTECHECK_NAME)
	 */
	@Test
	public void testGetConflictingAttributesCase1() {

		// create target object attribute
		ObjectAttributeBean objectAttribute = new ObjectAttributeBean("new attribute", GenticsContentAttribute.ATTR_TYPE_TEXT, true, "other attribute", false,
				GenticsContentObject.OBJ_TYPE_FOLDER, 0, null, null, null, false, false);

		objectAttribute.setOldname(null);

		// getConflictingAttributes by name from target attribute
		Collection<ObjectAttributeBean> conflicts = ObjectManagementManager.getConflictingAttributes(dbh, objectAttribute,
				ObjectManagementManager.ATTRIBUTECHECK_NAME);

		// verify conflicts
		assertTrue("number of conflicts should be 0 but was" + conflicts.size(), conflicts.size() == 0);

		checkDbRowCount(0, 0, 0, 0, 0, 0, 0);

	}

	/**
	 * Test of 'ObjectManagementManager.getConflictingAttributes()'. Executes 'getConflictingAttributes()' and verifies the returned
	 * collection. Case 2 - execute method on empty CR. (ATTRIBUTECHECK_TYPE)
	 */
	@Test
	public void testGetConflictingAttributesCase2() {

		// create target object attribute
		ObjectAttributeBean objectAttribute = new ObjectAttributeBean("new attribute", GenticsContentAttribute.ATTR_TYPE_TEXT, true, "other attribute", false,
				GenticsContentObject.OBJ_TYPE_FOLDER, 0, null, null, null, false, false);

		// getConflictingAttributes by name from target attribute
		Collection<ObjectAttributeBean> conflicts = ObjectManagementManager.getConflictingAttributes(dbh, objectAttribute,
				ObjectManagementManager.ATTRIBUTECHECK_TYPE);

		// verify conflicts
		assertTrue("number of conflicts should be 0 but was" + conflicts.size(), conflicts.size() == 0);

		checkDbRowCount(0, 0, 0, 0, 0, 0, 0);

	}

	/**
	 * Test of 'ObjectManagementManager.getConflictingAttributes()'. Executes 'getConflictingAttributes()' and verifies the returned
	 * collection. Case 3 - execute method on CR with two conflicting attributes. (ATTRIBUTECHECK_TYPE)
	 */
	@Test
	public void testGetConflictingAttributesCase3() throws Exception {

		// create target object attribute
		ObjectAttributeBean targetObjectAttribute = new ObjectAttributeBean("name1", GenticsContentAttribute.ATTR_TYPE_LONG, false, "quick_name2", true,
				GenticsContentObject.OBJ_TYPE_FILE, 1, "", "", "", true, false);

		// create & save conflicting attribute 1 (by TYPE)
		ObjectAttributeBean objectAttribute1 = new ObjectAttributeBean("name1", GenticsContentAttribute.ATTR_TYPE_DATE, true, "quick_name", false,
				GenticsContentObject.OBJ_TYPE_PAGE, 0, null, null, null, false, false);

		try {
			assertTrue("saving attribute was not successful ", ObjectManagementManager.saveAttributeType(wds, objectAttribute1, true));
		} catch (ObjectManagementException e) {
			logger.info("Exception:", e);
		}

		// create & save conflicting attribute 1 (by TYPE)
		ObjectAttributeBean objectAttribute2 = new ObjectAttributeBean("name1", GenticsContentAttribute.ATTR_TYPE_DATE, true, "quick_name", false,
				GenticsContentObject.OBJ_TYPE_FOLDER, 0, null, null, null, false, false);

		try {
			assertTrue("saving attribute was not successful ", ObjectManagementManager.saveAttributeType(wds, objectAttribute2, true));
		} catch (ObjectManagementException e) {
			logger.info("Exception:", e);
		}

		// getConflictingAttributes by type
		Collection<ObjectAttributeBean> conflicts = ObjectManagementManager.getConflictingAttributes(dbh, targetObjectAttribute,
				ObjectManagementManager.ATTRIBUTECHECK_TYPE);

		assertTrue(conflicts.size() == 2);
		ObjectAttributeBean conflict1 = ((ObjectAttributeBean) conflicts.toArray()[0]);
		ObjectAttributeBean conflict2 = ((ObjectAttributeBean) conflicts.toArray()[1]);

		Map<String, Object> row1 = crdbutils.getRecordContentattributetype("objecttype", "10007");
		Map<String, Object> row2 = crdbutils.getRecordContentattributetype("objecttype", "10002");

		checkAttribute(row1, conflict1);
		checkAttribute(row2, conflict2);

		checkDbRowCount(0, 0, 2, 0, 0, 0, 0);
	}

	/**
	 * Test of 'ObjectManagementManager.getConflictingAttributes()'. Executes 'getConflictingAttributes()' and verifies the returned
	 * collection. Case 4 - execute method on CR with two conflicting attributes. (ATTRIBUTECHECK_NAME)
	 */
	@Test
	public void testGetConflictingAttributesCase4() throws Exception {

		// create target object attribute
		ObjectAttributeBean targetObjectAttribute = new ObjectAttributeBean("name1", GenticsContentAttribute.ATTR_TYPE_TEXT, true, "quick_name", false,
				GenticsContentObject.OBJ_TYPE_PAGE, 0, null, null, null, false, false);

		targetObjectAttribute.setOldname(null);

		// create & save conflicting two attributes (by NAME)
		ObjectAttributeBean objectAttribute1 = new ObjectAttributeBean("name1", GenticsContentAttribute.ATTR_TYPE_TEXT, true, "quick_name", false,
				GenticsContentObject.OBJ_TYPE_PAGE, 0, null, null, null, false, false);

		try {
			ObjectManagementManager.saveAttributeType(wds, objectAttribute1, true);
		} catch (ObjectManagementException e) {
			logger.info("Exception:", e);
		}

		// create & save conflicting two attributes (by NAME)
		ObjectAttributeBean objectAttribute2 = new ObjectAttributeBean("name1", GenticsContentAttribute.ATTR_TYPE_TEXT, true, "quick_name", false,
				GenticsContentObject.OBJ_TYPE_FOLDER, 0, null, null, null, false, false);

		try {
			ObjectManagementManager.saveAttributeType(wds, objectAttribute2, true);
		} catch (ObjectManagementException e) {
			logger.info("Exception:", e);
		}

		// getConflictingAttributes by type
		Collection<ObjectAttributeBean> conflicts = ObjectManagementManager.getConflictingAttributes(dbh, targetObjectAttribute,
				ObjectManagementManager.ATTRIBUTECHECK_NAME);

		assertTrue("conflicts were " + conflicts.size() + " but should be 1", conflicts.size() == 1);

		ObjectAttributeBean conflictingAttribute = ((ObjectAttributeBean) conflicts.toArray()[0]);

		// verify collection
		Map<String, Object> row = crdbutils.getRecordContentattributetype("objecttype", "10007");

		assertTrue(row != null);
		checkAttribute(row, conflictingAttribute);
		checkAttribute(targetObjectAttribute, conflictingAttribute);

		checkDbRowCount(0, 0, 2, 0, 0, 0, 0);
	}

	/**
	 * Test of 'ObjectManagementManager.getConflictingAttributes()'. Executes 'getConflictingAttributes()' and verifies the returned
	 * collection. Case 3 - execute method on CR with non two conflicting attributes. (ATTRIBUTECHECK_TYPE)
	 */
	@Test
	public void testGetConflictingAttributesCase5() {

		// create target object attribute
		ObjectAttributeBean targetObjectAttribute = new ObjectAttributeBean("name1", GenticsContentAttribute.ATTR_TYPE_TEXT, true, "quick_name", false,
				GenticsContentObject.OBJ_TYPE_PAGE, 0, null, null, null, false, false);

		// create & save conflicting two attributes (by TYPE)
		ObjectAttributeBean objectAttribute1 = new ObjectAttributeBean("name2", GenticsContentAttribute.ATTR_TYPE_TEXT, true, "quick_name", false,
				GenticsContentObject.OBJ_TYPE_PAGE, 0, null, null, null, false, false);

		try {
			ObjectManagementManager.saveAttributeType(wds, objectAttribute1, true);
		} catch (ObjectManagementException e) {
			logger.info("Exception:", e);
		}

		ObjectAttributeBean objectAttribute2 = new ObjectAttributeBean("name1", GenticsContentAttribute.ATTR_TYPE_TEXT, true, "quick_name", false,
				GenticsContentObject.OBJ_TYPE_PAGE, 0, null, null, null, false, false);

		try {
			ObjectManagementManager.saveAttributeType(wds, objectAttribute2, true);
		} catch (ObjectManagementException e) {
			logger.info("Exception:", e);
		}

		// getConflictingAttributes by type
		Collection<ObjectAttributeBean> conflicts = ObjectManagementManager.getConflictingAttributes(dbh, targetObjectAttribute,
				ObjectManagementManager.ATTRIBUTECHECK_TYPE);

		assertTrue("number of conflicts should be 0 but was" + conflicts.size(), conflicts.size() == 0);

		checkDbRowCount(0, 0, 2, 0, 0, 0, 0);

	}

	/**
	 * Test of 'ObjectManagementManager.getConflictingAttributes()'. Executes 'getConflictingAttributes()' and verifies the returned
	 * collection. Case 6 - execute method on CR with two non-conflicting attributes. (ATTRIBUTECHECK_NAME)
	 */
	@Test
	public void testGetConflictingAttributesCase6() {

		// create target object attribute
		ObjectAttributeBean targetObjectAttribute = new ObjectAttributeBean("name1", GenticsContentAttribute.ATTR_TYPE_TEXT, true, "quick_name", false,
				GenticsContentObject.OBJ_TYPE_PAGE, 0, null, null, null, false, false);

		targetObjectAttribute.setOldname(null);

		// create & save non-conflicting two attributes (by NAME)
		ObjectAttributeBean objectAttribute1 = new ObjectAttributeBean("name2", GenticsContentAttribute.ATTR_TYPE_TEXT, true, "quick_name", false,
				GenticsContentObject.OBJ_TYPE_PAGE, 0, null, null, null, false, false);

		try {
			ObjectManagementManager.saveAttributeType(wds, objectAttribute1, true);
		} catch (ObjectManagementException e) {
			logger.info("Exception:", e);
		}

		ObjectAttributeBean objectAttribute2 = new ObjectAttributeBean("name3", GenticsContentAttribute.ATTR_TYPE_TEXT, true, "quick_name", false,
				GenticsContentObject.OBJ_TYPE_PAGE, 0, null, null, null, false, false);

		try {
			ObjectManagementManager.saveAttributeType(wds, objectAttribute2, true);
		} catch (ObjectManagementException e) {
			logger.info("Exception:", e);
		}

		// getConflictingAttributes by name
		Collection<ObjectAttributeBean> conflicts = ObjectManagementManager.getConflictingAttributes(dbh, targetObjectAttribute,
				ObjectManagementManager.ATTRIBUTECHECK_NAME);

		assertTrue("number of conflicts should be 0 but was" + conflicts.size(), conflicts.size() == 0);

		checkDbRowCount(0, 0, 2, 0, 0, 0, 0);

	}

	/**
	 * Test of 'ObjectManagementManager.getIndexName()'. Executes 'getIndexName()' and verifies the returned string.
	 */
	@Test
	public void testGetIndexName() {

		try {
			// getIndexName()
			String str = ObjectManagementManager.getIndexName("test1234");

			// verify string
			assertTrue("returned String is empty", str.length() != 0);
		} catch (Exception e) {
			logger.info("Exception:", e);
			assertTrue(false);
		}
		checkDbRowCount(0, 0, 0, 0, 0, 0, 0);
	}

	/**
	 * Test of 'ObjectManagementManager.deleteObjectType()'. Creates objecttypes and executes 'deleteObjectType()' to delete them. Deletion
	 * must occur properly. Case 1 - invoke deleteObjectType on empty database
	 */
	@Test
	public void testDeleteObjectTypeCase1() {

		// define object type
		Integer type = new Integer(GenticsContentObject.OBJ_TYPE_PAGE);
		ObjectTypeBean objectType = new ObjectTypeBean(type, "newObjectPageType", false);

		// delete object type - true
		try {
			ObjectManagementManager.deleteObjectType(dbh, objectType, true);
		} catch (ObjectManagementException e) {
			logger.info("Exception:", e);
		}

		// delete object type - false
		try {
			ObjectManagementManager.deleteObjectType(dbh, objectType, false);
		} catch (ObjectManagementException e) {
			logger.info("Exception:", e);
		}

		// verify that nothing happened
		checkDbRowCount(0, 0, 0, 0, 0, 0, 0);
	}

	/**
	 * Test of 'ObjectManagementManager.deleteObjectType()'. Case 2 - create&save one object type, delete this object type.
	 */
	@Test
	public void testDeleteObjectTypeCase2() {

		// create object type
		ObjectTypeBean objectType = new ObjectTypeBean(null, "newObjectPageType", false);

		// save object type
		try {
			ObjectManagementManager.saveObjectType(wds, objectType);
		} catch (ObjectManagementException e) {
			logger.info("Exception:", e);
		}
		checkDbRowCount(0, 0, 0, 0, 0, 1, 0);

		// delete object type
		try {
			ObjectManagementManager.deleteObjectType(dbh, objectType, true);
		} catch (ObjectManagementException e) {
			logger.info("Exception:", e);
		}

		checkDbRowCount(0, 0, 0, 0, 0, 0, 0);
	}

	/**
	 * Test of 'ObjectManagementManager.deleteObjectType()'. Case 3 - create&save two object type, delete just one object type.
	 */
	@Test
	public void testDeleteObjectTypeCase3() {

		// create object type #1
		ObjectTypeBean objectType1 = new ObjectTypeBean(null, "newObjectPageType", false);

		// save object type #1
		try {
			ObjectManagementManager.saveObjectType(wds, objectType1);
		} catch (ObjectManagementException e) {
			logger.info("Exception:", e);
		}
		checkDbRowCount(0, 0, 0, 0, 0, 1, 0);

		// create object type #2
		ObjectTypeBean objectType2 = new ObjectTypeBean(null, "newObjectPageType", false);

		// save object type #3
		try {
			ObjectManagementManager.saveObjectType(wds, objectType2);
		} catch (ObjectManagementException e) {
			logger.info("Exception:", e);
		}

		checkDbRowCount(0, 0, 0, 0, 0, 2, 0);

		// delete object type #2
		try {
			ObjectManagementManager.deleteObjectType(dbh, objectType2, true);
		} catch (ObjectManagementException e) {
			logger.info("Exception:", e);
		}

		checkDbRowCount(0, 0, 0, 0, 0, 1, 0);
	}

	/**
	 * Test of 'ObjectManagementManager.deleteObjectType()'. Creates object types and executes 'deleteObjectType()' to delete them. Deletion
	 * must occur properly. Case 4 - create&save two object types, delete another object type that does not exist in CR.
	 */
	@Test
	public void testDeleteObjectTypeCase4() {

		// create object type #1
		ObjectTypeBean objectType = new ObjectTypeBean(null, "ObjectPageType", false);

		// save object type #1
		try {
			ObjectManagementManager.saveObjectType(wds, objectType);
		} catch (ObjectManagementException e) {
			logger.info("Exception:", e);
		}

		// create object type #2
		ObjectTypeBean objectType2 = new ObjectTypeBean(null, "ObjectPageType", false);

		// save object type #2
		try {
			ObjectManagementManager.saveObjectType(wds, objectType2);
		} catch (ObjectManagementException e) {
			logger.info("Exception:", e);
		}

		checkDbRowCount(0, 0, 0, 0, 0, 2, 0);

		ObjectTypeBean targetObjectType = new ObjectTypeBean(new Integer(122), "newObjectPageType", false);

		// delete target object type
		try {
			ObjectManagementManager.deleteObjectType(dbh, targetObjectType, true);
		} catch (ObjectManagementException e) {
			logger.info("Exception:", e);
		}

		checkDbRowCount(0, 0, 0, 0, 0, 2, 0);
	}

	/**
	 * Test of 'ObjectManagementManager.deleteAttributeType()'. Creates attribute types and executes 'deleteAttributeType()' to delete them.
	 * Deletion must occur properly. Case 1 - invoke deleteAttributeType on empty database
	 */
	@Test
	public void testDeleteAttributeTypeCase1() {

		// create object attribute
		ObjectAttributeBean objectAttribute = new ObjectAttributeBean("new attribute", GenticsContentAttribute.ATTR_TYPE_TEXT, true, "other attribute", false,
				GenticsContentObject.OBJ_TYPE_FOLDER, 0, null, null, null, false, false);

		// delete attribute type()
		try {
			ObjectManagementManager.deleteAttributeType(dbh, objectAttribute, true);
		} catch (ObjectManagementException e) {
			logger.info("Exception:", e);
		}

		checkDbRowCount(0, 0, 0, 0, 0, 0, 0);

	}

	/**
	 * This test checks whether the invocation of the syncOptimizedColumn method stalls when using transactions. A mssql jdbc issue caused stalling connections when
	 * transactional and non transactional connections were used.
	 *
	 * @throws SQLException
	 */
	@Test
	public void testSyncOptimizedColumnWithTransactions() throws SQLException {

		DB.startTransaction(dbh);
		// create attribute type
		ObjectAttributeBean objectAttribute = new ObjectAttributeBean("name2", GenticsContentAttribute.ATTR_TYPE_TEXT, true, "quick_name", false,
				GenticsContentObject.OBJ_TYPE_FOLDER, 0, null, null, null, false, false);

		// save attribute type
		try {
			assertTrue(ObjectManagementManager.saveAttributeType(wds, objectAttribute, true));
		} catch (ObjectManagementException e) {
			logger.info("Exception:", e);
		}
		// delete attribute type
		try {
			ObjectManagementManager.deleteAttributeType(dbh, objectAttribute, true);
		} catch (ObjectManagementException e) {
			logger.info("Exception:", e);
		}
		DB.commitTransaction(dbh);

		// verify attribute was created
		checkDbRowCount(0, 0, 0, 0, 0, 0, 0);
	}

	/**
	 * Test of 'ObjectManagementManager.deleteAttributeType()'. Creates attribute types and executes 'deleteAttributeType()' to delete them.
	 * Deletion must occur properly. Case 2 - create&save AttributeType, delete this AttributeType
	 */
	@Test
	public void testDeleteAttributeTypeCase2() {

		// create attribute type
		ObjectAttributeBean objectAttribute = new ObjectAttributeBean("name2", GenticsContentAttribute.ATTR_TYPE_TEXT, true, "quick_name", false,
				GenticsContentObject.OBJ_TYPE_FOLDER, 0, null, null, null, false, false);

		// save attribute type
		try {
			assertTrue(ObjectManagementManager.saveAttributeType(wds, objectAttribute, true));
		} catch (ObjectManagementException e) {
			logger.info("Exception:", e);
		}

		checkDbRowCount(0, 0, 1, 0, 0, 0, 0);

		// delete attribute type
		try {
			ObjectManagementManager.deleteAttributeType(dbh, objectAttribute, true);
		} catch (ObjectManagementException e) {
			logger.info("Exception:", e);
		}

		// verify attribute was created
		checkDbRowCount(0, 0, 0, 0, 0, 0, 0);

	}

	/**
	 * Test of 'ObjectManagementManager.deleteAttributeType()'. Creates attribute types and executes 'deleteAttributeType()' to delete them.
	 * Deletion must occur properly. Case 3 - create&save two AttributeType, delete one AttributeType
	 */
	@Test
	public void testDeleteAttributeTypeCase3() throws Exception {

		// create attribute type #1
		ObjectAttributeBean objectAttribute1 = new ObjectAttributeBean("name1", GenticsContentAttribute.ATTR_TYPE_TEXT, true, "quick_name", false,
				GenticsContentObject.OBJ_TYPE_FOLDER, 0, null, null, null, false, false);

		// save attribute type #1
		try {
			assertTrue(ObjectManagementManager.saveAttributeType(wds, objectAttribute1, true));
		} catch (ObjectManagementException e) {
			logger.info("Exception:", e);
		}

		checkDbRowCount(0, 0, 1, 0, 0, 0, 0);

		// create attribute type #2
		ObjectAttributeBean objectAttribute2 = new ObjectAttributeBean("name2", GenticsContentAttribute.ATTR_TYPE_TEXT, true, "quick_name", false,
				GenticsContentObject.OBJ_TYPE_FOLDER, 0, null, null, null, false, false);

		// save attribute type #2
		try {
			assertTrue(ObjectManagementManager.saveAttributeType(wds, objectAttribute2, true));
		} catch (ObjectManagementException e) {
			logger.info("Exception:", e);
		}

		checkDbRowCount(0, 0, 2, 0, 0, 0, 0);

		// delete attribute type #2
		try {
			ObjectManagementManager.deleteAttributeType(dbh, objectAttribute2, true);
		} catch (ObjectManagementException e) {
			logger.info("Exception:", e);
		}

		// fetch attribute
		Map<String, Object> row = crdbutils.getRecordContentattributetype("name", "name1");
		Collection<ObjectAttributeBean> attributeTypes = ObjectManagementManager.loadAttributeTypes(dbh);
		ObjectAttributeBean attributeType = ((ObjectAttributeBean) attributeTypes.toArray()[0]);

		// verify that existing attribute is not attribute2
		checkAttribute(row, objectAttribute1);
		checkAttribute(attributeType, objectAttribute1);

		// verify attribute was deleted
		checkDbRowCount(0, 0, 1, 0, 0, 0, 0);

	}

	/**
	 * Test of 'ObjectManagementManager.deleteAttributeType()'. Creates attribute types and executes 'deleteAttributeType()' to delete them.
	 * Deletion must occur properly. Case 4 - create&save AttributeType, delete another AttributeType which was not saved to CR
	 */
	@Test
	public void testDeleteAttributeTypeCase4() {

		// create attribute type
		ObjectAttributeBean objectAttribute = new ObjectAttributeBean("name1", GenticsContentAttribute.ATTR_TYPE_TEXT, true, "quick_name", false,
				GenticsContentObject.OBJ_TYPE_FOLDER, 0, null, null, null, false, false);

		// save attribute type
		try {
			assertTrue(ObjectManagementManager.saveAttributeType(wds, objectAttribute, true));
		} catch (ObjectManagementException e) {
			logger.info("Exception:", e);
		}

		// create attribute type
		ObjectAttributeBean targetAttributeType = new ObjectAttributeBean("name3", GenticsContentAttribute.ATTR_TYPE_TEXT, true, "quick_name", false,
				GenticsContentObject.OBJ_TYPE_FOLDER, 0, null, null, null, false, false);

		// delete attribute type
		try {
			ObjectManagementManager.deleteAttributeType(dbh, targetAttributeType, true);
		} catch (ObjectManagementException e) {
			logger.info("Exception:", e);
		}

		checkDbRowCount(0, 0, 1, 0, 0, 0, 0);
	}

	/**
	 * Test of 'ObjectManagementManager.loadAttributeTypes()'. Executes loadAttributeTypes() and verifies the returned collection. Case 1 -
	 * execute loadAttributeTypes on empty CR
	 */
	@Test
	public void testLoadAttributeTypesCase1() {

		// loadAttributeTypes()
		Collection<ObjectAttributeBean> attributeTypes = ObjectManagementManager.loadAttributeTypes(dbh);

		// examine collection
		assertTrue("expected number of types is 0 but was " + attributeTypes.size(), attributeTypes.size() == 0);

		checkDbRowCount(0, 0, 0, 0, 0, 0, 0);
	}

	/**
	 * Test of 'ObjectManagementManager.loadAttributeTypes()'. Executes loadAttributeTypes() and verifies the returned collection. Case 2 -
	 * add fife attributes and execute loadAttributeTypes. Verify all attributes were included in collection.
	 */
	@Test
	public void testLoadAttributeTypesCase2() throws Exception {

		// create & save attribute type
		int i = 0;

		while (i < 5) {
			ObjectAttributeBean targetAttributeType = new ObjectAttributeBean("name" + i, GenticsContentAttribute.ATTR_TYPE_TEXT, true, "quick_name", false,
					GenticsContentObject.OBJ_TYPE_FOLDER, 0, null, null, null, false, false);

			try {
				assertTrue(ObjectManagementManager.saveAttributeType(wds, targetAttributeType, true));
			} catch (ObjectManagementException e) {
				logger.info("Exception:", e);
			}
			i++;
		}

		// load attribute types
		Collection<ObjectAttributeBean> attributeTypes = ObjectManagementManager.loadAttributeTypes(dbh);

		assertTrue("expected number of types is 5 but was " + attributeTypes.size(), attributeTypes.size() == 5);

		// examine collection
		i = 0;
		while (i < 5) {
			ObjectAttributeBean referenceAttribute = new ObjectAttributeBean("name" + i, GenticsContentAttribute.ATTR_TYPE_TEXT, true, "quick_name", false,
					GenticsContentObject.OBJ_TYPE_FOLDER, 0, null, null, null, false, false);

			ObjectAttributeBean attributeType = null;

			int r = 0;

			while (r < 5) {
				attributeType = ((ObjectAttributeBean) attributeTypes.toArray()[r]);
				if (attributeType.getName().equalsIgnoreCase("name" + i)) {
					break;
				}
				r++;
			}

			if (attributeType == null) {
				assertFalse("matching attribute was not found", true);
			}

			Map<String, Object> row = crdbutils.getRecordContentattributetype("name", "name" + i);

			// verify database (row) content
			checkAttribute(row, referenceAttribute);

			// verify collection attribute with reference
			checkAttribute(attributeType, referenceAttribute);
			i++;
		}

		checkDbRowCount(0, 0, 5, 0, 0, 0, 0);

	}

	/**
	 * Test of 'ObjectManagementManager.setReferences()'. Creates two collections and all references between both collections. Case 1 - set
	 * valid reference between object type and attribute type collection
	 */
	@Test
	public void testSetReferencesCase1() {

		// create c1 objectTypes collection
		ObjectTypeBean objectType = new ObjectTypeBean(null, "newObjectPageType", false);

		try {
			assertTrue(ObjectManagementManager.saveObjectType(wds, objectType));
		} catch (Exception e) {
			logger.info("Exception:", e);
		}
		Collection<ObjectTypeBean> c1 = ObjectManagementManager.loadObjectTypes(dbh);

		assertTrue("expected number of types is 1 but was " + c1.size(), c1.size() == 1);

		// create c2 attributeTypes collection
		ObjectAttributeBean objectAttribute = new ObjectAttributeBean("name1", GenticsContentAttribute.ATTR_TYPE_TEXT, true, "quick_name", false, 1, 0, null,
				null, null, false, false);

		try {
			assertTrue(ObjectManagementManager.saveAttributeType(wds, objectAttribute, false));
		} catch (Exception e) {
			logger.info("Exception:", e);
		}
		Collection<ObjectAttributeBean> c2 = ObjectManagementManager.loadAttributeTypes(dbh);

		assertTrue("expected number of types is 1 but was " + c2.size(), c2.size() == 1);

		// verify attribute types of object type before setting references
		ObjectTypeBean objectTypeNOREF = ((ObjectTypeBean) c1.toArray()[0]);
		ObjectAttributeBean[] nObjectTypeNOREF = objectTypeNOREF.getAttributeTypes();

		assertTrue("expecting 0 assigned attribute types but was " + nObjectTypeNOREF.length, nObjectTypeNOREF.length == 0);

		// setReferences(c1,c2)
		ObjectManagementManager.setReferences(c1, c2);

		// verify attribute types of object type after setting references
		ObjectTypeBean objectTypeREF = ((ObjectTypeBean) c1.toArray()[0]);
		ObjectAttributeBean[] nObjectTypeREF = objectTypeREF.getAttributeTypes();

		assertTrue("expecting 1 assigned attribute types but was " + nObjectTypeREF.length, nObjectTypeREF.length == 1);

		// validate attribute
		checkAttribute(objectAttribute, nObjectTypeREF[0]);

		checkDbRowCount(0, 0, 1, 0, 0, 1, 0);

	}

	/**
	 * Test of 'ObjectManagementManager.setReferences()'. Creates two collections and all references between both collections. Case 2 -
	 * execute setReference on two collections. No reference should be set because id of attribute type and object type are different.
	 */
	@Test
	public void testSetReferencesCase2() {

		// create c1 objectTypes collection
		ObjectTypeBean objectType = new ObjectTypeBean(null, "newObjectPageType", false);

		try {
			assertTrue(ObjectManagementManager.saveObjectType(wds, objectType));
		} catch (Exception e) {
			logger.info("Exception:", e);
		}
		Collection<ObjectTypeBean> c1 = ObjectManagementManager.loadObjectTypes(dbh);

		assertTrue("expected number of types is 1 but was " + c1.size(), c1.size() == 1);

		// create c2 attributeTypes collection
		ObjectAttributeBean objectAttribute = new ObjectAttributeBean("name1", GenticsContentAttribute.ATTR_TYPE_TEXT, true, "quick_name", false, 12, 0, null,
				null, null, false, false);

		try {
			assertTrue(ObjectManagementManager.saveAttributeType(wds, objectAttribute, false));
		} catch (Exception e) {
			logger.info("Exception:", e);
		}
		Collection<ObjectAttributeBean> c2 = ObjectManagementManager.loadAttributeTypes(dbh);

		assertTrue("expected number of types is 1 but was " + c2.size(), c2.size() == 1);

		// verify attribute types of object type before setting references
		ObjectTypeBean objectTypeNOREF = ((ObjectTypeBean) c1.toArray()[0]);
		ObjectAttributeBean[] nObjectTypeNOREF = objectTypeNOREF.getAttributeTypes();

		assertTrue("expecting 0 assigned attribute types but was " + nObjectTypeNOREF.length, nObjectTypeNOREF.length == 0);

		// setReferences(c1,c2)
		ObjectManagementManager.setReferences(c1, c2);

		// verify attribute types of object type after setting references
		ObjectTypeBean objectTypeREF = ((ObjectTypeBean) c1.toArray()[0]);
		ObjectAttributeBean[] nObjectTypeREF = objectTypeREF.getAttributeTypes();

		assertTrue("expecting 1 assigned attribute types but was " + nObjectTypeREF.length, nObjectTypeREF.length == 0);

		checkDbRowCount(0, 0, 1, 0, 0, 1, 0);

	}

	/**
	 * Test of 'ObjectManagementManager.getNextObjectType()'. Executes 'getNextObjectType()' and verifies returned string. Case 1 -
	 * getNextObjectType on empty CR
	 */
	@Test
	public void testGetNextObjectTypeCase1() {

		// getNextObjectType()
		String id = ObjectManagementManager.getNextObjectType(dbh);

		assertTrue("1".equalsIgnoreCase(id));

		checkDbRowCount(0, 0, 0, 0, 0, 0, 0);
	}

	/**
	 * Test of 'ObjectManagementManager.getNextObjectType()'. Executes 'getNextObjectType()' and verifies returned string. Case 2 - create
	 * new objects and getNextObjectType
	 */
	@Test
	public void testGetNextObjectTypeCase2() {

		int i = 0;

		while (i < 100) {
			String nid = String.valueOf(1000 + i);

			assertTrue(ObjectManagementManager.createNewObject(dbh, nid, "folder1"));

			// getNextObjectType()
			String id = ObjectManagementManager.getNextObjectType(dbh);

			// verify string
			assertTrue(String.valueOf(1000 + i + 1).equalsIgnoreCase(id));
			i++;
		}

		checkDbRowCount(0, 0, 0, 0, 0, 100, 0);

	}

	/**
	 * Test of 'ObjectManagementManager.getDiff()'. This method creates two collections and executes 'getDiff()' to determine the difference
	 * of both collections. Case 1 - No difference between both collections
	 */
	@Test
	public void testGetDiffCase1() {

		// create ten objects
		int i = 0;

		while (i < 10) {
			assertTrue(ObjectManagementManager.createNewObject(dbh, String.valueOf(1000 + i), "folder1"));
			i++;
		}

		checkDbRowCount(0, 0, 0, 0, 0, 10, 0);

		// getCollection
		Collection<ObjectTypeBean> objectTypes1 = ObjectManagementManager.loadObjectTypes(dbh);

		// getCollection another collection
		Collection<ObjectTypeBean> objectTypes2 = ObjectManagementManager.loadObjectTypes(dbh);

		// execute getDiff()
		TypeDiff diff = ObjectManagementManager.getDiff(objectTypes1, objectTypes2);
		Collection<?> subdiff;

		// verify that there is no difference
		subdiff = diff.getAddedObjectTypes();
		assertTrue(subdiff.size() == 0);

		// verify that there is no difference
		subdiff = diff.getDeletedObjectTypes();
		assertTrue(subdiff.size() == 0);

		// verify that there is no difference
		subdiff = diff.getModifiedObjectTypes();
		assertTrue(subdiff.size() == 0);

		checkDbRowCount(0, 0, 0, 0, 0, 10, 0);
	}

	/**
	 * Test of 'ObjectManagementManager.getDiff()'. This method creates two collections and executes 'getDiff()' to determine the difference
	 * of both collections. Case 2 - One object added between both collections
	 */
	@Test
	public void testGetDiffCase2() throws Exception {

		int i = 0;

		while (i < 10) {
			assertTrue(ObjectManagementManager.createNewObject(dbh, String.valueOf(1000 + i), "folder1"));
			i++;
		}

		checkDbRowCount(0, 0, 0, 0, 0, 10, 0);

		// getCollection
		Collection<ObjectTypeBean> objectTypes1 = ObjectManagementManager.loadObjectTypes(dbh);

		// create another object
		assertTrue(ObjectManagementManager.createNewObject(dbh, String.valueOf(1011), "folder1"));

		// getCollection
		Collection<ObjectTypeBean> objectTypes2 = ObjectManagementManager.loadObjectTypes(dbh);

		// execute getDiff()
		TypeDiff diff = ObjectManagementManager.getDiff(objectTypes1, objectTypes2);
		Collection<?> subdiff;

		subdiff = diff.getAddedObjectTypes();
		assertTrue(subdiff.size() == 1);
		ObjectTypeBean diffObject = ((ObjectTypeBean) subdiff.toArray()[0]);
		Map<String, Object> row = crdbutils.getRecordContentobject("type", String.valueOf(1011));

		checkObjectTypeBean(row, diffObject);

		subdiff = diff.getDeletedObjectTypes();
		assertTrue(subdiff.size() == 0);

		subdiff = diff.getModifiedObjectTypes();
		assertTrue(subdiff.size() == 0);

		checkDbRowCount(0, 0, 0, 0, 0, 11, 0);
	}

	/**
	 * Test of 'ObjectManagementManager.getDiff()'. This method creates two collections and executes 'getDiff()' to determine the difference
	 * of both collections. Case 3 - One object deleted between both collections
	 */
	@Test
	public void testGetDiffCase3() throws Exception {

		int i = 0;

		while (i < 10) {
			assertTrue(ObjectManagementManager.createNewObject(dbh, String.valueOf(1000 + i), "folder1"));
			i++;
		}

		checkDbRowCount(0, 0, 0, 0, 0, 10, 0);

		// getCollection
		Collection<ObjectTypeBean> objectTypes1 = ObjectManagementManager.loadObjectTypes(dbh);

		// create another object
		assertTrue(ObjectManagementManager.createNewObject(dbh, String.valueOf(1011), "folder1"));

		// getCollection
		Collection<ObjectTypeBean> objectTypes2 = ObjectManagementManager.loadObjectTypes(dbh);

		// execute getDiff()
		TypeDiff diff = ObjectManagementManager.getDiff(objectTypes2, objectTypes1);
		Collection<?> subdiff;

		subdiff = diff.getAddedObjectTypes();
		assertTrue(subdiff.size() == 0);

		subdiff = diff.getDeletedObjectTypes();
		assertTrue(subdiff.size() == 1);
		ObjectTypeBean diffObject = ((ObjectTypeBean) subdiff.toArray()[0]);
		Map<String, Object> row = crdbutils.getRecordContentobject("type", String.valueOf(1011));

		checkObjectTypeBean(row, diffObject);

		subdiff = diff.getModifiedObjectTypes();
		assertTrue(subdiff.size() == 0);

		checkDbRowCount(0, 0, 0, 0, 0, 11, 0);
	}

	/**
	 * Test of 'ObjectManagementManager.getDiff()'. This method creates two collections and executes 'getDiff()' to determine the difference
	 * of both collections. Case 4 - One object modified between both collections
	 */
	@Test
	public void testGetDiffCase4() throws Exception {

		int i = 0;

		while (i < 10) {
			assertTrue(ObjectManagementManager.createNewObject(dbh, String.valueOf(1000 + i), "folder1"));
			i++;
		}

		checkDbRowCount(0, 0, 0, 0, 0, 10, 0);

		// create another object type that should be modified
		assertTrue(ObjectManagementManager.createNewObject(dbh, String.valueOf(1011), "folder1"));

		Map<String, Object> rowOriginal = crdbutils.getRecordContentobject("type", String.valueOf(1011));
		// getCollection
		Collection<ObjectTypeBean> objectTypes1 = ObjectManagementManager.loadObjectTypes(dbh);

		// modify object type
		ObjectTypeBean modifiedObjectType = new ObjectTypeBean(new Integer(1011), "folder1", true);

		try {
			assertTrue(ObjectManagementManager.saveObjectType(wds, modifiedObjectType));
		} catch (Exception e) {
			logger.info("Exception:", e);
		}

		// getCollection
		Collection<ObjectTypeBean> objectTypes2 = ObjectManagementManager.loadObjectTypes(dbh);

		// execute getDiff()
		TypeDiff diff = ObjectManagementManager.getDiff(objectTypes1, objectTypes2);
		Collection<?> subdiff;

		subdiff = diff.getAddedObjectTypes();
		assertTrue(subdiff.size() == 0);

		subdiff = diff.getDeletedObjectTypes();
		assertTrue(subdiff.size() == 0);

		subdiff = diff.getModifiedObjectTypes();
		assertTrue(subdiff.size() == 1);

		ObjectTypeDiff diffObject = ((ObjectTypeDiff) subdiff.toArray()[0]);

		Collection<?> subDiffObjectCol;

		subDiffObjectCol = diffObject.getDeletedAttributeTypes();
		assertTrue(subDiffObjectCol.size() == 0);

		subDiffObjectCol = diffObject.getModifiedAttributeTypes();
		assertTrue(subDiffObjectCol.size() == 0);

		subDiffObjectCol = diffObject.getAddedAttributeTypes();
		assertTrue(subDiffObjectCol.size() == 0);

		// verify changes from objects
		Map<String, Object> rowModified = crdbutils.getRecordContentobject("type", String.valueOf(1011));
		ObjectTypeBean modifiedDiffObject = diffObject.getModifiedObjectType();
		ObjectTypeBean orginalDiffObject = diffObject.getOriginalObjectType();

		// compare modifiedDiffObject with collected reference data
		checkObjectTypeBean(rowModified, modifiedObjectType);
		checkObjectTypeBean(rowModified, modifiedDiffObject);
		checkObjectTypeBean(modifiedObjectType, modifiedDiffObject);

		// compare orginalDiffObject with collected reference data
		checkObjectTypeBean(rowOriginal, orginalDiffObject);

		checkDbRowCount(0, 0, 0, 0, 0, 11, 0);
	}

	/**
	 * Test of 'ObjectManagementManager.getTypeDiff()'. Case 1 - calculate difference between two object types (no difference)
	 */
	@Test
	public void testGetTypeDiffCase1() {

		Integer type = new Integer(GenticsContentObject.OBJ_TYPE_PAGE);
		// create object type
		ObjectTypeBean objectType = new ObjectTypeBean(type, "newObjectPageType", false);

		// getTypeDiff(type1,null)
		ObjectTypeDiff diff = ObjectManagementManager.getTypeDiff(objectType, objectType);

		assertTrue(diff == null);

		checkDbRowCount(0, 0, 0, 0, 0, 0, 0);
	}

	/**
	 * Test of 'ObjectManagementManager.getTypeDiff()'. Case 2 - calculate difference with two different object types - no modified
	 * attribute types - just modified name
	 */
	@Test
	public void testGetTypeDiffCase2() {

		Integer type = new Integer(GenticsContentObject.OBJ_TYPE_PAGE);
		// create object type 1,2
		ObjectTypeBean objectType1 = new ObjectTypeBean(type, "newObjectPageType1", false);
		ObjectTypeBean objectType2 = new ObjectTypeBean(type, "newObjectPageType2", false);

		// getTypeDiff(type1,type2)
		ObjectTypeDiff diff = ObjectManagementManager.getTypeDiff(objectType1, objectType2);

		Collection<?> subdiff;

		subdiff = diff.getAddedAttributeTypes();
		assertTrue(subdiff.size() == 0);

		subdiff = diff.getDeletedAttributeTypes();
		assertTrue(subdiff.size() == 0);

		subdiff = diff.getModifiedAttributeTypes();
		assertTrue(subdiff.size() == 0);

		ObjectTypeBean modDiffType = diff.getModifiedObjectType();

		checkObjectTypeBean(objectType2, modDiffType);

		ObjectTypeBean orgDiffType = diff.getOriginalObjectType();

		checkObjectTypeBean(objectType1, orgDiffType);

		checkDbRowCount(0, 0, 0, 0, 0, 0, 0);
	}

	/**
	 * Test of 'ObjectManagementManager.getTypeDiff()'. Case 3 - calculate difference with two different object types - added attribute
	 * types.
	 */
	@Test
	public void testGetTypeDiffCase3() {

		Integer type = new Integer(GenticsContentObject.OBJ_TYPE_PAGE);
		// create object type 1,2
		ObjectTypeBean objectType1 = new ObjectTypeBean(type, "newObjectPageType1", false);
		ObjectTypeBean objectType2 = new ObjectTypeBean(type, "newObjectPageType2", false);

		// add attribute type for objecType1
		ObjectAttributeBean objectAttribute = new ObjectAttributeBean("new attribute", GenticsContentAttribute.ATTR_TYPE_TEXT, true, "other attribute", false,
				GenticsContentObject.OBJ_TYPE_PAGE, 0, null, null, null, false, false);

		objectType1.addAttributeType(objectAttribute);

		// getTypeDiff(type1,type2)
		ObjectTypeDiff diff = ObjectManagementManager.getTypeDiff(objectType2, objectType1);

		Collection<?> subdiff;

		subdiff = diff.getAddedAttributeTypes();
		assertTrue("there should be one added attribute", subdiff.size() == 1);
		ObjectAttributeBean subDiffAttribute = ((ObjectAttributeBean) subdiff.toArray()[0]);

		checkAttribute(objectAttribute, subDiffAttribute);

		subdiff = diff.getDeletedAttributeTypes();
		assertTrue(subdiff.size() == 0);

		subdiff = diff.getModifiedAttributeTypes();
		assertTrue(subdiff.size() == 0);

		ObjectTypeBean modDiffType = diff.getModifiedObjectType();

		checkObjectTypeBean(objectType1, modDiffType);

		ObjectTypeBean orgDiffType = diff.getOriginalObjectType();

		checkObjectTypeBean(objectType2, orgDiffType);

		checkDbRowCount(0, 0, 0, 0, 0, 0, 0);
	}

	/**
	 * Test of 'ObjectManagementManager.getTypeDiff()'. Case 4 - calculate difference with two different object types - deleted attribute
	 * types.
	 */
	@Test
	public void testGetTypeDiffCase4() {

		Integer type = new Integer(GenticsContentObject.OBJ_TYPE_PAGE);
		// create object type 1,2
		ObjectTypeBean objectType1 = new ObjectTypeBean(type, "newObjectPageType1", false);
		ObjectTypeBean objectType2 = new ObjectTypeBean(type, "newObjectPageType2", false);

		// add attribute type for objecType1
		ObjectAttributeBean objectAttribute = new ObjectAttributeBean("new attribute", GenticsContentAttribute.ATTR_TYPE_TEXT, true, "other attribute", false,
				GenticsContentObject.OBJ_TYPE_PAGE, 0, null, null, null, false, false);

		objectType1.addAttributeType(objectAttribute);

		// getTypeDiff(type1,type2)
		ObjectTypeDiff diff = ObjectManagementManager.getTypeDiff(objectType1, objectType2);

		Collection<ObjectAttributeBean> subdiff;

		subdiff = diff.getAddedAttributeTypes();
		assertTrue(subdiff.size() == 0);

		subdiff = diff.getDeletedAttributeTypes();
		assertTrue("there should be one deleted attribute", subdiff.size() == 1);
		ObjectAttributeBean subDiffAttribute = ((ObjectAttributeBean) subdiff.toArray()[0]);

		checkAttribute(objectAttribute, subDiffAttribute);

		subdiff = diff.getModifiedAttributeTypes();
		assertTrue(subdiff.size() == 0);

		ObjectTypeBean modDiffType = diff.getModifiedObjectType();

		checkObjectTypeBean(objectType2, modDiffType);

		ObjectTypeBean orgDiffType = diff.getOriginalObjectType();

		checkObjectTypeBean(objectType1, orgDiffType);

		checkDbRowCount(0, 0, 0, 0, 0, 0, 0);
	}

	/**
	 * Test of 'ObjectManagementManager.getTypeDiff()'. Case 5 - calculate difference with two different object types - modified attribute
	 * types.
	 */
	@Test
	public void testGetTypeDiffCase5() {

		Integer type = new Integer(GenticsContentObject.OBJ_TYPE_PAGE);

		// create object type 1,2
		ObjectTypeBean objectType1 = new ObjectTypeBean(type, "newObjectPageType1", false);
		ObjectTypeBean objectType2 = new ObjectTypeBean(type, "newObjectPageType2", false);

		// add attribute type for objecType1
		ObjectAttributeBean objectAttribute1 = new ObjectAttributeBean("new attribute", GenticsContentAttribute.ATTR_TYPE_TEXT, true, "other attribute", false,
				GenticsContentObject.OBJ_TYPE_PAGE, 0, null, null, null, false, false);

		objectType1.addAttributeType(objectAttribute1);

		ObjectAttributeBean objectAttribute2 = new ObjectAttributeBean("new attribute", GenticsContentAttribute.ATTR_TYPE_BLOB, true, "other attribute", false,
				GenticsContentObject.OBJ_TYPE_PAGE, 0, null, null, null, false, false);

		objectType2.addAttributeType(objectAttribute2);

		// getTypeDiff(type1,type2)
		ObjectTypeDiff diff = ObjectManagementManager.getTypeDiff(objectType1, objectType2);

		Collection<ObjectAttributeBean> subdiff;

		subdiff = diff.getAddedAttributeTypes();
		assertTrue(subdiff.size() == 0);

		subdiff = diff.getDeletedAttributeTypes();
		assertTrue(subdiff.size() == 0);

		subdiff = diff.getModifiedAttributeTypes();
		assertTrue("there should be one modified attribute type", subdiff.size() == 1);
		ObjectAttributeBean subDiffAttribute = ((ObjectAttributeBean) subdiff.toArray()[0]);

		checkAttribute(objectAttribute2, subDiffAttribute);

		ObjectTypeBean modDiffType = diff.getModifiedObjectType();

		checkObjectTypeBean(objectType2, modDiffType);

		ObjectTypeBean orgDiffType = diff.getOriginalObjectType();

		checkObjectTypeBean(objectType1, orgDiffType);

		checkDbRowCount(0, 0, 0, 0, 0, 0, 0);
	}

	/**
	 * TODO only works for contentmap table now. add comments in dumps for other tables. get the create statement for the given sql table of
	 * a cr, specific to a database.
	 *
	 * @param table
	 *            the tablename, like contentmap.
	 * @param database
	 *            one of mysql, mssql or oracle.
	 * @return the create statment.
	 * @throws NodeException
	 *             when invalid database has been given, or file could not be parsed.
	 */
	public static String getCreateTableForCRTable(String table, String database) throws NodeException {

		try {

			String path = "/com/gentics/dumps/cr_structure_" + database + ".sql";
			InputStream stream = CRSyncObjectManagementManagerTest.class.getResourceAsStream(path);

			assertNotNull("Could not find dump in classpath [" + path + "]", stream);

			BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
			String line = null;
			String create = "";
			String sep = "\n";

			while ((line = reader.readLine()) != null) {
				create = create.concat(line + sep);
			}

			int start = create.indexOf("gentics-start-table-" + table);
			int end = create.indexOf("gentics-end-table-" + table);

			create = create.substring(start, end);

			// trim first and last line
			create = create.substring(create.indexOf(sep), create.lastIndexOf(sep));
			// add newline
			create = create + sep;
			return create;

		} catch (Exception ex) {
			throw new NodeException(ex);
		}

	}

	/**
	 * Test of 'ObjectManagementManager.getCreateTableForCRTable()'. Executes 'getCreateTableForCRTable()' with different combinations for
	 * mysql and oracle db.
	 */
	@Test
	public void testGetCreateTableForCRTable() {

		// Fetching 'contentmap_nodeversion'
		String sql = "";

		try {
			sql = getCreateTableForCRTable("contentmap_nodeversion", "mssql");
			assertTrue("returned string is empty", sql.length() != 0);
		} catch (Exception e) {
			logger.info("Exception:", e);
		}

		try {
			sql = getCreateTableForCRTable("contentmap_nodeversion", "mysql");
			assertTrue("returned string is empty", sql.length() != 0);
		} catch (Exception e) {
			logger.info("Exception:", e);
		}

		try {
			sql = getCreateTableForCRTable("contentmap_nodeversion", "oracle");
			assertTrue("returned string is empty", sql.length() != 0);
		} catch (Exception e) {
			logger.info("Exception:", e);
		}

		// Fetching 'contentmap'

		try {
			sql = getCreateTableForCRTable("contentmap", "mssql");
			assertTrue("returned string is empty", sql.length() != 0);
		} catch (Exception e) {
			logger.info("Exception:", e);
		}

		try {
			sql = getCreateTableForCRTable("contentmap", "mysql");
			assertTrue("returned string is empty", sql.length() != 0);
		} catch (Exception e) {
			logger.info("Exception:", e);
		}

		try {
			sql = getCreateTableForCRTable("contentmap", "oracle");
			assertTrue("returned string is empty", sql.length() != 0);
		} catch (Exception e) {
			logger.info("Exception:", e);
		}

		checkDbRowCount(0, 0, 0, 0, 0, 0, 0);

	}

	/**
	 * Test saving an attribute with filesystem flag
	 */
	@Test
	public void testSaveAttributeFilesystem() throws Exception {
		String fsTrueName = "fs_true";
		String fsFalseName = "fs_False";

		// save an attributetype with filesystem flag set
		ObjectAttributeBean filesystemTrue = new ObjectAttributeBean(fsTrueName, GenticsContentAttribute.ATTR_TYPE_TEXT_LONG, false, null, false,
				GenticsContentObject.OBJ_TYPE_FOLDER, 0, null, null, null, false, true);

		ObjectManagementManager.saveAttributeType(wds, filesystemTrue, false);
		assertFilesystemFlag(fsTrueName, true);

		// save an attributetype with filesystem flag not set
		ObjectAttributeBean filesystemFalse = new ObjectAttributeBean(fsFalseName, GenticsContentAttribute.ATTR_TYPE_TEXT_LONG, false, null, false,
				GenticsContentObject.OBJ_TYPE_FOLDER, 0, null, null, null, false, false);

		ObjectManagementManager.saveAttributeType(wds, filesystemFalse, false);
		assertFilesystemFlag(fsFalseName, false);

		// now swap the flags
		filesystemTrue.setFilesystem(false);
		ObjectManagementManager.saveAttributeType(wds, filesystemTrue, false);
		assertFilesystemFlag(fsTrueName, false);

		filesystemFalse.setFilesystem(true);
		ObjectManagementManager.saveAttributeType(wds, filesystemFalse, false);
		assertFilesystemFlag(fsFalseName, true);
	}

	/**
	 * Test saving inconsistent attributes
	 *
	 * @throws Exception
	 */
	@Test
	public void testInconsistentAttribute() throws Exception {

		/*
		 try {
		 ObjectManagementManager.saveAttributeType(dbh, new ObjectAttributeBean("inconsistent", GenticsContentAttribute.ATTR_TYPE_TEXT, true,
		 "bla", true, GenticsContentObject.OBJ_TYPE_FOLDER, 0, null, null, null, false, false), false);
		 fail("Saving optimized multivalue attribute should fail");
		 } catch (ObjectManagementException e) {
		 // anticipated
		 }

		 // try saving an optimized filesystem attribute
		 try {
		 ObjectManagementManager.saveAttributeType(dbh, new ObjectAttributeBean("inconsistent", GenticsContentAttribute.ATTR_TYPE_TEXT, true,
		 "bla", false, GenticsContentObject.OBJ_TYPE_FOLDER, 0, null, null, null, false, true), false);
		 fail("Saving optimized filesystem attribute should fail");
		 } catch (ObjectManagementException e) {
		 // anticipated
		 }
		 */

		// try saving an optimized multivalue attribute
		try {
			ObjectManagementManager.saveAttributeType(wds,
					new ObjectAttributeBean("inconsistent", GenticsContentAttribute.ATTR_TYPE_TEXT, true, "bla", true, GenticsContentObject.OBJ_TYPE_FOLDER, 0, null,
					null, null, false, false),
					false);
			fail("Saving optimized multivalue attribute should fail");
		} catch (ObjectManagementException e) {// anticipated
		}

		// try saving an optimized filesystem attribute
		try {
			ObjectManagementManager.saveAttributeType(wds,
					new ObjectAttributeBean("inconsistent", GenticsContentAttribute.ATTR_TYPE_TEXT, true, "bla", false, GenticsContentObject.OBJ_TYPE_FOLDER, 0, null,
					null, null, false, true),
					false);
			fail("Saving optimized filesystem attribute should fail");
		} catch (ObjectManagementException e) {// anticipated
		}
	}

	// Tests end

	/**
	 * emtpy database
	 *
	 * @param db
	 *            database connection
	 * @throws Exception
	 */
	private void emtpyTables(DBCon db) throws Exception {
		for (int i = 0; i < CRTableCompare.CRTABLES.length; i++) {
			db.updateSQL("DELETE FROM " + CRTableCompare.CRTABLES[i]);
		}

		db.updateSQL("DELETE FROM contentstatus");
		// drop quick columns after delete
		// dropQuickColumns(db, "contentmap");
		// setUpTables(db);
	}

	/**
	 * Make an assertion on the filesystem flag of the given attribute
	 * @param attributeName name of the attribute
	 * @param filesystem asserted flag value
	 * @throws Exception
	 */
	private void assertFilesystemFlag(final String attributeName, final boolean filesystem) throws Exception {
		DB.query(dbh, "SELECT * FROM contentattributetype WHERE name = ?", new Object[] { attributeName }, new ResultProcessor() {
			public void takeOver(ResultProcessor p) {}

			public void process(ResultSet rs) throws SQLException {
				while (rs.next()) {
					if (filesystem) {
						assertTrue("Filesystem flag must be set for attribute " + attributeName, rs.getBoolean(DatatypeHelper.FILESYSTEM_FIELD));
					} else {
						assertFalse("filesystem flag must not be set for attribute " + attributeName, rs.getBoolean(DatatypeHelper.FILESYSTEM_FIELD));
					}
				}
			}
		});
	}

	class CRDatabaseUtils {

		public static final String CONTENTATTRIBUTE_NODEVERSION = "contentattribute_nodeversion";

		public static final String CONTENTATTRIBUTE = "contentattribute";

		public static final String CONTENTATTRIBUTETYPE = "contentattributetype";

		public static final String CONTENTMAP = "contentmap";

		public static final String CONTENTMAP_NODEVERSION = "contentmap_nodeversion";

		public static final String CONTENTSTATUS = "contentstatus";

		public static final String CONTENTOBJECT = "contentobject";

		SQLUtils sqlUtils;

		public CRDatabaseUtils(SQLUtils sqlUtils) throws Exception {
			this.sqlUtils = sqlUtils;
		}

		public int getCountTable(String tablename) {
			try {
				ResultSet rs = sqlUtils.executeQuery("SELECT count(*) FROM " + tablename);

				if (rs.next()) {
					int num = rs.getInt(1);

					return num;
				}
			} catch (SQLException e) {
				logger.info("SQL EXCEPTION", e);
			}
			return -1;
		}

		/**
		 * @param keyname
		 * @param key
		 */
		public Map<String, Object> getRecordContentattributetype(String keyname, String key) throws Exception {
			SimpleResultProcessor proc = new SimpleResultProcessor();

			DB.query(dbh, "SELECT * FROM " + CONTENTATTRIBUTETYPE + " WHERE " + keyname + " = ?", new Object[] { key}, proc);
			if (proc.size() == 0) {
				return null;
			} else {
				return proc.getRow(1).getMap();
			}
		}

		/**
		 * @param keyname
		 * @param key
		 */
		public Map<String, Object> getRecordContentobject(String keyname, String key) throws Exception {
			SimpleResultProcessor proc = new SimpleResultProcessor();

			DB.query(dbh, "SELECT * FROM " + CONTENTOBJECT + " WHERE " + keyname + " = ?", new Object[] { key}, proc);
			if (proc.size() == 0) {
				return null;
			} else {
				return proc.getRow(1).getMap();
			}
		}
	}
}
