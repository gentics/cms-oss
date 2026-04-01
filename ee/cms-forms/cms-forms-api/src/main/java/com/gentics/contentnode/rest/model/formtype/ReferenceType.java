package com.gentics.contentnode.rest.model.formtype;

import java.io.Serializable;

/**
 * Referenced types
 */
public enum ReferenceType implements Serializable {
	/**
	 * Reference to a CMS folder
	 */
	folder,

	/**
	 * Reference to a CMS page
	 */
	page,

	/**
	 * Reference to a CMS file
	 */
	file,

	/**
	 * Reference to a CMS image
	 */
	image;
}
