package com.gentics.lib.datasource;

import java.lang.reflect.Constructor;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.lib.datasource.HandlePool;
import com.gentics.api.lib.datasource.VersioningDatasource;
import com.gentics.api.lib.datasource.WriteableVersioningDatasource;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.lib.content.GenticsContentFactory;
import com.gentics.lib.datasource.mccr.MCCRCacheHelper;
import com.gentics.lib.datasource.mccr.MCCRDatasource;
import com.gentics.lib.datasource.mccr.WritableMCCRDatasource;
import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.scheduler.SchedulingInterface;

/**
 * Implementation of {@link DatasourceFactory}
 */
public class DatasourceFactoryImpl implements DatasourceFactory, SchedulingInterface {

	/**
	 * flag: are datasources versioning
	 */
	private boolean versioning = false;

	/**
	 * interval for the scheduled job for autoupdating due future changes in
	 * seconds
	 */
	private int autoupdateInterval = 60;

	private NodeLogger logger = NodeLogger.getNodeLogger(getClass());

	private HandlePool associatedHandlePool = null;

	/**
	 * Set of handle IDs in the handle pool
	 */
	private Set<String> handleIds = new HashSet<>();

	private Map<String, String> parameters = null;

	private String id = null;

	/**
	 * datasource typeid
	 */
	private String typeId = null;

	/**
	 * class of datasource instances
	 */
	private Class<?> datasourceClass = null;
    
	/**
	 * datasource checked and valid
	 */
	private boolean valid = true;

	/**
	 * parameter classes for the required datasource constructor
	 */
	protected final static Class<?>[] DATASOURCE_CONSTRUCTOR_PARAMS = new Class[] {
		String.class, HandlePool.class, Map.class};

	/**
	 * key of the datasource factory in the job execution context
	 */
	public final static String DATASOURCEFACTORYKEY = "datasourcefactory";

	/**
	 * name of the job that upates future changes
	 */
	public final static String UPDATEFUTURECHANGEJOB = "updateFutureChange";

	/**
	 * name of the job that checks for background modifications
	 */
	public final static String CACHESYNCCHECKJOB = "cacheSyncCheck";

	/**
	 * Scheduled future of futureChangesUpdater (if scheduled)
	 */
	protected ScheduledFuture<?> futureChangesUpdater;

	/**
	 * Scheduled future of backgroundSyncChecker (if scheduled)
	 */
	protected ScheduledFuture<?> backgroundSyncChecker;

	/** Creates a new instance of DatasourceFactoryImpl */
	public DatasourceFactoryImpl(DatasourceSTRUCT ds_struct) {
		// define needed variables

		/**
		 * Robert Reinhardt 2004.09.20 datasource can use more than one
		 * datasource-handles HandleID are stored in DatasourceSTRUCT LinkedList
		 * HandleIDs this.HandleID = ds_struct.HandleID;
		 */
		this.id = ds_struct.ID;
		this.typeId = ds_struct.typeID;
		try {
			getDatasourceClass();
		} catch (ClassNotFoundException e) {
			logger.error("Error while configuring datasourcefactory {" + id + "}", e);
		}
		parameters = ds_struct.parameterMap;

		if (logger.isDebugEnabled()) {
			if (parameters == null) {
				logger.debug(String.format("Configuring datasourceFactory {%s} without parameters", this.id));
			} else {
				logger.debug(String.format("Configuring datasourceFactory {%s} with parameters", this.id));
				for (String key : parameters.keySet()) {
					logger.debug(String.format("%s:\t%s", key, parameters.get(key)));
				}
			}
		}

		if (parameters != null) {
			versioning = ObjectTransformer.getBoolean(parameters.get("versioning"), false);

			if (versioning) {
				// when versioning is supported, get the autoupdate interval (or
				// set it to the default value of 60 seconds)
				autoupdateInterval = ObjectTransformer.getInt(parameters.get("versioning.autoupdate.interval"), 60);
			}
		} else {
			versioning = false;
		}
	}

	@Override
	public String getId() {
		return id;
	}

	/**
	 * Set the handle pool to the datasource factory
	 * @param pool handle pool
	 * @param handleIds set of handles used in the handle pool
	 */
	public void setHandlePool(HandlePool pool, Set<String> handleIds) {
		this.associatedHandlePool = pool;
		this.handleIds.addAll(handleIds);

		// after the handle pool was set, we can check whether all versioning
		// requirements are met
		if (versioning) {
			Datasource dsInstance = getInstance();

			if (!(dsInstance instanceof VersioningDatasource)) {
				// versioning was configured but datasource does not support
				// versioning
				if (dsInstance == null) {
					logger.warn(
							"datasource configuration error for datasource '" + id + "': versioning was set to TRUE but no instance could be created; "
							+ "versioning will NOT be active");
				} else {
					logger.warn(
							"datasource configuration error for datasource '" + id + "': versioning was set to TRUE but class " + dsInstance.getClass().getName()
							+ " is no VersioningDatasource; versioning will NOT be active");
				}
				versioning = false;
			} else {
				// let the datasource instance check the requirements
				if (!((VersioningDatasource) dsInstance).checkRequirements()) {
					logger.warn("datasource '" + id + "' does not meet all requirements for versioning; versioning will NOT be active");
					versioning = false;
				}
			}
		}
	}

	/**
	 * Get the handle IDs used in this datasource factory
	 * @return set of handle IDs
	 */
	public Set<String> getHandleIds() {
		return handleIds;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.datasource.DatasourceFactory#getInstance()
	 */
	public Datasource getInstance() {
		if (associatedHandlePool == null || datasourceClass == null) {
			return null;
		}

		try {
			if (!valid) {
				throw new Exception("Datasource is marked as invalid.");
			}
			Constructor<?> constructor = datasourceClass.getConstructor(DATASOURCE_CONSTRUCTOR_PARAMS);
			Datasource ds = (Datasource) constructor.newInstance(new Object[] { id, associatedHandlePool, parameters});

			return ds;
		} catch (Exception e) {
			logger.error("Error while creating datasource instance", e);
			return null;
		}
	}

	/**
	 * get the class of datasources created by this factory
	 * @return the datasource class or null if not found
	 */
	public Class<?> getDatasourceClass() throws ClassNotFoundException {
		if (datasourceClass == null) {
			if ("Content.Node.Connector".equalsIgnoreCase(typeId) || "contentrepository".equalsIgnoreCase(typeId)) {
				logger.warn(
						"Deprecated typeId definition {" + typeId + "} found for datasource {" + id + "}. Use " + CNWriteableDatasource.class.getName() + " instead");
				datasourceClass = CNWriteableDatasource.class;
			} else if ("mccr".equalsIgnoreCase(typeId)) {
				datasourceClass = WritableMCCRDatasource.class;
			} else if ("mysql".equalsIgnoreCase(typeId)) {
				throw new UnsupportedOperationException("Not yet implemented");
			} else {
				// this is the preferred way to configure this, we suppose that the typeId is the classname of the Datasource implementation to use
				try {
					datasourceClass = Class.forName(typeId);
				} catch (ClassNotFoundException e) {
					// we did not find the class, so try prefixing the default package
					try {
						datasourceClass = Class.forName("com.gentics.lib.datasource." + typeId);
					} catch (ClassNotFoundException e1) {
						throw new ClassNotFoundException("No datasource class found for typeId {" + typeId + "} for datasource {" + id + "}");
					}
				}

				if (!Datasource.class.isAssignableFrom(datasourceClass)) {
					throw new ClassNotFoundException("Configured class {" + datasourceClass.getName() + "} is invalid for datasource {" + id + "}");
				}
			}
		}

		return datasourceClass;
	}

	public void close() {
		cancelScheduledJobs();
		if (associatedHandlePool == null) {
			return;
		}
		associatedHandlePool.close();
	}

	/**
	 * Cancel scheduled jobs
	 */
	protected void cancelScheduledJobs() {
		if (futureChangesUpdater != null) {
			futureChangesUpdater.cancel(true);
			futureChangesUpdater = null;
		}
		if (backgroundSyncChecker != null) {
			backgroundSyncChecker.cancel(true);
			backgroundSyncChecker = null;
		}
	}

	@Override
	public void scheduleJobs(ScheduledExecutorService scheduler) {
		cancelScheduledJobs();
		if (isVersioning() && datasourceClass != null && WriteableVersioningDatasource.class.isAssignableFrom(datasourceClass)) {
			// add job to periodically call the method updateDueFutureChanges()
			// of an instance of the datasource
			futureChangesUpdater = scheduler.scheduleWithFixedDelay(new FutureChangesUpdater(this), 0, autoupdateInterval, TimeUnit.SECONDS);
		}

		// Start cache warming if this has been enabled
		Datasource ds = this.getInstance();

		if (ds instanceof CNDatasource) {
			CNDatasource cnDS = (CNDatasource) ds;

			if (cnDS.isCacheWarmingActive() && cnDS.isCacheWarmingOnInit()) {
				try {
					GenticsContentFactory.refreshCaches(cnDS, -1L);
				} catch (Exception e) {
					logger.warn("Error while freshing cache ", e);
				}
			}
		} else if (ds instanceof MCCRDatasource) {
			MCCRDatasource mccrDS = (MCCRDatasource) ds;
			if (mccrDS.isCacheWarmingActive() && mccrDS.isCacheWarmingOnInit()) {
				MCCRCacheHelper.refreshCaches(mccrDS, -1);
			}
		}

		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Cache for %s is %b", id, ObjectTransformer.getBoolean(parameters.get("cache"), false)));
			logger.debug(String.format("Syncchecking for %s is %b", id, ObjectTransformer.getBoolean(parameters.get("cache.syncchecking"), false)));
		}
		if (isCacheSyncCheck()) {
			if (logger.isInfoEnabled()) {
				logger.info(String.format("Scheduling BackgroundSyncChecker for %s", id));
			}
			// the datasources support caching and have sync checking enabled

			// add job to periodically check the contentrepository for background updates
			try {
				// repeat the job every x seconds (defaults to 10)
				int interval = ObjectTransformer.getInt(parameters.get("cache.syncchecking.interval"), 10);
				backgroundSyncChecker = scheduler.scheduleWithFixedDelay(new BackgroundSyncChecker(this), 0, interval, TimeUnit.SECONDS);
			} catch (Throwable e) {
				logger.error("Error while scheduling BackgroundSyncChecker", e);
			}
		} else {
			if (logger.isInfoEnabled()) {
				logger.info(String.format("Not scheduling BackgroundSyncChecker for %s", id));
			}
		}
	}

	/**
	 * check whether datasources created by this factory support versioning or
	 * not
	 * @return true when versioning is supported, false if not
	 */
	public boolean isVersioning() {
		return versioning;
	}

	/**
	 * Check whether the datasources support caching and have to be checked
	 * frequently for background modifications (for example for
	 * contentrepositories that are written be Content.Node)
	 * @return true when sync check is enabled, false if not
	 */
	public boolean isCacheSyncCheck() {
		return ObjectTransformer.getBoolean(parameters.get("cache"), false) && ObjectTransformer.getBoolean(parameters.get("cache.syncchecking"), false);
	}

	/**
	 * Mark the datasource as valid/invalid.
	 * @param valid true if datasource is valid, false if not
	 */
	public void setValid(boolean valid) {
		this.valid = valid;
	}

	/**
	 * Check whether datasource is valid
	 * @return true if datasource is valid, false if not
	 */
	public boolean isValid() {
		return valid;
	}
}
