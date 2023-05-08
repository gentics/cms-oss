package com.gentics.api.portalnode.connector;

/**
 * Types of handles that are managed by the {@link PortalConnectorFactory}
 */
public enum HandleType {
	/**
	 * Handles of type sql are used to access SQL databases
	 */
	sql,

	/**
	 * Handles of type ldap are used to access LDAP servers
	 */
	ldap
}
