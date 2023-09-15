/**
 * 
 */
package com.gentics.contentnode.tests.publish.gis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.image.CNGenticsImageStore;
import com.gentics.contentnode.image.CNGenticsImageStore.ImageInformation;

/**
 * @author Najor
 */
public class GenticsImageStoreRegExprFilenameTest {

	/**
	 * Test regular expression for Gentics Image Store.
	 * 
	 * @throws NodeException
	 */
	@Test
	public void testFileNamePattern() throws NodeException {
		checkFileNameRegularExpression("200", "230", "smart", "//Content.Node/punta(del)_2.jpeg");
		checkFileNameRegularExpression("2300", "2320", "smart", "//Content.Node/punta(del]_2.jpeg");
		checkFileNameRegularExpression("2003", "2130", "smart", "//Content.Node/pu()[]nta(del]_2.jpeg");
	}

	/**
	 * Test regular expression for Gentics Image Store with white spaces
	 */
	@Test
	public void testFileNamePatternWhiteSpaces() {
		String expectedPath = "//Content.Node/pu()[ ]nta(del]_2.jpeg";
		Matcher matcher = createMatcher("200", "230", "smart", expectedPath);

		assertNotEquals(expectedPath, matcher.group("imageurl"));
	}

	/**
	 * Tests css with no qutoes
	 * 
	 * @throws NodeException
	 */
	@Test
	public void testCSSNoQuotes() throws NodeException {
		String cssTemplate = createCSSTemplateNoQuotes("/Content.Node/punta(some).jpg");
		Matcher matcher = CNGenticsImageStore.SANE_IMAGESTORE_URL_PATTERN.matcher(cssTemplate);

		matcher.find();

		assertPath("/Content.Node/punta(some).jpg", matcher);
	}

	private void assertCDATA(String template, String hostname) throws NodeException {

		String fileName = "/Content.Node/punta(some)]].jpg";

		Matcher matcher = CNGenticsImageStore.SANE_IMAGESTORE_URL_PATTERN.matcher(template.replace("{fileName}", fileName));

		matcher.find();

		if (hostname != null) {
			fileName = hostname + fileName;
		}

		assertPath(fileName, matcher);
	}

	@Test
	public void testCDATA() throws NodeException {
		assertCDATA("<![CDATA[//adfasd/GenticsImageStore/200/230/smart{fileName}]]", "adfasd");
		assertCDATA("<![ CDATA[ asdf/asdf/GenticsImageStore/200/230/smart{fileName}] ]", null);
		assertCDATA("<![ CDATA[/GenticsImageStore/200/230/smart{fileName}] ]", null);
		assertCDATA("<![ CDATA[//some-thing.at.com/stop_pan_thing/GenticsImageStore/200/230/smart{fileName} ]]", null);
		assertCDATA("<![ CDATA[ /GenticsImageStore/200/230/smart{fileName} ]]", null);
		assertCDATA("<![ CDATA [/GenticsImageStore/200/230/smart{fileName} ]]", null);
		assertCDATA("<![ CDATA [ ///GenticsImageStore/200/230/smart{fileName}] ]", null);
		assertCDATA("<![ CDATA [ /GenticsImageStore/200/230/smart{fileName}  ] ]", null);
	}

	/**
	 * Test CSS with quotes.
	 * 
	 * @throws NodeException
	 */
	@Test
	public void testCSSWithQuotes() throws NodeException {
		String pathExpected1 = "/Content.Node/punta(some).jpg";
		String pathExpected2 = "/Content.Node/punta(some]}][]}{)()/&).jpg";

		assertEquals(pathExpected1, getPathFromCSSTemplate(pathExpected1));
		assertEquals(pathExpected2, getPathFromCSSTemplate(pathExpected2));
	}

	/**
	 * Test for CSS.
	 * 
	 * @throws NodeException
	 */
	@Test
	public void testCSS() throws NodeException {
		String pathExpected = "/Content.Node/punta(s]ome).jpg";

		String cssWhiteSpaces = "background-image:url(/GenticsImageStore/200/230/smart" + pathExpected + ") url(asdfsad);";
		String cssSemicolon = "background-image:url(/GenticsImageStore/200/230/smart" + pathExpected + ");";
		String cssMoreAttrs = "background: url(/GenticsImageStore/200/230/smart" + pathExpected + ") black;";
		String cssMoreAttrsNoWhiteSpace = "background: url(/GenticsImageStore/200/230/smart" + pathExpected + ")black;";
		String cssCapitalize = "background: URL(/GenticsImageStore/200/230/smart" + pathExpected + ")black;";

		assertEqualPath(pathExpected, cssWhiteSpaces);
		assertEqualPath(pathExpected, cssMoreAttrs);
		assertEqualPath(pathExpected, cssSemicolon);
		assertEqualPath(pathExpected, cssMoreAttrsNoWhiteSpace);
		assertEqualPath(pathExpected, cssCapitalize);
	}

	@Test
	public void testStrangeUrl() throws NodeException {
		List<String> urls = Arrays.asList("=\"1\"><a href=\"https://hostname\"><img src=\"/GenticsImageStore/540/230/smart/path/to/image.jpg\"/></a><div>\"",
				"=\"1\"><a href=\"https://hostname/\"><img src=\"/GenticsImageStore/540/230/smart/path/to/image.jpg\"/></a><div>\"");
		for (String url : urls) {
			Matcher matcher = CNGenticsImageStore.SANE_IMAGESTORE_URL_PATTERN.matcher(url);
			matcher.find();
			assertPath("/path/to/image.jpg", matcher);
		}
	}

	/**
	 * Checks if pathExpected is correctly recognized by Gentics image regular
	 * expression.
	 * 
	 * @param pathExpected
	 * @param input
	 * @throws NodeException
	 */
	private void assertEqualPath(String pathExpected, String input) throws NodeException {
		Matcher matcher = CNGenticsImageStore.SANE_IMAGESTORE_URL_PATTERN.matcher(input);

		matcher.find();

		assertPath(pathExpected, matcher);
	}

	/**
	 * Gets the path from a CSS template.
	 * 
	 * @param pathExpected
	 * @return
	 * @throws NodeException
	 */
	private String getPathFromCSSTemplate(String pathExpected) throws NodeException {
		String cssTemplate = createCSSTemplateWithQuotes(pathExpected);
		Matcher matcher = CNGenticsImageStore.SANE_IMAGESTORE_URL_PATTERN.matcher(cssTemplate);

		matcher.find();

		checkAssertsMatcher("200", "230", "smart", pathExpected, matcher);

		return matcher.group("imageurl");
	}

	/**
	 * Checks that all parameters are recognized by the regular expression
	 * 
	 * @param expectedWidth
	 * @param expectedHeight
	 * @param expectedMode
	 * @param expectedPath
	 * @throws NodeException
	 */
	private void checkFileNameRegularExpression(String expectedWidth, String expectedHeight, String expectedMode, String expectedPath) throws NodeException {
		Matcher matcher = createMatcher(expectedWidth, expectedHeight, expectedMode, expectedPath);

		checkAssertsMatcher(expectedWidth, expectedHeight, expectedMode, expectedPath, matcher);
	}

	/**
	 * Checks matcher for the expected values.
	 * 
	 * @param expectedWidth
	 * @param expectedHeight
	 * @param expectedMode
	 * @param expectedPath
	 * @param matcher
	 * @throws NodeException
	 */
	private void checkAssertsMatcher(String expectedWidth, String expectedHeight, String expectedMode, String expectedPath, Matcher matcher)
			throws NodeException {
		String hostname = matcher.group("host");
		String width = matcher.group("width");
		String height = matcher.group("height");
		String mode = matcher.group("mode");

		assertNull("Hostname is null", hostname);
		assertEquals(expectedWidth, width);
		assertEquals(expectedHeight, height);
		assertEquals(expectedMode, mode);
		assertPath(expectedPath, matcher);
	}

	/**
	 * Creates a matcher from the image pattern.
	 * 
	 * @param expectedWidth
	 * @param expectedHeight
	 * @param expectedMode
	 * @param expectedPath
	 * @return
	 */
	private Matcher createMatcher(String expectedWidth, String expectedHeight, String expectedMode, String expectedPath) {
		String input = createTemplate(expectedWidth, expectedHeight, expectedMode, expectedPath);

		Matcher matcher = CNGenticsImageStore.SANE_IMAGESTORE_URL_PATTERN.matcher(input);

		assertTrue(matcher.find());
		return matcher;
	}

	/**
	 * Creates a CSS template with No Quotes in the beginning and end of url.
	 * 
	 * @param fileName
	 * @return
	 */
	private String createCSSTemplateNoQuotes(String fileName) {
		return ".dum {" + "background-image:url(/GenticsImageStore/200/230/smart" + fileName + ");" + "background-color:#cccccc;";
	}

	/**
	 * Creates a CSS template with Quotes in the beginning and end of url.
	 * 
	 * @param fileName
	 * @return
	 */
	private String createCSSTemplateWithQuotes(String fileName) {
		return ".dum {" + "background-image:url('/GenticsImageStore/200/230/smart" + fileName + "')url(dw);" + "background-color:#cccccc;";
	}

	/**
	 * Creates Gentics Image Store template example.
	 * 
	 * @param width
	 * @param height
	 * @param mode
	 * @param path
	 * @return
	 */
	private String createTemplate(String width, String height, String mode, String path) {
		return "<html><head><meta name='keyword' content='Gentics Content.Node(R) by Gentics (http://www.gentics.com/)' /></head>"
				+ "<body>/GenticsImageStore/" + width + "/" + height + "/" + mode + path + "</body></html>";
	}

	/**
	 * Assert that the url from the matcher matches the expected
	 * 
	 * @param expected
	 *            expected URL
	 * @param matcher
	 *            matcher
	 * @throws NodeException
	 */
	private void assertPath(String expected, Matcher matcher) throws NodeException {
		String hostname = matcher.group("host");
		if (hostname == null) {
			hostname = "";
		}
		String imageUrl = matcher.group("imageurl");
		String fullPath = expected.replaceAll("//+", "/");
		Map<String, ImageInformation> allImageData = new HashMap<String, CNGenticsImageStore.ImageInformation>();
		allImageData.put(fullPath, new ImageInformation(1, 1, expected, 1));
		assertNotNull(hostname + imageUrl + " does not match expected path " + expected, CNGenticsImageStore.getImage(hostname, imageUrl, allImageData));
	}
}
