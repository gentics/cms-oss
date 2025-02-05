package com.gentics.contentnode.rest.model.devtools;

import java.io.Serializable;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.User;

/**
 * Synchronization information for devtools sync
 */
@XmlRootElement
public class SyncInfo implements Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -647303978980830103L;

	private boolean enabled;

	private User user;

	/**
	 * Create empty instance
	 */
	public SyncInfo() {
	}

	/**
	 * Create instance with enabled flag and user
	 * @param enabled enabled flag
	 * @param user user
	 */
	public SyncInfo(boolean enabled, User user) {
		this.enabled = enabled;
		this.user = user;
	}

	/**
	 * Flag whether the sync is currently enabled or not
	 * @return true if enabled
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Set the enabled flag
	 * @param enabled flag
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	/**
	 * User who enabled the sync
	 * @return user
	 */
	public User getUser() {
		return user;
	}

	/**
	 * Set the user
	 * @param user user
	 */
	public void setUser(User user) {
		this.user = user;
	}
}
