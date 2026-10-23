package com.gentics.contentnode.mcp.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.gentics.contentnode.factory.object.UserLanguageFactory;
import com.gentics.contentnode.mcp.McpArgs;
import com.gentics.contentnode.object.UserLanguage;

/**
 * Tool arguments not covered by {@link McpArgs}
 */
public final class Args {
	/**
	 * Maximum number of entries of a map from language code to text
	 */
	public static final int MAX_I18N_ENTRIES = 20;

	private Args() {
	}

	/**
	 * Get a required object ID argument (a positive integer)
	 * @param arguments arguments
	 * @param name argument name
	 * @return ID
	 * @throws IllegalArgumentException if the argument is missing or not a positive integer
	 */
	public static int id(Map<String, Object> arguments, String name) {
		Integer id = McpArgs.intArg(arguments, name, 1, Integer.MAX_VALUE);
		if (id == null) {
			throw new IllegalArgumentException("Missing required argument '%s'".formatted(name));
		}
		return id;
	}

	/**
	 * Get an optional list of distinct values out of a fixed set
	 * @param arguments arguments
	 * @param name argument name
	 * @param allowed allowed values
	 * @param defaultValue value if not supplied
	 * @return value
	 * @throws IllegalArgumentException if the argument is not a list of distinct allowed values
	 */
	public static List<String> enumList(Map<String, Object> arguments, String name, List<String> allowed,
			List<String> defaultValue) {
		Object value = arguments.get(name);
		if (value == null) {
			return defaultValue;
		}
		if (!(value instanceof List<?> list)) {
			throw new IllegalArgumentException("Argument '%s' must be a list of %s".formatted(name, allowed));
		}
		List<String> values = new ArrayList<>();
		for (Object item : list) {
			if (!(item instanceof String string) || !allowed.contains(string)) {
				throw new IllegalArgumentException(
						"Argument '%s' contains '%s', expected one of %s".formatted(name, item, allowed));
			}
			if (values.contains(string)) {
				throw new IllegalArgumentException("Argument '%s' contains '%s' more than once".formatted(name, string));
			}
			values.add(string);
		}
		return values;
	}

	/**
	 * Get an optional list of positive integers
	 * @param arguments arguments
	 * @param name argument name
	 * @param maxItems maximum number of items
	 * @return value, null if not supplied
	 * @throws IllegalArgumentException if the argument is not a list of at most maxItems positive integers
	 */
	public static List<Integer> intList(Map<String, Object> arguments, String name, int maxItems) {
		Object value = arguments.get(name);
		if (value == null) {
			return null;
		}
		if (!(value instanceof List<?> list) || list.size() > maxItems) {
			throw new IllegalArgumentException(
					"Argument '%s' must be a list of at most %d positive integers".formatted(name, maxItems));
		}
		List<Integer> values = new ArrayList<>();
		for (Object item : list) {
			if (!(item instanceof Number number) || number.doubleValue() != Math.rint(number.doubleValue())
					|| number.longValue() < 1 || number.longValue() > Integer.MAX_VALUE) {
				throw new IllegalArgumentException(
						"Argument '%s' contains '%s', expected a positive integer".formatted(name, item));
			}
			values.add(number.intValue());
		}
		return values;
	}

	/**
	 * Get an optional list of distinct positive integers, like {@link #intList}
	 * @param arguments arguments
	 * @param name argument name
	 * @param maxItems maximum number of items
	 * @return value, null if not supplied
	 * @throws IllegalArgumentException if the argument is not a list of at most maxItems distinct positive integers
	 */
	public static List<Integer> distinctIntList(Map<String, Object> arguments, String name, int maxItems) {
		List<Integer> values = intList(arguments, name, maxItems);
		if (values != null && values.stream().distinct().count() < values.size()) {
			throw new IllegalArgumentException("Argument '%s' contains a value more than once".formatted(name));
		}
		return values;
	}

	/**
	 * Get the name of the one argument of two that is supplied
	 * @param arguments arguments
	 * @param first name of the first argument
	 * @param second name of the second argument
	 * @return name of the supplied argument
	 * @throws IllegalArgumentException if neither or both are supplied
	 */
	public static String exactlyOne(Map<String, Object> arguments, String first, String second) {
		boolean hasFirst = arguments.get(first) != null;
		if (hasFirst == (arguments.get(second) != null)) {
			throw new IllegalArgumentException("Pass exactly one of '%s' and '%s'".formatted(first, second));
		}
		return hasFirst ? first : second;
	}

	/**
	 * Get an optional map from UI language code to text, which must have a text in at least one of the active UI
	 * languages (the CMS drops the others)
	 * @param arguments arguments
	 * @param name argument name
	 * @param maxLength maximum length of a text
	 * @param uiLanguages codes of the active UI languages
	 * @return value, null if not supplied
	 * @throws IllegalArgumentException if the argument is not such a map
	 */
	public static Map<String, String> i18nMap(Map<String, Object> arguments, String name, int maxLength,
			Collection<String> uiLanguages) {
		Object value = arguments.get(name);
		if (value == null) {
			return null;
		}
		if (!(value instanceof Map<?, ?> map) || map.size() > MAX_I18N_ENTRIES) {
			throw new IllegalArgumentException("Argument '%s' must map at most %d language codes to texts"
					.formatted(name, MAX_I18N_ENTRIES));
		}
		Map<String, String> texts = new LinkedHashMap<>();
		for (Map.Entry<?, ?> entry : map.entrySet()) {
			if (!(entry.getValue() instanceof String text) || text.length() > maxLength) {
				throw new IllegalArgumentException("Argument '%s' must contain texts of at most %d characters"
						.formatted(name, maxLength));
			}
			texts.put(String.valueOf(entry.getKey()), text);
		}
		if (texts.keySet().stream().noneMatch(uiLanguages::contains)) {
			throw new IllegalArgumentException("Argument '%s' needs a text in one of the UI languages %s"
					.formatted(name, uiLanguages));
		}
		return texts;
	}

	/**
	 * Get the codes of the active UI languages
	 * @return codes
	 */
	public static List<String> uiLanguages() {
		return UserLanguageFactory.getActive().stream().map(UserLanguage::getCode).toList();
	}

	/**
	 * Get an optional list of strings
	 * @param arguments arguments
	 * @param name argument name
	 * @param maxItems maximum number of items
	 * @param maxLength maximum length of an item
	 * @return value, null if not supplied
	 * @throws IllegalArgumentException if the argument is not a list of at most maxItems strings of at most maxLength
	 */
	public static List<String> stringList(Map<String, Object> arguments, String name, int maxItems, int maxLength) {
		Object value = arguments.get(name);
		if (value == null) {
			return null;
		}
		if (!(value instanceof List<?> list) || list.size() > maxItems) {
			throw new IllegalArgumentException(
					"Argument '%s' must be a list of at most %d strings".formatted(name, maxItems));
		}
		List<String> values = new ArrayList<>();
		for (Object item : list) {
			if (!(item instanceof String string) || string.length() > maxLength) {
				throw new IllegalArgumentException("Argument '%s' must contain strings of at most %d characters"
						.formatted(name, maxLength));
			}
			values.add(string);
		}
		return values;
	}
}
