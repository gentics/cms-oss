package com.gentics.contentnode.cache;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

import org.apache.jcs.JCS;
import org.apache.jcs.access.exception.CacheException;
import org.apache.jcs.engine.ElementAttributes;
import org.apache.jcs.engine.behavior.IElementAttributes;
import org.apache.jcs.engine.control.CompositeCacheManager;
import org.apache.jcs.engine.control.event.behavior.IElementEventHandler;

import com.gentics.api.lib.cache.PortalCache;
import com.gentics.api.lib.cache.PortalCacheAttributes;
import com.gentics.api.lib.cache.PortalCacheException;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.lib.log.NodeLogger;

/**
 * GCMS Implementation for the cache
 */
public class GCMSCache extends PortalCache {
	/**
	 * Wrapped JCS instance
	 */
	private JCS jcsCache;

	/**
	 * Logger
	 */
	private static NodeLogger logger = NodeLogger.getNodeLogger(GCMSCache.class);

	/**
	 * Flag to mark whether JCS has already been initialized
	 */
	private static boolean initialized = false;

	/**
	 * Initialize the JCS cache configuration and
	 * load properties for JCS cache from cache.ccf file
	 * @throws PortalCacheException
	 */
	public synchronized static void initialize(String path) throws PortalCacheException {
		if (initialized) {
			return;
		} else {
			initialized = true;
		}
		PortalCache.setCacheClass(GCMSCache.class);

		boolean enablePortalCache = ObjectTransformer.getBoolean(System.getProperty("com.gentics.portalnode.portalcache"), true);

		if (!enablePortalCache) {
			if (logger.isDebugEnabled()) {
				logger.debug("Disabling portalcache");
			}
			PortalCache.disableCache = true;
		} else {
			Properties properties = new Properties();

			try (FileInputStream in = new FileInputStream(path)) {
				properties.load(in);
				CompositeCacheManager cManager = CompositeCacheManager.getUnconfiguredInstance();

				cManager.configure(properties);
			} catch (FileNotFoundException e) {
				logger.error("could not load JCS configuration file from {" + path + "}", e);
			} catch (IOException e) {
				logger.error("encountered IOException while trying to read file {" + path + "}", e);
			}
		}
	}

	/**
	 * create an instance for the given region
	 * @param region cache region
	 * @throws PortalCacheException
	 */
	public GCMSCache(String region) throws PortalCacheException {
		super(region);
		try {
			jcsCache = JCS.getInstance(region);
		} catch (Exception e) {
			throw new PortalCacheException("Could not create JCSPortalCache for region {" + region + "}", e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.portalnode.cache.PortalCache#get(java.lang.Object)
	 */
	public Object get(Object key) throws PortalCacheException {
		return jcsCache.get(key);
	}

	/* (non-Javadoc)
	 * @see com.gentics.portalnode.cache.PortalCache#getFromGroup(java.lang.String, java.lang.Object)
	 */
	public Object getFromGroup(String groupName, Object key) throws PortalCacheException {
		try {
			// RuntimeProfiler.beginMark(ComponentsConstants.PORTALCACHE_JCS_GET);
			return jcsCache.getFromGroup(key, groupName);
		} finally {// RuntimeProfiler.endMark(ComponentsConstants.PORTALCACHE_JCS_GET);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.portalnode.cache.PortalCache#put(java.lang.Object,
	 *      java.lang.Object)
	 */
	public void put(Object key, Object object) throws PortalCacheException {
		try {
			if (logger.isDebugEnabled()) {
				logger.debug("cache put for region {" + region + "} key {" + key + "}");
			}
			jcsCache.put(key, object);
		} catch (CacheException e) {
			throw new PortalCacheException("unable to put object into cache", e);
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.portalnode.cache.PortalCache#putIntoGroup(java.lang.String, java.lang.Object, java.lang.Object)
	 */
	public void putIntoGroup(String groupName, Object key, Object object) throws PortalCacheException {
		try {
			if (logger.isDebugEnabled()) {
				logger.debug("cache put for region {" + region + "} key {" + key + "} group {" + groupName + "}");
			}
			jcsCache.putInGroup(key, groupName, object);
		} catch (CacheException e) {
			throw new PortalCacheException("unable to put object into cache", e);
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.portalnode.cache.PortalCache#put(java.lang.Object, java.lang.Object, com.gentics.portalnode.cache.PortalCacheAttributes)
	 */
	public void put(Object key, Object object, PortalCacheAttributes attribs) throws PortalCacheException {
		try {
			if (logger.isDebugEnabled()) {
				logger.debug("cache put for region {" + region + "} key {" + key + "}");
			}
			if (attribs instanceof IElementAttributesWrapper) {
				jcsCache.put(key, object, ((IElementAttributesWrapper) attribs).wrappedAttributes);
			} else {
				jcsCache.put(key, object, new JCSCacheAttributes(attribs));
			}
		} catch (CacheException e) {
			throw new PortalCacheException("unable to put object into cache", e);
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.portalnode.cache.PortalCache#putIntoGroup(java.lang.String, java.lang.Object, java.lang.Object, com.gentics.portalnode.cache.PortalCacheAttributes)
	 */
	public void putIntoGroup(String groupName, Object key, Object object, PortalCacheAttributes attribs) throws PortalCacheException {
		try {
			if (logger.isDebugEnabled()) {
				logger.debug("cache put for region {" + region + "} key {" + key + "} group {" + groupName + "}");
			}
			if (attribs instanceof IElementAttributesWrapper) {
				jcsCache.putInGroup(key, groupName, object, ((IElementAttributesWrapper) attribs).wrappedAttributes);
			} else {
				jcsCache.putInGroup(key, groupName, object, new JCSCacheAttributes(attribs));
			}
		} catch (CacheException e) {
			throw new PortalCacheException("unable to put object into cache", e);
		}
	}

	@Override
	public PortalCacheAttributes getDefaultCacheAttributes() throws PortalCacheException {
		try {
			IElementAttributes jcsAttribs = jcsCache.getDefaultElementAttributes();
			if (jcsAttribs != null) {
				return new IElementAttributesWrapper(jcsAttribs);
			} else {
				return null;
			}
		} catch (CacheException e) {
			throw new PortalCacheException("unable to get default cache attributes", e);
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.portalnode.cache.PortalCache#getCacheAttributes(java.lang.Object)
	 */
	public PortalCacheAttributes getCacheAttributes(Object key) throws PortalCacheException {
		try {
			IElementAttributes jcsAttribs = jcsCache.getElementAttributes(key);

			if (jcsAttribs != null) {
				return new IElementAttributesWrapper(jcsAttribs);
			} else {
				return null;
			}
		} catch (CacheException e) {
			throw new PortalCacheException("unable to get cache attributes", e);
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.portalnode.cache.PortalCache#getCacheAttributes(java.lang.String, java.lang.Object)
	 */
	public PortalCacheAttributes getCacheAttributes(String groupName, Object key) throws PortalCacheException {
		try {
			IElementAttributes jcsAttribs = jcsCache.getElementAttributes(key);

			if (jcsAttribs != null) {
				return new IElementAttributesWrapper(jcsAttribs);
			} else {
				return null;
			}
		} catch (CacheException e) {
			throw new PortalCacheException("unable to get cache attributes", e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.portalnode.cache.PortalCache#remove(java.lang.Object)
	 */
	public void remove(Object key) throws PortalCacheException {
		try {
			if (logger.isDebugEnabled()) {
				logger.debug("cache remove for region {" + region + "} key {" + key + "}");
			}
			jcsCache.remove(key);
		} catch (CacheException e) {
			throw new PortalCacheException("unable to remove object from cache", e);
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.portalnode.cache.PortalCache#removeFromGroup(java.lang.String, java.lang.Object)
	 */
	public void removeFromGroup(String groupName, Object key) throws PortalCacheException {
		// due to an inkonsistency in the JCS API, this method does not throw any exceptions
		// try {
		if (logger.isDebugEnabled()) {
			logger.debug("cache remove for region {" + region + "} key {" + key + "} group {" + groupName + "}");
		}
		jcsCache.remove(key, groupName);
		// } catch (CacheException e) {
		// throw new PortalCacheException("unable to remove object from cache", e);
		// }
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.portalnode.cache.PortalCache#clear()
	 */
	public void clear() throws PortalCacheException {
		try {
			if (logger.isDebugEnabled()) {
				logger.debug("cache clear for region {" + region + "}");
			}
			jcsCache.clear();
		} catch (CacheException e) {
			throw new PortalCacheException("unable to remove object from cache", e);
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.portalnode.cache.PortalCache#clear(java.lang.String)
	 */
	public void clearGroup(String groupName) throws PortalCacheException {
		// due to an inkonsistency in the JCS API, this method does not throw any exceptions
		// try {
		if (logger.isDebugEnabled()) {
			logger.debug("cache clear for region {" + region + "} group {" + groupName + "}");
		}
		jcsCache.invalidateGroup(groupName);
		// } catch (CacheException e) {
		// throw new PortalCacheException("unable to remove object from cache", e);
		// }
	}

	/**
	 * Get the underlying JCS Cache instance.
	 * @return the jcs cache
	 */
	public JCS getJcsCache() {
		return jcsCache;
	}
}

/**
 * Wrapper implementation for {@link IElementAttributes} fetched from JCS
 */
class IElementAttributesWrapper implements PortalCacheAttributes {
	protected IElementAttributes wrappedAttributes;

	protected IElementAttributesWrapper(IElementAttributes wrappedAttributes) {
		this.wrappedAttributes = wrappedAttributes;
	}

	@Override
	public boolean getIsEternal() {
		return wrappedAttributes.getIsEternal();
	}

	@Override
	public void setIsEternal(boolean isEternal) {
		wrappedAttributes.setIsEternal(isEternal);
	}

	@Override
	public long getCreateDate() {
		return wrappedAttributes.getCreateTime();
	}

	@Override
	public long getLastAccessDate() {
		return wrappedAttributes.getLastAccessTime();
	}

	@Override
	public void setLastAccessDateToNow() {
		wrappedAttributes.setLastAccessTimeNow();
	}

	@Override
	public int getMaxAge() {
		return (int) wrappedAttributes.getMaxLifeSeconds();
	}

	@Override
	public void setMaxAge(int maxAge) {
		wrappedAttributes.setMaxLifeSeconds(maxAge);
	}

	@Override
	public int getMaxIdleTime() {
		return (int) wrappedAttributes.getIdleTime();
	}

	@Override
	public void setMaxIdleTime(int maxIdleTime) {
		wrappedAttributes.setIdleTime(maxIdleTime);
	}

	@Override
	public int getSize() {
		return wrappedAttributes.getSize();
	}

	@Override
	public void setSize(int size) {
		wrappedAttributes.setSize(size);
	}
}

/**
 * This is a wrapper implementation for the portalcacheattributes. Because the
 * JCS cache has a bug, this must extend elementattribute, not only implement
 * the IElementAttributes interface
 */
class JCSCacheAttributes extends ElementAttributes {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4081710945130558555L;

	private PortalCacheAttributes attribs;

	private long version;

	public JCSCacheAttributes(PortalCacheAttributes attribs) {
		this.attribs = attribs;
	}

	public PortalCacheAttributes getCacheAttributes() {
		return attribs;
	}

	public void setVersion(long l) {
		this.version = l;
	}

	public void setMaxLifeSeconds(long l) {
		attribs.setMaxAge((int) l);
	}

	public long getMaxLifeSeconds() {
		return attribs.getMaxAge();
	}

	public void setIdleTime(long l) {
		attribs.setMaxIdleTime((int) l);
	}

	public void setSize(int i) {
		attribs.setSize(i);
	}

	public int getSize() {
		return attribs.getSize();
	}

	public long getCreateTime() {
		return attribs.getCreateDate();
	}

	public long getLastAccessTime() {
		return attribs.getLastAccessDate();
	}

	public void setLastAccessTimeNow() {
		attribs.setLastAccessDateToNow();
	}

	public long getVersion() {
		return version;
	}

	public long getIdleTime() {
		return attribs.getMaxIdleTime();
	}

	public long getTimeToLiveSeconds() {
		long now = System.currentTimeMillis();
		long create = attribs.getCreateDate();
		int maxAge = attribs.getMaxAge();
		long access = attribs.getLastAccessDate();
		int maxIdle = attribs.getMaxIdleTime();
		long ttl = 0;

		// get the ttl depending on what options are set.
		if (create != 0 && maxAge != 0 && access != 0 && maxIdle != 0) {
			long age = (create + maxAge * 1000);
			long idle = (access + maxIdle * 1000);

			ttl = age < idle ? age - now : idle - now;
		} else if (create != 0 && maxAge != 0) {
			ttl = (create + maxAge * 1000) - now;
		} else if (access != 0 && maxIdle != 0) {
			ttl = (access + maxIdle * 1000) - now;
		}

		return ttl / 1000;
	}

	public IElementAttributes copy() {
		// TODO deep copy?
		return new JCSCacheAttributes(attribs);
	}

	public boolean getIsSpool() {
		return false;
	}

	public void setIsSpool(boolean b) {}

	public boolean getIsLateral() {
		return true;
	}

	public void setIsLateral(boolean b) {}

	public boolean getIsRemote() {
		return false;
	}

	public void setIsRemote(boolean b) {}

	public boolean getIsEternal() {
		return attribs.getIsEternal();
	}

	public void setIsEternal(boolean b) {}

	public void addElementEventHandler(IElementEventHandler iElementEventHandler) {}

	public ArrayList getElementEventHandlers() {
		return null;
	}

	public void addElementEventHandlers(ArrayList arrayList) {}
}
