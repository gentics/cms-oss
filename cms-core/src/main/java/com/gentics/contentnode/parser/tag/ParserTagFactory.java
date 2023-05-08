/*
 * @author Stefan Hepp
 * @date 13.12.2005
 * @version $Id: ParserTagFactory.java,v 1.3 2007-01-03 12:20:15 norbert Exp $
 */
package com.gentics.contentnode.parser.tag;

import java.util.Map;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.parser.attribute.Attribute;

/**
 * A ParserTagFactory is used to create new parsertags for a given tag.
 */
public interface ParserTagFactory {

	/**
	 * Get a new parsertag matching the found attributes.
	 *
	 * @param keyname the keyname/prefix of the tag
	 * @param params a parameter map of attribute-keys with values, as String->{@link Attribute}.
	 * @return a new parsertag, or null if no parsertag could be created for this tag.
	 */
	ParserTag getParserTag(String keyname, Map params) throws NodeException;

}
