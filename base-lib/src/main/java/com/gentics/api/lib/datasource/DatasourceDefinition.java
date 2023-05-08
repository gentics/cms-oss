/*
 * @author dietmar
 * @date 12.08.2004
 * @version $Id: DatasourceDefinition.java,v 1.1 2006-01-23 16:40:50 norbert Exp $
 * @gentics.sdk
 */
package com.gentics.api.lib.datasource;

/**
 * Interface for a datasource definition
 */
public interface DatasourceDefinition {

	/**
	 * Get the parameters of the datasource definition
	 * @return parameters of the datasource definition
	 */
	public java.util.Map getParameter();

	/**
	 * Get the id of the datasource definition
	 * @return id
	 */
	public String getID();
}
