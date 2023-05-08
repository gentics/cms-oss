package com.gentics.contentnode.tests.migration;

import java.util.HashMap;
import java.util.Map;

import com.gentics.api.contentnode.migration.MigrationException;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Consumer;
import com.gentics.contentnode.rest.model.Tag;
import com.gentics.lib.log.NodeLogger;

/**
 * Preprocessor that modifies tags
 */
public class ModifyingPreprocessor extends AbstractPreprocessor {
	/**
	 * Modifier implementations for specific tags
	 */
	protected static Map<String, Consumer<Tag>> modifiers = new HashMap<>();

	/**
	 * Define how a tag shall be modified
	 * @param name tag name
	 * @param mod modifier implementation
	 */
	public static void modify(String name, Consumer<Tag> mod) {
		modifiers.put(name, mod);
	}

	/**
	 * Reset all modification behaviour
	 */
	public static void reset() {
		modifiers.clear();
	}

	@Override
	public Result apply(Tag tag, NodeLogger logger) throws MigrationException {
		try {
			modifiers.getOrDefault(tag.getName(), t -> {}).accept(tag);
		} catch (NodeException e) {
			throw new MigrationException(e);
		}
		return Result.pass;
	}
}
