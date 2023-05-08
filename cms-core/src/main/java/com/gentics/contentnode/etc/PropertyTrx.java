package com.gentics.contentnode.etc;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;

/**
 * AutoClosable that temporarily sets a property value
 */
public class PropertyTrx implements AutoCloseable {
	/**
	 * Property that was set
	 */
	protected String name;

	/**
	 * Set the property to the given value
	 * @param name property name
	 * @param value value
	 * @throws NodeException
	 */
	public PropertyTrx(String name, String value) throws NodeException {
		NodeConfigRuntimeConfiguration.getDefault().getNodeConfig().getDefaultPreferences().setProperty(name, value);
		this.name = name;
	}

	/**
	 * Set the property to the given values
	 * @param name property name
	 * @param values values
	 * @throws NodeException
	 */
	public PropertyTrx(String name, String[] values) throws NodeException {
		NodeConfigRuntimeConfiguration.getDefault().getNodeConfig().getDefaultPreferences().setProperty(name, values);
		this.name = name;
	}

	@Override
	public void close() throws NodeException {
		NodeConfigRuntimeConfiguration.getDefault().getNodeConfig().getDefaultPreferences().unsetProperty(name);
	}
}
