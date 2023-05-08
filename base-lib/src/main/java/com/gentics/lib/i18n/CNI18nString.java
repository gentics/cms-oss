/*
 * @author norbert
 * @date 15.03.2007
 * @version $Id: CNI18nString.java,v 1.2.4.1 2011-02-10 13:43:41 tobiassteiner Exp $
 */
package com.gentics.lib.i18n;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.i18n.I18nString;
import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.log.RuntimeProfiler;
import com.gentics.lib.log.profilerconstants.ComponentsConstants;

/**
 * Implementation of an I18nString that uses the current transaction as language provider.
 * The form of the dynamic parameters in this implementation must be in the {0} notation, not in the $0 notation.
 */
public class CNI18nString extends I18nString {
    
	private static final long serialVersionUID = -3996995002921702480L;
    
	/**
	 * List of parameter
	 */
	protected List parameters = new LinkedList();
    
	/**
	 * Create a new instance with the given key
	 * @param key must conform to the syntax specified by {@link I18nString}.
	 *   Please note that content.node dic files will have lowercase keys,
	 *   (I think because do=24 spits them out that way) and that the provided
	 *   key should therefore also be lowercase.
	 */
	public CNI18nString(String key) {
		super(key, null);
	}

	/**
	 * Sets a list of parameters for this CN18nString.
	 * The parameters are read in the order they were put in the list and get numbers as names starting at 0.
	 * @param parameters The list with parmeter to set
	 */
	public void setParameters(List parameters) {
		if (parameters == null) {
			return;
		}
		int i = 0;

		for (Iterator it = parameters.iterator(); it.hasNext();) {
			Object object = it.next();

			setParameter(i + "", object);
			i++;
		}
	}
    
	/**
	 * Sets an array of parameters for this CN18nString.
	 * @param parameter The array with parmeters to set
	 */
	public void addParameters(String[] parameters) {
		for (int i = 0; i < parameters.length; i++) {
			addParameter(parameters[i]);            
		}
	}

	/**
	 * Adds a parameter
	 * @param parameter
	 */
	public void addParameter(String parameter) {
		if (parameter == null) {
			parameter = "";
		}
		parameters.add(parameter);
	}
    
	/**
	 * Sets a dynamic Paramaeter.<br>
	 * This implementation doesn't support named parameters and therefore the name will be ignored.<br>
	 */
	public void setParameter(String name, Object value) {
		parameters.add(value);
	}
    
	/**
	 * Returns a map of the set parameters.
	 * As keys you will get numbers starting from zero in the order you added the parameters. 
	 */
	public Map getParameters() {
		HashMap paramMap = new HashMap();
		int i = 0;

		for (Iterator it = parameters.iterator(); it.hasNext();) {
			String parameter = (String) it.next();

			paramMap.put(new Integer(i), parameter);
			i++;
		}
		return paramMap;
	}
    
	/**
	 * Get the translated, parameterized String.
	 */
	public String toString() {
		try {
			RuntimeProfiler.beginMark(ComponentsConstants.I18NSTRING_TOSTRING);
            
			if (_key == null) {
				// key was null, generate a warning and return empty string (no invalid output will be shown in templates)
				NodeLogger.getLogger(getClass()).warn("invalid i18nstring, key was null.");
				return "";
			}
            
			LanguageProvider languageProvider = getLanguageProvider();

			if (languageProvider != null) {
				if (languageProvider.getLanguage() == null) {
					NodeLogger.getLogger(getClass()).error("error translating i18nstring '" + _key + "', language from languageprovider was null");
					return _key;
				}

				Properties properties = languageProvider.getLanguage().getDic();
				String entry = properties.getProperty(_key, _key);
                
				// if the string has no parameters, return it unchanged, since these are user-defined strings. 
				if (ObjectTransformer.isEmpty(parameters)) {
					return entry;
				}
                
				try {
					entry = entry.replaceAll("'", "''");
					return MessageFormat.format(entry, parameters.toArray());
				} catch (IllegalArgumentException e) {
					NodeLogger.getLogger(getClass()).error("Invalid parameters for the String {" + entry + "}", e);
					return entry;
				}
                
			} else {
				NodeLogger.getLogger(getClass()).error("invalid i18nstring, languagecontainer was null.");
			}
            
			return _key;
		} finally {
			RuntimeProfiler.endMark(ComponentsConstants.I18NSTRING_TOSTRING);
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.i18n.I18nString#getLanguageProvider()
	 */
	@Override
	protected LanguageProvider getLanguageProvider() {
		try {
			return LanguageProviderFactory.getInstance().getProvider();
		} catch (NodeException e) {
			return null;
		}
	}
}