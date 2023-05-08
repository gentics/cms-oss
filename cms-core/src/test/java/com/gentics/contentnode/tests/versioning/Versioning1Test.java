package com.gentics.contentnode.tests.versioning;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.gentics.lib.db.DB;
import com.gentics.lib.db.SimpleResultProcessor;

public class Versioning1Test extends AbstractVersioningTest {

	/**
	 * Test creation of a single version
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCreateVersion() throws Exception {
		int timestamp = 1;
		String userId = "1";

		createPageVersion(PAGE_ID, CONTENT_ID, timestamp, userId);

		assertEquals("Check whether the version was generated in table {page}", 1, pageVersion.getVersions(PAGE_ID).length);
		assertEquals("Check whether the version was generated in table {contenttag}", 1, contenttagVersion.getVersions(CONTENT_ID).length);
		assertEquals("Check whether the version was generated in table {value}", 1, valueVersion.getVersions(CONTENT_ID).length);
		assertEquals("Check whether the version was generated in table {ds}", 1, dsVersion.getVersions(CONTENT_ID).length);
		assertEquals("Check whether the version was generated in table {ds_obj}", 1, dsObjVersion.getVersions(CONTENT_ID).length);
	}

	/**
	 * Test creation of a new version
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCreateNewVersion() throws Exception {
		// create the initial version
		int initialTimestamp = 1;
		int newTimestamp = 2;
		String userId = "1";

		// create the initial version
		createPageVersion(PAGE_ID, CONTENT_ID, initialTimestamp, userId);

		// modify data (page.name and one value.value_text)
		DB.update(dbHandle, "UPDATE page SET name = ? WHERE id = ?", new Object[] { "This is the modified page", PAGE_ID });
		// get one value
		SimpleResultProcessor currentValues = valueVersion.getVersionData(new Object[] { CONTENT_ID }, -1, true, false, true);
		Long valueId = currentValues.getRow(1).getLong("id");

		// modify this value
		DB.update(dbHandle, "UPDATE value SET value_text = ? WHERE id = ?", new Object[] { "This is the modified data", valueId });

		// create a second version
		createPageVersion(PAGE_ID, CONTENT_ID, newTimestamp, userId);

		// check whether only the modified data were versioned again
		Map<Long, Integer> versionEntries = new HashMap<Long, Integer>();

		versionEntries.put(PAGE_ID, 2);
		checkNumberOfVersionedEntries(pageVersion, PAGE_ID, versionEntries);
		checkNumberOfVersionedEntries(contenttagVersion, CONTENT_ID, null);
		versionEntries.clear();
		versionEntries.put(valueId, 2);
		checkNumberOfVersionedEntries(valueVersion, CONTENT_ID, versionEntries);
		checkNumberOfVersionedEntries(dsVersion, CONTENT_ID, null);
		checkNumberOfVersionedEntries(dsObjVersion, CONTENT_ID, null);
	}

	/**
	 * Test restoring of a version
	 * 
	 * @throws Exception
	 */
	@Test
	public void testRestoreVersion() throws Exception {
		int initialTimestamp = 1;
		int newTimestamp = 2;
		String userId = "1";

		// create the initial version
		createPageVersion(PAGE_ID, CONTENT_ID, initialTimestamp, userId);

		// modify data (page.name and one value.value_text)
		DB.update(dbHandle, "UPDATE page SET name = ? WHERE id = ?", new Object[] { "This is the modified page", PAGE_ID });
		// get one value
		SimpleResultProcessor currentValues = valueVersion.getVersionData(new Object[] { CONTENT_ID }, -1, true, false, true);
		Long valueId = currentValues.getRow(1).getLong("id");

		// modify this value
		DB.update(dbHandle, "UPDATE value SET value_text = ? WHERE id = ?", new Object[] { "This is the modified data", valueId });

		// create a second version
		createPageVersion(PAGE_ID, CONTENT_ID, newTimestamp, userId);

		// now restore the first version
		restorePageVersion(PAGE_ID, CONTENT_ID, initialTimestamp);

		// check whether the new versioned data is the current data
		compareLatestPageVersionWithCurrent(PAGE_ID, CONTENT_ID, initialTimestamp);
	}

	/**
	 * Test versioning of removed records
	 * 
	 * @throws Exception
	 */
	@Test
	public void testVersionWithRemovedRecords() throws Exception {
		int initialTimestamp = 1;
		int newTimestamp = 2;
		int restoredTimestamp = 3;
		int thirdTimestamp = 4;
		String userId = "1";

		// create the initial version
		createPageVersion(PAGE_ID, CONTENT_ID, initialTimestamp, userId);

		// now remove a contenttag together with it's value
		// get one contenttag
		SimpleResultProcessor contentTagRS = contenttagVersion.getVersionData(new Object[] { CONTENT_ID }, -1, true, false, true);
		Long contentTagId = contentTagRS.getRow(1).getLong("id");

		// get the value
		SimpleResultProcessor valueRS = new SimpleResultProcessor();

		DB.query(dbHandle, "SELECT id FROM value WHERE contenttag_id = ?", new Object[] { contentTagId }, valueRS);
		assertEquals("Check whether we found the value", 1, valueRS.size());
		Long valueId = valueRS.getRow(1).getLong("id");

		// now remove the contenttag and the value
		DB.update(dbHandle, "DELETE FROM contenttag WHERE id = ?", new Object[] { contentTagId });
		DB.update(dbHandle, "DELETE FROM value WHERE id = ?", new Object[] { valueId });

		// create a new version
		createPageVersion(PAGE_ID, CONTENT_ID, newTimestamp, userId);

		// restore the initial version and make a new version
		restorePageVersion(PAGE_ID, CONTENT_ID, initialTimestamp);
		createPageVersion(PAGE_ID, CONTENT_ID, restoredTimestamp, userId);

		// now modify the formerly removed value
		int modifiedValues = DB.update(dbHandle, "UPDATE value SET value_text = ? WHERE id = ?",
				new Object[] { "This is the modified value", valueId });

		assertEquals("Check that the value was actually modified", 1, modifiedValues);

		// create yet another versin
		createPageVersion(PAGE_ID, CONTENT_ID, thirdTimestamp, userId);
	}

}
