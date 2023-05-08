package com.gentics.lib.datasource;

import com.gentics.api.lib.datasource.Datasource;
import com.gentics.lib.content.GenticsContentFactory;
import com.gentics.lib.datasource.mccr.MCCRCacheHelper;
import com.gentics.lib.datasource.mccr.MCCRDatasource;

/**
 * Job to check for background modifications of contentrepositories (e.g. via
 * publishing of Content.Node) and clear caches if necessary
 */
public class BackgroundSyncChecker extends AbstractDatasourceJob {
	/**
	 * Last Update Timestamp
	 */
	protected long storedLastUpdate = -1;

	/**
	 * Create an instance
	 * @param factory datasource factory
	 */
	public BackgroundSyncChecker(DatasourceFactory factory) {
		super(factory);
	}

	@Override
	protected void process() {
		Datasource ds = getDatasource();
		if (ds instanceof CNDatasource) {
			checkCache((CNDatasource) ds);
		} else if (ds instanceof MCCRDatasource) {
			checkCache((MCCRDatasource) ds);
		}
	}

	/**
	 * Check the cache for the given CNDatasource
	 * @param cnDS CNDatasource instance
	 */
	protected void checkCache(CNDatasource cnDS) {
		long currentLastUpdate = cnDS.getLastUpdate();
		boolean differentialSyncChecking = cnDS.isDifferentialSyncChecking();

		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Job {%s} - Stored lastUpdate: %d vs. current lastUpdate: %d", name, storedLastUpdate, currentLastUpdate));
		}

		// this is the first run of the job (or the lastupdate is not
		// found), so don't clear any caches right now
		if (storedLastUpdate == -1) {
			if (logger.isDebugEnabled()) {
				logger.debug(String.format(
						"Job {%s} - last check was with -1 as timestamp, storing %d as last update timestamp", name,
						currentLastUpdate));
			}
			cnDS.clearCaches();

			if (cnDS.isCacheWarmingActive()) {
				try {
					GenticsContentFactory.refreshCaches(cnDS, storedLastUpdate);
				} catch (Exception e) {
					logger.error("Error while freshing cache ", e);
				}
			}
			storedLastUpdate = currentLastUpdate;
			return;
		}

		// compare the last update values and when different, clear caches
		if (storedLastUpdate != currentLastUpdate) {
			if (logger.isDebugEnabled()) {
				logger.debug(String.format("Job {%s} - Recognized background modification of contentrepository: will clear caches now%s", name,
						differentialSyncChecking ? " - only for the modified objects (differential sync checking)" : ""));
			}

			if (differentialSyncChecking) {
				if (cnDS.isCacheWarmingActive()) {
					try {
						GenticsContentFactory.refreshCaches(cnDS, storedLastUpdate);
					} catch (Exception e) {
						logger.error("Error while freshing cache ", e);
					}
				} else {
					GenticsContentFactory.clearDifferentialCaches(cnDS, storedLastUpdate);
				}
			} else {
				// clearing all caches
				cnDS.clearCaches();

				// if cache warming is activated, repopulate cache as configured
				if (cnDS.isCacheWarmingActive()) {
					try {
						GenticsContentFactory.refreshCaches(cnDS, -1L);
					} catch (Exception e) {
						logger.error("Error while freshing cache ", e);
					}
				} 
			}
			storedLastUpdate = currentLastUpdate;
		}
	}

	/**
	 * Check the cache for the given MCCRDatasource
	 * @param ds MCCRDatasource instance
	 */
	protected void checkCache(MCCRDatasource ds) {
		long currentLastUpdate = ds.getLastUpdate(true);
		boolean differentialSyncChecking = ds.isDifferentialSyncChecking();

		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Job {%s} - Stored lastUpdate: %d vs. current lastUpdate: %d", name, storedLastUpdate, currentLastUpdate));
		}

		// this is the first run of the job (or the lastupdate is not found), so don't clear any caches right now
		if (storedLastUpdate == -1) {
			if (logger.isDebugEnabled()) {
				logger.debug(String.format(
						"Job {%s} - last check was with -1 as timestamp, storing %d as last update timestamp", name,
						currentLastUpdate));
			}
			ds.clearCaches();

			if (ds.isCacheWarmingActive()) {
				MCCRCacheHelper.refreshCaches(ds, storedLastUpdate);
			}
			storedLastUpdate = currentLastUpdate;
			return;
		}

		// compare the last update values and when different, clear caches
		if (storedLastUpdate != currentLastUpdate) {
			if (logger.isDebugEnabled()) {
				logger.debug(String.format("Job {%s} - Recognized background modification of contentrepository: will clear caches now%s", name,
						differentialSyncChecking ? " - only for the modified objects (differential sync checking)" : ""));
			}

			if (differentialSyncChecking) {
				MCCRCacheHelper.refreshCaches(ds, storedLastUpdate);
			} else {
				// clearing all caches
				ds.clearCaches();

				// if cache warming is activated, repopulate cache as configured
				if (ds.isCacheWarmingActive()) {
					MCCRCacheHelper.refreshCaches(ds, storedLastUpdate);
				} 
			}
			storedLastUpdate = currentLastUpdate;
		}
	}
}
