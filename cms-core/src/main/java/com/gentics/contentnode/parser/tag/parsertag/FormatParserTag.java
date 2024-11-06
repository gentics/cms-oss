/*
 * @author: Stefan Hepp
 * @date: 05.02.2006
 * @version: $Id: FormatParserTag.java,v 1.6 2010-04-15 14:09:12 floriangutmann Exp $
 */
package com.gentics.contentnode.parser.tag.parsertag;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.parser.tag.ParserTag;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.render.TemplateRenderer;

import java.util.Map;

/**
 * A special formatter parsertag which passes all requests on to a nested ParserTag.
 * The render result is formatted using a templaterenderer.
 */
public class FormatParserTag implements ParserTag {

	private ParserTag nestedParserTag;
	private TemplateRenderer formatter;

	/**
	 * flag for disabling edit mode for formatted tags
	 */
	private boolean renderEditMode;

	/**
	 * create a new formatparsertag.
	 * @param nestedParserTag the nested parsertag.
	 * @param formatter the templaterenderer to use to format the code.
	 * @param renderEditMode true when the formatted tags may also be rendered in edit mode, false if not
	 */
	public FormatParserTag(ParserTag nestedParserTag, TemplateRenderer formatter, boolean renderEditMode) {
		this.nestedParserTag = nestedParserTag;
		this.formatter = formatter;
		this.renderEditMode = renderEditMode;
	}

	/**
	 * Get the nested parsertag.
	 * @return the nested parsertag.
	 */
	public ParserTag getNestedParserTag() {
		return nestedParserTag;
	}

	/**
	 * get the templaterenderer used to format the code.
	 * @return the templaterenderer used to format the code.
	 */
	public TemplateRenderer getFormatter() {
		return formatter;
	}

	public boolean hasClosingTag() throws NodeException {
		return nestedParserTag.hasClosingTag();
	}

	public boolean isClosingTag() {
		return nestedParserTag.isClosingTag();
	}

	public String getTagEndCode() {
		return nestedParserTag.getTagEndCode();
	}

	public String[] getSplitterTags() {
		return nestedParserTag.getSplitterTags();
	}

	public boolean doPreParseCode(boolean defaultValue, String part) throws NodeException {
		return nestedParserTag.doPreParseCode(defaultValue, part);
	}

	public boolean doPostParseCode(boolean defaultValue) {
		return nestedParserTag.doPostParseCode(defaultValue);
	}

	public String render(RenderResult renderResult, String template, Map codeParts) throws NodeException {
		if (renderEditMode) {
			// when editmode is allowed, there is no need to disable it
			return format(renderResult, nestedParserTag.render(renderResult, template, codeParts));
		} else {
			// rendering in edit mode is not allowed, so eventually disable it
			RenderType renderType = TransactionManager.getCurrentTransaction().getRenderType();
			int oldEditMode = renderType.getEditMode();
			boolean editMode = false;

			// when editmode, switch to preview mode
			if (editMode) {
				renderType.setEditMode(RenderType.EM_PREVIEW);
			}
			try {
				return format(renderResult, nestedParserTag.render(renderResult, template, codeParts));
			} finally {
				// eventually switch back to edit mode
				if (editMode) {
					renderType.setEditMode(oldEditMode);
				}
			}
		}
	}

	public String render(RenderResult renderResult) throws NodeException {
		if (renderEditMode) {
			// when editmode is allowed, there is no need to disable it
			return format(renderResult, nestedParserTag.render(renderResult));
		} else {
			// rendering in edit mode is not allowed, so eventually disable it
			RenderType renderType = TransactionManager.getCurrentTransaction().getRenderType();
			int oldEditMode = renderType.getEditMode();
			boolean editMode = false;

			// when editmode, switch to preview mode
			if (editMode) {
				renderType.setEditMode(RenderType.EM_PREVIEW);
			}
			try {
				return format(renderResult, nestedParserTag.render(renderResult));
			} finally {
				// eventually switch back to edit mode
				if (editMode) {
					renderType.setEditMode(oldEditMode);
				}
			}
		}
	}

	/**
	 * Format the code using the formatter.
	 *
	 * @param renderResult the current renderresult.
	 * @param code the result code of the nestedparsertag render.
	 * @return the formatted code.
	 */
	private String format(RenderResult renderResult, String code) throws NodeException {
		if (code == null) {
			return null;
		}

		if (formatter != null) {
			code = formatter.render(renderResult, code);
		}

		return code;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.parser.tag.ParserTag#isEditable()
	 */
	public boolean isEditable() throws NodeException {
		if (renderEditMode) {
			// edit mode is allowed
			return nestedParserTag.isEditable();
		} else {
			// edit mode is not allowed
			return false;
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.parser.tag.ParserTag#isInlineEditable()
	 */
	public boolean isInlineEditable() throws NodeException {
		if (renderEditMode) {
			// edit mode is allowed
			return nestedParserTag.isInlineEditable();
		} else {
			// edit mode is not allowed
			return false;
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.parser.tag.ParserTag#getEditLink(boolean, int, boolean, boolean)
	 */
	public String getEditLink() throws NodeException {
		return nestedParserTag.getEditLink();
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.parser.tag.ParserTag#getEditPostfix()
	 */
	public String getEditPostfix() throws NodeException {
		return nestedParserTag.getEditPostfix();
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.parser.tag.ParserTag#getEditPrefix()
	 */
	public String getEditPrefix() throws NodeException {
		return nestedParserTag.getEditPrefix();
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.parser.tag.ParserTag#isAlohaBlock()
	 */
	public boolean isAlohaBlock() throws NodeException {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.render.Renderable#render()
	 */
	public String render() throws NodeException {
		return render(TransactionManager.getCurrentTransaction().getRenderResult());
	}
}
