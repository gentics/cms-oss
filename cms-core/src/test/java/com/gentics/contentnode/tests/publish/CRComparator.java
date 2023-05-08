/*
 * @author johannes2
 * @date 19.08.2008
 * @version $Id: CRComparator.java,v 1.1 2010-02-04 14:25:04 norbert Exp $
 */
package com.gentics.contentnode.tests.publish;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;

import org.dbunit.Assertion;
import org.dbunit.DatabaseUnitException;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.xml.XmlDataSet;

import com.gentics.lib.log.NodeLogger;
import com.gentics.testutils.GenericTestUtils;
import com.gentics.testutils.dbunit.AssertionExtension;

public class CRComparator {

	// Reference CR database on dev6 (used by php publisher)
	private IDatabaseConnection connectionReferenceCR;

	private Properties crReferenceDBSettings;

	// Target CR database on dev6 (used for java publisher)
	private IDatabaseConnection connectionTargetCR;

	public Properties crTargetDBSettings;

	private String currentReferenceFile;

	private NodeLogger logger;

	public static Properties compareSettings;

	public CRComparator(Properties crTargetDBSettings, Properties crReferenceDBSettings,
			Properties crComparatorProperties, String currentReferenceFile) throws Exception {
		logger = NodeLogger.getNodeLogger(CRComparator.class.getClass());

		compareSettings = crComparatorProperties; 
		this.currentReferenceFile = currentReferenceFile;
		this.crTargetDBSettings = crTargetDBSettings;
		this.crReferenceDBSettings = crReferenceDBSettings;
		setupDBUnit();
	}

	public void disconnect() throws SQLException {
		connectionReferenceCR.close();
		connectionTargetCR.close();
	}

	/**
	 * Initialize a full compare of both content repositories
	 * @throws SQLException
	 * @throws DataSetException
	 * @throws DatabaseUnitException
	 */
	public void doFullCompare() throws SQLException, DataSetException, DatabaseUnitException {

		doContentobjectCompare();
		doContentmapCompare();
		doContentattributetypeCompare();
		doContentattributeCompare();
	}

	private ArrayList getAttributeNamesFromSettings(String name) {
		String line = compareSettings.getProperty(name);
		ArrayList ar = new ArrayList();

		if (line == null) {
			return ar;
		}
		String[] attrs = line.split(",");
		int i = 0;

		while (i < attrs.length) {
			ar.add(attrs[i]);
			i++;
		}

		return ar;
	}

	/**
	 * Compare the contentattribute table
	 * @throws SQLException
	 * @throws DataSetException
	 * @throws DatabaseUnitException
	 */
	public void doContentattributeCompare() throws SQLException, DataSetException,
				DatabaseUnitException {

		// These attributes must not be compared because php publisher creates
		// them but they can not be filled because of missing language
		// definition. java publisher knows this and wont create them.
		ArrayList excludedAttributes = getAttributeNamesFromSettings("excludedContentAttributes");

		// These attributes must be compared by removing 0,"",null records from
		// reference
		ArrayList diffAttributes = getAttributeNamesFromSettings("ignoreNullValuesForcontentAttriutes");

		// These attributes contain real diffs in the data (for files and
		// folders)
		// PHP Publisher wont work with these mapnames because the tagnames do
		// not match the specifications:
		// http://www.gentics.com/Content.Node/infoportal/documentation/contentnode/reserved_tags.php
		ArrayList realDiffAttributes = getAttributeNamesFromSettings("includeContentAttributeOnlyForPages");

		doContentattributeCompareStep1(excludedAttributes, diffAttributes, realDiffAttributes);
		doContentattributeCompareStep2(diffAttributes);
		doContentattributeCompareStep3(realDiffAttributes);
	}

	/**
	 * Compare the contentattribute table - only compare attributes that were not excluded  
	 * @param excludedAttributes
	 * @param diffAttributes
	 * @param realDiffAttributes
	 * @throws DataSetException
	 * @throws SQLException
	 */
	private void doContentattributeCompareStep1(ArrayList excludedAttributes,
			ArrayList diffAttributes, ArrayList realDiffAttributes) throws DataSetException,
				SQLException {
		String whereClause = getWhereClause(excludedAttributes, diffAttributes, realDiffAttributes);

		String sql = "SELECT contentid, name, value_text, value_bin,value_int, sortorder, value_blob, value_clob, value_long,value_double,value_date FROM contentattribute "
				+ whereClause + " order by contentid,name";

		logger.debug("CRComparator - SQL: " + sql);
		ITable reference = connectionReferenceCR.createQueryTable("contentattribute:reference", sql);

		ITable target = connectionTargetCR.createQueryTable("contentattribute:target", sql);

		// Compare the left attributes
		AssertionExtension.assertEqualsHandleTables(reference, target);
	}

	/**
	 * Compare the contentattribute table - only compare the attributes that have known differences within definitionsq of (null/0/"")
	 * @param diffAttributes
	 * @throws DataSetException
	 * @throws SQLException
	 */
	private void doContentattributeCompareStep2(ArrayList diffAttributes) throws DataSetException, SQLException {
		logger.info("CRComparator - Basic comparing done - starting extended comparing.");

		Iterator it = diffAttributes.iterator();

		while (it.hasNext()) {

			String attributeName = ((String) it.next());

			logger.info("CRComparator - Comparing: " + attributeName);
			if (attributeName.equalsIgnoreCase("startpage")) {
				// TODO handle this diff
				logger.info("CRComparator - Skipping startpage");
				continue;
			}
			String whereClause = "where name =\"" + attributeName + "\"";

			String sql = "SELECT contentid, name, value_text, value_bin,value_int, sortorder, value_blob, value_clob, value_long,value_double,value_date FROM contentattribute "
					+ whereClause + " and ( value_int != 0 or value_text != \"\") order by contentid,name";

			logger.debug("CRComparator - SQL: " + sql);

			ITable reference = connectionReferenceCR.createQueryTable("contentattribute:reference", sql);

			ITable target = connectionTargetCR.createQueryTable("contentattribute:target", sql);

			// Compare semi diff attributes
			AssertionExtension.assertEqualsWithNull(reference, target);

		}
	}

	/**
	 * Compare the contentattribute table - compare pages we skipped earlier 
	 * @param realDiffAttributes
	 * @throws DataSetException
	 * @throws SQLException
	 */
	private void doContentattributeCompareStep3(ArrayList realDiffAttributes) throws DataSetException, SQLException {
		logger.info("CRComparator - Comparing real diffs for pages. We skipped files and folders.");
		// Compare realdiff pages because we just skipped those.
		Iterator itd = realDiffAttributes.iterator();

		while (itd.hasNext()) {

			String attributeName = ((String) itd.next());
			String whereClause = "where name = \"" + attributeName + "\" and contentid like \"10007.%\"";
			String sql = "SELECT contentid, name, value_text, value_bin,value_int, sortorder, value_blob, value_clob, value_long,value_double,value_date FROM contentattribute "
					+ whereClause + " order by contentid,name";

			logger.debug("CRComparator - SQL: " + sql);

			ITable reference = connectionReferenceCR.createQueryTable("contentattribute:reference", sql);

			ITable target = connectionTargetCR.createQueryTable("contentattribute:target", sql);

			AssertionExtension.assertEqualsWithNull(reference, target);

		}
	}

	private String getWhereClause(ArrayList names, ArrayList names2, ArrayList names3) {
		if (names.isEmpty() && names2.isEmpty() && names3.isEmpty()) {
			return "";
		}
        
		String sql = "where ";

		Iterator it = names.iterator();
		Iterator it2 = names2.iterator();
		Iterator it3 = names3.iterator();

		while (it.hasNext()) {
			String name = (String) it.next();

			sql += "name != \"" + name + "\"";

			if (it.hasNext() || it2.hasNext() || it3.hasNext()) {
				sql += " and ";
			}

		}

		while (it2.hasNext()) {
			String name = (String) it2.next();

			sql += "name != \"" + name + "\"";

			if (it2.hasNext() || it3.hasNext()) {
				sql += " and ";
			}
		}

		while (it3.hasNext()) {
			String name = (String) it3.next();

			sql += "name != \"" + name + "\"";

			if (it3.hasNext()) {
				sql += " and ";
			}
		}

		return sql;
	}

	/**
	 * Compare the content attribute type table with the reference
	 * @throws SQLException
	 * @throws DatabaseUnitException
	 */
	public void doContentattributetypeCompare() throws SQLException, DatabaseUnitException {
		ITable reference = connectionReferenceCR.createQueryTable("contentattributetype:reference",
				"SELECT * FROM contentattributetype order by name, objecttype");

		ITable target = connectionTargetCR.createQueryTable("contentattributetype:target", "SELECT * FROM contentattributetype order by name, objecttype");

		AssertionExtension.assertEqualsHandleTables(reference, target);
	}

	/**
	 * Compare the contentmap table with the reference
	 * @throws SQLException
	 * @throws DatabaseUnitException
	 */
	public void doContentmapCompare() throws SQLException, DatabaseUnitException {
		ITable reference = connectionReferenceCR.createQueryTable("contentmap:reference",
				"SELECT contentid, obj_id, obj_type, mother_obj_id, mother_obj_type, motherid, quick_publisher, quick_public, quick_node_id, quick_navsortorder FROM contentmap order by contentid");

		ITable target = connectionTargetCR.createQueryTable("contentmap:target",
				"SELECT contentid, obj_id, obj_type, mother_obj_id, mother_obj_type, motherid, quick_publisher, quick_public, quick_node_id, quick_navsortorder FROM contentmap order by contentid");

		AssertionExtension.assertEqualsHandleTables(reference, target);
	}

	/**
	 * Compare the contentobject table
	 * @throws SQLException
	 * @throws DatabaseUnitException
	 */
	public void doContentobjectCompare() throws SQLException, DatabaseUnitException {
		ITable reference = connectionReferenceCR.createQueryTable("contentobject:reference", "SELECT * FROM contentobject order by type");
		ITable target = connectionTargetCR.createQueryTable("contentobject:target", "SELECT * FROM contentobject order by type");

		Assertion.assertEquals(reference, target);

	}

	/* File comparison methods */
    
	/**
	 * Save all tables and its data into xmlfile using dbunit.
	 * @throws Exception
	 */
	public void recordReference() throws Exception {
		IDataSet fullDataSet = connectionTargetCR.createDataSet();
		OutputStream fos = new FileOutputStream(currentReferenceFile);

		XmlDataSet.write(fullDataSet, fos);
	}

	/**
	 * Load the reference xml file and get a dataset of the current data and
	 * compare both
	 * @throws Exception
	 */
	public void compareWithReferenceFile(boolean recordReference) throws Exception {
		if (recordReference) {
			logger.info("recordReference is enabled. we will skip comparing sequence.");
			return;
		}
		logger.info("CRComparator - Starting comparison");
		IDataSet databaseDataSet2 = connectionTargetCR.createDataSet();
		OutputStream fos = new FileOutputStream(GenericTestUtils.getTMPFile());

		XmlDataSet.write(databaseDataSet2, fos);
		InputStream inst = new FileInputStream(GenericTestUtils.getTMPFile());
		IDataSet databaseDataSet = new XmlDataSet(inst);

		InputStream ins = new FileInputStream(new java.io.File(currentReferenceFile));
		IDataSet expectedFlatDataSet = new XmlDataSet(ins);

		AssertionExtension.assertEqualsWithColumnFilter(databaseDataSet, expectedFlatDataSet);
	}

	/**
	 * Setup the dbUnit database connection
	 * @throws Exception
	 */
	private void setupDBUnit() throws Exception {
		Class driverClass = Class.forName(crTargetDBSettings.getProperty("driverClass"));
		Connection jdbcConnectionTarget = DriverManager.getConnection(crTargetDBSettings.getProperty("url"), crTargetDBSettings.getProperty("username"),
				crTargetDBSettings.getProperty("passwd"));

		connectionTargetCR = new DatabaseConnection(jdbcConnectionTarget);
		connectionTargetCR.getConfig().setFeature(DatabaseConfig.FEATURE_QUALIFIED_TABLE_NAMES, true);

		driverClass = Class.forName(crReferenceDBSettings.getProperty("driverClass"));
		Connection jdbcConnectionSource = DriverManager.getConnection(crReferenceDBSettings.getProperty("url"), crReferenceDBSettings.getProperty("username"),
				crReferenceDBSettings.getProperty("passwd"));

		connectionReferenceCR = new DatabaseConnection(jdbcConnectionSource);
		connectionReferenceCR.getConfig().setFeature(DatabaseConfig.FEATURE_QUALIFIED_TABLE_NAMES, true);
	}

}
