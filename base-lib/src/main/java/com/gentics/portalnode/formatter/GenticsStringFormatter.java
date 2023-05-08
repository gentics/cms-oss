package com.gentics.portalnode.formatter;

import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.PatternSyntaxException;

import com.gentics.api.portalnode.imp.AbstractGenticsImp;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.log.NodeLogger;

import fi.iki.santtu.md5.MD5;

/**
 * Created by IntelliJ IDEA. User: marius.toader Date: Feb 18, 2005 Time:
 * 12:23:37 PM To change this template use Options | File Templates.
 */
public class GenticsStringFormatter extends AbstractGenticsImp {
	// order is important, do not change to other type of map
	private final static Map htmlEscapes = new LinkedHashMap();

	// order is important, do not change to other type of map
	private final static Map javaScriptEscapes = new LinkedHashMap();

	/**
	 * constant for an empty string
	 */
	private final static String EMPTY_STRING = "";

	static {
		// this must allways be the the first one to be escaped
		htmlEscapes.put("&", "&amp;");
		htmlEscapes.put("<", "&lt;");
		htmlEscapes.put(">", "&gt;");
		// htmlEscapes.put(" ", "&nbsp;");
		htmlEscapes.put("\u2122", "&#153;");
		htmlEscapes.put("\u00ae", "&reg;");
		htmlEscapes.put("\u00a9", "&copy;");
		htmlEscapes.put("\"", "&quot;");
		htmlEscapes.put("\'", "&#39;");

		javaScriptEscapes.put("\\\\", "\\\\\\\\");
		javaScriptEscapes.put("\"", "\\\\\"");
		javaScriptEscapes.put("\'", "\\\\'");
	}

	/**
	 * the default constructor
	 */
	public GenticsStringFormatter() {}

	/**
	 * Escapes the most common javascript escape characters. Please see the
	 * source code for the exact escape sequences.
	 * @param originalString The String that contains the unescaped string.
	 * @return A String that contains the string with the escape caracters.
	 */
	public String escapeJS(Object originalString) {
		if (originalString != null) {
			return applyRegex(originalString.toString(), javaScriptEscapes);
		} else {
			return EMPTY_STRING;
		}
	}

	/**
	 * Escapes the most common javascript escape caracters. Please see the
	 * source code for the exact escape sequences.
	 * @param originalString The String that contains the unescaped string.
	 * @return A String that contains the string with the escape caracters.
	 */
	public String escapeHTML(Object originalString) {
		if (originalString != null) {
			return applyRegex(originalString.toString(), htmlEscapes);
		} else {
			return EMPTY_STRING;
		}
	}

	/**
	 * Removes the embeded HTML tags from user input string to prevent potential
	 * problems (in fact it removes anything with ).
	 * @param originalString The original string that may contain HTML element
	 * @return A String that does not contain HTML elements ()
	 */
	public String stripML(Object originalString) {
		if (originalString == null) {
			return EMPTY_STRING;
		}
		String parsedString = null;

		parsedString = originalString.toString().replaceAll("<[^<]*?>", "");
		return parsedString;
	}

	/**
	 * Trims the string to the speciafied length if it's longer than that.
	 * @param originalString The string that must be trimmed.
	 * @param maxSize The maximum size in characters of the string.
	 * @return The trimmed string that now has max "maxSize" characters.
	 */
	public String trim(Object originalString, int maxSize) {
		if (originalString == null) {
			return EMPTY_STRING;
		}
		if (maxSize >= originalString.toString().length()) {
			return originalString.toString();
		}
		String parsedString = null;

		parsedString = originalString.toString().substring(0, maxSize);
		return parsedString;
	}

	/**
	 * Replaces certain portions of the string (based on a regular expression)
	 * with another string.
	 * @param originalString The string that will be parsed for matching
	 *        substrings.
	 * @param regex The regular expression (as defined in jdk) that will match
	 *        in the originalstring.
	 * @param replacement The string that will replace the parts of the original
	 *        string that match the regex.
	 * @return The original string where occurences of the regex are replaced by
	 *         the replacement string.
	 */
	public String regexp(Object originalString, Object regex, Object replacement) {
		if (originalString == null || regex == null || replacement == null) {
			if (originalString != null && originalString instanceof String) {
				return (String) originalString;
			}
			return EMPTY_STRING;
		}
		String parsedString = null;

		try {
			parsedString = originalString.toString().replaceAll(regex.toString(), replacement.toString());
		} catch (PatternSyntaxException e) {
			NodeLogger.getNodeLogger(getClass()).error("Error while using regexp", e);
		}
		return parsedString;
	}

	/**
	 * tests a given regex and returns a result string. error messages are not
	 * localized, because its not required, but improves performance.
	 * @param text the text to use. null is interpreted as empty string.
	 * @param regex the regex to test, null regexes will be invalid and generate
	 *        a custom error message.
	 * @return 1 for match, 0 for no match, or any other text for syntax error.
	 */
	public String testRegex(Object text, Object regex) {
		if (regex == null) {
			return "error, regex was null";
		}
		if (text == null) {
			text = EMPTY_STRING;
		}
		try {
			if (text.toString().matches(regex.toString())) {
				return "1";
			} else {
				return "0";
			}
		} catch (PatternSyntaxException e) {
			return "error, " + e.getMessage();
		}
	}

	// iterates over a Map and replaces occurences of the "keys" (regexs) with
	// the "values"
	// of the keys
	private String applyRegex(String originalString, Map regs) {
		if (originalString == null) {
			return "";
		}
		String parsedString = null;
		Iterator toEscape = regs.keySet().iterator();

		parsedString = originalString;
		try {
			while (toEscape.hasNext()) {
				String escape = (String) toEscape.next();

				parsedString = parsedString.replaceAll(escape, (String) regs.get(escape));
			}
		} catch (PatternSyntaxException e) {
			NodeLogger.getNodeLogger(getClass()).warn("Error while using regexp");
		}
		return parsedString;
	}

	/**
	 * convert the given string to all uppercase
	 * @param originalString original string
	 * @return original string converted to uppercase letters
	 */
	public String toUpper(Object originalString) {
		if (originalString == null) {
			return EMPTY_STRING;
		} else {
			return originalString.toString().toUpperCase();
		}
	}

	/**
	 * Trim all words in the string that exceed the given length
	 * @param originalString original string
	 * @param maxLength maximum length of words
	 * @param ellipsis to use in trimmed words
	 * @param template template to use as replacement for the long words. In the
	 *        template, $word and $trimmedword can be used for the word and
	 *        trimmed word. The template may be null or empty for just trimming
	 *        the words
	 * @return string with all words longer than maxLength trimmed
	 */
	public String trimWords(Object originalString, int maxLength, Object ellipsis,
			Object template) {
		if (originalString == null || ellipsis == null) {
			return EMPTY_STRING;
		}
		StringBuffer trimmedString = new StringBuffer(originalString.toString().length());
		boolean useTemplate = (template != null && template.toString().length() > 0);

		// split the string into words (by words boundaries, without losing
		// spaces between the words)
		String[] words = originalString.toString().split("\\b");
		int trimLength = ellipsis.toString().length() + 1;

		if (trimLength > maxLength - 1) {
			// the maxlength is too small, make it bigger
			maxLength = trimLength + 1;
		}
		for (int i = 0; i < words.length; ++i) {
			if (words[i].trim().length() > maxLength) {
				// we have to trim the word
				if (useTemplate) {
					// create the trimmed word
					StringBuffer trimmedWord = new StringBuffer(maxLength);

					trimmedWord.append(words[i].substring(0, Math.max(maxLength - trimLength, 1)));
					trimmedWord.append(ellipsis);
					trimmedWord.append(words[i].substring(words[i].length() - 1, words[i].length()));
					trimmedString.append(template.toString().replaceAll("\\$word\\b", words[i]).replaceAll("\\$trimmedword\\b", trimmedWord.toString()));
				} else {
					// just append the trimmed word
					trimmedString.append(words[i].substring(0, Math.max(maxLength - trimLength, 1)));
					trimmedString.append(ellipsis);
					trimmedString.append(words[i].substring(words[i].length() - 1, words[i].length()));
				}
			} else {
				// word is ok
				trimmedString.append(words[i]);
			}
		}

		return trimmedString.toString();
	}

	/**
	 * Trim all words in the string that exceed the given length
	 * @param originalString original string
	 * @param maxLength maximum length of words
	 * @return string with all words longer than maxLength trimmed
	 */
	public String trimWords(Object originalString, int maxLength, Object ellipsis) {
		return trimWords(originalString, maxLength, ellipsis, null);
	}

	/**
	 * Trim all words in the string that exceed the given length
	 * @param originalString original string
	 * @param maxLength maximum length of words
	 * @return string with all words longer than maxLength trimmed
	 */
	public String trimWords(Object originalString, int maxLength) {
		return trimWords(originalString, maxLength, "...", null);
	}
    
	/**
	 * Translates a string into <code>x-www-form-urlencoded</code>
	 * format. This method uses the platform's default encoding
	 * as the encoding scheme to obtain the bytes for unsafe characters.
	 * 
	 * @param string the string to be translated
	 * @return the translated string.
	 */
	public String encodeURL(String string) {
		try {
			return StringUtils.encodeURL(string, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			NodeLogger.getNodeLogger(getClass()).error("Error while encoding URL", e);
			return string;
		}
	}

	/**
	 * Translates a string into <code>x-www-form-urlencoded</code>
	 * format, using the given encoding to obtain the bytes for unsafe characters.
	 * @param string the string to be translated
	 * @param encoding encoding
	 * @return the translated string
	 */
	public String encodeURL(String string, String encoding) {
		try {
			return StringUtils.encodeURL(string, encoding);
		} catch (UnsupportedEncodingException e) {
			NodeLogger.getNodeLogger(getClass()).error("Error while encoding URL", e);
			return string;
		}
	}

	/**
	 * @see StringUtils#merge(Object[], String)
	 */
	public String implode(Object[] parts, String glue) {
		return StringUtils.merge(parts, glue);
	}
    
	/**
	 * @see StringUtils#merge(Object[], String, String, String) 
	 */
	public String implode(Object[] parts, String glue, String prefix, String postfix) {
		return StringUtils.merge(parts, glue, prefix, postfix);
	}
    
	/**
	 * @see #implode(Object[], String) 
	 */
	public String implode(Collection parts, String glue) {
		return StringUtils.merge(parts.toArray(), glue);
	}
    
	/**
	 * @see #implode(Object[], String, String, String) 
	 */
	public String implode(Collection parts, String glue, String prefix, String postfix) {
		return StringUtils.merge(parts.toArray(), glue, prefix, postfix);
	}
    
	/**
	 * create md5 hash of the given string
	 * null strings are treated like empty strings ""
	 * @param string to be hashed
	 * @return md5 hash of string
	 */
	public String md5(String string) {
		if (string == null) {
			string = "";
		}
        
		MD5         md5 = new MD5();

		md5.Init();
		md5.Update(string);
		String hash = md5.asHex();

		return hash;
	}    
}
