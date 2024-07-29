package com.gentics.contentnode.rest.model.staging;

import java.io.Serializable;
import java.util.List;

/**
 * Content staging package item.
 */
public interface ContentStagingPackageObject extends Serializable {

	/**
	 * Get list of errors related to this object.
	 * 
	 * @return
	 */
	public List<StagedError> getErrors();
}
