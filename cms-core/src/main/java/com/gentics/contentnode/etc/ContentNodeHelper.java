/*
 * @author norbert
 * @date 13.03.2007
 * @version $Id: ContentNodeHelper.java,v 1.5.2.1 2011-02-10 13:43:42 tobiassteiner Exp $
 */
package com.gentics.contentnode.etc;

import java.util.Locale;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.UserLanguage;

/**
 * Static Helper class for Content.Node specific implementations
 */
public class ContentNodeHelper {
	/**
	 * Thread local settings
	 */
	protected final static ThreadLocal<ThreadLocalSettings> localSettings = ThreadLocal.withInitial(() -> new ThreadLocalSettings());

	/**
	 * Get the local settings object (create a new one if none existent)
	 * @return local settings object
	 */
	protected static ThreadLocalSettings getSettings() {
		return localSettings.get();
	}

	/**
	 * Set language id
	 * @param languageId language id
	 */
	public static void setLanguageId(int languageId) {
		getSettings().setLanguageId(languageId);
	}

	/**
	 * Get the current language id
	 * @return current language id
	 * @throws NodeException if the language id not set
	 */
	public static int getLanguageId() throws NodeException {
		int languageId = getSettings().getLanguageId();

		if (languageId < 0) {
			throw new NodeException("languageid not set");
		} else {
			return languageId;
		}
	}

	/**
	 * Get the current language id or the default, if current is set to -1
	 * @param defaultLanguageId default language id
	 * @return current or default language id
	 */
	public static int getLanguageId(int defaultLanguageId) {
		int languageId = getSettings().getLanguageId();
		if (languageId < 0) {
			return defaultLanguageId;
		} else {
			return languageId;
		}
	}

	/**
	 * The language ID returned by {@link #getLanguageId()} isn't very useful
	 * by itself, since it is merely a reference to a {@link UserLanguage}
	 * instance.
	 * @return the {@link Locale} of the {@link UserLanguage} for the given ID.
	 */
	public static Locale getLocaleForLanguageId(int languageId, Transaction transaction) throws NodeException {
		//Fallback to standard language
		if (languageId < 1 || languageId > 2) {
			languageId = 1;
		}
		UserLanguage userLang = (UserLanguage) transaction.getObject(UserLanguage.class, languageId);

		return userLang.getLocale();
	}

	/**
	 * Threadlocal settings (useragent, language, ...)
	 */
	private static class ThreadLocalSettings {
		/**
		 * language of the current request
		 */
		private int languageId = -1;

		private Session session;

		/**
		 * Get the current language id
		 * @return current language id
		 */
		public int getLanguageId() {
			return languageId;
		}

		/**
		 * Set a new language id
		 * @param languageId new language id
		 */
		public void setLanguageId(int languageId) {
			this.languageId = languageId;
		}

		public Session getSession() {
			return session;
		}

		public void setSession(Session session) {
			this.session = session;
			if (session != null) {
				this.languageId = session.getLanguageId();
			}
		}

		/**
		 * Create an instance of the settings
		 */
		public ThreadLocalSettings() {}
	}

	/**
	 * Get the current session
	 * @return session
	 */
	public static Session getSession() {
		return getSettings().getSession();
	}

	/**
	 * Set the current session
	 * @param session session
	 */
	public static void setSession(Session session) {
		getSettings().setSession(session);
		if (session != null) {
			setLanguageId(session.getLanguageId());
		}
	}

	/**
	 * Create a transaction with the current session
	 * @return transaction with current session
	 * @throws NodeException
	 */
	public static Trx trx() throws NodeException {
		Session session = getSession();
		if (session != null) {
			return new Trx(session, true);
		} else {
			return new Trx();
		}
	}

	/**
	 * Create a transaction with the given session (which may be null)
	 * @param session session
	 * @return transaction with session
	 * @throws NodeException
	 */
	public static Trx trx(Session session) throws NodeException {
		if (session != null) {
			return new Trx(session, true);
		} else {
			return new Trx();
		}
	}
}
