package com.gentics.contentnode.staging;

import jakarta.xml.bind.annotation.XmlTransient;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Base class for the content staging related entities.
 * 
 * @author plyhun
 *
 */
public abstract class StagingBase implements StagingModel {

	private static final long serialVersionUID = 4347603228018799868L;

	/**
	 * global ID of the item
	 */
	private String id;

	/**
	 * Name of the item
	 */
	private String name;

	public String getId() {
		return id;
	}

	public void setId(String globalId) {
		this.id = globalId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Clone the origin into this base.
	 * @param origin
	 */
	@XmlTransient
	@JsonIgnore
    public void clone(StagingBase origin) {
		if (origin.id != null && !origin.id.trim().equals("")) {
			this.id = origin.id;
		}
		if (origin.name != null && !origin.name.trim().equals("")) {
			this.name = origin.name;
		}
	}
}
