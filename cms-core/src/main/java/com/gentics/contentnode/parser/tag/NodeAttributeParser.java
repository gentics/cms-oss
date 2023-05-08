/*
 * @author Stefan Hepp
 * @date ${date}
 * @version $Id: NodeAttributeParser.java,v 1.13 2009-12-16 16:12:13 herbert Exp $
 */
package com.gentics.contentnode.parser.tag;

import com.gentics.contentnode.parser.attribute.Attribute;
import com.gentics.contentnode.parser.attribute.AttributeParser;
import com.gentics.contentnode.parser.attribute.AttributeResult;
import com.gentics.contentnode.parser.tag.struct.TagPart;
import com.gentics.contentnode.render.RenderResult;

import java.util.HashMap;
import java.util.Map;

/**
 * The NodeAttributeParser can parse attributes of node-tags. The parser currently only supports
 * one attribute, which is the name/path of the tag to render.   
 *
 * TODO extend genericAttributeParser? (although this impl. is faster) ..
 */
public class NodeAttributeParser implements AttributeParser {

	public NodeAttributeParser() {}

	public AttributeResult parseAttributes(RenderResult renderResult, String code, int startPos, boolean isEndTag) {

		Map attributes = new HashMap(1);

		// TODO: handle prop=<name>, container=, format= .. (currently not supported by PHP-Frontend)

		int endPos = code.indexOf('>', startPos);

		if (endPos >= 0) {

			int pos = code.indexOf(' ', startPos);
			String prop;

			if (pos > 0 && pos < endPos) {
				prop = code.substring(startPos, pos);
			} else {
				prop = code.substring(startPos, endPos);
			}

			// check for format-options, remove format-option from property-name
			int formatPos = prop.indexOf(':');

			if (formatPos > 0) {
				attributes.put("format", new Attribute("format", prop.substring(formatPos + 1)));
				prop = prop.substring(0, formatPos);
			}

			// for end tags, put the property name as key, so it can be found as ds-endtag!
			attributes.put("property", new Attribute("property", prop, null));

		} else {// whoops, not found!
		}

		return new AttributeResult(endPos, false, attributes);
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.parser.attribute.AttributeParser#isMatchingPair(com.gentics.lib.parser.tag.struct.TagPart, com.gentics.lib.parser.tag.struct.TagPart)
	 */
	public boolean isMatchingPair(TagPart begin, TagPart end) {
		if (begin.getType() != TagPart.TYPE_OPEN || end.getType() != TagPart.TYPE_END) {
			// parts need to be "open" and "closed" to begin with (otherwise
			// they can never be matching)
			return false;
		}

		// get the attribute "property"
		Attribute beginProperty = (Attribute) begin.getAttributes().get("property");
		Attribute endProperty = (Attribute) end.getAttributes().get("property");

		if (beginProperty == null || endProperty == null) {
			// at least one tag did not have the property set
			return false;
		}

		return beginProperty.getValue().equals(endProperty.getValue());
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.parser.attribute.AttributeParser#isSplitterTag(com.gentics.lib.parser.tag.struct.TagPart)
	 */
	public boolean isSplitterTag(TagPart tagPart) {
		// there are no splitter tags for <node> tags
		return false;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.parser.attribute.AttributeParser#isEndTag(com.gentics.lib.parser.tag.struct.TagPart)
	 */
	public boolean isEndTag(TagPart tagPart) {
		return tagPart.getType() == TagPart.TYPE_END;
	}
}
