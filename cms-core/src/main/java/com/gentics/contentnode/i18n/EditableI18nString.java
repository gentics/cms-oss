package com.gentics.contentnode.i18n;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.i18n.I18nString;
import com.gentics.contentnode.factory.object.FactoryDataRow;
import com.gentics.contentnode.factory.object.UserLanguageFactory;
import com.gentics.contentnode.object.UserLanguage;
import com.gentics.lib.i18n.LanguageProvider;
import com.gentics.lib.i18n.LanguageProviderFactory;
import com.gentics.lib.log.NodeLogger;

/**
 * Implementation of I18nString that stores the translations in a map.
 * This can be used for editable objects that have translatable names, descriptions, ...
 */
public class EditableI18nString extends I18nString {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -7606880553100081867L;

	/**
	 * Entries
	 */
	protected Map<Integer, String> entries = new HashMap<>();

	/**
	 * Create an empty instance
	 */
	public EditableI18nString() {
		super("", null);
	}

	/**
	 * Set a translation in a language.
	 * The translation will also be set for all other languages that have not yet been set.
	 * @param language language (1 for german, 2 for english)
	 * @param translation translation
	 * @return true if something was changed
	 */
	public boolean put(int language, String translation) {
		boolean updated = false;
		for (UserLanguage userLanguage : UserLanguageFactory.getActive()) {
			int id = userLanguage.getId();
			if (id == language || entries.get(id) == null) {
				if (!StringUtils.equals(entries.get(id), translation)) {
					updated = true;
					entries.put(id, translation);
				}
			}
		}
		return updated;
	}

	/**
	 * Get the translation in the given language, null if not set
	 * @param language language
	 * @return translation or null
	 */
	public String get(int language) {
		return entries.get(language);
	}

	/**
	 * Initialize the instance with current entries from the dictionary with given output ID
	 * @param outputId output ID
	 * @throws NodeException
	 */
	public void init(int outputId) throws NodeException {
		if (outputId > 0) {
			List<FactoryDataRow> entries = CNDictionary.getDicuserEntries(outputId);

			for (FactoryDataRow dicEntry : entries) {
				put(dicEntry.getInt("language_id"), dicEntry.getString("value"));
			}
		}
	}

	@Override
	protected LanguageProvider getLanguageProvider() {
		try {
			return LanguageProviderFactory.getInstance().getProvider();
		} catch (NodeException e) {
			return null;
		}
	}

	@Override
	public String toString() {
		LanguageProvider languageProvider = getLanguageProvider();

		if (languageProvider != null) {
			if (languageProvider.getLanguage() == null) {
				NodeLogger.getLogger(getClass()).error("error translating i18nstring, language from languageprovider was null");
				return null;
			} else {
				return entries.get(Integer.parseInt(languageProvider.getLanguage().getId()));
			}
		} else {
			NodeLogger.getLogger(getClass()).error("invalid i18nstring, languagecontainer was null.");
			return null;
		}
	}
}
