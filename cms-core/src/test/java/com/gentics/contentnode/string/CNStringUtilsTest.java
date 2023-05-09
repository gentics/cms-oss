package com.gentics.contentnode.string;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.regex.Pattern;

import org.junit.Test;

public class CNStringUtilsTest {

	@Test
	public void testDaisyDiffFullHtml() throws Exception {
		String diff = CNStringUtils
				.daisyDiff(
						"<html><head><title>older</title></head><body>some<p>text</body>",
						"<html><head><title>newer</title></head><body id=\"the>Body\">some other<p>text and some<br/><br/> elements</body>");
		diff = diff.replaceAll("\\r\\n", "\n");

		assertEquals(
				"<html><head><title>newer</title></head><body id=\"the>Body\"><div id=\"gtxDiffWrapper\">some <span changeId=\"added-gtxDiff-0\" class=\"diff-html-added\" id=\"added-gtxDiff-0\" next=\"added-gtxDiff-1\" previous=\"first-gtxDiff\">other</span>"
						+ "\n<p>text <span changeId=\"added-gtxDiff-1\" class=\"diff-html-added\" id=\"added-gtxDiff-1\" next=\"last-gtxDiff\" previous=\"added-gtxDiff-0\">and some</span>"
						+ "\n<br>"
						+ "\n<br>"
						+ "\n<span changeId=\"added-gtxDiff-1\" class=\"diff-html-added\" next=\"last-gtxDiff\" previous=\"added-gtxDiff-0\"> elements</span>"
						+ "\n</p>" + "\n</div>" + "\n</body></html>", diff);
	}

	@Test
	public void testDaisyDiffFragmentHtml() throws Exception {
		String diff = CNStringUtils.daisyDiff(
				"<p>a paragraph<p>another paragraph",
				"<p>yet another paragraph<p>and yet another");
		diff = diff.replaceAll("\\r\\n", "\n");

		assertEquals(
				"<div id=\"gtxDiffWrapper\">"
						+ "\n<p>"
						+ "\n<span changeId=\"removed-gtxDiff-0\" class=\"diff-html-removed\" id=\"removed-gtxDiff-0\" next=\"added-gtxDiff-0\" previous=\"first-gtxDiff\">a </span><span changeId=\"added-gtxDiff-0\" class=\"diff-html-added\" id=\"added-gtxDiff-0\" next=\"added-gtxDiff-1\" previous=\"removed-gtxDiff-0\">yet another </span>paragraph</p>"
						+ "\n<p>"
						+ "\n<span changeId=\"added-gtxDiff-1\" class=\"diff-html-added\" id=\"added-gtxDiff-1\" next=\"removed-gtxDiff-1\" previous=\"added-gtxDiff-0\">and yet </span>another<span changeId=\"removed-gtxDiff-1\" class=\"diff-html-removed\" next=\"last-gtxDiff\" previous=\"added-gtxDiff-1\"> </span><span changeId=\"removed-gtxDiff-1\" class=\"diff-html-removed\" id=\"removed-gtxDiff-1\" next=\"last-gtxDiff\" previous=\"added-gtxDiff-1\">paragraph</span>"
						+ "\n</p>" + "\n</div>\n", diff);
	}

	@Test
	public void testDaisyDiffCdataInHeader() throws Exception {
		String diff = CNStringUtils
				.daisyDiff(
						"<html><head><title><![CDATA[older title<body>]]></title></head><body>older text</body></html>",
						"<html><head><title><![CDATA[newer title<body>]]></title></head><body>newer text</body></html>");
		diff = diff.replaceAll("\\r\\n", "\n");

		assertEquals(
				"<html><head><title>newer title&lt;body&gt;</title></head><body><div id=\"gtxDiffWrapper\">"
						+ "\n<span changeId=\"removed-gtxDiff-0\" class=\"diff-html-removed\" id=\"removed-gtxDiff-0\" next=\"added-gtxDiff-0\" previous=\"first-gtxDiff\">older </span><span changeId=\"added-gtxDiff-0\" class=\"diff-html-added\" id=\"added-gtxDiff-0\" next=\"last-gtxDiff\" previous=\"removed-gtxDiff-0\">newer </span>text</div>"
						+ "\n</body></html>", diff);
	}

	@Test
	public void testDaisyDiffCdataInBody() throws Exception {
		String diff = CNStringUtils
				.daisyDiff(
						"<html><head><title>older title></title></head><body><![CDATA[<body>older text]]></body></html>",
						"<html><head><title>newer title></title></head><body><![CDATA[<body>newer text]]></body></html>");
		diff = diff.replaceAll("\\r\\n", "\n");

		assertEquals(
				"<html><head><title>newer title></title></head><body><div id=\"gtxDiffWrapper\">"
						+ "\n<span changeId=\"removed-gtxDiff-0\" class=\"diff-html-removed\" id=\"removed-gtxDiff-0\" next=\"added-gtxDiff-0\" previous=\"first-gtxDiff\">&lt;body&gt;older </span><span changeId=\"added-gtxDiff-0\" class=\"diff-html-added\" id=\"added-gtxDiff-0\" next=\"last-gtxDiff\" previous=\"removed-gtxDiff-0\">&lt;body&gt;newer </span>text</div>"
						+ "\n</body></html>", diff);
	}

	@Test
	public void testEscapeRegex() throws Exception {
		String orig = "/\\start \\\\replacing here!->^$.?*+()[]{}|";
		String escaped = CNStringUtils.escapeRegex(orig);

		assertTrue(
			"The escaped string \"" + escaped + "\" did not match agains the original string \"" + orig + "\"",
			Pattern.matches(escaped, orig));
	}
}
