package com.gentics.contentnode.rest.model.perm;

import java.util.Map;

/**
 * Role permissions
 */
public class RolePermissions {
	private Map<PermType, Boolean> page;

	private Map<String, Map <PermType, Boolean>> pageLanguages;

	private Map<PermType, Boolean> file;

	/**
	 * Role permissions for all pages
	 * @return permission map
	 */
	public Map<PermType, Boolean> getPage() {
		return page;
	}

	/**
	 * Set role permissions for all pages
	 * @param page permission map
	 */
	public void setPage(Map<PermType, Boolean> page) {
		this.page = page;
	}

	/**
	 * Language specific role permissions
	 * @return language map
	 */
	public Map<String, Map<PermType, Boolean>> getPageLanguages() {
		return pageLanguages;
	}

	/**
	 * Set language specific role permissions
	 * @param pageLanguages language map
	 */
	public void setPageLanguages(Map<String, Map<PermType, Boolean>> pageLanguages) {
		this.pageLanguages = pageLanguages;
	}

	/**
	 * Role permissions for files
	 * @return permission map
	 */
	public Map<PermType, Boolean> getFile() {
		return file;
	}

	/**
	 * Set role permissions for files
	 * @param file permission map
	 */
	public void setFile(Map<PermType, Boolean> file) {
		this.file = file;
	}
}
