package com.gentics.node.tests.utils;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.junit.Test;

import com.gentics.lib.util.QueryStringUtils;
import org.junit.experimental.categories.Category;

@Category(BaseLibTest.class)
public class QueryStringUtilsTest {

	/**
	 * The encdoing to use for parsing/building the query string.
	 */
	private static final String encoding = "UTF-8";

	/**
	 * An arbitrary query string that contains multiple parameters and multiple
	 * values for one parameter. It also contains a parameter with an empty
	 * value, and a parameter without an equals sign (and without value). It
	 * also cotains URL-encoded characters.
	 */
	private static final String arbitraryQueryString = "name=value&name1=value11&name1=value12&name1=value13&name2=value2" + "&emptyValue=&noValue"
			+ "&encoded%20name=encoded+value&nameWith%3DEquals=%3D";

	/**
	 * The parsed version of {@link #arbitraryQueryString}. We must
	 */
	private static final Map<String, String[]> arbitraryQueryParams = new LinkedHashMap<String, String[]>();
	static {
		arbitraryQueryParams.put("name", new String[] { "value" });
		arbitraryQueryParams.put("name1", new String[] { "value11", "value12", "value13" });
		arbitraryQueryParams.put("name2", new String[] { "value2" });
		arbitraryQueryParams.put("emptyValue", new String[] { "" });
		arbitraryQueryParams.put("noValue", new String[] { "" });
		arbitraryQueryParams.put("encoded name", new String[] { "encoded value" });
		arbitraryQueryParams.put("nameWith=Equals", new String[] { "=" });
	}

	/**
	 * The query string that should come out of invoking {@link
	 * QueryStringUtils#buildQueryString(Map<String,String[]>)} with the query
	 * params above. This is almost exactly like {@link #arbitraryQueryString}
	 * but %20 escapes replaced with + and an equals sign added to the value
	 * without equals sign. We need this separate because information will be
	 * lost on encoding a query param map.
	 */
	private static final String arbitraryQueryStringRebuilt = "name=value&name1=value11&name1=value12&name1=value13&name2=value2" + "&emptyValue=&noValue="
			+ "&encoded+name=encoded+value&nameWith%3DEquals=%3D";

	@Test
	public void testParseArbitraryQueryString() throws Exception {
		Map<String, String[]> params = QueryStringUtils.parseQueryString(arbitraryQueryString, encoding);

		assertQueryParamsMatch(params, arbitraryQueryParams);
	}

	@Test
	public void testBuildArbitraryQueryString() throws Exception {
		String queryString = QueryStringUtils.buildQueryString(arbitraryQueryParams, encoding);

		assertEquals("The built query string doesn't match expected query string", arbitraryQueryStringRebuilt, queryString);
	}

	@Test
	public void testParseEmptyQueryString() throws Exception {
		Map<String, String[]> params = QueryStringUtils.parseQueryString("", encoding);

		assertEquals("An empty query string should parse to an empty param map", 0, params.size());
	}

	@Test
	public void testBuildEmptyQueryString() throws Exception {
		HashMap<String, String[]> emptyParams = new HashMap<String, String[]>();
		String queryString = QueryStringUtils.buildQueryString(emptyParams, encoding);

		assertEquals("An empty param map should parse to an empty query string", "", queryString);
	}

	protected void assertQueryParamsMatch(Map<String, String[]> expected,
			Map<String, String[]> actual) {
		// if the left is contained in the right, and the right
		// is contained in the left, they must be exactly equal.
		assertQueryParamsLeftContainedInRight(expected, actual);
		assertQueryParamsLeftContainedInRight(actual, expected);
	}

	protected void assertQueryParamsLeftContainedInRight(
			Map<String, String[]> paramsA, Map<String, String[]> paramsB) {
		for (Map.Entry<String, String[]> paramA : paramsA.entrySet()) {
			String keyA = paramA.getKey();
			String[] valuesA = paramA.getValue();
			String[] valuesB = paramsB.get(keyA);

			assertArrayEquals("the parameter values for parameter `" + keyA + "' don't match", valuesA, valuesB);
		}
	}
}
