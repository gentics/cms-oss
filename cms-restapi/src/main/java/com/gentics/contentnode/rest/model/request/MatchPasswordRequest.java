package com.gentics.contentnode.rest.model.request;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Match password request
 */
@XmlRootElement
public class MatchPasswordRequest {

	/**
	 * Plain text password
	 */
	private String password;

	/**
	 * Password hash
	 */
	private String hash;

	/**
	 * Constructor for JAXB
	 */
	public MatchPasswordRequest() {}

	/**
	 * Get the plain text password
	 *
	 * @return the password
	 */
	public String getPassword() {
		return this.password;
	}

	/**
	 * @param value the password
	 */
	public void setPassword(String value) {
		this.password = value;
	}

	/**
	 * Get the password hash
	 *
	 * @return the hash
	 */
	public String getHash() {
		return this.hash;
	}

	/**
	 * @param value the password hash
	 */
	public void setHash(String value) {
		this.hash = value;
	}
}
