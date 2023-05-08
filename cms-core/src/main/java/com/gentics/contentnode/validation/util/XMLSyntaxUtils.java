/*
 * @author tobiassteiner
 * @date Jan 27, 2011
 * @version $Id: XMLSyntaxUtils.java,v 1.1.2.1 2011-02-10 13:43:40 tobiassteiner Exp $
 */
package com.gentics.contentnode.validation.util;

/**
 * See http://www.w3.org/TR/REC-xml/ .
 */
public class XMLSyntaxUtils {

	/**
	 * A string describing characters that may validly start a XML name, in
	 * regex range syntax suitable for inclusion in a regex character class.
	 * TODO: Missing code points #x10000-#xEFFFF. How to match surrogate pairs?
	 */
	public static final String XML_NAME_START_CHARS = ":A-Z_a-z" + "\u00C0-\u00D6\u00D8-\u00F6\u00F8-\u02FF\u0370-\u037D\u037F-\u1FFF"
			+ "\u200C-\u200D\u2070-\u218F\u2C00-\u2FEF\u3001-\uD7FF\uF900-\uFDCF\uFDF0-\uFFFD";
    
	/**
	 * A string descring characters that may validly continue a XML name, in
	 * regex range syntax suitable for inclusion in a regex character class.
	 */
	public static final String XML_NAME_CHARS = XML_NAME_START_CHARS + "\\-\\.0-9\u00B7\u0300-\u036F\u203F-\u2040";
    
	/**
	 * A regex pattern that expresses a valid XML name.
	 */
	public static final String XML_NAME_PATTERN_STRING = "[" + XML_NAME_START_CHARS + "][" + XML_NAME_CHARS + "]*";
}
