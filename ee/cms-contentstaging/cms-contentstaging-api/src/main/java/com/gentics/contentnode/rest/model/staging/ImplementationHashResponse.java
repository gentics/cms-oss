package com.gentics.contentnode.rest.model.staging;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.ResponseInfo;

/**
 * Response containing the implementation hash (and string representation on which the hash is based)
 * for an entity
 */
@XmlRootElement
public class ImplementationHashResponse extends GenericResponse {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -578103079654461316L;

	/**
	 * Hash
	 */
	private String hash;

	/**
	 * String representation
	 */
	private String base;

	/**
	 * Create instance with "ok" response info
	 */
	public ImplementationHashResponse() {
		setResponseInfo(ResponseInfo.ok(""));
	}

	/**
	 * Implementation hash
	 * @return hash
	 */
	public String getHash() {
		return hash;
	}

	/**
	 * Set the implementation hash
	 * @param hash hash
	 * @return fluent API
	 */
	public ImplementationHashResponse setHash(String hash) {
		this.hash = hash;
		return this;
	}

	/**
	 * Base of the hash (string representation of the entity)
	 * @return base
	 */
	public String getBase() {
		return base;
	}

	/**
	 * Set the base
	 * @param base base
	 * @return fluent API
	 */
	public ImplementationHashResponse setBase(String base) {
		this.base = base;
		return this;
	}
}
