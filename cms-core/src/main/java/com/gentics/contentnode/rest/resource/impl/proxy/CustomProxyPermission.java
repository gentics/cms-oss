package com.gentics.contentnode.rest.resource.impl.proxy;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.TransactionManager;

/**
 * POJO representing the permission required to access a specific proxy
 */
public class CustomProxyPermission {
	/**
	 * Object Type
	 */
	private int type;

	/**
	 * Object ID
	 */
	private int id;

	/**
	 * Get the object Type
	 * @return object Type
	 */
	public int getType() {
		return type;
	}

	/**
	 * Set the object Type
	 * @param type object type
	 */
	public void setType(int type) {
		this.type = type;
	}

	/**
	 * Get the object ID
	 * @return object ID
	 */
	public int getId() {
		return id;
	}

	/**
	 * Set the object ID
	 * @param id object ID
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * Check whether access to the proxied resource is allowed
	 * @return true iff access is allowed
	 * @throws NodeException
	 */
	@JsonIgnore
	public boolean allowAccess() throws NodeException {
		return TransactionManager.getCurrentTransaction().getPermHandler().canView(type, id);
	}
}
