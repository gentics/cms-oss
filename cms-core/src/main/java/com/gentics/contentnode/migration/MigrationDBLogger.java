package com.gentics.contentnode.migration;

import java.io.File;
import java.io.FileNotFoundException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.migration.jobs.AbstractMigrationJob;
import com.gentics.contentnode.rest.model.migration.TagTypeMigrationMapping;
import com.gentics.contentnode.rest.model.migration.TemplateMigrationMapping;
import com.gentics.contentnode.rest.model.response.migration.MigrationJobEntry;
import com.gentics.contentnode.rest.model.response.migration.MigrationJobLogEntryItem;
import com.gentics.lib.db.SQLExecutor;
import com.gentics.lib.log.NodeLogger;

public class MigrationDBLogger {

	/**
	 * The default logger
	 */
	public static final NodeLogger DEFAULT_LOGGER = NodeLogger.getNodeLogger(MigrationDBLogger.class);

	/**
	 * Logger
	 */
	protected NodeLogger logger;

	/**
	 * SQL statement used to insert new tag type migration job entries into the migrationjob table
	 */
	protected final static String INSERT_MIGRATIONJOB_SQL = "INSERT INTO migrationjob (job_id, job_type, job_status, start_timestamp, job_config, log_name) VALUES (?, ?, ?, UNIX_TIMESTAMP(NOW()), ?, ?)";

	/**
	 * SQL statement used to select migration job entries from the migrationjob table
	 */
	protected final static String SELECT_MIGRATIONJOBS_SQL = "SELECT job_id, job_type, job_status, start_timestamp, job_config, log_name from migrationjob order by job_id desc";

	/**
	 * SQL statement used to get the counts of migrationjob items per entry
	 */
	protected final static String SELECT_MIGRATIONJOB_ENTRIES_COUNTS_SQL = "SELECT job_id, COUNT(obj_id) handled_objects FROM migrationjob_item GROUP BY job_id";

	/**
	 * SQL statement used to select a specific migration job entry for the migrationjob table
	 */
	protected final static String SELECT_MIGRATIONJOB_SQL = "SELECT job_id, job_type, job_status, start_timestamp, job_config, log_name from migrationjob where job_id = ?";

	/**
	 * SQL statement used to get the count of migrationjob items for a single job
	 */
	protected final static String SELECT_MIGRATIONJOB_ENTRIES_SQL = "SELECT COUNT(obj_id) handled_objects FROM migrationjob_item WHERE job_id = ?";

	/**
	 * SQL statement used to update tag type migration job entries in the migrationjob table
	 */
	protected final static String UPDATE_MIGRATIONJOB_STATUS_SQL = "UPDATE migrationjob SET job_status = ? WHERE job_id = ?";

	/**
	 * SQL statement used to insert new part migration entries into the migrationjob_item table
	 */
	protected final static String INSERT_MIGRATIONJOB_ITEM_SQL = "INSERT INTO migrationjob_item (job_id, obj_id, obj_type, status) VALUES (?, ?, ?, ?)";

	/**
	 * SQL statement used to select job migration entries from the migrationjob_item table
	 */
	protected final static String SELECT_MIGRATIONJOB_ITEM_SQL = "SELECT job_id, obj_id, obj_type, status FROM migrationjob_item WHERE job_id = ?";

	/**
	 * SQL statement used to update part migration entries in the migrationjob_item table
	 */
	protected final static String UPDATE_MIGRATIONJOB_ITEM_SQL = "UPDATE migrationjob_item SET status = ? WHERE job_id = ? AND obj_id = ? AND obj_type = ?";

	/**
	 * Create a new {@link MigrationDBLogger}
	 * 
	 * @param logger
	 *            The logger configured to be used for logging tag type migrations
	 * @throws NodeException
	 */
	public MigrationDBLogger(NodeLogger logger) throws NodeException {
		if (logger != null) {
			this.logger = logger;
		} else {
			throw new NodeException("The logger was not set");
		}
	}

	/**
	 * Create a Tag Type Migration Job entry in the database
	 * 
	 * @param jobId
	 *            the ID of the job to create an entry for
	 * @param jobType
	 *            the type of the job
	 * @param mappings
	 *            the part mappings received by the REST API to be used for the job being logged
	 * @throws NodeException
	 */
	public void createMigrationJobEntry(final int jobId, final int jobType, List<TagTypeMigrationMapping> mappings) throws NodeException {

		final String msg = "Error occured while attempting to create tag type migration log entry.";
		ObjectMapper mapper = new ObjectMapper();
		final String jsonValue;
		final String logPath = MigrationHelper.getLogPath(logger);

		try {
			jsonValue = mapper.writeValueAsString(mappings);
		} catch (Exception e) {
			logger.error("Error occured while attempting to get String value of TagTypeMigrationMapping.", e);
			throw new NodeException("Unable to acquire String value of TagTypeMigrationMapping.");
		}

		TransactionManager.execute(new TransactionManager.Executable() {
			public void execute() throws NodeException {
				List<Object> params = new ArrayList<Object>();

				params.add(jobId);
				params.add(jobType);
				params.add(AbstractMigrationJob.STATUS_STARTED);
				params.add(jsonValue);
				params.add(logPath);

				try {
					DBUtils.executeUpdate(INSERT_MIGRATIONJOB_SQL, (Object[]) params.toArray(new Object[params.size()]));
				} catch (NodeException e) {
					throw new NodeException(msg, e);
				}

			}
		});

	}

	/**
	 * Create a Tag Type Migration Job entry in the database
	 * 
	 * @param jobId
	 *            the ID of the job to create an entry for
	 * @param jobType
	 *            the type of the job
	 * @param mappings
	 *            the mappings received by the REST API to be used for the job being logged
	 * @throws NodeException
	 */
	public void createMigrationJobEntry(final int jobId, final int jobType, final TemplateMigrationMapping mappings) throws NodeException {

		ObjectMapper mapper = new ObjectMapper();
		final String jsonValue;
		final String logPath = MigrationHelper.getLogPath(logger);

		try {
			jsonValue = mapper.writeValueAsString(mappings);
		} catch (Exception e) {
			logger.error("Error occured while attempting to get String value of TemplateMigrationMapping.", e);
			throw new NodeException("Unable to acquire String value of TemplateMigrationMapping.");
		}

		TransactionManager.execute(new TransactionManager.Executable() {
			public void execute() throws NodeException {
				List<Object> params = new ArrayList<Object>();

				params.add(jobId);
				params.add(jobType);
				params.add(AbstractMigrationJob.STATUS_STARTED);
				params.add(jsonValue);
				params.add(logPath);

				DBUtils.executeUpdate(INSERT_MIGRATIONJOB_SQL, (Object[]) params.toArray(new Object[params.size()]));
			}
		});

	}

	/**
	 * Update the status of a tag type migration job entry
	 * 
	 * @param logger
	 *            the logger configured to be used for logging tag type migrations
	 * @param jobId
	 *            the id of the tag type migration job to update
	 * @param status
	 *            the job's new status
	 * @throws NodeException
	 */
	public void updateMigrationJobEntryStatus(final int jobId, final int status) throws NodeException {
		TransactionManager.execute(new TransactionManager.Executable() {
			public void execute() throws NodeException {
				List<Object> params = new ArrayList<Object>();

				params.add(status);
				params.add(jobId);

				DBUtils.executeUpdate(UPDATE_MIGRATIONJOB_STATUS_SQL, (Object[]) params.toArray(new Object[params.size()]));
			}
		});

	}

	/**
	 * Create a new part migration entry in the database
	 * 
	 * @param jobId
	 *            the id of the tag type migration job
	 * @param objId
	 *            the id of the part being migrated
	 * @param objType
	 *            the object type of the part being migrated
	 * @param status
	 *            the status of the part's migration
	 * @throws NodeException
	 */
	public void createMigrationJobItemEntry(final int jobId, final int objId, final int objType, final int status) throws NodeException {
		TransactionManager.execute(new TransactionManager.Executable() {
			public void execute() throws NodeException {
				DBUtils.executeInsert(INSERT_MIGRATIONJOB_ITEM_SQL, new Object[] { jobId, objId, objType, status });
			}
		});
	}

	/**
	 * Update the status of a part migration in the database
	 * 
	 * @param jobId
	 *            the id of the tag type migration job
	 * @param objId
	 *            the id of the part being migrated
	 * @param objType
	 *            the object type of the part being migrated
	 * @param status
	 *            the new status of the part migration
	 * @throws NodeException
	 */
	public void updateMigrationJobItemEntry(final int jobId, final int objId, final int objType, final int status) throws NodeException {
		TransactionManager.execute(new TransactionManager.Executable() {
			public void execute() throws NodeException {
				DBUtils.executeUpdate(UPDATE_MIGRATIONJOB_ITEM_SQL, new Object[] { status, jobId, objId, objType });
			}
		});

	}

	/**
	 * Returns the entry for the given jobId.
	 * 
	 * @return The TTMJobLogEntry or null when no entry could be found for the given jobId.
	 * @throws NodeException
	 */
	public MigrationJobEntry getMigrationJobEntry(final int jobId) throws NodeException {
		final MigrationJobEntry jobEntry = new MigrationJobEntry();

		TransactionManager.execute(() -> {
			DBUtils.executeStatement(SELECT_MIGRATIONJOB_SQL, Transaction.SELECT_STATEMENT, stmt -> {
				stmt.setInt(1, jobId);
			}, rs -> {
				while (rs.next()) {
					jobEntry.setJobId(rs.getInt("job_id"));
					jobEntry.setJobType(rs.getInt("job_type"));
					jobEntry.setStatus(rs.getInt("job_status"));
					jobEntry.setTimestamp(rs.getString("start_timestamp"));
					jobEntry.setConfig(rs.getString("job_config"));
					jobEntry.setLogName(rs.getString("log_name"));
				}
			});
		});

		TransactionManager.execute(() -> {
			DBUtils.executeStatement(SELECT_MIGRATIONJOB_ENTRIES_SQL, Transaction.SELECT_STATEMENT, stmt -> {
				stmt.setInt(1, jobId);
			}, rs -> {
				while (rs.next()) {
					jobEntry.setHandledObjects(rs.getLong("handled_objects"));
				}
			});
		});

		return jobEntry;
	}

	/**
	 * Returns a list of all job item entries that were found in the database ordered by jobId
	 * 
	 * @return
	 * @throws NodeException
	 */
	public List<MigrationJobEntry> getMigrationJobEntries() throws NodeException {
		final List<MigrationJobEntry> jobEntries = new ArrayList<MigrationJobEntry>();

		TransactionManager.execute(() -> {
			DBUtils.executeStatement(SELECT_MIGRATIONJOBS_SQL, Transaction.SELECT_STATEMENT, null, (rs) -> {
				while (rs.next()) {
					jobEntries.add(new MigrationJobEntry(rs.getInt("job_id"), rs.getInt("job_type"), rs.getInt("job_status"), rs.getString("start_timestamp"),
							rs.getString("job_config"), rs.getString("log_name"), 0L));
				}
			});
		});

		// Add the item counts
		final Map<Integer, Long> itemCounts = new HashMap<>();
		TransactionManager.execute(() -> {
			DBUtils.executeStatement(SELECT_MIGRATIONJOB_ENTRIES_COUNTS_SQL, Transaction.SELECT_STATEMENT, null, (rs) -> {
				while (rs.next()) {
					itemCounts.put(rs.getInt("job_id"), rs.getLong("handled_objects"));
				}
			});
		});
		jobEntries.forEach(entry -> entry.setHandledObjects(itemCounts.getOrDefault(entry.getJobId(), 0L)));

		return jobEntries;
	}

	/**
	 * Returns a list of job entry items
	 * 
	 * @param jobId
	 * @return
	 * @throws NodeException
	 */
	public List<MigrationJobLogEntryItem> getMigrationJobItemEntries(final int jobId) throws NodeException {
		final List<MigrationJobLogEntryItem> jobItems = new ArrayList<MigrationJobLogEntryItem>();
		final String errorMessage = "Error while fetching job log items for job {" + jobId + "} from database.";

		TransactionManager.execute(new TransactionManager.Executable() {
			public void execute() throws NodeException {
				try {
					DBUtils.executeStatement(SELECT_MIGRATIONJOB_ITEM_SQL,
							new SQLExecutor() {
						@Override
						public void prepareStatement(PreparedStatement stmt) throws SQLException {
							stmt.setInt(1, jobId);
						}

						@Override
						public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
							while (rs.next()) {
								jobItems.add(new MigrationJobLogEntryItem(rs.getInt("job_id"), rs.getInt("obj_id"), rs.getInt("obj_type"), rs.getInt("status")));

							}
						}
					});
				} catch (NodeException e) {
					throw new NodeException(errorMessage, e);
				}
			}
		});

		return jobItems;
	}

	/**
	 * Returns the logfile for the given jobId
	 * 
	 * @param jobId
	 * @return logFile for the given jobId
	 * @throws NodeException
	 * @throws FileNotFoundException
	 */
	public File getLogFileForJob(int jobId) throws NodeException, FileNotFoundException {
		File logFile = null;
		List<MigrationJobEntry> jobEntries = getMigrationJobEntries();

		for (MigrationJobEntry jobEntry : jobEntries) {
			if (jobEntry.getJobId() == jobId) {
				logFile = new File(MigrationHelper.getLogDir(), jobEntry.getLogName());
			}
		}

		if (logFile == null || !logFile.exists()) {
			throw new FileNotFoundException("Logfile for job {" + jobId + "} could not be found.");
		}
		// ensure that the request is not attempting to read other files on the system
		if (!logFile.getParent().equals(MigrationHelper.getLogDir().toString())) {
			throw new SecurityException(
					"Request received that was attempting to read a file not in the migration log directory: {" + MigrationHelper.getLogDir().toString() + "}.");
		}
		return logFile;
	}

}
