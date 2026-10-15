package com.gentics.contentnode.tests.aloha;

import static org.junit.Assert.assertEquals;

import com.gentics.contentnode.aloha.TagMarkers;
import org.junit.Test;

/**
 * Tests for {@link TagMarkers}
 */
public class TagMarkersTest {
	/**
	 * Annotations of the marked tag
	 */
	private final static String ANNOTATIONS = "data-gcn-pageid=\"556\" data-gcn-tagid=\"124\" data-gcn-tagname=\"dummy124\"";

	/**
	 * Marker, which the tag gets, when it renders only text
	 */
	private final static String MARKER_START = "<!--gtxtagmarker " + ANNOTATIONS + "-->";

	private final static String MARKER_END = "<!--/gtxtagmarker-->";

	private static String mark(String code) {
		return TagMarkers.mark(code, ANNOTATIONS);
	}

	@Test
	public void singleRoot() {
		assertEquals("<div " + ANNOTATIONS + " class=\"teaser\"><p>Text</p></div>", mark("<div class=\"teaser\"><p>Text</p></div>"));
	}

	@Test
	public void voidRoot() {
		assertEquals("<img " + ANNOTATIONS + " src=\"/a.jpg\" alt=\"\">", mark("<img src=\"/a.jpg\" alt=\"\">"));
	}

	@Test
	public void selfClosingRoot() {
		assertEquals("<img " + ANNOTATIONS + " src=\"/a.jpg\"/>", mark("<img src=\"/a.jpg\"/>"));
	}

	@Test
	public void whiteSpaceAroundRoot() {
		assertEquals("\n  <h1 " + ANNOTATIONS + ">Title</h1>\n", mark("\n  <h1>Title</h1>\n"));
	}

	@Test
	public void rootInOtherCase() {
		assertEquals("<DIV " + ANNOTATIONS + ">Text</div>", mark("<DIV>Text</div>"));
	}

	@Test
	public void siblings() {
		assertEquals("<p " + ANNOTATIONS + ">One</p>\n<img " + ANNOTATIONS + " src=\"/a.jpg\">\n<!-- comment --><p " + ANNOTATIONS + ">Two</p>",
				mark("<p>One</p>\n<img src=\"/a.jpg\">\n<!-- comment --><p>Two</p>"));
	}

	@Test
	public void nestedElementWithSameName() {
		assertEquals("<div " + ANNOTATIONS + "><div>Inner</div></div>", mark("<div><div>Inner</div></div>"));
	}

	@Test
	public void elementWithPrefixedNameInside() {
		assertEquals("<p " + ANNOTATIONS + ">Code: <pre>x</pre></p>", mark("<p>Code: <pre>x</pre></p>"));
	}

	@Test
	public void alreadyAnnotatedRoot() {
		String code = "<img data-gcn-pageid=\"556\" data-gcn-tagid=\"127\" data-gcn-tagname=\"inner\" src=\"/a.jpg\">";

		assertEquals(code, mark(code));
	}

	@Test
	public void textOnly() {
		assertEquals(" " + MARKER_START + "Some text" + MARKER_END + "\n", mark(" Some text\n"));
	}

	@Test
	public void mixedTextAndElements() {
		assertEquals("Some <b>bold</b> text", mark("Some <b>bold</b> text"));
	}

	@Test
	public void unclosedElement() {
		assertEquals("<div>Text", mark("<div>Text"));
	}

	@Test
	public void closingTagWithoutElement() {
		assertEquals("Text</span>", mark("Text</span>"));
	}

	@Test
	public void unclosedComment() {
		assertEquals("<!-- comment <p>Text</p>", mark("<!-- comment <p>Text</p>"));
	}

	@Test
	public void empty() {
		assertEquals("\n", mark("\n"));
	}

	@Test
	public void commentOnly() {
		assertEquals("<!-- comment -->", mark("<!-- comment -->"));
	}

	@Test
	public void resolveMarkerInParent() {
		assertEquals("<h1 " + ANNOTATIONS + " class=\"title\"> Title </h1>",
				TagMarkers.resolve("<h1 class=\"title\"> " + MARKER_START + "Title" + MARKER_END + " </h1>"));
	}

	@Test
	public void resolveMarkerInAnnotatedParent() {
		assertEquals("<h1 data-gcn-tagid=\"1\">Title</h1>",
				TagMarkers.resolve("<h1 data-gcn-tagid=\"1\">" + MARKER_START + "Title" + MARKER_END + "</h1>"));
	}

	@Test
	public void resolveMarkerBesideOtherContent() {
		assertEquals("<p>Price: 5 EUR</p>", TagMarkers.resolve("<p>Price: " + MARKER_START + "5" + MARKER_END + " EUR</p>"));
		assertEquals("<p><b>Price:</b>5</p>", TagMarkers.resolve("<p><b>Price:</b>" + MARKER_START + "5" + MARKER_END + "</p>"));
	}

	@Test
	public void resolveMarkerInAttribute() {
		assertEquals("<img alt=\"Text\" src=\"/a.jpg\">",
				TagMarkers.resolve("<img alt=\"" + MARKER_START + "Text" + MARKER_END + "\" src=\"/a.jpg\">"));
	}

	@Test
	public void resolveMarkerAtStart() {
		assertEquals("Text", TagMarkers.resolve(MARKER_START + "Text" + MARKER_END));
	}

	@Test
	public void resolveNestedMarkers() {
		String outer = "<!--gtxtagmarker data-gcn-tagid=\"2\" data-gcn-tagname=\"outer\"-->";

		assertEquals("<span data-gcn-tagid=\"2\" data-gcn-tagname=\"outer\">Text</span>",
				TagMarkers.resolve("<span>" + outer + MARKER_START + "Text" + MARKER_END + MARKER_END + "</span>"));
	}

	@Test
	public void resolveSeveralMarkers() {
		String other = "<!--gtxtagmarker data-gcn-tagid=\"2\" data-gcn-tagname=\"other\"-->";

		assertEquals("<h1 " + ANNOTATIONS + ">Title</h1><p data-gcn-tagid=\"2\" data-gcn-tagname=\"other\">Text</p>",
				TagMarkers.resolve("<h1>" + MARKER_START + "Title" + MARKER_END + "</h1><p>" + other + "Text" + MARKER_END + "</p>"));
	}

	@Test
	public void resolveWithoutMarkers() {
		String template = "<html><body><p>Text</p></body></html>";

		assertEquals(template, TagMarkers.resolve(template));
	}
}
