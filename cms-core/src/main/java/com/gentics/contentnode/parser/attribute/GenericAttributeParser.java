/*
 * @author Stefan Hepp
 * @date 22.01.2006
 * @version $Id: GenericAttributeParser.java,v 1.9 2007-03-29 13:49:54 norbert Exp $
 */
package com.gentics.contentnode.parser.attribute;

import com.gentics.contentnode.parser.expression.parser.ExpressionParser;
import com.gentics.contentnode.parser.tag.struct.TagPart;
import com.gentics.contentnode.render.RenderResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A generic implementaion of an attributeparser. This implementation uses the
 * {@link AbstractAttributeParser} to parse the attributes. All found attributes are
 * added to the attribute map using their found key and value without any cleanups.
 * Expression parsing is not supported.
 */
public class GenericAttributeParser extends AbstractAttributeParser {

	/**
	 * Constructor to create a new generic AttributeParser.
	 */
	public GenericAttributeParser() {
		super(true, true, true);
	}

	/**
	 * Check for expressionparsing. This always returns null.
	 * @param currentKey
	 * @param isQuoted
	 * @param isFirstChar
	 * @return null, as expression parsing is not used.
	 */
	protected ExpressionParser getExpressionParser(String currentKey, boolean isQuoted, boolean isFirstChar) {
		return null;
	}

	/**
	 * normalize the found attributes. All attributes are stored into the map without fuzzing around.
	 * Duplicate keys are overwritten using the last occurrence.
	 *
	 * @param renderResult
	 * @param attribs
	 * @return a map with all attributes as String->Attribute.
	 */
	protected Map normalizeAttributes(RenderResult renderResult, List attribs) {
		Map attributes = new HashMap(attribs.size());

		for (int i = 0; i < attribs.size(); i++) {
			Attribute attribute = (Attribute) attribs.get(i);

			attributes.put(attribute.getKey(), attribute);
		}
		return attributes;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.parser.attribute.AttributeParser#isMatchingPair(com.gentics.lib.parser.tag.struct.TagPart, com.gentics.lib.parser.tag.struct.TagPart)
	 */
	public boolean isMatchingPair(TagPart begin, TagPart end) {
		return false;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.parser.attribute.AttributeParser#isSplitterTag(com.gentics.lib.parser.tag.struct.TagPart)
	 */
	public boolean isSplitterTag(TagPart tagPart) {
		// generally nothing known about splitter tags
		return false;
	}
}
