/*
 * @author robert
 * @date 22.07.2004
 * @version $Id: GenticsUser.java,v 1.1 2006-01-23 16:40:45 norbert Exp $
 * @gentics.sdk
 */
package com.gentics.api.lib.auth;

import java.io.Serializable;

import com.gentics.api.lib.resolving.Resolvable;

/**
 * Interface for the Portal User
 */
public interface GenticsUser extends Resolvable, Serializable {

	/**
	 * Check whether the user is logged in
	 * @return true when the user is logged in, false if not
	 */
	boolean isLoggedIn();

	/**
	 * Check whether the user is anonymous (not logged in)
	 * @return true when the user is anonymous, false if not
	 */
	boolean isAnonymous();

	/**
	 * Get the id of the user
	 * @return id of the user
	 */
	String getId();
}
