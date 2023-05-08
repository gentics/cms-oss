package com.gentics.contentnode.jmx;

import com.gentics.contentnode.devtools.ChangeWatchService;

/**
 * MBean for change event listeners
 */
public class ChangeEventListener implements ChangeEventListenerMBean {
	@Override
	public int getCount() {
		return ChangeWatchService.getRegistryEntries();
	}
}
