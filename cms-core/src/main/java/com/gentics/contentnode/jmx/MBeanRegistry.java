package com.gentics.contentnode.jmx;

import java.lang.management.ManagementFactory;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import com.gentics.lib.log.NodeLogger;

public class MBeanRegistry {

	/**
	 * Publisher Info
	 */
	protected static PublisherInfo publisherInfo;

	/**
	 * Logger
	 */
	protected static NodeLogger logger = NodeLogger.getNodeLogger(MBeanRegistry.class);

	static {
		publisherInfo = new PublisherInfo();
		registerMBean(publisherInfo, "Publish", "PublisherInfo");
	}

	/**
	 * Get the publisher info MBean
	 * @return publisher info
	 */
	public static PublisherInfo getPublisherInfo() {
		return publisherInfo;
	}

	/**
	 * Register the MBean under the given type and name
	 * @param mBean MBean
	 * @param type type
	 * @param name name
	 */
	public static void registerMBean(Object mBean, String type, String name) {
		try {
			ManagementFactory.getPlatformMBeanServer().registerMBean(mBean, getName(type, name));
		} catch (Exception e) {
			logger.error("Error while registering MBean " + name, e);
		}
	}

	/**
	 * Unregister the MBean with the given name
	 * @param type type
	 * @param name name
	 */
	public static void unregisterMBean(String type, String name) {
		try {
			ManagementFactory.getPlatformMBeanServer().unregisterMBean(getName(type, name));
		} catch (Exception e) {
			logger.error("Error while unregistering MBean " + name, e);
		}
	}

	/**
	 * Get an ObjectName instance
	 * @param type type
	 * @param name name
	 * @return ObjectName
	 * @throws NullPointerException 
	 * @throws MalformedObjectNameException 
	 */
	protected static ObjectName getName(String type, String name) throws MalformedObjectNameException, NullPointerException {
		return new ObjectName("com.gentics.contentnode.mbeans:type=" + type + ",name=" + name);
	}
}
