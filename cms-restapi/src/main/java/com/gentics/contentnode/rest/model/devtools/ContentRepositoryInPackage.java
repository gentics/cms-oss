package com.gentics.contentnode.rest.model.devtools;

import javax.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.ContentRepositoryModel;

/**
 * Content Repository in a package.<br>
 * If the object does not have a (local) ID, this means that the object was not synchronized to the CMS.<br>
 * It is also possible, that the global ID of the object is null, if it is not set in the <code>gentics_structure.json</code> file.<br>
 * Objects without global ID in the filesystem can be imported into the CMS, but will never be "in sync", because they cannot be identified without global ID.
 */
@XmlRootElement
public class ContentRepositoryInPackage extends ContentRepositoryModel {
	/**
	 * Serial Version UUID
	 */
	private static final long serialVersionUID = -6176260086927445805L;

	protected String packageName;

	/**
	 * Name of the package that contains the object
	 * @return package name
	 */
	public String getPackageName() {
		return packageName;
	}

	/**
	 * Set the package name
	 * @param packageName package name
	 */
	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}
}
