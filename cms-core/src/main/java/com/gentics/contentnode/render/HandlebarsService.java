package com.gentics.contentnode.render;

import com.github.jknack.handlebars.Handlebars;

/**
 * Interface for implementations that register helpers to handlebars instances
 */
public interface HandlebarsService {
	/**
	 * Register helpers
	 * @param handlebars handlebars instance
	 */
	void registerHelpers(Handlebars handlebars);
}
