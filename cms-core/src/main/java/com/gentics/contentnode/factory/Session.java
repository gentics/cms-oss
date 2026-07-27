package com.gentics.contentnode.factory;

import static com.gentics.contentnode.db.DBUtils.firstInt;
import static com.gentics.contentnode.db.DBUtils.firstString;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.object.UserLanguageFactory;
import com.gentics.contentnode.object.UserLanguage;
import com.gentics.lib.i18n.LanguageProvider;

/**
 * Interface for Sessions. Implementations may be backed by a DB session or an Api Token
 */
public interface Session extends LanguageProvider, Serializable {
	/**
	 * Key of the systemuser_data key storing the UI Language
	 */
	static final String UI_LANGUAGE_DATA_KEY = "uiLanguage";

	/**
	 * Get the user language of the given user.
	 * First try to get uiLanguage from the systemuser_data.
	 * If that's not set, try the language ID from the last session of the user.
	 * If that is also not set, get the first active language.
	 * @param userId user id
	 * @return optional language
	 * @throws NodeException
	 */
	static Optional<UserLanguage> getUserLanguage(int userId) throws NodeException {
		UserLanguage language = null;

		String userDataCodeJson = DBUtils.select("SELECT json FROM systemuser_data WHERE systemuser_id = ? AND name = ?", pst -> {
			pst.setInt(1, userId);
			pst.setString(2, UI_LANGUAGE_DATA_KEY);
		}, firstString("json"));

		if (userDataCodeJson != null) {
			try {
				String code = new ObjectMapper().readValue(userDataCodeJson, JsonNode.class).asText(null);
				language = UserLanguageFactory.getByCode(code);
			} catch (JsonProcessingException e) {
			}
		}

		if (language == null) {
			int languageId = DBUtils.select("SELECT language FROM systemsession WHERE user_id = ? ORDER BY since DESC LIMIT 1", pst -> {
				pst.setInt(1, userId);
			}, firstInt("language"));

			if (languageId > 0) {
				language = UserLanguageFactory.getById(languageId);
			}
		}

		if (language == null) {
			List<UserLanguage> activeLanguages = UserLanguageFactory.getActive();
			if (activeLanguages.size() > 0) {
				language = activeLanguages.get(0);
			}
		}

		return Optional.ofNullable(language);
	}

	/**
	 * Get the ID of the session
	 * @return ID of the session
	 */
	int getId();

	/**
	 * User ID of the session
	 * @return user ID
	 */
	int getUserId();

	/**
	 * @return the language id for this session. This will be the id for
	 * the language which the user selected in the user preferences.
	 */
	int getLanguageId();
}
