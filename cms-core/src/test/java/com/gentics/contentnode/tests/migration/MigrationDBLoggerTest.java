package com.gentics.contentnode.tests.migration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;

import com.gentics.contentnode.factory.TransactionException;
import com.gentics.contentnode.migration.MigrationDBLogger;
import com.gentics.contentnode.migration.jobs.AbstractMigrationJob;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.rest.model.migration.TagTypeMigrationMapping;
import com.gentics.contentnode.rest.model.response.migration.MigrationJobEntry;
import com.gentics.contentnode.rest.model.response.migration.MigrationJobLogEntryItem;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.lib.log.NodeLogger;
import com.gentics.testutils.database.SQLUtils;

/**
 * Tests creating and reading of migration job log db data
 * @author johannes2
 *
 */
public class MigrationDBLoggerTest {
	
	@Rule
	public DBTestContext testContext = new DBTestContext();

	final int JOB_ID = 100;
	final int JOB_TYPE = 9;
	final int JOB_STATUS = AbstractMigrationJob.STATUS_STARTED;
	final int JOB_STATUS_NEW = AbstractMigrationJob.STATUS_COMPLETED;

	final int OBJ_ID = 10;
	final int OBJ_TYPE = Page.TYPE_PAGE;
	final int OBJ_STATUS = AbstractMigrationJob.STATUS_PENDING;
	final int OBJ_STATUS_NEW = AbstractMigrationJob.STATUS_COMPLETED;

	List<TagTypeMigrationMapping> mappings = new ArrayList<TagTypeMigrationMapping>();

	private void checkJob() throws TransactionException, SQLException {

		SQLUtils utils = testContext.getDBSQLUtils();
		ResultSet rs = utils.executeQuery("SELECT * FROM migrationjob");

		if (rs.next()) {
			assertEquals("The job id did not match the expected one.", JOB_ID, rs.getInt("job_id"));
			assertEquals("The job type did not match the expected one.", JOB_TYPE, rs.getInt("job_type"));
		} else {
			fail("No record was inserted into the migationjob table");
		}
	}

	private void checkJobItem() throws TransactionException, SQLException {

		SQLUtils utils = testContext.getDBSQLUtils();
		ResultSet rs = utils.executeQuery("SELECT * FROM migrationjob_item");

		if (rs.next()) {
			assertEquals("The job id did not match the expected one.", JOB_ID, rs.getInt("job_id"));
			assertEquals("The obj id did not match the expected one.", OBJ_ID, rs.getInt("obj_id"));
			assertEquals("The obj type did not match the expected one.", OBJ_TYPE, rs.getInt("obj_type"));
			assertEquals("The obj status did not match the expected one.", OBJ_STATUS, rs.getInt("status"));
		} else {
			fail("No record was inserted into the migationjob table");
		}

	}

	@Test
	public void testJobItemEntryTable() throws Exception {

		MigrationDBLogger dblogger = new MigrationDBLogger(MigrationDBLogger.DEFAULT_LOGGER);

		dblogger.createMigrationJobEntry(JOB_ID, JOB_TYPE, mappings);
		checkJob();

		dblogger.createMigrationJobItemEntry(JOB_ID, OBJ_ID, OBJ_TYPE, OBJ_STATUS);
		checkJobItem();

		List<MigrationJobLogEntryItem> items = dblogger.getMigrationJobItemEntries(JOB_ID);

		assertTrue("There should be exactly one job item entry.", items.size() == 1);

		MigrationJobEntry job = dblogger.getMigrationJobEntry(JOB_ID);

		assertEquals(JOB_STATUS, job.getStatus());
		assertEquals(JOB_ID, job.getJobId());
		assertEquals(JOB_TYPE, job.getJobType());
		assertEquals("Check # of items in job", 1L, job.getHandledObjects());

		List<MigrationJobEntry> jobEntries = dblogger.getMigrationJobEntries();

		assertTrue("There should be exactly one job entry.", jobEntries.size() == 1);
		MigrationJobEntry jobEntry = jobEntries.get(0);

		assertEquals(JOB_STATUS, jobEntry.getStatus());
		assertEquals("Check # of items in job", 1L, jobEntry.getHandledObjects());

		// Update the job
		dblogger.updateMigrationJobEntryStatus(JOB_ID, JOB_STATUS_NEW);
		
		// Load the job and check the new status
		MigrationJobEntry updatedEntry = dblogger.getMigrationJobEntry(JOB_ID);

		assertEquals("The job status should be updated", JOB_STATUS_NEW, updatedEntry.getStatus());

		// Load the items and check it
		List<MigrationJobLogEntryItem> jobItems = dblogger.getMigrationJobItemEntries(JOB_ID);

		assertTrue("There should be exactly one job item entry.", items.size() == 1);
		MigrationJobLogEntryItem jobItem = jobItems.get(0);

		assertNotNull(jobItem);
		assertEquals(OBJ_STATUS, jobItem.getStatus());
		assertEquals(OBJ_ID, jobItem.getObjectId());
		assertEquals(OBJ_TYPE, jobItem.getObjectType());

		// Update a job item
		dblogger.updateMigrationJobItemEntry(JOB_ID, OBJ_ID, OBJ_TYPE, OBJ_STATUS_NEW);

		// Load the items and check for update
		List<MigrationJobLogEntryItem> updatedItems = dblogger.getMigrationJobItemEntries(JOB_ID);

		assertTrue("There should be exactly one job item entry.", items.size() == 1);
		MigrationJobLogEntryItem updatedItem = updatedItems.get(0);

		assertNotNull(updatedItem);
		assertEquals(OBJ_STATUS_NEW, updatedItem.getStatus());
		assertEquals(OBJ_ID, updatedItem.getObjectId());
		assertEquals(OBJ_TYPE, updatedItem.getObjectType());

	}
}
