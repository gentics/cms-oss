/*
 * @author Stefan Hepp
 * @date 22.1.2006
 * @version $Id: NodeFactory.java,v 1.42.2.3 2011-04-07 09:57:52 norbert Exp $
 */
package com.gentics.contentnode.factory;

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

import com.gentics.api.lib.cache.PortalCache;
import com.gentics.api.lib.cache.PortalCacheException;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.InconsistentDataException;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.contentnode.cache.CacheService;
import com.gentics.contentnode.distributed.DistributionUtil;
import com.gentics.contentnode.etc.NodeConfig;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.events.QueueEntry;
import com.gentics.contentnode.factory.TransactionStatistics.Item;
import com.gentics.contentnode.factory.object.AbstractFactory.FactoryDataField;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentFile;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.NodeObjectInfo;
import com.gentics.contentnode.object.NodeObjectVersion;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.PublishableNodeObject;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.publish.wrapper.PublishablePage;
import com.gentics.contentnode.publish.wrapper.PublishableTemplate;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.log.RuntimeProfiler;
import com.gentics.lib.log.profilerconstants.JavaParserConstants;

/**
 * This is the main factory implementation.
 * TODO refactor methods to get the FactoryConnection as additional parameter
 */
public class NodeFactory {

	/**
	 * Name of the trx attribute that defines, whether objects shall be unlocked on transaction commit
	 */
	public final static String UNLOCK_AT_TRX_COMMIT = "NodeFactory.unlockAtTrxCommit";

	private static NodeFactory instance;

	/**
	 * Loader for instances of {@link CacheService}
	 */
	protected static ServiceLoader<CacheService> cacheServiceLoader = ServiceLoader.load(CacheService.class);

	private Map<Class<? extends NodeObject>, ObjectFactory> factoryMap;

	/**
	 * Map of tablenames to classes
	 */
	private Map<String, Class<? extends NodeObject>> table2Class = new HashMap<String, Class<? extends NodeObject>>();

	/**
	 * Map of classes to tablenames
	 */
	private Map<Class<? extends NodeObject>, String> class2Table = new HashMap<Class<? extends NodeObject>, String>();

	private Set<ObjectFactory> factorySet;
	private Map<Class<? extends NodeObject>, Set<PreloadableObjectFactory>> preloaderMap;
	private List<Integer> typeList;
	private List<Class<? extends NodeObject>> classList;
	private Map<Class<? extends NodeObject>, Map<String, DataFieldHandler>> dataFieldHandlers = new HashMap<Class<? extends NodeObject>, Map<String, DataFieldHandler>>();
	private String factoryKeyname;
	private boolean initialized = false;
	private boolean forcedInstance;

	private PortalCache cache = null;

	/**
	 * Separate caches for object types
	 */
	private Map<Class<? extends NodeObject>, PortalCache> typeCaches = new HashMap<>();

	public final static String CACHEREGION = "gentics-nodeobjects";

	/**
	 * logger
	 */
	private NodeLogger logger = NodeLogger.getNodeLogger(getClass());

	/**
	 * default number of seconds, the dirtqueue thread will wait when nothing to
	 * do (or publish is running)
	 */
	private final static int DIRTQUEUE_THREAD_DEFAULTWAIT = 10;

	/**
	 * Background Thread that handles dirtqueue entries
	 */
	private TriggerEventJobThread dirtQueueWorkerThread;

	/**
	 * The factoryhandle implementation which is passed to the objectfactories. The handle
	 * references to the current nodefactory instance.
	 */
	private class NodeFactoryHandle implements FactoryHandle {
		private NodeFactoryHandle() {}

		public NodeFactory getFactory() {
			return NodeFactory.this;
		}

		public void putObject(Class<? extends NodeObject> clazz, NodeObject obj, int versionTimestamp) throws NodeException {
			NodeFactory.this.putObject(clazz, obj, versionTimestamp);
		}

		/*
		 * (non-Javadoc)
		 * @see com.gentics.lib.base.factory.FactoryHandle#getObjectFactory(java.lang.Class)
		 */
		public ObjectFactory getObjectFactory(Class<? extends NodeObject> clazz) {
			return NodeFactory.this.getObjectFactory(clazz);
		}

		/*
		 * (non-Javadoc)
		 * @see com.gentics.lib.base.factory.FactoryHandle#removeDeleteLists(com.gentics.lib.base.factory.Transaction)
		 */
		public void removeDeleteLists(Transaction t) {
			NodeFactory.this.removeDeleteLists(t);
		}

		/*
		 * (non-Javadoc)
		 * @see com.gentics.lib.base.factory.FactoryHandle#flushAll()
		 */
		public void flushAll() throws NodeException {
			NodeFactory.this.flushAll();
		}

		public <T extends NodeObject> T createObject(Class<T> clazz) throws NodeException {
			return NodeFactory.this.createObject(clazz);
		}

		public NodeObjectInfo createObjectInfo(Class<? extends NodeObject> clazz,
				int versionTimestamp) {
			return NodeFactory.this.createObjectInfo(clazz, false, versionTimestamp);
		}

		public NodeObjectInfo createObjectInfo(Class<? extends NodeObject> clazz, boolean editable) {
			return NodeFactory.this.createObjectInfo(clazz, editable, -1);
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.factory.FactoryHandle#getObject(java.lang.Class, java.lang.Integer, boolean, int, boolean, boolean)
		 */
		public <T extends NodeObject> T getObject(Class<T> clazz, Integer id,
				boolean forUpdate, int versionTimestamp, boolean multichannelFallback, boolean logErrorIfNotFound) throws NodeException, ReadOnlyException {
			return NodeFactory.this.getObject(clazz, id, forUpdate, versionTimestamp, multichannelFallback, logErrorIfNotFound);
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.FactoryHandle#getClass(int)
		 */
		public Class<? extends NodeObject> getClass(int objType) {
			return NodeFactory.this.getClass(objType);
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.FactoryHandle#getClass(java.lang.String)
		 */
		public Class<? extends NodeObject> getClass(String tableName) {
			return NodeFactory.this.getClass(tableName);
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.FactoryHandle#getTable(java.lang.Class)
		 */
		public String getTable(Class<? extends NodeObject> clazz) {
			return NodeFactory.this.getTable(clazz);
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.FactoryHandle#getTType(java.lang.Class)
		 */
		public int getTType(Class<? extends NodeObject> clazz) {
			return NodeFactory.this.getTType(clazz);
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.FactoryHandle#dirtObjectCache(java.lang.Class, java.lang.Integer)
		 */
		public void dirtObjectCache(Class<? extends NodeObject> clazz, Integer id) throws NodeException {
			NodeFactory.this.dirtObjectCache(clazz, id);
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.FactoryHandle#getFieldData(com.gentics.lib.base.object.NodeObject)
		 */
		public Map<String, Object> getFieldData(NodeObject object) throws NodeException {
			if (object == null) {
				throw new NodeException("Cannot read export data from null object");
			}
			Map<String, DataFieldHandler> map = dataFieldHandlers.get(object.getObjectInfo().getObjectClass());

			if (map == null) {
				throw new NodeException("Cannot read export data from " + object + " not field information found");
			}
			Map<String, Object> dataMap = new HashMap<String, Object>(map.size());

			for (Map.Entry<String, DataFieldHandler> fieldInfo : map.entrySet()) {
				dataMap.put(fieldInfo.getKey(), fieldInfo.getValue().get(object));
			}
			return dataMap;
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.FactoryHandle#setFieldData(com.gentics.lib.base.object.NodeObject, java.util.Map)
		 */
		public void setFieldData(NodeObject object, Map<String, Object> dataMap) throws NodeException {
			if (object == null) {
				throw new NodeException("Cannot set import data to null object");
			}

			if (dataMap == null) {
				return;
			}

			Map<String, DataFieldHandler> map = dataFieldHandlers.get(object.getObjectInfo().getObjectClass());

			if (map == null) {
				throw new NodeException("Cannot set import data to " + object + " not field information found");
			}

			for (Map.Entry<String, Object> data : dataMap.entrySet()) {
				DataFieldHandler fieldHandler = map.get(data.getKey());

				if (fieldHandler != null) {
					fieldHandler.set(object, data.getValue());
				}
			}
		}

		@Override
		public <T extends NodeObject> List<T> getObjects(Class<T> clazz, Collection<Integer> ids, boolean forUpdate, int versionTimestamp, boolean allowMultichannellingFallback)
				throws NodeException, ReadOnlyException {
			return NodeFactory.this.getObjects(clazz, ids, forUpdate, versionTimestamp, allowMultichannellingFallback);
		}
	}

	private NodeFactory(boolean forcedInstance) {
		this.forcedInstance = forcedInstance;
		factoryMap = new LinkedHashMap<Class<? extends NodeObject>, ObjectFactory>(10);
		factorySet = new LinkedHashSet<ObjectFactory>(10);
		preloaderMap = new HashMap<Class<? extends NodeObject>, Set<PreloadableObjectFactory>>(5);
		typeList = new ArrayList<Integer>(20);
		classList = new ArrayList<Class<? extends NodeObject>>(20);
		initialized = false;
		try {
			cache = PortalCache.getCache(CACHEREGION);
		} catch (PortalCacheException e) {
			logger.error("Error while initializing cache for region {" + CACHEREGION + "}, will not use object cache", e);
		}
	}

	/**
	 * Get an instance of NodeFactory
	 * @return instance of NodeFactory
	 */
	public synchronized static NodeFactory getInstance() {
		if (instance == null) {
			instance = new NodeFactory(true);
		}

		return instance;
	}

	/**
	 * check if this factory instance has been inizialized.
	 * @return true, if initialized and ready to use, else false.
	 */
	public boolean isInitialized() {
		return initialized;
	}

	/**
	 * initialize this factory, its cache and other things, using the given
	 * configuration. The configuration will be used for all other configuration
	 * requests of the factory and will be given to all nodeobjectinfos of
	 * objects created with this factory.
	 */
	public void initialize() {
		// start the job that triggers dirt event in the background (if not testing)
		initialized = true;
		if (!ObjectTransformer.getBoolean(System.getProperty("com.gentics.contentnode.testmode"), false)) {
			try {
				startDirtQueueWorker();
				Session.scheduleSessionCleaning();
			} catch (NodeException e) {
				logger.error("Error while initializing the factory", e);
			}
		}
	}

	/**
	 * Reload the configuration of all factories in the factoryMap
	 */
	public void reloadConfiguration() {
		for (ObjectFactory factory : factorySet) {
			factory.reloadConfiguration();
		}
	}

	/**
	 * Returns an objectFactory from the factoryMap.
	 * @param clazz The class for which the factory should be returned
	 * @return The factory
	 */
	public ObjectFactory getObjectFactory(Class<? extends NodeObject> clazz) {
		return factoryMap.get(clazz);
	}

	/**
	 * Get the class of objects stored in the given table
	 * @param tableName name of the table
	 * @return object class or null if not found
	 */
	public Class<? extends NodeObject> getClass(String tableName) {
		return table2Class.get(tableName);
	}

	/**
	 * Get the table name into which objects of the given class are stored
	 * @param clazz class
	 * @return table name
	 */
	public String getTable(Class<? extends NodeObject> clazz) {
		return class2Table.get(clazz);
	}

	/**
	 * check, if the factory was forced to be a new instance.
	 * @return true, if the factory is not registered in the static factory list.
	 */
	public boolean isForcedInstance() {
		return forcedInstance;
	}

	/**
	 * Add an objectfactory to the factory.
	 *
	 * @param clazz the classes which are created by this factory.
	 * @param factory
	 */
	public void registerObjectFactory(Class<? extends NodeObject> clazz, ObjectFactory factory) {
		if (factory == null) {
			return;
		}
		factoryMap.put(clazz, factory);
		DBTables dbTables = factory.getClass().getAnnotation(DBTables.class);

		if (dbTables != null) {
			DBTable[] tables = dbTables.value();

			for (DBTable dbTable : tables) {
				if (dbTable.table2Class()) {
					if (table2Class.containsKey(dbTable.name())) {
						// it is possible to have several classes stored in the same table (e.g. File and ImageFile in table contentfile)
						// in such a case, we get the "more general" class
						Class<? extends NodeObject> storedClazz = table2Class.get(dbTable.name());

						if (storedClazz.isAssignableFrom(dbTable.clazz())) {// we already have the more general class stored, so do nothing
						} else if (dbTable.clazz().isAssignableFrom(storedClazz)) {
							// we store the more general class now
							table2Class.put(dbTable.name(), dbTable.clazz());
						} else {
							// the classes have nothing in common, this is an error!
							logger.error(
									"Error while registering class for tablename " + dbTable.name() + ": found two incompatible classes " + storedClazz + " and "
											+ dbTable.clazz());
						}
					} else {
						table2Class.put(dbTable.name(), dbTable.clazz());
					}
				}
				class2Table.put(dbTable.clazz(), dbTable.name());

				// also put a mapping for an optional alias
				if (!ObjectTransformer.isEmpty(dbTable.alias())) {
					table2Class.put(dbTable.alias(), dbTable.clazz());
				}
			}
		}
		factorySet.add(factory);

		// TODO check for conflicts with ttypes
		int objType = 0;
		TType ttype = clazz.getAnnotation(TType.class);
		if (ttype != null) {
			objType = ttype.value();
		}

		if (objType > 0) {
			typeList.add(objType);
			classList.add(clazz);
			if (typeList.size() != classList.size()) {// woho, some mystery is happening here!
			}
		}

		// get all annotated setters/getters for exported fields of the class
		Method[] methods = clazz.getMethods();
		Map<String, DataFieldHandler> classFieldHandlers = new HashMap<String, NodeFactory.DataFieldHandler>();

		dataFieldHandlers.put(clazz, classFieldHandlers);
		for (Method method : methods) {
			FieldGetter getter = method.getAnnotation(FieldGetter.class);

			if (getter != null) {
				DataFieldHandler handler = classFieldHandlers.get(getter.value());

				if (handler == null) {
					handler = new DataFieldHandler(getter.value());
					classFieldHandlers.put(getter.value(), handler);
				}
				handler.setGetter(method);
			}
			FieldSetter setter = method.getAnnotation(FieldSetter.class);

			if (setter != null) {
				DataFieldHandler handler = classFieldHandlers.get(setter.value());

				if (handler == null) {
					handler = new DataFieldHandler(setter.value());
					classFieldHandlers.put(setter.value(), handler);
				}
				handler.setSetter(method);
			}
		}
	}

	/**
	 * register an objectfactory to the factory. The factory is registered for all classes it
	 * announces with {@link ObjectFactory#getProvidedClasses()}
	 * @param factory the factory to register.
	 */
	public void registerObjectFactory(ObjectFactory factory) {
		DBTables dbTables = factory.getClass().getAnnotation(DBTables.class);
		if (dbTables != null) {
			for (DBTable dbTable : dbTables.value()) {
				registerObjectFactory(dbTable.clazz(), factory);
			}
		}

		try {
			factory.initialize();
		} catch (NodeException e) {
			logger.error(String.format("Error while initializing object factory %s", factory.getClass().getSimpleName()), e);
		}
	}

	/**
	 * register a new preloader to the factory.
	 * @param factory the preloaderfactory to register.
	 */
	public void registerPreloader(PreloadableObjectFactory factory) {
		if (factory == null) {
			return;
		}

		Class<? extends NodeObject>[] provides = factory.getPreloadTriggerClasses();

		for (int i = 0; i < provides.length; i++) {
			Class<? extends NodeObject> provide = provides[i];

			Set<PreloadableObjectFactory> preloader = preloaderMap.get(provide);

			if (preloader == null) {
				preloader = new HashSet<PreloadableObjectFactory>(2);
				preloaderMap.put(provide, preloader);
			}
			preloader.add(factory);
		}
	}

	/**
	 * clear the cache of the factory.
	 * This method will also call {@link CacheService#clear()} on all found instances
	 */
	public void clear() throws NodeException {
		clearLocal();

		for (CacheService service : cacheServiceLoader) {
			service.clear();
		}
	}

	/**
	 * clear the cache of the factory.
	 */
	public void clearLocal() throws NodeException {
		if (cache != null) {
			try {
				cache.clear();
			} catch (PortalCacheException e) {
				throw new NodeException("Error while clearing the factory caches", e);
			}
		}
		typeCaches.values().forEach(cache -> {
			try {
				cache.clear();
			} catch (PortalCacheException e) {
			}
		});
	}

	/**
	 * clear the cache of the factory for a given class. All objects of this nodeobject-class
	 * are removed from the cache.
	 * @param clazz nodeobject-class to clear from the cache.
	 */
	public void clear(Class<? extends NodeObject> clazz) throws NodeException {
		clearLocal(clazz);

		for (CacheService service : cacheServiceLoader) {
			service.clear(clazz);
		}
	}

	/**
	 * clear the cache of the factory for a given class. All objects of this nodeobject-class
	 * are removed from the cache.
	 * @param clazz nodeobject-class to clear from the cache.
	 */
	public void clearLocal(Class<? extends NodeObject> clazz) throws NodeException {
		PortalCache cache = getCache(clazz);
		if (cache != null && clazz != null) {
			try {
				String cacheGroupName = normalizeClass(clazz).getName();

				cache.clearGroup(cacheGroupName);
			} catch (PortalCacheException e) {
				throw new NodeException("Error while clearing the factory caches", e);
			}
		}
	}

	/**
	 * remove a given object and all its versions from the cache.
	 * @param clazz class of the nodeobject.
	 * @param id id of the object to remove.
	 */
	public void clear(Class<? extends NodeObject> clazz, Integer id) throws NodeException {
		clearLocal(clazz, id);

		for (CacheService service : cacheServiceLoader) {
			service.clear(clazz, id);
		}
	}

	/**
	 * remove a given object and all its versions from the cache.
	 * @param clazz class of the nodeobject.
	 * @param id id of the object to remove.
	 */
	public void clearLocal(Class<? extends NodeObject> clazz, Integer id) throws NodeException {
		PortalCache cache = getCache(clazz);
		if (cache != null && clazz != null && id != null) {
			try {
				Object cacheKey = createCacheKey(clazz, id, -1);
				String cacheGroupName = normalizeClass(clazz).getName();

				cache.removeFromGroup(cacheGroupName, cacheKey);
			} catch (PortalCacheException e) {
				throw new NodeException("Error while clearing the factory caches", e);
			}
		}
	}

	/**
	 * Revalidate the cache for all objects. Check for changes since the last revalidation
	 * for each classes and remove all changed objects from the cache.
	 */
	public void revalidate() {}

	/**
	 * Revalidate the cache for a given object-class. Check all changes since last revalidation
	 * and remove all changed objects of this class.
	 * @param clazz the class of the node-objects to check.
	 */
	public void revalidate(Class<? extends NodeObject> clazz) {}

	/**
	 * Revalidate an object in the cache. check for changes since last check and remove it
	 * from the cache if it has been changed.
	 * @param clazz the class of the node-object to check.
	 * @param id id of the node-object to check.
	 */
	public void revalidate(Class<? extends NodeObject> clazz, int id) {}

	/**
	 * Try to get the TType of a given objectclass.
	 * @param clazz class of the object.
	 * @return the corresponding ttype, or 0 if no ttype is mapped to this class.
	 */
	public int getTType(Class<? extends NodeObject> clazz) {
		int pos = classList.indexOf(clazz);

		return (pos != -1) ? typeList.get(pos) : 0;
	}

	/**
	 * Get the class for a given ttype.
	 * @param objType the ttype the find the correspondig class to.
	 * @return the mapped class for this ttype, or null if the ttype is unknown.
	 */
	public Class<? extends NodeObject> getClass(int objType) {
		int pos = typeList.indexOf(objType);

		return (pos != -1) ? classList.get(pos) : null;
	}

	/**
	 * Normalize the class, when used for caching objects.
	 * Currently, only the class {@link ImageFile} will be normalized to {@link File}
	 * @param clazz original object class
	 * @return normalized class
	 */
	protected Class<? extends NodeObject> normalizeClass(Class<? extends NodeObject> clazz) {
		if (File.class.isAssignableFrom(clazz)) {
			return File.class;
		} else {
			return clazz;
		}
	}

	/**
	 * Normalize the version timestamp for the given class. If versioning is not supported, change the version timestamp to -1. Otherwise return the given versiontimestamp.
	 * @param clazz object class
	 * @param versionTimestamp version timestamp
	 * @return either the version timestamp or -1
	 */
	protected int normalizeVersionTimestamp(Class<? extends NodeObject> clazz, int versionTimestamp) {
		ObjectFactory objFactory = getObjectFactory(clazz);

		if (objFactory.isVersioningSupported(clazz)) {
			return versionTimestamp;
		} else {
			return -1;
		}
	}

	protected void updateNonVersionedData(NodeObject object) throws NodeException {
		if (object == null) {
			return;
		}
		// first get the object factory
		ObjectFactory objectFactory = getObjectFactory(object.getObjectInfo().getObjectClass());

		// versioning not supported, so nothing to do
		if (!objectFactory.isVersioningSupported(object.getObjectInfo().getObjectClass())) {
			return;
		}

		// when this instance is not the current version, we get the current
		// version and let the factory update the non-versioned data
		if (!object.getObjectInfo().isCurrentVersion()) {
			Transaction t = TransactionManager.getCurrentTransaction();
			// get the current flag status
			boolean disableVersionedPublish = t.isDisableVersionedPublish();

			try {
				// disable versioned publishing
				t.setDisableVersionedPublish(true);
				// get the current version of the object; if the object can not be found here
				// warnings will not be sent to the logs to avoid log flooding (for example, if
				// a widely used tag part was removed)
				NodeObject currentVersion = getObject(object.getObjectInfo().getObjectClass(), object.getId(), false, -1, false, false);

				objectFactory.updateNonVersionedData(object, currentVersion);
			} finally {
				// restore the original flag status
				t.setDisableVersionedPublish(disableVersionedPublish);
			}
		}
	}

	/**
	 * Returns the overall size of objectes marked for deletion in a given transaction.
	 * @param t The transaction to get the amount of objects marked to delete.
	 * @return The number of objects marked for delete.
	 */
	public int getDeleteListsSize(Transaction t) {
		int cnt = 0;

		for (ObjectFactory factory : factorySet) {
			cnt += factory.getDeleteListsSize(t);
		}
		return cnt;
	}

	/**
	 * Removes the deleteLists for the given transaction from every factory.
	 * @param t Transaction for which to remove the deleteLists.
	 */
	public void removeDeleteLists(Transaction t) {
		for (ObjectFactory factory : factorySet) {
			factory.removeDeleteList(t);
		}
	}

	/**
	 * Flushes all ObjectFactories so that all cached operations are performed
	 * @throws NodeException if an internal error occurs
	 */
	public void flushAll() throws NodeException {
		for (ObjectFactory factory : factorySet) {
			factory.flush();
		}
	}

	/**
	 * Create the cache key for the object of given class, id and version timestamp
	 * @param clazz object class
	 * @param id object id
	 * @param versionTimestamp version timestamp. -1 for current version
	 * @return the cache key
	 */
	private static String createCacheKey(Class<? extends NodeObject> clazz, Integer id, int versionTimestamp) {
		// generate the cache key. The cache key will contain the version
		// timestamp (if != -1), so that different versions of the same object
		// can be cached
		return id + "|" + versionTimestamp;
		// return versionTimestamp == -1 ? id : id + "@" + versionTimestamp;
	}

	/**
	 * Get the given object. Try to fetch it from the cache first.
	 * If the object should be loaded for update it will always be loaded directly from the database.
	 *
	 * @param clazz object class
	 * @param id object id
	 * @param forUpdate indicates whether the object should be loaded for update
	 * @param versionTimestamp version timestamp. -1 for current version.
	 * @param allowMultichannelFallback true when multichannelling fallback is allowed, false if not
	 * @param logErrorIfNotFound if true, a warning message is sent to the logs if the object was not found
	 * @return object or null if object not found
	 * @throws NodeException
	 * @throws ReadOnlyException if the object is requested for update but is only available for reading
	 */
	private <T extends NodeObject> T getObject(Class<T> clazz, Integer id, boolean forUpdate,
			int versionTimestamp, boolean allowMultichannelFallback, boolean logErrorIfNotFound) throws NodeException,
				ReadOnlyException {

		if (isZeroOrNull(id) || clazz == null) {
			return null;
		}

		Transaction t = TransactionManager.getCurrentTransaction();
		PublishData publishData = t.getPublishData();
		TransactionStatistics stats = t.getStatistics();
		T obj = null;
		ObjectFactory objFactory = factoryMap.get(clazz);

		// do the shortcut for getting Constructs or Parts
		if (clazz.isAssignableFrom(Construct.class) && forUpdate == false && publishData != null) {
			return (T) publishData.getConstruct(ObjectTransformer.getInt(id, 0));
		}
		if (clazz.isAssignableFrom(Part.class) && forUpdate == false && publishData != null) {
			return (T) publishData.getPart(ObjectTransformer.getInt(id, 0));
		}

		// do the shortcut for getting PublishablePages
		if (clazz.isAssignableFrom(Page.class) && forUpdate == false && versionTimestamp < 0 && allowMultichannelFallback == false
				&& !t.isDisableVersionedPublish() && isVersionedPublishing() && t.isPublishCacheEnabled()) {
			return (T)PublishablePage.getInstance(ObjectTransformer.getInt(id, 0));
		}

		// do the shortcut for getting PublishableTemplates
		if (clazz.isAssignableFrom(Template.class) && forUpdate == false && !t.isDisableVersionedPublish() && isVersionedPublishing()
				&& t.isPublishCacheEnabled()) {
			obj = (T)PublishableTemplate.getInstance(ObjectTransformer.getInt(id, 0));
			if (obj != null && allowMultichannelFallback) {
				obj = objFactory.doMultichannellingFallback(obj);
			}
			return obj;
		}

		// normalize the version timestamp
		versionTimestamp = normalizeVersionTimestamp(clazz, versionTimestamp);

		// normalize the class
		String cacheGroupName = normalizeClass(clazz).getName();

		ObjectContainer objectContainer = null;

		// create the cache key
		String cacheKey = createCacheKey(clazz, id, versionTimestamp);

		PortalCache cache = getCache(clazz);
		if (cache != null) {
			try {
				RuntimeProfiler.beginMark(JavaParserConstants.NODEFACTORY_GETOBJECT_GETCACHE, cacheGroupName);
				if (stats != null) {
					stats.get(Item.ACCESS_CACHE).start();
				}
				try {
					// try loading the object from the cache
					objectContainer = (ObjectContainer) cache.getFromGroup(cacheGroupName, cacheKey);
					if (objectContainer != null) {
						// purge old versions
						objectContainer.purgeOldVersions();
						// get the object from the cache
						obj = (T) objectContainer.getObject(null);
					}
				} finally {
					if (stats != null) {
						stats.get(Item.ACCESS_CACHE).stop();
					}
					RuntimeProfiler.endMark(JavaParserConstants.NODEFACTORY_GETOBJECT_GETCACHE, cacheGroupName);
				}
			} catch (PortalCacheException e) {
				logger.warn("Error while fetching object from cache, not using cache", e);
			}
		}

		if (obj == null) {
			RuntimeProfiler.beginMark(JavaParserConstants.NODEFACTORY_GETOBJECT_LOADOBJECT, clazz.getName());
			try {
				if (stats != null) {
					stats.get(Item.ACCESS_DB).start();
				}
				obj = loadObject(clazz, id, versionTimestamp, logErrorIfNotFound);
				if (cache != null) {
					try {
						// regenerate the cachekey, if the object is a different one than we wanted to load (multichannelling fallback)
						if (obj != null && !id.equals(obj.getId())) {
							id = obj.getId();
							cacheKey = createCacheKey(clazz, id, versionTimestamp);
							objectContainer = (ObjectContainer) cache.getFromGroup(cacheGroupName, cacheKey);
						}
						if (objectContainer == null) {
							objectContainer = new ObjectContainer(clazz, id);
							cache.putIntoGroup(cacheGroupName, cacheKey, objectContainer);
						}
						objectContainer.setObject(obj);
					} catch (PortalCacheException e) {
						logger.warn("Error while putting object into cache", e);
					}
				}
			} finally {
				if (stats != null) {
					stats.get(Item.ACCESS_DB).stop();
				}
				RuntimeProfiler.endMark(JavaParserConstants.NODEFACTORY_GETOBJECT_LOADOBJECT, clazz.getName());
			}
		}

		// do the multichannelling fallback here
		if (obj != null && allowMultichannelFallback) {
			obj = objFactory.doMultichannellingFallback(obj);
		}

		if (obj != null && forUpdate) {
			// when the object shall be fetched for update, we now try to create an editable copy here
			if (objFactory != null) {
				obj = objFactory.getEditableCopy(obj, createObjectInfo(clazz, true, -1));
			}
		}

		// if the current render type is PUBLISH, we get the published version of the object
		if (!t.isDisableVersionedPublish() && obj != null && versionTimestamp <= 0 && isVersionedPublishing()
				&& !forUpdate) {
			obj = (T) obj.getPublishedObject();
		}

		// finally, we possibly need to update non-versioned fields
		updateNonVersionedData(obj);

		return obj;
	}

	/**
	 * Dirt the object cache for the given object.
	 * This method will also call {@link CacheService#dirt(Class, Integer)} on all found instances
	 * @param clazz object class
	 * @param id object id
	 * @throws NodeException
	 */
	public void dirtObjectCache(Class<? extends NodeObject> clazz, Integer id) throws NodeException {
		dirtObjectCacheLocal(clazz, id);

		for (CacheService service : cacheServiceLoader) {
			service.dirt(clazz, id);
		}
	}

	/**
	 * Dirt the object cache for the given object
	 * @param clazz object class
	 * @param id object id
	 * @throws NodeException
	 */
	public void dirtObjectCacheLocal(Class<? extends NodeObject> clazz, Integer id) throws NodeException {
		if (isZeroOrNull(id) || clazz == null) {
			return;
		}

		// normalize the class
		String cacheGroupName = normalizeClass(clazz).getName();

		if (logger.isDebugEnabled()) {
			logger.debug("Dirting cache for object class {" + clazz.getName() + "}, id {" + id + "}");
		}
		ObjectContainer objectContainer = null;
		NodeObject nodeObject = null;

		PortalCache cache = getCache(clazz);
		if (cache != null) {
			try {

				String cacheKey = createCacheKey(clazz, id, -1);

				// try loading the object from the cache
				objectContainer = (ObjectContainer) cache.getFromGroup(cacheGroupName, cacheKey);
				// try getting the object from the cache
				if (objectContainer != null) {
					nodeObject = objectContainer.getObject(null);
				}
				// when the object is not found, load it here (so we can dirt the subobjects)
				if (nodeObject == null) {
					// no need to log an error, if the object was not found
					nodeObject = loadObject(clazz, id, -1, false);
				}
				// dirt the object
				if (objectContainer != null) {
					// purge old versions
					objectContainer.purgeOldVersions();
					objectContainer.dirtObject();
				}
				// dirt the cache of the subobjects
				if (nodeObject != null) {
					nodeObject.dirtCache();
				}

				// if the object supports versioning, also remove caches for all known versions
				if (nodeObject instanceof PublishableNodeObject) {
					NodeObjectVersion[] versions = ((PublishableNodeObject) nodeObject).getVersions();
					for (NodeObjectVersion version : versions) {
						cacheKey = createCacheKey(clazz, id, version.getDate().getIntTimestamp());
						cache.removeFromGroup(cacheGroupName, cacheKey);
					}
				}

			} catch (PortalCacheException e) {
				logger.warn("Error while fetching object from cache, not using cache", e);
			} catch (InconsistentDataException e) {
				logger.error("Detected data inconsistency while dirting object {" + (nodeObject == null ? "null" : nodeObject.toString()) + "}", e);
			}
		}
	}

	/**
	 * create a new object of a given class.
	 * @param clazz the class of the new object.
	 * @return a new, editable instance of the object.
	 */
	public <T extends NodeObject> T createObject(Class<T> clazz) throws NodeException {
		T obj = null;

		ObjectFactory objFactory = factoryMap.get(clazz);

		if (objFactory != null) {
			FactoryHandle handle = getFactoryHandle(clazz);

			obj = objFactory.createObject(handle, clazz);
		} else {// TODO throw exception
		}

		return obj;
	}

	/**
	 * get a bunch of objects from the factory.
	 *
	 * @param clazz class of the objects
	 * @param ids list of ids as Integers from the objects
	 * @return list of all nodeobjects of the given class which could be loaded.
	 * @throws NodeException when an error occurred while loading the objects
	 */
	private <T extends NodeObject> List<T> getObjects(Class<T> clazz, Collection<Integer> ids) throws NodeException {
		return getObjects(clazz, ids, false, -1, true);
	}

	/**
	 * get a bunch of objects from the factory.
	 *
	 * @param clazz class of the objects
	 * @param ids list of ids as Integers from the objects
	 * @param forUpdate true when the objects should be fetched for update, false if not
	 * @param versionTimestamp version timestamp. -1 for current version.
	 * @param allowMultichannelFallback true when multichannelling fallback is allowed, false if not
	 * @return list of all nodeobjects of the given class which could be loaded.
	 * @throws NodeException when an error occurred while loading the objects
	 * @throws ReadOnlyException when the objects should be fetched for update, but at least one was only available readonly
	 */
	@SuppressWarnings("unchecked")
	private <T extends NodeObject> List<T> getObjects(Class<T> clazz, Collection<Integer> ids, boolean forUpdate, int versionTimestamp, boolean allowMultichannelFallback) throws NodeException, ReadOnlyException {
		// TODO make clean implementation here
		if (ids == null || ids.size() == 0 || clazz == null) {
			return Collections.emptyList();
		}

		Transaction t = TransactionManager.getCurrentTransaction();
		PublishData publishData = t.getPublishData();
		TransactionStatistics stats = t.getStatistics();

		// normalize the version timestamp
		versionTimestamp = normalizeVersionTimestamp(clazz, versionTimestamp);

		// normalize the class
		String cacheGroupName = normalizeClass(clazz).getName();

		if (ids.size() == 1) {
			final Integer id = ids.iterator().next();
			final T obj = getObject(clazz, id, forUpdate, versionTimestamp, allowMultichannelFallback, true);

			if (obj != null) {
				List<T> result = new ArrayList<T>();
				result.add(obj);
				return result;
			} else {
				return Collections.emptyList();
			}
		}

		ObjectFactory objFactory = factoryMap.get(clazz);
		List<Object> objs = new ArrayList<Object>(ids.size());
		Map<Integer, Integer> fetchIds = new HashMap<Integer, Integer>();

		// determine, whether we shall return instances of PublishablePage
		boolean getPublishablePage = clazz.isAssignableFrom(Page.class) && forUpdate == false && versionTimestamp < 0 && t.usePublishablePages();
		List<Integer> publishablePageIds = null;
		if (getPublishablePage) {
			publishablePageIds = new ArrayList<>();
		}

		// load all objects from cache if available
		int pos = 0;

		try {
			RuntimeProfiler.beginMark(JavaParserConstants.NODEFACTORY_GETOBJECTS_GETCACHE, cacheGroupName);
			if (stats != null) {
				stats.get(Item.ACCESS_CACHE).start();
			}
			for (Iterator<Integer> it = ids.iterator(); it.hasNext();) {
				Integer id = it.next();

				if (isZeroOrNull(id)) {
					continue;
				}

				NodeObject obj = null;
				if (getPublishablePage) {
					obj = PublishablePage.getInstance(id);
					if (obj != null && allowMultichannelFallback) {
						obj = objFactory.doMultichannellingFallback(obj);
					}
				} else if (clazz.isAssignableFrom(Construct.class) && forUpdate == false && publishData != null) {
					obj = publishData.getConstruct(id);
				} else if (clazz.isAssignableFrom(Part.class) && forUpdate == false && publishData != null) {
					obj = publishData.getPart(id);
				} else {
					obj = loadCachedObject(clazz, id, forUpdate, versionTimestamp, allowMultichannelFallback);
				}

				if (obj != null) {
					if (getPublishablePage) {
						publishablePageIds.add(ObjectTransformer.getInteger(obj.getId(), null));
					} else {
						objs.add(obj);
					}
				} else if (!getPublishablePage) {
					// when the object was not found, and multichannelling fallback is allowed, we try to get the object without mc fallback
					// if the master object with that id exists in the cache, the object has no representation in the current channel
					// and must therefore be ignored
					if (allowMultichannelFallback) {
						if (loadCachedObject(clazz, id, false, versionTimestamp, false) != null) {
							continue;
						}
					}

					Integer iPos = new Integer(pos);

					objs.add(iPos);
					fetchIds.put(iPos, id);
				}
				pos++;
			}
		} finally {
			if (stats != null) {
				stats.get(Item.ACCESS_CACHE).stop();
			}
			RuntimeProfiler.endMark(JavaParserConstants.NODEFACTORY_GETOBJECTS_GETCACHE, cacheGroupName);
		}

		if (fetchIds.size() > 0) {
			if (logger.isDebugEnabled()) {
				logger.debug("must load {" + fetchIds.size() + "/" + ids.size() + "} of class {" + clazz.getName() + "} objects from db");
			}
			RuntimeProfiler.beginMark(JavaParserConstants.NODEFACTORY_GETOBJECTS_LOADOBJECTS, cacheGroupName);
			if (stats != null) {
				stats.get(Item.ACCESS_DB).start();
			}

			try {
				loadObjects(objs, clazz, fetchIds, forUpdate, versionTimestamp, allowMultichannelFallback);
			} finally {
				if (stats != null) {
					stats.get(Item.ACCESS_DB).stop();
				}
				RuntimeProfiler.endMark(JavaParserConstants.NODEFACTORY_GETOBJECTS_LOADOBJECTS, cacheGroupName);
			}
		}

		if (getPublishablePage) {
			return (List<T>) new LightWeightPageList(publishablePageIds);
		} else {
			// if the rendertype is PUBLISH, we get the published versions of the objects
			if (objs.size() > 0 && isVersionedPublishing()) {
				List newObjs = new ArrayList();

				for (Iterator iterator = objs.iterator(); iterator.hasNext();) {
					NodeObject object = (NodeObject) iterator.next();

					newObjs.add(object.getPublishedObject());
				}
				objs = newObjs;
			}

			List<T> result = new ArrayList<T>(objs.size());

			for (Object o : objs) {
				T tO = (T) o;

				result.add(tO);
			}
			return result;
		}
	}

	/**
	 * Create a new transaction
	 * @param sessionId sessionId of the user which is associated to the transaction
	 * @param useConnectionPool flag whether db connections shall be pooled or not
	 * @return a new, open transaction
	 * @throws TransactionException
	 */
	public Transaction createTransaction(String sessionId, boolean useConnectionPool) throws TransactionException, InvalidSessionIdException {
		return TransactionManager.getTransaction(sessionId, new NodeFactoryHandle(), useConnectionPool);
	}

	/**
	 * Create a new transaction
	 * @param sessionId sessionId of the user that is specified in the second parameter
	 * @param userId userId of the user that should be associated with that transaction
	 * @param useConnectionPool flag whether db connections shall be pooled or not
	 * @return a new, open transaction
	 * @throws TransactionException
	 */
	public Transaction createTransaction(String sessionId, Integer userId, boolean useConnectionPool) throws TransactionException, InvalidSessionIdException {
		return TransactionManager.getTransaction(sessionId, userId, new NodeFactoryHandle(),
				useConnectionPool);
	}

	/**
	 * Create a new transaction
	 * @param useConnectionPool flag whether db connections shall be pooled or not
	 * @return a new, open transaction
	 * @throws TransactionException
	 */
	public Transaction createTransaction(boolean useConnectionPool) throws TransactionException, InvalidSessionIdException {
		return TransactionManager.getTransaction(new NodeFactoryHandle(), useConnectionPool);
	}

	/**
	 * Create a new transaction
	 * @param useConnectionPool flag whether db connections shall be pooled or not
	 * @param multiconnection true if you want a transaction which can be accessed from multiple threads
	 * @return a new, open transaction
	 * @throws TransactionException
	 */
	public Transaction createTransaction(boolean useConnectionPool, boolean multiconnection) throws TransactionException, InvalidSessionIdException {
		return TransactionManager.getTransaction(new NodeFactoryHandle(), useConnectionPool, multiconnection);
	}

	/**
	 * Get a cached object from the cache.
	 * When the object shall be fetched for update, always create an editable copy of the object
	 * @param clazz class of the object
	 * @param id id of the object
	 * @param forUpdate true when the object shall be fetched for update, false if not
	 * @param versionTimestamp version timestamp. -1 for current version.
	 * @param allowMultichannelFallback true when multichannelling fallback is allowed, false if not
	 * @return the cached object, or null if it is not in the cache.
	 */
	private <T extends NodeObject> T loadCachedObject(Class<T> clazz, Integer id, boolean forUpdate, int versionTimestamp, boolean allowMultichannelFallback) throws NodeException {

		// TODO: preloading if this object was itself preloaded?
		T obj = null;

		Transaction t = TransactionManager.getCurrentTransaction();
		TransactionStatistics stats = t.getStatistics();

		// create the cache key
		Object cacheKey = createCacheKey(clazz, id, versionTimestamp);

		PortalCache cache = getCache(clazz);
		if (cache != null) {
			try {
				if (stats != null) {
					stats.get(Item.ACCESS_CACHE).start();
				}
				String cacheGroupName = normalizeClass(clazz).getName();
				// try loading the object from the cache
				ObjectContainer objectContainer = (ObjectContainer) cache.getFromGroup(cacheGroupName, cacheKey);

				if (objectContainer != null) {
					// purge old versions
					objectContainer.purgeOldVersions();
					// get the object from the cache
					obj = (T) objectContainer.getObject(null);
				}
			} catch (PortalCacheException e) {
				logger.warn("Error while fetching object from cache, not using cache", e);
			} finally {
				if (stats != null) {
					stats.get(Item.ACCESS_CACHE).stop();
				}
			}
		}

		// do the multichannelling fallback here
		if (obj != null && allowMultichannelFallback) {
			ObjectFactory objFactory = factoryMap.get(clazz);
			obj = objFactory.doMultichannellingFallback(obj);
		}

		// when an object is found in the cache and the object shall be fetched for update, create an editable copy here
		if (obj != null && forUpdate) {
			ObjectFactory factory = getObjectFactory(clazz);

			if (factory != null) {
				obj = factory.getEditableCopy(obj, createObjectInfo(clazz, true, -1));
			}
		}

		// we possibly need to update non-versioned fields
		updateNonVersionedData(obj);

		return obj;
	}

	/**
	 * load a new object. preloading is also handled here. if the object is found, it is
	 * put into the cache.
	 * @param clazz object class
	 * @param id object id
	 * @param versionTimestamp version timestamp. -1 for current version.
	 * @param logErrorIfNotFound true when an error shall be logged, if the object was not found, false if not
	 * @return a new object, or null if it cannot be created.
	 */
	private <T extends NodeObject> T loadObject(Class<T> clazz, Integer id, int versionTimestamp, boolean logErrorIfNotFound) throws NodeException {
		// TODO lock getter of this factory for this object to prevent endless-loops
		ObjectFactory objFactory = factoryMap.get(clazz);

		if (objFactory != null) {
			T obj = null;

			// TODO preloading
			// int iId = id.intValue();
			// preloadObjects(handle, clazz, iId);

			NodeObjectInfo info = createObjectInfo(clazz, false, versionTimestamp);

			obj = objFactory.loadObject(clazz, id, info);

			if (obj != null) {
				putObject(clazz, obj, versionTimestamp);
			} else {
				if (logErrorIfNotFound) {
					TransactionManager.getCurrentTransaction().getRenderResult().warn(NodeFactory.class,
							"Unable to load object of class {" + clazz.getName() + "} with id {" + id + "}");
				}
			}

			return obj;
		} else {
			throw new NodeException("Could not load object of class " + clazz.getName() + ": no object factory found");
		}
	}

	/**
	 * load a list of objects from the objectfactories. do preloading and batchloading here.
	 * @param objs the list of objects, where the loaded objects must be put into.
	 * @param clazz class of the objects to load
	 * @param ids map of 'position in objs' to 'id' as Integer->Integer.
	 * @param forUpdate true when the objects shall be fetched for update, false if not
	 * @param versionTimestamp version timestamp
	 * @param allowMultichannelFallback true when multichannelling fallback is allowed, false if not
	 */
	private <T extends NodeObject> void loadObjects(List<Object> objs, Class<T> clazz, Map<Integer, Integer> ids, boolean forUpdate, int versionTimestamp,
			boolean allowMultichannelFallback) throws NodeException {
		ObjectFactory objFactory = factoryMap.get(clazz);

		if (objFactory != null) {
			FactoryHandle handle = getFactoryHandle(clazz);

			if (objFactory instanceof BatchObjectFactory) {

				preloadObjects(handle, clazz, ids.values());

				NodeObjectInfo info = createObjectInfo(clazz, false, versionTimestamp);
				NodeObjectInfo editableInfo = null;

				if (forUpdate) {
					editableInfo = createObjectInfo(clazz, true, -1);
				}
				Collection<T> newObjs = ((BatchObjectFactory) objFactory).batchLoadObjects(clazz, ids.values(), info);

				if (newObjs != null) {

					Map<Object, T> objMap = new HashMap<Object, T>(newObjs.size());

					for (Iterator<T> it = newObjs.iterator(); it.hasNext();) {
						T obj = it.next();

						putObject(clazz, obj, versionTimestamp);
						objMap.put(obj.getId(), obj);
					}

					for (Iterator<Integer> it = ids.keySet().iterator(); it.hasNext();) {
						Integer pos = it.next();
						T obj = objMap.get(ids.get(pos));

						if (obj == null) {
							logger.warn("Unable to load object with id {" + ids.get(pos) + "} for Class {" + clazz.getName() + "}");
						}

						// do the multichannelling fallback here
						if (obj != null && allowMultichannelFallback) {
							obj = objFactory.doMultichannellingFallback(obj);
						}

						if (forUpdate && obj != null) {
							// when the objects shall be loaded for update, create a copy here
							objs.set(pos.intValue(), objFactory.getEditableCopy(obj, editableInfo));
						} else {
							objs.set(pos.intValue(), obj);
						}
					}
				}

			} else {

				T obj = null;

				for (Iterator<Integer> it = ids.keySet().iterator(); it.hasNext();) {
					Integer pos = it.next();
					Integer id = ids.get(pos);

					// TODO fix this
					// obj = loadObject(clazz, id, editable);
					if (obj != null) {
						objs.set(pos.intValue(), obj);
					}
				}
			}
		} else {
			throw new NodeException("Could not load factory for class of type " + clazz.getName());
		}

		// finally remove the null objects
		for (Iterator<Object> iter = objs.iterator(); iter.hasNext();) {
			Object element = iter.next();

			if (element == null) {
				iter.remove();
			}
		}
	}

	/**
	 * create a new objectinfo element which will be passed to new objects or used to search in the cache.
	 * @param clazz object class
	 * @param editable true when the object shall be editable, false for non-editable objects
	 * @param versionTimestamp version timestamp. -1 for the current version
	 * @return a new objectinfo containing infos about an object-type
	 */
	private NodeObjectInfo createObjectInfo(final Class<? extends NodeObject> clazz, final boolean editable, final int versionTimestamp) {
		return new FactoryNodeObjectInfo(clazz, editable, versionTimestamp);
	}

	/**
	 * put an object into the cache.
	 * @param clazz class of the object
	 * @param obj object to put into the cache.
	 * @param versionTimestamp version timestamp (-1 for current version)
	 */
	private void putObject(Class<? extends NodeObject> clazz, NodeObject obj, int versionTimestamp) throws NodeException {
		PortalCache cache = getCache(clazz);
		if (cache != null && obj != null) {
			try {
				String cacheGroupName = normalizeClass(clazz).getName();
				Integer id = obj.getId();
				Object cacheKey = createCacheKey(clazz, id, versionTimestamp);

				ObjectContainer objectContainer = (ObjectContainer) cache.getFromGroup(cacheGroupName, cacheKey);

				if (objectContainer == null) {
					objectContainer = new ObjectContainer(clazz, id);
					cache.putIntoGroup(cacheGroupName, cacheKey, objectContainer);
				}
				objectContainer.setObject(obj);
			} catch (PortalCacheException e) {
				logger.warn("Error while putting object into cache", e);
			}
		}
	}

	/**
	 * do preloading for a requested object.
	 * @param handle
	 * @param clazz
	 * @param id
	 */
	private void preloadObjects(FactoryHandle handle, Class<? extends NodeObject> clazz, int id) {
		Set<? extends PreloadableObjectFactory> preloader = getPreloader(clazz);

		for (Iterator<? extends PreloadableObjectFactory> it = preloader.iterator(); it.hasNext();) {
			PreloadableObjectFactory factory = it.next();

			factory.preload(handle, clazz, id);
		}
	}

	/**
	 * do preloading for a list of requested objects.
	 * @param handle
	 * @param clazz
	 * @param ids list of ids as list of Integer.
	 */
	private void preloadObjects(FactoryHandle handle, Class<? extends NodeObject> clazz, Collection<? extends Object> ids) {
		Set<? extends PreloadableObjectFactory> preloader = getPreloader(clazz);

		for (Iterator<? extends PreloadableObjectFactory> it = preloader.iterator(); it.hasNext();) {
			PreloadableObjectFactory factory = (PreloadableObjectFactory) it.next();

			factory.preload(handle, clazz, ids);
		}
	}

	/**
	 * get the preloader for a given object-class.
	 * @param clazz nodeobject class
	 * @return a set of preloader, or an empty set if no preloader is registered for this class.
	 */
	private Set<? extends PreloadableObjectFactory> getPreloader(Class<? extends NodeObject> clazz) {
		Set<? extends PreloadableObjectFactory> preloader = preloaderMap.get(clazz);

		if (preloader == null) {
			preloader = Collections.emptySet();
		}
		return preloader;
	}

	/**
	 * create a new factoryhandle of this nodefactory.
	 * @param clazz
	 * @return
	 */
	public FactoryHandle getFactoryHandle(Class<? extends NodeObject> clazz) {
		// TODO pass the connection to every method, so that it is available here
		return new NodeFactoryHandle();
	}

	/**
	 * determine weither the give id is int 0 or null, to avoid cache misses
	 * @param id
	 * @return true when the id is 0 or null, false otherwise
	 */
	private static boolean isZeroOrNull(Object id) {
		if (ObjectTransformer.getInt(id, 0) == 0) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Check whether the current transaction has a rendertype with editmode set
	 * to {@link RenderType#EM_PUBLISH}.
	 *
	 * @return true if the rendertype is set to EM_PUBLISH, false if not
	 * @throws NodeException
	 */
	private boolean isVersionedPublishing() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		// with this feature, it is possible to disable the versioned rendering
		// of pages while publishing
		if (t.getNodeConfig().getDefaultPreferences().getFeature("disable_versioned_publishing")) {
			return false;
		}
		RenderType renderType = t.getRenderType();

		if (renderType == null) {
			return false;
		}
		return renderType.getEditMode() == RenderType.EM_PUBLISH;
	}

	/**
	 * Stop the currently running dirtqueue worker thread.
	 * This method will wait until the thread is not running any more
	 */
	public void stopDirtQueueWorker() throws NodeException {
		if (!isInitialized()) {
			throw new NodeException("Could not stop dirt queue worker, factory is not initialized");
		}
		if (dirtQueueWorkerThread == null) {
			if (logger.isDebugEnabled()) {
				logger.debug("No dirt queue worker thread running, nothing to stop");
			}
			return;
		}
		// interrupt the thread
		if (logger.isDebugEnabled()) {
			logger.debug("Interrupting dirt queue worker thread");
		}
		dirtQueueWorkerThread.stopWorker();
		dirtQueueWorkerThread.interrupt();
		try {
			if (logger.isDebugEnabled()) {
				logger.debug("Waiting for the dirt queue worker to die");
			}
			dirtQueueWorkerThread.join();
		} catch (InterruptedException e) {}

		if (logger.isDebugEnabled()) {
			logger.debug("Dirt queue worker was stopped");
		}
		dirtQueueWorkerThread = null;
	}

	/**
	 * Start the dirtqueue worker thread (if not already running)
	 * @throws NodeException
	 */
	public void startDirtQueueWorker() throws NodeException {
		if (!isInitialized()) {
			throw new NodeException("Could not start dirt queue worker, factory is not initialized");
		}
		if (dirtQueueWorkerThread != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Dirt queue worker is already running, no need to start it");
			}
			return;
		}

		dirtQueueWorkerThread = new TriggerEventJobThread();
		dirtQueueWorkerThread.setName("DirtQueue worker");
		// The thread is never closed in a nice way so it has to be a deamon. Otherwise Tomcat is not able to stop.
		dirtQueueWorkerThread.setDaemon(true);
		dirtQueueWorkerThread.start();
		if (logger.isDebugEnabled()) {
			logger.debug("Started dirt queue worker thread");
		}
	}

	/**
	 * Initialize the caches
	 */
	public void initCaches() {
		typeCaches.clear();

		NodePreferences prefs = NodeConfigRuntimeConfiguration.getDefault().getNodeConfig().getDefaultPreferences();
		Map<?, ?> cacheSettings = prefs.getPropertyMap("node_cache");
		if (cacheSettings != null) {
			cacheSettings.entrySet().stream().forEach(entry -> {
				Integer type = ObjectTransformer.getInteger(entry.getKey(), null);
				String cacheName = ObjectTransformer.getString(entry.getValue(), null);
				if (type == null) {
					logger.error("Cannot set node_cache for type '" + entry.getKey() + "'. Type must be a number");
				}

				if (type != null && cacheName != null) {
					Class<? extends NodeObject> clazz = getClass(type);
					if (clazz == null) {
						logger.error("Cannot set node_cache for unknown type '" + type + "'");
					} else {
						try {
							typeCaches.put(normalizeClass(clazz), PortalCache.getCache(cacheName));
						} catch (Exception e) {
							logger.error("Error while setting node_cache for type '" + type + "' to '" + cacheName + "'", e);
						}
					}
				}
			});
		}
	}

	/**
	 * Get the cache for objects of the given class
	 * @param clazz object class
	 * @return cache instance
	 */
	protected PortalCache getCache(Class<? extends NodeObject> clazz) {
		return typeCaches.getOrDefault(normalizeClass(clazz), cache);
	}

	/**
	 * Factory specific implementation of {@link NodeObjectInfo}
	 */
	protected class FactoryNodeObjectInfo implements NodeObjectInfo {
		/**
		 * Hash Key
		 */
		protected String hashKey;

		/**
		 * Class of the {@link NodeObject} instance
		 */
		protected Class<? extends NodeObject> clazz;

		/**
		 * True if object is editable
		 */
		protected boolean editable;

		/**
		 * Version Timestamp of the object
		 */
		protected int versionTimestamp;

		/**
		 * Create an instance
		 * @param clazz object class
		 * @param editable true for editable objects
		 * @param versionTimestamp version timestamp
		 */
		public FactoryNodeObjectInfo(Class<? extends NodeObject> clazz, boolean editable, int versionTimestamp) {
			this.clazz = clazz == ContentFile.class ? File.class : clazz;
			this.editable = editable;
			this.versionTimestamp = versionTimestamp;
			this.hashKey = clazz.getName() + ";" + Boolean.toString(editable) + ";" + versionTimestamp;
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.object.NodeObjectInfo#getObjectClass()
		 */
		public Class<? extends NodeObject> getObjectClass() {
			return clazz;
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.object.NodeObjectInfo#isEditable()
		 */
		public boolean isEditable() {
			return editable;
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.object.NodeObjectInfo#getEditUserId()
		 */
		public int getEditUserId() {
			return 0;
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.object.NodeObjectInfo#getHashKey()
		 */
		public String getHashKey() {
			return hashKey;
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.object.NodeObjectInfo#getFactory()
		 */
		public NodeFactory getFactory() {
			return NodeFactory.this;
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.object.NodeObjectInfo#getConfiguration()
		 */
		public NodeConfig getConfiguration() {
			return NodeConfigRuntimeConfiguration.getDefault().getNodeConfig();
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.object.NodeObjectInfo#getVersionTimestamp()
		 */
		public int getVersionTimestamp() {
			return versionTimestamp;
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.object.NodeObjectInfo#isCurrentVersion()
		 */
		public boolean isCurrentVersion() {
			return versionTimestamp == -1;
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.object.NodeObjectInfo#getSubInfo(java.lang.Class)
		 */
		public NodeObjectInfo getSubInfo(Class<? extends NodeObject> clazz) {
			return new FactoryNodeObjectInfo(clazz, editable, versionTimestamp);
		}
	}

	/**
	 * Background thread for periodically triggering dirt events
	 */
	protected class TriggerEventJobThread extends Thread {

		/**
		 * Flag to mark whether the dirt queue worker has been stopped
		 */
		protected boolean stopped = false;

		/*
		 * (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		public void run() {
			if (logger.isInfoEnabled()) {
				logger.info("Background thread {" + getName() + "} starting to trigger queued events");
			}
			while (!stopped && !isInterrupted()) {
				QueueEntry entry = null;
				boolean doSleep = true;

				if (DistributionUtil.isTaskExecutionAllowed()) {
					try (Trx trx = new Trx()) {
						doSleep = true;

						entry = QueueEntry.getOldestQueueEntry();

						if (entry != null) {

							trx.getTransaction().setTimestamp(entry.getTimestamp());

							if (entry.isPublish()) {
								// TODO check whether the publish process is still
								// running, if yes wait some time
								if (logger.isInfoEnabled()) {
									logger.info("Publish is running, waiting " + DIRTQUEUE_THREAD_DEFAULTWAIT + " seconds");
								}
								doSleep = true;
							} else if (entry.isBlocker()) {
								if (logger.isInfoEnabled()) {
									logger.info(
											"Found a manual blocker, waiting " + DIRTQUEUE_THREAD_DEFAULTWAIT
											+ " seconds... don't forget to remove the blocker when maintenance work is done (id is " + entry.getId() + ")!");
								}
								doSleep = true;
							} else {
								if (logger.isInfoEnabled()) {
									logger.info("Triggering event " + entry);
								}
								// this is no publish process, so trigger the event
								if (entry.triggerEvent()) {
									// event was triggered, delete the queue entry
									entry.delete();
									doSleep = false;
								}
							}
						} else {
							if (logger.isInfoEnabled()) {
								logger.info("Found no queued event, waiting " + DIRTQUEUE_THREAD_DEFAULTWAIT + " seconds");
							}
							// found no queued dirt, so wait some time
							doSleep = true;
						}
						trx.success();
					} catch (Exception e) {
						// when an entry was handled, we try to set the entry to be failed
						if (entry != null) {
							logger.error("Error while handling event", e);
							// store the failure in the dirtqueue entry
							try {
								Trx.consume(tmpEntry -> tmpEntry.setFailed(e), entry);

								// only immediately proceed with the next event, when we managed to set the previous entry to be failed and the
								// originating error was not an SQLException
								if (!(e instanceof SQLException) && !(e.getCause() instanceof SQLException)) {
									doSleep = false;
								}
							} catch (NodeException e1) {
								logger.error("Error while setting the entry to failed", e1);
							}
						} else {
							logger.error("Error while fetching next entry", e);
						}
					}
				}

				if (doSleep) {
					try {
						sleep(ObjectTransformer.getInt(System.getProperty("com.gentics.contentnode.dirtqueue.wait"), DIRTQUEUE_THREAD_DEFAULTWAIT * 1000));
					} catch (InterruptedException e) {
						logger.warn("Thread was interrupted while sleeping");
					}
				}
			}

			if (logger.isInfoEnabled()) {
				logger.info("Background thread {" + getName() + "} was stopped");
			}
		}

		/**
		 * Stop the dirt queue worker
		 */
		public void stopWorker() {
			stopped = true;
		}
	}

	/**
	 * Implementation of getting and setting field values to/from NodeObjects
	 */
	protected static class DataFieldHandler {

		/**
		 * Field name
		 */
		protected String fieldName;

		/**
		 * Setter for the field
		 */
		protected Method setter;

		/**
		 * Getter for the field
		 */
		protected Method getter;

		/**
		 * Create an instance for the given field
		 * @param fieldName field name
		 */
		public DataFieldHandler(String fieldName) {
			this.fieldName = fieldName;
		}

		/**
		 * Set the getter method
		 * @param getter getter method
		 */
		public void setGetter(Method getter) {
			this.getter = getter;
		}

		/**
		 * Set the setter method
		 * @param setter setter method
		 */
		public void setSetter(Method setter) {
			this.setter = setter;
		}

		/**
		 * Read the field value from the given object
		 * @param object object
		 * @return field value
		 * @throws NodeException
		 */
		public Object get(NodeObject object) throws NodeException {
			// when no object found, throw an exception
			if (object == null) {
				throw new NodeException("Cannot get field " + fieldName + " from null object");
			}

			// check whether getter exists
			if (getter == null) {
				throw new NodeException("Cannot get field " + fieldName + " from " + object + ": no getter found");
			}

			try {
				return FactoryDataField.internal2External(getter.invoke(object));
			} catch (Exception e) {
				throw new NodeException("Error while getting field " + fieldName + " from " + object, e);
			}
		}

		/**
		 * Set the field value for the given object
		 * @param object object
		 * @param value value to set
		 * @throws NodeException
		 */
		public void set(NodeObject object, Object value) throws NodeException {
			// when no object found, throw an exception
			if (object == null) {
				throw new NodeException("Cannot set field " + fieldName + " for null object");
			}

			// check whether setter exists
			if (setter == null) {
				throw new NodeException("Cannot set field " + fieldName + " for " + object + ": no setter found");
			}

			try {
				setter.invoke(object, FactoryDataField.external2Internal(value, setter.getParameterTypes()[0]));
			} catch (Exception e) {
				throw new NodeException("Error while setting field " + fieldName + " to " + object, e);
			}
		}
	}
}
