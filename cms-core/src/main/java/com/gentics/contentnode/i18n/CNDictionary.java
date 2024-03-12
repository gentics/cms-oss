/*
 * @author norbert
 * @date 15.03.2007
 * @version $Id: CNDictionary.java,v 1.6 2009-12-16 16:12:13 herbert Exp $
 */
package com.gentics.contentnode.i18n;

import static com.gentics.contentnode.factory.Trx.supply;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.Vector;
import java.util.stream.Collectors;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.i18n.Language;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.UniquifyHelper;
import com.gentics.contentnode.factory.UniquifyHelper.SeparatorType;
import com.gentics.contentnode.factory.object.FactoryDataRow;
import com.gentics.contentnode.factory.object.UserLanguageFactory;
import com.gentics.contentnode.object.UserLanguage;
import com.gentics.lib.db.SQLExecutor;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.log.NodeLogger;

/**
 * Dictionary class for i18n() implementation for Content.Node (fetch
 * translations from dicuser table)
 */
public class CNDictionary extends Properties {
	private static final NodeLogger logger = NodeLogger.getNodeLogger(CNDictionary.class);

	private static final long serialVersionUID = 3018940455755303053L;

	public final static int LANGUAGE_GERMAN = 1;
	public final static int LANGUAGE_ENGLISH = 2;
	public final static int LANGUAGE_DESC = 3;
	public final static int LANGUAGE_META = 4;

	private final static Locale enLocale = new Locale("en", "EN");

	/**
	 * English Dictionary
	 */
	public final static CNDictionary ENGLISH_DICTIONARY = new CNDictionary("en");

	/**
	 * English Language Instance
	 */
	public final static Language ENGLISH = ENGLISH_DICTIONARY.asLanguage();

	/**
	 * SQL Statement for selecting dicuser entries for an output id
	 */
	public final static String SELECT_DICUSER_ENTRIES = "SELECT * FROM dicuser WHERE output_id = ?";

	/**
	 * SQL Statement for selecting an outputuser entry for an id
	 */
	public final static String SELECT_OUTPUTUSER_ENTRY = "SELECT * FROM outputuser WHERE id = ?";

	/**
	 * Language ID
	 */
	private int languageId;

	/**
	 * Resource bundle that is being used by this dictionary
	 */
	private ResourceBundle resouceBundle;

	/**
	 * Locale
	 */
	private Locale locale;

	/**
	 * Internal store for all resolved properties, synchronized to allow concurrent access
	 */
	private Map<String, String> propStore = Collections.synchronizedMap(new HashMap<String, String>());

	/**
	 * Get the outputuser entry for the given id, if not found, an entry is added
	 * @param outputId id
	 * @return entry (with uuid and udate) or null, if outputId <= 0
	 * @throws NodeException
	 */
	public final static FactoryDataRow getOutputUserEntry(int outputId) throws NodeException {
		if (outputId <= 0) {
			return null;
		}
		Transaction t = TransactionManager.getCurrentTransaction();
		PreparedStatement pst = null;
		PreparedStatement insert = null;
		ResultSet res = null;
		FactoryDataRow result = null;

		try {
			pst = t.prepareStatement(SELECT_OUTPUTUSER_ENTRY);
			pst.setInt(1, outputId);
			res = pst.executeQuery();

			if (res.next()) {
				result = new FactoryDataRow(res);
			} else {
				t.closeResultSet(res);

				insert = t.prepareInsertStatement("INSERT INTO outputuser (id) VALUES (?)");
				insert.setInt(1, outputId);
				insert.execute();

				res = pst.executeQuery();
				if (res.next()) {
					result = new FactoryDataRow(res);
				} else {
					throw new NodeException("Failed to insert missing outputuser entry for {" + outputId + "}");
				}
			}
		} catch (SQLException e) {
			throw new NodeException("Error while getting outputuser entry for {" + outputId + "}", e);
		} finally {
			t.closeResultSet(res);
			t.closeStatement(insert);
			t.closeStatement(pst);
		}

		return result;
	}

	/**
	 * Get the dicuser entries for the given output id
	 * @param outputId output id
	 * @return list of dicuser entries
	 * @throws NodeException
	 */
	public final static List<FactoryDataRow> getDicuserEntries(int outputId) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		PreparedStatement pst = null;
		ResultSet res = null;
		List<FactoryDataRow> results = new Vector<FactoryDataRow>(3);

		try {
			pst = t.prepareStatement(SELECT_DICUSER_ENTRIES);
			pst.setInt(1, outputId);
			res = pst.executeQuery();

			while (res.next()) {
				results.add(new FactoryDataRow(res));
			}
		} catch (SQLException e) {
			throw new NodeException("Error while fetching dicuser entries", e);
		} finally {
			t.closeResultSet(res);
			t.closeStatement(pst);
		}

		return results;
	}

	/**
	 * Create instance of the dictionary for the given language code
	 * @param code language code
	 * @throws NodeException 
	 */
	public CNDictionary(String code) {
		super();

		UserLanguage language = UserLanguageFactory.getByCode(code, true);

		this.languageId = language.getId();
		this.locale = language.getLocale();
		this.resouceBundle = ResourceBundle.getBundle("contentnode", locale);
	}

	/**
	 * Create instance of the dictionary for the given language
	 * @param languageId id of the language
	 * @throws NodeException 
	 */
	public CNDictionary(int languageId) throws NodeException {
		super();

		UserLanguage language = UserLanguageFactory.getById(languageId, true);

		this.languageId = language.getId();
		this.locale = language.getLocale();
		this.resouceBundle = ResourceBundle.getBundle("contentnode", locale);
	}

	/*
	 * (non-Javadoc)
	 * @see java.util.Properties#getProperty(java.lang.String)
	 */
	public String getProperty(String key) {
		return getProperty(key, null);
	}

	/**
	 * If no current transaction is found, try to from the resource bundle, otherwise first try to translate from the DB and then from the resource bundle.
	 * @param meta key to translate
	 * @param defaultValue default translation
	 * @return translation or the default
	 * @throws NodeException
	 */
	protected String getFromMeta(String meta, String defaultValue) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransactionOrNull();
		if (t == null) {
			try {
				return getFromResourceBundle(meta);
			} catch (Exception e) {
				return defaultValue;
			}
		} else {
			try {
				String value = DBUtils.select("SELECT dic.value, dic.output_id FROM dicuser AS dic, dicuser AS mdic "
						+ "WHERE dic.output_id = mdic.output_id AND dic.language_id = ? AND mdic.language_id = ? AND mdic.value = ?",
						st -> {
							st.setInt(1, languageId);
							st.setInt(2, LANGUAGE_META);
							st.setString(3, meta);
						}, rs -> {
							if (rs.next()) {
								return rs.getString("value");
							} else {
								return null;
							}
						});
				if (ObjectTransformer.isEmpty(value)) {
					return getFromResourceBundle(meta);
				} else {
					return value;
				}
			} catch (Exception e) {
				return defaultValue;
			}
		}
	}

	/**
	 * Retrieves the i18n translation from the resouce bundle by using the given i18n key
	 * @param i18nKey
	 * @return found translation 
	 * @throws NodeException
	 */
	protected String getFromResourceBundle(String i18nKey) throws NodeException {

		String i18nString = resouceBundle.getString(i18nKey);

		// Fallback to english i18n
		if (i18nString == null) {
			resouceBundle = ResourceBundle.getBundle("contentnode", enLocale);
			i18nString = resouceBundle.getString(i18nKey);
		}

		// We throw an exception when the key still can't be resolved
		if (i18nString == null) {
			throw new NodeException("Could not find i18n translation for key {" + i18nKey + "} and languageId {" + languageId + "}");
		}

		return i18nString;
	}

	/**
	 * @param outputId
	 * @param defaultValue
	 * @return returns the translated string represented by the output id.
	 */
	protected String getFromOutputId(int outputId, String defaultValue) throws NodeException {
		try {
			Map<Integer, String> dicUserMap = DBUtils.select("SELECT language_id, value FROM dicuser WHERE output_id = ?", st -> {
				st.setInt(1, outputId);
			}, rs -> {
				Map<Integer, String> tmpMap = new HashMap<>();
				while (rs.next()) {
					tmpMap.put(rs.getInt("language_id"), rs.getString("value"));
				}
				return tmpMap;
			});

			int minKey = dicUserMap.keySet().stream().min(Integer::compare).orElse(-1);
			String fromDicUser = dicUserMap.getOrDefault(languageId, dicUserMap.getOrDefault(minKey, null));

			if (fromDicUser != null && StringUtils.isEmpty(fromDicUser)) {
				fromDicUser = dicUserMap.getOrDefault(minKey, null);
			}

			if (fromDicUser != null) {
				return fromDicUser;
			} else {
				return getFromResourceBundle(defaultValue);
			}
		} catch (Exception e) {
			return defaultValue;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see java.util.Properties#getProperty(java.lang.String, java.lang.String)
	 */
	public String getProperty(String key, String defaultValue) {
		// first check propstore, so we don't have to resolve again
		if (propStore.containsKey(key)) {
			return propStore.get(key);
		}
		try {
			String value = null;
			int iKey = ObjectTransformer.getInt(key, -1);

			if (iKey < 0) {
				value = getFromMeta(key, defaultValue);
			} else {
				value = getFromOutputId(iKey, defaultValue);
			}
			// put into prop store (we don't want to resolve again)
			propStore.put(key, value);
			return value;
		} catch (NodeException e) {
			NodeLogger.getNodeLogger(getClass()).error("Error while getting key {" + key + "} from dictionary", e);
			return null;
		}
	}

	@Override
	public synchronized Object setProperty(String key, String value) {
		return propStore.put(key, value);
	}

	/**
	 * Create a new output id (for new objects with translations)
	 * @return new output id
	 * @throws NodeException
	 */
	public static int createNewOutputId() throws NodeException {
		List<Integer> keys = DBUtils.executeInsert("INSERT INTO outputuser (info) VALUES (?)", new Object[] { 6 });

		if (keys.size() != 1) {
			throw new NodeException("Error while creating new outputuser id: could not get generated id");
		}

		return keys.get(0);
	}

	/**
	 * Save the dicuser entry (insert or update, depending on whether it was already saved)
	 * @param outputId output id
	 * @param languageId language id
	 * @param translation translation
	 * @return true when something was changed, false if not
	 * @throws NodeException
	 */
	public static boolean saveDicUserEntry(int outputId, int languageId, String translation) throws NodeException {
		Set<Integer> langIdSet = UserLanguageFactory.getActive().stream().map(UserLanguage::getId).collect(Collectors.toSet());
		if (!langIdSet.contains(languageId)) {
			throw new NodeException(String.format("language must be one of %s, but was %d", langIdSet, languageId));
		}
		Transaction t = TransactionManager.getCurrentTransaction();
		PreparedStatement pst = null;
		ResultSet res = null;
		boolean changed = false;

		if (!StringUtils.isEmpty(translation)) {
			translation = translation.trim();
		}
		try {
			pst = t.prepareStatement("SELECT * FROM dicuser WHERE output_id = ? AND language_id = ?", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
			pst.setInt(1, outputId);
			pst.setInt(2, languageId);
			res = pst.executeQuery();

			if (res.next()) {
				if (!StringUtils.isEqual(res.getString("value"), translation)) {
					res.updateString("value", translation);
					res.updateRow();
					changed = true;
				}
			} else {
				res.moveToInsertRow();
				res.updateInt("output_id", outputId);
				res.updateInt("language_id", languageId);
				res.updateString("value", translation);
				res.insertRow();
				changed = true;
			}

			if (changed) {
				if (ContentNodeHelper.getLanguageId() == languageId) {
					t.getLanguage().getDic().setProperty(Integer.toString(outputId), translation);
				}
			}
			return changed;
		} catch (SQLException e) {
			throw new NodeException("Error while saving dicuser entry", e);
		} finally {
			t.closeResultSet(res);
			t.closeStatement(pst);
		}
	}

	/*
	 * @throws NodeException
	 */
	public static void deleteDicUserEntries(List<Integer> ids) throws NodeException {
		try {
			DBUtils.executeMassStatement("DELETE FROM dicuser WHERE output_id IN ", ids, 1, null);
			DBUtils.executeMassStatement("DELETE FROM outputuser WHERE id IN ", ids, 1, null);
		} catch (NodeException e) {
			throw new NodeException("Couldn't delete user translations (dicuser) {" + ids.toString() + "}", e);
		}
	}

	/**
	 * Make the given value (which shall be stored for the given outputId and languageId)
	 * unique among all values for the same language for outputIds, that can be found in
	 * the given column of the given value
	 * @param outputId output id
	 * @param languageId language id
	 * @param value initial value
	 * @param table table
	 * @param column column
	 * @return unique name
	 * @throws NodeException
	 */
	public static String makeUnique(int outputId, int languageId, String value, String table, String column) throws NodeException {
		if (value == null) {
			value = "";
		}
		if (!StringUtils.isEmpty(value)) {
			value = value.trim();
		}
		StringBuffer sqlBuffer = new StringBuffer("SELECT value FROM dicuser WHERE language_id = ? AND output_id != ? AND output_id IN (SELECT ").append(column).append(" FROM ").append(table).append(
				") AND value = ?");
		String sql = sqlBuffer.toString();

		return UniquifyHelper.makeUnique(value, 0, sql, SeparatorType.blank, languageId, outputId);
	}

	/**
	 * Prefill the dictionary of the current transaction with translations for values stored in the given table/column
	 * @param table table name
	 * @param column column name
	 * @throws NodeException
	 */
	public static void prefillDictionary(String table, String column) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Language language = t.getLanguage();
	
		if (language == null) {
			return;
		}
		final Properties dict = language.getDic();
		final int languageId = ContentNodeHelper.getLanguageId();
		StringBuffer sqlBuffer = new StringBuffer("SELECT value, output_id FROM ").append(table).append(" LEFT JOIN dicuser ON ").append(table).append(".").append(column).append(
				" = dicuser.output_id where language_id = ?");
		String sql = sqlBuffer.toString();

		DBUtils.executeStatement(sql, new SQLExecutor() {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				stmt.setInt(1, languageId);
			}

			@Override
			public void handleResultSet(ResultSet rs) throws SQLException,
						NodeException {
				while (rs.next()) {
					dict.setProperty(rs.getString("output_id"), rs.getString("value"));
				}
			}
		});
	}

	/**
	 * Ensure that all translations in dicuser for all activated languages do exist.
	 * @throws NodeException
	 */
	public static void ensureConsistency() throws NodeException {
		List<UserLanguage> activeLanguages = UserLanguageFactory.getActive();
		UserLanguage defaultLanguage = activeLanguages.get(0);
		int defaultLangId = defaultLanguage.getId();

		logger.info(String.format("Check missing translations. Default language is %s", defaultLanguage));

		for (UserLanguage language : activeLanguages) {
			if (language.equals(defaultLanguage)) {
				continue;
			}

			logger.info(String.format("Checking %s", language));
			int langId = language.getId();
			List<Integer> missingIds = DBUtils.select(
					"SELECT fb.output_id id FROM dicuser fb LEFT JOIN dicuser ON fb.output_id = dicuser.output_id AND dicuser.language_id = ? WHERE fb.language_id = ? AND dicuser.output_id IS NULL",
					ps -> {
						ps.setInt(1, langId);
						ps.setInt(2, defaultLangId);
					}, DBUtils.IDLIST);

			logger.info(String.format("Found %d missing translations", missingIds.size()));

			if (!missingIds.isEmpty()) {
				DBUtils.executeMassStatement("INSERT INTO dicuser (output_id, language_id, value) SELECT output_id, ?, value FROM dicuser WHERE language_id = ? AND output_id IN ", missingIds, 3, new SQLExecutor() {
					@Override
					public void prepareStatement(PreparedStatement ps) throws SQLException {
						ps.setInt(1, langId);
						ps.setInt(2, defaultLangId);
					}
				});
			}
		}
	}

	/**
	 * Get the dictionary as language instance
	 * @return language instance backed by this dictionary
	 */
	public Language asLanguage() {
		return new Language(Integer.toString(languageId), locale, this);
	}
}
