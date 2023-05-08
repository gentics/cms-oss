package com.gentics.contentnode.publish;

import java.io.Serializable;
import java.util.concurrent.Callable;

import com.gentics.contentnode.jmx.MBeanRegistry;
import com.gentics.contentnode.jmx.PublisherInfo;

/**
 * Callable Implementation that gets the PublisherInfo from the MBean Registry
 */
public class GetPublisherInfo implements Callable<PublisherInfo>, Serializable {
	/**
	 * Serial Version UI
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public PublisherInfo call() throws Exception {
		return MBeanRegistry.getPublisherInfo();
	}
}
