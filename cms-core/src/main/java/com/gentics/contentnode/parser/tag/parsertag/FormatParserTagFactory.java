/*
 * @author: Stefan Hepp
 * @date: 05.02.2006
 * @version: $Id: FormatParserTagFactory.java,v 1.4 2008-11-10 10:54:29 norbert Exp $
 */
package com.gentics.contentnode.parser.tag.parsertag;

import com.gentics.contentnode.parser.tag.ParserTag;
import com.gentics.contentnode.render.RendererFactory;

/**
 * The FormatParserTagFactory can be used to create a formatter parsertag which
 * passes all requests on to a nested parsertag and formats the result-code
 * on a specified way.
 */
public class FormatParserTagFactory {

	/**
	 * create a new FormatParserTag which wraps the given nested parsertag and formats
	 * it rendered code.
	 *
	 * @param format the requested format method.
	 * @param nestedParserTag the nested parsertag.
	 * @return a parsertag which will format the result of the nested parsertag.
	 */
	public static ParserTag getFormatterParserTag(String format, ParserTag nestedParserTag) {

		// TODO configure format options and implementations

		if ("escape".equals(format)) {
			// rendering escaped tags will disable edit mode
			return new FormatParserTag(nestedParserTag, RendererFactory.getRenderer(RendererFactory.RENDERER_ESCAPE), false);
		} else if ("nbsp".equals(format)) {
			// rendering nbsp tags will not disable edit mode
			return new FormatParserTag(nestedParserTag, RendererFactory.getRenderer(RendererFactory.RENDERER_NBSP), true);
		} else if ("oldescape".equals(format)) {
			return new FormatParserTag(nestedParserTag, RendererFactory.getRenderer(RendererFactory.RENDERER_OLDESCAPE), true);
		}

		return nestedParserTag;
	}
}
