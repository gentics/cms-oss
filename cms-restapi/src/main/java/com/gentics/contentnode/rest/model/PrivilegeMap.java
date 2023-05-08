package com.gentics.contentnode.rest.model;

import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.perm.PermissionsMap;

/**
 * Privilege Map containing privilege information (in general and language specific)
 * @deprecated use {@link PermissionsMap} instead
 */
@XmlRootElement
public class PrivilegeMap {
	protected Map<Privilege, Boolean> privileges;

	protected List<LanguagePrivileges> languages;

	public PrivilegeMap() {
	}

	/**
	 * Privileges not specific to languages
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

	/**
	 * Language specific privileges
	 * @return list of languages and privileges
	 */
	public List<LanguagePrivileges> getLanguages() {
		return languages;
	}

	/**
	 * Set the language privileges
	 * @param languages list of languages and privileges
	 */
	public void setLanguages(List<LanguagePrivileges> languages) {
		this.languages = languages;
	}
}
