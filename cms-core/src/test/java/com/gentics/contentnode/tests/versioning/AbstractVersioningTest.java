/**
 * 
 */
package com.gentics.contentnode.tests.versioning;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.init.MigrateTimeManagement;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.lib.db.DB;
import com.gentics.lib.db.DBHandle;
import com.gentics.lib.db.ResultProcessor;
import com.gentics.lib.db.SimpleResultProcessor;
import com.gentics.lib.db.SimpleResultRow;
import com.gentics.lib.db.TableVersion;
import com.gentics.lib.db.TableVersion.Join;
import com.gentics.lib.db.UpdateProcessor;
import com.gentics.testutils.database.SQLUtils;
import com.gentics.testutils.database.SQLUtilsFactory;
import com.gentics.testutils.database.TestDatabase;
import com.gentics.testutils.database.pool.TestDatabaseRepository;
import com.gentics.testutils.database.utils.SQLDumpUtils;

/**
 * @author norbert
 * 
 */
public class AbstractVersioningTest {
	/**
	 * Name of the SQL File containing the testdata
	 */
	public final static String NODEDB_TESTDATA_FILENAME = "node4_versioning_nodedb.sql";

	public final static Long PAGE_ID = 8l;

	public final static Long CONTENT_ID = 8l;

	// we omit the time management migration, because the dump we use is not compatible with the new data structure
	// the time management migration will be run after the dump is inserted
	@Rule
	public DBTestContext testContext = new DBTestContext().omit(MigrateTimeManagement.class);

	protected DBHandle dbHandle;

	protected TableVersion pageVersion;

	protected TableVersion contenttagVersion;

	protected TableVersion dsVersion;

	protected TableVersion dsObjVersion;

	protected TableVersion valueVersion;

	@SuppressWarnings("deprecation")
	@Before
	public void setUp() throws Exception {

		TestDatabase database = TestDatabaseRepository.getMySQLStableDatabase();
		SQLUtils dbUtils = SQLUtilsFactory.getSQLUtils(database);
		SQLDumpUtils dumpUtils = new SQLDumpUtils(dbUtils);

		// insert test data
		ArrayList statements = dumpUtils.readSQLFile(new File(getClass().getResource(NODEDB_TESTDATA_FILENAME).toURI()));
		for (Object statement : statements) {
			DBUtils.executeStatement(ObjectTransformer.getString(statement, null), Transaction.UPDATE_STATEMENT);
		}

		Transaction t = TransactionManager.getCurrentTransaction();
		dbHandle = t.getDBHandle();

		new MigrateTimeManagement().execute();
		t.commit(false);

		pageVersion = new TableVersion(false);
		pageVersion.setHandle(dbHandle);
		pageVersion.setTable("page");
		pageVersion.setWherePart("gentics_main.id = ?");

		contenttagVersion = new TableVersion(false);
		contenttagVersion.setHandle(dbHandle);
		contenttagVersion.setTable("contenttag");
		contenttagVersion.setJoin("content", "id", "content_id");
		contenttagVersion.setWherePart("content.id = ?");

		valueVersion = new TableVersion(false);
		valueVersion.setHandle(dbHandle);
		valueVersion.setTable("value");
		valueVersion.setJoin("contenttag", "id", "contenttag_id");
		valueVersion.setWherePart("contenttag.content_id = ?");

		dsVersion = new TableVersion(false);
		dsVersion.setHandle(dbHandle);
		dsVersion.setTable("ds");
		dsVersion.setJoin("contenttag", "id", "contenttag_id");
		dsVersion.setWherePart("contenttag.content_id = ?");

		dsObjVersion = new TableVersion(false);
		dsObjVersion.setHandle(dbHandle);
		dsObjVersion.setTable("ds_obj");
		dsObjVersion.setJoin("contenttag", "id", "contenttag_id");
		dsObjVersion.setWherePart("contenttag.content_id = ?");

	}

	/**
	 * Check number of versioned entries. The expected entries may be given in the Map, if not given (or Map is null), 1 versioned entry is expected
	 * 
	 * @param tableVersion
	 *            table version
	 * @param id
	 *            recordset id
	 * @param expectedEntries
	 *            map of expected entries (may be null).
	 * @throws Exception
	 */
	protected void checkNumberOfVersionedEntries(final TableVersion tableVersion, Long id, final Map<Long, Integer> expectedEntries) throws Exception {
		StringBuffer sql = new StringBuffer("SELECT gentics_main.id, count(*) c FROM ");

		sql.append(tableVersion.getTable());
		sql.append("_nodeversion gentics_main");

		List<Join> joins = tableVersion.getJoins();
		for (Join join : joins) {
			sql.append(" ").append(join.getLeftJoin());
		}
		sql.append(" WHERE ").append(tableVersion.getWherePart()).append(" GROUP BY gentics_main.id");

		DB.query(dbHandle, sql.toString(), new Object[] { id }, new ResultProcessor() {

			/*
			 * (non-Javadoc)
			 * 
			 * @see com.gentics.lib.db.ResultProcessor#process(java.sql.ResultSet )
			 */
			public void process(ResultSet rs) throws SQLException {
				while (rs.next()) {
					int expectedCount = expectedEntries != null ? ObjectTransformer.getInt(
							expectedEntries.get(ObjectTransformer.getLong(rs.getObject("id"), null)), 1) : 1;

					assertEquals("Check number of versioned records for id {" + rs.getObject("id") + "} in table {" + tableVersion.getTable() + "}",
							expectedCount, rs.getInt("c"));
				}
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see com.gentics.lib.db.ResultProcessor#takeOver(com.gentics .lib.db.ResultProcessor)
			 */
			public void takeOver(ResultProcessor p) {
			}
		});
	}

	/**
	 * Compare the current version of the page (including contenttags, values, ds' and dsObjs) with the given versioned data
	 * 
	 * @param pageId
	 *            page id
	 * @param contentId
	 *            content id
	 * @param timestamp
	 *            timestamp
	 * @throws Exception
	 */
	protected void compareLatestPageVersionWithCurrent(Long pageId, Long contentId, int timestamp) throws Exception {
		compareLatestVersionWithCurrent(pageVersion, pageId, timestamp);
		compareLatestVersionWithCurrent(contenttagVersion, contentId, timestamp);
		compareLatestVersionWithCurrent(valueVersion, contentId, timestamp);
		compareLatestVersionWithCurrent(dsVersion, contentId, timestamp);
		compareLatestVersionWithCurrent(dsObjVersion, contentId, timestamp);
	}

	/**
	 * Compare the current version data with the version at the given timesamp.
	 * 
	 * @param tableVersion
	 *            table version
	 * @param id
	 *            data id
	 * @param timestamp
	 *            timestamp
	 * @throws Exception
	 */
	protected void compareLatestVersionWithCurrent(TableVersion tableVersion, Long id, int timestamp) throws Exception {
		// get the versioned data
		SimpleResultProcessor versionData = tableVersion.getVersionData(new Object[] { id }, timestamp, true, false, true);

		// get the current data
		SimpleResultProcessor currentData = tableVersion.getVersionData(new Object[] { id }, -1, true, false, true);

		// now compare the results
		assertTrue("Check whether current and version data contain the same number of records", currentData.size() == versionData.size());

		int size = currentData.size();
		List columnNames = tableVersion.getVersionedColumns(tableVersion.getTable());

		// now compare each record
		for (int i = 1; i <= size; i++) {
			SimpleResultRow currentRow = currentData.getRow(i);
			SimpleResultRow versionRow = versionData.getRow(i);

			for (Iterator it = columnNames.iterator(); it.hasNext();) {
				String name = (String) it.next();

				assertEquals("Check data for column {" + name + "} of table {" + tableVersion.getTable() + "}", currentRow.getObject(name),
						versionRow.getObject(name));
			}
		}
	}

	/**
	 * Create a new version of the page (including contenttags, values, ds' and dsObjs)
	 * 
	 * @param pageId
	 *            page id
	 * @param contentId
	 *            content id
	 * @param timestamp
	 *            timestamp of the new version
	 * @param userId
	 *            user id
	 * @throws Exception
	 */
	protected void createPageVersion(Long pageId, Long contentId, int timestamp, String userId) throws Exception {
		pageVersion.createVersion2(pageId, timestamp, userId);
		contenttagVersion.createVersion2(contentId, timestamp, userId);
		valueVersion.createVersion2(contentId, timestamp, userId);
		dsVersion.createVersion2(contentId, timestamp, userId);
		dsObjVersion.createVersion2(contentId, timestamp, userId);

		compareLatestPageVersionWithCurrent(pageId, contentId, timestamp);
	}

	/**
	 * Restore a version of the page (including contenttags, values, ds' and dsObjs)
	 * 
	 * @param pageId
	 *            page id
	 * @param contentId
	 *            content id
	 * @param timestamp
	 *            timestamp of the version to be restored
	 * @throws Exception
	 */
	protected void restorePageVersion(Long pageId, Long contentId, int timestamp) throws Exception {
		pageVersion.restoreVersion(pageId, timestamp);
		contenttagVersion.restoreVersion(contentId, timestamp);
		valueVersion.restoreVersion(contentId, timestamp);
		dsVersion.restoreVersion(contentId, timestamp);
		dsObjVersion.restoreVersion(contentId, timestamp);
	}

	/**
	 * UpdateProcessor implementation that collects a single insert id
	 */
	public static class InsertIdUpdateProcessor implements UpdateProcessor {

		/**
		 * insert id
		 */
		protected int insertId = -1;

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.gentics.lib.db.UpdateProcessor#process(java.sql.Statement)
		 */
		public void process(Statement stmt) throws SQLException {
			ResultSet keys = stmt.getGeneratedKeys();

			if (keys.next()) {
				insertId = keys.getInt(1);
			}
		}

		/**
		 * Reset this update processor
		 */
		public void reset() {
			insertId = -1;
		}

		/**
		 * Get the insert id
		 * 
		 * @return the insertId
		 */
		public int getInsertId() {
			return insertId;
		}
	}
}
