package com.gentics.contentnode.init;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.ContentNodeFactory;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.TransactionManager.Executable;
import com.gentics.lib.db.SQLExecutor;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.log.NodeLogger;

/**
 * Static helper class for Initialization Jobs that need to be run
 */
public class InitializationJobs {
	/**
	 * Logger instance
	 */
	private static NodeLogger logger = NodeLogger.getNodeLogger(InitializationJobs.class);

	/**
	 * This will start all init jobs, that are scheduled
	 * @throws NodeException
	 */
	public static void start() throws NodeException {
		final Set<String> keys = Job.getKeys();

		if (logger.isInfoEnabled()) {
			logger.info("Loading scheduled Jobs");
		}

		Transaction t = null;
		try {
			t = ContentNodeFactory.getInstance().startTransaction(true);

			final List<String> jobNames = new ArrayList<String>();
			DBUtils.executeStatement("SELECT * FROM nodesetup WHERE name in (" + StringUtils.repeat("?", keys.size(), ",") + ")", new SQLExecutor() {
				@Override
				public void prepareStatement(PreparedStatement stmt) throws SQLException {
					int pCounter = 1;
					for (String key : keys) {
						stmt.setString(pCounter++, key);
					}
				}

				@Override
				public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
					while (rs.next()) {
						String key = rs.getString("name");

						if (jobNames.contains(key)) {
							logger.warn("Ignoring duplicate entry for job '" + key + "'.");
						} else {
							if (logger.isInfoEnabled()) {
								logger.info("Found scheduled job '" + key + "'.");
							}
							jobNames.add(key);
						}
					}
				}
			});

			List<Job> jobs = new ArrayList<InitializationJobs.Job>();
			for (String name : jobNames) {
				jobs.add(Job.get(name));
			}
			Collections.sort(jobs, new Comparator<Job>() {
				@Override
				public int compare(Job o1, Job o2) {
					return o1.sortOrder - o2.sortOrder;
				}
			});

			for (Job job : jobs) {
				if (logger.isInfoEnabled()) {
					logger.info("Starting job '" + job.nodeSetupKey + "'.");
				}
				job.start();

				DBUtils.executeUpdate("DELETE FROM nodesetup WHERE name = ?", new Object[] { job.nodeSetupKey });
			}

			if (logger.isInfoEnabled()) {
				logger.info("Finished starting jobs.");
			}

		} finally {
			if (t != null) {
				t.commit();
			}
		}
	}

	/**
	 * Enumeration of existing jobs
	 */
	public static enum Job {
		/**
		 * Job that fixes DatasourcePartType datasources
		 */
		FIX_DATASOURCES("fixdatasources", FixDatasourcesJob.class, 4),

		/**
		 * Job that synchronizes published versions of page variants
		 */
		SYNC_VARIANTVERSIONS("syncvariantversions", SynchronizePageVariantVersions.class, 5),

		/**
		 * Job that hashes all passwords with bcrypt.
		 */
		BCRYPT_PASSWORDS("bcryptpasswords", BcryptPasswords.class, 2),

		/**
		 * Job that generates new passwords for the system and the gentics user
		 */
		SECURE_SYSTEM_GENTICS_LOGINS("securesystemgenticslogins", SecureSystemGenticsLogins.class, 3),

		/**
		 *  Job that delete page tags that reference none existing constructs
		 */
		FIX_INCONSISTENT_TAGS("fixinconsistenttags", FixInconsistentTagsJob.class, 6),

		/**
		 * Job that migrates the global IDs to Uuids
		 */
		MIGRATE_GLOBALIDS("migrateglobalids", MigrateGlobalIds.class, 1),

		/**
		 * Job that migrates an existing scheduler.suspend file into the nodesetup table
		 */
		MIGRATE_SCHEDULER_SUSPEND("migrateschedulersuspend", MigrateSchedulerSuspend.class, 7),

		/**
		 * Job that migrates time management to the new structure
		 */
		MIGRATE_TIME_MANAGEMENT("migratetimemanagement", MigrateTimeManagement.class, 8),

		/**
		 * Job that synchronizes the default packages into the CMS
		 */
		SYNC_DEFAULT_PACKAGE("syncdefaultpackage", SyncDefaultPackage.class, 9),

		/**
		 * Job that migrates the old scheduler to the new one
		 */
		MIGRATE_SCHEDULER("migratescheduler", MigrateScheduler.class, 10);

		/**
		 * Get the nodesetup keys of all existing jobs
		 * @return set of nodesetup keys
		 */
		public static Set<String> getKeys() {
			Set<String> keys = new HashSet<String>();
			for (Job job : values()) {
				keys.add(job.nodeSetupKey);
			}
			return keys;
		}

		/**
		 * Get the job instance by nodesetup key
		 * @param nodeSetupKey nodesetup key
		 * @return job instance
		 * @throws NodeException if no job was found
		 */
		public static Job get(String nodeSetupKey) throws NodeException {
			for (Job job : values()) {
				if (StringUtils.isEqual(job.nodeSetupKey, nodeSetupKey)) {
					return job;
				}
			}

			throw new NodeException("Could not find initialization job with key '" + nodeSetupKey + "'");
		}

		/**
		 * nodesetup key. This will always start with "job:"
		 */
		private String nodeSetupKey;

		/**
		 * Sortorder of the init jobs
		 */
		private int sortOrder;

		/**
		 * Implementation class of the job
		 */
		private Class<? extends InitJob> jobClass;

		/**
		 * Create an enumeration instance
		 * @param nodeSetupKey nodesetup key. When it does not start with the "job:" prefix, it will be added
		 * @param jobClass
		 * @param sortOrder sortorder
		 */
		private Job(String nodeSetupKey, Class<? extends InitJob> jobClass, int sortOrder) {
			nodeSetupKey = ObjectTransformer.getString(nodeSetupKey, "");
			if (!nodeSetupKey.startsWith("job:")) {
				nodeSetupKey = "job:" + nodeSetupKey;
			}
			this.nodeSetupKey = nodeSetupKey;
			this.jobClass = jobClass;
			this.sortOrder = sortOrder;
		}

		/**
		 * Start the job with specified foreground time
		 * @throws NodeException
		 */
		public void start() throws NodeException {
			try {
				TransactionManager.execute(new Executable() {
					/* (non-Javadoc)
					 * @see com.gentics.contentnode.factory.TransactionManager.Executable#execute()
					 */
					public void execute() throws NodeException {
						try {
							InitJob job = jobClass.newInstance();
							job.execute();
						} catch (Exception e) {
							throw new NodeException("Error while starting job '" + nodeSetupKey + "' of " + jobClass, e);
						}
					}
				});
			} catch (NodeException e) {
				throw new RuntimeException("The InitJob " + name() + " failed.", e);
			}
		}
	}
}
