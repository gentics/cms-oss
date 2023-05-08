/*
 * @author johannes2
 * @date 17.07.2008
 * @version $Id: ExtendedPortalConnectorFactory.java,v 1.1 2010-02-04 14:25:06 norbert Exp $
 */
package com.gentics.portalconnector.tests;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.lib.datasource.DatasourceException;
import com.gentics.api.lib.datasource.WriteableDatasource;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.lib.base.CMSUnavailableException;
import com.gentics.lib.content.DatatypeHelper;
import com.gentics.lib.datasource.AbstractContentRepositoryStructure;
import com.gentics.lib.datasource.CNWriteableDatasource;
import com.gentics.lib.datasource.DatasourceFactoryImpl;
import com.gentics.lib.datasource.DatasourceSTRUCT;
import com.gentics.lib.datasource.SQLHandle;
import com.gentics.lib.datasource.SimpleHandlePool;
import com.gentics.lib.db.DBHandle;
import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.scheduler.PausableScheduledThreadPoolExecutor;

/**
 * Class can be used to create instances for datasources,
 * @author johannes2
 */
public final class ExtendedPortalConnectorFactory {

	/**
	 * Don't allow any instances of this class...
	 */
	private ExtendedPortalConnectorFactory() {}

	/**
	 * datasource id used when creating a new SQL handle.
	 */
	private static final String DEFAULT_HANDLE_ID = "default";

	/**
	 * map of active handles.
	 * @see #createDatasource(Map, Map).
	 */
	private static Map activeHandles = new HashMap();

	/**
	 * map containing all used datasourceFactories ..
	 */
	private static Map datasourceFactories = new HashMap();

	private static NodeLogger logger = NodeLogger.getNodeLogger(ExtendedPortalConnectorFactory.class);

	private static PausableScheduledThreadPoolExecutor scheduler;

	static {
		scheduler = new PausableScheduledThreadPoolExecutor(5);
	}

	private static String createUniqueId(Map handleprops, Map dsprops, Class clazz) {
		return new StringBuffer(handleprops.toString()).append('|').append(dsprops.toString()).append('|').append(clazz.getName()).toString();
	}

	/**
	 * Creates a new datasource handle with the specified properties as well as
	 * a new writeable datasource with the specified properties.
	 * @param handleProperties Handle properties used when initializing SQL
	 *        handle.
	 * @param datasourceProperties Datasource properties, may be an empty map.
	 * @return The WriteableDatasource which might be used to read/write content
	 *         objects or null if the sanity check failed
	 */
	public static WriteableDatasource createWriteableDatasource(Map handleProperties,
			Map datasourceProperties) {
		return (WriteableDatasource) createGenericDatasource(handleProperties, datasourceProperties, CNWriteableDatasource.class);
	}

	public static void clearHandles() {
		// activeHandles = new HashMap();
		activeHandles.clear();
	}

	public static void clearDatasourceFactories() {
		// datasourceFactories = new HashMap();
		datasourceFactories.clear();
	}

	/**
	 * return an instance of the given datasourceclass.
	 * @param handleProperties
	 * @param datasourceProperties
	 * @param clazz
	 * @return the new instance. or null if the sanity check failed
	 */
	private static Datasource createGenericDatasource(Map handleProperties,
			Map datasourceProperties, Class clazz) {
		SQLHandle handle;

		// check parameters
		if (handleProperties == null || datasourceProperties == null) {
			return null;
		} else {
			boolean sanityCheck = ObjectTransformer.getBoolean(datasourceProperties.get("sanitycheck"), true);
			boolean autoRepair = ObjectTransformer.getBoolean(datasourceProperties.get("autorepair"), true);
			boolean sanityCheck2 = ObjectTransformer.getBoolean(datasourceProperties.get("sanitycheck2"), false);
			boolean autoRepair2 = ObjectTransformer.getBoolean(datasourceProperties.get("autorepair2"), false);

			String handlekey = handleProperties.toString();

			// lookup existing handles in acvtivehandles map
			synchronized (activeHandles) {

				handle = (SQLHandle) activeHandles.get(handlekey);
				// not found, create new handle
				if (handle == null) {
					handle = new SQLHandle(DEFAULT_HANDLE_ID);
					handle.init(handleProperties);

					// set the configured contentrepository table names
					DBHandle dbHandle = ((SQLHandle) handle).getDBHandle();

					try {
						dbHandle.setTableNames(ObjectTransformer.getString(datasourceProperties.get("table.contentstatus"), null),
								ObjectTransformer.getString(datasourceProperties.get("table.contentobject"), null),
								ObjectTransformer.getString(datasourceProperties.get("table.contentattributetype"), null),
								ObjectTransformer.getString(datasourceProperties.get("table.contentmap"), null),
								ObjectTransformer.getString(datasourceProperties.get("table.contentattribute"), null),
								ObjectTransformer.getString(datasourceProperties.get("table.channel"), null));
					} catch (DatasourceException e) {
						logger.error("Error in the customized table configuration", e);
						return null;
					}

					// when the datasource shall be checked for sanity, we do it
					// now
					// do not use sanitycheck if sanitycheck2 is enabled
					if (sanityCheck && !sanityCheck2) {
						try {
							if (!DatatypeHelper.checkContentRepository("pc_handle", handle.getDBHandle(), autoRepair)) {
								return null;
							}
						} catch (CMSUnavailableException e) {
							logger.error("Error while checking datasource for sanity", e);
							return null;
						}
					}

					activeHandles.put(handlekey, handle);
				}
			}

			// Find DatasourceFactory
			DatasourceFactoryImpl datasourceFactory = null;
			String id = createUniqueId(handleProperties, datasourceProperties, clazz);
			boolean createdFactory = false;

			// synchronize to prevent multiple creation of same factory
			synchronized (datasourceFactories) {
				datasourceFactory = (DatasourceFactoryImpl) datasourceFactories.get(id);

				// not found, create new factory
				if (datasourceFactory == null) {
					DatasourceSTRUCT struct = new DatasourceSTRUCT();

					struct.ID = id;
					struct.typeID = clazz.getName();
					struct.parameterMap = datasourceProperties;
					datasourceFactory = new DatasourceFactoryImpl(struct);
					datasourceFactories.put(id, datasourceFactory);
					datasourceFactory.setHandlePool(new SimpleHandlePool(handle), Collections.singleton(handlekey));
					// we just created a new factory
					createdFactory = true;
				}
			}

			Datasource ds = datasourceFactory.getInstance();

			// synchronize to prevent multiple parallel sanity checks
			synchronized (datasourceFactory) {
				// only do sanity checks once
				if (createdFactory) {
					// do sanitycheck2 and autorepair2
					if (sanityCheck2) {
						try {
							AbstractContentRepositoryStructure checkStructure = AbstractContentRepositoryStructure.getStructure(ds, "pc_handle");
							boolean check = checkStructure.checkStructureConsistency(autoRepair2);

							if (!check) {
								datasourceFactory.setValid(false);
							} else {
								check &= checkStructure.checkDataConsistency(autoRepair2);
								if (!check) {
									datasourceFactory.setValid(false);
								}
							}
						} catch (CMSUnavailableException cmsue) {
							logger.error("Error while checking datasource for sanity", cmsue);
							datasourceFactory.setValid(false);
						}
					}
					// for valid datasource factories, schedule background jobs
					if (datasourceFactory.isValid()) {
						datasourceFactory.scheduleJobs(scheduler);
					}
				}
				// for invalid datasource factories, do not return datasources
				if (!datasourceFactory.isValid()) {
					ds = null;
				}
			}
			return ds;
		}
	}

}
