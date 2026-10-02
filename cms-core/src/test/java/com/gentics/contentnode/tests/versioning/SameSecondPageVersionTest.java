package com.gentics.contentnode.tests.versioning;

import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createPage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static org.assertj.core.api.Assertions.assertThat;

import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.List;
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
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for page versions, that are created in the same second (SUP-20172).
 * The transaction timestamps are set explicitly (relative to {@link #base}), so the tests do not depend on timing.
 * The base timestamp is in the future, so that the content lock, which is set with the transaction timestamp when the page is created,
 * does not expire (an expired lock would create an additional version with the current time when the page is locked again).
 */
public class SameSecondPageVersionTest {
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
	public void setup() {
		base = (int) (System.currentTimeMillis() / 1000) + 3600;
	}

	/**
	 * Test that saving the page (with creating a version) in the same second as publishing it does not modify the published version
	 * @throws NodeException
	 */
	@Test
	public void testSaveAfterPublish() throws NodeException {
		Page page = createPageAt(base);
		String tagName = addTagAndPublishAt(page, base + 10, "first");

		// change the tag and save in the same second
		setValueAndSaveAt(page, base + 10, tagName, "second");

		assertVersions(page, version("1.1", base + 11, false), version("1.0", base + 10, true), version("0.1", base, false));
		assertValue(page, base + 10, tagName, "first");
		assertValue(page, base + 11, tagName, "second");
		assertValue(page, -1, tagName, "second");
	}

	/**
	 * Test that publishing, saving and publishing again in the same second creates a new published version and does not modify the
	 * previously published version
	 * @throws NodeException
	 */
	@Test
	public void testPublishAfterSaveAfterPublish() throws NodeException {
		Page page = createPageAt(base);
		String tagName = addTagAndPublishAt(page, base + 10, "first");

		// change the tag and save, then publish again (in the same second)
		setValueAndSaveAt(page, base + 10, tagName, "second");
		Trx.operate(t -> {
			t.setTimestamp((base + 10) * 1000L);
			t.getObject(page, true).publish();
		});

		assertVersions(page, version("2.0", base + 11, true), version("1.0", base + 10, false), version("0.1", base, false));
		assertValue(page, base + 10, tagName, "first");
		assertValue(page, base + 11, tagName, "second");
	}

	/**
	 * Test that saving the page after publishing it in the same transaction modifies the published version (since no other transaction can
	 * see the published version yet)
	 * @throws NodeException
	 */
	@Test
	public void testSaveAfterPublishInSameTransaction() throws NodeException {
		Page page = createPageAt(base);
		String tagName = Trx.supply(t -> {
			t.setTimestamp((base + 10) * 1000L);
			Page editablePage = t.getObject(page, true);
			ContentTag tag = editablePage.getContent().addContentTag(constructId);
			tag.setEnabled(true);
			getPartType(ShortTextPartType.class, tag, "url").setText("first");
			// publish before saving (the new tag is not contained in the published version yet)
			editablePage.publish();
			editablePage.save();
			return tag.getName();
		});

		assertVersions(page, version("1.0", base + 10, true), version("0.1", base, false));
		assertValue(page, base + 10, tagName, "first");
	}

	/**
	 * Test that the versioned content is not taken from the cache, after a version (which is not published) was modified in the same second
	 * @throws NodeException
	 */
	@Test
	public void testCachedVersionedContent() throws NodeException {
		Page page = createPageAt(base);

		// add a first tag and save (creates the unpublished version 0.2)
		String firstTagName = addTagAndSaveAt(page, base + 10, "first");
		assertVersions(page, version("0.2", base + 10, false), version("0.1", base, false));

		// load the version, so that the versioned content is put into the cache
		assertValue(page, base + 10, firstTagName, "first");

		// add a second tag and save in the same second (this modifies the unpublished version 0.2)
		String secondTagName = addTagAndSaveAt(page, base + 10, "second");
		assertVersions(page, version("0.2", base + 10, false), version("0.1", base, false));

		// the version must contain both tags
		assertValue(page, base + 10, firstTagName, "first");
		assertValue(page, base + 10, secondTagName, "second");
	}

	/**
	 * Test that saving the page does not modify the published version, if another (unpublished) version with the same timestamp exists
	 * (like created by concurrent requests)
	 * @throws NodeException
	 */
	@Test
	public void testSaveWithDuplicateVersionTimestamp() throws NodeException {
		Page page = createPageAt(base);
		String tagName = addTagAndPublishAt(page, base + 10, "first");

		// add another (unpublished) version with the timestamp of the published version
		Trx.operate(t -> {
			DBUtils.update("INSERT INTO nodeversion (timestamp, user_id, o_type, o_id, published, nodeversion) VALUES (?, ?, ?, ?, ?, ?)", base + 10, 1,
					Page.TYPE_PAGE, page.getId(), 0, "1.1");
			t.dirtObjectCache(Page.class, page.getId());
		});
		assertVersions(page, version("1.1", base + 10, false), version("1.0", base + 10, true), version("0.1", base, false));

		// change the tag and save in the same second
		setValueAndSaveAt(page, base + 10, tagName, "second");

		assertVersions(page, version("1.2", base + 11, false), version("1.1", base + 10, false), version("1.0", base + 10, true),
				version("0.1", base, false));
		assertValue(page, base + 10, tagName, "first");
		assertValue(page, base + 11, tagName, "second");
	}

	/**
	 * Test that duplicate rows for the same version of a value are handled consistently: the first written row is used when loading
	 * the version and when creating a new version
	 * @throws NodeException
	 */
	@Test
	public void testDuplicateVersionedValueRows() throws NodeException {
		Page page = createPageAt(base);
		String tagName = addTagAndSaveAt(page, base + 10, "first");
		int valueId = Trx.supply(t -> t.getObject(page).getContentTag(tagName).getValues().getByKeyname("url").getId());

		// add a second row for the same version of the value (with higher auto_id)
		Trx.operate(() -> duplicateVersionedValue(valueId, base + 10, "duplicate"));
		Trx.operate(t -> t.clearNodeObjectCache());

		// loading the version of the page must use the first row
		assertValue(page, base + 10, tagName, "first");

		// loading the version of the value must use the first row
		Trx.operate(t -> t.clearNodeObjectCache());
		Trx.operate(t -> {
			assertThat(t.getObject(Value.class, valueId, base + 10).getValueText()).as("Versioned value").isEqualTo("first");
		});

		// creating a new version (with another change) must also use the first row, so the (unchanged) value is not versioned again
		addTagAndSaveAt(page, base + 20, "other");
		assertThat(getVersionTimestamps(valueId)).as("Version timestamps of the value").containsExactly(base + 10, base + 10);
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
	 * Add a tag with the given value to the page and save it with the given transaction timestamp
	 * @param page page
	 * @param timestamp timestamp in seconds
	 * @param value tag value
	 * @return tag name
	 * @throws NodeException
	 */
	protected String addTagAndSaveAt(Page page, int timestamp, String value) throws NodeException {
		return addTagAt(page, timestamp, value, false);
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
		return addTagAt(page, timestamp, value, true);
	}

	/**
	 * Add a tag with the given value to the page and save (and optionally publish) it with the given transaction timestamp
	 * @param page page
	 * @param timestamp timestamp in seconds
	 * @param value tag value
	 * @param publish true to publish the page after saving
	 * @return tag name
	 * @throws NodeException
	 */
	protected String addTagAt(Page page, int timestamp, String value, boolean publish) throws NodeException {
		return Trx.supply(t -> {
			t.setTimestamp(timestamp * 1000L);
			Page editablePage = t.getObject(page, true);
			ContentTag tag = editablePage.getContent().addContentTag(constructId);
			tag.setEnabled(true);
			getPartType(ShortTextPartType.class, tag, "url").setText(value);
			editablePage.save();
			if (publish) {
				editablePage.publish();
			}
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
	 * Duplicate the row of the given value version in value_nodeversion (with a new auto_id) and set the given value text in the duplicate
	 * @param valueId value ID
	 * @param timestamp version timestamp
	 * @param valueText value text for the duplicate row
	 * @throws NodeException
	 */
	protected void duplicateVersionedValue(int valueId, int timestamp, String valueText) throws NodeException {
		List<String> columns = DBUtils.select("SELECT * FROM value_nodeversion WHERE 1 = 0", rs -> {
			List<String> names = new ArrayList<>();
			ResultSetMetaData metaData = rs.getMetaData();
			for (int i = 1; i <= metaData.getColumnCount(); i++) {
				if (!"auto_id".equals(metaData.getColumnName(i))) {
					names.add(metaData.getColumnName(i));
				}
			}
			return names;
		});
		String selectColumns = columns.stream().map(column -> "value_text".equals(column) ? "?" : column).collect(Collectors.joining(", "));
		DBUtils.update(String.format("INSERT INTO value_nodeversion (%s) SELECT %s FROM value_nodeversion WHERE id = ? AND nodeversiontimestamp = ?",
				String.join(", ", columns), selectColumns), valueText, valueId, timestamp);
	}

	/**
	 * Get the version timestamps of all rows of the value in value_nodeversion (ordered by auto_id)
	 * @param valueId value ID
	 * @return list of version timestamps
	 * @throws NodeException
	 */
	protected List<Integer> getVersionTimestamps(int valueId) throws NodeException {
		return Trx.supply(() -> DBUtils.select("SELECT nodeversiontimestamp FROM value_nodeversion WHERE id = ? ORDER BY auto_id",
				ps -> ps.setInt(1, valueId), rs -> {
					List<Integer> timestamps = new ArrayList<>();
					while (rs.next()) {
						timestamps.add(rs.getInt("nodeversiontimestamp"));
					}
					return timestamps;
				}));
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
}
