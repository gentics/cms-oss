/*
 * @author Stefan Hepp
 * @date 30.12.2005
 * @version $Id: ParserTag.java,v 1.13 2010-04-15 14:09:12 floriangutmann Exp $
 */
package com.gentics.contentnode.parser.tag;

import java.util.Map;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.render.GCNRenderable;
import com.gentics.contentnode.render.RenderResult;

/**
 * A parsertag is used to render a found tag and get informations about how the code should be prepared
 * for the tag.
 */
public interface ParserTag extends GCNRenderable {

	/**
	 * check, if the tag needs a template and a closing tag. If this function returns false,
	 * no template will be searched, even if the tag is not closed.
	 * @return false, if the tag has no template, even if the tag may not be closed, else true.
	 * @throws NodeException 
	 */
	boolean hasClosingTag() throws NodeException;

	/**
	 * check, if this tag is a closing tag, even if it is not a closing tag by syntax.
	 * This can be used to make a tag a closing tag using attribute values.
	 * @return true, if this tag closes a template, else false.
	 */
	boolean isClosingTag();

	/**
	 * Very special method to allow end-tag search. <br>
	 * This is only used if doPreParseCode returns false, the tag is open and hasClosingTag returns true and
	 * the function returns not null. <br>
	 *
	 * If an endCode is returned, and code is parsed without structure-dependent parsing, the first tag, which
	 * is an end tag and contains the endCode as name of an attribute, is used as closing tag.
	 *
	 * @return endCode to search, or null if no endCode is available.
	 */
	String getTagEndCode();

	/**
	 * Very special method to allow splitter-tag search.<br>
	 * If a tag is not an end-Tag and contains one of the returned keys as name of an attribute, it is used as splitter tag.
	 * 
	 * @return a list of splitter tag keynames or null if none are requested.
	 */
	String[] getSplitterTags();

	/**
	 * check, if the template and codeparts should be parsed before they are passed to the render method.
	 *
	 * @param defaultValue the default preparse mode from the parser.
	 * @param part the splitter-part name for which preparsing is checked, or null if it is checked for the main template.
	 * @return true, if the code should be parsed, else false.
	 * @throws NodeException TODO
	 */
	boolean doPreParseCode(boolean defaultValue, String part) throws NodeException;

	/**
	 * check, if the result of the render-method should be parsed before it is added to the compiled code.
	 * @param defaultValue the default post-parse mode from the parser.
	 * @return true, if the code should be parsed after it was build, else false.
	 */
	boolean doPostParseCode(boolean defaultValue);

	/**
	 * Render the current object to a string, regarding the given renderType and
	 * renderResult. A template and codeparts, if any are found, are also passed to this method.
	 *
	 * @param renderResult container for return-messages.
	 * @param template the template code of the tag, or null if the tag has no template at all.
	 * @param codeParts a list of codeparts, as partname->template, or an empty map if no parts are found.
	 * @return the rendered code.
	 * @throws NodeException
	 */
	String render(RenderResult renderResult, String template, Map codeParts) throws NodeException;

	/**
	 * Check whether this tag is inline editable
	 * @return true when this tag is inline editable, false if not
	 * @throws NodeException
	 */
	boolean isInlineEditable() throws NodeException;

	/**
	 * Check whether this tag is editable (in a page in edit mode)
	 * @return true when this tag is editable, false if not
	 * @throws NodeException
	 */
	boolean isEditable() throws NodeException;

	/**
	 * Generate an edit link for this parser tag
	 * @return edit link for the tag
	 * @throws NodeException
	 */
	String getEditLink() throws NodeException;

	/**
	 * Get the prefix when the tag is rendered in editmode
	 * @return prefix for editmode
	 * @throws NodeException
	 */
	String getEditPrefix() throws NodeException;

	/**
	 * Get the postfix when the tag is rendered in editmode
	 * @return postfix for editmode
	 * @throws NodeException
	 */
	String getEditPostfix() throws NodeException;
    
	/**
	 * Tells if the tag is an Aloha block
	 * @return true if the tag is an Aloha block otherwise false
	 */
	boolean isAlohaBlock() throws NodeException;
}
