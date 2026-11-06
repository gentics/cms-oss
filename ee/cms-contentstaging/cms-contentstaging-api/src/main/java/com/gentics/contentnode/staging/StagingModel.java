package com.gentics.contentnode.staging;

import java.io.Serializable;
import java.util.Optional;

import jakarta.xml.bind.annotation.XmlTransient;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * A model can be stored in the content staging storage.
 * 
 * @author plyhun
 *
 */
public interface StagingModel extends Serializable {

	/**
	 * Get the global ID of this stageable.
	 * 
	 * @return
	 */
	String getId();

	/**
	 * Get the stageable name for this model type.
	 * 
	 * @return
	 */
	String getTypeName();

	/**
	 * Set the global ID
	 * 
	 * @param string
	 */
	void setId(String string);

	/**
	 * Whether this model considers a binary data along.
	 * 
	 * @return
	 */
	@XmlTransient
	@JsonIgnore
    default boolean hasBinary() {
		return false;
	}

	/**
	 * If this model considers versioning (current/published/scheduled) content, return its version data.
	 * 
	 * @return
	 */
	@XmlTransient
	@JsonIgnore
    default Optional<StagedObjectVersion> maybeVersioned() {
		return Optional.empty();
	}
}