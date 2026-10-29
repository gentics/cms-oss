package com.gentics.contentnode.staging;

import jakarta.xml.bind.annotation.XmlTransient;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * A model can be stored in the content staging storage.
 * 
 * @author plyhun
 *
 */
public interface StoredModel extends StagingModel {

	/**
	 * Get the stageable name suffix for this model type.
	 * 
	 * @return
	 */
	@XmlTransient
	@JsonIgnore
    default String getSuffix() {
		return "." + getTypeName();
	}

	/**
	 * Make this model independent of any foreign references, to raise its chance for an import.
	 */
	default void makeSafe() {}
}
