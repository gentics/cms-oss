/*
 * @author Stefan Hepp
 * @date 15.12.2006
 * @version $Id: StructRenderer.java,v 1.4 2007-03-19 07:57:35 norbert Exp $
 */
package com.gentics.contentnode.parser.tag.struct;

import java.util.List;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.parser.tag.TagParser;
import com.gentics.contentnode.render.RenderResult;

/**
 * This is an interface for renderer which can compile a template with a list of codeparts into a code.
 * The method of rendering depends on the implementation of the renderer.
 */
public interface StructRenderer {

	/**
	 * Render a given template into a code, starting with a given position in the codepart list.
	 *
	 * @param parser a reference to the main parser.
	 * @param result the current renderResult.
	 * @param source the source where the rendered code should be appended to.
	 * @param template the code template.
	 * @param struct the list of {@link CodePart}.
	 * @param firstElement the first element in the struct list to compile.
	 * @param splitter the list of splitter keys to honor, or null if not used.
	 * @param endCode the endcode to use for the closing tag, or null if not used.
	 * @param omitTags list of tags that shall not be rendered any more
	 * @param omitTagsEdit list of tags that shall not be rendered in edit mode any more
	 * @return a returncode container.
	 */
	RenderReturnCode renderStruct(TagParser parser, RenderResult result, StringBuffer source,
			String template, List struct, int firstElement, String[] splitter, String endCode, List omitTags, List omitTagsEdit) throws NodeException;

}
