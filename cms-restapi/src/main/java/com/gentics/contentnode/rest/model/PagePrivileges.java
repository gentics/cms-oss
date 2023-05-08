package com.gentics.contentnode.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Model for page privileges
 */
public class PagePrivileges {
	protected Boolean viewpage;

	protected Boolean createpage;

	protected Boolean updatepage;

	protected Boolean deletepage;

	protected Boolean publishpage;

	protected Boolean translatepage;

	/**
	 * True if viewpage is granted
	 * @return viewpage flag
	 */
	public Boolean getViewpage() {
		return viewpage;
	}

	/**
	 * Set viewpage flag
	 * @param viewpage flag
	 * @return fluent API
	 */
	public PagePrivileges setViewpage(Boolean viewpage) {
		this.viewpage = viewpage;
		return this;
	}

	/**
	 * True when createpage is granted
	 * @return createpage flag
	 */
	public Boolean getCreatepage() {
		return createpage;
	}

	/**
	 * Set createpage flag
	 * @param createpage flag
	 * @return fluent API
	 */
	public PagePrivileges setCreatepage(Boolean createpage) {
		this.createpage = createpage;
		return this;
	}

	/**
	 * True when updatepage is granted
	 * @return updatepage flag
	 */
	public Boolean getUpdatepage() {
		return updatepage;
	}

	/**
	 * Set updatepage flag
	 * @param updatepage flag
	 * @return fluent API
	 */
	public PagePrivileges setUpdatepage(Boolean updatepage) {
		this.updatepage = updatepage;
		return this;
	}

	/**
	 * True when deletepage is granted
	 * @return deletepage flag
	 */
	public Boolean getDeletepage() {
		return deletepage;
	}

	/**
	 * Set deletepage flag
	 * @param deletepage flag
	 * @return fluent API
	 */
	public PagePrivileges setDeletepage(Boolean deletepage) {
		this.deletepage = deletepage;
		return this;
	}

	/**
	 * True when publishpage is granted
	 * @return publishpage flag
	 */
	public Boolean getPublishpage() {
		return publishpage;
	}

	/**
	 * Set publishpage flag
	 * @param publishpage flag
	 * @return fluent API
	 */
	public PagePrivileges setPublishpage(Boolean publishpage) {
		this.publishpage = publishpage;
		return this;
	}

	/**
	 * True when translatepage is granted
	 * @return translatepage flag
	 */
	public Boolean getTranslatepage() {
		return translatepage;
	}

	/**
	 * Set translatepage flag
	 * @param translatepage flag
	 * @return fluent API
	 */
	public PagePrivileges setTranslatepage(Boolean translatepage) {
		this.translatepage = translatepage;
		return this;
	}

	@JsonIgnore
	public Boolean get(Privilege privilege) {
		switch (privilege) {
		case createpage:
			return getCreatepage();
		case deletepage:
			return getDeletepage();
		case publishpage:
			return getPublishpage();
		case translatepage:
			return getTranslatepage();
		case updatepage:
			return getUpdatepage();
		case viewpage:
			return getViewpage();
		default:
			return false;
		}
	}

	@JsonIgnore
	public PagePrivileges set(Privilege privilege, Boolean flag) {
		switch (privilege) {
		case createpage:
			return setCreatepage(flag);
		case deletepage:
			return setDeletepage(flag);
		case publishpage:
			return setPublishpage(flag);
		case translatepage:
			return setTranslatepage(flag);
		case updatepage:
			return setUpdatepage(flag);
		case viewpage:
			return setViewpage(flag);
		default:
			return this;
		}
	}
}
