package com.gentics.lib.formatter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import javax.xml.bind.JAXBException;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.gentics.api.portalnode.imp.ImpException;
import com.gentics.lib.jaxb.JAXBHelper;
import org.junit.experimental.categories.Category;

@Category(BaseLibTest.class)
public class GenticsDateFormatterTest {

	private static GenericGenticsDateFormatter formatter;
	private final static String IMP_NAME = "bogus";
	private final static String DATE_FORMAT = "dd MMM yyyy HH:mm:ss";
	private final static String DATE_RESULT = "01 Jan 1970 01:00:00";
	private final static long FIXED_DATE_IN_2014 = 1397478595585L;

	@BeforeClass
	public static void setupOnce() throws JAXBException, ImpException {
		JAXBHelper.init(null);
		formatter = new GenericGenticsDateFormatter();
		Map<String, String> settings = new HashMap<String, String>();

		String confPath = GenticsDateFormatterTest.class.getResource("dateformatter.xml").getFile();
		settings.put(GenericGenticsDateFormatter.CONFIGPATH_PARAM, confPath);

		formatter.init(IMP_NAME, settings);

	}

	@Test
	public void testInvalidFormat() {
		String dateString = formatter.format(new Integer(2));
		assertNull(dateString);
	}

	@Test
	@Ignore("Test unstable due to local settings")
	public void simpleFormatterTest() throws ImpException, JAXBException {
		assertNotNull(formatter.format());
		String dateResult = "Thu Jan 01 01:00:00 CET 1970";
		assertEquals("The result should match the expected format", dateResult, formatter.format(new Date(3)));
		assertEquals("The result should match the expected format", dateResult, formatter.format((Object) new Date(3)));
		assertNotNull(formatter.format(DATE_FORMAT));

		assertEquals("The result should match the expected format", DATE_RESULT, formatter.format((Object) new Date(1), DATE_FORMAT));
		assertNotNull(formatter.format(DATE_FORMAT, "DE"));
		assertEquals("The result should match the expected format", DATE_RESULT, formatter.format(new Date(1), DATE_FORMAT, "DE"));
		assertEquals("The result should match the expected format", DATE_RESULT, formatter.format((Object) new Date(1), DATE_FORMAT, "DE"));
	}

	@Test
	@Ignore("Test unstable due to local settings")
	public void testFormatDate() {
		assertNotNull(formatter.formatDate());
		String date = "Thu Jan 01 01:00:00 CET 1970";
		assertEquals("The dates should match.", date, formatter.formatDate(new Date(1)));
		String input = "1.1.2014 12:30";
		assertEquals("Input should match the output.", input, formatter.formatDate(input));

		assertEquals("The dates should match.", DATE_RESULT, formatter.formatDate(new Date(1), DATE_FORMAT));
		assertEquals("The dates should match.", DATE_RESULT, formatter.formatDate((Object) new Date(1), DATE_FORMAT));
		assertNotNull(formatter.formatDate(DATE_FORMAT, "EN"));
		assertEquals("The dates should match.", DATE_RESULT, formatter.formatDate(new Date(1), DATE_FORMAT, "EN"));
		assertEquals("The dates should match.", DATE_RESULT, formatter.formatDate((Object) new Date(1), DATE_FORMAT, "EN"));
	}

	@Test
	public void testFromTimestamp() {
		Date date = formatter.fromTimestamp(1000);
		assertEquals("Both dates should match", new Date(1000 * 1000).getTime(), date.getTime());
		date = formatter.fromTimestamp("1000");
		assertEquals("Both dates should match", new Date(1000 * 1000).getTime(), date.getTime());
	}

	@Test
	public void testParseDate() {
		String inputDate = "2014-03-12 12:30";
		Date date = formatter.parse(null);
		assertNull(date);
		date = formatter.parseDate(inputDate, DATE_FORMAT, "EN");
		assertNull(date);
		date = formatter.parse(inputDate, DATE_FORMAT, "DE");
		assertNull(date);
	}

	@Test
	public void testDateDiff() {

		GenericGenticsDateFormatter formatter = new GenericGenticsDateFormatter();
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

	@Test
	public void testMisc() {
		assertEquals("The impname should match the name we choose during init.", IMP_NAME, formatter.getImpId());

		TimeZone tz = TimeZone.getTimeZone("Europe/Vienna");
		int offset = (int) ((Calendar.getInstance().get(Calendar.ZONE_OFFSET) + Calendar.getInstance().get(Calendar.DST_OFFSET)) / 60 / 60 / 1000);

		assertEquals("+0" + offset + ":00", formatter.getRfc3339Timezone());
		assertEquals("+0" + offset + ":00", formatter.getRfc3339Timezone(new Date()));

		assertFalse("The given object is not a date object", formatter.isDate("1.1.2014"));
		assertTrue("The given object is a date", formatter.isDate(new Date(1)));
		assertFalse("The given object is not a date", formatter.isDate(new Integer(1)));
	}
}
