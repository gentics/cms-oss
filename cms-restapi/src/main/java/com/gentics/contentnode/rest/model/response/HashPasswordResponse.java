package com.gentics.contentnode.rest.model.response;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Response to a login request
 */
@XmlRootElement
public class HashPasswordResponse extends AuthenticationResponse {

	/**
	 * Password hash
	 */
	private String hash;

	/**
	 * Create an instance
	 */
	public HashPasswordResponse() {}

	/**
	 * Get the password hash
	 * @return password hash
	 */
	public String getHash() {
		return hash;
	}

	/**
	 * Set the password hash
	 * @param value password hash
	 */
	public void setHash(String value) {
		this.hash = value;
	}
}
