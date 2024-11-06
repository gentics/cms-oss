/*
 * @author herbert
 * @date 23.03.2006
 * @version $Id$
 */
package com.gentics.api.portalnode.connector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.lib.datasource.DatasourceException;
import com.gentics.api.lib.datasource.DatasourceHandle;
import com.gentics.api.lib.datasource.DatasourceNotAvailableException;
import com.gentics.api.lib.datasource.HandlePool;
import com.gentics.api.lib.datasource.MultichannellingDatasource;
import com.gentics.api.lib.datasource.WritableMultichannellingDatasource;
import com.gentics.api.lib.datasource.WriteableDatasource;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ParserException;
import com.gentics.api.lib.expressionparser.Expression;
import com.gentics.api.lib.expressionparser.ExpressionParser;
import com.gentics.api.lib.resolving.Changeable;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.api.lib.rule.RuleTree;
import com.gentics.lib.base.CMSUnavailableException;
import com.gentics.lib.base.NodeIllegalArgumentException;
import com.gentics.lib.content.DatatypeHelper;
import com.gentics.lib.content.GenticsContentFactory;
import com.gentics.lib.datasource.AbstractContentRepositoryStructure;
import com.gentics.lib.datasource.CNDatasource;
import com.gentics.lib.datasource.DatasourceFactory;
import com.gentics.lib.datasource.DatasourceFactoryImpl;
import com.gentics.lib.datasource.DatasourceSTRUCT;
import com.gentics.lib.datasource.RoundRobinHandlePool;
import com.gentics.lib.datasource.SQLHandle;
import com.gentics.lib.datasource.SimpleHandlePool;
import com.gentics.lib.datasource.mccr.MCCRDatasource;
import com.gentics.lib.datasource.mccr.MCCRHelper;
import com.gentics.lib.datasource.mccr.MCCRObject;
import com.gentics.lib.db.DBHandle;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.io.FileRemover;
import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.parser.rule.DefaultRuleTree;
import com.gentics.lib.scheduler.PausableScheduledThreadPoolExecutor;

/**
 * PortalConnectorFactory can be used to create instances for datasources,
 * RuleTrees and Resolvables.
 * @author herbert
 */
@SuppressWarnings("deprecation")
public final class PortalConnectorFactory {

	/**
	 * Don't allow any instances of this class...
	 */
	private PortalConnectorFactory() {}

	/**
	 * List holding all forbidden datasource ids
	 */
	private final static List<String> FORBIDDEN_DATASOURCE_IDS = Arrays.asList("false");

	/**
	 * Map of registered handles
	 */
	private static Map<String, DatasourceHandle> registeredHandles = Collections.synchronizedMap(new HashMap<String, DatasourceHandle>());

	/**
	 * Synchronization object for the handle registry
	 */
	private final static Object handleRegistrySync = new Object();

	/**
	 * Map of registered datasource factories
	 */
	private static Map<String, DatasourceFactory> registeredDatasources = Collections.synchronizedMap(new HashMap<String, DatasourceFactory>());

	/**
	 * Logger
	 */
	private static NodeLogger logger = NodeLogger.getNodeLogger(PortalConnectorFactory.class);

	/**
	 * Scheduler
	 */
	private static PausableScheduledThreadPoolExecutor scheduler;

	/**
	 * Registered Job Run Listeners
	 */
	private static Map<UUID, JobListener> jobRunListeners = new HashMap<>();

	/**
	 * Flag for the autorepair state
	 */
	private static boolean isAutoRepairEnabled = false;

	/**
	 * Flag for the autorepair2 state
	 */
	private static boolean isAutoRepair2Enabled = false;

	static {
		startScheduler();
	}

	/**
	 * Creates a new datasource handle with the specified properties as well as
	 * a new Datasource (with default properties).
	 * @param handleprops Handle properties used when initializing SQL handle.
	 * @return a new initialized Datasource
	 * @see Datasource
	 * @see #createDatasource(Map, Map)
	 */
	public static Datasource createDatasource(Map handleprops) {
		Map<String, String> dsprops = new HashMap<String, String>();

		dsprops.put("versioning", "false");
		return createDatasource(handleprops, dsprops);
	}

	/**
	 * Creates a new Datasource connection with the specified properties which
	 * should point to a Content Repository of Gentics Content.Node. For every
	 * different datasource (different handleproperties) a datasource handle
	 * will be created and reused with the given pooling settings.<br>
	 * Note: the returned instance of {@link Datasource} is NOT thread-safe.
	 * <br>
	 * 
	 * <pre>
	 *      Handle Property - Parameters:
	 *      type - Type of the datasource. (jndi or jdbc)
	 *      For JNDI:
	 *      name - The name of the defined JNDI datasource.
	 *      
	 *      For JDBC:
	 *      driverClass = The name of the JDBC class to be used (e.g. com.mysql.jdbc.Driver)
	 *      url = The URL which describes the JDBC datasource. (e.g. jdbc:mariadb://playground.office:3306/testdb?user=root)
	 *      
	 *      Optional JDBC Pool Parameters:
	 *      maxActive = Controls the maximum number of objects that can be borrowed from the pool at one time.
	 *      maxIdle = Controls the maximum number of objects that can sit idle in the pool at any time.
	 *      
	 *      for more options and more detailed documentation for JDBC Pool options
	 *      take a look at the API documentation of
	 *      {@link org.apache.commons.pool.impl.GenericObjectPool}
	 * </pre>
	 * 
	 * @param handleprops Handle properties used when initializing SQL handle.
	 * @param dsprops Datasource properties, may be an empty map.
	 * @return a new and initialized Datasource or null if one of the parameters
	 *         where null or the sanity check failed. Datasource may be unusable
	 *         (DatasourceNotAvailableException) if invalid properties where
	 *         given.
	 */
	public static Datasource createDatasource(Map handleprops, Map dsprops) {
		try {
			return createGenericDatasource(handleprops, dsprops, DatasourceType.contentrepository);
		} catch (NodeException e) {
			logger.error("Error while creating datasource", e);
			return null;
		}
	}

	/**
	 * Create a new instance of a multichannelling datasource
	 * @param handleProps handle properties
	 * @param dsProps datasource properties
	 * @return instance of the multichannelling datasource or null
	 */
	public static MultichannellingDatasource createMultichannellingDatasource(Map<String, String> handleProps, Map<String, String> dsProps) {
		try {
			return (MultichannellingDatasource) createGenericDatasource(handleProps, dsProps, DatasourceType.mccr);
		} catch (NodeException e) {
			logger.error("Error while creating datasource", e);
			return null;
		}
	}

	/**
	 * Returns a content object with the given content id.
	 * @param contentId The content id of the object which should be returned.
	 *        ([objecttype].[object id] e.g. 10002.123)
	 * @param datasource Datasource used to load the content object.
	 * @return the content object corresponding to the given content id. This
	 *         method returns null if datasource is null or contentid is null or
	 *         it doesn't exist or the syntax of the contentid is wrong.
	 * @throws DatasourceNotAvailableException when the datasource is not available
	 */
	public static Resolvable getContentObject(String contentId, Datasource datasource) throws DatasourceNotAvailableException {
		try {
			if (datasource instanceof MCCRDatasource) {
				MCCRDatasource ds = (MCCRDatasource) datasource;

				return ds.getObjectByContentId(contentId);
			} else {
				return GenticsContentFactory.createContentObject(contentId, datasource);
			}
		} catch (CMSUnavailableException e) {
			throw new DatasourceNotAvailableException(e.toString());
		} catch (NodeIllegalArgumentException e) {
			return null;
		} catch (DatasourceException e) {
			return null;
		}
	}

	/**
	 * Parse the given expression string into an Expression. You should reuse
	 * parsed Expressions whereever possible as well as reuse DatasourceFilters
	 * which are created from those expressions.
	 * @see Datasource#createDatasourceFilter(Expression)
	 * @param rule The Rule string to be parsed as Expression.
	 * @return the parsed Expression.
	 * @throws ParserException when creating the expression fails
	 */
	public static Expression createExpression(String rule) throws ParserException {
		return ExpressionParser.getInstance().parse(rule);
	}

	/**
	 * Creates a new RuleTree with the given rule string. Take care that you
	 * reuse parsed RuleTree's wherever possible by replacing the resolver.
	 * Parsing strings to RuleTrees is cpu expensive.
	 * @param rule The rule string which is used to initialize the RuleTree
	 * @return a new and initialized RuleTree to match the given rule string.
	 * @throws ParserException when rule has invalid syntax or is null.
	 * @see RuleTree
	 * @deprecated deprecated, replaced by {@link #createExpression(String)}
	 */
	public static com.gentics.api.lib.rule.RuleTree createRuleTree(String rule) throws ParserException {

		com.gentics.api.lib.rule.RuleTree ruleTree = new DefaultRuleTree();

		ruleTree.parse(rule);
		return ruleTree;
	}

	/**
	 * Creates a new datasource handle with the specified properties as well as
	 * a new writeable datasource (with default properties).
	 * @param handleProperties Handle properties used when initializing SQL
	 *        handle.
	 * @return The WriteableDatasource which might be used to read/write content
	 *         objects.
	 */
	public static WriteableDatasource createWriteableDatasource(Map handleProperties) {
		Map<String, String> dsprops = new HashMap<String, String>();

		dsprops.put("versioning", "false");
		return createWriteableDatasource(handleProperties, dsprops);
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
	public static WriteableDatasource createWriteableDatasource(Map handleProperties, Map datasourceProperties) {
		try {
			return (WriteableDatasource) createGenericDatasource(handleProperties, datasourceProperties, DatasourceType.contentrepository);
		} catch (NodeException e) {
			logger.error("Error while creating datasource", e);
			return null;
		}
	}

	/**
	 * Create a new writeable multichannelling datasource instance
	 * @param handleProps handle properties
	 * @param dsProps datasource properties
	 * @return instance of the datasource or null
	 */
	public static WritableMultichannellingDatasource createWritableMultichannellingDatasource(Map<String, String> handleProps,
			Map<String, String> dsProps) {
		try {
			return (WritableMultichannellingDatasource) createGenericDatasource(handleProps, dsProps, DatasourceType.mccr);
		} catch (NodeException e) {
			logger.error("Error while creating datasource", e);
			return null;
		}
	}

	/**
	 * return an instance of the given datasourceclass.
	 * @param handleProperties
	 * @param datasourceProperties
	 * @param clazz
	 * @return the new instance. or null if the sanity check failed
	 */
	private static Datasource createGenericDatasource(Map<String, String> handleProperties, Map<String, String> datasourceProperties, DatasourceType type)
			throws NodeException {
		// check parameters
		if (handleProperties == null || datasourceProperties == null) {
			return null;
		} else {
			String handleId = StringUtils.md5(handleProperties.toString());
			String dsId = StringUtils.md5(handleId + datasourceProperties.toString());
			boolean newHandle = false;

			if (!registeredHandles.containsKey(handleId)) {
				synchronized (handleRegistrySync) {
					if (!registeredHandles.containsKey(handleId)) {
						registerHandle(handleId, HandleType.sql, handleProperties);
						newHandle = true;
					}
				}
			}
			if (!registeredDatasources.containsKey(dsId)) {
				synchronized (registeredDatasources) {
					if (!registeredDatasources.containsKey(dsId)) {
						try {
							registerDatasource(dsId, type, datasourceProperties, Arrays.asList(handleId));
						} catch (NodeException e) {
							// if registering the datasource fails and the handle has been registered (just for this datasource)
							// we close and unregister the handle, so we don't have any unused handles left
							if (newHandle) {
								DatasourceHandle handle = registeredHandles.get(handleId);
								if (handle != null) {
									registeredHandles.remove(handleId);
									handle.close();
								}
							}

							throw e;
						}
					}
				}
			}

			return registeredDatasources.get(dsId).getInstance();
		}
	}

	/**
	 * Returns a changeable content object with the given content id.
	 * @param contentId The content id of the object which should be returned.
	 *        ([objecttype].[object id] e.g. 10002.123)
	 * @param datasource Writeable Datasource used to load the content object.
	 * @return the content object corresponding to the given content id.
	 * @throws DatasourceNotAvailableException when the datasource is not available
	 */
	public static Changeable getChangeableContentObject(String contentId,
			WriteableDatasource datasource) throws DatasourceNotAvailableException {
		return (Changeable) getContentObject(contentId, datasource);
	}

	/**
	 * Prefill the given attributes for objects which were fetched from the given datasource
	 * @param ds datasource
	 * @param objects collection of objects fetched from the datasource
	 * @param prefillAttributes list of attributes to be prefilled
	 * @throws NodeException when prefilling fails
	 */
	public static void prefillAttributes(Datasource ds, Collection<Resolvable> objects, List<String> prefillAttributes) throws NodeException {
		// edge case for no objects or no prefill attributes
		if (ObjectTransformer.isEmpty(objects) || ObjectTransformer.isEmpty(prefillAttributes)) {
			return;
		}
		// we support CNDatasource or MCCRDatasource
		if (ds instanceof CNDatasource) {
			GenticsContentFactory.prefillContentObjects(ds, objects, (String[]) prefillAttributes.toArray(new String[prefillAttributes.size()]), -1);
		} else if (ds instanceof MCCRDatasource) {
			List<MCCRObject> mccrObjects = new ArrayList<MCCRObject>(objects.size());

			for (Resolvable resolvable : objects) {
				if (resolvable instanceof MCCRObject) {
					mccrObjects.add((MCCRObject) resolvable);
				} else {
					if (resolvable == null) {
						logger.warn("Ignoring null object");
					} else {
						logger.warn("Ignoring invalid object of class " + resolvable.getClass().getName());
					}
				}
			}
			MCCRHelper.batchLoadAttributes((MCCRDatasource) ds, mccrObjects, prefillAttributes, true);
		} else if (ds == null) {
			throw new NodeException("Cannot prefill objects without datasource");
		} else {
			throw new NodeException("Cannot prefill objects for datasource of class " + ds.getClass().getName() + ".");
		}
	}

	/**
	 * Start the scheduler
	 */
	protected static synchronized void startScheduler() {
		if (scheduler != null) {
			return;
		}
		scheduler = new PausableScheduledThreadPoolExecutor(5);
	}

	/**
	 * Stop the scheduler. This method should be called before the factory is taken out of service
	 */
	protected static synchronized void stopScheduler() {
		if (scheduler == null) {
			return;
		}

		try {
			scheduler.shutdown();
			scheduler.awaitTermination(10, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			logger.error("Error while stopping scheduler.", e);
		}
		scheduler = null;
	}

	/**
	 * Destroy all created datasource factories, close all handles (closing database connections)
	 */
	protected static void destroyDatasourceFactories() {
		synchronized (registeredDatasources) {
			for (DatasourceFactory factory : registeredDatasources.values()) {
				factory.close();
			}
			registeredDatasources.clear();
		}

		synchronized (handleRegistrySync) {
			for (DatasourceHandle handle : registeredHandles.values()) {
				handle.close();
			}
			registeredHandles.clear();
		}
	}

	/**
	 * Destroy the portal connector factory, close all database connections, remove the scheduler (created background threads).
	 * This method must be called to take the PortalConnectorFactory out of service
	 */
	public static void destroy() {
		// shut down the fileremover
		FileRemover.shutdown();
		// stop the scheduler
		stopScheduler();
		// destroy all factories
		destroyDatasourceFactories();
	}

	/**
	 * Register a datasource handle at the factory.
	 * @param id id of the datasource handle
	 * @param type handle type
	 * @param parameters handle parameters
	 * @throws DuplicateIdException if a handle with that id was already registered
	 * @throws NodeException if the parameters are invalid
	 */
	public static void registerHandle(String id, HandleType type, Map<String, String> parameters) throws DuplicateIdException, NodeException {
		registerHandle(id, type, parameters, false);
	}

	/**
	 * Register a datasource handle at the factory. With this method, it is possible to re-register an existing handle with different parameters
	 * @param id id of the datasource handle
	 * @param type handle type
	 * @param parameters handle parameters
	 * @param allowOverwrite true if overwriting existing handles should be allowed
	 * @throws DuplicateIdException if a handle with that id was already registered and allowOverwrite was false
	 * @throws NodeException if the parameters are invalid
	 */
	public static void registerHandle(String id, HandleType type, Map<String, String> parameters, boolean allowOverwrite) throws DuplicateIdException,
			NodeException {
		synchronized (handleRegistrySync) {
			// validate input data
			if (ObjectTransformer.isEmpty(id)) {
				throw new NodeException("Cannot register handle with empty ID");
			}
			if (type == null) {
				throw new NodeException("Cannot register handle without a type");
			}
			if (parameters == null) {
				parameters = Collections.emptyMap();
			}
			DatasourceHandle existingHandle = registeredHandles.get(id);
			Class<? extends DatasourceHandle> handleClass = getImplementationClass(type);

			if (existingHandle != null) {
				if (allowOverwrite) {
					if (!handleClass.equals(existingHandle.getClass())) {
						throw new NodeException("Error while re-registering handle with id " + id + ": Existing handle is of different type");
					}
				} else {
					throw new DuplicateIdException("Cannot register handle with id " + id + ": Handle already registered");
				}
			}

			// create the instance
			try {
				DatasourceHandle newHandle = handleClass.getConstructor(String.class).newInstance(id);
				newHandle.init(parameters);
				// TODO what about the datasource definition?
				registeredHandles.put(id, newHandle);
			} catch (Exception e) {
				throw new NodeException("Error while registering handle " + id, e);
			}
		}
	}

	/**
	 * Unregister (and close) currently unused handles
	 */
	public static void unregisterUnusedHandles() {
		Set<String> allUsedHandles = new HashSet<>();
		List<DatasourceHandle> toClose = new ArrayList<>();
		synchronized (handleRegistrySync) {
			// collect all handle IDs that are currently in use
			for (DatasourceFactory dsFactory : registeredDatasources.values()) {
				allUsedHandles.addAll(dsFactory.getHandleIds());
			}

			// collect all registered handles, that are not in use and remove their entries from the registry
			for (Iterator<Map.Entry<String, DatasourceHandle>> i = registeredHandles.entrySet().iterator(); i.hasNext(); ) {
				Entry<String, DatasourceHandle> handleEntry = i.next();
				if (!allUsedHandles.contains(handleEntry.getKey())) {
					toClose.add(handleEntry.getValue());
					i.remove();
				}
			}
		}

		// finally close all handles
		for (DatasourceHandle handle : toClose) {
			handle.close();
		}
	}

	/**
	 * Register a datasource at the factory.
	 * @param id datasource id
	 * @param type datasource type
	 * @param parameters datasource parameters
	 * @param handles list of datasource handle ids
	 * @throws DuplicateIdException if a datasource with that id was already registered
	 * @throws NodeException when registering fails
	 */
	public static void registerDatasource(String id, DatasourceType type, Map<String, String> parameters, List<String> handles) throws DuplicateIdException,
			NodeException {
		registerDatasource(id, type, parameters, handles, false);
	}

	/**
	 * Register a datasource at the factory.
	 * @param id datasource id
	 * @param type datasource type
	 * @param parameters datasource parameters
	 * @param handleIds list of datasource handle ids
	 * @param allowOverwrite true if overwriting existing datasources should be allowed, false if not
	 * @throws DuplicateIdException if a datasource with that id was already registered and allowOverwrite was false
	 * @throws NodeException when registering fails
	 */
	public static void registerDatasource(String id, DatasourceType type, Map<String, String> parameters, List<String> handleIds, boolean allowOverwrite)
			throws DuplicateIdException, NodeException {
		synchronized (registeredDatasources) {

			// validate input data
			if (ObjectTransformer.isEmpty(id)) {
				throw new NodeException("Cannot register datasource with empty id");
			}
			if (FORBIDDEN_DATASOURCE_IDS.contains(id)) {
				throw new NodeException("Cannot register datasource with id '" + id + "'");
			}
			if (type == null) {
				throw new NodeException("Cannot register datasource without a type");
			}
			if (ObjectTransformer.isEmpty(handleIds)) {
				throw new NodeException("Cannot register datasource without handles");
			}
			if (parameters == null) {
				parameters = Collections.emptyMap();
			}

			// create datasource factory
			DatasourceSTRUCT ds = new DatasourceSTRUCT();
			ds.ID = id;
			ds.typeID = type.toString();
			ds.parameterMap = parameters;
			DatasourceFactoryImpl factory = new DatasourceFactoryImpl(ds);

			// the old sanitycheck is only on be default for non mccr contentrepositories
			boolean oldSanitycheckDefault = (type == DatasourceType.contentrepository);
			if (type == DatasourceType.mccr
					&& ObjectTransformer.getBoolean(
							ds.parameterMap.get("sanitycheck"),
							oldSanitycheckDefault)) {
				// if the old sanitycheck is requested for mccr datasources, we print a warning and switch it off
				logger.warn("sanitycheck is not supported by mccr datasources. Consider using sanitycheck2 for datasource with id " + id);
				ds.parameterMap.remove("sanitycheck");
			}

			try {
				// first of all, get the datasource class
				Class<?> datasourceClass = factory.getDatasourceClass();

				if (datasourceClass == null) {
					throw new NodeException("Error while registering datasource with id " + id + ": Could not find implementation class for type " + type);
				}

				// check for re-registering
				DatasourceFactory existingFactory = registeredDatasources.get(id);
				if (existingFactory != null) {
					if (allowOverwrite) {
						if (!datasourceClass.equals(existingFactory.getDatasourceClass())) {
							throw new NodeException("Error while re-registering datasource with id " + id + ": Existing datasource is of different type");
						}
					} else {
						throw new DuplicateIdException("Cannot register datasource with id " + id + ": Datasource already registered");
					}
				}

				// now check and set the handles
				List<DatasourceHandle> handles = new ArrayList<DatasourceHandle>();
				boolean versioning = factory.isVersioning();

				Set<String> usedHandleIds = new HashSet<>();
				for (String handleId : handleIds) {
					DatasourceHandle handle = registeredHandles.get(handleId);

					if (handle == null) {
						logger.error(
								"Error while initializing datasource {" + ds.ID + "}: datasource handle {" + handleId
								+ "} does not exist. Existing datasource-handles: {" + registeredHandles.keySet() + "}");
					} else if (!isCompatible(handleId.toString(), datasourceClass, handle,
							ObjectTransformer.getBoolean(ds.parameterMap.get("sanitycheck"), oldSanitycheckDefault),
							ObjectTransformer.getBoolean(ds.parameterMap.get("autorepair"), null), ds.parameterMap,
							ObjectTransformer.getBoolean(ds.parameterMap.get("sanitycheck2"), false),
							ObjectTransformer.getBoolean(ds.parameterMap.get("autorepair2"), null), versioning)) {
						logger.error(
								"Error while initializing datasource {" + ds.ID + "}: datasource handle {" + handleId
								+ "} is not compatible with the Datasource");
					} else {
						// the handle is ok and is added to the list
						if (logger.isInfoEnabled()) {
							logger.info("Adding datasource-handle {" + handleId + "} to datasource {" + ds.ID + "}");
						}
						handles.add(handle);
						usedHandleIds.add(handleId);
					}
				}

				// check whether at least one handle was found for the datasource
				if (handles.size() > 0) {
					boolean backgroundValidation = ObjectTransformer.getBoolean(ds.parameterMap.get("backgroundvalidation"), true);
					int backgroundValidationInterval = ObjectTransformer.getInt(ds.parameterMap.get("backgroundvalidation.interval"),
							RoundRobinHandlePool.BACKGROUND_VALIDATION_INTERVAL_DEFAULT);

					HandlePool pool = null;
					if (handles.size() == 1) {
						pool = new SimpleHandlePool(handles.get(0));
					} else {
						pool = new RoundRobinHandlePool((DatasourceHandle[]) handles.toArray(new DatasourceHandle[handles.size()]), backgroundValidation,
								backgroundValidationInterval);
					}
					factory.setHandlePool(pool, usedHandleIds);
					registeredDatasources.put(id, factory);

					// get the scheduler from the portal and let the factory add jobs
					// (if needed)
					startScheduler();
					factory.scheduleJobs(scheduler);

					if (logger.isInfoEnabled()) {
						logger.info("Datasource {" + id + "} successfully configured and ready to use.");
					}
				} else {
					throw new NodeException("Failed to initialize datasource {" + id + "}: No valid handles found. Datasource will not be available.");
				}
			} catch (ClassNotFoundException e) {
				throw new NodeException("Failed to initialize datasource {" + ds.ID + "}. Datasource will not be available.", e);
			}
		}
	}

	/**
	 * Get the implementation class for the given handle type
	 * @param type handle type
	 * @return implementation class
	 * @throws NodeException
	 */
	private static Class<? extends DatasourceHandle> getImplementationClass(HandleType type) throws NodeException {
		switch (type) {
		case sql:
			return SQLHandle.class;
		default:
			throw new NodeException("Unknown handle type " + type);
		}
	}

	/**
	 * Check whether the configured combination of datasource class and
	 * datasource handle is compatible
	 * @param handleId id of the handle
	 * @param datasourceClass datasource class
	 * @param handle datasource handle
	 * @param doDeepTest true when deep tests shall be done, false for no deep
	 *        tests
	 * @param autoRepairDatasource true when the handle shall be repaired
	 *        automatically, false if not or null when the default behaviour of
	 *        the portal shall determine autoRepair
	 * @param parameterMap parameter map
	 * @param sanitycheck2 true if extended checks should be done, false if not
	 * @param autorepair2 true if extended repair should be enabled, false if not
	 * @param versioning true if datasource supports versioning, false if not
	 * @return true when the factory and handle are compatible, false if not
	 */
	private final static boolean isCompatible(String handleId, Class<?> datasourceClass, DatasourceHandle handle, boolean doDeepTest,
			Boolean autoRepairDatasource, Map<String, String> parameterMap, boolean sanitycheck2, Boolean autorepair2, boolean versioning) {
		if (CNDatasource.class.isAssignableFrom(datasourceClass) || MCCRDatasource.class.isAssignableFrom(datasourceClass)) {
			// CNDatasource requires SQLHandles
			if (!(handle instanceof SQLHandle)) {
				return false;
			} else if (((SQLHandle) handle).getDBHandle() == null) {
				logger.error("The handle {" + handleId + "} does not seem to be properly configured.");
				return false;
			} else {
				// set the configured contentrepository table names
				DBHandle dbHandle = ((SQLHandle) handle).getDBHandle();

				try {
					dbHandle.setTableNames(ObjectTransformer.getString(parameterMap.get("table.contentstatus"), null),
							ObjectTransformer.getString(parameterMap.get("table.contentobject"), null),
							ObjectTransformer.getString(parameterMap.get("table.contentattributetype"), null),
							ObjectTransformer.getString(parameterMap.get("table.contentmap"), null),
							ObjectTransformer.getString(parameterMap.get("table.contentattribute"), null), null);
				} catch (DatasourceException e) {
					logger.error("Error in the customized table configuration for handle {" + handleId + "}", e);
					return false;
				}

				boolean consistencyCheck = true;

				// only do sanitycheck if sanitycheck2 is disabled
				if (doDeepTest && !sanitycheck2) {
					try {
						boolean contentRepositoryCheck = DatatypeHelper.checkContentRepository(handleId, dbHandle,
								autoRepairDatasource != null ? autoRepairDatasource.booleanValue() : isAutoRepair());

						if (!contentRepositoryCheck) {
							logger.error(
									"The database for the handle {" + handleId
									+ "} does not have the needed database structure and could not be repaired automatically!"
									+ "\nCheck the tables manually and eventually check the changelog of your current buld for more information about changes in the contentrepository.");
						}
						consistencyCheck &= contentRepositoryCheck;
					} catch (CMSUnavailableException e) {
						logger.fatal("Error while checking handle {" + handleId + "}", e);
						return false;
					}
				}
				if (sanitycheck2) {
					try {
						AbstractContentRepositoryStructure checkStructure = AbstractContentRepositoryStructure.getStructure(dbHandle, handleId, versioning,
								MCCRDatasource.class.isAssignableFrom(datasourceClass));
						boolean autorepair = autorepair2 != null ? autorepair2.booleanValue() : isAutoRepair2();
						boolean check = checkStructure.checkStructureConsistency(autorepair);

						if (!check) {
							return false;
						}
						check &= checkStructure.checkDataConsistency(autorepair);
						consistencyCheck &= check;
					} catch (CMSUnavailableException cmsue) {
						logger.fatal("Error while checking handle {" + handleId + "}", cmsue);
						return false;
					}
				}
				return consistencyCheck;
			}
		} else {
			// all other datasources cannot be checked
			return true;
		}
	}

	/**
	 * Check whether the auto-repair functionality is switched on or off by
	 * configuration. Default is off
	 * @return true when auto-repair is on, false if not
	 */
	public static boolean isAutoRepair() {
		return isAutoRepairEnabled;
	}

	/**
	 * Set the autorepair flag
	 * @param flag true to enable auto repair
	 */
	public static void setAutoRepair(boolean flag) {
		isAutoRepairEnabled = flag;
	}

	/**
	 * Check whether the auto-repair2 functionality is switched on or off by
	 * configuration. Default is off
	 * @return true when auto-repair2 is on, false if not
	 */
	public static boolean isAutoRepair2() {
		return isAutoRepair2Enabled;
	}

	/**
	 * Set the autorepair2 flag
	 * @param flag true to enable auto repair 2
	 */
	public static void setAutoRepair2(boolean flag) {
		isAutoRepair2Enabled = flag;
	}

	/**
	 * Get the list of all available datasource ids
	 * @return lsit of available datasource ids
	 */
	public static List<String> getAvailableDatasources() {
		return getAvailableDatasources(null);
	}

	/**
	 * Get the available datasources as list of ids
	 * @param datasourceClass datasource class. If null, all available datasources will be returned
	 * @return list of available datasource ids
	 */
	public static List<String> getAvailableDatasources(Class< ? extends Datasource> datasourceClass) {
		List<String> availableIds = new ArrayList<String>();
		if (datasourceClass == null) {
			// return all datasource ids
			availableIds.addAll(registeredDatasources.keySet());
		} else {
			for (Map.Entry<String, DatasourceFactory> entry : registeredDatasources.entrySet()) {
				String id = entry.getKey();
				DatasourceFactory factory = entry.getValue();
				try {
					if (datasourceClass.isAssignableFrom(factory.getDatasourceClass())) {
						availableIds.add(id);
					}
				} catch (ClassNotFoundException ignored) {}
			}
		}

		return availableIds;
	}

	/**
	 * Create a datasource instance for the datasource with given id
	 * @param id datasource id
	 * @return datasource instance (never null)
	 * @throws NodeException if no datasource with given id was registered
	 */
	public static Datasource createDatasource(String id) throws NodeException {
		return createDatasource(Datasource.class, id);
	}

	/**
	 * Create a datasource instance of the given class with given id
	 * @param <T> Datasource implementation class
	 * @param clazz datasource class
	 * @param id datasource id
	 * @return datasource instance
	 * @throws NodeException if no datasource with given id was registered or if the datasource does not implement the given class
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Datasource> T createDatasource(Class<T> clazz, String id) throws NodeException {
		if (ObjectTransformer.isEmpty(id)) {
			throw new NodeException("Cannot get datasource with empty id");
		}
		if (clazz == null) {
			throw new NodeException("Cannot get datasource without class");
		}
		DatasourceFactory factory = registeredDatasources.get(id);

		if (factory == null) {
			throw new NodeException("No datasource registered with id {" + id + "}");
		}

		Datasource dataSource = factory.getInstance();

		try {
			return (T)dataSource;
		} catch (ClassCastException e) {
			throw new NodeException("Datasource " + id + " does not implement requested " + clazz);
		}
	}

	/**
	 * Let registered job listeners handle a job run
	 * @param clazz class of the job
	 * @param name name of the job
	 * @param e exception (if the job had an error)
	 */
	public static void handleJobRun(Class<? extends Runnable> clazz, String name, Throwable e) {
		for (JobListener listener : new ArrayList<>(jobRunListeners.values())) {
			listener.handle(clazz, name, e);
		}
	}

	/**
	 * Register a job listener
	 * @param listener job listener
	 * @return UUID of the registry
	 */
	public static UUID registerJobListener(JobListener listener) {
		UUID uuid = UUID.randomUUID();
		jobRunListeners.put(uuid, listener);
		return uuid;
	}

	/**
	 * Unregister a job listener
	 * @param uuid UUID of the registry
	 */
	public static void unregisterJobListener(UUID uuid) {
		jobRunListeners.remove(uuid);
	}

	/**
	 * Pause the scheduler for background jobs
	 */
	public static void pauseScheduler() {
		if (scheduler != null) {
			scheduler.pause();
			scheduler.await(10000);
		}
	}

	/**
	 * Resume the scheduler for background jobs
	 */
	public static void resumeScheduler() {
		if (scheduler != null && scheduler.isPaused()) {
			scheduler.resume();
		}
	}

	/**
	 * Interface for JobListeners
	 */
	public static interface JobListener {
		/**
		 * Handle a job run
		 * @param clazz job class
		 * @param name job name
		 * @param e exception (if the job had an error)
		 */
		void handle(Class<? extends Runnable> clazz, String name, Throwable e);
	}
}
