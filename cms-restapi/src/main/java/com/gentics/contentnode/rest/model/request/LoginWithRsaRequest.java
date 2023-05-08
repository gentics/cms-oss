package com.gentics.contentnode.rest.model.request;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Login Request containing the user credentials
 */
@XmlRootElement
public class LoginWithRsaRequest {
	/**
	 * User name
	 */
	protected String username;

	/**
	 * A randomly generated salt
	 */
	protected String salt;

	/**
	 * The unix timestamp until this RSA token is valid
	 */
	protected int expirationTimestamp;

	/**
	 * RSA signature that confirms the validity of the login
	 */
	protected String rsaSignature;


	/**
	 * Create an empty instance
	 */
	public LoginWithRsaRequest() {}

	/**
	 * Get the user name
	 * @return user name
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * Set the username
	 * @param username The user name
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * @return the salt
	 */
	public String getSalt() {
		return salt;
	}

	/**
	 * @param salt the salt to set
	 */
	public void setSalt(String salt) {
		this.salt = salt;
	}

	/**
	 * @return the expirationTimestamp
	 */
	public int getExpirationTimestamp() {
		return expirationTimestamp;
	}

	/**
	 * @param expirationTimestamp the expirationTimestamp to set
	 */
	public void setExpirationTimestamp(int expirationTimestamp) {
		this.expirationTimestamp = expirationTimestamp;
	}

	/**
	 * @return the rsaSignature
	 */
	public String getRsaSignature() {
		return rsaSignature;
	}

	/**
	 * @param rsaSignature the rsaSignature to set
	 */
	public void setRsaSignature(String rsaSignature) {
		this.rsaSignature = rsaSignature;
	}
}
