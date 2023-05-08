/*
 * @author Stefan Hepp
 * @date 21.01.2006
 * @version $Id: AttributeResult.java,v 1.3 2006-02-02 23:35:13 stefan Exp $
 */
package com.gentics.contentnode.parser.attribute;

import java.util.Map;

/**
 * This is a container for the result values of a attribute parsing call.
 * This correlates to a parsed tag with its attributes and ending position.
 *
 * If you want to store more than one value for the same key, store something
 * like this: ('attrnum'=2,'attr1'=value1,'attr2'=value2). If you really need
 * this feature, using something like this is more xml-compliant.
 */
public class AttributeResult {

	private int endPos;
	private boolean closed;
	private Map attributes;

	/**
	 * Constructor to create a new attribute result container.
	 *
	 * @param endPos the position of the last character of the tag.
	 * @param isClosed true, if the tag is closed and has therefore no child elements.
	 * @param attributes a map of all attribute-names and their values of the tag as String->Object.
	 */
	public AttributeResult(int endPos, boolean isClosed, Map attributes) {
		this.endPos = endPos;
		closed = isClosed;
		this.attributes = attributes;
	}

	/**
	 * Check, if this tag has child-elements.
	 * @return true, if this tag has no childs, else false.
	 */
	public boolean isClosed() {
		return closed;
	}

	/**
	 * Get the position of the last character of the tag in the parsed code.
	 * @return the position of the last character of the tag.
	 */
	public int getEndPos() {
		return endPos;
	}

	/**
	 * Get the map of the tag's attribute names and their values.
	 * The value is usually String or Expression, but depends on the implementation.
	 * @return the parsed attributes with values as String->Object.
	 */
	public Map getAttributes() {
		return attributes;
	}
}
