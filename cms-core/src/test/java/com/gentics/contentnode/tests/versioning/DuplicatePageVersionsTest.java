package com.gentics.contentnode.tests.versioning;

import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createPage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.parttype.ShortTextPartType;
import com.gentics.contentnode.publish.PublishQueue;
import com.gentics.contentnode.scheduler.CoreInternalSchedulerTask;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for the internal scheduler tasks, which check and repair duplicate page versions (SUP-20172).
 * The duplicates are created with SQL, like they were created by concurrent creation of page versions.
 * The transaction timestamps are set explicitly (relative to {@link #base}).
 */
public class DuplicatePageVersionsTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	private static Node node;

	private static Template template;

	private static Integer constructId;

	/**
	 * Base timestamp (in seconds) for the transactions of a test
	 */
	private int base;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		node = Trx.supply(() -> createNode());
		template = Trx.supply(() -> createTemplate(node.getFolder(), "Template"));
		constructId = Trx.supply(() -> createConstruct(node, ShortTextPartType.class, "link", "url"));
	}

	@Before
	public void setup() throws NodeException {
		base = (int) (System.currentTimeMillis() / 1000) + 3600;

		// repair duplicates left over by other tests, so that every test starts without duplicates
		CoreInternalSchedulerTask.repairduplicatepageversions.execute(new ArrayList<>());
		Trx.operate(() -> DBUtils.update("DELETE FROM publishqueue"));
	}

	/**
	 * Test that the filled row is kept, when the empty row has the lower auto_id
	 * @throws NodeException
	 */
	@Test
	public void testEmptyRowWithLowerAutoId() throws NodeException {
		Page page = createPageAt(base);
		String tagName = addTagAndPublishAt(page, base + 10, "first");
		int valueId = getValueId(page, tagName);

		int duplicate = duplicateVersionedRow("value_nodeversion", valueId, base + 10, Map.of("value_text", "first"));
		Trx.operate(() -> DBUtils.update("UPDATE value_nodeversion SET value_text = ? WHERE id = ? AND nodeversiontimestamp = ? AND auto_id != ?", "",
				valueId, base + 10, duplicate));
		// load the version into the cache
		loadValue(page, base + 10, tagName);

		assertThat(repair()).as("Repair result").isTrue();

		assertThat(getRows("value_nodeversion", valueId, base + 10)).as("Rows of the value").extracting(VersionRow::autoId).containsExactly(duplicate);
		assertValue(page, base + 10, tagName, "first");
	}

	/**
	 * Test that the filled row is kept, when the empty row has the higher auto_id
	 * @throws NodeException
	 */
	@Test
	public void testEmptyRowWithHigherAutoId() throws NodeException {
		Page page = createPageAt(base);
		String tagName = addTagAndPublishAt(page, base + 10, "first");
		int valueId = getValueId(page, tagName);
		int original = getRows("value_nodeversion", valueId, base + 10).get(0).autoId();

		duplicateVersionedRow("value_nodeversion", valueId, base + 10, Map.of("value_text", ""));
		// load the version into the cache
		loadValue(page, base + 10, tagName);

		assertThat(repair()).as("Repair result").isTrue();

		assertThat(getRows("value_nodeversion", valueId, base + 10)).as("Rows of the value").extracting(VersionRow::autoId).containsExactly(original);
		assertValue(page, base + 10, tagName, "first");
	}

	/**
	 * Test that the row with the highest auto_id is kept, when all rows are empty
	 * @throws NodeException
	 */
	@Test
	public void testAllRowsEmpty() throws NodeException {
		Page page = createPageAt(base);
		String tagName = addTagAndPublishAt(page, base + 10, "");
		int valueId = getValueId(page, tagName);

		int duplicate = duplicateVersionedRow("value_nodeversion", valueId, base + 10, Collections.emptyMap());

		assertThat(repair()).as("Repair result").isTrue();

		assertThat(getRows("value_nodeversion", valueId, base + 10)).as("Rows of the value").extracting(VersionRow::autoId).containsExactly(duplicate);
	}

	/**
	 * Test that the row with the highest auto_id is kept for duplicate rows of contenttags
	 * @throws NodeException
	 */
	@Test
	public void testDuplicateContentTag() throws NodeException {
		Page page = createPageAt(base);
		String tagName = addTagAndPublishAt(page, base + 10, "first");
		int tagId = Trx.supply(t -> t.getObject(page).getContentTag(tagName).getId());

		int duplicate = duplicateVersionedRow("contenttag_nodeversion", tagId, base + 10, Collections.emptyMap());

		assertThat(repair()).as("Repair result").isTrue();

		assertThat(getRows("contenttag_nodeversion", tagId, base + 10)).as("Rows of the tag").extracting(VersionRow::autoId)
				.containsExactly(duplicate);
		assertValue(page, base + 10, tagName, "first");
	}

	/**
	 * Test that the kept row is marked as latest, when another row of the group was marked as latest
	 * @throws NodeException
	 */
	@Test
	public void testLatestFlag() throws NodeException {
		Page page = createPageAt(base);
		String tagName = addTagAndPublishAt(page, base + 10, "first");
		int valueId = getValueId(page, tagName);
		assertThat(getRows("value_nodeversion", valueId, base + 10)).as("Rows of the value").extracting(VersionRow::latest).containsExactly(1);

		int duplicate = duplicateVersionedRow("value_nodeversion", valueId, base + 10, Map.of("nodeversionlatest", 0));

		assertThat(repair()).as("Repair result").isTrue();

		assertThat(getRows("value_nodeversion", valueId, base + 10)).as("Rows of the value")
				.containsExactly(new VersionRow(duplicate, "first", 1, 0));
	}

	/**
	 * Test that the same rule is used for groups, whose rows differ in nodeversionremoved
	 * @throws NodeException
	 */
	@Test
	public void testRemovedDiffers() throws NodeException {
		Page page = createPageAt(base);
		String tagName = addTagAndPublishAt(page, base + 10, "first");
		int valueId = getValueId(page, tagName);

		int duplicate = duplicateVersionedRow("value_nodeversion", valueId, base + 10, Map.of("nodeversionremoved", base + 10));

		assertThat(repair()).as("Repair result").isTrue();

		assertThat(getRows("value_nodeversion", valueId, base + 10)).as("Rows of the value").extracting(VersionRow::autoId, VersionRow::removed)
				.containsExactly(tuple(duplicate, base + 10));
	}

	/**
	 * Test that the published nodeversion entry is kept, and the planned publishing referencing the deleted entry is moved to the kept entry
	 * @throws NodeException
	 */
	@Test
	public void testDuplicateNodeversion() throws NodeException {
		Page page = createPageAt(base);
		addTagAndPublishAt(page, base + 10, "first");
		int published = getVersionIds(page, base + 10).get(0);

		int duplicate = insertVersion(page, base + 10, "1.0", false);
		Trx.operate(() -> DBUtils.update("UPDATE page SET time_pub_version = ? WHERE id = ?", duplicate, page.getId()));

		assertThat(repair()).as("Repair result").isTrue();

		assertThat(getVersionIds(page, base + 10)).as("Versions @" + (base + 10)).containsExactly(published);
		assertThat(Trx.supply(() -> DBUtils.select("SELECT time_pub_version FROM page WHERE id = ?", ps -> ps.setInt(1, page.getId()),
				DBUtils.firstInt("time_pub_version")))).as("Planned publishing version").isEqualTo(published);
		assertVersions(page, version("1.0", base + 10, true), version("0.1", base, false));
	}

	/**
	 * Test that only the newest published version stays published
	 * @throws NodeException
	 */
	@Test
	public void testMultiplePublished() throws NodeException {
		Page page = createPageAt(base);
		String tagName = addTagAndPublishAt(page, base + 10, "first");
		setValueAndPublishAt(page, base + 20, tagName, "second");
		int first = getVersionIds(page, base + 10).get(0);
		Trx.operate(() -> DBUtils.update("UPDATE nodeversion SET published = 1 WHERE id = ?", first));

		assertThat(repair()).as("Repair result").isTrue();

		assertVersions(page, version("2.0", base + 20, true), version("1.0", base + 10, false), version("0.1", base, false));
	}

	/**
	 * Test that versions with a number, which is not greater than the number of the previous version, are renumbered (and all later versions)
	 * @throws NodeException
	 */
	@Test
	public void testRenumber() throws NodeException {
		Page page = createPageAt(base);
		String tagName = addTagAndPublishAt(page, base + 10, "first");
		setValueAndSaveAt(page, base + 20, tagName, "second");
		setValueAndSaveAt(page, base + 30, tagName, "third");
		assertVersions(page, version("1.2", base + 30, false), version("1.1", base + 20, false), version("1.0", base + 10, true),
				version("0.1", base, false));

		int second = getVersionIds(page, base + 20).get(0);
		Trx.operate(() -> DBUtils.update("UPDATE nodeversion SET nodeversion = ? WHERE id = ?", "1.0", second));

		assertThat(repair()).as("Repair result").isTrue();

		assertVersions(page, version("2.1", base + 30, false), version("2.0", base + 20, false), version("1.0", base + 10, true),
				version("0.1", base, false));
	}

	/**
	 * Test that the repaired online page is dirted for republishing, but not the page without duplicates, and that a second run changes nothing
	 * @throws NodeException
	 */
	@Test
	public void testDirtAndRepeat() throws NodeException {
		Page page = createPageAt(base);
		String tagName = addTagAndPublishAt(page, base + 10, "first");
		Page otherPage = createPageAt(base);
		addTagAndPublishAt(otherPage, base + 10, "other");
		int valueId = getValueId(page, tagName);
		duplicateVersionedRow("value_nodeversion", valueId, base + 10, Map.of("value_text", ""));
		Trx.operate(() -> DBUtils.update("DELETE FROM publishqueue"));

		List<String> firstOut = new ArrayList<>();
		assertThat(CoreInternalSchedulerTask.repairduplicatepageversions.execute(firstOut)).as("Repair result").isTrue();
		assertThat(firstOut).as("Output of the repair").containsExactly("Repaired pages: 1");

		assertThat(Trx.supply(() -> PublishQueue.getDirtedObjectIds(Page.class, false, node))).as("Dirted pages").containsOnly(page.getId());

		List<String> out = new ArrayList<>();
		assertThat(CoreInternalSchedulerTask.repairduplicatepageversions.execute(out)).as("Result of the second repair").isTrue();
		assertThat(out).as("Output of the second repair").contains("Nothing to repair");
	}

	/**
	 * Test that the check outputs the number of affected pages and does not change anything
	 * @throws NodeException
	 */
	@Test
	public void testCheck() throws NodeException {
		Page page = createPageAt(base);
		String tagName = addTagAndPublishAt(page, base + 10, "first");
		int valueId = getValueId(page, tagName);
		duplicateVersionedRow("value_nodeversion", valueId, base + 10, Map.of("value_text", ""));
		insertVersion(page, base + 10, "1.0", false);
		List<VersionRow> rowsBefore = getRows("value_nodeversion", valueId, base + 10);
		List<Integer> versionsBefore = getVersionIds(page, base + 10);

		List<String> out = new ArrayList<>();
		assertThat(CoreInternalSchedulerTask.checkduplicatepageversions.execute(out)).as("Check result").isTrue();

		assertThat(out).as("Output of the check").containsExactly("Pages with duplicate versions: 1");

		assertThat(getRows("value_nodeversion", valueId, base + 10)).as("Rows of the value after the check").isEqualTo(rowsBefore);
		assertThat(getVersionIds(page, base + 10)).as("Versions after the check").isEqualTo(versionsBefore);
	}

	/**
	 * Run the repair task
	 * @return result of the task
	 * @throws NodeException
	 */
	protected boolean repair() throws NodeException {
		return CoreInternalSchedulerTask.repairduplicatepageversions.execute(new ArrayList<>());
	}

	/**
	 * Create a page with the given transaction timestamp
	 * @param timestamp timestamp in seconds
	 * @return page
	 * @throws NodeException
	 */
	protected Page createPageAt(int timestamp) throws NodeException {
		return Trx.supply(t -> {
			t.setTimestamp(timestamp * 1000L);
			return createPage(node.getFolder(), template, "Page");
		});
	}

	/**
	 * Add a tag with the given value to the page, save and publish it with the given transaction timestamp
	 * @param page page
	 * @param timestamp timestamp in seconds
	 * @param value tag value
	 * @return tag name
	 * @throws NodeException
	 */
	protected String addTagAndPublishAt(Page page, int timestamp, String value) throws NodeException {
		return Trx.supply(t -> {
			t.setTimestamp(timestamp * 1000L);
			Page editablePage = t.getObject(page, true);
			ContentTag tag = editablePage.getContent().addContentTag(constructId);
			tag.setEnabled(true);
			getPartType(ShortTextPartType.class, tag, "url").setText(value);
			editablePage.save();
			editablePage.publish();
			return tag.getName();
		});
	}

	/**
	 * Set the value of the tag and save the page with the given transaction timestamp
	 * @param page page
	 * @param timestamp timestamp in seconds
	 * @param tagName tag name
	 * @param value tag value
	 * @throws NodeException
	 */
	protected void setValueAndSaveAt(Page page, int timestamp, String tagName, String value) throws NodeException {
		Trx.operate(t -> {
			t.setTimestamp(timestamp * 1000L);
			Page editablePage = t.getObject(page, true);
			getPartType(ShortTextPartType.class, editablePage.getContentTag(tagName), "url").setText(value);
			editablePage.save();
		});
	}

	/**
	 * Set the value of the tag, save and publish the page with the given transaction timestamp
	 * @param page page
	 * @param timestamp timestamp in seconds
	 * @param tagName tag name
	 * @param value tag value
	 * @throws NodeException
	 */
	protected void setValueAndPublishAt(Page page, int timestamp, String tagName, String value) throws NodeException {
		Trx.operate(t -> {
			t.setTimestamp(timestamp * 1000L);
			Page editablePage = t.getObject(page, true);
			getPartType(ShortTextPartType.class, editablePage.getContentTag(tagName), "url").setText(value);
			editablePage.save();
			editablePage.publish();
		});
	}

	/**
	 * Get the id of the value "url" of the tag
	 * @param page page
	 * @param tagName tag name
	 * @return value id
	 * @throws NodeException
	 */
	protected int getValueId(Page page, String tagName) throws NodeException {
		return Trx.supply(t -> t.getObject(page).getContentTag(tagName).getValues().getByKeyname("url").getId());
	}

	/**
	 * Duplicate the row of the record in the versioned table (with a new auto_id), with optionally overwritten column values
	 * @param table versioned table
	 * @param id record id
	 * @param timestamp version timestamp
	 * @param overrides map of column names to the values in the duplicate row
	 * @return auto_id of the duplicate row
	 * @throws NodeException
	 */
	protected int duplicateVersionedRow(String table, int id, int timestamp, Map<String, Object> overrides) throws NodeException {
		return Trx.supply(() -> {
			List<String> columns = DBUtils.select("SELECT * FROM `" + table + "` WHERE 1 = 0", rs -> {
				List<String> names = new ArrayList<>();
				ResultSetMetaData metaData = rs.getMetaData();
				for (int i = 1; i <= metaData.getColumnCount(); i++) {
					if (!"auto_id".equals(metaData.getColumnName(i))) {
						names.add(metaData.getColumnName(i));
					}
				}
				return names;
			});
			List<Object> params = new ArrayList<>();
			for (String column : columns) {
				if (overrides.containsKey(column)) {
					params.add(overrides.get(column));
				}
			}
			params.add(id);
			params.add(timestamp);
			String selectColumns = columns.stream().map(column -> overrides.containsKey(column) ? "?" : "`" + column + "`")
					.collect(Collectors.joining(", "));
			String insertColumns = columns.stream().map(column -> "`" + column + "`").collect(Collectors.joining(", "));

			DBUtils.update(String.format("INSERT INTO `%s` (%s) SELECT %s FROM `%s` WHERE id = ? AND nodeversiontimestamp = ? ORDER BY auto_id LIMIT 1",
					table, insertColumns, selectColumns, table), params.toArray());

			return DBUtils.select("SELECT MAX(auto_id) auto_id FROM `" + table + "` WHERE id = ? AND nodeversiontimestamp = ?", ps -> {
				ps.setInt(1, id);
				ps.setInt(2, timestamp);
			}, DBUtils.firstInt("auto_id"));
		});
	}

	/**
	 * Insert a nodeversion entry for the page
	 * @param page page
	 * @param timestamp version timestamp
	 * @param number version number
	 * @param published published flag
	 * @return id of the nodeversion entry
	 * @throws NodeException
	 */
	protected int insertVersion(Page page, int timestamp, String number, boolean published) throws NodeException {
		return Trx.supply(() -> {
			DBUtils.update("INSERT INTO nodeversion (timestamp, user_id, o_type, o_id, published, nodeversion) VALUES (?, ?, ?, ?, ?, ?)", timestamp, 1,
					Page.TYPE_PAGE, page.getId(), published, number);
			return DBUtils.select("SELECT MAX(id) id FROM nodeversion WHERE o_type = ? AND o_id = ?", ps -> {
				ps.setInt(1, Page.TYPE_PAGE);
				ps.setInt(2, page.getId());
			}, DBUtils.firstInt("id"));
		});
	}

	/**
	 * Get the ids of the nodeversion entries of the page with the given timestamp
	 * @param page page
	 * @param timestamp version timestamp
	 * @return list of ids, ordered by id
	 * @throws NodeException
	 */
	protected List<Integer> getVersionIds(Page page, int timestamp) throws NodeException {
		return Trx.supply(() -> DBUtils.select("SELECT id FROM nodeversion WHERE o_type = ? AND o_id = ? AND timestamp = ? ORDER BY id", ps -> {
			ps.setInt(1, Page.TYPE_PAGE);
			ps.setInt(2, page.getId());
			ps.setInt(3, timestamp);
		}, DBUtils.IDLIST));
	}

	/**
	 * Get the rows of the record in the versioned table with the given timestamp
	 * @param table versioned table
	 * @param id record id
	 * @param timestamp version timestamp
	 * @return list of rows, ordered by auto_id
	 * @throws NodeException
	 */
	protected List<VersionRow> getRows(String table, int id, int timestamp) throws NodeException {
		boolean value = "value_nodeversion".equals(table);
		return Trx.supply(() -> DBUtils.select("SELECT auto_id, " + (value ? "value_text" : "NULL value_text")
				+ ", nodeversionlatest, nodeversionremoved FROM `" + table + "` WHERE id = ? AND nodeversiontimestamp = ? ORDER BY auto_id", ps -> {
					ps.setInt(1, id);
					ps.setInt(2, timestamp);
				}, rs -> {
					List<VersionRow> rows = new ArrayList<>();
					while (rs.next()) {
						rows.add(new VersionRow(rs.getInt("auto_id"), rs.getString("value_text"), rs.getInt("nodeversionlatest"),
								rs.getInt("nodeversionremoved")));
					}
					return rows;
				}));
	}

	/**
	 * Load the value "url" of the tag in the given version of the page (which puts the versioned objects into the cache)
	 * @param page page
	 * @param versionTimestamp version timestamp in seconds
	 * @param tagName tag name
	 * @throws NodeException
	 */
	protected void loadValue(Page page, int versionTimestamp, String tagName) throws NodeException {
		Trx.operate(t -> {
			Page version = t.getObject(Page.class, page.getId(), versionTimestamp);
			version.getContentTag(tagName).getValues().getByKeyname("url").getValueText();
		});
	}

	/**
	 * Get the string representation of a version, like it is used in {@link #assertVersions(Page, String...)}
	 * @param number version number
	 * @param timestamp version timestamp
	 * @param published true for the published version
	 * @return version as string
	 */
	protected String version(String number, int timestamp, boolean published) {
		return String.format("%s@%d%s", number, timestamp, published ? " (published)" : "");
	}

	/**
	 * Assert the versions of the page (latest version first) in the form "number@timestamp" with the suffix " (published)" for the published version
	 * @param page page
	 * @param expected expected versions
	 * @throws NodeException
	 */
	protected void assertVersions(Page page, String... expected) throws NodeException {
		List<String> versions = Trx.supply(t -> Stream.of(t.getObject(page).getVersions())
				.map(v -> version(v.getNumber(), v.getDate().getIntTimestamp(), v.isPublished()))
				.collect(Collectors.toList()));
		assertThat(versions).as("Page versions").containsExactly(expected);
	}

	/**
	 * Assert the value of the tag in the given version of the page
	 * @param page page
	 * @param versionTimestamp version timestamp in seconds (-1 for the current version)
	 * @param tagName tag name
	 * @param expected expected value
	 * @throws NodeException
	 */
	protected void assertValue(Page page, int versionTimestamp, String tagName, String expected) throws NodeException {
		Trx.operate(t -> {
			Page version = t.getObject(Page.class, page.getId(), versionTimestamp);
			assertThat(version).as("Page version @" + versionTimestamp).isNotNull();
			ContentTag tag = version.getContentTag(tagName);
			assertThat(tag).as("Tag " + tagName + " in version @" + versionTimestamp).isNotNull();
			Value value = tag.getValues().getByKeyname("url");
			assertThat(value).as("Value of tag " + tagName + " in version @" + versionTimestamp).isNotNull();
			assertThat(value.getValueText()).as("Value of tag " + tagName + " in version @" + versionTimestamp).isEqualTo(expected);
		});
	}

	/**
	 * Row of a versioned table
	 * @param autoId auto_id
	 * @param valueText value_text (only for value_nodeversion)
	 * @param latest nodeversionlatest
	 * @param removed nodeversionremoved
	 */
	protected static record VersionRow(int autoId, String valueText, int latest, int removed) {
	}
}
