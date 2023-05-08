package com.gentics.node.tests.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.junit.Test;

import com.gentics.lib.etc.StringUtils;
import org.junit.experimental.categories.Category;

/**
 * Tests for StringUtils
 */
@Category(BaseLibTest.class)
public class StringUtilsTest {

	/**
	 * Tests the insertHtmlIntoHead function
	 */
	@Test
	public void testInsertHtmlIntoHeadTest1() {
		insertHtmlIntoHeadAndCheck("<html><head>", "</head></html>", "<title>Test</title>", true);
	}

	/**
	 * Tests the insertHtmlIntoHead function
	 */
	@Test
	public void testInsertHtmlIntoHeadTest2() {
		insertHtmlIntoHeadAndCheck("<html><head  >", "</head></html>", "<title>Test</title>", true);
	}

	/**
	 * Tests the insertHtmlIntoHead function
	 */
	@Test
	public void testInsertHtmlIntoHeadTest3() {
		insertHtmlIntoHeadAndCheck("<html><head title=\"text\" >", "</head></html>", "<title>Test</title>", true);
	}

	/**
	 * Tests the insertHtmlIntoHead function
	 */
	@Test
	public void testInsertHtmlIntoHeadTest4() {
		insertHtmlIntoHeadAndCheck("<html><head title=\"text\">", "</head></html>", "<title>Test</title>", true);
	}

	/**
	 * Tests the insertHtmlIntoHead function
	 */
	@Test
	public void testInsertHtmlIntoHeadTest5() {
		insertHtmlIntoHeadAndCheck("<html><head title=\"text\" anotherAttribute=\"3\">", "</head></html>", "<title>Test</title>", true);
	}

	/**
	 * Tests the insertHtmlIntoHead function
	 */
	@Test
	public void testInsertHtmlIntoHeadTest6() {
		insertHtmlIntoHeadAndCheck("<html><head title=\"text\" anotherAttribute=\"3\"  >", "</head></html>", "<title>Test</title>", true);
	}

	/**
	 * Tests the insertHtmlIntoHead function
	 */
	@Test
	public void testInsertHtmlIntoHeadTest7() {
		insertHtmlIntoHeadAndCheck("<html><header>", "</header></html>", "<title>Test</title>", false);
	}

	/**
	 * Tests the insertHtmlIntoHead function
	 */
	@Test
	public void testInsertHtmlIntoHeadTest8() {
		insertHtmlIntoHeadAndCheck("<html><  head>", "</head></html>", "<title>Test</title>", false);
	}

	/**
	 * Tests the insertHtmlIntoHead function
	 */
	@Test
	public void testInsertHtmlIntoHeadTest9() {
		insertHtmlIntoHeadAndCheck("<html>", "</html>", "<title>Test</title>", false);
	}

	/**
	 * Tests the insertHtmlIntoHead function with xhtml namespace
	 */
	@Test
	public void testInsertHtmlIntoHeadTest10() {
		insertHtmlIntoHeadAndCheck("<html><h:head>", "</h:head></html>", "<title>Test</title>", true);
	}

	/**
	 * Inserts the given headHtml into the html String concatenated by
	 * beforeHtml and afterHtml. Checks if the insertion was performed
	 * correctly.
	 *
	 * @param beforeHtml
	 *            HTML fragment until opening &lt;head&gt; tag
	 * @param afterHtml
	 *            HTML fragment after opening &lt;head&gt; tag
	 * @param headHtml
	 *            HTML fragment to insert into head
	 * @param shouldBeModified
	 *            indicates if the html should have been modified or not
	 */
	private void insertHtmlIntoHeadAndCheck(String beforeHtml,
			String afterHtml, String headHtml, boolean shouldBeModified) {
		String html = StringUtils.insertHtmlIntoHead(beforeHtml + afterHtml, headHtml);

		if (shouldBeModified) {
			assertEquals("Check if HTML was inserted correctly into the head tag", beforeHtml + "\n" + headHtml + afterHtml, html);
		} else {
			assertFalse("Check if HTML was not modified", html.equals(beforeHtml + "\n" + headHtml + afterHtml));
		}
	}



	/**
	 * This test will add a doctype to a html string that contains no doctype
	 */
	@Test
	public void testInsertDoctype1() {
		String html = "<html><head></head><body></body></html>";
		String result = StringUtils.insertHtml5DocType(html);

		assertFalse(result.equals(html));
	}

	/**
	 * This test will test the ommit of a doctype to a html string that contains
	 * already a doctype
	 */
	@Test
	public void testInsertDoctype2() {
		String doctypeTag = "<!DOCTYPE html>";
		String html = doctypeTag + "<html><head></head><body></body></html>";

		String result = StringUtils.insertHtml5DocType(html);

		assertEquals(html, result);
	}

	/**
	 * This test will test the ommit of a doctype to a html string that contains
	 * already a doctype
	 */
	@Test
	public void testInsertDoctype3() {
		String doctypeTag = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">";
		String html = doctypeTag + "<html><head></head><body></body></html>";

		String result = StringUtils.insertHtml5DocType(html);

		assertEquals(html, result);
	}

	/**
	 * This test will test the ommit of a doctype to a html string that contains
	 * already a doctype
	 */
	@Test
	public void testInsertDoctype4() {
		String doctypeTag = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\"    \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">";
		String html = doctypeTag + "<html><head></head><body></body></html>";

		String result = StringUtils.insertHtml5DocType(html);

		assertEquals(html, result);
	}

	/**
	 * This will test different mysql like string comparisons
	 */
	@Test
	public void testMysqlLikeCompare() {
		assertEquals(-1, StringUtils.mysqlLikeCompare(" aaa", "bbb"));
		assertEquals(-1, StringUtils.mysqlLikeCompare(" bbb", "aaa"));
		assertEquals(1, StringUtils.mysqlLikeCompare("aaa", " bbb"));
		assertEquals(1, StringUtils.mysqlLikeCompare("bbb", " aaa"));
		assertEquals(1, StringUtils.mysqlLikeCompare(" ", ""));
		assertEquals(0, StringUtils.mysqlLikeCompare("", ""));
		assertEquals(-1, StringUtils.mysqlLikeCompare("", " "));
	}
}
