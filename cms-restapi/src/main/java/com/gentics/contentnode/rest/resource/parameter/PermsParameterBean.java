package com.gentics.contentnode.rest.resource.parameter;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;

/**
 * Parameter bean for permissions
 */
public class PermsParameterBean {
	/**
	 * Flag to add permission information for the returned items.
	 */
	@QueryParam("perms")
	@DefaultValue("false")
	public boolean perms = false;

	public PermsParameterBean setPerms(boolean perms) {
		this.perms = perms;
		return this;
	}
}
