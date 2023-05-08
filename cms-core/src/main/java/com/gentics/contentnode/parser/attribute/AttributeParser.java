/*
 * @author Stefan Hepp
 * @date 21.01.2006
 * @version $Id: AttributeParser.java,v 1.8 2007-08-17 10:37:24 norbert Exp $
 */
package com.gentics.contentnode.parser.attribute;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.parser.tag.struct.TagPart;
import com.gentics.contentnode.render.RenderResult;

/**
 * An Attributeparser parses attributes of a xml-like tag in a given code, starting at a given position.
 * The parser must find the end of the tag and load all found attributes into a map.
 *
 * @see AttributeResult
 */
public interface AttributeParser {

	/**
	 * Parse the attributes of a tag into a map and find its end in the given code-template.
	 *
	 * @param renderResult a renderResult to store messages.
	 * @param code the code where the attributes should be parsed.
	 * @param startPos the position in the code where the parsing should be started.
	 * @param isEndTag true, if the current tag is a closing tag (pe. &lt;/tag...&gt;)
	 * @return an attributeresult object containing information about the attributes, or null if tag cound not be parsed at all.
	 * @throws NodeException 
	 */
	AttributeResult parseAttributes(RenderResult renderResult, String code, int startPos, boolean isEndTag) throws NodeException;

	/**
	 * Checks whether the given pair of tagparts may be matching (start and end of a tag)
	 * @param begin begin part
	 * @param end end part
	 * @return true when the tagparts may be matching, false if not
	 */
	boolean isMatchingPair(TagPart begin, TagPart end);

	/**
	 * Checks whether the given tagpart is a splitter tag
	 * @param tagPart tagpart in question
	 * @return true when the tagpart is a splitter tag, false if not
	 */
	boolean isSplitterTag(TagPart tagPart);

	/**
	 * Check whether the given tagpart is an end tag
	 * @param tagPart tagpart in question
	 * @return true when the tagpart is an end part, false if not
	 */
	boolean isEndTag(TagPart tagPart);
}
