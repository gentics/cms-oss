package com.gentics.contentnode.rest.model.token;

import java.io.Serializable;

/**
 * Request to create a new API Token for the current user
 */
public class ApiTokenCreationRequest implements Serializable {
	private static final long serialVersionUID = 5883346357733120180L;

	protected String name;

	protected int expires;

	/**
	 * Token name
	 * @return name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the token name
	 * @param name name
	 * @return fluent API
	 */
	public ApiTokenCreationRequest setName(String name) {
		this.name = name;
		return this;
	}

	/**
	 * Token expiry date (or 0 if the token shall not expire)
	 * @return expiry date
	 */
	public int getExpires() {
		return expires;
	}

	/**
	 * Set the expiry date
	 * @param expires expiry date
	 * @return fluent API
	 */
	public ApiTokenCreationRequest setExpires(int expires) {
		this.expires = expires;
		return this;
	}
}
