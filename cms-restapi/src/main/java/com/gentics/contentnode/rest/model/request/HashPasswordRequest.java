package com.gentics.contentnode.rest.model.request;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Hash password request
 */
@XmlRootElement
public class HashPasswordRequest {

	/**
	 * Plain text Password
	 */
	private String password;

	/**
	 * Id of the user the password belongs to (can be null)
	 */
	private int userId;


	/**
	 * Constructor for JAXB
	 */
	public HashPasswordRequest() {}

	/**
	 * Get the password to hash (plain-text)
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
	 * Get the user ID
	 *
	 * @return the user ID
	 */
	public int getUserId() {
		return this.userId;
	}

	/**
	 * @param value the user ID
	 */
	public void setUserId(int value) {
		this.userId = value;
	}
}
