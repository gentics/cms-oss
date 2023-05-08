package com.gentics.node.tests.datasource.cn;

import static org.junit.Assert.assertTrue;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.datasource.DatasourceException;
import com.gentics.api.lib.datasource.WriteableDatasource;
import com.gentics.api.lib.resolving.Changeable;
import com.gentics.lib.content.DatatypeHelper;
import com.gentics.lib.content.GenticsContentAttribute;
import com.gentics.lib.content.GenticsContentObject;
import com.gentics.lib.datasource.CNWriteableDatasource;
import com.gentics.lib.datasource.SQLHandle;
import com.gentics.lib.datasource.SimpleHandlePool;
import com.gentics.lib.datasource.object.ObjectAttributeBean;
import com.gentics.lib.datasource.object.ObjectManagementManager;
import com.gentics.lib.db.DB;
import com.gentics.lib.db.DBHandle;
import com.gentics.lib.db.StringLengthManipulator;
import com.gentics.testutils.database.JDBCMalformedURLException;
import com.gentics.testutils.database.SQLUtilException;
import com.gentics.testutils.database.SQLUtils;
import com.gentics.testutils.database.SQLUtilsFactory;
import com.gentics.testutils.database.TestDatabase;
import com.gentics.testutils.database.variations.AbstractSingleVariationDatabaseTest;
import com.gentics.testutils.database.variations.TestDatabaseVariationConfigurations;

/**
 * Test whether Character length semantics detection is exact for different
 * database configurations.
 *
 * @author escitalopram
 *
 */
@Category(BaseLibTest.class)
public class CharacterLengthSemanticsTest extends AbstractSingleVariationDatabaseTest {

	/**
	 * Use a non-BMP Character for databases that support it as well as a
	 * character with a 3 byte UTF-8 representation and 256 'ä' characters
	 */
	public static final String WITH_NON_BMP_CHAR = "\uD834\uDD1E\u5666ääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääää";

	/** No non-BMP Characters for databases that don't support it (MySQL) */
	public static final String WITHOUT_NON_BMP_CHAR = "\u5666\u5666\u5666ääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääääää";

	/** A String length manipulator implementation that does nothing */
	public static final StringLengthManipulator NO_OP_TRUNCATOR = new StringLengthManipulator() {

		public String truncate(String text, int length) {
			return text;
		}

		public int getLength(String text) {
			return text.codePointCount(0, text.length());
		}
	};

	public CharacterLengthSemanticsTest(TestDatabase testDatabase) {
		super(testDatabase);
	}

	/**
	 * Get variation data
	 *
	 * @return variation data
	 * @throws JDBCMalformedURLException
	 */
	@Parameters(name = "{index}: singleDBTest: {0}")
	public static Collection<Object[]> data() throws JDBCMalformedURLException {
		return Arrays.asList(getData(TestDatabaseVariationConfigurations.MYSQL_VARIATIONS, TestDatabaseVariationConfigurations.MSSQL_VARIATIONS,
				TestDatabaseVariationConfigurations.ORACLE_VARIATIONS));
	}

	private SQLUtils sqlUtils;
	private StringLengthManipulator dbTruncator;
	private CNWriteableDatasource ds;
	private SQLHandle handle;
	private DBHandle dbHandle;

	/**
	 * Set up a content repository with a page contentobject and a name
	 * attribute
	 *
	 * @throws Exception
	 *             when something goes wrong
	 */
	@Before
	public void setUp() throws Exception {
		sqlUtils = SQLUtilsFactory.getSQLUtils(testDatabase);

		sqlUtils.connectDatabase();
		testDatabase.setRandomDatabasename("prefix");
		sqlUtils.createCRDatabase(getClass());

		handle = new SQLHandle("mumu");

		handle.init(testDatabase.getSettingsMap());

		dbHandle = handle.getDBHandle();
		dbTruncator = dbHandle.getStringLengthManipulator();

		DB.update(dbHandle, "insert into contentobject (name, type, id_counter, exclude_versioning) values ('page', 10007, 1, 0)");
		ds = new CNWriteableDatasource(null, new SimpleHandlePool(handle), (Map<?, ?>) testDatabase.getSettingsMap());

		ObjectManagementManager.saveAttributeType(ds, new ObjectAttributeBean("name", GenticsContentAttribute.ATTR_TYPE_TEXT, true, "quick_name", false,
				GenticsContentObject.OBJ_TYPE_PAGE, 0, null, null, null, false, false), true);
		assertTrue(((WriteableDatasource) ds).canWrite());

	}

	/**
	 * return a truncated test string that the current database can handle
	 *
	 * @return
	 * @throws SQLException
	 */
	private String getTruncatedTestString() throws SQLException {

		// Work around MySQL's inability to work with non-BMP-characters
		if (Arrays.asList(DatatypeHelper.MYSQL_NAME, DatatypeHelper.MARIADB_NAME).contains(DB.getDatabaseProductName(dbHandle))) {
			return dbTruncator.truncate(WITHOUT_NON_BMP_CHAR, 255);
		} else {
			return dbTruncator.truncate(WITH_NON_BMP_CHAR, 255);
		}

	}

	/**
	 * Test if text attributes are undertruncated
	 *
	 * @throws Exception
	 */
	@Test
	public void testUnderTruncation() throws Exception {

		String testName = getTruncatedTestString();

		// Don't truncate in the database layer beyond this point
		dbHandle.setStringLengthManipulator(NO_OP_TRUNCATOR);

		Map<String, String> map = new HashMap<String, String>();

		map.put("obj_type", "10007");
		map.put("name", testName);
		Changeable changeable = ((WriteableDatasource) ds).create(map);

		// This will throw an exception if the attribute is too long.
		ds.store(Collections.singleton(changeable));
	}

	/**
	 * Test if text attributes are overtruncated
	 *
	 * @throws Exception
	 */
	@Test(expected = DatasourceException.class)
	public void testOverTruncation() throws Exception {

		// attributes are overtruncated if we can add a character and it still
		// fits into the db column
		String testName = getTruncatedTestString() + "ä";

		// Don't truncate in the database layer beyond this point
		dbHandle.setStringLengthManipulator(NO_OP_TRUNCATOR);

		Map<String, String> map = new HashMap<String, String>();

		map.put("obj_type", "10007");
		map.put("name", testName);
		Changeable changeable = ((WriteableDatasource) ds).create(map);

		// This will throw an exception if the attribute is too long.
		ds.store(Collections.singleton(changeable));
	}

	/**
	 * Cleanup
	 * @throws SQLUtilException
	 */
	@After
	public void tearDown() throws SQLUtilException {
			ds.getHandlePool().close();
			sqlUtils.removeDatabase();
	}

}
