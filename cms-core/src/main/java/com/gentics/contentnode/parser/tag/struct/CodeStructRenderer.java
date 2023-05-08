/*
 * @author Stefan Hepp
 * @date 15.12.2006
 * @version $Id: CodeStructRenderer.java,v 1.7 2007-04-19 12:00:29 norbert Exp $
 */
package com.gentics.contentnode.parser.tag.struct;

import java.util.List;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.parser.tag.TagParser;
import com.gentics.contentnode.render.RenderResult;

/**
 * This is a special StructRenderer, which implements rendering without using the parsertags.
 * The end of the code is determined by the levels of the tag, or, if an endCode or splitter is
 * given, the first matching tag.
 * The found code is simply copied without any parsing.
 */
public class CodeStructRenderer implements StructRenderer {

	private static StructRenderer renderer = new CodeStructRenderer();

	public CodeStructRenderer() {}

	/**
	 * get a static instance of this parser.
	 * @return a reference to a static instance.
	 */
	public static StructRenderer getInstance() {
		return renderer;
	}

	/**
	 * Render the code, without parsing it.
	 * If endCode is something other than null, find the end-tag by searching any type of tag which contains the code as
	 * attribute-key, else, the list will be searched using the tag-type info. The ParserTags itself will not be asked!!
	 *
	 * @param parser a reference to the parser which calls this method.
	 * @param result the current renderResult.
	 * @param source the source, where the found code should be appended.
	 * @param template the code template.
	 * @param struct a list of codeparts of the template.
	 * @param firstElement the first element of the struct list to add to the source.
	 * @param splitter a list of splittertags for which should be checked, or null if not set.
	 * @param endCode the endcode which must match the last tag, or null if not set.
	 * @return a status returncode.
	 */
	public RenderReturnCode renderStruct(TagParser parser, RenderResult result, StringBuffer source,
			String template, List struct, int firstElement, String[] splitter, String endCode, List omitTags, List omitTagsEdit) throws NodeException {

		int pos;
		int depth = 0;

		for (pos = firstElement; pos < struct.size(); pos++) {

			CodePart part = (CodePart) struct.get(pos);

			if (part instanceof TagPart) {

				TagPart tag = (TagPart) part;

				// End-Tag must contain code and must be a closing tag
				if (tag.matchEndCode(endCode)) {

					loadCode(source, template, struct, firstElement, part);

					return new RenderReturnCode(pos + 1, RenderReturnCode.RETURN_CLOSED, null);

				}

				String splitterKey = tag.matchSplitterCodes(splitter);

				if (splitterKey != null && depth <= 0) {
					loadCode(source, template, struct, firstElement, part);
					return new RenderReturnCode(pos + 1, RenderReturnCode.RETURN_SPLITTER, splitterKey);
				}

				// search by depth, rely on syntax, not ParserTag for open/closing!
				if (tag.getType() == TagPart.TYPE_OPEN) {
					depth++;
				} else if (tag.getType() == TagPart.TYPE_END) {
					depth--;

					if (depth < 0) {
						// end tag found
						loadCode(source, template, struct, firstElement, part);

						return new RenderReturnCode(pos + 1, RenderReturnCode.RETURN_CLOSED, null);
					}
				}

			}

		}

		// TODO check whether the restriction to struct.size() is correct
		if (firstElement < struct.size()) {
			loadCode(source, template, struct, firstElement, pos - 1);
		}
		return new RenderReturnCode(pos, RenderReturnCode.RETURN_LAST, null);
	}

	/**
	 * Copy the code of the template to the source.
	 * @param source the source to append the code to.
	 * @param template the template of the code.
	 * @param struct the list of codeparts.
	 * @param firstElement the first element to copy to the code.
	 * @param pos the position of the last codepart in the struct list.
	 */
	private void loadCode(StringBuffer source, String template, List struct, int firstElement, int pos) {
		loadCode(source, template, struct, firstElement, (CodePart) struct.get(pos));
	}

	/**
	 * Copy the code of the template to the source.
	 * @param source the source to append the code to.
	 * @param template the template of the code.
	 * @param struct the list of codeparts.
	 * @param firstElement the first element to copy to the code.
	 * @param part the last codepart to copy to the code.
	 */
	private void loadCode(StringBuffer source, String template, List struct, int firstElement, CodePart part) {
		source.append(template.substring(((CodePart) struct.get(firstElement)).getStartPos(), part.getStartPos()));
	}

}
