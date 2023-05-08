/*
 * @author Stefan Hepp
 * @date 15.12.2005
 * @version $Id: TagParser.java,v 1.9 2006-02-03 17:34:39 stefan Exp $
 */
package com.gentics.contentnode.parser.tag;

import com.gentics.contentnode.parser.tag.struct.StructParser;
import com.gentics.contentnode.parser.tag.struct.StructRenderer;
import com.gentics.contentnode.render.TemplateRenderer;

/**
 * A Tagparser is a special templaterenderer which can search and render
 * tags, using ParserTags to render the tags. The parsertags are generated
 * using a {@link ParserTagFactory}.
 */
public interface TagParser extends TemplateRenderer {

	/**
	 * get the default mode for parsing code which is passed to render methods.
	 * @return true, if templates for rendermethods should be parsed by default, else false.
	 */
	boolean doParseInputCode();

	/**
	 * get the default mode for parsing the result of render methods.
	 * @return true, if the results of rendermethods should be parsed by default, else false.
	 */
	boolean doParseResultCode();

	/**
	 * get the current mode for parsing attributes.
	 * @return true, if attribute values should be parsed before passing them to the parsertag factory.
	 */
	boolean doParseAttributes();

	/**
	 * set the default mode for parsing code which is passed to render methods.
	 * @param parseInputCode the new default value.
	 */
	void setParseInputCode(boolean parseInputCode);

	/**
	 * set the default mode for parsing the result of render methods.
	 * @param parseResultCode the new default value.
	 */
	void setParseResultCode(boolean parseResultCode);

	/**
	 * set the mode for parsing the attribute values of attributes before passing them to the tagfactory.
	 * @param parseAttributes the new mode.
	 */
	void setParseAttributes(boolean parseAttributes);

	/**
	 * Get the parsertag factory which should be used to create new ParserTags for found tags.
	 * @return a reference to the used parsertagfactory.
	 */
	ParserTagFactory getParserTagFactory();

	/**
	 * Get the structure parser which should be used to find tag positions.
	 * @return the structure parser to use.
	 */
	StructParser getStructParser();

	/**
	 * Get the structure renderer which should be used to compile the result code.
	 * @param parsing true, if the renderer should parse the code, or false if the renderer should only copy the code.
	 * @return the structure renderer to use for the requested mode. 
	 */
	StructRenderer getStructRenderer(boolean parsing);
}
