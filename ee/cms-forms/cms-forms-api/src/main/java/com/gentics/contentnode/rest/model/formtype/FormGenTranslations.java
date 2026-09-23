package com.gentics.contentnode.rest.model.formtype;

import org.apache.commons.codec.binary.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Model for formGen translations
 */
public class FormGenTranslations extends LinkedHashMap<String, Map<String, String>> implements Cloneable {
	private static final long serialVersionUID = -2315418348370311821L;

	/**
	 * Return a copy of this translations, which is overwritten by translations found in the other instance
	 * @param other instance
	 * @return probably overwritten copy
	 */
	public FormGenTranslations overwrite(FormGenTranslations other) {
		FormGenTranslations copy = clone();

		// overwrite with translations from the other
		if (other != null) {
			for (Map.Entry<String, Map<String, String>> entry : other.entrySet()) {
				String key = entry.getKey();
				if (entry.getValue() == null) {
					continue;
				}
				var data = copy.computeIfAbsent(key, k -> new LinkedHashMap<>());
				for (Map.Entry<String, String> translationEntry : entry.getValue().entrySet()) {
					var translationString = translationEntry.getValue();
					if (translationString == null || translationString.isEmpty()) {
						continue;
					}
					data.put(translationEntry.getKey(), translationString);
				}
			}
		}

		return copy;
	}

	@Override
	public FormGenTranslations clone() {
		FormGenTranslations copy = new FormGenTranslations();

		// create a copy of this
		for (Map.Entry<String, Map<String, String>> entry : entrySet()) {
			String key = entry.getKey();
			Map<String, String> translations = new LinkedHashMap<String, String>(entry.getValue());
			copy.put(key, translations);
		}

		return copy;
	}
}
