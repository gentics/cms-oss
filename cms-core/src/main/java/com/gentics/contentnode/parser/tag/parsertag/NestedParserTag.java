/*
 * @author Stefan Hepp
 * @date 20.01.2005
 * @version $Id: NestedParserTag.java,v 1.6 2010-04-15 14:09:12 floriangutmann Exp $
 */
package com.gentics.contentnode.parser.tag.parsertag;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.parser.tag.ParserTag;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.render.RendererFactory;
import com.gentics.contentnode.render.TemplateRenderer;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * The NestedParserTag is a special parsertag which can contain and render several
 * parsertags.
 * The nested tags are parsed in the reverse order they are added. The top parsertag
 * can request a template or splittertags. The other parsertags are given the rendered code
 * from the previous parsertags as template, if the tag requests a template.
 * This can be used if more than one ParserTag should be used to render a tag in the code.
 */
public class NestedParserTag implements ParserTag {

	private List parserTags;
	private boolean checkPreparsing;

	/**
	 * Create a new nestedparsertag and initialize it with a parsertag.
	 * @param rootParserTag the first parsertag to add.
	 */
	public NestedParserTag(ParserTag rootParserTag) {
		parserTags = new ArrayList(2);
		parserTags.add(rootParserTag);
		checkPreparsing = true;
	}

	/**
	 * Create a new nestedparsertag and initialize it with a parsertag.
	 * If checkPreparsing is disabled, all tags will be rendered, even if the result
	 * code is not used.
	 *
	 * @param rootParserTag the first parsertag to add.
	 * @param checkPreparsing true, if the tags should be checked if they need a template, else false.
	 */
	public NestedParserTag(ParserTag rootParserTag, boolean checkPreparsing) {
		this.checkPreparsing = checkPreparsing;
		parserTags = new ArrayList(2);
		parserTags.add(rootParserTag);
	}

	/**
	 * Get the last used parsertag.
	 * @return
	 */
	public ParserTag getRootParserTag() {
		return (ParserTag) parserTags.get(0);
	}

	/**
	 * get the first used parsertag.
	 * @return
	 */
	public ParserTag getTopParserTag() {
		return (ParserTag) parserTags.get(parserTags.size() - 1);
	}

	/**
	 * add a new parsertag as the first used parsertag.
	 * @param tag the new parsertag to add to the stack.
	 */
	public void addNestedParserTag(ParserTag tag) {
		parserTags.add(tag);
	}

	/**
	 * check if the top parser needs a closing tag.
	 * @return true, if the top parser needs a closing tag.
	 */
	public boolean hasClosingTag() throws NodeException {
		return getTopParserTag().hasClosingTag();
	}

	/**
	 * check, if the top parser is a closing tag.
	 * @return true, if the top parser needs a closing tag.
	 */
	public boolean isClosingTag() {
		return getTopParserTag().isClosingTag();
	}

	/**
	 * get the endcode of the top parsertag.
	 * @return the endcode of the top parsertag, or null if it does not have one.
	 */
	public String getTagEndCode() {
		return getTopParserTag().getTagEndCode();
	}

	/**
	 * get the splitter tags from the top parsertag.
	 * @return the splitter tags from the top parsertag, or null if it does not have any.
	 */
	public String[] getSplitterTags() {
		return getTopParserTag().getSplitterTags();
	}

	public boolean doPreParseCode(boolean defaultValue, String part) throws NodeException {
		return getTopParserTag().doPreParseCode(defaultValue, part);
	}

	public boolean doPostParseCode(boolean defaultValue) {
		return getRootParserTag().doPostParseCode(defaultValue);
	}

	public String render(RenderResult renderResult, String template, Map codeParts) throws NodeException {
		int pos = getFirstParserTag();
		String code;

		if (pos == parserTags.size() - 1) {
			code = ((ParserTag) parserTags.get(pos)).render(renderResult, template, codeParts);
		} else {
			code = ((ParserTag) parserTags.get(pos)).render(renderResult);
		}
		return renderNestedTags(renderResult, code, pos - 1);
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.render.Renderable#render()
	 */
	public String render() throws NodeException {
		return render(TransactionManager.getCurrentTransaction().getRenderResult());
	}

	public String render(RenderResult renderResult) throws NodeException {
		int pos = getFirstParserTag();
		String code = ((ParserTag) parserTags.get(pos)).render(renderResult);

		return renderNestedTags(renderResult, code, pos - 1);
	}

	private int getFirstParserTag() throws NodeException {

		if (!checkPreparsing) {
			return parserTags.size() - 1;
		}

		int pos;

		for (pos = 0; pos < parserTags /**/.size(); pos++) {
			if (pos == parserTags.size() - 1 || ((ParserTag) parserTags.get(pos)).doPreParseCode(true, null)) {
				break;
			}
		}

		return pos;
	}

	private String renderNestedTags(RenderResult result, String code, int pos) throws NodeException {
		RenderType renderType = TransactionManager.getCurrentTransaction().getRenderType();

		for (int i = pos; i >= 0; i--) {
			if (((ParserTag) parserTags.get(i + 1)).doPostParseCode(true)) {
				renderType.push();

				try {
					TemplateRenderer renderer = RendererFactory.getRenderer(renderType.getDefaultRenderer());

					code = renderer.render(result, code);
				} finally {
					renderType.pop();
				}
			}
			code = ((ParserTag) parserTags.get(i)).render(result, code, Collections.EMPTY_MAP);
		}
		return code;
	}

	 /*
	 * (non-Javadoc)
	 * @see com.gentics.lib.parser.tag.ParserTag#isAlohaBlock()
	 */
	public boolean isAlohaBlock() throws NodeException {
		return false;
	}    
    
	/* (non-Javadoc)
	 * @see com.gentics.lib.parser.tag.ParserTag#isEditable()
	 */
	public boolean isEditable() throws NodeException {
		return false;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.parser.tag.ParserTag#isInlineEditable()
	 */
	public boolean isInlineEditable() throws NodeException {
		return false;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.parser.tag.ParserTag#getEditLink(boolean, int, boolean, boolean)
	 */
	public String getEditLink() throws NodeException {
		return null;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.parser.tag.ParserTag#getEditPostfix()
	 */
	public String getEditPostfix() throws NodeException {
		return "";
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.parser.tag.ParserTag#getEditPrefix()
	 */
	public String getEditPrefix() throws NodeException {
		return "";
	}
}
