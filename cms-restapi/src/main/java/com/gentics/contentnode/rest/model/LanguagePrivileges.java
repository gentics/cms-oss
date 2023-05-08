package com.gentics.contentnode.rest.model;

import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Privileges set on a specific language
 */
@XmlRootElement
public class LanguagePrivileges {
	protected ContentLanguage language;

	protected Map<Privilege, Boolean> privileges;

	/**
	 * Create empty instance
	 */
	public LanguagePrivileges() {
	}

	/**
	 * Language for which the privileges are set
	 * @return language
	 */
	public ContentLanguage getLanguage() {
		return language;
	}

	/**
	 * Set the language
	 * @param language
	 */
	public void setLanguage(ContentLanguage language) {
		this.language = language;
	}

	/**
	 * Privileges for the language
	 * @return privilege map
	 */
	public Map<Privilege, Boolean> getPrivileges() {
		return privileges;
	}

	/**
	 * Set the privilege map
	 * @param privileges privilege map
	 */
	public void setPrivileges(Map<Privilege, Boolean> privileges) {
		this.privileges = privileges;
	}
}
