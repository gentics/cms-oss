package com.gentics.contentnode.factory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.BiFunction;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.etc.Supplier;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.Disinheritable;
import com.gentics.contentnode.object.I18nMap;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.lib.db.SQLExecutor;

/**
 * Uniquify Helper
 */
public class UniquifyHelper {

	/**
	 * Protected Uniquify Helper
	 */
	protected UniquifyHelper() {}

	/**
	 * Make the given value unique among all values which are selected with the given sql statement.
	 * The SQL Statement must accept the value as LAST parameter
	 * @param value initial value
	 * @param maxLength maximum length for the value
	 * @param sql SQL statement
	 * @param params list of additional parameters for the sql statement
	 * @return unique name
	 * @throws NodeException
	 */
	public static String makeUnique(String value, int maxLength, String sql, Object... params) throws NodeException {
		return makeUnique(value, maxLength, sql, SeparatorType.none, params);
	}

	/**
	 * Make the given value unique among all values which are selected with the given sql statement.
	 * The SQL Statement must accept the value as LAST parameter
	 * @param value initial value
	 * @param maxLength maximum length for the value
	 * @param sql SQL statement
	 * @param type SeparatorType
	 * @param params list of additional parameters for the sql statement
	 * @return unique name
	 * @throws NodeException
	 */
	public static String makeUnique(String value, int maxLength, String sql, SeparatorType type, Object... params) throws NodeException {
		if (value == null) {
			value = "";
		}
		if (StringUtils.isNotEmpty(value)) {
			value = value.trim();
		}
		if (type == null) {
			type = SeparatorType.none;
		}

		// check for maxLength of initial value
		if (maxLength > 0 && value.length() > maxLength) {
			value = value.substring(0, Math.min(maxLength, value.length()));
		}

		// check whether conflicting values are found
		if (!findConflictingValues(value, sql, params)) {
			return value;
		}

		// now add numbers to the given value and try again
		String base = value;
		String separator = type.getSeparator();
		int number = 0;

		Matcher matcher = type.getMatcher(value);

		if (matcher.matches()) {
			base = matcher.group(1);
			separator = "";
			number = Integer.parseInt(matcher.group(2));
		}

		while (true) {
			// try the next number
			++number;
			String toAdd = String.format("%s%d", separator, number);

			if (maxLength > 0) {
				value = String.format("%s%s", base.substring(0, Math.min(base.length(), maxLength - toAdd.length())), toAdd);
			} else {
				value = String.format("%s%s", base, toAdd);
			}
			if (!findConflictingValues(value, sql, params)) {
				return value;
			}
		}
	}

	/**
	 * Make the given value unique
	 * @param start supplier that supplies the start value
	 * @param checker function that checks a value for uniqueness and returns true, when the value is unique
	 * @param generator function that generates a new value out of the base value and the number
	 * @param initial optional function that generates the base value and starting number from the value
	 * @return unique value
	 * @throws NodeException
	 */
	public static String makeUnique(Supplier<String> start, Function<String, Boolean> checker, BiFunction<String, Integer, String> generator,
			Function<String, Pair<String, Integer>> initial) throws NodeException {
		String value = start.supply();
		if (checker.apply(value)) {
			return value;
		}

		// now add numbers to the given value and try again
		String base = value;
		int number = 0;

		if (initial != null) {
			Pair<String, Integer> initialPair = initial.apply(value);
			base = initialPair.getLeft();
			number = initialPair.getRight();
		}

		while (true) {
			// try the next number
			++number;
			value = generator.apply(base, number);

			if (checker.apply(value)) {
				return value;
			}
		}
	}

	/**
	 * Check for conflicting values
	 * @param value value
	 * @param sql sql statement
	 * @param params optional parameters
	 * @return true if conflicting values found, false if not
	 * @throws NodeException
	 */
	public static boolean findConflictingValues(final String value, String sql, final Object... params) throws NodeException {
		final List<Boolean> retVal = new Vector<Boolean>(1);

		DBUtils.executeStatement(sql, new SQLExecutor() {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				int paramCount = 1;

				for (Object p : params) {
					stmt.setObject(paramCount++, p);
				}
				stmt.setString(paramCount++, value);
			}

			@Override
			public void handleResultSet(ResultSet rs) throws SQLException,
						NodeException {
				retVal.add(rs.next());
			}
		});

		return retVal.get(0);
	}

	/**
	 * Check for conflicting values
	 * @param value value
	 * @param sql sql statement
	 * @param params optional parameters
	 * @return true if conflicting values found, false if not
	 * @throws NodeException
	 */
	public static boolean findConflictingValues(final int value, String sql, final Object... params) throws NodeException {
		final List<Boolean> retVal = new Vector<Boolean>(1);
	
		DBUtils.executeStatement(sql, new SQLExecutor() {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				int paramCount = 1;
	
				for (Object p : params) {
					stmt.setObject(paramCount++, p);
				}
				stmt.setInt(paramCount++, value);
			}
	
			@Override
			public void handleResultSet(ResultSet rs) throws SQLException,
						NodeException {
				retVal.add(rs.next());
			}
		});
	
		return retVal.get(0);
	}

	/**
	 * Get object, that is hindering the given object to use a property in the given object segment or null if the property is available
	 * @param clazz class of the objects
	 * @param referenceValue reference value
	 * @param propertyFunction function that extracts the property to check
	 * @param objectIds collection of object IDs to check against
	 * @return null iff the property can be used on the specified object or the object, that is in the way
	 * @throws NodeException
	 */
	public static <T extends NodeObject> T getObjectUsingProperty(Class<T> clazz, String referenceValue,
			Function<T, Set<String>> propertyFunction, Collection<Integer> objectIds) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		List<T> objects = t.getObjects(clazz, objectIds, false, false);
		for (T o : objects) {
			Set<String> objectValues = propertyFunction.apply(o);
			if (objectValues.contains(referenceValue)) {
				return o;
			}
		}
		return null;
	}

	/**
	 * Checks if the object's property is available.
	 * @param clazz class of the objects
	 * @param referenceValue reference value
	 * @param propertyFunction function that extracts the property to check
	 * @param objectIds collection of object IDs to check against
	 * @return true iff the property can be used on the specified object
	 * @throws NodeException
	 */
	public static <T extends NodeObject> boolean isPropertyAvailable(Class<T> clazz, String referenceValue,
			Function<T, Set<String>> propertyFunction,
			Collection<Integer> objectIds)
			throws NodeException {
		return getObjectUsingProperty(clazz, referenceValue, propertyFunction, objectIds) == null;
	}

	/**
	 * Make the given reference value unique by adding numbers to the original value.
	 * Uniqueness is checked with the given checkFunction
	 * @param referenceValue value to make unique
	 * @param type separator type
	 * @param maxLength maximum allowed length
	 * @param checkFunction function, which checks whether a value is unique. It should return "true" for unique values, "false" otherwise
	 * @return possibly modified value
	 * @throws NodeException
	 */
	public static String makeUnique(String referenceValue, UniquifyHelper.SeparatorType type, int maxLength,
			Function<String, Boolean> checkFunction) throws NodeException {
		if (type == null) {
			type = UniquifyHelper.SeparatorType.none;
		}
		if (StringUtils.isNotEmpty(referenceValue)) {
			referenceValue = referenceValue.trim();
		}

		if (checkFunction.apply(referenceValue)) {
			return referenceValue;
		}

		// now add numbers to the given value and try again
		String base = referenceValue;
		String separator = type.getSeparator();
		int number = 0;

		Matcher matcher = type.getMatcher(base);

		if (matcher.matches()) {
			base = matcher.group(1);
			separator = "";
			number = Integer.parseInt(matcher.group(2));
		}

		String modifiedValue = null;
		while (true) {
			// try the next number
			++number;
			String toAdd = String.format("%s%d", separator, number);
			if (maxLength > 0 && (base.length() + toAdd.length()) > maxLength) {
				modifiedValue = String.format("%s%s", base.substring(0, maxLength - toAdd.length()), toAdd);
			} else {
				modifiedValue = String.format("%s%s", base, toAdd);
			}
			if (checkFunction.apply(modifiedValue)) {
				return modifiedValue;
			}
		}
	}

	/**
	 * Make the given value unique for the object
	 * @param <T> type of the object
	 * @param object object holding the reference value
	 * @param referenceValue value to be made unique
	 * @param propertyFunction function that extracts the attribute values from other objects
	 * @param objectIds object IDs of the objects from which the attribute values shall be extracted
	 * @param type separator type (when the reference value needs to be made unique by adding numbers)
	 * @param maxLength maximum allowed length for the reference value
	 * @return unique value
	 * @throws NodeException
	 */
	public static <T extends NodeObject> String makeUnique(Disinheritable<T> object,
			String referenceValue, Function<T, Set<String>> propertyFunction, Collection<Integer> objectIds,
			UniquifyHelper.SeparatorType type, int maxLength) throws NodeException {
		@SuppressWarnings("unchecked")
		Class<T> clazz = (Class<T>) object.getObjectInfo().getObjectClass();

		return makeUnique(referenceValue, type, maxLength,
				value -> isPropertyAvailable(clazz, value, propertyFunction, objectIds));
	}

	/**
	 * Make all translations in the i18n map unique
	 * @param <T> type of the object
	 * @param object object holding the reference value
	 * @param referenceI18nMap reference value map
	 * @param propertyFunction function that extracts the attribute values from other objects
	 * @param objectIds object IDs of the objects from which the attribute values shall be extracted
	 * @param type separator type (when the reference value needs to be made unique by adding numbers)
	 * @param maxLength maximum allowed length for the reference value
	 * @return i18n map containing unique values
	 * @throws NodeException
	 */
	public static <T extends NodeObject> I18nMap makeUnique(Disinheritable<T> object,
			I18nMap referenceI18nMap, Function<T, Set<String>> propertyFunction, Collection<Integer> objectIds,
			UniquifyHelper.SeparatorType type, int maxLength) throws NodeException {
		if (type == null) {
			type = UniquifyHelper.SeparatorType.none;
		}

		I18nMap uniqueI18nMap = new I18nMap();
		uniqueI18nMap.putAll(referenceI18nMap);

		for (Map.Entry<Integer, String> entry : uniqueI18nMap.entrySet()) {
			String referenceValue = entry.getValue();
			String modifiedValue = makeUnique(object, referenceValue, propertyFunction, objectIds, type, maxLength);
			entry.setValue(modifiedValue);
		}

		return uniqueI18nMap;
	}

	/**
	 * Make the values in the given {@link I18nMap} unique among each other. Checks will be done in specific order:
	 * <ol>
	 * <li>Modified values (which are different from the given original) are checked first</li>
	 * <li>Values are checked in the reverse order of the given languages (so translations for languages with the highest priority will be checked and possibly modified last)</li>
	 * </ol>
	 * The translations will also be checked against the given default value (only the "en" translation is allowed to be identical to the default value)
	 * @param languages list of content languages to check
	 * @param map map to be checked
	 * @param defaultValue default value
	 * @param original original translations
	 * @param type separator type
	 * @param maxLength maximum allowed value length
	 * @return translation map with unique values
	 * @throws NodeException
	 */
	public static I18nMap makeUnique(List<ContentLanguage> languages, I18nMap map, String defaultValue,
			Optional<I18nMap> original, UniquifyHelper.SeparatorType type, int maxLength) throws NodeException {
		languages = new ArrayList<>(languages);
		Collections.reverse(languages);
		List<ContentLanguage> languagesToCheck = new ArrayList<>();

		// first get the languages (in defined order) for the values that were changed, we will check those first
		for (ContentLanguage language : languages) {
			String originalValue = original.map(o -> o.get(language.getId())).orElse(null);
			if (StringUtils.isNotBlank(map.getOrDefault(language.getId(), null))
					&& !StringUtils.equalsIgnoreCase(map.get(language.getId()), originalValue)) {
				languagesToCheck.add(language);
			}
		}

		// now add the remaining languages, for which translations exist
		for (ContentLanguage language : languages) {
			// omit languages, which were added before
			if (languagesToCheck.contains(language)) {
				continue;
			}

			if (StringUtils.isNotBlank(map.getOrDefault(language.getId(), null))) {
				languagesToCheck.add(language);
			}
		}

		I18nMap uniqueI18nMap = new I18nMap();
		uniqueI18nMap.putAll(map);

		// now iterate over the languages and check, whether the value is different from
		// all other values (and the default value, if the language is not "en")
		for (ContentLanguage language : languagesToCheck) {
			String value = uniqueI18nMap.get(language.getId());

			value = makeUnique(value, type, maxLength, v -> {
				// check whether any other translation uses the value in question
				if (uniqueI18nMap.entrySet().stream().filter(entry -> Integer.compare(entry.getKey(), language.getId()) != 0)
						.filter(entry -> StringUtils.equals(v, entry.getValue())).findAny().isPresent()) {
					return false;
				}
				// when checking for languages other than "en", the value must also be different from the default value
				if (!StringUtils.equals(language.getCode(), "en")) {
					return !StringUtils.equals(v, defaultValue);
				} else {
					return true;
				}
			});
			uniqueI18nMap.put(language.getId(), value);
		}

		return uniqueI18nMap;
	}

	/**
	 * Enumeration for the types, how the possibly added numbers are separated from the base names
	 */
	public static enum SeparatorType {

		/**
		 * Not separated
		 */
		none("(.*)([0-9]+)", ""), /**
		 * Separated by whitespace
		 */ blank("(.*\\s+)([0-9]+)", " "), /**
		 * Separated by an underscore
		 */ underscore("(.*_)([0-9]+)", "_");

		/**
		 * Pattern to detect names that already contain numbers
		 */
		private Pattern pattern;

		/**
		 * Separator
		 */
		private String separator;

		/**
		 * Create an instance
		 * @param regex regex of the pattern
		 * @param separator separator
		 */
		private SeparatorType(String regex, String separator) {
			pattern = Pattern.compile(regex);
			this.separator = separator;
		}

		/**
		 * Get the separator
		 * @return separator
		 */
		public String getSeparator() {
			return separator;
		}

		/**
		 * Get a matcher for the given value
		 * @param value value
		 * @return matcher
		 */
		public Matcher getMatcher(String value) {
			return pattern.matcher(value);
		}
	}
}
