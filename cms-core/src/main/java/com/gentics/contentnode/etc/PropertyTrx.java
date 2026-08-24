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
	 * True when the wrapped node preferences from a thread local instance shall be used
	 */
	protected boolean unwrapThreadLocal;

	/**
	 * Set the property to the given value
	 * @param name property name
	 * @param value value
	 * @throws NodeException
	 */
	public PropertyTrx(String name, String value) throws NodeException {
		this(name, value, false);
	}

	/**
	 * Set the property to the given value
	 * @param name property name
	 * @param value value
	 * @param unwrapThreadLocal flag to unwrap the thread local instance of the node preferences
	 * @throws NodeException
	 */
	public PropertyTrx(String name, String value, boolean unwrapThreadLocal) throws NodeException {
		this.name = name;
		this.unwrapThreadLocal = unwrapThreadLocal;
		getPreferences().setProperty(name, value);
	}

	/**
	 * Set the property to the given values
	 * @param name property name
	 * @param values values
	 * @throws NodeException
	 */
	public PropertyTrx(String name, String[] values) throws NodeException {
		this(name, values, false);
	}

	/**
	 * Set the property to the given values
	 * @param name property name
	 * @param values values
	 * @param unwrapThreadLocal flag to unwrap the thread local instance of the node preferences
	 * @throws NodeException
	 */
	public PropertyTrx(String name, String[] values, boolean  unwrapThreadLocal) throws NodeException {
		this.name = name;
		this.unwrapThreadLocal = unwrapThreadLocal;
		getPreferences().setProperty(name, values);
	}

	@Override
	public void close() throws NodeException {
		getPreferences().unsetProperty(name);
	}

	/**
	 * Get the preferences. When {@link #unwrapThreadLocal} is set and the preferences are an instance of {@link ThreadLocalPropertyPreferences},
	 * the wrapped {@link NodePreferences} instance is returned
	 * @return node preferences
	 */
	protected NodePreferences getPreferences() {
		NodePreferences preferences = NodeConfigRuntimeConfiguration.getPreferences();
		if (preferences instanceof ThreadLocalPropertyPreferences threadLocal && this.unwrapThreadLocal) {
			preferences = threadLocal.getWrappedPreferences();
		}
		return preferences;
	}
}
