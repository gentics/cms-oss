package com.gentics.api.portalnode.connector;

/**
 * Types of datasources that are managed by the {@link PortalConnectorFactory}
 */
public enum DatasourceType {
	/**
	 * Datasource for accessing a contentrepository
	 */
	contentrepository,

	/**
	 * Datasource for accessing a multichannelling aware contentrepository
	 */
	mccr,

	/**
	 * Datasource for accessing an LDAP server
	 */
	ldap
}
