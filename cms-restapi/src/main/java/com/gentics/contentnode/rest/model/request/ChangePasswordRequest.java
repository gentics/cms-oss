package com.gentics.contentnode.rest.model.request;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Request to change the password of the current user
 */
@XmlRootElement
public class ChangePasswordRequest {

	/**
	 * Current (old) plain text password
	 */
	private String oldPassword;

	/**
	 * New plain text password
	 */
	private String newPassword;

	/**
	 * Constructor for JAXB
	 */
	public ChangePasswordRequest() {}

	/**
	 * Get the current (old) plain text password, which must match the stored password
	 *
	 * @return the old password
	 */
	public String getOldPassword() {
		return oldPassword;
	}

	/**
	 * Set the current (old) plain text password
	 *
	 * @param oldPassword the old password
	 * @return fluent API
	 */
	public ChangePasswordRequest setOldPassword(String oldPassword) {
		this.oldPassword = oldPassword;
		return this;
	}

	/**
	 * Get the new plain text password
	 *
	 * @return the new password
	 */
	public String getNewPassword() {
		return newPassword;
	}

	/**
	 * Set the new plain text password
	 *
	 * @param newPassword the new password
	 * @return fluent API
	 */
	public ChangePasswordRequest setNewPassword(String newPassword) {
		this.newPassword = newPassword;
		return this;
	}
}
