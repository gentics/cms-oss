/*
 * @author Stefan Hepp
 * @date 15.12.2005
 * @version $Id: GenericTagParser.java,v 1.5 2007-01-03 12:20:15 norbert Exp $
 */
package com.gentics.contentnode.parser.tag;

import com.gentics.contentnode.parser.attribute.AttributeParser;
import com.gentics.contentnode.parser.attribute.GenericAttributeParser;

/**
 * This is a generic implementation of the tagparser, using the abstracttagparser.
 */
public class GenericTagParser extends AbstractTagParser {

	private AttributeParser attributeParser;

	public GenericTagParser() {
		super(true, false, false);
		attributeParser = new GenericAttributeParser();
	}

	protected AttributeParser getAttributeParser(String keyname) {
		return attributeParser;
	}

	protected String[] getKeynames() {
		return getParserKeynames();
	}

	/**
	 * get the list of registered parsertagfactory keynames.
	 * @return
	 */
	public String[] getParserKeynames() {
		return new String[0];
	}

	/**
	 * remove a keyname and its objectparser from the current keyname list.
	 * @param keyname the keyname to be removed.
	 * @return the registed factory for this keyname.
	 */
	public ParserTagFactory removeObjectParser(String keyname) {
		return null;
	}

	public ParserTagFactory setObjectParser(String keyname, ParserTagFactory parser) {
		return null;
	}

	public ParserTagFactory setObjectParser(String keyname, ParserTagFactory parser, String[] alias) {
		return null;
	}

	/**
	 * get a wrapper parsertagfactory which uses the registered factories depending on the keyname.
	 * @return a wrapper parsertagfactory.
	 */
	public ParserTagFactory getParserTagFactory() {
		return null;
	}
}
