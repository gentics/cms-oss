package com.gentics.contentnode.formatter;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import com.gentics.api.portalnode.imp.AbstractGenticsImp;
import com.gentics.api.portalnode.imp.ImpException;
import com.gentics.contentnode.etc.ContentNodeDate;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.render.RenderableResolvable;
import com.gentics.contentnode.resolving.StackResolvable;
import com.gentics.lib.formatter.dateformatter.DateFormatConfig;

/**
 * New and better Version if the GenticsDateFormatter. Can be used to format
 * dates in Portal.Node and Content.Node
 */
public class CNDateFormatterImp extends AbstractGenticsImp {

	/**
	 * some basic definitions needed for calculations
	 */
	public static final int MINUTE = 60;

	public static final int HOUR = 60 * MINUTE;

	public static final int DAY = 24 * HOUR;

	public static final int WEEK = 7 * DAY;

	public static final int MONTH = 30 * DAY;

	public static final int YEAR = 365 * DAY;

	/**
	 * 
	 */
	public CNDateFormatterImp() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.gentics.api.portalnode.imp.AbstractGenticsImp#init(java.lang.String,
	 * java.util.Map)
	 */
	public void init(String impId, Map parameters) throws ImpException {
		super.init(impId, parameters);
	}

	/**
	 * Format the current date in the given format and the current language
	 * 
	 * @param format
	 *            format
	 * @return formatted date
	 */
	public String format(String format) {
		return format(new Date(), format, getCurrentLanguageCode());
	}

	/**
	 * @param format
	 * @deprecated use {@link #format(String)} instead
	 * @return
	 */
	public String formatDate(String format) {
		return format(format);
	}

	/**
	 * Format the current date in the given format and language
	 * 
	 * @param format
	 *            format
	 * @param languageCode
	 *            language code
	 * @return formatted date
	 */
	public String format(String format, String languageCode) {
		return format(new Date(), format, languageCode);
	}

	/**
	 * @param format
	 * @param languageCode
	 * @deprecated use {@link #format(String, String)} instead
	 * @return
	 */
	public String formatDate(String format, String languageCode) {
		return format(format, languageCode);
	}

	/**
	 * Format the given date in the given format and current language
	 * 
	 * @param date
	 *            date
	 * @param format
	 *            format
	 * @return formatted date
	 */
	public String format(Date date, String format) {
		return format(date, format, getCurrentLanguageCode());
	}

	/**
	 * 
	 * @param date
	 * @param format
	 * @deprecated use {@link #format(Date, String)} instead
	 * @return
	 */
	public String formatDate(Date date, String format) {
		return format(date, format);
	}

	/**
	 * Format the given date in the given format and language
	 * 
	 * @param date
	 *            date
	 * @param format
	 *            format
	 * @param languageCode
	 *            language
	 * @return formatted date
	 */
	public String format(Date date, String format, String languageCode) {
		DateFormat dateFormat = DateFormatConfig.createDateTimeFormat(format, DateFormatConfig.getLocale(languageCode));
		return dateFormat.format(date);
	}

	/**
	 * @param date
	 * @param format
	 * @param languageCode
	 * @deprecated use {@link #format(Date, String, String)} instead
	 * @return
	 */
	public String formatDate(Date date, String format, String languageCode) {
		return format(date, format, languageCode);
	}

	/**
	 * Format the given date in the given format and current language
	 * 
	 * @param date
	 *            date
	 * @param format
	 *            format
	 * @return formatted date
	 */
	public String format(Object date, String format) {
		return format(toDate(date), format);
	}

	/**
	 * 
	 * @param date
	 * @param format
	 * @deprecated use {@link #format(Object, String)} instead
	 * @return
	 */
	public String formatDate(Object date, String format) {
		return format(date, format);
	}

	/**
	 * Format the given date in the given format and language
	 * 
	 * @param date
	 *            date
	 * @param format
	 *            format
	 * @param languageCode
	 *            language
	 * @return formatted date
	 */
	public String format(Object date, String format, String languageCode) {
		return format(toDate(date), format, languageCode);
	}

	/**
	 * 
	 * @param date
	 * @param format
	 * @param languageCode
	 * @deprecated use {@link #format(Object, String, String)} instead
	 * @return
	 */
	public String formatDate(Object date, String format, String languageCode) {
		return format(date, format, languageCode);
	}

	/**
	 * parse the given string to a date (in the default format) and return the
	 * date or null if the string is not parsable
	 * 
	 * @param formattedDate
	 *            formatted date to be parsed
	 * @return date object or null
	 */
	public Date parse(String formattedDate) {
		return parse(formattedDate, null);
	}

	/**
	 * 
	 * @param formattedDate
	 * @deprecated use {@link #parse(String)} instead
	 * @return
	 */
	public Date parseDate(String formattedDate) {
		return parse(formattedDate);
	}

	/**
	 * parse the given string to a date in the given format and return the date
	 * or null if the string is unparseable
	 * 
	 * @param formattedDate
	 *            formatted date to be parsed
	 * @param dateFormat
	 *            date format or null for the default format
	 * @return date object or null
	 */
	public Date parse(String formattedDate, String dateFormat) {
		return parse(formattedDate, dateFormat, getCurrentLanguageCode());
	}

	/**
	 * 
	 * @param formattedDate
	 * @param dateFormat
	 * @deprecated use {@link #parse(String, String))} instead
	 * @return
	 */
	public Date parseDate(String formattedDate, String dateFormat) {
		return parse(formattedDate, dateFormat);
	}

	/**
	 * Parse the given string to a date in the given format and given
	 * languagecode
	 * 
	 * @param formattedDate
	 *            formatted date to be parsed
	 * @param format
	 *            date format or null for the default format
	 * @param languageCode
	 *            language code
	 * @return date object or null
	 */
	public Date parse(String formattedDate, String format, String languageCode) {

		try {
			DateFormat dateFormat = DateFormatConfig.createDateTimeFormat(format, DateFormatConfig.getLocale(languageCode));
			return dateFormat.parse(formattedDate);
		} catch (ParseException e) {
			logger.error("Error while parsing {" + formattedDate + "} into a date with format {" + format + "}", e);
			return null;
		}
	}

	/**
	 * 
	 * @param formattedDate
	 * @param format
	 * @param languageCode
	 * @deprecated use {@link #parse(String, String, String)} instead
	 * @return
	 */
	public Date parseDate(String formattedDate, String format, String languageCode) {
		return parse(formattedDate, format, languageCode);
	}

	/**
	 * generate a date from a timestamp
	 * 
	 * @param timestamp
	 *            timestamp
	 * @return date object
	 */
	public Date fromTimestamp(int timestamp) {
		return new Date((long) timestamp * 1000L);
	}

	/**
	 * generate a date from a timestamp
	 * 
	 * @param timestamp
	 *            timestamp (as string)
	 * @return date object
	 */
	public Date fromTimestamp(String timestamp) {
		try {
			return new Date(Long.parseLong(timestamp) * 1000L);
		} catch (NumberFormatException ex) {
			return null;
		}
	}

	/**
	 * check whether the object is a date or not
	 * 
	 * @param object
	 *            object to check
	 * @return true when the object is a date, false if not
	 */
	public boolean isDate(Object object) {
		return object instanceof Date;
	}

	/**
	 * Calculate the difference between the given dates in the given time unit.
	 * Possible time units are:
	 * <ul>
	 * <li><b>y</b> for years</li>
	 * <li><b>M</b> for months</li>
	 * <li><b>w</b> for weeks</li>
	 * <li><b>d</b> for days</li>
	 * <li><b>h</b> for hours</li>
	 * <li><b>m</b> for minutes</li>
	 * <li><b>s</b> for seconds</li>
	 * <li><b>ms</b> for milliseconds (the default)</li>
	 * </ul>
	 * 
	 * @param fromDate
	 *            first date (should be earlier)
	 * @param toDate
	 *            second date (should be later)
	 * @param unit
	 *            the time unit for output of the date difference
	 * @return difference in the given unit
	 */
	public long dateDiff(Date fromDate, Date toDate, String unit) {
		long msDiff = toDate.getTime() - fromDate.getTime();
		long diff = msDiff;

		if ("y".equals(unit)) {
			// difference in years
			Calendar toCal = Calendar.getInstance();

			toCal.setTime(toDate);
			Calendar fromCal = Calendar.getInstance();

			fromCal.setTime(fromDate);
			diff = toCal.get(Calendar.YEAR) - fromCal.get(Calendar.YEAR);
			// now check whether the to-day is earlier in the year as the
			// from-day
			toCal.set(Calendar.YEAR, fromCal.get(Calendar.YEAR));
			if (toCal.getTime().before(fromCal.getTime())) {
				diff -= 1;
			}
		} else if ("M".equals(unit)) {
			// difference in months
			Calendar toCal = Calendar.getInstance();

			toCal.setTime(toDate);
			Calendar fromCal = Calendar.getInstance();

			fromCal.setTime(fromDate);
			diff = (toCal.get(Calendar.YEAR) - fromCal.get(Calendar.YEAR)) * 12;
			// now check whether the to-day is earlier in the year as the
			// from-day
			toCal.set(Calendar.YEAR, fromCal.get(Calendar.YEAR));
			diff += (toCal.get(Calendar.MONTH) - fromCal.get(Calendar.MONTH));
			toCal.set(Calendar.MONTH, fromCal.get(Calendar.MONTH));
			if (toCal.getTime().before(fromCal.getTime())) {
				diff -= 1;
			}
		} else if ("w".equals(unit)) {
			// difference in weeks
			diff = msDiff / (7 * 24 * 60 * 60 * 1000);
		} else if ("d".equals(unit)) {
			// difference in days
			diff = msDiff / (24 * 60 * 60 * 1000);
		} else if ("h".equals(unit)) {
			// difference in hours
			diff = msDiff / (60 * 60 * 1000);
		} else if ("m".equals(unit)) {
			// difference in minutes
			diff = msDiff / (60 * 1000);
		} else if ("s".equals(unit)) {
			// difference in seconds
			diff = msDiff / 1000;
		}
		return diff;
	}

	/**
	 * Other version of the {@link #dateDiff(Date, Date, String)} Method
	 * 
	 * @param fromDate
	 *            first date
	 * @param toDate
	 *            second date
	 * @param unit
	 *            the time unit for output of the date difference
	 * @return difference in the given unit
	 */
	public long dateDiff(Object fromDate, Object toDate, String unit) {
		return dateDiff(toDate(fromDate), toDate(toDate), unit);
	}

	/**
	 * Calculate the difference between the given dates in milliseconds.
	 * 
	 * @param fromDate
	 *            first date (should be earlier)
	 * @param toDate
	 *            second date (should be later)
	 * @return difference in milliseconds
	 */
	public long dateDiff(Date fromDate, Date toDate) {
		return dateDiff(fromDate, toDate, "ms");
	}

	/**
	 * Other version of the {@link #dateDiff(Date, Date)} Method
	 * 
	 * @param fromDate
	 *            first date (should be earlier)
	 * @param toDate
	 *            second date (should be later)
	 * @return difference in milliseconds
	 */
	public long dateDiff(Object fromDate, Object toDate) {
		return dateDiff(toDate(fromDate), toDate(toDate));
	}

	/**
	 * Transform the ContentNodeDate to a Date
	 * 
	 * @param date
	 *            ContentNodeDate
	 * @return Date
	 */
	protected static Date toDate(Object date) {
		if (date == null) {
			return null;
		}

		// unwrap the object (if wrapped)
		if (date instanceof RenderableResolvable) {
			date = ((RenderableResolvable) date).getWrappedObject();
		}

		if (date instanceof Date) {
			return (Date) date;
		} else if (date instanceof ContentNodeDate) {
			return new Date(((ContentNodeDate) date).getTimestamp().longValue() * 1000L);
		} else if (date instanceof Integer) {
			return new Date((Integer) date);
		} else {
			return null;
		}
	}

	/**
	 * Get the current language code or null
	 * 
	 * @return current language code or null
	 */
	protected String getCurrentLanguageCode() {

		// try to get the currently rendered page (if any)
		try {
			Transaction t = TransactionManager.getCurrentTransaction();
			StackResolvable rootObject = t.getRenderType().getRenderedRootObject();
			if (rootObject instanceof Page) {
				return ((Page) rootObject).getLanguage().getCode();
			}
		} catch (Exception e) {// hmm, silently ignore these exceptions?
		}
		return null;
	}

	/**
	 * Get the timezone of the current date in RFC3339 format. See <a
	 * href="http://www.ietf.org/rfc/rfc3339.txt">RFC 3339</a> for details.
	 * 
	 * @return timezone of the current time in RFC3339 format.
	 */
	public String getRfc3339Timezone() {

		Date date = new Date();

		return getRfc3339Timezone(date);
	}

	/**
	 * Get the timezone of the given date in RFC3339 format. See <a
	 * href="http://www.ietf.org/rfc/rfc3339.txt">RFC 3339</a> for details.
	 * 
	 * @param date
	 *            date
	 * @return timezone of the given date in RFC3339 format.
	 */
	public String getRfc3339Timezone(Object date) {
		Date foo = toDate(date);
		SimpleDateFormat format = new SimpleDateFormat("Z");
		String rfc3339tz = format.format(foo).substring(0, 3) + ":" + format.format(foo).substring(3);

		return rfc3339tz;
	}

}
