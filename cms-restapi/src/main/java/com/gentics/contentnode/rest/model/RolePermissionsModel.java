package com.gentics.contentnode.rest.model;

import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Model for role permissions
 */
@XmlRootElement
public class RolePermissionsModel {
	protected PagePrivileges page;

	protected Map<Integer, PagePrivileges> pageLanguages;

	protected FilePrivileges file;

	/**
	 * Global page permissions
	 * @return page permissions
	 */
	public PagePrivileges getPage() {
		return page;
	}

	/**
	 * Set global page permissions
	 * @param page page permissions
	 * @return fluent API
	 */
	public RolePermissionsModel setPage(PagePrivileges page) {
		this.page = page;
		return this;
	}

	/**
	 * Language specific page permissions
	 * @return map of page permissions per language
	 */
	public Map<Integer, PagePrivileges> getPageLanguages() {
		return pageLanguages;
	}

	/**
	 * Set language specific page permissions
	 * @param pageLanguages map of page permissions per language
	 * @return fluent API
	 */
	public RolePermissionsModel setPageLanguages(Map<Integer, PagePrivileges> pageLanguages) {
		this.pageLanguages = pageLanguages;
		return this;
	}

	/**
	 * File permissions
	 * @return file permissions
	 */
	public FilePrivileges getFile() {
		return file;
	}

	/**
	 * Set file permissions
	 * @param file file permissions
	 * @return fluent API
	 */
	public RolePermissionsModel setFile(FilePrivileges file) {
		this.file = file;
		return this;
	}
}
