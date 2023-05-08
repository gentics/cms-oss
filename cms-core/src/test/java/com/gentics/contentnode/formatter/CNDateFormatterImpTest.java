package com.gentics.contentnode.formatter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

import javax.xml.bind.JAXBException;

import org.junit.BeforeClass;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.portalnode.imp.ImpException;
import com.gentics.contentnode.etc.ContentNodeDate;

/**
 * The CNDateFormatterImpTest will test various public methods that can be
 * accessed using velocity within GCN
 * 
 * @author johannes2
 * 
 */
public class CNDateFormatterImpTest {

	private static CNDateFormatterImp formatter;
	private final static String IMP_NAME = "bogus";
	private final static String DATE_FORMAT = "dd MMM yyyy HH:mm:ss";
	private final static String DATE_RESULT = "01 Jan 1970 01:00:00";
	private final static String DATE_RESULT_DE = "01 Jan. 1970 01:00:00";
	private final static long FIXED_DATE_IN_2014 = 1397478595585L;

	@BeforeClass
	public static void setupOnce() throws JAXBException, ImpException, NodeException {
		// The tests assume UTC+1 and english locale
		TimeZone.setDefault(TimeZone.getTimeZone("Europe/Berlin"));
		Locale.setDefault(new Locale("en", "US"));

		formatter = new CNDateFormatterImp();
		formatter.init(IMP_NAME, new HashMap<String, String>());
	}

	@Test
	public void testIntegerFormat() {
		String dateString = formatter.format(new Integer(2), DATE_FORMAT);
		assertEquals("01 Jan 1970 01:00:00", dateString);
	}

	@Test
	public void testFormatCNDates() {
		ContentNodeDate cnDate = new ContentNodeDate(0);
		assertEquals("01 Jan 1970 01:00:00", formatter.format(cnDate, DATE_FORMAT));
	}

	@Test
	public void simpleFormatterTest() throws ImpException, JAXBException {
		assertNotNull(formatter.format(DATE_FORMAT));
		assertEquals("The result should match the expected format", DATE_RESULT, formatter.format((Object) new Date(1), DATE_FORMAT));
		assertNotNull(formatter.format(DATE_FORMAT, "DE"));
		assertEquals("The result should match the expected format", DATE_RESULT_DE, formatter.format(new Date(1), DATE_FORMAT, "DE"));
		assertEquals("The result should match the expected format", DATE_RESULT_DE, formatter.format((Object) new Date(1), DATE_FORMAT, "DE"));
	}

	@Test
	public void testFormatDate() {
		String input = "1.1.2014 12:30";
		assertEquals("Input should match the output.", input, formatter.format(input));

		assertEquals("The dates should match.", DATE_RESULT, formatter.format(new Date(1), DATE_FORMAT));
		assertEquals("The dates should match.", DATE_RESULT, formatter.format((Object) new Date(1), DATE_FORMAT));
		assertNotNull(formatter.format(DATE_FORMAT, "EN"));
		assertEquals("The dates should match.", DATE_RESULT, formatter.format(new Date(1), DATE_FORMAT, "EN"));
		assertEquals("The dates should match.", DATE_RESULT, formatter.format((Object) new Date(1), DATE_FORMAT, "EN"));
	}

	@Test
	public void testFromTimestamp() {
		Date date = formatter.fromTimestamp(1000);
		assertEquals("Both dates should match", new Date(1000 * 1000).getTime(), date.getTime());
		date = formatter.fromTimestamp("1000");
		assertEquals("Both dates should match", new Date(1000 * 1000).getTime(), date.getTime());
	}

	/**
	 * Test a common usecase for the dateformatter imp.
	 */
	@Test
	public void testFormatterUsecase() {
		String date = formatter.format(formatter.fromTimestamp(1000), "MMMM dd, yyyy", "en");
		assertEquals("January 01, 1970", date);
	}

	@Test
	public void testParseDate() {
		String inputDate = "2014-03-12 12:30";
		Date date = formatter.parse(inputDate, DATE_FORMAT, "EN");
		assertNull("The given datestring should not be parseable by the given pattern", date);

		date = formatter.parse(inputDate, DATE_FORMAT, "DE");
		assertNull("The given datestring should not be parseable by the given pattern", date);

		date = formatter.parse("22 MAR 1984 12:30:00", "dd MMM yyyy HH:mm:ss", "EN");
		assertNotNull("The given datestring should be parsable", date);

	}

	@Test
	public void testDateDiff() {
		long diff = formatter.dateDiff(new Date(0), new Date(1));
		assertEquals("Diff does not match", 1, diff);
		diff = formatter.dateDiff((Object) new Date(1), (Object) new Date(2));
		assertEquals("diff does not match", 1, diff);

		diff = formatter.dateDiff(new Date(1), new Date(FIXED_DATE_IN_2014), "y");
		assertEquals("Years since 1970 do not match", 44, diff);
		diff = formatter.dateDiff(new Date(1), new Date(FIXED_DATE_IN_2014), "M");
		assertEquals("Months since 1970 do not match", 531, diff);
		diff = formatter.dateDiff(new Date(1), new Date(FIXED_DATE_IN_2014), "w");
		assertEquals("Weeks since 1970 do not match", 2310, diff);
		diff = formatter.dateDiff(new Date(1), new Date(FIXED_DATE_IN_2014), "d");
		assertEquals("Days since 1970 do not match", 16174, diff);
		diff = formatter.dateDiff(new Date(1), new Date(FIXED_DATE_IN_2014), "h");
		assertEquals("Hours since 1970 do not match", 388188, diff);
		diff = formatter.dateDiff(new Date(1), new Date(FIXED_DATE_IN_2014), "m");
		assertEquals("Minutes since 1970 do not match", 23291309, diff);
		diff = formatter.dateDiff(new Date(1), new Date(FIXED_DATE_IN_2014), "s");
		assertEquals("Seconds since 1970 do not match", 1397478595, diff);
		diff = formatter.dateDiff((Object) new Date(0), (Object) new Date(FIXED_DATE_IN_2014), "M");
		assertEquals("Months since 1970 do not match.", 531, diff);
		diff = formatter.dateDiff((Object) new Date(0), (Object) new Date(FIXED_DATE_IN_2014), "y");
		assertEquals("44 Years have past since 1970.", 44, diff);
	}

	/**
	 * Test various other methods of the date formatter imp
	 */
	@Test
	public void testMisc() {
		assertEquals("The impname should match the name we choose during init.", IMP_NAME, formatter.getImpId());

		if (isDST(new Date())) {
			assertEquals("+02:00", formatter.getRfc3339Timezone());
			assertEquals("+02:00", formatter.getRfc3339Timezone(new Date()));
		} else {
			assertEquals("+01:00", formatter.getRfc3339Timezone());
			assertEquals("+01:00", formatter.getRfc3339Timezone(new Date()));
		}
		assertFalse("The given object is not a date object", formatter.isDate("1.1.2014"));
		assertTrue("The given object is a date", formatter.isDate(new Date(1)));
		assertFalse("The given object is not a date", formatter.isDate(new Integer(1)));
	}

	/**
	 * Check whether the given date is within the daylight saving time frame
	 * 
	 * @param date
	 * @return when the date is within dst, otherwise false.
	 */
	private boolean isDST(Date date) {
		Calendar c = Calendar.getInstance();
		c.setTime(date);
		int offset = c.get(Calendar.DST_OFFSET);
		if (offset == 0) {
			return false;
		}
		return true;
	}
}
