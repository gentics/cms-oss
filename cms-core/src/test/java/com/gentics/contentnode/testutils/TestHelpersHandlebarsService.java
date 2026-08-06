package com.gentics.contentnode.testutils;

import java.util.HashSet;
import java.util.Set;

import com.gentics.contentnode.render.HandlebarsService;
import com.github.jknack.handlebars.Handlebars;

/**
 * Implementation of {@link HandlebarsService} which will register the helpers via the helper source classes given by {@link #addHelper(Class)}
 */
public class TestHelpersHandlebarsService implements HandlebarsService {
	protected static Set<Class<?>> helperSources = new HashSet<>();

	/**
	 * Add the given class as helper source
	 * @param helperSource helper source class
	 */
	public static void addHelper(Class<?> helperSource) {
		helperSources.add(helperSource);
	}

	/**
	 * Remove the class from the helper sources
	 * @param helperSource helper source class
	 */
	public static void removeHelper(Class<?> helperSource) {
		helperSources.remove(helperSource);
	}

	@Override
	public void registerHelpers(Handlebars handlebars) {
		helperSources.forEach(handlebars::registerHelpers);
	}
}
