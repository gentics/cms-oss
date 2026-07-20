package com.gentics.contentnode.rest.model.token;

import java.io.Serializable;
import java.util.Objects;

/**
 * Model for an API Token
 */
public class ApiTokenDataModel implements Serializable {
	private static final long serialVersionUID = 6322580482053154169L;

	/**
	 * Token ID
	 */
	private int id;

	/**
	 * User ID
	 */
	private int userId;

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
	public ApiTokenDataModel setId(int id) {
		this.id = id;
		return this;
	}

	/**
	 * User ID
	 * @return user Id
	 */
	public int getUserId() {
		return userId;
	}

	/**
	 * Set the user ID
	 * @param userId user ID
	 * @return fluent API
	 */
	public ApiTokenDataModel setUserId(int userId) {
		this.userId = userId;
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
	public ApiTokenDataModel setName(String name) {
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
	public ApiTokenDataModel setCdate(int cdate) {
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
	public ApiTokenDataModel setExpires(int expires) {
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
	public ApiTokenDataModel setLastUsed(int lastUsed) {
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
	public ApiTokenDataModel setValid(boolean valid) {
		this.valid = valid;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, userId, name, cdate, expires, lastUsed, valid);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ApiTokenDataModel other) {
			return id == other.id
					&& userId == other.userId
					&& Objects.equals(name, other.name)
					&& cdate == other.cdate
					&& expires == other.expires
					&& lastUsed == other.lastUsed
					&& valid == other.valid;
		} else {
			return false;
		}
	}

	@Override
	public String toString() {
		return "ApiToken '%s'. Id: %d, User: %d, Created: %d, Expires: %d, Last Used: %d, Valid: %b.".formatted(name,
				id, userId, cdate, expires, lastUsed, valid);
	}
}
