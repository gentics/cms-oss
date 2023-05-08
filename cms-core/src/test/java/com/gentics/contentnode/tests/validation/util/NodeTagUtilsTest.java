package com.gentics.contentnode.tests.validation.util;

import junit.framework.TestCase;

import com.gentics.contentnode.validation.util.NodeTagUtils;

/**
 * TODO: it was a bad idea to test by iterating over an array of test data.
 *   this should be implemented by writing one test method for each test case,
 *   and the test data should be inline.
 */
public class NodeTagUtilsTest extends TestCase {
	protected static final String[] GTXTIFIABLE_NODE_TAGS = new String[] {
		// node tags with non-matching quotes are left as-is.
		"<p><node " + NodeTagUtils.NODE_TAG_ATTRIBUTE + "='tag.name\"></p>", // node tags with invalid tag names are left as-is.
		"<node " + NodeTagUtils.NODE_TAG_ATTRIBUTE + "='!node.tag'>", // node tags contained in attributes should be converted; also tests single quotes.
		"<p><a href=\"<node " + NodeTagUtils.NODE_TAG_ATTRIBUTE + " = 'tag.name' />\" />", // the same but for double quotes; closing tags shouldn't interfere.
		"<p><a href='<node " + NodeTagUtils.NODE_TAG_ATTRIBUTE + " = \"tag.name\" >'>some text</a>", // several node tags; also tests valid non-alphabetic characters in tag names.
		"<p><node " + NodeTagUtils.NODE_TAG_ATTRIBUTE + "=\"o:n_e\"><p><node " + NodeTagUtils.NODE_TAG_ATTRIBUTE + "='t-w.o'>", // newline instead of whitespace as delimiter
		"<node\n" + NodeTagUtils.NODE_TAG_ATTRIBUTE + "='tag.name'>"
	};
	// mirrors GTXTIFIABLE_NODE_TAGS
	protected static final String[] GTXTIFIED_NODE_TAGS = new String[] {
		GTXTIFIABLE_NODE_TAGS[0], GTXTIFIABLE_NODE_TAGS[1], "<p><a href=\"<node tag.name>\" />", "<p><a href='<node tag.name>'>some text</a>",
		"<p><node o:n_e><p><node t-w.o>", "<node tag.name>"
	};
	// mirrors GTXTFIED_NODE_TAGS
	protected static final String[] UNGTXTIFIED_NODE_TAGS = new String[] {
		GTXTIFIABLE_NODE_TAGS[0], GTXTIFIABLE_NODE_TAGS[1], "<p><a href=\"<node " + NodeTagUtils.NODE_TAG_ATTRIBUTE + "=\"tag.name\"/>\" />",
		"<p><a href='<node " + NodeTagUtils.NODE_TAG_ATTRIBUTE + "=\"tag.name\"/>'>some text</a>",
		"<p><node " + NodeTagUtils.NODE_TAG_ATTRIBUTE + "=\"o:n_e\"/><p><node " + NodeTagUtils.NODE_TAG_ATTRIBUTE + "=\"t-w.o\"/>",
		"<node " + NodeTagUtils.NODE_TAG_ATTRIBUTE + "=\"tag.name\"/>"
	};
	
	protected static final String[] TIDIED_NODE_TAGS = new String[] {
		"<node tag='x'/>", // only empty attributes are converted
		"<node tag.name=\"\" />", // tests double quotes and closing slash
		"<node tag.name='' >"     // tests single quotes and without closing slash (but spaces!)
	};
	// mirrors TIDIED_NODE_TAGS
	protected static final String[] GTXTIFIED_TIDIED_NODE_TAGS = new String[] {
		TIDIED_NODE_TAGS[0], "<node tag.name>", "<node tag.name>"
	};
	
	public void testGtxtification() {
		for (int i = 0; i < GTXTIFIABLE_NODE_TAGS.length; i++) {
			String gtxtified = NodeTagUtils.gtxtifyNodeTags(GTXTIFIABLE_NODE_TAGS[i]);
			String ungtxtified = NodeTagUtils.ungtxtifyNodeTags(GTXTIFIED_NODE_TAGS[i]);

			assertEquals("gtxtification is broken,", GTXTIFIED_NODE_TAGS[i], gtxtified);
			assertEquals("ungtxtification is broken,", UNGTXTIFIED_NODE_TAGS[i], ungtxtified); 
		}
	}
	
	public void testTextification() {
		for (int i = 0; i < GTXTIFIED_NODE_TAGS.length; i++) {
			String gtxtifiedExpected = GTXTIFIED_NODE_TAGS[i];

			if (GTXTIFIABLE_NODE_TAGS[i].equals(gtxtifiedExpected)) {
				continue;
			}
			String textified = NodeTagUtils.textifyNodeTags(gtxtifiedExpected);
			String untextified = NodeTagUtils.untextifyNodeTags(textified);

			assertNotSame("textification didn't do anything,", gtxtifiedExpected, textified);
			assertEquals("untextification is broken,", gtxtifiedExpected, untextified);
		}
	}
	
	public void testGtxtifyTidiedNodeTags() {
		for (int i = 0; i < TIDIED_NODE_TAGS.length; i++) {
			String tidiedExpected = TIDIED_NODE_TAGS[i];
			String gtxtifiedExpected = GTXTIFIED_TIDIED_NODE_TAGS[i];
			String gtxtified = NodeTagUtils.gtxtifyTidiedNodeTags(tidiedExpected);

			assertEquals("gtxtification of tidied node tags is broken,", gtxtifiedExpected, gtxtified);
		}
	}
}
