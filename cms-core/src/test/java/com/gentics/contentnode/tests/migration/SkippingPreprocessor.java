package com.gentics.contentnode.tests.migration;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.gentics.api.contentnode.migration.MigrationException;
import com.gentics.contentnode.rest.model.Tag;
import com.gentics.lib.log.NodeLogger;

/**
 * Preprocessor implementation that will skip tags or the complete object
 */
public class SkippingPreprocessor extends AbstractPreprocessor {
	/**
	 * Names of tags, that shall be skipped
	 */
	protected static Set<String> skippedTags = new HashSet<>();

	/**
	 * Names of tags, that will skip the object
	 */
	protected static Set<String> skipObjectTags = new HashSet<>();

	/**
	 * Let the preprocessor skip the tags with given names
	 * @param names tag names
	 */
	public static void skipTags(String...names) {
		skippedTags.clear();
		skippedTags.addAll(Arrays.asList(names));
		skipObjectTags.removeAll(Arrays.asList(names));
	}

	/**
	 * Let the preprocessor skip the object if a tag with the given names shall be migrated
	 * @param names tag names
	 */
	public static void skipObject(String...names) {
		skipObjectTags.clear();
		skipObjectTags.addAll(Arrays.asList(names));
		skippedTags.removeAll(Arrays.asList(names));
	}

	/**
	 * Reset all skipping behaviour
	 */
	public static void reset() {
		skippedTags.clear();
		skipObjectTags.clear();
	}

	@Override
	public Result apply(Tag tag, NodeLogger logger) throws MigrationException {
		if (skipObjectTags.contains(tag.getName())) {
			return Result.skipobject;
		} else if (skippedTags.contains(tag.getName())) {
			return Result.skiptag;
		} else {
			return Result.pass;
		}
	}
}
