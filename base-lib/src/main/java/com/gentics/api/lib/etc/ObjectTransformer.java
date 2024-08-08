/*
 * @author Erwin Mascher (e.mascher@gentics.com)
 * @date 11.12.2003
 * @version $Id: ObjectTransformer.java,v 1.2 2009-12-16 16:12:08 herbert Exp $
 */
package com.gentics.api.lib.etc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Clob;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.render.Renderable;

/**
 * Helper class to transform objects into specific types (classes).
 */
public final class ObjectTransformer {

	/**
	 * the logger
	 */
	protected static NodeLogger logger = NodeLogger.getNodeLogger(ObjectTransformer.class);

	/**
	 * private constructor to avoid instantiation
	 */
	private ObjectTransformer() {}

	/**
	 * Get the given object as a collection.
	 * <ul>
	 * <li>When the object is a collection, it is returned unmodified</li>
	 * <li>When the object is an array, it is transformed into a {@link List}</li>
	 * <li>When the object is null, the default value is returned</li>
	 * <li>In any other case, a {@link Vector} containing the given object is returned</li>
	 * </ul>
	 * @param o object to get as collection
	 * @param defaultValue default value
	 * @return o as collection or default  value when o is null
	 */
	public static Collection getCollection(Object o, Collection defaultValue) {
		if (o == null) {
			return defaultValue;
		} else if (o instanceof Collection) {
			return (Collection) o;
		} else if (o instanceof Object[]) {
			return Arrays.asList((Object[]) o);
		} else {
			Collection c = new Vector(1);

			c.add(o);
			return c;
		}
	}

	/**
	 * Get an integer representation of the given object
	 * @param o object to transform
	 * @param defaultValue default value (when the object does not have an
	 *        integer representation)
	 * @return integer representation of the object or defaultValue
	 */
	public static int getInt(Object o, int defaultValue) {
		if (o == null) {
			return defaultValue;
		}
		if (o instanceof Number) {
			return ((Number) o).intValue();
		}
		try {
			return Integer.parseInt(o.toString().trim());
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	/**
	 * Get a float representation of the given object.
	 * 
	 * @param o object to transform
	 * @param defaultValue default value (when the object does not have an
	 *        float representation)
	 * @return float representation of the object or defaultValue
	 */
	public static float getFloat(Object o, float defaultValue) {
		if (o == null) {
			return defaultValue;
		}
		if (o instanceof Number) {
			float v = ((Number) o).floatValue();
			return Float.isNaN(v) ? defaultValue : v;
		}
		try {
			float v = Float.parseFloat(o.toString().trim());
			return Float.isNaN(v) ? defaultValue : v;
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	/**
	 * Get the Integer representation of the given object
	 * @param o object to transform
	 * @param defaultValue default value (when the object does not have an
	 *        Integer representation)
	 * @return Integer or defaultValue
	 */
	public static Integer getInteger(Object o, Integer defaultValue) {
		if (o == null) {
			return defaultValue;
		} else if (o instanceof Integer) {
			return (Integer) o;
		} else if (o instanceof Number) {
			return new Integer(((Number) o).intValue());
		}
		try {
			return new Integer(o.toString().trim());
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	/**
	 * Get a long representation of the given object
	 * @param o object to transform
	 * @param defaultValue default value (when the object does not have a long
	 *        representation)
	 * @return long value of the object or defaultValue
	 */
	public static long getLong(Object o, long defaultValue) {
		if (o == null) {
			return defaultValue;
		}
		if (o instanceof Number) {
			return ((Number) o).longValue();
		}
		if (o instanceof Date) {
			return ((Date) o).getTime();
		}
		try {
			return Long.parseLong(o.toString());
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	/**
	 * Get a Long representation of the given object
	 * @param o object to transform
	 * @param defaultValue default value (when the object does not have a Long
	 *        representation)
	 * @return Long value of the object or defaultValue
	 */
	public static Long getLong(Object o, Long defaultValue) {
		if (o == null) {
			return defaultValue;
		} else if (o instanceof Long) {
			return (Long) o;
		} else if (o instanceof Number) {
			return new Long(((Number) o).longValue());
		}
		try {
			return new Long(o.toString());
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	/**
	 * Get a double value of the given object
	 * @param o object to transform
	 * @param defaultValue default value (when the object does not have a double
	 *        representation)
	 * @return double value of the object or defaultValue
	 */
	public static double getDouble(Object o, double defaultValue) {
		if (o == null) {
			return defaultValue;
		}
		if (o instanceof Number) {
			return ((Number) o).doubleValue();
		}
		if (o instanceof Date) {
			return ((Date) o).getTime();
		}
		try {
			return Double.parseDouble(o.toString());
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	/**
	 * Get the Double representation of the object
	 * @param o object to transform
	 * @param defaultValue default value
	 * @return Double value of the object or defaultValue
	 */
	public static Double getDouble(Object o, Double defaultValue) {
		if (o == null) {
			return defaultValue;
		} else if (o instanceof Double) {
			return (Double) o;
		} else if (o instanceof Number) {
			return new Double(((Number) o).doubleValue());
		} else {
			try {
				return new Double(o.toString());
			} catch (NumberFormatException e) {
				return defaultValue;
			}
		}
	}

	/**
	 * Get the boolean value of the given object.<br> <b>true</b>, <b>1</b>,
	 * <b>yes</b> and <b>on</b> will be interpreted as TRUE.<br> <b>false</b>,
	 * <b>0</b>, <b>no</b> and <b>off</b> will be interpreted as FALSE.<br>
	 * Everything else cannot be interpreted as boolean and will return the
	 * defaultValue. All comparisons are made case-insensitive.
	 * @param o object to transform
	 * @param defaultValue default value (when the object cannot be interpreted
	 *        as boolean)
	 * @return boolean value of the object or defaultValue
	 */
	public static boolean getBoolean(Object o, boolean defaultValue) {
		if (o == null) {
			return defaultValue;
		}
		if (o instanceof Boolean) {
			return ((Boolean) o).booleanValue();
		}
		String oStr = o.toString();

		if ("true".equalsIgnoreCase(oStr) || "1".equalsIgnoreCase(oStr) || "yes".equalsIgnoreCase(oStr) || "on".equalsIgnoreCase(oStr)) {
			return true;
		} else if ("false".equalsIgnoreCase(oStr) || "0".equalsIgnoreCase(oStr) || "no".equalsIgnoreCase(oStr) || "off".equalsIgnoreCase(oStr)) {
			return false;
		} else {
			return defaultValue;
		}
	}

	/**
	 * Get the Boolean value of the given object.<br> <b>true</b>, <b>1</b>,
	 * <b>yes</b> and <b>on</b> will be interpreted as TRUE.<br> <b>false</b>,
	 * <b>0</b>, <b>no</b> and <b>off</b> will be interpreted as FALSE.<br>
	 * Everything else cannot be interpreted as boolean and will return the
	 * defaultValue. All comparisons are made case-insensitive.
	 * @param o object to transform
	 * @param defaultValue default value (when the object cannot be interpreted
	 *        as boolean)
	 * @return Boolean value of the object or defaultValue
	 */
	public static Boolean getBoolean(Object o, Boolean defaultValue) {
		if (o == null) {
			return defaultValue;
		} else if (o instanceof Boolean) {
			return (Boolean) o;
		} else {
			String oStr = o.toString();

			if ("true".equalsIgnoreCase(oStr) || "1".equalsIgnoreCase(oStr) || "yes".equalsIgnoreCase(oStr) || "on".equalsIgnoreCase(oStr)) {
				return Boolean.TRUE;
			} else if ("false".equalsIgnoreCase(oStr) || "0".equalsIgnoreCase(oStr) || "no".equalsIgnoreCase(oStr) || "off".equalsIgnoreCase(oStr)) {
				return Boolean.FALSE;
			} else {
				return defaultValue;
			}
		}
	}

	/**
	 * Get the object as binary value (byte array)
	 * @param o object to transform
	 * @param defaultValue default value
	 * @return byte array or default value
	 */
	public static byte[] getBinary(Object o, byte[] defaultValue) {
		if (o == null) {
			return defaultValue;
		} else if (o instanceof byte[]) {
			return (byte[]) o;
		} else {
			return defaultValue;
		}
	}

	/**
	 * Transform the given object into a string (its string representation)
	 * @param o object to transform
	 * @param defaultValue default value (when the object is null)
	 * @return string representation of the object or the defaultvalue
	 */
	public static String getString(Object o, String defaultValue) {
		if (o == null) {
			return defaultValue;
		}
		if (o instanceof String) {
			return (String) o;
		}
		if (o instanceof char[]) {
			return new String((char[])o);
		}
		if (o instanceof Clob) {
			Clob clob = (Clob) o;

			try {
				return clob.getSubString(1, (int) clob.length());
			} catch (SQLException e) {
				logger.error("Error while fetching clob value: ", e);
				return defaultValue;
			}
		}
		if (o instanceof Renderable) {
			try {
				return ((Renderable) o).render();
			} catch (NodeException e) {
				return defaultValue;
			}
		}
		return o.toString();
	}

	/**
	 * Transform the given object into a string array
	 * @param o object to transform
	 * @param defaultValue default value, when the object cannot be interpreted as string array
	 * @return string array representation of the object or the defaultvalue
	 */
	public static String[] getStringArray(Object o, String[] defaultValue) {
		if (o == null) {
			return defaultValue;
		} else if (o instanceof String[]) {
			return (String[]) o;
		} else if (o instanceof Collection) {
			Collection c = (Collection) o;
			String[] retval = new String[c.size()];
			Iterator iter = c.iterator();

			for (int i = 0; i < retval.length; i++) {
				retval[i] = getString(iter.next(), null);
			}
			return retval;
		} else {
			return new String[] { getString(o, null)};
		}
	}

	/**
	 * Get the Date representation of the given object
	 * @param o object to transform
	 * @param defaultValue default value
	 * @return object as Date or default value
	 */
	public static Date getDate(Object o, Date defaultValue) {
		if (o == null) {
			return defaultValue;
		} else if (o instanceof Date) {
			return (Date) o;
		} else if (o instanceof Long) {
			return new Date(((Long) o).longValue());
		} else if (o instanceof Integer) {
			return new Date(((Integer) o).intValue() * 1000L);
		} else {
			return defaultValue;
		}
	}

	/**
	 * Transform the given object into a token. A token does not contain
	 * carriage return (#xD), line feed (#xA) or tab (#x9), does not have
	 * leading or trailing whitespace and no more than one space (#x20) in a
	 * sequence.
	 * @param o object to transform
	 * @param defaultValue default value (when the object is null)
	 * @return string representation of the object as token or the defaultvalue
	 */
	public static String getToken(Object o, String defaultValue) {
		return StringUtils.collapseString(getString(o, defaultValue));
	}

	/**
	 * Check whether the given value somehow is empty. The value is empty if at
	 * least one of the following is true:
	 * <ul>
	 * <li>value is null</li>
	 * <li>value is an empty Map</li>
	 * <li>value is an empty Collection</li>
	 * <li>value is an empty Array</li>
	 * <li>value is an empty String</li>
	 * </ul>
	 * @param value value to check for emptyness
	 * @return true when the value is empty, false if not
	 */
	public static boolean isEmpty(Object value) {
		if (value == null) {
			// null is empty
			return true;
		} else if (value instanceof Map) {
			// empty map is empty
			return ((Map<?, ?>) value).isEmpty();
		} else if (value instanceof Collection) {
			// empty collection is empty
			return ((Collection<?>) value).isEmpty();
		} else if (value instanceof Object[]) {
			// empty array is empty
			return ((Object[]) value).length == 0;
		} else if (value instanceof String) {
			// empty string is empty
			return ((String) value).length() == 0;
		} else {
			// everything else is nonempty
			return false;
		}
	}

	/**
	 * Encode the given binary data into hexcode (leading zeros, all uppercase
	 * letters)
	 * @param binaryData binary data to encode
	 * @return encoded binary data
	 */
	public static String encodeBinary(byte[] binaryData) {
		StringBuffer encoded = new StringBuffer();

		if (binaryData != null) {
			for (int i = 0; i < binaryData.length; ++i) {
				int unsignedInt = (int) binaryData[i];

				if (unsignedInt < 0) {
					unsignedInt += 256;
				}
				String code = Integer.toHexString(unsignedInt).toUpperCase();

				if (code.length() == 1) {
					encoded.append("0");
				}
				encoded.append(code);
			}
		}

		return encoded.toString();
	}

	/**
	 * Decode the encoded binary data (hexcode, leading zeros, all uppercase
	 * letters)
	 * @param encoded encoded binary data
	 * @return decoded data
	 * @throws IllegalArgumentException if the encoded data cannot be decoded
	 */
	public static byte[] decodeBinary(String encoded) {
		if (encoded == null || encoded.length() % 2 != 0) {
			throw new IllegalArgumentException("Encoded string must not be null and must have an even number of characters");
		}

		byte[] decoded = new byte[encoded.length() / 2];

		for (int i = 0; i < encoded.length(); i += 2) {
			try {
				int value = Integer.parseInt(encoded.substring(i, i + 2), 16);

				if (value > 127) {
					value -= 256;
				}
				decoded[i / 2] = (byte) value;
			} catch (NumberFormatException ex) {
				throw new IllegalArgumentException("Invalid encoded string {" + encoded + "} at position " + i);
			}
		}

		return decoded;
	}

	/**
	 * Get the timestamp value of the object (if it is a Date) or the
	 * defaultValue for non-Date objects
	 * @param object Date object
	 * @param defaultValue default value
	 * @return timestamp of the object
	 */
	public static int getTimestamp(Object object, int defaultValue) {
		if (object instanceof Date) {
			return (int) (((Date) object).getTime() / 1000L);
		} else {
			return defaultValue;
		}
	}

	/**
	 * Get the timestamp in milliseconds of the object (if it is a Date) or the
	 * defaultValue for non-Date objects
	 * @param object Date object
	 * @param defaultValue default value
	 * @return timestamp of the object or the default value
	 */
	public static long getLongTimestamp(Object object, long defaultValue) {
		if (object instanceof Date) {
			return ((Date) object).getTime();
		} else {
			return defaultValue;
		}
	}

	/**
	 * Get the number represented by the given object
	 * @param object object to transform into a number
	 * @param defaultValue defaultvalue when object is null, or does not represent a number
	 * @return number representation of the object or defaultValue
	 */
	public static Number getNumber(Object object, Number defaultValue) {
		if (object instanceof Number) {
			return (Number) object;
		} else if (object instanceof Date) {
			return new Long(((Date) object).getTime());
		} else if (!isEmpty(object)) {
			try {
				return new Double(object.toString());
			} catch (NumberFormatException e) {
				return defaultValue;
			}
		} else {
			return defaultValue;
		}
	}
    
	/**
	 * Tries to transforms a given object to the specified class.
	 * 
	 * (Currently only supports converting from Strings to Integers)
	 * @param baseObject The base object which to transform
	 * @param targetClass The target class to which the baseObect shall be transformed.
	 * @return returns the transformed object, or null if unsuccessful.
	 */
	public static Object transformObject(Object baseObject, Class targetClass) {
		if (targetClass.isAssignableFrom(baseObject.getClass())) {
			// Wow we are lucky .. caller gave us what he wants..
			return baseObject;
		}
        
		if (baseObject instanceof String) {
			if (targetClass.isPrimitive()) {
				if (targetClass.equals(Integer.TYPE)) {
					return new Integer((String) baseObject);
				}
			} else {
				if (targetClass.equals(Integer.class)) {
					return new Integer((String) baseObject);
				}
			}
		}
		logger.warn("Unable to transform object type {" + baseObject.getClass().getName() + "} to {" + targetClass.toString() + "}");
		return null;
	}

	/**
	 * Get object as instance of the given class
	 * @param <T> type of the result class
	 * @param classOfT class
	 * @param object object
	 * @return object as instance of the class or null
	 */
	public static <T> T get(Class<T> classOfT, Object object) {
		return get(classOfT, object, null);
	}

	/**
	 * Get object as instance of the given class
	 * @param <T> type of the result class
	 * @param classOfT class
	 * @param object object
	 * @param defaultObject default object
	 * @return object as instance of the class or the default object
	 */
	public static <T> T get(Class<T> classOfT, Object object, T defaultObject) {
		if (object == null) {
			return null;
		}
		try {
			return classOfT.cast(object);
		} catch (ClassCastException e) {
			return defaultObject;
		}
	}

	/**
	 * Read data coming from the given input stream, using the default charset.
	 * The input stream is not closed in this function.
	 * @param inputStream input stream containing data
	 * @return string
	 * @throws IOException when reading fails
	 */
	public static String readInputStreamIntoString(InputStream inputStream) throws IOException {
		return readInputStreamIntoString(inputStream, null);
	}

	/**
	 * Read data coming from the given input stream, using the given charset.
	 * The input stream is not closed in this function.
	 * @param inputStream input stream containing data
	 * @param charset charset to be used
	 * @return string
	 * @throws IOException when reading fails
	 */
	public static String readInputStreamIntoString(InputStream inputStream, String charset) throws IOException {
		BufferedReader input = new BufferedReader(
				StringUtils.isEmpty(charset) ? new InputStreamReader(inputStream) : new InputStreamReader(inputStream, charset));
		String r = null;
		StringBuffer res = new StringBuffer();

		while ((r = input.readLine()) != null) {
			res.append(r);
			res.append('\n');
		}
		return res.toString();
	}

	/**
	 * Helper class that extends {@link Runnable} to read data from an input stream in a separate thread
	 */
	public static class InputStreamReaderRunnable implements Runnable {
		private InputStream stream;
		private String charset;
		private String ret;
		private Throwable throwable;

		/**
		 * Create an instance of the helper class that will read from the given stream using the default charset
		 * @param stream stream to read from
		 */
		public InputStreamReaderRunnable(InputStream stream) {
			this(stream, null);
		}

		/**
		 * Create an instance of the helper class that will read from the given stream using the given charset
		 * @param stream stream to read from
		 * @param charset charset to use
		 */
		public InputStreamReaderRunnable(InputStream stream, String charset) {
			this.stream = stream;
			this.charset = charset;
		}

		/* (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		public void run() {
			try {
				ret = readInputStreamIntoString(stream, charset);
			} catch (IOException e) {
				throwable = e;
			}
		}

		/**
		 * Get the throwable, if one was caught reading from the stream
		 * @return throwable or null if none caught
		 */
		public Throwable getThrowable() {
			return throwable;
		}

		/**
		 * Get the read string or null if more data can be read from the stream
		 * @return the read string or null
		 */
		public String getString() {
			return ret;
		}
	}

	/**
	 * Compare objects, care also for null objects
	 * @param first first object
	 * @param second second object
	 * @param nullfirst true when nulls shall be smaller than nonnulls, false if
	 *        nulls are bigger
	 * @return -1 when the first object is smaller, 0 when they are equal (or
	 *         both null), 1 when the second object is smaller
	 */
	public static int compareObjects(Comparable first, Comparable second, boolean nullfirst) {
		if (first == null) {
			if (second == null) {
				return 0;
			} else {
				return nullfirst ? -1 : 1;
			}
		} else if (second == null) {
			return nullfirst ? 1 : -1;
		} else {
			return first.compareTo(second);
		}
	}

	/**
	 * Compare objects using Object.equals, but also process null references
	 * @param o1 first object
	 * @param o2 second object
	 * @return whether both are null or o1 equals o2
	 */
	public static boolean equals(Object o1, Object o2) {
		if (o1 == null && o2 == null) {
			return true;
		}
		if (o1 == null || o2 == null) {
			return false;
		}
		return o1.equals(o2);
	}
}
