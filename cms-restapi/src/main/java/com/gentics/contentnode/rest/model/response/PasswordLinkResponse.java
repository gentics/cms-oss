package com.gentics.contentnode.rest.model.response;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Response containing a new token for the link, with which a user sets their password
 */
@XmlRootElement
public class PasswordLinkResponse extends GenericResponse {

	/**
	 * Token for the password link
	 */
	private String token;

	/**
	 * Expiry of the token as Unix timestamp in seconds
	 */
	private Integer expiresAt;

	/**
	 * Create an empty instance
	 */
	public PasswordLinkResponse() {}

	/**
	 * Create instance with message and response info
	 * @param message message
	 * @param responseInfo response info
	 */
	public PasswordLinkResponse(Message message, ResponseInfo responseInfo) {
		super(message, responseInfo);
	}

	/**
	 * Token for {@code POST /auth/password}, valid until {@link #getExpiresAt()} and usable once
	 * @return token
	 */
	public String getToken() {
		return token;
	}

	/**
	 * Set the token
	 * @param token token
	 * @return fluent API
	 */
	public PasswordLinkResponse setToken(String token) {
		this.token = token;
		return this;
	}

	/**
	 * Expiry of the token as Unix timestamp in seconds
	 * @return expiry
	 */
	public Integer getExpiresAt() {
		return expiresAt;
	}

	/**
	 * Set the expiry
	 * @param expiresAt Unix timestamp in seconds
	 * @return fluent API
	 */
	public PasswordLinkResponse setExpiresAt(Integer expiresAt) {
		this.expiresAt = expiresAt;
		return this;
	}
}
