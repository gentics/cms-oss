package com.gentics.contentnode.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Model for file privileges
 */
public class FilePrivileges {
	protected Boolean viewfile;

	protected Boolean createfile;

	protected Boolean updatefile;

	protected Boolean deletefile;

	/**
	 * True when viewfile is granted
	 * @return viewfile flag
	 */
	public Boolean getViewfile() {
		return viewfile;
	}

	/**
	 * Set viewfile flag
	 * @param viewfile flag
	 * @return fluent API
	 */
	public FilePrivileges setViewfile(Boolean viewfile) {
		this.viewfile = viewfile;
		return this;
	}

	/**
	 * True when createfile is granted
	 * @return createfile flag
	 */
	public Boolean getCreatefile() {
		return createfile;
	}

	/**
	 * Set createfile flag
	 * @param createfile flag
	 * @return fluent API
	 */
	public FilePrivileges setCreatefile(Boolean createfile) {
		this.createfile = createfile;
		return this;
	}

	/**
	 * True when updatefile is granted
	 * @return updatefile flag
	 */
	public Boolean getUpdatefile() {
		return updatefile;
	}

	/**
	 * Set updatefile flag
	 * @param updatefile flag
	 * @return fluent API
	 */
	public FilePrivileges setUpdatefile(Boolean updatefile) {
		this.updatefile = updatefile;
		return this;
	}

	/**
	 * True when deletefile is granted
	 * @return deletefile flag
	 */
	public Boolean getDeletefile() {
		return deletefile;
	}

	/**
	 * Set deletefile flag
	 * @param deletefile flag
	 * @return fluent API
	 */
	public FilePrivileges setDeletefile(Boolean deletefile) {
		this.deletefile = deletefile;
		return this;
	}

	@JsonIgnore
	public Boolean get(Privilege privilege) {
		switch (privilege) {
		case createfile:
			return getCreatefile();
		case deletefile:
			return getDeletefile();
		case updatefile:
			return getUpdatefile();
		case viewfile:
			return getViewfile();
		default:
			return false;
		}
	}

	@JsonIgnore
	public FilePrivileges set(Privilege privilege, Boolean flag) {
		switch (privilege) {
		case createfile:
			return setCreatefile(flag);
		case deletefile:
			return setDeletefile(flag);
		case updatefile:
			return setUpdatefile(flag);
		case viewfile:
			return setViewfile(flag);
		default:
			return this;
		}
	}
}
