package com.gentics.contentnode.rest.model.migration;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class TemplateMigrationTagMapping implements Serializable {

	private static final long serialVersionUID = -2171306855580220371L;

	private Integer fromTagId;
	private Integer toTagId;

	/**
	 * Returns the source or from template tag for this mapping
	 * 
	 * @return
	 */
	public Integer getFromTagId() {
		return fromTagId;
	}

	/**
	 * Sets the source or from template tag for this mapping
	 * 
	 * @param fromTag
	 */
	public void setFromTagId(Integer fromTagId) {
		this.fromTagId = fromTagId;
	}

	/**
	 * Returns the target or to template tag for this mapping
	 * 
	 * @return
	 */
	public Integer getToTagId() {
		return toTagId;
	}

	/**
	 * Sets the target or to template tag for this mapping
	 * 
	 * @param toTag
	 */
	public void setToTagId(Integer toTagId) {
		this.toTagId = toTagId;
	}

	/**
	 * Checks whether this mapping was marked as not mapped. This is done by setting the toTagId to -1
	 * 
	 * @return
	 */
	@JsonIgnore
	public boolean isMarkedAsNotMapped() {
		return toTagId == -1;
	}
}
