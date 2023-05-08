package com.gentics.node.tests.datasource.mccr;

import java.util.Collection;
import java.util.Map;
import java.util.Vector;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.junit.experimental.categories.Category;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.lib.content.GenticsContentAttribute;
import com.gentics.lib.datasource.object.ObjectAttributeBean;
import com.gentics.testutils.database.JDBCMalformedURLException;
import com.gentics.testutils.database.TestDatabase;
import com.gentics.testutils.database.variations.TestDatabaseVariationConfigurations;

@Category(BaseLibTest.class)
public class MCCRDatasourceFilterVariantBTest extends AbstractMCCRDatasourceFilterTest {
	static {
		try {

			// make some AND tests
			for (ObjectAttributeBean firstAttrType : types.get(1000).getAttributeTypesList()) {
				if (firstAttrType.isFilesystem() || firstAttrType.getAttributetype() == GenticsContentAttribute.ATTR_TYPE_FOREIGNOBJ) {
					continue;
				}
				for (ObjectAttributeBean secondAttrType : types.get(1000).getAttributeTypesList()) {
					if (secondAttrType.isFilesystem() || secondAttrType.getAttributetype() == GenticsContentAttribute.ATTR_TYPE_FOREIGNOBJ) {
						continue;
					}
					if (secondAttrType.equals(firstAttrType)) {
						continue;
					}

					boolean linkAttribute = firstAttrType.getAttributetype() == GenticsContentAttribute.ATTR_TYPE_OBJ
							&& secondAttrType.getAttributetype() == GenticsContentAttribute.ATTR_TYPE_OBJ;
					boolean multivalue = firstAttrType.getMultivalue() && secondAttrType.getMultivalue();

					int expect = linkAttribute && multivalue ? 5 : 1;

					int expectInMaster = linkAttribute ? expect : 0;
					int expectInChannel = expect;
					int expectInSubchannel = linkAttribute ? expect : 0;

					// test with AND
					String expression = "object." + firstAttrType.getName() + " " + getComparisonOperator(firstAttrType)
							+ " data.0.value AND object." + secondAttrType.getName() + " " + getComparisonOperator(secondAttrType) + " data.1.value";

					FILTERTESTS.add(FilterTest
							.create(expression)
							.expect(1, expectInMaster)
							.expect(2, expectInChannel)
							.expect(3, expectInSubchannel)
							.data(new ListResolver(new DataResolver(firstAttrType, 2, NUM_OBJECTS / 2), new DataResolver(secondAttrType, 2,
									NUM_OBJECTS / 2))));
				}
			}

		} catch (Exception e) {
			logger.error(e);
		}

	}

	public MCCRDatasourceFilterVariantBTest(TestDatabase testDatabase, FilterTest filterTest) {
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
