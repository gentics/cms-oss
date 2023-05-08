package com.gentics.contentnode.tests.versioning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;

import com.gentics.api.lib.datasource.VersioningDatasource.Version;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.lib.db.DB;
import com.gentics.lib.db.ResultProcessor;
import com.gentics.lib.db.SimpleResultProcessor;
import com.gentics.lib.db.TableVersion.Diff;
import com.gentics.lib.etc.StringUtils;

public class Versioning2Test extends AbstractVersioningTest {

	/**
	 * Test diffing of versions
	 *
	 * @throws Exception
	 */
	@Test
	public void testDiff() throws Exception {
		int initialTimestamp = 1;
		int secondTimestamp = 2;
		String userId = "1";
		int constructId = 14;
		String tagName = "html1";

		// create the initial verson
		createPageVersion(PAGE_ID, CONTENT_ID, initialTimestamp, userId);

		// modify the page name
		DB.update(dbHandle, "UPDATE page SET name = ? WHERE id = ?", new Object[] { "This is the modified page", PAGE_ID });

		// remove a tag
		// now remove a contenttag together with it's value
		// get one contenttag
		SimpleResultProcessor contentTagRS = contenttagVersion.getVersionData(new Object[] { CONTENT_ID }, -1, true, false, true);
		Long contentTagId = contentTagRS.getRow(1).getLong("id");

		DB.update(dbHandle, "DELETE FROM contenttag WHERE id = ?", new Object[] { contentTagId });

		// add a tag
		DB.update(dbHandle, "INSERT INTO contenttag (content_id,construct_id,enabled,name,unused) VALUES (?,?,?,?,?)", new Object[] { CONTENT_ID,
				constructId, 1, tagName, 0 }, null);

		// create a new version
		createPageVersion(PAGE_ID, CONTENT_ID, secondTimestamp, userId);

		// check pageversion diff (one modified record)
		List diff = pageVersion.getDiff(new Object[] { PAGE_ID }, initialTimestamp, secondTimestamp);

		assertEquals("Check number of different records for table page", 1, diff.size());

		Diff d1 = (Diff) diff.get(0);

		assertEquals("Check difftype for table page", Diff.DIFFTYPE_MOD, d1.getDiffType());
		assertNotNull("Check detection of different columns", d1.getModColumns());
		assertEquals("Check detection of different columns", "name", StringUtils.merge(d1.getModColumns(), ","));

		// check contenttag diff (one added and one removed record)
		diff = contenttagVersion.getDiff(new Object[] { CONTENT_ID }, initialTimestamp, secondTimestamp);
		assertEquals("Check number of different records for table contenttag", 2, diff.size());

		boolean delFound = false;
		boolean addFound = false;

		for (Iterator iterator = diff.iterator(); iterator.hasNext();) {
			Diff d = (Diff) iterator.next();

			switch (d.getDiffType()) {
			case Diff.DIFFTYPE_ADD:
				addFound = true;
				break;

			case Diff.DIFFTYPE_DEL:
				delFound = true;
				break;

			default:
				fail("Found unexpected difftype for table contenttag");
			}
		}

		assertTrue("Check whether added record was found as diff", addFound);
		assertTrue("Check whether deleted record was found as diff", delFound);

		// check value diff (which has no diff)
		diff = valueVersion.getDiff(new Object[] { CONTENT_ID }, initialTimestamp, secondTimestamp);
		assertEquals("Check whether no diff was found for table value", 0, diff.size());
	}

	/**
	 * Test versioning of added records
	 *
	 * @throws Exception
	 */
	@Test
	public void testVersionWithAddedRecords() throws Exception {
		int initialTimestamp = 1;
		int newTimestamp = 2;
		String userId = "1";
		int constructId = 14;
		String tagName = "html1";
		String tagContent = "This is the created Tag";

		// create the initial version
		createPageVersion(PAGE_ID, CONTENT_ID, initialTimestamp, userId);

		List<Integer> keys = DBUtils.executeInsert("INSERT INTO contenttag (content_id,construct_id,enabled,name,unused) VALUES (?,?,?,?,?)", new Object[] {
				CONTENT_ID, constructId, 1, tagName, 0 });
		assertThat(keys).as("Insert IDs for contenttag").isNotEmpty();
		int contentTagId = keys.get(0);

		keys = DBUtils.executeInsert(
				"INSERT INTO `value` (part_id,info,static,templatetag_id,contenttag_id,globaltag_id,objtag_id,value_text,value_ref) VALUES (44,0,0,0,?,0,0,?,0)",
				new Object[] {contentTagId, tagContent});
		assertThat(keys).as("Insert IDs for value").isNotEmpty();

		// create a page version
		createPageVersion(PAGE_ID, CONTENT_ID, newTimestamp, userId);

		// now restore the first version
		restorePageVersion(PAGE_ID, CONTENT_ID, initialTimestamp);

		// check whether the contenttag vanished
		DB.query(dbHandle, "SELECT count(*) c FROM contenttag WHERE id = ?", new Object[] { contentTagId }, new ResultProcessor() {

			/*
			 * (non-Javadoc)
			 *
			 * @see com.gentics.lib.db.ResultProcessor#process(java.sql.ResultSet )
			 */
			public void process(ResultSet rs) throws SQLException {
				if (rs.next()) {
					assertEquals("Check # of contenttag records", 0, rs.getInt("c"));
				} else {
					fail("Could not check # of contenttag records");
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
		// // check whether the value vanished TODO this is currently not
		// checked, because the current implementation CANNOT remove the value
		// when restoring, since the contenttag is removed first
		// and the connection between the value and the content is done via the
		// contenttag.
		// DB.query(dbHandle, "SELECT count(*) c FROM value WHERE id = ?", new
		// Object[] {valueId}, new ResultProcessor() {
		// /* (non-Javadoc)
		// * @see com.gentics.lib.db.ResultProcessor#process(java.sql.ResultSet)
		// */
		// public void process(ResultSet rs) throws SQLException {
		// if (rs.next()) {
		// assertEquals("Check # of value records", 0, rs
		// .getInt("c"));
		// } else {
		// fail("Could not check # of value records");
		// }
		// }
		//
		// /* (non-Javadoc)
		// * @see
		// com.gentics.lib.db.ResultProcessor#takeOver(com.gentics.lib.db.ResultProcessor)
		// */
		// public void takeOver(ResultProcessor p) {
		// }
		// });

		// restore the second version
		restorePageVersion(PAGE_ID, CONTENT_ID, newTimestamp);

		// check whether contenttag and value reappeared
		DB.query(dbHandle, "SELECT count(*) c FROM contenttag WHERE id = ?", new Object[] { contentTagId }, new ResultProcessor() {

			/*
			 * (non-Javadoc)
			 *
			 * @see com.gentics.lib.db.ResultProcessor#process(java.sql.ResultSet )
			 */
			public void process(ResultSet rs) throws SQLException {
				if (rs.next()) {
					assertEquals("Check # of contenttag records", 1, rs.getInt("c"));
				} else {
					fail("Could not check # of contenttag records");
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
		// NOTE: this is currently not tested, since the current implementation
		// of TableVersion does not garantuee that values will be correctly be
		// removed when restoring versions!
		// The reason for this is that values are identified by joining with
		// contenttag and restricting via contenttag.content_id, but the
		// contenttag value has already been removed before
		// DB.query(dbHandle, "SELECT count(*) c FROM value WHERE id = ?", new
		// Object[] {valueId}, new ResultProcessor() {
		// /* (non-Javadoc)
		// * @see com.gentics.lib.db.ResultProcessor#process(java.sql.ResultSet)
		// */
		// public void process(ResultSet rs) throws SQLException {
		// if (rs.next()) {
		// assertEquals("Check # of value records", 1, rs
		// .getInt("c"));
		// } else {
		// fail("Could not check # of value records");
		// }
		// }
		//
		// /* (non-Javadoc)
		// * @see
		// com.gentics.lib.db.ResultProcessor#takeOver(com.gentics.lib.db.ResultProcessor)
		// */
		// public void takeOver(ResultProcessor p) {
		// }
		// });
	}

	/**
	 * Test fetching of versioned data with timestamps between versions (should fetch the older version)
	 *
	 * @throws Exception
	 */
	@Test
	public void testBetweenVersions() throws Exception {
		int initialTimestamp = 1;
		int betweenTimestamps = 2;
		int secondTimestamp = 3;
		int thirdTimestamp = 4;
		String userId = "1";
		int constructId = 14;
		String tagName = "html1";

		// create the initial verson
		createPageVersion(PAGE_ID, CONTENT_ID, initialTimestamp, userId);

		// modify the page name
		DB.update(dbHandle, "UPDATE page SET name = ? WHERE id = ?", new Object[] { "This is the modified page", PAGE_ID });

		// remove a tag
		// now remove a contenttag together with it's value
		// get one contenttag
		SimpleResultProcessor contentTagRS = contenttagVersion.getVersionData(new Object[] { CONTENT_ID }, -1, true, false, true);
		Long contentTagId = contentTagRS.getRow(1).getLong("id");

		DB.update(dbHandle, "DELETE FROM contenttag WHERE id = ?", new Object[] { contentTagId });

		// add a tag
		DB.update(dbHandle, "INSERT INTO contenttag (content_id,construct_id,enabled,name,unused) VALUES (?,?,?,?,?)", new Object[] { CONTENT_ID,
				constructId, 1, tagName, 0 }, null);

		// create a new version
		createPageVersion(PAGE_ID, CONTENT_ID, secondTimestamp, userId);

		// restore initial version and make new version
		restorePageVersion(PAGE_ID, CONTENT_ID, initialTimestamp);
		createPageVersion(PAGE_ID, CONTENT_ID, thirdTimestamp, userId);

		// compare the current version with timestamp between initial and second
		// version
		compareLatestPageVersionWithCurrent(PAGE_ID, CONTENT_ID, betweenTimestamps);
	}

	/**
	 * Test user for versions
	 *
	 * @throws Exception
	 */
	@Test
	public void testVersionUser() throws Exception {
		int initialTimestamp = 1;
		int secondTimestamp = 2;
		String initialUserId = "1";
		String secondUserId = "2";

		// create initial version
		createPageVersion(PAGE_ID, CONTENT_ID, initialTimestamp, initialUserId);

		// modify the page name
		DB.update(dbHandle, "UPDATE page SET name = ? WHERE id = ?", new Object[] { "This is the modified page", PAGE_ID });

		createPageVersion(PAGE_ID, CONTENT_ID, secondTimestamp, secondUserId);

		// now get the versions for the page table
		Version[] versions = pageVersion.getVersions(PAGE_ID);

		assertEquals("Check # of versions", 2, versions.length);

		// check the version data
		assertEquals("Check timestamp of first version", initialTimestamp, versions[0].getTimestamp());
		assertEquals("Check userId of first version", initialUserId, versions[0].getUser());
		assertEquals("Check # of different records of first version", 1, versions[0].getDiffCount());
		assertEquals("Check timestamp of second version", secondTimestamp, versions[1].getTimestamp());
		assertEquals("Check userId of second version", secondUserId, versions[1].getUser());
		assertEquals("Check # of different records of second version", 1, versions[1].getDiffCount());
	}

}
