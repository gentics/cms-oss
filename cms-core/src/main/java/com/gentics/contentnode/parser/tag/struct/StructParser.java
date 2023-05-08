/*
 * @author stefan hepp
 * @date 14.02.2005
 * @version $Id: StructParser.java,v 1.4 2007-01-03 12:20:14 norbert Exp $
 */

package com.gentics.contentnode.parser.tag.struct;

import java.util.List;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.parser.attribute.AttributeParser;
import com.gentics.contentnode.parser.tag.TagParser;
import com.gentics.contentnode.render.RenderResult;

/**
 * parser interface for parsers which parse the code into a list of code parts.
 */
public interface StructParser {

	/**
	 * parse the code into a list of codeparts, so that the template can be handled
	 * more easily by the renderer.
	 *
	 * @param tagParser provides parsers used for eventual parsing of
	 *        subtemplates.
	 * @param renderResult may contain informational or error messages.
	 * @param template containing tags to find.
	 * @param keyname name of the tags to find.
	 * @param parser parser to parse the attributes of found tags.
	 * @return list of found {@link CodePart}
	 */
	List parseToStruct(TagParser tagParser, RenderResult renderResult,
			String template, String keyname, AttributeParser parser) throws NodeException;

}
