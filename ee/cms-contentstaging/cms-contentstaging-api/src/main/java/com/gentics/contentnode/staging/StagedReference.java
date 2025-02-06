package com.gentics.contentnode.staging;

import java.io.Serializable;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A Global ID + Hash pair
 * 
 * @author plyhun
 *
 */
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class StagedReference implements Serializable {
	/**
	 * Serial Version UUID
	 */
	private static final long serialVersionUID = -6103765832729102197L;

	/**
	 * Global ID of the reference
	 */
	private String id;

	/**
	 * Hash
	 */
	private String hash;

	/**
	 * ID of the reference
	 * @return ID
	 */
	public String getId() {
		return id;
	}

	/**
	 * Set the global ID
	 * @param id global ID
	 * @return fluent API
	 */
	public StagedReference setId(String id) {
		this.id = id;
		return this;
	}

	/**
	 * Hash of the reference
	 * @return hash
	 */
	public String getHash() {
		return hash;
	}

	/**
	 * Set the hash
	 * @param hash hash
	 * @return fluent API
	 */
	public StagedReference setHash(String hash) {
		this.hash = hash;
		return this;
	}

	@Override
	public String toString() {
		return "StagedReference [id=" + id + ", hash=" + hash + "]";
	}
}
