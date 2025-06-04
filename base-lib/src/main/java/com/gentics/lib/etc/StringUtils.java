package com.gentics.lib.etc;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Collator;
import java.text.ParseException;
import java.text.RuleBasedCollator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.incava.util.diff.Diff;
import org.incava.util.diff.Difference;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.lib.log.NodeLogger;

public class StringUtils {

	private static NodeLogger logger = NodeLogger.getNodeLogger(StringUtils.class);

	/**
	 * pattern to find system properties
	 */
	protected static Pattern findSystemProperties = Pattern.compile("\\$\\{([a-zA-Z0-9\\._]+)\\}");
    
	private static final String HEXDIGITS = "0123456789ABCDEF";

	protected final static int MODE_CHAR = 0;

	protected final static int MODE_TAG = 1;

	protected final static int MODE_WHITESPACE = 2;

	private static RuleBasedCollator correctedCollator = null;

	private static final String[] VOID_HTML_TAGS = new String[] {
			"wbr", "track", "source", "plaintext", "param", "meta", "link", "keygen", 
			"isindex", "input", "img", "hr", "embed", "!Doctype", "!--", "command", "col", "br", "base", "area"
	};

	static {
		try {
			// Create a customized RuleBasedCollator:
			//
			// Our tailored collator will prioritize white spaces, which the
			// Locale.GERMANY collator rules does not do by default.
			//
			// It will also prioritize alphanumeric characters before special
			// characters.  We do this in order to maintain Content.Node's
			// canonical order that was used when generating the tree (when
			// this used to be done via PHP).
			//
			// Our "corrected" collation rules will be:
			// punctuation < white space < alphanumeric < special characters

			String rules =
					((RuleBasedCollator) Collator.getInstance(Locale.GERMANY))
						.getRules();

			int punctuationIndex = rules.indexOf("<'\u005f'");
			int alphanumericIndex = rules.indexOf("<0");

			String punctuation = rules.substring(0, punctuationIndex);
			String alphanumeric = rules.substring(alphanumericIndex);
			String special = rules.substring(punctuationIndex, alphanumericIndex);

			rules = punctuation + "<' '" + alphanumeric + special;

			correctedCollator = new RuleBasedCollator(rules);
		} catch (ParseException e) {
			logger.error("Could not initalize RuleBasedCollator.", e);
		}
	}

	/**
	 * Is given string a HTML tag, that requires no closing part (aka void/singleton tag).
	 */
	public static final boolean isVoidTag(String s) {
		return Arrays.stream(VOID_HTML_TAGS).anyMatch(t -> t.equals(s));
	}

	public static int[] splitInt(String str, String expr) {
		StringTokenizer token = new StringTokenizer(str, expr);
		int[] iarr = new int[token.countTokens()];
		int i = 0;

		while (token.hasMoreTokens()) {
			String t = token.nextToken();

			try {
				iarr[i] = Integer.parseInt(t);
			} catch (NumberFormatException e) {
				iarr[i] = 0;
				// e.printStackTrace();
			}
			i++;
		}
		return iarr;
	}

	public static String[] splitString(String str, char delimiter) {
		if (str == null) {
			return new String[] {};
		}
		int idx = str.indexOf(delimiter);

		if (idx < 0) {
			return new String[] { str };
		}

		ArrayList parts = new ArrayList(10);
		int lastIdx = 0;

		while (idx >= 0) {
			parts.add(str.substring(lastIdx, idx));
			lastIdx = idx + 1;
			idx = str.indexOf(delimiter, idx + 1);
		}
		parts.add(str.substring(lastIdx));
		return (String[]) parts.toArray(new String[parts.size()]);
	}

	public static String[] splitString(String str, String delimiter) {
		if (str == null) {
			return new String[] {};
		}
		int idx = str.indexOf(delimiter);

		if (idx < 0) {
			return new String[] { str };
		}

		ArrayList parts = new ArrayList(10);
		int lastIdx = 0;

		while (idx >= 0) {
			parts.add(str.substring(lastIdx, idx));
			lastIdx = idx + 1;
			idx = str.indexOf(delimiter, idx + 1);
		}
		parts.add(str.substring(lastIdx));
		return (String[]) parts.toArray(new String[parts.size()]);
	}
	
	/**
	 * Joins the elements of `list` separated by comma.
	 * @param list
	 * @return
	 */
	public static String joinWithComma(List<?> list) {
		return org.apache.commons.lang.StringUtils.join(list, ",");
	}

	/**
	 * check whether the given string is empty or null
	 * @param str string to check
	 * @return true when the string is empty or null, false if not
	 */
	public static boolean isEmpty(String str) {
		if (str == null) {
			return true;
		}
		return str.length() == 0;
	}

	/**
	 * private static String simpleStripHTML(String text) strips html code from
	 * given string. < will be replaced with &lt; > will be replaced with &gt;
	 * @param text String HTML Code
	 * @return String strippedHTML Code
	 */
	public static String simpleStripHTML(String text) {

		if (text != null) {
			text = text.replaceAll("<", "&lt;");
			return text = text.replaceAll(">", "&gt;");

		} else {
			return "";
		}
	}

	/**
	 * strips character line breaks to HTML breaks (<br>
	 * tags) \r, \r\n, and \n will be replaced with <br>
	 * tags
	 * @param text String
	 * @return String
	 */
	public static String lineBreaksToHtmlBR(String text) {

		String returnText;

		if (text != null) {

			returnText = text.replaceAll("\\r\\n", "<br>");
			returnText = returnText.replaceAll("\\r", "<br>");
			returnText = returnText.replaceAll("\\n", "<br>");

		} else {
			returnText = "";
		}

		return returnText;

	}

	/**
	 * encode the given (unicode) string by converting non-ascii characters into
	 * html entities. Additionally, the "dangerous" characters (&lt;, &gt; &amp; " ') are replaced by their entities.
	 * @param text string to encode
	 * @return encoded string
	 */
	public static String encodeWithEntities(String text) {
		if (text == null || text.length() == 0) {
			// nothing to encode here
			return text;
		}
		StringBuffer encoded = new StringBuffer(text.length());

		for (int i = 0; i < text.length(); ++i) {
			char c = text.charAt(i);

			// check for non-ascii characters and encode them (including the
			// ampersand "&")
			if (c > 127 || c <= 0 || c == 38 || c == '<' || c == '>' || c == '"' || c == '\'') {
				encoded.append("&#").append((int) c).append(";");
			} else {
				encoded.append(c);
			}
		}

		return encoded.toString();
	}

	public static String encodeURL(String text, String encoding) throws UnsupportedEncodingException {
		if (text == null) {
			return null;
		}
		int textLength = text.length();
		int lastCopied = 0;
		int i = 0;
		StringBuffer encoded = new StringBuffer(textLength);

		while (i < textLength) {
			char c;

			// first copy all safe characters
			while (i < textLength && isCharacterURLSafe(text.charAt(i))) {
				++i;
			}
			encoded.append(text.substring(lastCopied, i));

			while (i < textLength && text.charAt(i) == ' ') {
				++i;
				encoded.append('+');
			}

			lastCopied = i;
			while (i < textLength && (c = text.charAt(i)) != ' ' && !isCharacterURLSafe(c)) {
				++i;
			}
			if (i != lastCopied) {
				// transform a substring to safe characters
				transformURL(encoded, text.substring(lastCopied, i), encoding);
				lastCopied = i;
			}
		}

		return encoded.toString();
	}

	/**
	 * Transform the given string into URL encoded String.
	 * @param buffer where to append the URL encoded String
	 * @param toEncode to encode
	 * @param encoding used encoding
	 * @throws UnsupportedEncodingException
	 */
	private final static void transformURL(StringBuffer buffer, String toEncode, String encoding) throws UnsupportedEncodingException {
		byte[] stringBytes = toEncode.getBytes(encoding);

		for (int i = 0; i < stringBytes.length; ++i) {
			buffer.append('%');
			buffer.append(HEXDIGITS.charAt((stringBytes[i] & 0xf0) >> 4));
			buffer.append(HEXDIGITS.charAt(stringBytes[i] & 0x0f));
		}
	}

	/**
	 * Check whether the given character is safe for being part of an url
	 * @param c character to check
	 * @return true when the character is safe for urls, false if not
	 */
	private final static boolean isCharacterURLSafe(char c) {
		return ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '-' || c == '_' || c == '.' || c == '*');
	}

	public static String encodeWithUnicode(String text) {
		if (text == null || text.length() == 0) {
			// nothing to encode here
			return text;
		}
		StringBuffer encoded = new StringBuffer(text.length());

		for (int i = 0; i < text.length(); ++i) {
			char c = text.charAt(i);

			// check for non-ascii characters and encode them
			if (c > 127 || c <= 0) {
				encoded.append("\\u").append(pad(Integer.toHexString((int) c), "0", 4, PADDING_START));
			} else {
				encoded.append(c);
			}
		}

		return encoded.toString();
	}

	public final static int PADDING_START = 1;
	public final static int PADDING_END = 2;
	public final static int PADDING_BOTH = 3;

	public static String pad(String text, String padding, int minLength, int paddingMode) {
		if (text == null || padding == null || padding.length() == 0 || minLength <= 0 || minLength <= text.length()) {
			return text;
		} else {
			StringBuffer paddedString = new StringBuffer(minLength);

			switch (paddingMode) {
			case PADDING_START:
				paddedString.append(repeat(padding, (int) Math.ceil((double) (minLength - text.length()) / (padding.length()))));
				paddedString.append(text);
				break;

			case PADDING_END:
				paddedString.append(text);
				paddedString.append(repeat(padding, (int) Math.ceil((double) (minLength - text.length()) / (padding.length()))));
				break;

			case PADDING_BOTH:
				int paddingLength = (int) Math.ceil((double) (minLength - text.length()) / (padding.length() * 2.));

				paddedString.append(repeat(padding, paddingLength));
				paddedString.append(text);
				paddedString.append(repeat(padding, paddingLength));
				break;

			default:
				paddedString.append(text);
				break;
			}
			return paddedString.toString();
		}
	}

	/**
	 * helper method to split the given string into a map. <br>
	 * example (using | as elementDelimiter and : as keyValueDelimiter)�: <br>
	 * the string "firstname:Norbert|lastname:Pomaroli" would be decoded to the
	 * map:
	 * <ul>
	 * <li>firstname -&gt; Norbert</li>
	 * <li>lastname -&gt; Pomaroli</li>
	 * </ul>
	 * limitation: none of the delimiter strings may occur in the keys or values
	 * @param encodedMap string containing the encoded map
	 * @param elementDelimiter regex for the delimiter used between elements
	 * @param keyValueDelimiter regex for the delimiter used in each element
	 *        between key and value
	 * @return the map
	 */
	public static Map splitIntoMap(String encodedMap, String elementDelimiter,
			String keyValueDelimiter) {
		Map decodedMap = new LinkedHashMap();

		if (encodedMap == null || encodedMap.length() == 0) {
			return decodedMap;
		}
		// split the encodedMap into the elements
		String[] elements = encodedMap.split(elementDelimiter);

		for (int i = 0; i < elements.length; i++) {
			// split each element into key and value
			String[] keyValuePair = elements[i].split(keyValueDelimiter, 2);

			if (keyValuePair.length == 2) {
				// only add the value when two parts were found
				decodedMap.put(keyValuePair[0], keyValuePair[1]);
			} else {// TODO: return null since syntax of encodedMap was incorrect?
			}
		}

		return decodedMap;
	}

	/**
	 * helper method to split the given string into a map. <br>
	 * the map may be encoded in multiple "layers" (meaning that the values of
	 * the "outermost" map may also be maps themselves) <br>
	 * example:
	 * splitIntoMap("norbert=firstname:Norbert|lastname:Pomaroli^laurin=firstname:Laurin|lastname:Herlt",
	 * new String[][] {new String[] {"\\^", "="}, new String[] {"\\|", ":"}});
	 * <br>
	 * (using delimiters ^, = and |, :): <br>
	 * the string
	 * "norbert=firstname:Norbert|lastname:Pomaroli^laurin=firstname:Laurin|lastname:Herlt"
	 * would be decoded to: <br>
	 * norbert -&gt;
	 * <ul>
	 * <li>firstname -&gt; Norbert</li>
	 * <li>lastname -&gt; Pomaroli</li>
	 * </ul>
	 * laurin -&gt;
	 * <ul>
	 * <li>firstname -&gt; Laurin</li>
	 * <li>lastname -&gt; Herlt</li>
	 * </ul>
	 * @param encodedMap string containing the encoded map
	 * @param delimiters array of stringarrays holding the delimiters, each
	 *        stringarray must have exactly 2 strings, first the element
	 *        delimiter and then the key-value delimiter
	 * @param level level of recursion step
	 * @return decoded map
	 */
	private static Map splitIntoMap(String encodedMap, String[][] delimiters, int level) {
		if (delimiters.length <= level) {
			return null;
		}

		// do the next recursion step
		Map partialMap = splitIntoMap(encodedMap, delimiters[level][0], delimiters[level][1]);

		if (delimiters.length > level) {
			// further steps required, so proceed with the recursion
			for (Iterator iter = partialMap.keySet().iterator(); iter.hasNext();) {
				String key = (String) iter.next();
				Map tempMap = splitIntoMap(partialMap.get(key).toString(), delimiters, level + 1);

				if (tempMap != null) {
					partialMap.put(key, tempMap);
				}
			}
		}

		return partialMap;
	}

	/**
	 * helper method to split the given string into a map. <br>
	 * the map may be encoded in multiple "layers" (meaning that the values of
	 * the "outermost" map may also be maps themselves) <br>
	 * example:
	 * splitIntoMap("norbert=firstname:Norbert|lastname:Pomaroli^laurin=firstname:Laurin|lastname:Herlt",
	 * new String[][] {new String[] {"\\^", "="}, new String[] {"\\|", ":"}});
	 * <br>
	 * (using delimiters ^, = and |, :): <br>
	 * the string
	 * "norbert=firstname:Norbert|lastname:Pomaroli^laurin=firstname:Laurin|lastname:Herlt"
	 * would be decoded to: <br>
	 * norbert -&gt;
	 * <ul>
	 * <li>firstname -&gt; Norbert</li>
	 * <li>lastname -&gt; Pomaroli</li>
	 * </ul>
	 * laurin -&gt;
	 * <ul>
	 * <li>firstname -&gt; Laurin</li>
	 * <li>lastname -&gt; Herlt</li>
	 * </ul>
	 * @param encodedMap string containing the encoded map
	 * @param delimiters array of stringarrays holding the delimiters, each
	 *        stringarray must have exactly 2 strings, first the element
	 *        delimiter and then the key-value delimiter
	 * @return decoded map
	 */
	public static Map splitIntoMap(String encodedMap, String[][] delimiters) {
		return splitIntoMap(encodedMap, delimiters, 0);
	}

	/**
	 * Resolve system properties encoded in the string as ${property.name}
	 * @param string string holding encoded system properties
	 * @return string with the system properties resolved
	 */
	public static String resolveSystemProperties(String string) {
		// avoid NPE here
		if (string == null) {
			return null;
		}
		// create a matcher
		Matcher m = findSystemProperties.matcher(string);
		StringBuffer output = new StringBuffer();
		int startIndex = 0;

		while (m.find()) {
			// copy static string between the last found system property and this one
			if (m.start() > startIndex) {
				output.append(string.substring(startIndex, m.start()));
			}
			output.append(System.getProperty(m.group(1), ""));
			startIndex = m.end();
		}
		// if some trailing static string exists, copy it
		if (startIndex < string.length()) {
			output.append(string.substring(startIndex));
		}

		return output.toString();
	}

	/**
	 * Resolve map data encoded in the string as ${key} from the given Map
	 * @param string string holding encoded map data
	 * @param data data map
	 * @return string with map data resolved
	 */
	public static String resolveMapData(String string, Map<String, String> data) {
		// avoid NPE here
		if (string == null) {
			return null;
		}
		if (data == null) {
			data = Collections.emptyMap();
		}
		// create a matcher
		Matcher m = findSystemProperties.matcher(string);
		StringBuffer output = new StringBuffer();
		int startIndex = 0;

		while (m.find()) {
			// copy static string between the last found system property and this one
			if (m.start() > startIndex) {
				output.append(string.substring(startIndex, m.start()));
			}
			output.append(ObjectTransformer.getString(data.get(m.group(1)), ""));
			startIndex = m.end();
		}
		// if some trailing static string exists, copy it
		if (startIndex < string.length()) {
			output.append(string.substring(startIndex));
		}

		return output.toString();
	}

	/**
	 * encodes the given string into md5 string in hex style.
	 * @param text the string to encode, null is interpreted as empty string
	 * @return the md5 representation in hex style: [0-9A-F]{32}, or empty
	 *         string on internal error.
	 */
	public static String md5(String text) {
		MessageDigest md;

		if (text == null) {
			text = "";
		}
		try {
			md = MessageDigest.getInstance("MD5");
			md.update(text.getBytes());
			byte[] digest = md.digest();

			return ObjectTransformer.encodeBinary(digest);
		} catch (NoSuchAlgorithmException e) {
			NodeLogger.getLogger(StringUtils.class).warn("failed to m5 string", e);
			return "";
		}
	}
    
	/**
	 * repeat a given string n times
	 * @param str string to repeat
	 * @param repeat how many times the given string should be repeated
	 * @return fully concatenated string
	 */
	public static String repeat(String str, int repeat) {
		return repeat(str, repeat, null);
	}

	/**
	 * repeat a given string n times
	 * @param str string to repeat
	 * @param repeat how many times the given string should be repeated
	 * @param glue string to be inserted between the parts (may be null for no glue)
	 * @return fully concatenated string
	 */
	public static String repeat(String str, int repeat, String glue) {
		if (repeat <= 0 || str.equals("")) {
			return "";
		}
		StringBuffer out = new StringBuffer();

		for (int i = 0; i < repeat; i++) {
			if (i > 0 && glue != null) {
				out.append(glue);
			}
			out.append(str);
		}
		return out.toString();
	}

	/**
	 * Merge the given String array into a single string.
	 * @param parts array of string parts
	 * @return combined string
	 */
	public static String merge(Object[] parts) {
		return merge(parts, null, null, null);
	}

	/**
	 * Merge the given String array into a single string.<br>
	 * Example:<br>
	 * parts: [1,2,3]<br>
	 * glue: ;<br>
	 * return: 1;2;3<br>
	 * @param parts array of string parts
	 * @param glue filled in between merged parts (may be null for no glue)
	 * @return combined string
	 */
	public static String merge(Object[] parts, String glue) {
		return merge(parts, glue, null, null);
	}

	/**
	 * Merge the given String array into a single string.<br>
	 * Example: <br>
	 * parts: [1,2,3]<br>
	 * glue: ;<br>
	 * prefix: (<br>
	 * postfix: )<br>
	 * return: (1);(2);(3)<br>
	 * @param parts array of string parts
	 * @param glue filled in between merged parts (may be null for no glue)
	 * @param prefix prefix to be prepended to every part (may be null for no prefix)
	 * @param postfix postfix to be appende to every part (may be null for no postfix)
	 * @return combined string
	 */
	public static String merge(Object[] parts, String glue, String prefix, String postfix) {
		StringBuffer combined = new StringBuffer();

		boolean first = true;

		for (int i = 0; i < parts.length; i++) {
			if (parts[i] == null) {
				continue;
			}
			if (first) {
				first = false;
			} else if (glue != null) {
				combined.append(glue);
			}
			if (prefix != null) {
				combined.append(prefix);
			}
			combined.append(parts[i]);
			if (postfix != null) {
				combined.append(postfix);
			}
		}

		return combined.toString();
	}
    
	/**
	 * Convert the given likeString (right part of a "LIKE" condition) into a
	 * regex that can be used with {@link String#matches(java.lang.String)} to evaluate the LIKE condition.
	 * @param likeString comparision value. may contain % as wildcard for multiple characters and _ for one character
	 * @return regex for evaluating the LIKE condition
	 */
	public static String likeStringToRegex(String likeString) {
		if (likeString == null) {
			return "";
		}
		// escape all regex symbols with \ and replace % with .*
		return likeString.replaceAll("((\\\\)|(\\.)|(\\[)|(\\])|(\\()|(\\))|(\\+)|(\\*))", "\\\\$1").replaceAll("\\%", ".*").replaceAll("_", ".");
	}

	/**
	 * Collapse the given String (in the sense of XML collapse). Collapsing the string is done in the following steps:
	 * <ul>
	 *   <li>Replace all carriage return (#xD), line feed (#xA) or tab (#x9) with spaces (#x20)</li>
	 *   <li>Remove all leading or trailing spaces</li>
	 *   <li>Replace sequences of more than one space by a single space</li>
	 * </ul>
	 * The resulting strings meet the requirements of XML tokens.
	 * @param str string to collapse
	 * @return collapsed string
	 */
	public static String collapseString(String str) {
		if (str == null) {
			return str;
		} else {
			return str.replaceAll("\\s", " ").replaceAll("\\s+", " ").trim();
		}
	}

	/**
	 * Check whether the string arrays are equal or not
	 * @param firstArray first string array
	 * @param secondArray second string array
	 * @return true when the arrays are equal, false if not
	 */
	public static boolean isEqual(String[] firstArray, String[] secondArray) {
		boolean equal = true;

		if (firstArray == null) {
			if (secondArray != null) {
				equal = false;
			}
		} else {
			if (secondArray == null) {
				equal = false;
			} else if (firstArray.length != secondArray.length) {
				equal = false;
			} else {
				for (int i = 0; i < firstArray.length && equal; ++i) {
					equal &= isEqual(firstArray[i], secondArray[i]);
				}
			}
		}

		return equal;
	}

	/**
	 * Check whether two strings are equal (including checks for null strings)
	 * @param firstString first string
	 * @param secondString second string
	 * @return true when the strings are equal, false if not
	 */
	public static boolean isEqual(String firstString, String secondString) {
		return firstString == null ? secondString == null : firstString.equals(secondString);
	}

	/**
	 * Returns a literal replacement <code>String</code> for the specified
	 * <code>String</code>.
	 *
	 * This method produces a <code>String</code> that will work
	 * use as a literal replacement <code>s</code> in the
	 * <code>appendReplacement</code> method of the {@link Matcher} class.
	 * The <code>String</code> produced will match the sequence of characters
	 * in <code>s</code> treated as a literal sequence. Slashes ('\') and
	 * dollar signs ('$') will be given no special meaning.
	 *
	 * @param  s The string to be literalized
	 * @return  A literal string replacement
	 */
	public static String quoteReplacement(String s) {
		if ((s.indexOf('\\') == -1) && (s.indexOf('$') == -1)) {
			return s;
		}
		StringBuffer sb = new StringBuffer();

		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);

			if (c == '\\') {
				sb.append('\\');
				sb.append('\\');
			} else if (c == '$') {
				sb.append('\\');
				sb.append('$');
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	/**
	 * Unescape the given escaped string
	 * @param escapedString string holding escaped character sequences
	 * @return unescaped string
	 */
	public static String unEscape(String escapedString) {
		if (escapedString == null) {
			return escapedString;
		}
		int stringLength = escapedString.length();
		StringBuffer unescaped = new StringBuffer(stringLength);

		for (int i = 0; i < stringLength; ++i) {
			boolean consumed = false;

			if (i + 1 < stringLength && escapedString.charAt(i) == '\\') {
				switch (escapedString.charAt(i + 1)) {
				case 'n':
					unescaped.append("\n");
					consumed = true;
					i++;
					break;

				case 't':
					unescaped.append("\t");
					consumed = true;
					i++;
					break;

				case 'b':
					unescaped.append("\b");
					consumed = true;
					i++;
					break;

				case 'r':
					unescaped.append("\r");
					consumed = true;
					i++;
					break;

				case 'f':
					unescaped.append("\f");
					consumed = true;
					i++;
					break;

				case '\\':
					unescaped.append("\\");
					consumed = true;
					i++;
					break;

				case '\'':
					unescaped.append("\'");
					consumed = true;
					i++;
					break;

				case '\"':
					unescaped.append("\"");
					consumed = true;
					i++;
					break;

				default:
					break;
				}
			}

			if (!consumed) {
				unescaped.append(escapedString.charAt(i));
			}
		}
		return unescaped.toString();
	}

	/**
	 * Compute the word based diff between the given strings. Return a list of
	 * parts, either instances of {@link String} (for unmodified text parts) or
	 * instances of {@link Difference}.
	 * @param s1 first string
	 * @param s2 second string
	 * @param showTags true when html tags shall be converted to [tag] before
	 *        diffing
	 * @param ignoreRegex regex for text parts that shall be ignored when
	 *        diffing. may be null
	 * @return diff as list of Strings and Differences
	 */
	public static List diffHTMLStrings(String s1, String s2, boolean showTags, String ignoreRegex) {
		// break the contents into words
		List content1 = html2list(s1, showTags);
		List content2 = html2list(s2, showTags);

		List compareContent1 = null;
		List compareContent2 = null;

		// check the ignore regex
		if (isEmpty(ignoreRegex)) {
			// no ignore regex, so compare the original lists
			compareContent1 = content1;
			compareContent2 = content2;
		} else {
			// remove all parts matching the ignore regex in the text parts before comparing
			compareContent1 = new Vector(content1.size());
			for (Iterator iter = content1.iterator(); iter.hasNext();) {
				String element = (String) iter.next();

				compareContent1.add(element.replaceAll(ignoreRegex, ""));
			}
			compareContent2 = new Vector(content2.size());
			for (Iterator iter = content2.iterator(); iter.hasNext();) {
				String element = (String) iter.next();

				compareContent2.add(element.replaceAll(ignoreRegex, ""));
			}
		}

		// compare the word lists
		List diffs = (new Diff(compareContent1, compareContent2)).diff();
		List out = new Vector();

		Iterator it = diffs.iterator();
		int lastEnd = 0;

		while (it.hasNext()) {
			Difference diff = (Difference) it.next();
			int delStart = diff.getDeletedStart();
			int delEnd = diff.getDeletedEnd();
			int addStart = diff.getAddedStart();
			int addEnd = diff.getAddedEnd();
			int type = delEnd != Difference.NONE && addEnd != Difference.NONE
					? DiffPart.TYPE_CHANGE
					: (delEnd == Difference.NONE ? DiffPart.TYPE_INSERT : DiffPart.TYPE_REMOVE);

			if (delStart != Difference.NONE && delStart > lastEnd) {
				for (int i = lastEnd; i < delStart; ++i) {
					out.add(content1.get(i));
				}
			}
			switch (type) {
			case DiffPart.TYPE_CHANGE:
				out.add(new DiffPart(append(delStart, delEnd, content1), append(addStart, addEnd, content2), DiffPart.TYPE_CHANGE));
				lastEnd = delEnd + 1;
				break;

			case DiffPart.TYPE_REMOVE:
				out.add(new DiffPart(append(delStart, delEnd, content1), null, DiffPart.TYPE_REMOVE));
				lastEnd = delEnd + 1;
				break;

			case DiffPart.TYPE_INSERT:
				out.add(new DiffPart(null, append(addStart, addEnd, content2), DiffPart.TYPE_INSERT));
				lastEnd = delStart;
				break;

			default:
				break;
			}
		}

		if (lastEnd < content1.size()) {
			for (int i = lastEnd; i < content1.size(); ++i) {
				out.add(content1.get(i));
			}
		}

		return out;
	}

	/**
	 * Append a list of elements from a list into a string
	 * @param start first object
	 * @param end last object
	 * @param lines list of lines
	 * @return merged string
	 */
	protected static String append(int start, int end, List lines) {
		StringBuffer s = new StringBuffer();

		for (int lnum = start; lnum <= end; ++lnum) {
			s.append(lines.get(lnum));
		}

		return s.toString();
	}

	/**
	 * Break the given string into a list of words
	 * @param x string to break
	 * @param b true when html tags shall be replaced by [tag]
	 * @return list of words
	 */
	protected static List html2list(String x, boolean b) {
		int mode = MODE_CHAR;
		StringBuffer cur = new StringBuffer();
		List out = new Vector();

		char[] c = x.toCharArray();

		for (int i = 0; i < c.length; i++) {
			switch (mode) {
			case MODE_TAG:
				if (c[i] == '>') {
					if (b) {
						cur.append("&gt;");
					} else {
						cur.append(c[i]);
					}
					out.add(cur.toString());
					cur.delete(0, cur.length());
					mode = MODE_CHAR;
				} else if (c[i] == '<' && b) {
					cur.append("&lt;");
				} else {
					cur.append(c[i]);
				}
				break;

			case MODE_CHAR:
				if (c[i] == '<') {
					if (cur.length() > 0) {
						out.add(cur.toString());
						cur.delete(0, cur.length());
					}
					if (b) {
						cur.append("&lt;");
					} else {
						cur.append(c[i]);
					}
					mode = MODE_TAG;
				} else if (Character.isWhitespace(c[i])) {
					if (cur.length() > 0) {
						out.add(cur.toString());
						cur.delete(0, cur.length());
					}
					cur.append(c[i]);
					mode = MODE_WHITESPACE;
				} else {
					cur.append(c[i]);
				}
				break;

			case MODE_WHITESPACE:
				if (c[i] == '<') {
					if (cur.length() > 0) {
						out.add(cur.toString());
						cur.delete(0, cur.length());
					}
					if (b) {
						cur.append("&lt;");
					} else {
						cur.append(c[i]);
					}
					mode = MODE_TAG;
				} else if (Character.isWhitespace(c[i])) {
					if (c[i] == '\n') {
						// newline, so break the word
						if (cur.length() > 0) {
							out.add(cur.toString());
							cur.delete(0, cur.length());
						}
					}
					cur.append(c[i]);
				} else {
					if (cur.length() > 0) {
						out.add(cur.toString());
						cur.delete(0, cur.length());
					}
					cur.append(c[i]);
					mode = MODE_CHAR;
				}

			default:
				break;
			}
		}
		out.add(cur.toString());

		return out;
	}

	/**
	 * Determine the position in the given input string in means of line and col
	 * @param input input string
	 * @param index position in the string (index)
	 * @return array containing [line, col] or null when input is null or index
	 *         is out of bounds
	 */
	public static int[] findPosition(String input, int index) {
		if (input == null || index < 0 || index > input.length()) {
			return null;
		}

		int line = 0;
		int col = 0;
		// first count # of newlines in input before the given index
		int lastNLFound = index;

		while (lastNLFound >= 0) {
			lastNLFound = input.lastIndexOf('\n', lastNLFound - 1);
			if (lastNLFound >= 0) {
				line++;
			}
		}

		// now determine the col
		lastNLFound = input.lastIndexOf('\n', index - 1);
		if (lastNLFound >= 0) {
			col = Math.max(index - lastNLFound - 1, 0);
		} else {
			col = index;
		}

		return new int[] { line, col};
	}

	/**
	 * Convert the given string, such that the first letter is uppercase and all
	 * other letters are lowercase
	 * @param string string to convert
	 * @return converted string
	 */
	public static String upperCaseFirstLetter(String string) {
		if (isEmpty(string)) {
			return string;
		}

		StringBuffer newString = new StringBuffer(string.length());

		newString.append(string.substring(0, 1).toUpperCase());
		if (string.length() > 1) {
			newString.append(string.substring(1).toLowerCase());
		}

		return newString.toString();
	}

	/**
	 * Class for modified text parts
	 */
	public static class DiffPart {

		public final static int TYPE_CHANGE = 0;

		public final static int TYPE_INSERT = 1;

		public final static int TYPE_REMOVE = 2;

		/**
		 * original text part
		 */
		protected String original;

		/**
		 * modified text part
		 */
		protected String modified;

		/**
		 * type of the diff
		 */
		protected int diffType = TYPE_CHANGE;

		/**
		 * Create an instance of the DiffPart
		 * @param original original text part
		 * @param modified modified text part
		 * @param diffType type of the diff
		 */
		public DiffPart(String original, String modified, int diffType) {
			this.original = original;
			this.modified = modified;
			this.diffType = diffType;
		}

		/**
		 * Get the original text part
		 * @return original text part
		 */
		public String getOriginal() {
			return original;
		}

		/**
		 * Get the modified text part
		 * @return modified text part
		 */
		public String getModified() {
			return modified;
		}

		/**
		 * Get the type of the diff
		 * @return type of the diff
		 */
		public int getDiffType() {
			return diffType;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			StringBuffer out = new StringBuffer();

			switch (diffType) {
			case TYPE_CHANGE:
				out.append("[change]\"");
				out.append(original);
				out.append("\"=>\"");
				out.append(modified);
				out.append("\"");
				break;

			case TYPE_INSERT:
				out.append("[add]\"");
				out.append(modified);
				out.append("\"");
				break;

			case TYPE_REMOVE:
				out.append("[del]\"");
				out.append(original);
				out.append("\"");
				break;
			}
			return out.toString();
		}
	}

	/**
	 * Escape the given string for use in XML:
	 * (‘<’ gets converted to ‘&amp;lt;’, ‘>’ gets converted to ‘&amp;gt;’
	 * ‘&’ gets converted to ‘&amp;amp;’, ‘‘’ gets converted to ‘&amp;#039;’, ‘”’ gets converted
	 * to ‘&amp;#034;’
	 * @param string strint to escape
	 * @return escaped string
	 */
	public static String escapeXML(String string) {
		return string.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("'", "&#039;").replaceAll("\"", "&#034;");
	}

	/**
	 * Create a Pattern Matcher that will find the html head tag (including ones with an xhtml namespace)
	 * @param html HTML string which should be searched for the head tag
	 * @return pattern matcher
	 */
	public static Matcher getHeadMatcher(String html) {
		Pattern headPattern = Pattern.compile("(<(\\w+:)?head\\s*(\\s[^>]*)?>)");

		return headPattern.matcher(html);
	}
    
	/**
	 * Parses an HTML String and inserts a given head String to the begin in the &lt;head&gt; section.<br>
	 * 
	 * @param html HTML in which the head String should be added
	 * @param head HTML String which should be added to the head section
	 * @return Final HTML 
	 */
	public static String insertHtmlIntoHead(String html, String head) {
		Matcher headMatcher = getHeadMatcher(html);

		if (!headMatcher.find()) {
			// we found no head, so we need to add one
			Pattern bodyPattern = Pattern.compile("(<body\\s*(\\s[^>]*)?>)");
			Matcher bodyMatcher = bodyPattern.matcher(html);

			if (!bodyMatcher.find()) {
				// we also found no body, so add head and body
				StringBuffer newOutput = new StringBuffer();

				newOutput.append("<head>\n");
				newOutput.append(head);
				newOutput.append("\n</head>\n<body>\n");
				newOutput.append(html);
				newOutput.append("</body>\n");
				return newOutput.toString();
			} else {
				// we found a body, so just prepend a head
				StringBuffer newOutput = new StringBuffer();

				newOutput.append("<head>\n");
				newOutput.append(head);
				newOutput.append("</head>\n");
				newOutput.append(html);
				return newOutput.toString();
			}
		} else {
			// we found a head, so just insert the head markup there
			return headMatcher.replaceFirst("$1\n" + StringUtils.quoteReplacement(head));
		}
	}
    
	/**
	 * Counts the number of occurrences of the given substring in string.
	 * 
	 * @param string String in which for the occurences is searched
	 * @param substring Substring that should be 
	 * @return Number of occurrences of substring in haystack
	 * @throws NullPointerException if haystack is null
	 */
	public static int countOccurrences(String string, String substring) {
		int count = 0;
		int index = 0;

		while ((index = string.indexOf(substring, index)) != -1) {
			count++;
			index += string.length();
		}
		return count;
	}

	/**
	 * Reads an inputstream and transforms all content into one string
	 * @param in
	 * @return
	 * @throws IOException
	 */
	public static String readStream(InputStream in) throws IOException {
		return readStream(in, "UTF-8");
	}

	/**
	 * Reads an inputstream and transforms all content into one string
	 * @param in
	 * @param encoding encoding
	 * @return
	 * @throws IOException
	 */
	public static String readStream(InputStream in, String encoding) throws IOException {
		StringBuffer out = new StringBuffer();
		InputStreamReader reader = new InputStreamReader(in, encoding);

		char[] b = new char[4096];
		int read = 0;

		while ((read = reader.read(b)) > 0) {
			out.append(b, 0, read);
		}

		return out.toString();
	}

	/**
	 * This method will add a doctype to the given html content and return the
	 * modified content. Adding will be omitted if a doctype has already been
	 * set.
	 * 
	 * @param html
	 * @return
	 */
	public static String insertHtml5DocType(String html) {
		String doctypeTag = "<!DOCTYPE html>";
		StringBuffer newOutput = new StringBuffer();

		Pattern doctypePattern = Pattern.compile("(<!DOCTYPE.*>)");
		Matcher docTypeMatcher = doctypePattern.matcher(html);

		if (!docTypeMatcher.find()) {

			// we found no doctype, so we need to add one
			newOutput.append(doctypeTag);
			
		}
		newOutput.append(html);
		return newOutput.toString();
	}

	/**
	 * This method tries to emulate the mysql comparison with collation utf8_general_ci
	 * @param first first string
	 * @param second second string
	 * @return -1 if first string is less, 1 if second string is less and 0 if they are equal
	 */
	public static int mysqlLikeCompare(String first, String second) {

		// Compare and forward return value
		return correctedCollator.compare(Optional.ofNullable(first).map(String::toUpperCase).orElse(""),
				Optional.ofNullable(second).map(String::toUpperCase).orElse(""));
	}

	/**
	 * replaces all tags in a string
	 * @param string to be stripped of tags
	 * @return tag-free string
	 */
	public static String stripTags(String str) {
		return str.replaceAll("<[^>]*>", "");
	}

	/**
	 * Given a string, it checks whether it contains a valid
	 * decimal integer number or not.
	 * 
	 * @param str  The String to check
	 * @return     true or false
	 */
	public static boolean isInteger(String str) {
	    return isInteger(str, 10);
	}

	/**
	 * Given a string and a radix base, it checks whether the
	 * string contains an integer number or not.
	 *
	 * @param str    The String to check
	 * @param radix  The radix defines the numeral system to use (decimal, octal...)
	 * @return       true or false
	 */
	public static boolean isInteger(String str, int radix) {
		if (str == null) {
			return false;
		}

		if (str.equals("")) {
			return false;
		}

		for (int i = 0; i < str.length(); i++) {
			if (i == 0 && str.charAt(i) == '-') {
				if (str.length() == 1) {
					return false;
				} else {
					continue;
				}
			}

			if (Character.digit(str.charAt(i),radix) < 0) {
				return false;
			}
		}

		return true;
	}

	/**
	 * small helper method to append an attribute if the value is not empty
	 * @param buffer buffer where to append the attribute
	 * @param name name of the attribute
	 * @param value value of the attribute
	 * @return the buffer
	 */
	public static StringBuffer appendAttribute(StringBuffer buffer, String name, String value) {
		if (!StringUtils.isEmpty(value)) {
			buffer.append(" ").append(name).append("=\"").append(StringUtils.escapeXML(value)).append("\"");
		}

		return buffer;
	}
}
