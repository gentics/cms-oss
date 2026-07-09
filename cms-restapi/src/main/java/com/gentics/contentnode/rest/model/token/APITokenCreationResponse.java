package com.gentics.contentnode.rest.model.token;

import com.gentics.contentnode.rest.model.response.GenericResponse;

/**
 * Response containing the created token
 */
public class APITokenCreationResponse extends GenericResponse {
	private static final long serialVersionUID = -4651294677958860376L;

	private String token;

	private APITokenDataModel tokenData;

	/**
	 * Token
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
	public APITokenCreationResponse setToken(String token) {
		this.token = token;
		return this;
	}

	/**
	 * API Token data
	 * @return token data
	 */
	public APITokenDataModel getData() {
		return tokenData;
	}

	/**
	 * Set the token
	 * @param token token
	 * @return fluent API
	 */
	public APITokenCreationResponse setData(APITokenDataModel tokenData) {
		this.tokenData = tokenData;
		return this;
	}
}
