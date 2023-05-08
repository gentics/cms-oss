/**
 * Title:        <p>
 * Description:  <p>
 * Copyright:    Copyright (c) <p>
 * Company:      <p>
 * @author Mascher Erwin
 * @version 1.0
 */
package com.gentics.lib.etc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.Collator;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import com.gentics.lib.log.NodeLogger;

/**
 * this class contains various utiilities for formating dates and times, easily
 * extracting of properties, and helps to coordinate a global debug value
 */

public final class MiscUtils {

	/**
	 * the following value can be used to convert times between 1904 and 1970
	 * standard. 1970-time is the standard in java. subtract this value from any
	 * given 1904-time to get the 1970-time.
	 */
	public static final long ADD_TO_1970_TO_GET_1904_TIME = 2082844800000L;

	private static final SimpleDateFormat standardDateFormatter = new SimpleDateFormat("d.M.yyyy");

	public static String formatDate(long unixTimestamp) {
		if (unixTimestamp <= 0) {
			return "";
		}
		return standardDateFormatter.format(new Date(unixTimestamp * 1000));
	}

	public static String stackToString(Class[] stack) {
		String ret = "";

		for (int i = 0; i < stack.length; i++) {
			Class c = stack[i];

			if (c != null) {
				ret += c.getName() + "\n";
			}
		}
		return ret;
	}

	public static String getByteFormatString(long bytes) {
		int dim = 0;

		while (bytes >= 1024) {
			dim++;
			bytes /= 1024;
		}
		switch (dim) {
		case 0:
			return bytes + " b";

		case 1:
			return bytes + " kB";

		case 2:
			return bytes + " MB";

		case 3:
			return bytes + " GB";

		case 4:
			return bytes + " TB";

		default:
			return "INSANE bytes";
		}
	}

	public static String replaceAll(String str, String what, String withWhat) {
		int pos = 0, oldpos = 0;

		if (str == null) {
			return null;
		}
		StringBuffer ret = new StringBuffer(str.length());

		while ((pos = str.indexOf(what, oldpos)) >= 0) {
			ret.append(str.substring(oldpos, pos));
			ret.append(withWhat);
			oldpos = pos + what.length();
		}
		ret.append(str.substring(oldpos));
		return ret.toString();
	}

	/**
	 * returs true, if haystack does not contain any other characters than
	 * "characters"
	 * @param haystack
	 * @param characters
	 * @return
	 */
	public static boolean strConsistsOf(String haystack, String characters) {
		for (int i = 0; i < haystack.length(); i++) {
			char ch = haystack.charAt(i);

			if (characters.indexOf(ch) < 0) {
				return false;
			}
		}

		return true;
	}

	/**
	 * replaces all occurences of needle in haystack with replacement. NOTE: two
	 * occurences of needle, which follow each other immediately will not be
	 * replaces twice: example: "Hello replaceme World" "replaceme" "!" -->
	 * "Hello ! World" "Hello replacemereplacemereplacemereplaceme World
	 * replaceme" "replaceme" "!" --> "Hello ! World !"
	 * @param haystack
	 * @param needle
	 * @param replacement
	 * @return
	 */
	public static String replaceString(String haystack, String needle, String replacement) {
		StringTokenizer token = new StringTokenizer(haystack, needle);
		StringBuffer buff = new StringBuffer();
		int i = 0;

		if (haystack.startsWith(needle)) {
			buff.append(replacement);
		}
		while (token.hasMoreTokens()) {
			String t = token.nextToken();

			if (t.length() > 0) {
				if (i > 0) {
					buff.append(replacement + t);
				} else {
					buff.append(t);
				}
			}
			i++;
		}
		if (haystack.endsWith(needle)) {
			buff.append(replacement);
		}
		return buff.toString();
	}

	public static int countBitsSet(long flags) {
		int count = 0;
		long or = 1;

		for (int i = 0; i < 64; i++) {
			if ((flags & or) == or) {
				count++;
			}
			or = or << 1;
		}
		return count;
	}

	/**
	 * returns a printable string, concated from a given list of strings,
	 * seperated by the given character(s).
	 */
	public static String getPrintableStrArray(String[] sArray, String seperator) {
		String ret = new String();

		for (int i = 0; i < sArray.length; i++) {
			if (i > 0) {
				ret += seperator;
			}
			ret += sArray[i];
		}
		return ret;
	}

	/**
	 * compares 2 given strings, where the 2nd may contain wildcards. returns
	 * true, if the given <code>name</code> matches the given
	 * <code>filter</code>.
	 */
	public static boolean compareWildCard(String name, String filter) {
		return compareWildCard(name, 0, filter, 0);
	}

	/**
	 * compares the first given string from the given startNameIdx with the
	 * filter, that may contain wildcards ( * and ? ). Indexes are usually 0.
	 * returns true, if the given <code>name</code> matches the given
	 * <code>filter</code>.
	 */
	public static boolean compareWildCard(String name, int startNameIdx, String filter,
			int startFilterIdx) {
		int nameLen = name.length();
		int filterLen = filter.length();
		char c;
		int filterIdx = startFilterIdx, nameIdx = startNameIdx;

		if (filterLen == filterIdx) {
			if (nameLen == nameIdx) {
				return true;
			} else {
				return false;
			}
		}

		int AgainName, AgainFilter;

		if (filter.charAt(filterIdx) == '*') {
			c = filter.charAt(filterIdx);
			while ((c == '*' || c == '?')) {
				filterIdx++;

				if (c == '?') {
					nameIdx++;
				}

				if (filterIdx == filterLen) {
					return nameIdx <= nameLen;
				} else {
					c = filter.charAt(filterIdx);
				}
			}

			while ((nameIdx < nameLen) ? (name.charAt(nameIdx) != filter.charAt(filterIdx)) : false) {
				nameIdx++;
			}
			if (nameIdx == nameLen) {
				return false;
			}
		} else if (name.charAt(nameIdx) != filter.charAt(filterIdx)) {
			return false;
		}

		AgainName = nameIdx + 1;
		AgainFilter = filterIdx + 1;
		while (true) {
			if (filterIdx >= filterLen) {
				if (nameIdx >= nameLen) {
					return true;
				} else {
					return false;
				}
			} else if (nameIdx >= nameLen) {
				return false;
			}

			c = filter.charAt(filterIdx);
			c = name.charAt(nameIdx);
			switch (filter.charAt(filterIdx)) {
			case '*':
				return compareWildCard(name, nameIdx, filter, filterIdx);

			case '?':
				nameIdx++;
				filterIdx++;
				break;

			default:
				if (nameIdx >= nameLen) {
					return false;
				}
				if (name.charAt(nameIdx++) != filter.charAt(filterIdx++)) {
					return compareWildCard(name, AgainName, filter, AgainFilter);
				}

			}
		}
	}

	/**
	 * tries to sleep for the given time.
	 * @param millis the time to try sleep
	 * @return true, if there was no InterruptedException, false otherwise.
	 */

	public static final boolean sleepInnocent(long millis) {
		try {
			Thread.sleep(millis);
			return true;
		} catch (InterruptedException exc) {
			return false;
		}
	}

	/**
	 * adds the given add-char to the beginning of the string, so that the size
	 * of the return string is between minSize and maxSize
	 */
	public static String leadingChar(String unformated, char add, int minSize, int maxSize) {
		if (unformated.length() > maxSize) {
			return unformated.substring(0, maxSize - 1);
		}

		String ret;

		ret = new String();
		for (int i = unformated.length(); i < minSize; i++) {
			ret += add;
		}
		ret += unformated;
		return ret;
	}

	/**
	 * adds the given add-char to the beginning of the string, so that the size
	 * of the return string is at least minSize
	 */
	public static String leadingChar(String unformated, char add, int minSize) {
		return leadingChar(unformated, add, minSize, Integer.MAX_VALUE);
	}

	/**
	 * adds the given add-char to the end of the string, so that the size of the
	 * return string is between minSize and maxSize
	 */
	public static String trailingChar(String unformated, char add, int minSize, int maxSize) {
		if (unformated.length() > maxSize) {
			return unformated.substring(0, maxSize - 1);
		}

		String ret;

		ret = new String(unformated);
		for (int i = unformated.length(); i < minSize; i++) {
			ret += add;
		}
		return ret;
	}

	/**
	 * adds the given add-char to the end of the string, so that the size of the
	 * return string is at least minSize
	 */
	public static String trailingChar(String unformated, char add, int minSize) {
		return trailingChar(unformated, add, minSize, Integer.MAX_VALUE);
	}

	/**
	 * converts a number to a string with leading zeros.
	 * @param nr the number to convert
	 * @param zerocount maximum number of digits, that should be filled with
	 *        zeroes. for example: (1,3) --> "001" (1023, 3) --> "1023"
	 * @return a String containing the given nr plus eventually leading zeroes
	 */

	public static String leadingZero(long nr, long zerocount) {
		int i = 0;
		String s = new String();

		s = "" + nr;
		if (nr == 0) {
			i++;
		}
		while (nr != 0) {
			nr /= 10;
			i++;
		}
		while (i < zerocount) {
			s = "0" + s;
			i++;
		}
		return s;
	}

	/**
	 * formats the given millis to a readable date format.
	 * @param millis time in [ms] since January, 1st, 1970, 00:00 a.m. as for
	 *        example returned by System.currentTimeMillis
	 * @return a Date-String with format 'DD.MM.YYYY HH:MM:SS'
	 */

	public static String getDateTimeString(long millis) // date from
	{
		DateFormat df = DateFormat.getDateTimeInstance();

		return df.format(new Date(millis));
	}

	/**
	 * standard format ( days - HH:MM:SS.sss )
	 */
	public static final int TD_FORMAT_LONG = 1;

	/**
	 * hides everything greater than one day ( HH:MM:SS.sss )
	 */
	public static final int TD_FORMAT_ONE_DAY = 2;

	/**
	 * hides everything greater than one hour ( MM:SS.sss )
	 */
	public static final int TD_FORMAT_ONE_HOUR = 4;

	/**
	 * hides everything greater than one minute ( SS.sss )
	 */
	public static final int TD_FORMAT_ONE_MINUTE = 8;

	/**
	 * hides everything greater than one second ( sss )
	 */
	public static final int TD_FORMAT_ONE_SECOND = 16;

	/**
	 * hides hundreds. example: TD_FORMAT_LONG | TD_FORMAT_NO_HUNDREDS will
	 * return a string of format 'days - HH:MM:SS'
	 */
	public static final int TD_FORMAT_NO_HUNDREDS = 32;

	/**
	 * hides seconds and hundreds. example: TD_FORMAT_LONG |
	 * TD_FORMAT_NO_SECONDS will return a string of format 'days - HH:MM'
	 */
	public static final int TD_FORMAT_NO_SECONDS = 64;

	/**
	 * hides minutes, seconds and hundreds. example: TD_FORMAT_LONG |
	 * TD_FORMAT_NO_MINUTES will return a string of format 'days - HH'
	 */
	public static final int TD_FORMAT_NO_MINUTES = 128;

	/**
	 * shows only days. example: TD_FORMAT_LONG | TD_FORMAT_NO_HOURS will return
	 * a string of format 'days'
	 */
	public static final int TD_FORMAT_NO_HOURS = 256;

	/**
	 * formats a time duration to a readable standard-format
	 * @param millis the time in [ms] to convert
	 * @return a String of format 'D[D...] - HH:MM:SS.sss'
	 */

	public static String getTimeDurationString(long millis) {
		return getTimeDurationString(millis, TD_FORMAT_LONG);
	}

	/**
	 * formats a time duration.
	 * @param secs the time in [s] to be converted
	 * @return see {@link #getTimeDurationString(long)}
	 */
	public static String getTimeDurationString(double secs) {
		return getTimeDurationString((long) (secs * 1000.0));
	}

	/**
	 * @see #getTimeDurationString(double)
	 * @see #getTimeDurationString(long, int )
	 */
	public static String getTimeDurationString(double secs, int flags) {
		return getTimeDurationString((long) (secs * 1000.0), flags);
	}

	/**
	 * formats a time duration to a readable format. be careful using
	 * TD_FORMAT_NO_... symbols. date may become unreadable ( example: format
	 * flags: TD_FORMAT_LONG | TD_FORMAT_NO_HUNDREDS | TD_FORMAT_NO_MINUTES will
	 * produce a string of following format: 'days - HH:SS'
	 * @param millis the time in [ms] to convert
	 * @param format one or more combined <code>TD_FORMAT_...</code> flags;
	 *        modifies the format of the returned string.
	 * @return a String of the given format, containing the given time duration
	 */

	public static String getTimeDurationString(long millis, int format/* TD_FORMAT_ */) {
		String s = new String();
		int f;

		long days = millis / 86400000;

		millis %= 86400000;

		long hours = millis / 3600000;

		millis %= 3600000;

		long min = millis / 60000;

		millis %= 60000;

		long sec = millis / 1000;

		millis %= 1000;

		if ((f = format & 31) == 0) {
			f = TD_FORMAT_LONG;
		}

		if (f <= TD_FORMAT_LONG) {
			s += days;
		}

		if (f <= TD_FORMAT_ONE_DAY) {
			if ((format & TD_FORMAT_NO_HOURS) != 0) {
				return s;
			}
			if (f <= TD_FORMAT_LONG) {
				s += " - ";
			}

			s += leadingZero(hours, 2);
		}

		if (f <= TD_FORMAT_ONE_HOUR) {
			if ((format & TD_FORMAT_NO_MINUTES) != 0) {
				return s;
			}
			if (f <= TD_FORMAT_ONE_DAY) {
				s += ":";
			}

			s += leadingZero(min, 2);
		}

		if (f <= TD_FORMAT_ONE_MINUTE) {
			if ((format & TD_FORMAT_NO_SECONDS) != 0) {
				return s;
			}
			if (f <= TD_FORMAT_ONE_HOUR) {
				s += ":";
			}

			s += leadingZero(sec, 2);
		}

		if (f <= TD_FORMAT_ONE_SECOND) {
			if ((format & TD_FORMAT_NO_HUNDREDS) != 0) {
				return s;
			}
			if (f <= TD_FORMAT_ONE_MINUTE) {
				s += ".";
			}

			s += leadingZero(millis, 3);
		}

		return s;
	}

	/**
	 * tries to convert a given property to a double.
	 * @param p the Properties to look for the given String
	 * @param propertyName the Property to search for
	 * @param def the default value, if the Property could not be
	 *        found/converted.
	 * @return a double value containing the given Property if possible,
	 *         otherwise <code>def</code>
	 */
	public static double tryDoubleProperty(Properties p, String propertyName, double def) {
		return tryDoubleProperty(p, propertyName, def, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
	}

	/**
	 * tries to convert a given property to a double.
	 * @param p the Properties to look for the given String
	 * @param propertyName the Property to search for
	 * @param def the default value, if the Property could not be
	 *        found/converted.
	 * @param min if the found value is lower than <code>min</code> the
	 *        default value will be returned
	 * @param max if the found value is greater than <code>max</code> the
	 *        default value will be returned
	 * @return a double value containing the given Property if it was found and
	 *         between <code>min <= value <= max</code>, otherwise
	 *         <code>def</code>
	 */
	public static double tryDoubleProperty(Properties p, String propertyName, double def,
			double min, double max) {
		String str;
		double d;

		try {
			if ((str = p.getProperty(propertyName, null)) != null) {
				d = Double.valueOf(str).doubleValue();
				if (d < min || d > max) {
					return def;
				} else {
					return d;
				}
			} else {
				return def;
			}
		} catch (Exception e) {
			return def;
		}
	}

	/**
	 * tries to convert a given property to a int.
	 * @param p the Properties to look for the given String
	 * @param propertyName the Property to search for
	 * @param def the default value, if the Property could not be
	 *        found/converted.
	 * @return a double value containing the given Property if possible,
	 *         otherwise <code>def</code>
	 */
	public static int tryIntegerProperty(Properties p, String propertyName, int def) {
		return tryIntegerProperty(p, propertyName, def, Integer.MIN_VALUE, Integer.MAX_VALUE);
	}

	/**
	 * tries to convert a given property to a int.
	 * @param p the Properties to look for the given String
	 * @param propertyName the Property to search for
	 * @param def the default value, if the Property could not be
	 *        found/converted.
	 * @param min if the found value is lower than <code>min</code> the
	 *        default value will be returned
	 * @param max if the found value is greater than <code>max</code> the
	 *        default value will be returned
	 * @return a double value containing the given Property if it was found and
	 *         between <code>min <= value <= max</code>, otherwise
	 *         <code>def</code>
	 */
	public static int tryIntegerProperty(Properties p, String propertyName, int def, int min,
			int max) {
		String str;
		int d;

		try {
			if ((str = p.getProperty(propertyName, null)) != null) {
				d = Integer.valueOf(str).intValue();
				if (d < min || d > max) {
					return def;
				} else {
					return d;
				}
			} else {
				return def;
			}
		} catch (Exception e) {
			return def;
		}
	}

	/**
	 * tries to convert a given property to a long.
	 * @param p the Properties to look for the given String
	 * @param propertyName the Property to search for
	 * @param def the default value, if the Property could not be
	 *        found/converted.
	 * @return a double value containing the given Property if possible,
	 *         otherwise <code>def</code>
	 */
	public static long tryLongProperty(Properties p, String propertyName, long def) {
		return tryLongProperty(p, propertyName, def, Long.MIN_VALUE, Long.MAX_VALUE);
	}

	/**
	 * tries to convert a given property to a long.
	 * @param p the Properties to look for the given String
	 * @param propertyName the Property to search for
	 * @param def the default value, if the Property could not be
	 *        found/converted.
	 * @param min if the found value is lower than <code>min</code> the
	 *        default value will be returned
	 * @param max if the found value is greater than <code>max</code> the
	 *        default value will be returned
	 * @return a double value containing the given Property if it was found and
	 *         between <code>min <= value <= max</code>, otherwise
	 *         <code>def</code>
	 */
	public static long tryLongProperty(Properties p, String propertyName, long def, long min,
			long max) {
		String str;
		long d;

		try {
			if ((str = p.getProperty(propertyName, null)) != null) {
				d = Long.valueOf(str).intValue();
				if (d < min || d > max) {
					return def;
				} else {
					return d;
				}
			} else {
				return def;
			}
		} catch (Exception e) {
			return def;
		}
	}

	// ############# heyyyy

	/**
	 * tries to convert a given property to a double.
	 * @param p the Properties to look for the given String
	 * @param propertyName the Property to search for
	 * @param def the default value, if the Property could not be
	 *        found/converted.
	 * @return a double value containing the given Property if possible,
	 *         otherwise <code>def</code>
	 */
	public static double[] tryDoublePropertyArray(Properties p, String propertyName, double def) {
		return tryDoublePropertyArray(p, propertyName, def, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
	}

	/**
	 * tries to convert a given property to a double.
	 * @param p the Properties to look for the given String
	 * @param propertyName the Property to search for
	 * @param def the default value, if the Property could not be
	 *        found/converted.
	 * @param min if the found value is lower than <code>min</code> the
	 *        default value will be returned
	 * @param max if the found value is greater than <code>max</code> the
	 *        default value will be returned
	 * @return a double value containing the given Property if it was found and
	 *         between <code>min <= value <= max</code>, otherwise
	 *         <code>def</code>
	 */
	public static double[] tryDoublePropertyArray(Properties p, String propertyName,
			double def, double min, double max) {
		String str;
		StringTokenizer token;
		double[] d;
		double value;
		int i;

		try {
			if ((str = p.getProperty(propertyName, null)) != null) {
				token = new StringTokenizer(str, ",");
				d = new double[token.countTokens()];
				i = 0;
				while (token.hasMoreElements()) {
					str = token.nextToken();
					try {
						value = Double.valueOf(str).doubleValue();
						if (value < min || value > max) {
							d[i] = def;
						} else {
							d[i] = value;
						}
					} catch (NumberFormatException nfe) {
						d[i] = def;
					}
					i++;
				}
				return d;
			} else {
				return new double[] {};
			}
		} catch (Exception e) {
			return new double[] {};
		}
	}

	/**
	 * tries to convert a given property to a int.
	 * @param p the Properties to look for the given String
	 * @param propertyName the Property to search for
	 * @param def the default value, if the Property could not be
	 *        found/converted.
	 * @return a double value containing the given Property if possible,
	 *         otherwise <code>def</code>
	 */
	public static int[] tryIntegerPropertyArray(Properties p, String propertyName, int def) {
		return tryIntegerPropertyArray(p, propertyName, def, Integer.MIN_VALUE, Integer.MAX_VALUE);
	}

	/**
	 * tries to convert a given property to a int.
	 * @param p the Properties to look for the given String
	 * @param propertyName the Property to search for
	 * @param def the default value, if the Property could not be
	 *        found/converted.
	 * @param min if the found value is lower than <code>min</code> the
	 *        default value will be returned
	 * @param max if the found value is greater than <code>max</code> the
	 *        default value will be returned
	 * @return a double value containing the given Property if it was found and
	 *         between <code>min <= value <= max</code>, otherwise
	 *         <code>def</code>
	 */
	public static int[] tryIntegerPropertyArray(Properties p, String propertyName, int def,
			int min, int max) {
		String str;
		StringTokenizer token;
		int[] d;
		int value;
		int i;

		try {
			if ((str = p.getProperty(propertyName, null)) != null) {
				token = new StringTokenizer(str, ",");
				d = new int[token.countTokens()];
				i = 0;
				while (token.hasMoreElements()) {
					str = token.nextToken();
					try {
						value = Integer.valueOf(str).intValue();
						if (value < min || value > max) {
							d[i] = def;
						} else {
							d[i] = value;
						}
					} catch (NumberFormatException nfe) {
						d[i] = def;
					}
					i++;
				}
				return d;
			} else {
				return new int[] {};
			}
		} catch (Exception e) {
			return new int[] {};
		}
	}

	/**
	 * tries to convert a given property to a long.
	 * @param p the Properties to look for the given String
	 * @param propertyName the Property to search for
	 * @param def the default value, if the Property could not be
	 *        found/converted.
	 * @return a double value containing the given Property if possible,
	 *         otherwise <code>def</code>
	 */
	public static long[] tryLongPropertyArray(Properties p, String propertyName, long def) {
		return tryLongPropertyArray(p, propertyName, def, Long.MIN_VALUE, Long.MAX_VALUE);
	}

	/**
	 * tries to convert a given property to a long.
	 * @param p the Properties to look for the given String
	 * @param propertyName the Property to search for
	 * @param def the default value, if the Property could not be
	 *        found/converted.
	 * @param min if the found value is lower than <code>min</code> the
	 *        default value will be returned
	 * @param max if the found value is greater than <code>max</code> the
	 *        default value will be returned
	 * @return a double value containing the given Property if it was found and
	 *         between <code>min <= value <= max</code>, otherwise
	 *         <code>def</code>
	 */
	public static long[] tryLongPropertyArray(Properties p, String propertyName, long def,
			long min, long max) {
		String str;
		StringTokenizer token;
		long[] d;
		long value;
		int i;

		try {
			if ((str = p.getProperty(propertyName, null)) != null) {
				token = new StringTokenizer(str, ",");
				d = new long[token.countTokens()];
				i = 0;
				while (token.hasMoreElements()) {
					str = token.nextToken();
					try {
						value = Long.valueOf(str).longValue();
						if (value < min || value > max) {
							d[i] = def;
						} else {
							d[i] = value;
						}
					} catch (NumberFormatException nfe) {
						d[i] = def;
					}
					i++;
				}
				return d;
			} else {
				return new long[] {};
			}
		} catch (Exception e) {
			return new long[] {};
		}
	}

	/**
	 * tries to convert a given property to an array of strings.
	 * @param p the Properties to look for the given String
	 * @param propertyName the Property to search for
	 * @return a String array containing all strings that where seperated by a
	 *         comma
	 */

	public static String[] tryStringPropertyArray(Properties p, String propertyName) {
		String str;
		StringTokenizer token;
		String[] d;
		int i;

		try {
			str = p.getProperty(propertyName, null);
			if (str != null) {
				token = new StringTokenizer(str, ",");
				d = new String[token.countTokens()];
				i = 0;
				while (token.hasMoreElements()) {
					d[i] = token.nextToken().trim();
					i++;
				}
				return d;
			} else {
				return new String[] {};
			}
		} catch (Exception e) {
			return new String[] {};
		}
	}

	public static Integer[] nativeToObjectArray(int[] arr) {
		Integer[] ret = new Integer[arr.length];

		for (int i = 0; i < arr.length; i++) {
			ret[i] = new Integer(arr[i]);
		}
		return ret;
	}

	/**
	 * tries to find a given double-value in a given double-array an returns its
	 * index.
	 * @return index of the given double-value or -1 if the array does not
	 *         contain the given value
	 */

	public static double getArrayIndex(double whatToFind, double[] dArray) {
		for (int i = 0; i < dArray.length; i++) {
			if (dArray[i] == whatToFind) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * tries to find a given long-value in a given long-array an returns its
	 * index.
	 * @return index of the given long-value or -1 if the array does not contain
	 *         the given value
	 */

	public static int getArrayIndex(long whatToFind, long[] dArray) {
		for (int i = 0; i < dArray.length; i++) {
			if (dArray[i] == whatToFind) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * tries to find a given int-value in a given int-array an returns its
	 * index.
	 * @return index of the given int-value or -1 if the array does not contain
	 *         the given value
	 */

	public static int getArrayIndex(int whatToFind, int[] dArray) {
		for (int i = 0; i < dArray.length; i++) {
			if (dArray[i] == whatToFind) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * tries to find a given float-value in a given float-array an returns its
	 * index.
	 * @return index of the given float-value or -1 if the array does not
	 *         contain the given value
	 */

	public static int getArrayIndex(float whatToFind, float[] dArray) {
		for (int i = 0; i < dArray.length; i++) {
			if (dArray[i] == whatToFind) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * tries to find a given String-value in a given String-array an returns its
	 * index. does not find null
	 * @param whatToFind a String != null
	 * @return index of the given String-value or -1 if the array does not
	 *         contain the given value
	 */

	public static int getArrayIndex(String whatToFind, String[] dArray) {
		if (whatToFind == null) {
			return -1;
		} else {
			for (int i = 0; i < dArray.length; i++) {
				if (whatToFind.equals(dArray[i])) {
					return i;
				}
			}
		}
		return -1;
	}

	/**
	 * tries to find a given String-value in a given String-array an returns its
	 * index. difference to <code>getArrayIndex</code>: it compares with
	 * String.equalsIgnoreCase()
	 * @return index of the given String-value or -1 if the array does not
	 *         contain the given value
	 */

	public static int getArrayIndexIC(String whatToFind, String[] dArray) {
		for (int i = 0; i < dArray.length; i++) {
			if (dArray[i].equalsIgnoreCase(whatToFind)) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * returns true, if the given string can be converted into a Double value
	 */
	public static boolean isValidDouble(String s) {
		try {
			Double.parseDouble(s);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * returns true, if the given string can be converted into a Long value
	 */
	public static boolean isValidLong(String s) {
		try {
			Long.parseLong(s);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * returns true, if the given string can be converted into an Integer value
	 */
	public static boolean isValidInteger(String s) {
		try {
			Integer.parseInt(s);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * compares multiple Objects about their equality. example: param toCompare
	 * is: { {"String", "String", "String"}, {"Key", "Key"}, {"Mouth", "Mouth",
	 * "Eye"} } return value will be { true, true, false };
	 */
	public static boolean[] multipleEqual(Object[][] toCompare) {
		boolean[] ret = new boolean[toCompare.length];

		for (int i = 0; i < toCompare.length; i++) {
			ret[i] = true;
			for (int j = 1; j < toCompare[i].length && ret[i]; j++) {
				ret[i] = toCompare[i][0].equals(toCompare[i][j]);
			}
		}
		return ret;
	}

	/** ****************************************************************************** */
	
	/**
	 * ******** getArrayString functions are mainly for simplifying debug
	 * output. ***
	 */
	
	/** ****************************************************************************** */

	/**
	 * returns a String containing all of the values from the array, seperated
	 * by <code>seperator</code>
	 */
	public static String getArrayString(int[] array, String seperator) {
		if (array == null) {
			return "";
		}
		String ret = new String();

		for (int i = 0; i < array.length; i++) {
			if (i > 0) {
				ret += seperator;
			}
			ret += array[i];
		}
		return ret;
	}

	/**
	 * returns a String containing all of the values from the array, seperated
	 * by <code>seperator</code>
	 */
	public static String getArrayString(long[] array, String seperator) {
		if (array == null) {
			return "";
		}
		String ret = new String();

		for (int i = 0; i < array.length; i++) {
			if (i > 0) {
				ret += seperator;
			}
			ret += array[i];
		}
		return ret;
	}

	/**
	 * returns a String containing all of the values from the array, seperated
	 * by <code>seperator</code>
	 */
	public static String getArrayString(double[] array, String seperator) {
		if (array == null) {
			return "";
		}
		String ret = new String();

		for (int i = 0; i < array.length; i++) {
			if (i > 0) {
				ret += seperator;
			}
			ret += array[i];
		}
		return ret;
	}
    
	/**
	 * Copies a sourcefile to the given destination
	 * @param source
	 * @param dest
	 * @throws IOException
	 */
	public static void copyFile(File source, File dest) throws IOException {

		if (!dest.exists()) {
			dest.createNewFile();
		}
		InputStream in = null;
		OutputStream out = null;

		try {
			in = new FileInputStream(source);
			out = new FileOutputStream(dest);

			// Transfer bytes from in to out
			byte[] buf = new byte[1024];
			int len;

			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
		} finally {
			if (in != null) {
				in.close();
			}
			if (out != null) {
				out.close();
			}
		}

	}
    
	/**
	 * returns a String containing all of the values from the array, seperated
	 * by <code>seperator</code>
	 */
	public static String getArrayString(String[] array, String seperator) {
		return getArrayString(array, seperator, array.length);
	}

	/**
	 * returns a String containing all of the values from the array, seperated
	 * by <code>seperator</code>
	 */
	public static String getArrayString(String[] array, String seperator, int count) {
		StringBuffer buf = new StringBuffer(array.length);

		if (array == null) {
			return "";
		}
		for (int i = 0; i < count; i++) {
			if (i > 0) {
				buf.append(seperator);
			}
			buf.append(array[i]);
		}
		return buf.toString();
	}

	public static String repeatString(String str, int count, String seperator) {
		if (count <= 0) {
			return "";
		}
		StringBuffer buf = new StringBuffer(count * 2 - 1);

		buf.append(str);
		for (int i = 1; i < count; i++) {
			buf.append(seperator);
			buf.append(str);
		}
		return buf.toString();
	}

	/**
	 * returns true, if all values of the boolean array are true, false
	 * otherwise
	 */
	public static boolean allTrue(boolean[] all) {
		for (int i = 0; i < all.length; i++) {
			if (!all[i]) {
				return false;
			}
		}
		return true;
	}

	/**
	 * @param day 1-31
	 * @param month 1-12
	 * @param year 1970 - 2038
	 * @return
	 */
	public static long toUnixTimestamp(int day, int month, int year) {
		Calendar c = GregorianCalendar.getInstance();

		c.set(year, month - 1, day);
		return c.getTimeInMillis() / 1000;
	}

	/**
	 * helper class to represent an object pair
	 * @author norbert
	 */
	public static class MiscObjectPair {
		private Object left;

		private Object right;

		/**
		 * constructor
		 * @param left left (first) object of pair
		 * @param right right (second) object of pair
		 */
		public MiscObjectPair(Object left, Object right) {
			this.left = left;
			this.right = right;
		}

		/**
		 * get the first object, alias for {@link #getLeft()}
		 * @return first (or left) object
		 */
		public Object getFirst() {
			return left;
		}

		/**
		 * get the left object, alias for {@link #getFirst()}
		 * @return left (or first) object
		 */
		public Object getLeft() {
			return left;
		}

		/**
		 * get the right object, alias for {@link #getSecond()}
		 * @return right (or second) object
		 */
		public Object getRight() {
			return right;
		}

		/**
		 * get the second object, alias for {@link #getRight()}
		 * @return second (or right) object
		 */
		public Object getSecond() {
			return right;
		}
	}

	/**
	 * get the intersection of two object lists, that means the list of objects
	 * that are contained in both lists. The resulting list is returned as list
	 * of object pairs where the left part comes from the left list and the
	 * right part from the right list.
	 * @param left list of "left" objects
	 * @param right list of "right" objects
	 * @return intersection of the lists as list of object pairs
	 */
	public static List getIntersection(List left, List right) {
		// check for empty lists
		if (left == null || left.size() == 0 || right == null || right.size() == 0) {
			// return an empty list
			return Collections.EMPTY_LIST;
		}

		// copy both lists into temp lists
		List leftTemp = new Vector(left);
		List rightTemp = new Vector(right);

		// remove from leftTemp all objects that are not contained in right
		leftTemp.retainAll(right);
		// remove from rightTemp all objects that are not contained in left
		rightTemp.retainAll(left);

		// check whether the lists have equal size now, if not an error occurred
		if (leftTemp.size() != rightTemp.size()) {
			NodeLogger.getLogger(MiscUtils.class).error("intersection lists must be equal but are not, cannot compute intersection");
			return null;
		}

		// when the lists are both empty, return an empty pair list
		if (leftTemp.size() == 0) {
			return Collections.EMPTY_LIST;
		}

		// sort both temp collections
		Collections.sort(leftTemp);
		Collections.sort(rightTemp);

		// generate list of object pairs
		List pairList = new Vector(leftTemp.size());

		for (int i = 0; i < leftTemp.size(); i++) {
			pairList.add(new MiscObjectPair(leftTemp.get(i), rightTemp.get(i)));
		}

		return pairList;
	}

	/**
	 * get the union of the left and right list
	 * @param left left list
	 * @param right right list
	 * @return union of the lists (where each object is contained just once)
	 */
	public static List getUnion(List left, List right) {
		Set unionSet = new HashSet();

		if (left != null) {
			unionSet.addAll(left);
		}
		if (right != null) {
			unionSet.addAll(right);
		}

		List union = new Vector(unionSet);

		return union;
	}

	/**
	 * get the difference of two lists
	 * @param minuend minuend list
	 * @param subtrahend subtrahend list
	 * @return the difference list
	 */
	public static List getDifference(List minuend, List subtrahend) {
		if (minuend == null) {
			// when the minuend is null, the difference is null
			// (what do you get, when you take something away from nothing?)
			return null;
		}
		if (subtrahend == null) {
			// when the subtrahend is null, the difference is the minuend
			return minuend;
		}

		// build the difference
		List difference = new Vector();

		difference.addAll(minuend);
		difference.removeAll(subtrahend);

		return difference;
	}

	/**
	 * Compare two objects.
	 * <br>
	 * Following rules apply in comparison: 
	 * <ul>
	 *  <li>NULL objects are smaller than non-NULL</li>
	 *  <li>Number objects are compared by their value</li>
	 *  <li>Date objects are compared by their Date value</li>
	 *  <li>when Numbers are compared with Dates, the numbers are interpreted as timestamps (in s)</li>
	 *  <li>all other comparisons are done with the string representations of the objects</li>
	 * </ul>
	 * @param o1 first object
	 * @param o2 second object
	 * @param caseSensitive true when Strings shall be compared case sensitive, false if not
	 * @return -1 when o1 &lt; o2, 0 when o1 == o2, +1 when o1 &gt; o2
	 */
	public static int compareObjects(Object o1, Object o2, boolean caseSensitive) {
		return compareObjects(o1, o2, caseSensitive, null, null);
	}

	/**
	 * Compare two objects.
	 * <br>
	 * Following rules apply in comparison: 
	 * <ul>
	 *  <li>NULL objects are smaller than non-NULL</li>
	 *  <li>Number objects are compared by their value</li>
	 *  <li>Date objects are compared by their Date value</li>
	 *  <li>when Numbers are compared with Dates, the numbers are interpreted as timestamps (in s)</li>
	 *  <li>all other comparisons are done with the string representations of the objects</li>
	 * </ul>
	 * @param o1 first object
	 * @param o2 second object
	 * @param collator collator to be used to compare strings locale specific (may be null to do not locale specific comparisons)
	 * @param locale locale to be used for sorting (should fit to the given collator)
	 * @return -1 when o1 &lt; o2, 0 when o1 == o2, +1 when o1 &gt; o2
	 */
	public static int compareObjects(Object o1, Object o2, Collator collator, Locale locale) {
		return compareObjects(o1, o2, false, collator, locale);
	}

	/**
	 * Compare two objects.
	 * <br>
	 * Following rules apply in comparison: 
	 * <ul>
	 *  <li>NULL objects are smaller than non-NULL</li>
	 *  <li>Number objects are compared by their value</li>
	 *  <li>Date objects are compared by their Date value</li>
	 *  <li>when Numbers are compared with Dates, the numbers are interpreted as timestamps (in s)</li>
	 *  <li>all other comparisons are done with the string representations of the objects</li>
	 * </ul>
	 * @param o1 first object
	 * @param o2 second object
	 * @param caseSensitive true when Strings shall be compared case sensitive, false if not.
	 * @param collator collator to be used to compare strings locale specific (may be null to do not locale specific comparisons)
	 * @param locale locale to be used for sorting (should fit to the given collator)
	 * @return -1 when o1 &lt; o2, 0 when o1 == o2, +1 when o1 &gt; o2
	 */
	public static int compareObjects(Object o1, Object o2, boolean caseSensitive, Collator collator, Locale locale) {
		// check the trivial cases (one or both objects are NULL)
		if (o1 == null) {
			return o2 == null ? 0 : -1;
		} else if (o2 == null) {
			return 1;
		}

		if (o1 instanceof Number) {
			if (o2 instanceof Number) {
				// compare two Numbers
				return o1.equals(o2) || ((Number) o1).doubleValue() == ((Number) o2).doubleValue()
						? 0
						: (((Number) o1).doubleValue() < ((Number) o2).doubleValue() ? -1 : 1);
			} else if (o2 instanceof Date) {
				// compare a Number with a Date
				Date d1 = new Date(((Number) o1).longValue() * 1000L);
				Date d2 = (Date) o2;

				return d1.equals(d2) ? 0 : (d1.before(d2) ? -1 : 1);
			}
		} else if (o1 instanceof Date) {
			if (o2 instanceof Number) {
				// compare a Date with a Number
				Date d1 = (Date) o1;
				Date d2 = new Date(((Number) o2).longValue() * 1000L);

				return d1.equals(d2) ? 0 : (d1.before(d2) ? -1 : 1);
			} else if (o2 instanceof Date) {
				// compare two Dates
				Date d1 = (Date) o1;
				Date d2 = (Date) o2;

				return d1.equals(d2) ? 0 : (d1.before(d2) ? -1 : 1);
			}
		} else if (o1 instanceof Comparable && !(o1 instanceof String)) {
			return ((Comparable) o1).compareTo(o2);
		}

		int compare = 0;

		if (collator != null) {
			if (caseSensitive) {
				compare = collator.compare(o1.toString(), o2.toString());
			} else if (locale != null) {
				compare = collator.compare(o1.toString().toLowerCase(locale), o2.toString().toLowerCase(locale));
			} else {
				compare = collator.compare(o1.toString().toLowerCase(), o2.toString().toLowerCase());
			}
		} else {
			// compare the objects as Strings
			compare = caseSensitive ? o1.toString().compareTo(o2.toString()) : o1.toString().compareToIgnoreCase(o2.toString());
		}
		return compare < 0 ? -1 : compare > 0 ? 1 : 0;
	}

	/**
	 * Compare two objects.
	 * <br>
	 * Following rules apply in comparison: 
	 * <ul>
	 *  <li>NULL objects are smaller than non-NULL</li>
	 *  <li>Number objects are compared by their value</li>
	 *  <li>Date objects are compared by their Date value</li>
	 *  <li>when Numbers are compared with Dates, the numbers are interpreted as timestamps (in s)</li>
	 *  <li>all other comparisons are done with the string representations of the objects (case insensitive)</li>
	 * </ul>
	 * @param o1 first object
	 * @param o2 second object
	 * @return -1 when o1 &lt; o2, 0 when o1 == o2, +1 when o1 &gt; o2
	 */
	public static int compareObjects(Object o1, Object o2) {
		return compareObjects(o1, o2, false);
	}

	/**
	 * Check whether the two objects are equal.
	 * MiscUtils.objectsEqual(o1, o2); is equivalent to compareObjects(o1, o2) == 0
	 * @param o1 first object
	 * @param o2 second object
	 * @return true when the objects are equal, false if not
	 */
	public static boolean objectsEqual(Object o1, Object o2) {
		return objectsEqual(o1, o2, false);
	}

	/**
	 * Check whether the two objects are equal.
	 * MiscUtils.objectsEqual(o1, o2); is equivalent to compareObjects(o1, o2) == 0
	 * @param o1 first object
	 * @param o2 second object
	 * @param caseSensitive whether String comparisons shall be case sensitive or not
	 * @return true when the objects are equal, false if not
	 */
	public static boolean objectsEqual(Object o1, Object o2, boolean caseSensitive) {
		return compareObjects(o1, o2, false) == 0;
	}
}
