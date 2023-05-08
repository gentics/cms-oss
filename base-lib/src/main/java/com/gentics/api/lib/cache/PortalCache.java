/*
 * @author clemens
 * @date unknown
 * @version $Id: PortalCache.java,v 1.1.4.2 2011-04-07 09:57:53 norbert Exp $
 */
package com.gentics.api.lib.cache;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import com.gentics.lib.cache.JCSPortalCache;
import com.gentics.lib.log.NodeLogger;

/**
 * Abstract Portal Cache implementation
 */
public abstract class PortalCache {
	/**
	 * Constructor for the concrete implementation
	 */
	protected static Constructor<? extends PortalCache> cacheClassConstructor;

	/**
	 * Static "initialize" method of the concrete implementation
	 */
	private static Method cacheClassInitializer = null;

	/**
	 * Defines if cache is disabled. Is set e.g. by JCSPortalCache during initialization if no
	 * cache.ccf could be found.
	 */
	protected static boolean disableCache = false;

	/**
	 * Class of the concrete Cache implementation
	 */
	protected static Class<? extends PortalCache> cacheClass = JCSPortalCache.class;

	/**
	 * remember if the cacheClass has already been set to prevent
	 * re-initialisation
	 */
	protected static boolean allowSetCacheClass = true;

	/**
	 * Flag to mark whether the cache implementation has already been initialized
	 */
	private static boolean initialized = false;

	/**
	 * Region of the cache instance
	 */
	protected String region;

	/**
	 * Initialize the cache implementation (if not done before).
	 * <ol>
	 * <li>Load the cache implementation class</li>
	 * <li>Get the constructor method (with region)</li>
	 * <li>Get the static "initialize" method (if implementation class has one)</li>
	 * </ol>
	 */
	private static void initialize() {
		// do not initialze the cache twice
		if (initialized) {
			return;
		} else {
			initialized = true;
		}
		try {
			Class.forName(cacheClass.getName(), true, PortalCache.class.getClassLoader());
			cacheClassConstructor = cacheClass.getConstructor(new Class[] { String.class});
		} catch (NoSuchMethodException e) {
			NodeLogger.getLogger(PortalCache.class).error("cache class {" + cacheClass + "} does not contain a sufficent constructor", e);
		} catch (ClassNotFoundException ignored) {}
		// try to get the "initialize" method from the configured cache class
		try {
			cacheClassInitializer = cacheClass.getMethod("initialize");
		} catch (NoSuchMethodException ignored) {
			NodeLogger.getLogger(PortalCache.class).debug("cache class {" + cacheClass + "} does not contain a initialize method");
		}
	}

	/**
	 * create an instance for the given region
	 * @param region cache region
	 */
	public PortalCache(String region) {
		// make sure the cache is initialized
		initialize();
		this.region = region;
	}

	/**
	 * retrieve a PortalCache instance
	 * @param region cache region
	 * @return PortalCache instance
	 * @throws PortalCacheException when the cache cannot be instantiated
	 */
	public static PortalCache getCache(String region) throws PortalCacheException {
		// make sure the cache is initialized
		initialize();
		// also the cache class must be initialized
		if (cacheClassInitializer != null) {
			try {
				cacheClassInitializer.invoke(null);
			} catch (ReflectiveOperationException e) {}
		}

		// If cache was disabled, don't try to create instance of cache.
		if (disableCache) {
			return null;
		}
		try {
			return (PortalCache) cacheClassConstructor.newInstance(new Object[] { region});
		} catch (Exception e) {
			NodeLogger.getLogger(PortalCache.class).error("failed to retrieve a cache instance for {" + region + "}", e);
			throw new PortalCacheException("Could not create PortalCache for region {" + region + "}", e);
		}
	}

	/**
	 * sets the cache class to be used
	 * @param cacheClass to be used, null for default implementation
	 * @throws PortalCacheException if the cache class is already set
	 */
	public static void setCacheClass(Class<? extends PortalCache> cacheClass) throws PortalCacheException {
		if (!allowSetCacheClass) {
			throw new PortalCacheException("cacheClass has already been set.");
		}
		allowSetCacheClass = false;

		if (cacheClass != null) {
			PortalCache.cacheClass = cacheClass;
		}
	}

	/**
	 * Get the name of the region of this cache instance.
	 * @return the cache region name
	 */
	public String getRegion() {
		return region;
	}

	/**
	 * retrieve an object from the cache
	 * @param key of the object
	 * @return object identified by key or null
	 * @throws PortalCacheException when the cache cannot be accessed
	 */
	public abstract Object get(Object key) throws PortalCacheException;

	/**
	 * Retrieven an object from the group in the cache
	 * @param groupName group name
	 * @param key key of the object
	 * @return object from the cache or null
	 * @throws PortalCacheException when the cache cannot be accessed
	 */
	public abstract Object getFromGroup(String groupName, Object key) throws PortalCacheException;

	/**
	 * put an object into the cache
	 * @param key of the object
	 * @param object to put inside the cache
	 * @throws PortalCacheException when the cache cannot be accessed
	 */
	public abstract void put(Object key, Object object) throws PortalCacheException;

	/**
	 * Put an object into a group of the cache
	 * @param groupName name of the group
	 * @param key key of the object
	 * @param object from the cache or null
	 * @throws PortalCacheException when the cache cannot be accessed
	 */
	public abstract void putIntoGroup(String groupName, Object key, Object object) throws PortalCacheException;

	/**
	 * put an object into the cache, using some attributes to define caching
	 * methods.
	 * @param key the key of the entry.
	 * @param object the object to put inside the cache.
	 * @param attribs the cache element attributes for the object.
	 * @throws PortalCacheException when the cache cannot be accessed
	 */
	public abstract void put(Object key, Object object, PortalCacheAttributes attribs) throws PortalCacheException;

	/**
	 * put an object into the a group in the cache, using some attributes to
	 * define caching methods.
	 * @param groupName group name
	 * @param key the key of the entry.
	 * @param object the object to put inside the cache.
	 * @param attribs the cache element attributes for the object.
	 * @throws PortalCacheException when the cache cannot be accessed
	 */
	public abstract void putIntoGroup(String groupName, Object key, Object object,
			PortalCacheAttributes attribs) throws PortalCacheException;

	/**
	 * Get the default cache attributes
	 * @return default cache attributes
	 * @throws PortalCacheException when the cache cannot be accessed
	 */
	public abstract PortalCacheAttributes getDefaultCacheAttributes() throws PortalCacheException;

	/**
	 * get the cache attributes for a cached entry. If no attributes have been
	 * set, then null is returned.
	 * @param key the key of the entry.
	 * @return the cache attributes or null if not defined.
	 * @throws PortalCacheException when the cache cannot be accessed
	 */
	public abstract PortalCacheAttributes getCacheAttributes(Object key) throws PortalCacheException;

	/**
	 * get the cache attributes for a cached entry in a group. If no attributes
	 * have been set, then null is returned.
	 * @param groupName group name
	 * @param key the key of the entry.
	 * @return the cache attributes or null if not defined.
	 * @throws PortalCacheException when the cache cannot be accessed
	 */
	public abstract PortalCacheAttributes getCacheAttributes(String groupName, Object key) throws PortalCacheException;

	/**
	 * remove an object from the cache
	 * @param key of object to be removed
	 * @throws PortalCacheException when the cache cannot be accessed
	 */
	public abstract void remove(Object key) throws PortalCacheException;

	/**
	 * remove an object from a group in the cache
	 * @param groupName group name
	 * @param key of object to be removed
	 * @throws PortalCacheException when the cache cannot be accessed
	 */
	public abstract void removeFromGroup(String groupName, Object key) throws PortalCacheException;

	/**
	 * clears the whole cache
	 * @throws PortalCacheException when the cache cannot be accessed
	 */
	public abstract void clear() throws PortalCacheException;

	/**
	 * clears the a group in the cache
	 * @param groupName group name
	 * @throws PortalCacheException when the cache cannot be accessed
	 */
	public abstract void clearGroup(String groupName) throws PortalCacheException;
}
