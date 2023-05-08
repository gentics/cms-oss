package com.gentics.contentnode.factory.object;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.DBTable;
import com.gentics.contentnode.factory.DBTables;
import com.gentics.contentnode.factory.FactoryHandle;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.NodeObjectInfo;
import com.gentics.contentnode.object.UserLanguage;
import com.gentics.lib.log.NodeLogger;

/**
 * Factory for {@link UserLanguage} objects
 */
@DBTables({ @DBTable(clazz = UserLanguage.class, name = "language") })
public class UserLanguageFactory extends AbstractFactory {
	/**
	 * Name of the configuration parameter
	 */
	public final static String UI_LANGUAGES_PARAM = "ui_languages";

	protected final static NodeLogger logger = NodeLogger.getNodeLogger(UserLanguageFactory.class);

	/**
	 * Threadlocal flag for using all languages
	 */
	protected final static ThreadLocal<Boolean> useAllLanguages = new ThreadLocal<>();

	/**
	 * Cache for all languages
	 */
	protected static List<UserLanguage> allLanguages;

	/**
	 * Cache for active languages
	 */
	protected static List<UserLanguage> activeLanguages;

	/**
	 * Set the (thread-local) allLanguages flag to true and return an autoclosable, that will reset it to false when closed.
	 * @return autoclosable
	 */
	public static WithAllLanguages withAllLanguages() {
		return new WithAllLanguages();
	}

	/**
	 * Initialize the active languages (activated in the configuration)
	 * Make sure that at least either "de" or "en" are active
	 * @throws NodeException
	 */
	public static void init() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		List<String> configuredCodes = new ArrayList<>();

		try {
			configuredCodes.addAll(t.getNodeConfig().getDefaultPreferences().getPropertyObject(UI_LANGUAGES_PARAM));
		} catch (ClassCastException e) {
			// in cases, where the configured ui_languages is no array, we simply leave the list empty ("de" will be added later)
		}
		// make sure that at least "de" or "en" are in the list (add "de", if neither found)
		if (!configuredCodes.contains("de") && !configuredCodes.contains("en")) {
			configuredCodes.add("de");
		}

		String activateSql = String.format("UPDATE language SET active = ? WHERE short IN (%s)",
				StringUtils.repeat("?", ",", configuredCodes.size()));
		String deactivateSql = String.format("UPDATE language SET active = ? WHERE short NOT IN (%s)",
				StringUtils.repeat("?", ",", configuredCodes.size()));

		// activate languages to be actived and deactivate all others
		DBUtils.executeStatement(activateSql, Transaction.UPDATE_STATEMENT, ps -> {
			int counter = 1;
			ps.setBoolean(counter++, true);
			for (String code : configuredCodes) {
				ps.setString(counter++, code);
			}
		});
		DBUtils.executeStatement(deactivateSql, Transaction.UPDATE_STATEMENT, ps -> {
			int counter = 1;
			ps.setBoolean(counter++, false);
			for (String code : configuredCodes) {
				ps.setString(counter++, code);
			}
		});

		// fill the caches
		allLanguages = t.getObjects(UserLanguage.class,
				DBUtils.select("SELECT id FROM language WHERE short != ? ORDER BY id", ps -> {
					ps.setString(1, "");
				}, DBUtils.IDS));
		activeLanguages = t.getObjects(UserLanguage.class, DBUtils.select("SELECT id FROM language WHERE active = ? ORDER BY id", ps -> {
			ps.setBoolean(1, true);
		}, DBUtils.IDS));
	}

	/**
	 * Get the list of all existing languages
	 * @return list of all languages
	 */
	public static List<UserLanguage> getAll() {
		return allLanguages;
	}

	/**
	 * Get the list of currently active languages
	 * @return list of active languages
	 */
	public static List<UserLanguage> getActive() {
		if (ObjectTransformer.getBoolean(useAllLanguages.get(), false)) {
			return allLanguages;
		} else {
			return activeLanguages;
		}
	}

	/**
	 * Get the activated UserLanguage with given code or null, if not found (or not activated)
	 * @param code language code
	 * @return UserLanguage or null
	 */
	public static UserLanguage getByCode(String code) {
		return getByCode(code, false);
	}

	/**
	 * Get the activated UserLanguage with given code or the default or null, if not found (or not activated)
	 * @param code language code
	 * @param fallback true to do a fallback to the default language
	 * @return UserLanguage or null
	 */
	public static UserLanguage getByCode(String code, boolean fallback) {
		List<UserLanguage> list = getActive();
		UserLanguage def = fallback ? list.get(0) : null;
		return list.stream().filter(language -> StringUtils.equalsIgnoreCase(language.getCode(), code))
				.findFirst().orElse(def);
	}

	/**
	 * Get the activated UserLanguage with given id or null, if not found (or not activated)
	 * @param id language ID
	 * @return UserLanguage or null
	 */
	public static UserLanguage getById(int id) {
		return getById(id, false);
	}

	/**
	 * Get the activated UserLanguage with given id or the default or null, if not found (or not activated)
	 * @param id language ID
	 * @param fallback true to do a fallback to the default language
	 * @return UserLanguage or null
	 */
	public static UserLanguage getById(int id, boolean fallback) {
		List<UserLanguage> list = getActive();
		UserLanguage def = fallback ? list.get(0) : null;
		return list.stream().filter(language -> language.getId().intValue() == id).findFirst().orElse(def);
	}

	/**
	 * Implementation class
	 */
	protected static class UserLanguageImpl extends UserLanguage {
		private static final long serialVersionUID = -4847483187716621521L;

		private final String name;
		private final String code;
		private final String country;
		private final boolean isActive;

		private final transient Locale locale;

		protected UserLanguageImpl(Integer id, NodeObjectInfo info, String name, String code, String country, boolean isActive) {
			super(id, info);
			this.name = name;
			this.code = code;
			this.country = country;
			this.isActive = isActive;

			if (!StringUtils.isEmpty(code)) {
				this.locale = new Locale(code, ObjectTransformer.getString(this.country, ""));
			} else if (!isActive) {
				// the locale may be null if it isn't active,
				// e.g. the "Meta" language. 
				this.locale = null;
			} else {
				// this should never be.
				logger.error("empty language code for active language given: " + name + " {" + id + "}");
				// here it makes sense to recover the error with
				// Locale.getDefault()
				this.locale = Locale.getDefault();
			}
		}

		@Override
		public Locale getLocale() {
			return locale;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public String getCountry() {
			return country;
		}

		@Override
		public boolean isActive() {
			return isActive;
		}

		@Override
		public String getCode() {
			return code;
		}

		public NodeObject copy() throws NodeException {
			return new UserLanguageImpl(getId(), getObjectInfo(), name, code, country, isActive);
		}

		@Override
		public String toString() {
			return String.format("%s (code: %s, id: %d)", name, code, id);
		}
	}

	/**
	 * Implementation of {@link AutoCloseable} which will set the thread-local
	 * {@link UserLanguageFactory#useAllLanguages} flag in the constructor and will
	 * reset in {@link WithAllLanguages#close()}
	 */
	public static class WithAllLanguages implements AutoCloseable {
		/**
		 * Create an instance. Set the thread-local {@link UserLanguageFactory#useAllLanguages} flag
		 */
		public WithAllLanguages() {
			useAllLanguages.set(true);
		}

		@Override
		public void close() {
			useAllLanguages.set(false);
		}
	}

	/**
	 * Create an instance
	 */
	public UserLanguageFactory() {
		super();
	}

	@SuppressWarnings("unchecked")
	@Override
	protected <T extends NodeObject> T loadResultSet(Class<T> clazz, Integer id, NodeObjectInfo info,
			FactoryDataRow rs, List<Integer>[] idLists) throws SQLException, NodeException {
		String name = rs.getString("name");
		String code = rs.getString("short");
		String country = rs.getString("country");
		int active = rs.getInt("active");

		return (T) new UserLanguageImpl(id, info, name, code, country, active != 0);
	}

	@Override
	public <T extends NodeObject> Collection<T> batchLoadObjects(Class<T> clazz, Collection<Integer> ids, NodeObjectInfo info) throws NodeException {
		return batchLoadDbObjects(clazz, ids, info, "SELECT * FROM language WHERE id IN " + buildIdSql(ids));
	}

	@Override
	public <T extends NodeObject> T createObject(FactoryHandle handle, Class<T> clazz) throws NodeException {
		// A UserLanguage instance is currently read-only, so we can't create
		// an editable instance.
		return null;
	}

	@Override
	public <T extends NodeObject> T loadObject(Class<T> clazz, Integer id, NodeObjectInfo info) throws NodeException {
		return loadDbObject(clazz, id, info, "SELECT * FROM language WHERE id = ?", null, null);
	}
}
