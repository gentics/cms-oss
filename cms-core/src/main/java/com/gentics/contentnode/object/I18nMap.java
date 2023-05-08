package com.gentics.contentnode.object;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.BiFunction;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.object.LanguageFactory;
import com.gentics.contentnode.render.RenderType;

/**
 * Implementation of a "translation map", which is a map of Language IDs to Translation Strings
 */
public class I18nMap extends HashMap<Integer, String> {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -1788511030146263695L;

	/**
	 * Transform the given i18n map into the REST model (transforming the language IDs to language codes)
	 */
	public final static BiFunction<I18nMap, Map<String, String>, Map<String, String>> NODE2REST = (map, restModel) -> {
		Transaction t = TransactionManager.getCurrentTransaction();
		// load languages used as keys in the data map
		List<ContentLanguage> languageList = t.getObjects(ContentLanguage.class, map.keySet());

		// make a map of id -> language
		Map<Integer, ContentLanguage> languageMap = languageList.stream()
				.collect(Collectors.toMap(ContentLanguage::getId, java.util.function.Function.identity()));

		// for every entry, get the language with the id and transform the map key to the language code
		restModel.putAll(map.entrySet().stream().filter(entry -> languageMap.containsKey(entry.getKey())).collect(
				Collectors.toMap(entry -> languageMap.get(entry.getKey()).getCode(), entry -> entry.getValue())));

		return restModel;
	};

	/**
	 * Transform the given i18n map into the REST model as new map (transforming the language IDs to language codes)
	 */
	public final static Function<I18nMap, Map<String, String>> TRANSFORM2REST = map -> {
		return NODE2REST.apply(map, new HashMap<String, String>());
	};

	/**
	 * Transform the given i18n map REST model into the internal representation (transforming the language codes to language IDs)
	 */
	public final static BiFunction<Map<String, String>, I18nMap, I18nMap> REST2NODE = (restModel, map) -> {
		// make a map of code -> language
		Map<String, ContentLanguage> languageMap = LanguageFactory.languagesPerCode();

		// for every entry, get the language with the code and transform the map key to the language id
		map.putAll(restModel.entrySet().stream().filter(entry -> entry.getValue() != null)
				.filter(entry -> languageMap.containsKey(entry.getKey())).collect(
						Collectors.toMap(entry -> languageMap.get(entry.getKey()).getId(), entry -> entry.getValue())));

		return map;
	};

	/**
	 * Transform the given i18n map REST model into the internal representation (transforming the language codes to language IDs)
	 */
	public final static Function<Map<String, String>, I18nMap> TRANSFORM2NODE = restModel -> {
		return REST2NODE.apply(restModel, new I18nMap());
	};

	/**
	 * Transform the given object into an instance of {@link I18nMap}.
	 * If the object is a map with only Integers as keys, we interpret the keys as language IDs.
	 * If the object is a map with none-Integers as keys, we interpret the keys as language codes.
	 * If the object is not a map, return the default value
	 * @param object object to transform
	 * @param defaultValue default value
	 * @return transformed object or default value
	 * @throws NodeException
	 */
	public static I18nMap transform(Object object, I18nMap defaultValue) throws NodeException {
		if (object instanceof Map) {
			Map<?, ?> map = (Map<?, ?>) object;

			// check whether all keys are Integers
			if (map.keySet().stream().filter(key -> !(key instanceof Integer)).findAny().isPresent()) {
				// not all keys are Integers, so we interpret the keys as language codes
				Map<String, String> restModel = new HashMap<>();
				for (Object key : map.keySet()) {
					String code = ObjectTransformer.getString(key, null);
					if (code != null) {
						String value = ObjectTransformer.getString(map.get(code), null);
						if (value != null) {
							restModel.put(code, value);
						}
					}
				}

				return TRANSFORM2NODE.apply(restModel);
			} else {
				I18nMap returnValue = new I18nMap();
				for (Object key : map.keySet()) {
					Integer id = ObjectTransformer.getInteger(key, null);
					if (id != null) {
						String value = ObjectTransformer.getString(map.get(id), null);
						if (value != null) {
							returnValue.put(id, value);
						}
					}
				}
				return returnValue;
			}
		} else {
			return defaultValue;
		}
	}

	/**
	 * Create an empty instance
	 */
	public I18nMap() {
		super();
	}

	/**
	 * Create an instance as copy of the other instance
	 * @param other instance
	 */
	public I18nMap(I18nMap other) {
		super(other);
	}

	/**
	 * Get the translation in the current language set to the rendertype. If no language was set (or no rendertype exists),
	 * or there is no translation in the language, return the default value
	 *
	 * @param defaultValue default value
	 * @return translation or default value
	 * @throws NodeException
	 */
	public String translate(String defaultValue) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		RenderType renderType = t.getRenderType();
		if (renderType != null) {
			ContentLanguage language = renderType.getLanguage();
			if (language != null) {
				return getOrDefault(language.getId(), defaultValue);
			}
		}
		return defaultValue;
	}

	/**
	 * Add a translation with the given language code
	 * @param code language code
	 * @param translation translation
	 * @return fluent API
	 * @throws NodeException
	 */
	public I18nMap put(String code, String translation) throws NodeException {
		ContentLanguage language = LanguageFactory.get(code);
		if (language != null) {
			put(language.getId(), translation);
		}
		return this;
	}
}
