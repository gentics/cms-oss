package com.gentics.contentnode.tests.migration;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.gentics.api.contentnode.migration.MigrationException;
import com.gentics.contentnode.rest.model.Tag;
import com.gentics.lib.log.NodeLogger;

/**
 * Broken Preprocessor (may return null, throw a {@link MigrationException} or a {@link RuntimeException}
 */
public class BrokenPreprocessor extends AbstractPreprocessor {
	/**
	 * Tagnames for which null shall be returned
	 */
	protected static Set<String> nullForTags = new HashSet<>();

	/**
	 * Tagnames for which a {@link MigrationException} shall be thrown
	 */
	protected static Set<String> exceptionForTags = new HashSet<>();

	/**
	 * Tagnames for which a {@link RuntimeException} shall be thrown
	 */
	protected static Set<String> runtimeExceptionForTags = new HashSet<>();

	/**
	 * Let the Preprocessor return null for the given tag names
	 * @param names tag names
	 */
	public static void nullFor(String...names) {
		nullForTags.clear();
		nullForTags.addAll(Arrays.asList(names));
		exceptionForTags.removeAll(Arrays.asList(names));
		runtimeExceptionForTags.removeAll(Arrays.asList(names));
	}

	/**
	 * Let the Preprocessor throw a {@link MigrationException} for the given tag names
	 * @param names tag names
	 */
	public static void exceptionFor(String...names) {
		exceptionForTags.clear();
		exceptionForTags.addAll(Arrays.asList(names));
		nullForTags.removeAll(Arrays.asList(names));
		runtimeExceptionForTags.removeAll(Arrays.asList(names));
	}

	/**
	 * Let the Preprocessor throw a {@link RuntimeException} for the given tag names
	 * @param names tag names
	 */
	public static void runtimeExceptionFor(String...names) {
		runtimeExceptionForTags.clear();
		runtimeExceptionForTags.addAll(Arrays.asList(names));
		nullForTags.removeAll(Arrays.asList(names));
		exceptionForTags.removeAll(Arrays.asList(names));
	}

	/**
	 * Reset all error behaviour
	 */
	public static void reset() {
		nullForTags.clear();
		exceptionForTags.clear();
		runtimeExceptionForTags.clear();
	}

	@Override
	protected Result apply(Tag tag, NodeLogger logger) throws MigrationException {
		if (nullForTags.contains(tag.getName())) {
			return null;
		} else if (exceptionForTags.contains(tag.getName())) {
			throw new MigrationException(String.format("Failing on tag %s", tag.getName()));
		} else if (runtimeExceptionForTags.contains(tag.getName())) {
			throw new RuntimeException(String.format("Failing on tag %s", tag.getName()));
		} else {
			return Result.pass;
		}
	}
}
