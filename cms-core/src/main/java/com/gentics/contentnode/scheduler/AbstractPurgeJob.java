package com.gentics.contentnode.scheduler;

import static com.gentics.contentnode.db.DBUtils.deleteWithPK;
import static com.gentics.contentnode.factory.Trx.supply;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.lib.log.NodeLogger;

/**
 * Abstract base class for purge jobs
 */
public abstract class AbstractPurgeJob {
	/**
	 * Maximum number of records to delete in one transaction
	 */
	public final static int BATCH_SIZE = 100000;

	/**
	 * Purge old records from the given table in batches
	 * @param logger logger
	 * @param table table to purge
	 * @param timestamp timestamp
	 * @param sqlWhere sql where clause, which identifies records to purge
	 * @param whereParams parameters for the where clause
	 * @throws NodeException
	 */
	protected void batchedPurge(NodeLogger logger, String table, int timestamp, String sqlWhere, Object[] whereParams) throws NodeException {
		if (logger.isInfoEnabled()) {
			logger.info(String.format("Start purging records with timestamp < %d from %s", timestamp, table));
		}
		int deleted = 0;
		int total = 0;
		do {
			deleted = supply(() -> deleteWithPK(table, "id", sqlWhere, whereParams, BATCH_SIZE));
			total += deleted;
			if (logger.isInfoEnabled() && deleted > 0) {
				logger.info(String.format("Purged %d old records from table %s", deleted, table));
			}
		} while (deleted > 0);
		if (logger.isInfoEnabled()) {
			logger.info(String.format("Purged total of %d records from table %s", total, table));
		}
	}
}
