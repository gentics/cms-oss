package com.gentics.contentnode.rest.util;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.apache.commons.text.lookup.StringLookupFactory;

/**
 * Utility class for property substitution
 */
public final class PropertySubstitutionUtil {
	/**
	 * Pattern for a single property
	 */
	protected final static Pattern PROPERTY_PATTERN = Pattern.compile("\\$\\{\\w+:[^\\}]+\\}");

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
	 * @return value after optional property substitution
	 */
	public static String substituteSingleProperty(String value) {
		return substitute(value, getStringSubstitutor(), PROPERTY_ONLY);
	}

	/**
	 * If the given value is a single property, return it as Optional, otherwise, return an empty Optional
	 * @param value value to test
	 * @return Optional
	 */
	public static Optional<String> isSingleProperty(String value) {
		return PROPERTY_ONLY.test(value) ? Optional.ofNullable(value) : Optional.empty();
	}
}
