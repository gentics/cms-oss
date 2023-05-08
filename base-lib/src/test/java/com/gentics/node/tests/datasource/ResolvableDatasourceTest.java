/*
 * @author herbert
 * @date 18.12.2006
 * @version $Id: ResolvableDatasourceTest.java,v 1.1 2010-02-04 14:25:05 norbert Exp $
 */
package com.gentics.node.tests.datasource;

import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.junit.Test;

import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.lib.datasource.DatasourceNotAvailableException;
import com.gentics.api.lib.datasource.ResolvableDatasource;
import com.gentics.api.lib.expressionparser.ExpressionParser;
import com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.lib.base.MapResolver;
import org.junit.experimental.categories.Category;

@Category(BaseLibTest.class)
public class ResolvableDatasourceTest {

	@Test
	public void testGetResultSimple() throws DatasourceNotAvailableException {
		ResolvableDatasource ds = createResolvableDatasource();
		Collection coll = ds.getResult();

		verifySortorder(coll, new String[] { "x", "a", "c", "f", "d" }, "Simple getResult");
	}

	@Test
	public void testGetResultWithLimit() throws DatasourceNotAvailableException {
		ResolvableDatasource ds = createResolvableDatasource();
		Collection coll = ds.getResult(0, 3, null, Datasource.SORTORDER_NONE, null);

		verifySortorder(coll, new String[] { "x", "a", "c" }, "getResult with limit");
	}

	@Test
	public void testGetResultWithStart() throws DatasourceNotAvailableException {
		ResolvableDatasource ds = createResolvableDatasource();
		Collection coll = ds.getResult(3, -1, null, Datasource.SORTORDER_NONE, null);

		verifySortorder(coll, new String[] { "f", "d" }, "getResult with count");
	}

	@Test
	public void testGetResultWithStartAndLimit() throws DatasourceNotAvailableException {
		ResolvableDatasource ds = createResolvableDatasource();
		Collection coll = ds.getResult(3, 1, null, Datasource.SORTORDER_NONE, null);

		verifySortorder(coll, new String[] { "f" }, "getResult with count and limit");
	}

	@Test
	public void testGetResultWithSort() throws DatasourceNotAvailableException {
		ResolvableDatasource ds = createResolvableDatasource();
		Collection coll = ds.getResult(-1, -1, "name", Datasource.SORTORDER_ASC, null);

		verifySortorder(coll, new String[] { "a", "c", "d", "f", "x" }, "getResult with order");
	}

	@Test
	public void testGetResultWithSortDesc() throws DatasourceNotAvailableException {
		ResolvableDatasource ds = createResolvableDatasource();
		Collection coll = ds.getResult(-1, -1, "name", Datasource.SORTORDER_DESC, null);

		verifySortorder(coll, new String[] { "x", "f", "d", "c", "a" }, "getResult with desc order");
	}

	@Test
	public void testGetResultWithSortStart() throws DatasourceNotAvailableException {
		ResolvableDatasource ds = createResolvableDatasource();
		Collection coll = ds.getResult(2, -1, "name", Datasource.SORTORDER_ASC, null);

		verifySortorder(coll, new String[] { "d", "f", "x" }, "getResult with order and start");
	}

	@Test
	public void testGetResultWithSortStartLimit() throws DatasourceNotAvailableException {
		ResolvableDatasource ds = createResolvableDatasource();
		Collection coll = ds.getResult(2, 2, "name", Datasource.SORTORDER_ASC, null);

		verifySortorder(coll, new String[] { "d", "f" }, "getResult with order, start and limit");
	}

	@Test
	public void testGetResultWithoutData() throws DatasourceNotAvailableException {
		ResolvableDatasource ds = new ResolvableDatasource();
		Collection coll = ds.getResult();

		assertEquals("Validating empty list.", 0, ((List) coll).size());
	}

	@Test
	public void testGetResultWithoutDataAndSort() throws DatasourceNotAvailableException {
		ResolvableDatasource ds = new ResolvableDatasource();
		Collection coll = ds.getResult(0, 5, "name", Datasource.SORTORDER_ASC, null);

		assertEquals("Validating empty list.", 0, ((List) coll).size());
	}

	/**
	 * Test filtering from resolvable datasources
	 *
	 * @throws Exception
	 */
	@Test
	public void testResolvableDatasourceFilter() throws Exception {
		ResolvableDatasource ds = createResolvableDatasource();
		DatasourceFilter filter = ds.createDatasourceFilter(ExpressionParser.getInstance().parse("object.number > 5"));
		Collection results = ds.getResult(filter, null);

		assertEquals("Check correct number of filtered objects", 2, results.size());
	}

	@Test
	public void testFilterWithSorting() throws Exception {
		ResolvableDatasource ds = createResolvableDatasource();
		DatasourceFilter filter = ds.createDatasourceFilter(ExpressionParser.getInstance().parse("object.number != 0 && !isempty(object.name)"));
		Collection results = ds.getResult(filter, null, 0, -1, new Datasource.Sorting[] { new Datasource.Sorting("name", Datasource.SORTORDER_ASC) });

		assertEquals("Check correct number of filtered objects", 5, results.size());

		String[] expected = new String[] { "a", "c", "d", "f", "x" };

		verifySortorder(results, expected, "Check whether sorting in the datasource works");
	}

	/**
	 * Test filtering with paging from resolvable datasources
	 *
	 * @throws Exception
	 */
	@Test
	public void testPaging() throws Exception {
		ResolvableDatasource ds = createResolvableDatasource();
		DatasourceFilter filter = ds.createDatasourceFilter(ExpressionParser.getInstance().parse("!isempty(object.name)"));
		Collection results = ds.getResult(filter, null, 0, -1, null);

		assertEquals("Check correct number of filtered objects", 5, results.size());

		results = ds.getResult(filter, null, 2, 2, null);
		assertEquals("Check correct number of filtered objects", 2, results.size());
	}

	private ResolvableDatasource createResolvableDatasource() {
		ResolvableDatasource ds = new ResolvableDatasource();
		String[] testData = new String[] { "x", "a", "c", "f", "d" };
		int[] testNumber = new int[] { 3, 4, 20, 1, 7 };

		for (int i = 0; i < testData.length; i++) {
			Map data = new HashMap();

			data.put("name", testData[i]);
			data.put("number", new Integer(testNumber[i]));
			ds.add(new MapResolver(data));
		}

		return ds;
	}

	private void verifySortorder(Collection coll, String[] order, String testText) {
		assertEquals(testText + ": invalid number of results", order.length, coll.size());
		List list = (List) coll;

		for (int i = 0; i < order.length; i++) {
			Resolvable r = (Resolvable) list.get(i);

			assertEquals(testText + ": Invalid result (wrong order ??)", order[i], r.get("name"));
		}
	}
}
