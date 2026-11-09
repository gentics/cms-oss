package com.gentics.contentnode.render;

import com.github.jknack.handlebars.Handlebars;

/**
 * Implementation of {@link HandlebarsService}, that registers the forms helpers from class {@link FormsHandlebarsHelpers}.
 */
public class FormsHandlebarsService implements HandlebarsService {
	@Override
	public void registerHelpers(Handlebars handlebars) {
		handlebars.registerHelpers(FormsHandlebarsHelpers.class);
	}
}
