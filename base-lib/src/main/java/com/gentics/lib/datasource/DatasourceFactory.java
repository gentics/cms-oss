package com.gentics.lib.datasource;

import java.util.Set;

import com.gentics.api.lib.datasource.Datasource;

/**
 * Created by IntelliJ IDEA. User: erwin Date: 06.08.2004 Time: 11:25:44
 */
public interface DatasourceFactory {

	/**
	 * Get an instance of the datasource or null if no instance can be created
	 * @return instance of the datasource
	 */
	public Datasource getInstance();

	public void close();

	/**
	 * Get the class of the generated datasource instances.
	 * @return class of the datasource instances
	 * @throws ClassNotFoundException 
	 */
	public Class<?> getDatasourceClass() throws ClassNotFoundException;

	/**
	 * Get the handle IDs used in this datasource factory
	 * @return set of handle IDs
	 */
	Set<String> getHandleIds();

	/**
	 * Get the factory ID
	 * @return factory ID
	 */
	String getId();
}
