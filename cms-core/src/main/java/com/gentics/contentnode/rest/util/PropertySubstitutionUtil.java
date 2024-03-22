package com.gentics.contentnode.rest.util;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.apache.commons.text.lookup.StringLookup;
import org.apache.commons.text.lookup.StringLookupFactory;

/**
 * Utility class for property substitution
 */
public final class PropertySubstitutionUtil {
	/**
	 * Pattern for a single property, including prefix <code>${</code> and postfix <code>}</code>
	 */
	protected final static Pattern PROPERTY_PATTERN = Pattern.compile("\\$\\{\\w+:[^\\}]+\\}");

	/**
	 * Pattern for the name of a single property, (excluding prefix and postfix). There are two named capturing groups: <code>key</code> and <code>name</code>
	 */
	protected final static Pattern PROPERTY_NAME_PATTERN = Pattern.compile("(?<key>\\w+):(?<name>[^\\}]+)");

	/**
	 * Predicate that matches all strings
	 */
	public final static Predicate<String> ALL = value -> true;

	/**
	 * Predicate that matches only a single property
	 */
	public final static Predicate<String> PROPERTY_ONLY = value -> value != null
			&& PROPERTY_PATTERN.matcher(value).matches();

	/**
	 * Get the string substitutor
	 * @return string substitutor
	 */
	public static StringSubstitutor getStringSubstitutor() {
		// configure the substitutors, that we want to support (base64 encoding and
		// decoding, url encoding and decoding, environment variables and system
		// properties)
		System.setProperty(StringLookupFactory.DEFAULT_STRING_LOOKUPS_PROPERTY,
				"BASE64_DECODER,BASE64_ENCODER,DATE,ENVIRONMENT,SYSTEM_PROPERTIES,URL_DECODER,URL_ENCODER");
		return StringSubstitutor.createInterpolator();
	}

	/**
	 * Get the string substitutor
	 * @param variableNameFilter optional variable name filter
	 * @return string substitutor
	 */
	public static StringSubstitutor getStringSubstitutor(Predicate<String> variableNameFilter) {
		StringSubstitutor stringSubstitutor = getStringSubstitutor();

		FilteredStringLookupWrapper lookupWrapper = new FilteredStringLookupWrapper(stringSubstitutor.getStringLookup(), variableNameFilter);
		stringSubstitutor.setVariableResolver(lookupWrapper);

		return stringSubstitutor;
	}

	/**
	 * Substitute all properties in the given map (will modify the map)
	 * @param map map
	 */
	public static void substituteAll(Map<String, Object> map) {
		recursivelySubstituteProperties(map, getStringSubstitutor());
	}

	/**
	 * Recursively use the {@link StringSubstitutor} on all string values of the map
	 * @param map map
	 * @param substitutor substitutor
	 */
	protected static void recursivelySubstituteProperties(Map<String, Object> map, StringSubstitutor substitutor) {
		for (Entry<String, Object> entry : map.entrySet()) {
			if (entry.getValue() instanceof Map) {
				@SuppressWarnings("unchecked")
				Map<String, Object> valueMap = (Map<String, Object>) entry.getValue();
				recursivelySubstituteProperties(valueMap, substitutor);
			} else if (entry.getValue() instanceof List) {
				@SuppressWarnings("unchecked")
				List<Object> listValue = (List<Object>) entry.getValue();
				listValue.replaceAll(value -> {
					if (value instanceof String) {
						return substitute(value.toString(), substitutor, ALL);
					} else {
						return value;
					}
				});
			} else if (entry.getValue() instanceof String) {
				String stringValue = entry.getValue().toString();
				stringValue = substitute(stringValue, substitutor, ALL);
				entry.setValue(stringValue);
			}
		}
	}

	/**
	 * Substitute the properties in the given value, if it matches the predicate
	 * @param value value
	 * @param substitutor substitutor instance
	 * @param valuePredicate predicate
	 * @return value after optional variable substitution
	 */
	protected static String substitute(String value, StringSubstitutor substitutor, Predicate<String> valuePredicate) {
		if (StringUtils.isEmpty(value)) {
			return value;
		}
		if (valuePredicate.test(value)) {
			return substitutor.replace(value);
		} else {
			return value;
		}
	}

	/**
	 * Substitute the property, if the value is only a single property (otherwise the value is returned unchanged)
	 * @param value value
	 * @param filter variable filter
	 * @return value after optional property substitution
	 */
	public static String substituteSingleProperty(String value, Predicate<String> filter) {
		return substitute(value, getStringSubstitutor(filter), PROPERTY_ONLY);
	}

	/**
	 * If the given value is a single property, return it as Optional, otherwise, return an empty Optional
	 * @param value value to test
	 * @return Optional
	 */
	public static Optional<String> isSingleProperty(String value) {
		return PROPERTY_ONLY.test(value) ? Optional.ofNullable(value) : Optional.empty();
	}

	/**
	 * Wrapper implementation of {@link StringLookup} which will optionally filter the property before substitution. Properties, which do not pass the filter, will not be passed to {@link #wrappedLookup}.
	 */
	public static class FilteredStringLookupWrapper implements StringLookup {
		/**
		 * Wrapped lookup
		 */
		protected StringLookup wrappedLookup;

		/**
		 * Optional filter
		 */
		protected Predicate<String> filter;

		/**
		 * Create a wrapper instance
		 * @param wrappedLookup wrapped lookup
		 * @param filter optional filter (may be null)
		 */
		public FilteredStringLookupWrapper(StringLookup wrappedLookup, Predicate<String> filter) {
			this.wrappedLookup = wrappedLookup;
			this.filter = filter;
		}

		@Override
		public String lookup(String key) {
			if (filter != null) {
				// the filter will get the property name, without the prefixing key (env:, sys:, ...)
				String name = key;
				Matcher matcher = PROPERTY_NAME_PATTERN.matcher(key);
				if (matcher.matches()) {
					name = matcher.group("name");
				}
				// if the name does not pass the filter, return null
				if (!filter.test(name)) {
					return null;
				}
			}
			return wrappedLookup.lookup(key);
		}
	}
}
