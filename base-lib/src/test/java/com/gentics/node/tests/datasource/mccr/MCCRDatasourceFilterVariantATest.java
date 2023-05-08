package com.gentics.node.tests.datasource.mccr;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.junit.experimental.categories.Category;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.lib.base.MapResolver;
import com.gentics.lib.content.GenticsContentAttribute;
import com.gentics.lib.datasource.object.ObjectAttributeBean;
import com.gentics.testutils.database.JDBCMalformedURLException;
import com.gentics.testutils.database.TestDatabase;
import com.gentics.testutils.database.variations.AbstractDatabaseVariationTest;
import com.gentics.testutils.database.variations.TestDatabaseVariationConfigurations;

@Category(BaseLibTest.class)
public class MCCRDatasourceFilterVariantATest extends AbstractMCCRDatasourceFilterTest {

	static {
		try {
			// simplest filter
			FILTERTESTS.add(FilterTest.create("true").expect(1, NUM_OBJECTS).expect(2, NUM_OBJECTS).expect(3, NUM_OBJECTS));

			// select by object type
			FILTERTESTS.add(FilterTest.create("object.obj_type == 1000").expect(1, NUM_OBJECTS).expect(2, NUM_OBJECTS).expect(3, NUM_OBJECTS));

			// select by different object type
			FILTERTESTS.add(FilterTest.create("object.obj_type == 1001").expect(1, 0).expect(2, 0).expect(3, 0));

			// select by contentid
			FILTERTESTS.add(FilterTest.create("object.contentid CONTAINSONEOF mapdata.contentids").expect(1, 3).expect(2, 3).expect(3, 3)
					.data("contentids", Arrays.asList("1000.3", "1000.5", "1000.7")));

			// select by a single attribute
			for (String attrName : MCCRTestDataHelper.ATTRIBUTE_NAMES) {
				ObjectAttributeBean attrType = types.get(1000).getAttributeTypesMap().get(attrName);

				// omit attributes that are written into the filesystem
				if (attrType.isFilesystem()) {
					continue;
				}
				boolean linkAttribute = attrType.getAttributetype() == GenticsContentAttribute.ATTR_TYPE_OBJ;
				boolean multivalue = attrType.getMultivalue();
				String expression = "object." + attrName + " " + getComparisonOperator(attrType) + " data.value";

				// normal attributes are set only once, multivalue link attributes will be set 5 times, when we search with CONTAINSONEOF to a list of 3 subsequent linked
				// objects
				int expect = linkAttribute && multivalue ? 5 : 1;

				// link attributes are also set in the master and subchannel
				int expectInMaster = linkAttribute ? expect : 0;
				int expectInChannel = expect;
				int expectInSubchannel = linkAttribute ? expect : 0;

				FILTERTESTS.add(FilterTest.create(expression).expect(1, expectInMaster).expect(2, expectInChannel).expect(3, expectInSubchannel)
						.data(new DataResolver(attrType, 2, NUM_OBJECTS / 2)));

				// for link attributes, add tests for nested attributes
				if (linkAttribute) {
					for (String subAttrName : MCCRTestDataHelper.ATTRIBUTE_NAMES) {
						ObjectAttributeBean subAttrType = types.get(1000).getAttributeTypesMap().get(subAttrName);

						// omit attributes that are written into the filesystem
						if (subAttrType.isFilesystem()) {
							continue;
						}

						boolean subLinkAttribute = subAttrType.getAttributetype() == GenticsContentAttribute.ATTR_TYPE_OBJ;
						boolean subMultivalue = subAttrType.getMultivalue();

						expression = "object." + attrName + "." + subAttrName + " " + getComparisonOperator(subAttrType) + " data.value";

						// normal attributes are set only once, multivalue link attributes will be set 5 times, when we search with CONTAINSONEOF to a list of 3
						// subsequent linked objects
						// if the primary attribute is a multivalue link, we get 2 additional results
						expect = (subLinkAttribute && subMultivalue ? 5 : 1) + (linkAttribute && multivalue ? 2 : 0);

						// link attributes are also set in the master and subchannel
						expectInMaster = subLinkAttribute ? expect : 0;
						expectInChannel = expect;
						expectInSubchannel = subLinkAttribute ? expect : 0;

						FILTERTESTS.add(FilterTest.create(expression).expect(1, expectInMaster).expect(2, expectInChannel)
								.expect(3, expectInSubchannel).data(new DataResolver(subAttrType, 2, NUM_OBJECTS / 2)));

					}
				}
			}

			// select by foreign link attribute
			for (ObjectAttributeBean attrType : types.get(1000).getAttributeTypesList()) {
				if (attrType.getAttributetype() != GenticsContentAttribute.ATTR_TYPE_FOREIGNOBJ) {
					continue;
				}

				ObjectAttributeBean linkAttrType = types.get(1000).getAttributeTypesMap().get(attrType.getForeignlinkattribute());

				String expression = "object." + attrType.getName() + " CONTAINSONEOF mapdata.value";
				// when the link attribute is single value, we expect 2 objects linking to any of the 2 given
				// when the link attribute is multivalue, we expect 6 (3 each)
				int expected = linkAttrType.getMultivalue() ? 6 : 2;

				FILTERTESTS.add(FilterTest.create(expression).expect(1, expected).expect(2, expected).expect(3, expected)
						.data("value", Arrays.asList("1000.1", "1000.5")));
			}

			// tests for other operators
			FILTERTESTS.add(FilterTest.create("object.int != data.value").expect(1, NUM_OBJECTS).expect(2, NUM_OBJECTS - 1).expect(3, NUM_OBJECTS)
					.data(new DataResolver(types.get(1000).getAttributeTypesMap().get("int"), 2, NUM_OBJECTS / 2)));
			FILTERTESTS.add(FilterTest.create("object.int_opt != data.value").expect(1, NUM_OBJECTS).expect(2, NUM_OBJECTS - 1)
					.expect(3, NUM_OBJECTS).data(new DataResolver(types.get(1000).getAttributeTypesMap().get("int_opt"), 2, NUM_OBJECTS / 2)));

			FILTERTESTS.add(FilterTest.create("object.int > data.value").expect(1, 0).expect(2, NUM_OBJECTS / 2).expect(3, NUM_OBJECTS)
					.data(new DataResolver(types.get(1000).getAttributeTypesMap().get("int"), 2, NUM_OBJECTS / 2)));
			FILTERTESTS.add(FilterTest.create("object.int_opt > data.value").expect(1, 0).expect(2, NUM_OBJECTS / 2).expect(3, NUM_OBJECTS)
					.data(new DataResolver(types.get(1000).getAttributeTypesMap().get("int_opt"), 2, NUM_OBJECTS / 2)));

			FILTERTESTS.add(FilterTest.create("object.int >= data.value").expect(1, 0).expect(2, NUM_OBJECTS / 2 + 1).expect(3, NUM_OBJECTS)
					.data(new DataResolver(types.get(1000).getAttributeTypesMap().get("int"), 2, NUM_OBJECTS / 2)));
			FILTERTESTS.add(FilterTest.create("object.int_opt >= data.value").expect(1, 0).expect(2, NUM_OBJECTS / 2 + 1).expect(3, NUM_OBJECTS)
					.data(new DataResolver(types.get(1000).getAttributeTypesMap().get("int_opt"), 2, NUM_OBJECTS / 2)));

			FILTERTESTS.add(FilterTest.create("object.int <= data.value").expect(1, NUM_OBJECTS).expect(2, NUM_OBJECTS / 2).expect(3, 0)
					.data(new DataResolver(types.get(1000).getAttributeTypesMap().get("int"), 2, NUM_OBJECTS / 2)));
			FILTERTESTS.add(FilterTest.create("object.int_opt <= data.value").expect(1, NUM_OBJECTS).expect(2, NUM_OBJECTS / 2).expect(3, 0)
					.data(new DataResolver(types.get(1000).getAttributeTypesMap().get("int_opt"), 2, NUM_OBJECTS / 2)));

			FILTERTESTS.add(FilterTest.create("object.int < data.value").expect(1, NUM_OBJECTS).expect(2, NUM_OBJECTS / 2 - 1).expect(3, 0)
					.data(new DataResolver(types.get(1000).getAttributeTypesMap().get("int"), 2, NUM_OBJECTS / 2)));
			FILTERTESTS.add(FilterTest.create("object.int_opt < data.value").expect(1, NUM_OBJECTS).expect(2, NUM_OBJECTS / 2 - 1).expect(3, 0)
					.data(new DataResolver(types.get(1000).getAttributeTypesMap().get("int_opt"), 2, NUM_OBJECTS / 2)));

			FILTERTESTS.add(FilterTest.create("object.text LIKE mapdata.value").expect(1, 0).expect(2, NUM_OBJECTS).expect(3, 0)
					.data("value", "%channel 2%"));

			// test some functions
			FILTERTESTS.add(FilterTest.create("concat(mapdata.value, object.text) LIKE mapdata.pattern").expect(1, 1).expect(2, 1).expect(3, 1)
					.data("value", "bla").data("pattern", "blaValue%#" + (NUM_OBJECTS / 2)));
			FILTERTESTS.add(FilterTest.create("!isempty(object.text_multi)").expect(1, NUM_OBJECTS).expect(2, NUM_OBJECTS).expect(3, NUM_OBJECTS));
			FILTERTESTS.add(FilterTest.create("object.contentid CONTAINSONEOF subrule(\"contentid\", \"subobject.text LIKE mapdata.value\")")
					.eval("object.text LIKE mapdata.value").expect(1, 0).expect(2, NUM_OBJECTS).expect(3, 0).data("value", "%channel 2%"));

			// matches function
			Map<String, Object> matchesMap = new HashMap<String, Object>();

			matchesMap.put("text1", MCCRTestDataHelper.getValue(types.get(1000).getAttributeTypesMap().get("text_multi"), 2, NUM_OBJECTS / 2));
			matchesMap.put("text2", MCCRTestDataHelper.getValue(types.get(1000).getAttributeTypesMap().get("longtext_multi"), 2, NUM_OBJECTS / 2));
			FILTERTESTS.add(FilterTest
					.create("matches(mapdata.value, object.text_multi CONTAINSONEOF this.text1 && object.longtext_multi CONTAINSONEOF this.text2)")
					.expect(1, 0).expect(2, 1).expect(3, 0).data("value", new MapResolver(matchesMap)));

			// filter functions
			FILTERTESTS.add(FilterTest.create("filter(true, 'com.gentics.node.tests.datasource.mccr.FilterChannelPostProcessor', 1)").eval(false)
					.expect(1, NUM_OBJECTS).expect(2, 0).expect(3, 0));
			FILTERTESTS.add(FilterTest.create("filter(true, 'com.gentics.node.tests.datasource.mccr.FilterChannelPostProcessor', 2)").eval(false)
					.expect(1, 0).expect(2, NUM_OBJECTS).expect(3, 0));
			FILTERTESTS.add(FilterTest.create("filter(true, 'com.gentics.node.tests.datasource.mccr.FilterChannelPostProcessor', 3)").eval(false)
					.expect(1, 0).expect(2, 0).expect(3, NUM_OBJECTS));

			// filter by channel_id
			FILTERTESTS.add(FilterTest.create("object.channel_id == 1").expect(1, NUM_OBJECTS).expect(2, 0).expect(3, 0));
			FILTERTESTS.add(FilterTest.create("object.channel_id == 2").expect(1, 0).expect(2, NUM_OBJECTS).expect(3, 0));
			FILTERTESTS.add(FilterTest.create("object.channel_id == 3").expect(1, 0).expect(2, 0).expect(3, NUM_OBJECTS));

			// filter by channelset_id
			FILTERTESTS.add(FilterTest.create("object.channelset_id == 1 || object.channelset_id == " + NUM_OBJECTS).expect(1, 2).expect(2, 2).expect(3, 2));


		} catch (Exception e) {
			logger.error(e);
		}

	}

	public MCCRDatasourceFilterVariantATest(TestDatabase testDatabase, FilterTest filterTest) {
		super(testDatabase, filterTest);
	}

	/**
	 * Get the test parameters
	 *
	 * @return collection of test parameter sets
	 * @throws JDBCMalformedURLException
	 */
	@Parameters(name = "{index}: {0}, filter: {1}")
	public static Collection<Object[]> data() throws JDBCMalformedURLException {
		Map<String, TestDatabase> variations = getVariations(TestDatabaseVariationConfigurations.MYSQL_VARIATIONS,
				TestDatabaseVariationConfigurations.MSSQL_VARIATIONS, TestDatabaseVariationConfigurations.ORACLE_VARIATIONS);

		Collection<Object[]> data = new Vector<Object[]>();

		for (TestDatabase testDB : variations.values()) {
			for (FilterTest filterTest : FILTERTESTS) {
				if (!filterTest.isRestricted(testDB)) {
					data.add(new Object[] { testDB, filterTest });
				}
			}
		}
		return data;
	}

}
