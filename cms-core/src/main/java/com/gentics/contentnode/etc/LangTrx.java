package com.gentics.contentnode.etc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.UserLanguage;
import com.gentics.lib.db.SQLExecutor;

/**
 * AutoCloseable that sets the current language
 */
public class LangTrx implements AutoCloseable {
	/**
	 * Old language ID
	 */
	private int oldLanguageId;

	/**
	 * Language code
	 */
	private String code = "en";

	/**
	 * Set the language for the user
	 * @param user user
	 * @throws NodeException
	 */
	public LangTrx(final SystemUser user) throws NodeException {
		oldLanguageId = ContentNodeHelper.getLanguageId(-1);
		if (user != null) {
			setLanguageId(user.getId());
		}
		setCode();
	}

	/**
	 * Set the language for the user with given id
	 * @param userId user ID
	 * @throws NodeException
	 */
	public LangTrx(final int userId) throws NodeException {
		oldLanguageId = ContentNodeHelper.getLanguageId(-1);
		if (userId > 0) {
			setLanguageId(userId);
		}
		setCode();
	}

	/**
	 * Set the userlanguage with given language code
	 * @param langCode language code
	 * @throws NodeException
	 */
	public LangTrx(String langCode) throws NodeException {
		this(langCode, true);
	}

	/**
	 * Set the userlanguge with given language code
	 * @param langCode language code
	 * @param override true to override the current language (if one set), false to only set the language, if none has been set before
	 * @throws NodeException
	 */
	public LangTrx(String langCode, boolean override) throws NodeException {
		oldLanguageId = ContentNodeHelper.getLanguageId(-1);
		if (oldLanguageId <= 0 || override) {
			DBUtils.executeStatement("SELECT id FROM language where short = ?", Transaction.SELECT_STATEMENT, st -> st.setString(1, langCode), rs -> {
				if (rs.next()) {
					ContentNodeHelper.setLanguageId(rs.getInt("id"));
					code = langCode;
				}
			});
		}
	}

	/**
	 * Set the given userlanguage
	 * @param lang user language
	 * @throws NodeException
	 */
	public LangTrx(UserLanguage lang) throws NodeException {
		oldLanguageId = ContentNodeHelper.getLanguageId(-1);
		if (lang != null) {
			ContentNodeHelper.setLanguageId(lang.getId());
			code = lang.getCode();
		}
	}

	@Override
	public void close() throws NodeException {
		ContentNodeHelper.setLanguageId(oldLanguageId);
	}

	/**
	 * Get the language code
	 * @return language code
	 */
	public String getCode() {
		return code;
	}

	/**
	 * Set the language ID 
	 * @param userId
	 * @throws NodeException
	 */
	protected void setLanguageId(int userId) throws NodeException {
		DBUtils.executeStatement(
				"SELECT language FROM systemsession WHERE user_id = ? ORDER BY id DESC LIMIT 1",
				new SQLExecutor() {
					@Override
					public void prepareStatement(PreparedStatement stmt) throws SQLException {
						stmt.setInt(1, userId);
					}

					@Override
					public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
						if (rs.next()) {
							ContentNodeHelper.setLanguageId(rs.getInt("language"));
						}
					}
				});
	}

	/**
	 * Set the language code to the current language
	 * @throws NodeException
	 */
	protected void setCode() throws NodeException {
		DBUtils.executeStatement("SELECT short FROM language WHERE id = ?", new SQLExecutor() {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				try {
					stmt.setInt(1, ContentNodeHelper.getLanguageId());
				} catch (NodeException e) {
					throw new SQLException(e);
				}
			}

			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				if (rs.next()) {
					code = rs.getString("short");
				}
			}
		});
	}
}
