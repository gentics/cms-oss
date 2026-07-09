package com.gentics.contentnode.rest.model.token;

import java.io.Serializable;

/**
 * Model for an API Token
 */
public class APITokenDataModel implements Serializable {
	private static final long serialVersionUID = 6322580482053154169L;

	/**
	 * Token ID
	 */
	private int id;

	/**
	 * Token name
	 */
	private String name;

	/**
	 * Creation date
	 */
	private int cdate;

	/**
	 * Expiry data (0 when the token does not expire)
	 */
	private int expires;

	/**
	 * Time when the token was used the last time (0 when it was never used)
	 */
	private int lastUsed;

	/**
	 * Flag to mark valid tokens
	 */
	private boolean valid;

	/**
	 * ID of the token
	 * @return ID
	 */
	public int getId() {
		return id;
	}

	/**
	 * Set the ID
	 * @param id ID
	 * @return fluent API
	 */
	public APITokenDataModel setId(int id) {
		this.id = id;
		return this;
	}

	/**
	 * Token name
	 * @return name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the name
	 * @param name name
	 * @return fluent API
	 */
	public APITokenDataModel setName(String name) {
		this.name = name;
		return this;
	}

	/**
	 * Creation date
	 * @return creation date
	 */
	public int getCdate() {
		return cdate;
	}

	/**
	 * Set the creation date
	 * @param cdate creation date
	 * @return fluent API
	 */
	public APITokenDataModel setCdate(int cdate) {
		this.cdate = cdate;
		return this;
	}

	/**
	 * Expiry date
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
	public APITokenDataModel setExpires(int expires) {
		this.expires = expires;
		return this;
	}

	/**
	 * Date of the last usage
	 * @return last usage date
	 */
	public int getLastUsed() {
		return lastUsed;
	}

	/**
	 * Set the last usage date
	 * @param lastUsed date
	 * @return fluent API
	 */
	public APITokenDataModel setLastUsed(int lastUsed) {
		this.lastUsed = lastUsed;
		return this;
	}

	/**
	 * True when the token is still valid (not expired), false when the token is expired
	 * @return flag
	 */
	public boolean isValid() {
		return valid;
	}

	/**
	 * Set the "valid" flag
	 * @param valid flag
	 * @return fluent API
	 */
	public APITokenDataModel setValid(boolean valid) {
		this.valid = valid;
		return this;
	}
}
