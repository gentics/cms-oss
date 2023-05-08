package com.gentics.contentnode.jmx;

/**
 * Interface for change event listeners MBean
 *
 */
public interface ChangeEventListenerMBean {
	/**
	 * Get number of registered listeners
	 * @return number
	 */
	int getCount();
}
