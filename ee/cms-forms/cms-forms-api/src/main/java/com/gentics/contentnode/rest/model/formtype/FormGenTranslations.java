package com.gentics.contentnode.rest.model.formtype;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Model for formGen translations
 */
public class FormGenTranslations extends LinkedHashMap<String, Map<String, String>> {
	private static final long serialVersionUID = -2315418348370311821L;

	/**
	 * Return a copy of this translations, which is overwritten by translations found in the other instance
	 * @param other instance
	 * @return probably overwritten copy
	 */
	public FormGenTranslations overwrite(FormGenTranslations other) {
		FormGenTranslations copy = new FormGenTranslations();

		// create a copy of this
		for (Map.Entry<String, Map<String, String>> entry : entrySet()) {
			String key = entry.getKey();
			Map<String, String> translations = new LinkedHashMap<String, String>(entry.getValue());
			copy.put(key, translations);
		}

		// overwrite with translations from the other
		if (other != null) {
			for (Map.Entry<String, Map<String, String>> entry : other.entrySet()) {
				String key = entry.getKey();
				copy.computeIfAbsent(key, k -> new LinkedHashMap<>()).putAll(entry.getValue());
			}
		}

		return copy;
	}
}
