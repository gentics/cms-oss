package com.gentics.node.tests.datasource.cn;

import java.util.Arrays;
import java.util.Collection;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.lib.expressionparser.ExpressionParser;
import com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter;
import com.gentics.testutils.database.JDBCMalformedURLException;
import com.gentics.testutils.database.TestDatabase;
import com.gentics.testutils.database.variations.TestDatabaseVariationConfigurations;

@Category(BaseLibTest.class)
public class CNDatasourceFilter3Test extends AbstractCNDatasourceFilterTest {

	public CNDatasourceFilter3Test(TestDatabase testDatabase) throws Exception {
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

	/**
	 * Test prefetching of large result sets (more than 1000 entries)
	 *
	 * @throws Exception
	 */
	@Test
	public void testPrefetchLargeResults() throws Exception {

		// first generate 3000 new entries
		int numEntries = 3000;

		for (int i = 1; i <= numEntries; i++) {
			createNode(null, "entry #" + i);
		}

		// create the filter for fetching the results
		DatasourceFilter filter = ds.createDatasourceFilter(ExpressionParser.getInstance().parse(
				"object.obj_type == " + NODETYPE + " AND object.name LIKE 'entry #%'"));

		// first fetch less than 1000 entries
		internalTestPrefetchLargeResults(filter, 500);

		// now test with 999 entries
		internalTestPrefetchLargeResults(filter, 999);

		// now test with 1000 entries
		internalTestPrefetchLargeResults(filter, 1000);

		// and test with more than 1000 entries
		internalTestPrefetchLargeResults(filter, 2500);

		// and test with all entries
		internalTestPrefetchLargeResults(filter, -1);
	}

	/**
	 * Test whether the updatetimestamp is written on update/insert/delete, when configured with datasource parameter "setUpdatetimestampOnWrite" (and not, when not
	 * configured)
	 */
	@Test
	public void testSetUpdatetimestampOnWrite() throws Exception {
		internalTestSetUpdatetimestampOnWrite(ds, false);
		internalTestSetUpdatetimestampOnWrite(cachedDs, true);
	}

	/**
	 * Test autoprefetching of optimized attributes
	 *
	 * @throws Exception
	 */
	@Test
	public void testAutoPrefetchingUnsorted() throws Exception {
		internalTestAutoPrefetching(null);
	}

	/**
	 * Test autoprefetching of optimized attributes when sorting by an optimized attribute
	 *
	 * @throws Exception
	 */
	@Test
	public void testAutoPrefetchingSortedByQuick() throws Exception {
		internalTestAutoPrefetching(new Datasource.Sorting[] { new Datasource.Sorting("shortname", Datasource.SORTORDER_ASC) });
	}

	/**
	 * Test autoprefetching of optimized attributes when sorting by a meta attribute
	 *
	 * @throws Exception
	 */
	@Test
	public void testAutoPrefetchingSortedByMeta() throws Exception {
		internalTestAutoPrefetching(new Datasource.Sorting[] { new Datasource.Sorting("obj_id", Datasource.SORTORDER_ASC) });
	}

	/**
	 * Test autoprefetching of optimized attributes when sorting by a normal attribute
	 *
	 * @throws Exception
	 */
	@Test
	public void testAutoPrefetchingSortedByNormal() throws Exception {
		internalTestAutoPrefetching(new Datasource.Sorting[] { new Datasource.Sorting("name", Datasource.SORTORDER_ASC) });
	}

	/**
	 * Test autoprefetching of optimized attributes when sorting by a more than one attribute
	 *
	 * @throws Exception
	 */
	@Test
	public void testAutoPrefetchingMultisorted() throws Exception {
		internalTestAutoPrefetching(new Datasource.Sorting[] { new Datasource.Sorting("name", Datasource.SORTORDER_ASC),
				new Datasource.Sorting("shortname", Datasource.SORTORDER_DESC), new Datasource.Sorting("obj_id", Datasource.SORTORDER_ASC) });
	}

}
