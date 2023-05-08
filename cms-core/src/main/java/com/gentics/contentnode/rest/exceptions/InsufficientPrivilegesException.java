package com.gentics.contentnode.rest.exceptions;

import java.util.Arrays;
import java.util.List;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.rest.model.perm.PermType;

/**
 * Exception, that is thrown when access to a resource is denied due to missing privileges
 */
public class InsufficientPrivilegesException extends NodeException {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * User ID
	 */
	private int userId;

	/**
	 * User name (login name)
	 */
	private String userName = "unknown user";

	/**
	 * Type of denied permission
	 */
	private PermType permType;

	/**
	 * Object Type of accessed resource
	 */
	private int objectType;

	/**
	 * Object ID of accessed resource
	 */
	private int objectId;

	/**
	 * Create instance
	 * @param message message
	 * @param object accessed object
	 * @param permType type of denied permission
	 */
	public InsufficientPrivilegesException(String message, NodeObject object, PermType permType) {
		this(message, null, (List<String>)null, object, permType);
	}

	/**
	 * Create instance
	 * @param message message
	 * @param messageKey message key
	 * @param parameter parameter for the message
	 * @param object accessed object
	 * @param permType type of denied permission
	 */
	public InsufficientPrivilegesException(String message, String messageKey, String parameter, NodeObject object, PermType permType) {
		this(message, messageKey, Arrays.asList(parameter), object, permType);
	}

	/**
	 * Create instance
	 * @param message message
	 * @param messageKey message key
	 * @param parameters parameters
	 * @param object accessed object
	 * @param permType type of denied permission
	 */
	public InsufficientPrivilegesException(String message, String messageKey, List<String> parameters, NodeObject object, PermType permType) {
		this(message, messageKey, parameters, object != null ? object.getTType() : 0, object != null ? object.getId() : 0, permType);
	}

	/**
	 * Create instance
	 * @param message message
	 * @param messageKey message key
	 * @param parameters parameters
	 * @param objectType type of accessed object
	 * @param objectId ID of accessed object
	 * @param permType type of denied permission
	 */
	public InsufficientPrivilegesException(String message, String messageKey, List<String> parameters, int objectType, int objectId, PermType permType) {
		super(message, messageKey, parameters);
		determineUserId();
		this.objectType = objectType;
		this.objectId = objectId;
		this.permType = permType;
	}

	/**
	 * Get User ID
	 * @return user ID
	 */
	public int getUserId() {
		return userId;
	}

	/**
	 * Get User name
	 * @return username
	 */
	public String getUserName() {
		return userName;
	}

	/**
	 * Get Type of accessed object
	 * @return object type
	 */
	public int getObjectType() {
		return objectType;
	}

	/**
	 * Get ID of accessed object
	 * @return object ID
	 */
	public int getObjectId() {
		return objectId;
	}

	/**
	 * Get Type of denied permission
	 * @return permission type
	 */
	public PermType getPermType() {
		return permType;
	}

	/**
	 * Determine userId and userName of the current user
	 */
	protected void determineUserId() {
		Transaction t = TransactionManager.getCurrentTransactionOrNull();
		if (t != null) {
			userId = t.getUserId();
			try {
				SystemUser user = t.getObject(SystemUser.class, userId);
				if (user != null) {
					userName = user.getLogin();
				}
			} catch (NodeException e) {
			}
		}
	}
}
