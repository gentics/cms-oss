/*
 * @author norbert
 * @date 17.10.2007
 * @version $Id: DateFormatConfig.java,v 1.3 2008-05-26 15:05:57 norbert Exp $
 */
package com.gentics.lib.formatter.dateformatter;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.formatter.dateformatter.impl.JAXBdateFormatTypeImpl;

/**
 * @author norbert
 */
public class DateFormatConfig extends JAXBdateFormatTypeImpl {

	/**
	 * Map of languagecode specific formats, keys are the languagecodes, values
	 * are either instances of {@link DateFormat} or
	 * {@link DateFormatConfig.CombinedDateFormat}
	 */
	protected Map languageFormats = new HashMap();

	/**
	 * Format the given date in the given languagecode
	 * @param date date to format
	 * @param languageCode languagecode (may be null)
	 * @return formatted date
	 */
	public String format(java.util.Date date, String languageCode) {
		// garbage in, garbage out
		if (date == null) {
			return null;
		}

		// get the format
		Object format = getDateFormat(languageCode);

		// format the date
		if (format instanceof DateFormat) {
			return ((DateFormat) format).format(date);
		} else if (format instanceof CombinedDateFormat) {
			return ((CombinedDateFormat) format).format(date);
		} else {
			// when no suitable format is found, just make to String representation of the date
			return date.toString();
		}
	}

	/**
	 * Parse the given formatted date into a Date object
	 * @param formattedDate formatted date
	 * @param languageCode languagecode
	 * @return date object
	 * @throws ParseException
	 */
	public java.util.Date parse(String formattedDate, String languageCode) throws ParseException {
		// garbage in, garbage out
		if (formattedDate == null) {
			return null;
		}

		// get the format
		Object format = getDateFormat(languageCode);

		// format the date
		if (format instanceof DateFormat) {
			return ((DateFormat) format).parse(formattedDate);
		} else if (format instanceof CombinedDateFormat) {
			return ((CombinedDateFormat) format).parse(formattedDate);
		} else {
			// when no suitable format is found, just make to String representation of the date
			return new java.util.Date(formattedDate);
		}
	}

	/**
	 * Get the formatter for the given languagecode, returns either a
	 * {@link DateFormat} or a {@link DateFormatConfig.CombinedDateFormat} or
	 * null
	 * @param languageCode language code
	 * @return date formatter
	 */
	protected Object getDateFormat(String languageCode) {
		if (languageCode == null) {
			return null;
		}
		Object format = languageFormats.get(languageCode);

		if (format == null && !languageFormats.containsKey(languageCode)) {
			Locale locale = getLocale(languageCode);

			// first get language specific date/time variants
			JAXBdateOrTimeType[] dateOrTime = getDateOrTime();
			String datePart = null;
			String timePart = null;

			for (int i = 0; i < dateOrTime.length && (datePart == null || timePart == null); i++) {
				if (languageCode.equalsIgnoreCase(dateOrTime[i].getLanguage())) {
					if (datePart == null && dateOrTime[i] instanceof Date) {
						datePart = dateOrTime[i].getValue();
					} else if (timePart == null && dateOrTime[i] instanceof Time) {
						timePart = dateOrTime[i].getValue();
					}
				}
			}

			// use default values where no specific formats found
			if (datePart == null) {
				datePart = getDefaultdate();
			}
			if (timePart == null) {
				timePart = getDefaulttime();
			}

			// now generate the formats
			if (datePart != null) {
				if (timePart != null) {
					// both parts set
					format = new CombinedDateFormat(createDateFormat(datePart, locale), createTimeFormat(timePart, locale));
				} else {
					// only datepart set
					format = createDateFormat(datePart, locale);
				}
			} else {
				if (timePart != null) {
					// only timepart set
					format = createTimeFormat(timePart, locale);
				} else {// no suitable format found
				}
			}

			languageFormats.put(languageCode, format);
		}

		return format;
	}

	/**
	 * Parse the given format into the shortformat int
	 * @param format format as string
	 * @return the constant for the short format or -1
	 */
	public static int getShortFormat(String format) {
		if ("FULL".equalsIgnoreCase(format)) {
			return DateFormat.FULL;
		} else if ("LONG".equalsIgnoreCase(format)) {
			return DateFormat.LONG;
		} else if ("MEDIUM".equalsIgnoreCase(format)) {
			return DateFormat.MEDIUM;
		} else if ("SHORT".equalsIgnoreCase(format)) {
			return DateFormat.SHORT;
		} else {
			return -1;
		}
	}

	/**
	 * Create the dateformat for the given date format configuration
	 * @param dateFormat configured format
	 * @param locale locale
	 * @return dateformat
	 */
	public static DateFormat createDateFormat(String dateFormat, Locale locale) {
		int shortFormat = getShortFormat(dateFormat);

		if (shortFormat >= 0) {
			return DateFormat.getDateInstance(shortFormat, locale);
		} else {
			return new SimpleDateFormat(dateFormat, locale);
		}
	}

	/**
	 * Create the dateformat for the given time format configuration
	 * @param timeFormat configured format
	 * @param locale locale
	 * @return timeformat
	 */
	public static DateFormat createTimeFormat(String timeFormat, Locale locale) {
		int shortFormat = getShortFormat(timeFormat);

		if (shortFormat >= 0) {
			return DateFormat.getTimeInstance(shortFormat, locale);
		} else {
			return new SimpleDateFormat(timeFormat, locale);
		}
	}

	/**
	 * Create the dateformat for the given date/time format configuration
	 * @param dateTimeFormat configured format
	 * @param locale locale
	 * @return date/time format
	 */
	public static DateFormat createDateTimeFormat(String dateTimeFormat, Locale locale) {
		int shortFormat = getShortFormat(dateTimeFormat);

		if (shortFormat >= 0) {
			return DateFormat.getDateTimeInstance(shortFormat, shortFormat, locale);
		} else {
			return new SimpleDateFormat(dateTimeFormat, locale);
		}
	}

	/**
	 * Get the locale for the given language code
	 * @param languageCode language code
	 * @return locale
	 */
	public static Locale getLocale(String languageCode) {
		if (StringUtils.isEmpty(languageCode)) {
			return Locale.getDefault();
		} else {
			return new Locale(languageCode);
		}
	}

	/**
	 * Internal class for combined date/time formats
	 */
	protected static class CombinedDateFormat {

		/**
		 * date part
		 */
		protected DateFormat dateFormat;

		/**
		 * time part
		 */
		protected DateFormat timeFormat;

		/**
		 * Create an instance of the combined format
		 * @param dateFormat date format
		 * @param timeFormat time format
		 */
		public CombinedDateFormat(DateFormat dateFormat, DateFormat timeFormat) {
			this.dateFormat = dateFormat;
			this.timeFormat = timeFormat;
		}

		/**
		 * format the given date
		 * @param date date to format
		 * @return formatted date
		 */
		public String format(java.util.Date date) {
			if (dateFormat == null) {
				if (timeFormat == null) {
					return null;
				} else {
					return timeFormat.format(date);
				}
			} else {
				if (timeFormat == null) {
					return dateFormat.format(date);
				} else {
					StringBuffer buffer = new StringBuffer();

					buffer.append(dateFormat.format(date)).append(" ").append(timeFormat.format(date));
					return buffer.toString();
				}
			}
		}

		/**
		 * Parse the given formatted date into a Date object
		 * @param formattedDate formatted date
		 * @return date object
		 */
		public java.util.Date parse(String formattedDate) throws ParseException {
			if (dateFormat == null) {
				if (timeFormat == null) {
					return null;
				} else {
					return timeFormat.parse(formattedDate);
				}
			} else {
				if (timeFormat == null) {
					return dateFormat.parse(formattedDate);
				} else {
					// first parse with the date part
					java.util.Date datePart = dateFormat.parse(formattedDate);
					// now format the datePart again and remove from the original formatted date
					String formattedDatePart = dateFormat.format(datePart);

					formattedDate = formattedDate.substring(Math.min(formattedDate.length(), formattedDatePart.length() + 1));
					// parse the remaining part with the timeformat
					java.util.Date timePart = timeFormat.parse(formattedDate);

					// calculate the offset
					long offset = Calendar.getInstance().getTimeZone().getOffset(0);

					// now we have date/time part we have to combine both and
					// add the offset, since it is contained in both parts and
					// would otherwise be calculated twice
					return new java.util.Date(datePart.getTime() + timePart.getTime() + offset);
				}
			}
		}
	}
}
