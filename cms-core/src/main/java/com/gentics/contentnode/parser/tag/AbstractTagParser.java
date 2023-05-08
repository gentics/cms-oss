/*
 * @author Stefan Hepp
 * @date 21.01.2006
 * @version $Id: AbstractTagParser.java,v 1.13 2008-03-07 12:53:46 norbert Exp $
 */
package com.gentics.contentnode.parser.tag;

import java.util.List;
import java.util.Vector;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.parser.attribute.AttributeParser;
import com.gentics.contentnode.parser.tag.struct.CodePart;
import com.gentics.contentnode.parser.tag.struct.CodeStructRenderer;
import com.gentics.contentnode.parser.tag.struct.ParseStructRenderer;
import com.gentics.contentnode.parser.tag.struct.RenderReturnCode;
import com.gentics.contentnode.parser.tag.struct.StructParser;
import com.gentics.contentnode.parser.tag.struct.StructRenderer;
import com.gentics.contentnode.parser.tag.struct.TagStructParser;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.log.RuntimeProfiler;
import com.gentics.lib.log.profilerconstants.JavaParserConstants;

/**
 * This is an abstract, generic implementation of the TagParser interface.
 * This implementation uses the default structparser and structrenderer.
 */
public abstract class AbstractTagParser implements TagParser {

	private boolean parseInputCode;

	private boolean parseResultCode;

	private boolean parseAttributes;

	protected final static NodeLogger logger = NodeLogger.getNodeLogger(AbstractTagParser.class);

	/**
	 * initialize a new abstracttagparser and set some parsing modes.
	 *
	 * @param parseInputCode default mode for the inputcode parsing.
	 * @param parseResultCode default mode for the resultcode parsing.
	 * @param parseAttributes default mode for the attribute parsing.
	 */
	protected AbstractTagParser(boolean parseInputCode, boolean parseResultCode, boolean parseAttributes) {
		this.parseInputCode = parseInputCode;
		this.parseResultCode = parseResultCode;
		this.parseAttributes = parseAttributes;
	}

	public boolean doParseInputCode() {
		return parseInputCode;
	}

	public boolean doParseResultCode() {
		return parseResultCode;
	}

	public boolean doParseAttributes() {
		return parseAttributes;
	}

	public void setParseInputCode(boolean parseInputCode) {
		this.parseInputCode = parseInputCode;
	}

	public void setParseResultCode(boolean parseResultCode) {
		this.parseResultCode = parseResultCode;
	}

	public void setParseAttributes(boolean parseAttributes) {
		this.parseAttributes = parseAttributes;
	}

	public StructParser getStructParser() {
		return TagStructParser.getInstance();
	}

	public StructRenderer getStructRenderer(boolean parsing) {
		return parsing ? ParseStructRenderer.getInstance() : CodeStructRenderer.getInstance();
	}

	public String render(RenderResult renderResult, String template) throws NodeException {

		String[] keynames = getKeynames();

		String source = template;

		for (int i = 0; i < keynames.length; i++) {
			String keyname = keynames[i];

			AttributeParser parser = getAttributeParser(keyname);

			List struct = null;
            
			try {
				RuntimeProfiler.beginMark(JavaParserConstants.PARSER_PARSE, getClass().getName());
				struct = getStructParser().parseToStruct(this, renderResult, source, keyname, parser);
			} finally {
				RuntimeProfiler.endMark(JavaParserConstants.PARSER_PARSE, getClass().getName());
			}

			try {
				RuntimeProfiler.beginMark(JavaParserConstants.PARSER_RENDER, getClass().getName());
				source = renderStruct(renderResult, source, struct);
			} finally {
				RuntimeProfiler.endMark(JavaParserConstants.PARSER_RENDER, getClass().getName());
			}
		}

		return source;
	}

	private String renderStruct(RenderResult renderResult, String template, List struct) throws NodeException {

		StringBuffer source = new StringBuffer(template.length());

		int pos = 0;

		List omitTags = new Vector();
		List omitTagsEdit = new Vector();

		while (pos < struct.size()) {
			RenderReturnCode retCode = getStructRenderer(true).renderStruct(this, renderResult, source, template, struct, pos, null, null, omitTags,
					omitTagsEdit);

			if (retCode.getReason() != RenderReturnCode.RETURN_LAST || retCode.getPos() < struct.size()) {
				// whoops, one closing tag too much, handle error..
				int realpos = retCode.getPos() - 1;
				String info = "";

				if (realpos < struct.size()) {
					CodePart part = (CodePart) struct.get(realpos);

					info = " for {" + part.toString() + "}";
				}
				renderResult.warn("Invalid tag", "Found an end-tag without an opening tag" + info);
			}

			// beware of endless loops here
			if (retCode.getPos() == pos) {
				CodePart codePart = (CodePart) struct.get(pos);
				int[] posInTemplate = StringUtils.findPosition(template, codePart.getStartPos());

				// this would cause an endless loop
				if (posInTemplate != null) {
					logger.warn(
							"Possible endless loop found while rendering {" + template + "}. Avoiding loop by advancing to next struct entry. " + struct.get(pos)
							+ " @ line: " + posInTemplate[0] + ", col: " + posInTemplate[1]);
				} else {
					logger.warn(
							"Possible endless loop found while rendering {" + template + "}. Avoiding loop by advancing to next struct entry. " + struct.get(pos));
				}
				pos = retCode.getPos() + 1;
			} else {
				pos = retCode.getPos();
			}
		}

		return source.toString();
	}

	/**
	 * get an attribute parser which is used to parse the attributes of a tag.
	 * @param keyname the keyname for which the attributeparser should be used.
	 * @return the attribute parser used to parse attribute values.
	 */
	protected abstract AttributeParser getAttributeParser(String keyname);

	/**
	 * get a list of all keynames this parser should search and parse.
	 * @return a list of all tag names to parse.
	 */
	protected abstract String[] getKeynames();

}

