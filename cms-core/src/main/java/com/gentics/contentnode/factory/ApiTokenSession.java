package com.gentics.contentnode.factory;

import static com.gentics.contentnode.db.DBUtils.firstInt;
import static com.gentics.contentnode.db.DBUtils.select;
import static com.gentics.contentnode.db.DBUtils.update;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.i18n.Language;
import com.gentics.contentnode.i18n.CNDictionary;
import com.gentics.contentnode.rest.model.token.ApiTokenDataModel;

/**
 * Implementation of {@link Session} that is based on an API Token
 */
public class ApiTokenSession implements Session {

	private static final long serialVersionUID = 3933457009674048195L;

	private Language language;

	private int id;

	private int userId;

	private int languageId;

	/**
	 * Create an instance from the given token
	 * @param dataModel token data
	 * @throws NodeException
	 */
	public ApiTokenSession(ApiTokenDataModel dataModel) throws NodeException {
		id = dataModel.getId();
		userId = dataModel.getUserId();

		// set the language Id of the last session of the user or 1
		languageId = Math
				.max(select("SELECT language FROM systemsession WHERE user_id = ? ORDER BY since DESC LIMIT 1", pst -> {
					pst.setInt(1, userId);
				}, firstInt("language")), 1);

		language = new CNDictionary(languageId).asLanguage();

		// touch the API Token
		update("UPDATE api_token SET last_used = ? WHERE id = ?",
				TransactionManager.getCurrentTransaction().getUnixTimestamp(), dataModel.getId());
	}

	@Override
	public Language getLanguage() {
		return language;
	}

	@Override
	public int getId() {
		return id;
	}

	@Override
	public int getUserId() {
		return userId;
	}

	@Override
	public int getLanguageId() {
		return languageId;
	}
}
