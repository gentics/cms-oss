package com.gentics.contentnode.tests.rest.meta;

import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.assertj.GCNAssertions.attribute;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.ContentNodeDate;
import com.gentics.contentnode.object.CustomMetaDateNodeObject;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.parttype.OverviewPartType;
import com.gentics.contentnode.rest.model.ContentNodeItem;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for custom item dates (custom_cdate, custom_edate)
 */
public abstract class CustomMetaDateTest<T extends CustomMetaDateNodeObject, R extends ContentNodeItem> {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	protected static Map<String, DateFormat> cDatePropertyMap = new HashMap<>();

	protected static Map<String, DateFormat> eDatePropertyMap = new HashMap<>();

	static {
		cDatePropertyMap.put("creationtimestamp", DateFormat.TIMESTAMP);
		cDatePropertyMap.put("erstellungstimestamp", DateFormat.TIMESTAMP);
		cDatePropertyMap.put("createtimestamp", DateFormat.TIMESTAMP);
		cDatePropertyMap.put("createtime", DateFormat.FULLFORMAT);
		cDatePropertyMap.put("creationdate", DateFormat.TOSTRING);
		cDatePropertyMap.put("erstellungsdatum", DateFormat.TOSTRING);

		eDatePropertyMap.put("edittimestamp", DateFormat.TIMESTAMP);
		eDatePropertyMap.put("bearbeitungstimestamp", DateFormat.TIMESTAMP);
		eDatePropertyMap.put("edittime", DateFormat.FULLFORMAT);
		eDatePropertyMap.put("editdate", DateFormat.TOSTRING);
		eDatePropertyMap.put("bearbeitungsdatum", DateFormat.TOSTRING);
	}

	protected static Node node;

	protected static Template template;

	protected static SystemUser systemUser;

	protected static Integer overviewConstructId;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		node = supply(() -> createNode());
		template = supply(() -> createTemplate(node.getFolder(), "Template"));

		systemUser = supply(t -> t.getObject(SystemUser.class, 1));

		overviewConstructId = supply(() -> createConstruct(node, OverviewPartType.class, "overview", "ds"));
	}

	/**
	 * Test default values for new created item
	 * @throws NodeException
	 */
	@Test
	public void testDefaultValues() throws NodeException {
		int createTime = 10;
		R item = createMetaDated(createTime);

		assertThat(item).as("Created item")
			.has(attribute("cdate", createTime))
			.has(attribute("edate", createTime))
			.has(attribute("customCdate", 0))
			.has(attribute("customEdate", 0));
	}

	/**
	 * Test setting a custom cdate
	 * @throws NodeException
	 */
	@Test
	public void testSetCustomCDate() throws NodeException {
		int createTime = 10;
		int updateTime = 30;
		int customCdate = 20;
		R item = createMetaDated(createTime);

		item = updateMetaDated(updateTime, item.getId(), Optional.empty(), Optional.empty(), Optional.of(customCdate), Optional.empty());

		assertThat(item).as("Updated item")
			.has(attribute("cdate", createTime))
			.has(attribute("edate", updateTime))
			.has(attribute("customCdate", customCdate))
			.has(attribute("customEdate", 0));
	}

	public abstract R createMetaDated(int createTime) throws NodeException;

	public abstract R updateMetaDated(int updateTime, Integer id, Optional<Integer> maybeDate, Optional<Integer> maybeEDate, Optional<Integer> maybeCustomCDate, Optional<Integer> maybeCustomEDate) throws NodeException;

	/**
	 * Test unsetting the custom cdate
	 * @throws NodeException
	 */
	@Test
	public void testUnsetCustomCDate() throws NodeException {
		int createTime = 10;
		int updateTime = 30;
		int customCdate = 20;
		int updateTime2 = 40;
		R item = createMetaDated(createTime);

		item = updateMetaDated(updateTime, item.getId(), Optional.empty(), Optional.empty(), Optional.of(customCdate), Optional.empty());

		item = updateMetaDated(updateTime2, item.getId(), Optional.empty(), Optional.empty(), Optional.of(0), Optional.empty());

		assertThat(item).as("Updated item")
			.has(attribute("cdate", createTime))
			.has(attribute("edate", updateTime2))
			.has(attribute("customCdate", 0))
			.has(attribute("customEdate", 0));
	}

	/**
	 * Test setting a custom edate
	 * @throws NodeException
	 */
	@Test
	public void testSetCustomEDate() throws NodeException {
		int createTime = 10;
		int updateTime = 30;
		int customEdate = 20;
		R item = createMetaDated(createTime);

		item = updateMetaDated(updateTime, item.getId(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(customEdate));

		assertThat(item).as("Updated item")
			.has(attribute("cdate", createTime))
			.has(attribute("edate", updateTime))
			.has(attribute("customCdate", 0))
			.has(attribute("customEdate", customEdate));
	}

	/**
	 * Test unsetting a custom edate
	 * @throws NodeException
	 */
	@Test
	public void testUnsetCustomEDate() throws NodeException {
		int createTime = 10;
		int updateTime = 30;
		int customEdate = 20;
		int updateTime2 = 40;
		R item = createMetaDated(createTime);

		item = updateMetaDated(updateTime, item.getId(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(customEdate));

		item = updateMetaDated(updateTime2, item.getId(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(0));

		assertThat(item).as("Updated item")
			.has(attribute("cdate", createTime))
			.has(attribute("edate", updateTime2))
			.has(attribute("customCdate", 0))
			.has(attribute("customEdate", 0));
	}

	/**
	 * Test setting cdate (which does not change anything)
	 * @throws NodeException
	 */
	@Test
	public void testSetCDate() throws NodeException {
		int createTime = 10;
		int updateTime = 30;
		int newCdate = 20;
		R item = createMetaDated(createTime);

		item = updateMetaDated(updateTime, item.getId(), Optional.of(newCdate), Optional.empty(), Optional.empty(), Optional.empty());

		assertThat(item).as("Updated item")
			.has(attribute("cdate", createTime))
			.has(attribute("edate", createTime))
			.has(attribute("customCdate", 0))
			.has(attribute("customEdate", 0));
	}

	/**
	 * Test setting edate (which does not change anything)
	 * @throws NodeException
	 */
	@Test
	public void testSetEDate() throws NodeException {
		int createTime = 10;
		int updateTime = 30;
		int newEdate = 20;
		R item = createMetaDated(createTime);

		item = updateMetaDated(updateTime, item.getId(), Optional.empty(), Optional.of(newEdate), Optional.empty(), Optional.empty());

		assertThat(item).as("Updated item")
			.has(attribute("cdate", createTime))
			.has(attribute("edate", createTime))
			.has(attribute("customCdate", 0))
			.has(attribute("customEdate", 0));
	}

	/**
	 * Enum of date formats for rendering
	 */
	protected static enum DateFormat {
		/**
		 * Rendering the timestamp
		 */
		TIMESTAMP(date -> String.valueOf(date.getIntTimestamp())),

		/**
		 * Rendering by calling {@link ContentNodeDate#getFullFormat()}
		 */
		FULLFORMAT(ContentNodeDate::getFullFormat),

		/**
		 * Rendering by calling {@link ContentNodeDate#toString()}
		 */
		TOSTRING(ContentNodeDate::toString);

		/**
		 * Rendering function
		 */
		protected Function<ContentNodeDate, String> renderer;

		/**
		 * Create instance with rendering function
		 * @param renderer function
		 */
		DateFormat(Function<ContentNodeDate, String> renderer) {
			this.renderer = renderer;
		}

		/**
		 * Render the date
		 * @param date date
		 * @return rendered date
		 */
		public String render(ContentNodeDate date) {
			return renderer.apply(date);
		}
	}
}
