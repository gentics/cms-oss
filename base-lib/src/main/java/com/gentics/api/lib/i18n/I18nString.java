/*
 * @author laurin
 * @date 06.04.2005
 * @version $Id: I18nString.java,v 1.10 2009-12-16 16:12:07 herbert Exp $
 * @gentics.sdk
 */
package com.gentics.api.lib.i18n;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.i18n.LanguageProvider;
import com.gentics.lib.i18n.LanguageProviderFactory;
import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.log.RuntimeProfiler;
import com.gentics.lib.log.profilerconstants.ComponentsConstants;

/**
 * Localized string. Is localized automaticaly into a string during rendering by
 * .toString(), usually in template engine.
 */
public abstract class I18nString implements Serializable {

	/**
	 * key of the i18String
	 */
	protected String _key;

	/**
	 * language provider (for translating the key depending on the currently
	 * selected language)
	 */
	protected transient LanguageProvider languageProvider;

	/**
	 * map of variables, may be null when no variables are set
	 */
	private Map variablesMap;

	/**
	 * Create an instance of the I18nString. Instances are always created inside
	 * the portal, so this constructor is protected.
	 * @param key the key of the i18n item.
	 * @param languageProvider the languageprovider to use, for fetching the
	 *        language during toString.
	 */
	protected I18nString(String key, LanguageProvider languageProvider) {
		this.languageProvider = languageProvider;
		if (key == null) {
			_key = null;
			NodeLogger.getLogger(getClass()).warn("created I18nString with null-key");
		} else {
			_key = key;
		}
	}

	/**
	 * Method to get the current language provider
	 * @return languageprovider
	 */
	protected LanguageProvider getLanguageProvider() {
		// when no languageprofiver was set (maybe after the object was restored
		// from a serialized session), we set the current portal
		if (languageProvider == null) {
			try {
				languageProvider = LanguageProviderFactory.getInstance().getProvider();
			} catch (NodeException e) {
				NodeLogger.getLogger(getClass()).error("Error translating i18nstring '" + _key + "', languageprovider was null");
			}
		}
		return languageProvider;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		try {
			RuntimeProfiler.beginMark(ComponentsConstants.I18NSTRING_TOSTRING);
			if (_key != null) {
				LanguageProvider languageProvider = getLanguageProvider();

				if (languageProvider != null) {
					if (languageProvider.getLanguage() == null) {
						NodeLogger.getLogger(getClass()).error("error translating i18nstring '" + _key + "', language from languageprovider was null");
						return _key;
					}
					String entry = languageProvider.getLanguage().getDic().getProperty(_key, _key);

					// check the dictionary entry for $ (variables)
					if (entry.indexOf('$') < 0 || variablesMap == null) {
						// entry does not contain any variables or no variables
						// where set -> return it without modification
						return entry;
					} else {
						// entry contains variables -> fill them in
						for (Iterator i = variablesMap.entrySet().iterator(); i.hasNext();) {
							Map.Entry mapEntry = (Map.Entry) i.next();

							entry = entry.replaceAll(mapEntry.getKey().toString(), mapEntry.getValue().toString());
						}
						return entry;
					}
				} else {
					NodeLogger.getLogger(getClass()).error("invalid i18nstring, languagecontainer was null.");
				}
				return _key;
			} else {
				// key was null, generate a warning and return empty string (no invalid output will be shown in templates)
				NodeLogger.getLogger(getClass()).warn("invalid i18nstring, key was null.");
				return "";
			}
		} finally {
			RuntimeProfiler.endMark(ComponentsConstants.I18NSTRING_TOSTRING);
		}
	}

	/**
	 * Set all parameters from the map to the string
	 * @param parameters parameters as map
	 */
	public void setParameters(Map parameters) {
		if (parameters != null) {
			for (Iterator iter = parameters.entrySet().iterator(); iter.hasNext();) {
				Map.Entry element = (Map.Entry) iter.next();

				if (element.getKey() != null) {
					setParameter(element.getKey().toString(), element.getValue());
				}
			}
		}
	}

	/**
	 * sets a variable parameter in the string.
	 * @param name name to replace in template, e.g. "name" for replacing $name
	 * @param value new value for variable. may also be an object with a
	 *        toString implementation.
	 */
	public void setParameter(String name, Object value) {
		if (name == null || name.length() == 0) {
			// when name is not set, we do nothing
			return;
		}

		if (variablesMap == null) {
			// when the variables map does not exist, create it
			variablesMap = new I18nStringParameters();
		}

		if (value == null) {
			// setting null means removing the key
			variablesMap.remove(name);
		} else {
			variablesMap.put(name, value);
		}
	}

	/**
	 * Get the length of the I18nString
	 * @return length
	 */
	public int length() {
		return toString().length();
	}

	/**
	 * Get the parameters map
	 * @return parameters map
	 */
	public Map getParameters() {
		if (variablesMap == null) {
			variablesMap = new I18nStringParameters();
		}
		return variablesMap;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (obj instanceof I18nString) {
			I18nString iObj = (I18nString) obj;

			// TODO check also for properties map
			return StringUtils.isEqual(_key, iObj._key);
		} else if (obj instanceof String) {
			return StringUtils.isEqual(_key, (String) obj);
		} else {
			return false;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return _key.hashCode();
	}

	/**
	 * Internal map implementation that transforms keys to the regexes.
	 */
	public final class I18nStringParameters extends HashMap {

		/**
		 * serial version uid
		 */
		private static final long serialVersionUID = -8909547288333872330L;

		/**
		 * Transform the key into the internal representation
		 * @param key original key
		 * @return transformed key
		 */
		private Object transformKey(Object key) {
			if (!(key instanceof String)) {
				return key;
			} else {
				// prepare the key to be the replace pattern (starting with $
				// and ending
				// with a word boundary)
				return "\\$" + key.toString() + "\\b";
			}
		}

		/* (non-Javadoc)
		 * @see java.util.Map#containsKey(java.lang.Object)
		 */
		public boolean containsKey(Object key) {
			return super.containsKey(transformKey(key));
		}

		/* (non-Javadoc)
		 * @see java.util.Map#get(java.lang.Object)
		 */
		public Object get(Object key) {
			return super.get(transformKey(key));
		}

		/* (non-Javadoc)
		 * @see java.util.Map#put(java.lang.Object, java.lang.Object)
		 */
		public Object put(Object key, Object value) {
			return super.put(transformKey(key), StringUtils.quoteReplacement(ObjectTransformer.getString(value, "")));
		}

		/* (non-Javadoc)
		 * @see java.util.Map#containsValue(java.lang.Object)
		 */
		public boolean containsValue(Object value) {
			return super.containsValue(StringUtils.quoteReplacement(ObjectTransformer.getString(value, "")));
		}
	}
}
