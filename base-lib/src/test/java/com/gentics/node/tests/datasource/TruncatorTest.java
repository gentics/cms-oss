/*
 * @author jan
 * @date Jul 29, 2008
 * @version $Id: TruncatorTest.java,v 1.1 2010-02-04 14:25:05 norbert Exp $
 */
package com.gentics.node.tests.datasource;

import static org.junit.Assert.assertEquals;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.junit.Test;

import com.gentics.lib.db.ByteCountTruncator;
import com.gentics.lib.db.CharacterCountTruncator;
import com.gentics.lib.db.StringLengthManipulator;
import com.gentics.lib.db.SurrogatePairAwareCodePointTruncator;
import com.gentics.lib.db.SurrogatePairAwareCodeUnitTruncator;
import org.junit.experimental.categories.Category;

@Category(BaseLibTest.class)
public class TruncatorTest {

	private static StringLengthManipulator charCountTruncator = new CharacterCountTruncator();

	private static StringLengthManipulator byteCountTruncator = new ByteCountTruncator();

	// A string consisting of a character outside the BMP
	static final String ONE_GCLEF_CHAR = "\ud834\udd1e";

	// A String consisting of an ASCII character, a BMP character and a non-BMP
	// surrogate pair
	static final String MIXED_CHARS = "A\u00c4\ud834\udd1e";

	// A String consisting of mixed ASCII characters, BMP characters and
	// non-BMP surrogate pairs
	static final String MANY_MIXED_CHARS = MIXED_CHARS + MIXED_CHARS + MIXED_CHARS;

	static final String MANY_MIXED_CHARS_TRUNC_1_CHAR = MIXED_CHARS + MIXED_CHARS + "A\u00c4";

	static final String MANY_MIXED_CHARS_TRUNC_2_CHAR = MIXED_CHARS + MIXED_CHARS + "A";

	static final String MANY_MIXED_CHARS_TRUNC_3_CHAR = MIXED_CHARS + MIXED_CHARS ;

	@Test
	public void testGetLength() {
		String x1 = "abcdef";

		assertEquals("Length of simple string by counting characters.", 6, charCountTruncator.getLength(x1));
		assertEquals("Length of simple string by counting bytes.", 6, byteCountTruncator.getLength(x1));

		String x2 = null;

		assertEquals("Length of null string by counting characters.", 0, charCountTruncator.getLength(x2));
		assertEquals("Length of null string by counting bytes.", 0, byteCountTruncator.getLength(x2));

		String x3 = "";

		assertEquals("Length of empty string by counting characters.", 0, charCountTruncator.getLength(x3));
		assertEquals("Length of empty string by counting bytes.", 0, byteCountTruncator.getLength(x3));

		String x4 = "\u00ae"; // (r) - should be 2 bytes

		assertEquals("Length of special character string by counting characters.", 1, charCountTruncator.getLength(x4));
		assertEquals("Length of special string by counting bytes.", 2, byteCountTruncator.getLength(x4));
	}

	@Test
	public void testTruncateSimpleContent() {
		String x1 = "abcdef";

		assertEquals("Truncate simple short string by counting characters", "ab", charCountTruncator.truncate(x1, 2));
		assertEquals("Truncate simple short string by counting bytes", "ab", byteCountTruncator.truncate(x1, 2));

		String x2 = "\u00ae\u00ae\u00ae";

		assertEquals("Truncate special characters string by counting characters", "\u00ae\u00ae", charCountTruncator.truncate(x2, 2));
		assertEquals("Truncate special characters short string by counting bytes", "\u00ae", byteCountTruncator.truncate(x2, 2));
		assertEquals("Truncate special characters short string by counting bytes in the middle of multibyte a character", "\u00ae",
				byteCountTruncator.truncate(x2, 3));
	}

	@Test
	public void testTruncateComplexContent() {
		String x1 = "123456789012345678901234567890"; // 30 chars, 30 bytes

		assertEquals("Truncate text to longer length than it is using character truncator.", x1, charCountTruncator.truncate(x1, 40));
		assertEquals("Truncate text to longer length than it is using byte truncator.", x1, byteCountTruncator.truncate(x1, 40));

		String x2 = "\u00ae\u00ae\u00ae\u00ae\u00ae\u00ae\u00ae\u00ae\u00ae\u00ae"; // 10

		// characters,
		// 20
		// bytes
		assertEquals("Truncate text shorter (character count) and longer (byte count) than desired length.", "\u00ae\u00ae\u00ae\u00ae\u00ae\u00ae",
				byteCountTruncator.truncate(x2, 12));

		assertEquals(
				"Truncate text shorter (character count) and longer (byte count) than desired length and the cut lies in the middle of a multibyte character.",
				"\u00ae\u00ae\u00ae\u00ae\u00ae\u00ae", byteCountTruncator.truncate(x2, 13));

	}

	@Test
	public void testTruncateResultLength() {
		String x1 = "123456789012345678901234567890"; // 30 chars, 30 bytes

		assertEquals("Test resulting length after truncating text with character count truncator", 10,
				charCountTruncator.getLength(charCountTruncator.truncate(x1, 10)));

		assertEquals("Test resulting length after truncating text with byte count truncator", 10,
				byteCountTruncator.getLength(byteCountTruncator.truncate(x1, 10)));
	}

	@Test
	public void testCodePointLength() {
		StringLengthManipulator m = new SurrogatePairAwareCodePointTruncator();
		assertEquals(1, m.getLength(ONE_GCLEF_CHAR));
		assertEquals(9, m.getLength(MANY_MIXED_CHARS));
	}

	@Test
	public void testCodePointTruncation() {
		StringLengthManipulator m = new SurrogatePairAwareCodePointTruncator();
		assertEquals("Removed wrong code point count", MANY_MIXED_CHARS_TRUNC_1_CHAR, m.truncate(MANY_MIXED_CHARS, 8));
		assertEquals("Removed wrong code point count", MANY_MIXED_CHARS_TRUNC_2_CHAR, m.truncate(MANY_MIXED_CHARS, 7));
		assertEquals("Removed wrong code point count", MANY_MIXED_CHARS_TRUNC_3_CHAR, m.truncate(MANY_MIXED_CHARS, 6));
		assertEquals("Truncating at 0 must yield emtpy string", "", m.truncate(MANY_MIXED_CHARS, 0));
		assertEquals("Truncating at 0 must yield emtpy string", "", m.truncate("", 0));
		assertEquals("Truncating emtpy string must yield emtpy string", "", m.truncate("", 15));

	}

	@Test
	public void testCodeUnitLength() {
		StringLengthManipulator m = new SurrogatePairAwareCodeUnitTruncator();
		assertEquals(2, m.getLength(ONE_GCLEF_CHAR));
		assertEquals(12, m.getLength(MANY_MIXED_CHARS));
	}

	@Test
	public void testCodeUnitTruncation() {
		StringLengthManipulator m = new SurrogatePairAwareCodeUnitTruncator();
		assertEquals("The whole surrogate pair at the end must be removed", MANY_MIXED_CHARS_TRUNC_1_CHAR, m.truncate(MANY_MIXED_CHARS, 11));
		assertEquals("Removed wrong code unit count", MANY_MIXED_CHARS_TRUNC_1_CHAR, m.truncate(MANY_MIXED_CHARS, 10));
		assertEquals("Removed wrong code unit count", MANY_MIXED_CHARS_TRUNC_2_CHAR, m.truncate(MANY_MIXED_CHARS, 9));
		assertEquals("Removed wrong code unit count", MANY_MIXED_CHARS_TRUNC_3_CHAR, m.truncate(MANY_MIXED_CHARS, 8));
		assertEquals("Truncating at 0 must yield emtpy string", "", m.truncate(MANY_MIXED_CHARS, 0));
		assertEquals("Truncating at 0 must yield emtpy string", "", m.truncate("", 0));
		assertEquals("Truncating emtpy string must yield emtpy string", "", m.truncate("", 15));
	}
}
