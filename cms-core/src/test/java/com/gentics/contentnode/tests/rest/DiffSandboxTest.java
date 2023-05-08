/**
 * 
 */
package com.gentics.contentnode.tests.rest;

import static org.junit.Assert.assertEquals;

import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.contentnode.rest.model.request.DaisyDiffRequest;
import com.gentics.contentnode.rest.model.request.DiffRequest;
import com.gentics.contentnode.rest.model.response.DiffResponse;
import com.gentics.contentnode.rest.resource.impl.DiffResourceImpl;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Tests for using the diff tool for calculating diffs between HTML contents
 */
public class DiffSandboxTest {

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	public static final String DIFF_IGNORE = "(time=[0-9]+|\u200b)";

	/**
	 * Test diff with inserted code into HTML content
	 */
	@Test
	public void testDiffHTMLWithInsert() {
		DiffResourceImpl diffResource = new DiffResourceImpl();
		DiffRequest request = new DiffRequest();

		request.setContent1("<span>one two four five</span>");
		request.setContent2("<span>one two three four five</span>");
		DiffResponse response = diffResource.diffHTML(request);

		assertEquals("Check diff", "<span>one two <ins class='diff modified gtx-diff'>three </ins>four five</span>", response.getDiff());
	}

	/**
	 * Test diff with removed code from HTML content
	 */
	@Test
	public void testDiffHTMLWithRemove() {
		DiffResourceImpl diffResource = new DiffResourceImpl();

		DiffRequest request = new DiffRequest();

		request.setContent1("<span>one two three four five</span>");
		request.setContent2("<span>one two four five</span>");
		DiffResponse response = diffResource.diffHTML(request);

		assertEquals("Check diff", "<span>one two <del class='diff modified gtx-diff'>three </del>four five</span>", response.getDiff());
	}

	/**
	 * Test diff with changed code in HTML content
	 */
	@Test
	public void testDiffHTMLWithChange() {
		DiffResourceImpl diffResource = new DiffResourceImpl();
		DiffRequest request = new DiffRequest();

		request.setContent1("<span>one two three four five</span>");
		request.setContent2("<span>one two THREE four five</span>");
		DiffResponse response = diffResource.diffHTML(request);

		assertEquals("Check diff",
				"<span>one two <del class='diff modified gtx-diff'>three</del><ins class='diff modified gtx-diff'>THREE</ins> four five</span>",
				response.getDiff());
	}

	/**
	 * Test diff with inserted code into HTML content using a custom template
	 */
	@Test
	public void testDiffHTMLWithInsertTemplate() {
		DiffResourceImpl diffResource = new DiffResourceImpl();
		DiffRequest request = new DiffRequest();

		request.setContent1("<span>one two four five</span>");
		request.setContent2("<span>one two three four five</span>");
		request.setInsertTemplate("[$insert]");
		DiffResponse response = diffResource.diffHTML(request);

		assertEquals("Check diff", "<span>one two [three ]four five</span>", response.getDiff());

		// test again using a template with wrong placeholder
		request.setInsertTemplate("[$remove]");
		response = diffResource.diffHTML(request);
		assertEquals("Check diff", "<span>one two []four five</span>", response.getDiff());
	}

	/**
	 * Test diff with removed code from HTML content
	 */
	@Test
	public void testDiffHTMLWithRemoveTemplate() {
		DiffResourceImpl diffResource = new DiffResourceImpl();
		DiffRequest request = new DiffRequest();

		request.setContent1("<span>one two three four five</span>");
		request.setContent2("<span>one two four five</span>");
		request.setRemoveTemplate("[$remove]");
		DiffResponse response = diffResource.diffHTML(request);

		assertEquals("Check diff", "<span>one two [three ]four five</span>", response.getDiff());

		// test again using a template with wrong placeholder
		request.setRemoveTemplate("[$insert]");
		response = diffResource.diffHTML(request);
		assertEquals("Check diff", "<span>one two []four five</span>", response.getDiff());
	}

	/**
	 * Test diff with changed code in HTML content
	 */
	@Test
	public void testDiffHTMLWithChangeTemplate() {
		DiffResourceImpl diffResource = new DiffResourceImpl();
		DiffRequest request = new DiffRequest();

		request.setContent1("<span>one two three four five</span>");
		request.setContent2("<span>one two THREE four five</span>");
		request.setChangeTemplate("[$remove|$insert]");
		DiffResponse response = diffResource.diffHTML(request);

		assertEquals("Check diff", "<span>one two [three|THREE] four five</span>", response.getDiff());

		// test again using a different template
		request.setChangeTemplate("[$insert|$remove]");
		response = diffResource.diffHTML(request);
		assertEquals("Check diff", "<span>one two [THREE|three] four five</span>", response.getDiff());
	}

	/**
	 * Test diff with showing X words before
	 */
	@Test
	public void testDiffWordsBefore() {
		DiffResourceImpl diffResource = new DiffResourceImpl();
		DiffRequest request = new DiffRequest();

		request.setContent1("<span>1 2 3 4 5 6 7 8 9 10 11 12 13 14 15</span>");
		request.setContent2("<span>1 2 3 4 5 6 7 8 9 10 11 12 13 NEW 14 15</span>");
		request.setInsertTemplate("[($before) $insert]");

		// first test with the default value (10 words, spaces count as words)
		DiffResponse response = diffResource.diffHTML(request);

		assertEquals("Check diff", "<span>1 2 3 4 5 6 7 8 9 10 11 12 13 [(9 10 11 12 13 ) NEW ]14 15</span>", response.getDiff());

		// now test again with a different number of words before
		request.setWordsBefore(2);
		response = diffResource.diffHTML(request);
		assertEquals("Check diff", "<span>1 2 3 4 5 6 7 8 9 10 11 12 13 [(13 ) NEW ]14 15</span>", response.getDiff());
	}

	/**
	 * Test diff with showing X words after
	 */
	@Test
	public void testDiffWordsAfter() {
		DiffResourceImpl diffResource = new DiffResourceImpl();
		DiffRequest request = new DiffRequest();

		request.setContent1("<span>1 2 3 4 5 6 7 8 9 10 11 12 13 14 15</span>");
		request.setContent2("<span>1 2 NEW 3 4 5 6 7 8 9 10 11 12 13 14 15</span>");
		request.setInsertTemplate("[$insert ($after)]");

		// first test with the default value (10 words, spaces count as words)
		DiffResponse response = diffResource.diffHTML(request);

		assertEquals("Check diff", "<span>1 2 [NEW  (3 4 5 6 7 )]3 4 5 6 7 8 9 10 11 12 13 14 15</span>", response.getDiff());

		// now test again with a different number of words before
		request.setWordsAfter(2);
		response = diffResource.diffHTML(request);
		assertEquals("Check diff", "<span>1 2 [NEW  (3 )]3 4 5 6 7 8 9 10 11 12 13 14 15</span>", response.getDiff());
	}

	/**
	 * Test whether Zerowidth Space is ignored (using the ignore regex that is used when doing version diff in the CMS)
	 */
	@Test
	public void testDiffZeroWidthSpace() {
		DiffResourceImpl diffResource = new DiffResourceImpl();
		DiffRequest request = new DiffRequest();

		request.setContent1("<p>one two three</p>");
		request.setContent2("<p>one two three\u200B</p>");
		request.setIgnoreRegex(DIFF_IGNORE);

		DiffResponse response = diffResource.diffHTML(request);
		assertEquals("Check diff", "<p>one two three</p>", response.getDiff());
	}

	/**
	 * Test whether Zerowidth Space is ignored using daisy diff
	 */
	@Test
	public void testDaisyDiffZeroWidthSpace() {
		DiffResourceImpl diffResource = new DiffResourceImpl();
		DaisyDiffRequest request = new DaisyDiffRequest();

		request.setOlder("<p>one two three</p>");
		request.setNewer("<p>one two three\u200B</p>");
		request.setIgnoreRegex(DIFF_IGNORE);

		DiffResponse response = diffResource.daisyDiff(request);
		assertEquals("Check diff", "<div id=\"gtxDiffWrapper\">\n<p>one two three</p>\n</div>\n", response.getDiff());
	}
}
