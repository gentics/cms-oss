package com.gentics.contentnode.scheduler;

import java.util.Properties;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.contentnode.etc.NodePreferences;

/**
 * Properties for the persistent Scheduler that is accessible in NodeConfig.getPersistentScheduler();
 * 
 * @author floriangutmann
 */
public class PersistentSchedulerProperties extends Properties {

	private static final long serialVersionUID = 9108564453915185112L;

	/**
	 * Constructor which initializes the properties from the given NodePreferences.
	 * @param preferences NodePreferences from which to initialize the Scheduler.
	 */
	public PersistentSchedulerProperties(NodePreferences preferences) {
		this.put("org.quartz.dataSource.defaultDS.connectionProvider.class", "com.gentics.contentnode.scheduler.PersistentSchedulerConnectionProvider");

		this.put("org.quartz.scheduler.instanceName", "PersistentScheduler");
		this.put("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool");
		this.put("org.quartz.threadPool.threadCount", ObjectTransformer.getString(preferences.getProperty("quartz.threadPool.threadCount"), "3"));

		this.put("org.quartz.jobStore.class", "org.quartz.impl.jdbcjobstore.JobStoreTX");
		this.put("org.quartz.jobStore.driverDelegateClass", "org.quartz.impl.jdbcjobstore.StdJDBCDelegate");
		this.put("org.quartz.jobStore.dataSource", "defaultDS");
	}
}
