package com.gentics.contentnode.etc;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.apache.commons.text.lookup.StringLookupFactory;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.rest.util.MiscUtils;

/**
 * Implementation of {@link NodePreferences} that holds the configuration as nested Maps
 */
public class MapPreferences implements NodePreferences {
	/**
	 * Deprecated prefixes, which are removed from all configuration properties (in the given order)
	 */
	public final static List<String> DEPRECATED_PREFIXES = Arrays.asList("contentnode.", "global.config.", "config.");

	private Map<String, Object> data;

	/**
	 * Create instance
	 * @param data data as nested Maps
	 */
	public MapPreferences(Map<String, Object> data) {
		this.data = data;
		normalizeMap();
		substituteVariables();
	}

	@Override
	public String getProperty(String name) {
		Object value = getPropertyObject(name);
		if (value instanceof Collection || value instanceof Map) {
			return null;
		} else {
			return ObjectTransformer.getString(value, null);
		}
	}

	@Override
	public String[] getProperties(String name) {
		Object value = getPropertyObject(name);

		if (value instanceof Map) {
			return null;
		} else if (value instanceof Collection) {
			List<String> stringList = ((Collection<?>)value).stream().map(o -> ObjectTransformer.getString(o, null)).filter(string -> string != null).collect(Collectors.toList());
			return stringList.toArray(new String[stringList.size()]);
		} else {
			String stringValue = ObjectTransformer.getString(value, null);
			if (stringValue != null) {
				return new String[] {stringValue};
			} else {
				return null;
			}
		}
	}

	@Override
	public void setProperty(String name, String value) {
		set(name, value);
	}

	@Override
	public void setProperty(String name, String[] values) {
		set(name, values);
	}

	@Override
	public void unsetProperty(String name) {
		set(name, null);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> Map<String, T> getPropertyMap(String name) {
		Object value = getPropertyObject(name);
		if (value instanceof Map) {
			return (Map<String, T>)value;
		} else {
			return null;
		}
	}

	@Override
	public void setPropertyMap(String name, Map<String, String> map) throws NodeException {
		set(name, map);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getPropertyObject(String name) {
		name = cleanKey(name);
		StringTokenizer tokenizer = new StringTokenizer(name, ".");
		Object value = data;
		while (tokenizer.hasMoreTokens() && value instanceof Map) {
			value = ((Map<?, ?>) value).get(tokenizer.nextToken());
		}

		if (!tokenizer.hasMoreTokens()) {
			try {
				return (T)value;
			} catch (ClassCastException e) {
				return null;
			}
		} else {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getPropertyObject(Node node, String name) throws NodeException {
		// get property map
		Object nodeMapObject = getPropertyObject(name);

		if (!(nodeMapObject instanceof Map)) {
			return null;
		}

		Map<?, ?> nodeMap = (Map<?, ?>) nodeMapObject;

		List<Object> keys = Arrays.asList(node.getGlobalId().toString(), node.getFolder().getName(), Integer.toString(node.getId()));

		for (Object key : keys) {
			if (nodeMap.containsKey(key)) {
				try {
					return (T) nodeMap.get(key);
				} catch (ClassCastException e) {
					return null;
				}
			}
		}

		return null;
	}

	@Override
	public Map<String, Object> toMap() {
		return data;
	}

	/**
	 * Set the property with given name
	 * @param name name (may be a path)
	 * @param value value to set
	 */
	@SuppressWarnings("unchecked")
	public void set(String name, Object value) {
		name = cleanKey(name);
		StringTokenizer tokenizer = new StringTokenizer(name, ".");
		Map<String, Object> tmpData = data;
		while (tokenizer.hasMoreTokens() && tmpData != null) {
			String token = tokenizer.nextToken();
			if (tokenizer.hasMoreTokens()) {
				Object tmpValue = tmpData.computeIfAbsent(token, key -> new HashMap<>());
				if (tmpValue instanceof Map) {
					tmpData = (Map<String, Object>)tmpValue;
				} else {
					tmpData = null;
				}
			} else {
				tmpData.put(token, value);
			}
		}
	}

	/**
	 * Normalize the configuration map by moving the contents of entries "contentnode", "global" and "config" up one level (each)
	 */
	protected void normalizeMap() {
		if (this.data != null) {
			unwrap("contentnode");
			unwrap("global");
			unwrap("config");
		}
	}

	/**
	 * Unwrap the given key (move the value with that key one level up, if it is a map itself)
	 * @param key key to unwrap
	 */
	protected void unwrap(String key) {
		if (this.data.get(key) instanceof Map) {
			@SuppressWarnings("unchecked")
			Map<String, ?> toUnwrap = (Map<String, ?>) this.data.get(key);

			this.data.remove(key);

			for (Entry<String, ?> entryToMerge : toUnwrap.entrySet()) {
				String keyToMerge = entryToMerge.getKey();
				Object valueToMerge = entryToMerge.getValue();

				if (this.data.containsKey(keyToMerge)) {
					Object existing = this.data.get(keyToMerge);
					if (existing instanceof Map && valueToMerge instanceof Map) {
						@SuppressWarnings("unchecked")
						Map<String, Object> existingMap = (Map<String, Object>) existing;
						@SuppressWarnings("unchecked")
						Map<String, Object> mapToMerge = (Map<String, Object>) valueToMerge;
						MiscUtils.merge(existingMap, mapToMerge);
					} else {
						this.data.put(keyToMerge, valueToMerge);
					}
				} else {
					this.data.put(keyToMerge, valueToMerge);
				}
			}
		}
	}

	/**
	 * Substitute variables in all string properties
	 */
	protected void substituteVariables() {
		if (this.data != null) {
			// configure the substitutors, that we want to support (base64 encoding and decoding, url encoding and decoding, environment variables and system properties)
			System.setProperty(StringLookupFactory.DEFAULT_STRING_LOOKUPS_PROPERTY, "BASE64_DECODER,BASE64_ENCODER,DATE,ENVIRONMENT,SYSTEM_PROPERTIES,URL_DECODER,URL_ENCODER");
			recursivelySubstituteVariables(this.data, StringSubstitutor.createInterpolator());
		}
	}

	/**
	 * Recursively use the {@link StringSubstitutor} on all string values of the map
	 * @param map map
	 * @param substitutor substitutor
	 */
	protected void recursivelySubstituteVariables(Map<String, Object> map, StringSubstitutor substitutor) {
		for (Entry<String, Object> entry : map.entrySet()) {
			if (entry.getValue() instanceof Map) {
				@SuppressWarnings("unchecked")
				Map<String, Object> valueMap = (Map<String, Object>) entry.getValue();
				recursivelySubstituteVariables(valueMap, substitutor);
			} else if (entry.getValue() instanceof List) {
				@SuppressWarnings("unchecked")
				List<Object> listValue = (List<Object>) entry.getValue();
				listValue.replaceAll(value -> {
					if (value instanceof String) {
						return substitutor.replace(value.toString());
					} else {
						return value;
					}
				});
			} else if (entry.getValue() instanceof String) {
				String stringValue = entry.getValue().toString();
				stringValue = substitutor.replace(stringValue);
				entry.setValue(stringValue);
			}
		}
	}

	/**
	 * Clean the given key by removing deprecated prefixes
	 * @param key key to clean
	 * @return cleaned key
	 */
	protected String cleanKey(String key) {
		for (String prefix : DEPRECATED_PREFIXES) {
			key = StringUtils.removeStart(key, prefix);
		}
		return key;
	}
}
