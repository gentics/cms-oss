package com.gentics.contentnode.migration.jobs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.gentics.api.contentnode.migration.IMigrationPreprocessor;
import com.gentics.api.contentnode.migration.IMigrationPreprocessor.Result;
import com.gentics.api.contentnode.migration.MigrationException;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.job.AbstractBackgroundJob;
import com.gentics.contentnode.migration.MigrationDBLogger;
import com.gentics.contentnode.object.NodeObjectVersion;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Tag;
import com.gentics.contentnode.rest.model.migration.MigrationPreProcessor;
import com.gentics.contentnode.rest.util.ModelBuilder;
import com.gentics.lib.log.NodeLogger;

/**
 * Abstract class that provides base functionality for migration jobs.
 * 
 * @author Taylor
 * 
 */
public abstract class AbstractMigrationJob extends AbstractBackgroundJob {

	/**
	 * The job type for a tagtype migration
	 */
	public static final int TAGTYPE_MIGRATION_JOB_TYPE = 1;

	/**
	 * The job type for a template migration
	 */
	public static final int TEMPLATE_MIGRATION_JOB_TYPE = 2;

	/**
	 * Unknown status
	 */
	public static final int STATUS_UNKOWN = -1;

	/**
	 * The system is waiting for a new job
	 */
	public static final int STATUS_PENDING = 0;

	/**
	 * A job is currently being processed
	 */
	public static final int STATUS_IN_PROGRESS = 1;

	/**
	 * The job is complete
	 */
	public static final int STATUS_COMPLETED = 2;

	/**
	 * The job is complete with warnings
	 */
	public static final int STATUS_COMPLETED_WITH_WARNINGS = 10;

	/**
	 * The item / job was skipped
	 */
	public static final int STATUS_SKIPPED = 3;

	/**
	 * Error code for a job or an job item
	 */
	public static final int STATUS_ERROR = 4;

	/**
	 * The job has just been started
	 */
	public static final int STATUS_STARTED = 5;

	/**
	 * Status for a job that contained an invalid mapping
	 */
	public static final int STATUS_INVALID = 6;

	/**
	 * The export creation is currently in progress
	 */
	public static final int STATUS_EXPORT_IN_PROGRESS = 7;

	/**
	 * The export was successfully completed
	 */
	public static final int STATUS_EXPORT_COMPLETED = 8;

	/**
	 * The creation of the export has failed
	 */
	public static final int STATUS_EXPORT_FAILED = 9;

	/**
	 * Counter for omitted objects
	 */
	private int nOmittedObjects = 0;

	/**
	 * Sorted list of Preprocessor instances (if any were requested)
	 */
	protected List<IMigrationPreprocessor> preprocessors;

	/**
	 * Create an instance. This will also set the {@link #jobId} to the next available jobId
	 * @throws NodeException
	 */
	public AbstractMigrationJob() throws NodeException {
		jobId = getNextJobId();
	}

	/**
	 * Returns the counter value for omitted objects
	 * @return
	 */
	public int getOmittedObjectsCount() {
		return nOmittedObjects;
	}

	/**
	 * Increase the counter value for omitted objects
	 */
	public void incrementOmittedObjectsCount() {
		this.nOmittedObjects++;
	}

	/**
	 * Logger
	 */
	protected NodeLogger logger = NodeLogger.getNodeLogger(AbstractMigrationJob.class);

	/**
	 * DB Logger
	 */
	protected MigrationDBLogger dbLogger;

	/**
	 * Percentage of migration job completed
	 */
	protected int percentCompleted = 0;

	/**
	 * Total number of node objects involved in the migration
	 */
	protected int migrationObjectCountTotal;

	/**
	 * Number of node objects that have been migrated
	 */
	protected int migrationObjectCountFinished = 0;

	/**
	 * Unique jobId
	 */
	protected int jobId;

	/**
	 * Key for the ttm commit batch size option
	 */
	protected static final String MIGRATION_COMMIT_BATCH_SIZE_KEY = "contentnode.ttm_commit_batch_size";

	/**
	 * Return a localized string representation of the job
	 */
	public abstract String getJobDescription();

	/**
	 * Mark all tags that must be skipped during a migration process
	 * 
	 * @param tags
	 *            the collection of tags that need to be skipped
	 */
	protected abstract void markSkippedTags(Collection<? extends Tag> tags);

	/**
	 * Update the percentComplete value to reflect the percentage of objects that have been migrated so far
	 */
	protected void updatePercentCompleted() {
		migrationObjectCountFinished++;
		setPercentCompleted((int) (((double) migrationObjectCountFinished / (double) migrationObjectCountTotal) * 100));
	}

	/**
	 * Handles the original page status
	 * 
	 * @param page
	 * @param online page was originally online
	 * @param modified page was originally modified
	 * @param queueUser optional user who put the page in queue
	 * @param timestamp timestamp for pages that were queued for publish at
	 * @param queuedVersion optional version for pages that were queued for publish at
	 * @throws ReadOnlyException
	 * @throws NodeException
	 */
	protected void handleOriginalPageStatus(Page page, boolean online, boolean modified, SystemUser queueUser, int timestamp, NodeObjectVersion queuedVersion) throws ReadOnlyException, NodeException {
		if (online && !modified && queueUser == null) {
			logger.debug(String.format("Page was originally online: %b, modified: %b -> publishing again", online, modified));
			page.publish();
		} else if (queueUser != null) {
			logger.debug(String.format("Page was originally queued, queueing again"));
			if (timestamp > 0 && queuedVersion != null) {
				page.queuePublish(queueUser, timestamp, queuedVersion);
			} else {
				page.queuePublish(queueUser);
			}
		} else {
			logger.debug(String.format("Page was originally online: %b, modified: %b -> no action", online, modified));
		}
	}

	/**
	 * Get the enabled preprocessors
	 * @return list of enabled preprocessors
	 */
	protected abstract List<MigrationPreProcessor> getEnabledPreProcessors();

	/**
	 * Prepare the requested preprocessors
	 * @throws NodeException
	 */
	protected void preparePreprocessors() throws NodeException {
		List<MigrationPreProcessor> sortedPreprocessors = getEnabledPreProcessors();
		if (ObjectTransformer.isEmpty(sortedPreprocessors)) {
			return;
		}
		Collections.sort(sortedPreprocessors);

		preprocessors = new ArrayList<>();
		for (MigrationPreProcessor migrationPreProcessor : sortedPreprocessors) {
			try {
				preprocessors.add((IMigrationPreprocessor) Class.forName(migrationPreProcessor.getClassName()).newInstance());
			} catch (Exception e) {
				throw new NodeException(String.format("Error while initializing preprocessor {%s}", migrationPreProcessor.getClassName()), e);
			}
		}
	}

	/**
	 * Apply the preprocessor to the tag
	 * @param tag tag
	 * @param preProcessor preprocessor
	 * @return result
	 * @throws MigrationException
	 */
	protected abstract Result invokePreProcessor(com.gentics.contentnode.rest.model.Tag tag, IMigrationPreprocessor preProcessor) throws MigrationException;

	/**
	 * Invoke the preprocessors in the given tag.
	 * @param tag tag to
	 * @return Result.pass, if the tag shall be migrated, Result.skiptag if the tag shall be omitted, Result.skipobject if the object shall be omitted
	 * @throws NodeException
	 */
	protected Result invokePreProcessors(Tag tag) throws NodeException {
		if (ObjectTransformer.isEmpty(preprocessors)) {
			return Result.pass;
		}

		com.gentics.contentnode.rest.model.Tag restTag = ModelBuilder.getTag(tag, false);
		for (IMigrationPreprocessor pre : preprocessors) {
			try {
				Result result = invokePreProcessor(restTag, pre);
				switch (result) {
				case pass:
					// write the data back into the tag
					ModelBuilder.fillRest2Node(restTag, tag);
					break;
				case skiptag:
					// return immediately (with Result.skiptag)
					return Result.skiptag;
				case skipobject:
					// return immediately (with Result.skipobject)
					return Result.skipobject;
				}
			} catch (Exception e) {
				throw new NodeException(String.format("Error while applying preprocessor {%s} on {%s}", pre.getClass().getName(), tag), e);
			}
		}

		return Result.pass;
	}

	/**
	 * Get the next available jobId
	 * @return next jobId (starting with 1)
	 * @throws NodeException
	 */
	protected int getNextJobId() throws NodeException {
		return Optional.ofNullable(Trx.supply(() -> {
			return DBUtils.select("SELECT max(job_id) maximum FROM migrationjob", DBUtils.firstInt("maximum"));
		})).orElse(0) + 1;
	}


	public int getMigrationJobId() {
		return jobId;
	}

	public int getPercentCompleted() {
		return percentCompleted;
	}

	public void setPercentCompleted(int percentCompleted) {
		this.percentCompleted = percentCompleted;
	}

}
