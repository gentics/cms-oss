/*
 * @author norbert
 * @date 07.03.2007
 * @version $Id: AbstractExtensiblePartType.java,v 1.16.4.2 2011-04-07 09:57:55 norbert Exp $
 */
package com.gentics.api.contentnode.parttype;

import java.util.HashMap;
import java.util.Map;

import com.gentics.api.lib.cache.PortalCache;
import com.gentics.api.lib.cache.PortalCacheException;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.parttype.CMSResolver;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.render.RenderableResolvable;

/**
 * Abstract base implementation of
 * {@link com.gentics.api.contentnode.parttype.ExtensiblePartType}. This class
 * should be the base class for all new implementations of PartTypes.
 * 
 * To use extensible parttypes you need to insert a row into the type table of the cms database.
 * Please note that once the row has been inserted, any changes to the row, such as updating
 * the class name, require a tomcat restart (the part type information is cached internally).
 *
 * To avoid id conflicts please calculate your personal id range for now:
 * take the 1st 4 characters of one of the customers cms license keys.
 * convert them from hex to dec.
 * multiply by 1000. this is the lowest allowed id.
 * add 999. thid is the highest allowed id.
 * eg: license key is A2B4-xxxx-xxxx...
 * hex a2b4 is 41652 in decimal.
 * your personal id range is 41652000 - 41652999.
 */
public abstract class AbstractExtensiblePartType implements ExtensiblePartType {

	/**
	 * name of the cache region for extensible parttypes
	 */
	private final static String CACHE_REGION = "gentics-content-extensibleparttype";

	/**
	 * Creates a new cms resolver and adds it to the stack.
	 * @return the cms resolver
	 * @throws NodeException
	 */
	protected CMSResolver createCMSResolver() throws NodeException {
		return TransactionManager.getCurrentTransaction().getRenderType().getCMSResolver();
	}

	/**
	 * Helper method to resolve property paths in the context of the part
	 * @param path property path to resolve
	 * @return resolved object or null if path could not be resolved
	 * @throws NodeException when an error occurred while resolving
	 */
	public Object resolve(String path) throws NodeException {
		RenderType renderType = TransactionManager.getCurrentTransaction().getRenderType();

		// TODO use a resolver with the correct base objects
		// tag, page, template, object, folder, node, file
		// not suitable base objects may be empty
		return renderType.getStack().resolve(path);
	}
    
	/**
	 * Help method to resolve a property and return it's string representation.
	 * @param path property path to resolve
	 * @return string representation of path
	 * @throws NodeException when an error occurred
	 */
	public String resolveToString(String path) throws NodeException {
		return ObjectTransformer.getString(resolve(path), null);
	}    

	/**
	 * Get the context of the parttype. The context maps base words to the resolved objects.
	 * @param wrapRenderables flag to wrap renderable objects into instances of RenderableResolvable
	 * @return map of base objects
	 * @throws NodeException
	 */
	public Map createContext(boolean wrapRenderables) throws NodeException {
		CMSResolver cmsResolver = createCMSResolver();

		Map baseObjects = new HashMap();

		if (wrapRenderables) {
			baseObjects.put("cms", new RenderableResolvable(cmsResolver));
		} else {
			baseObjects.put("cms", cmsResolver);
		}

		return baseObjects;
	}

	/**
	 * Get the object cached for this parttype class with the given key
	 * @param key cache key
	 * @return cached object or null
	 */
	public Object getCachedObject(Object key) {
		try {
			PortalCache portalCache = PortalCache.getCache(CACHE_REGION);

			if (portalCache != null) {
				return portalCache.getFromGroup(getCacheGroup(), key);
			} else {
				return null;
			}
		} catch (PortalCacheException e) {
			// TODO log a warning?
			return null;
		}
	}

	/**
	 * Put the given object into the cache
	 * @param key cache key
	 * @param object cached object
	 */
	public void putObjectIntoCache(Object key, Object object) {
		try {
			PortalCache portalCache = PortalCache.getCache(CACHE_REGION);

			if (portalCache != null) {
				portalCache.putIntoGroup(getCacheGroup(), key, object);
			}
		} catch (PortalCacheException e) {// TODO log a warning?
		}
	}

	/**
	 * Remove the object with the given key from the cache
	 * @param key cache key
	 */
	public void removeObjectFromCache(Object key) {
		try {
			PortalCache portalCache = PortalCache.getCache(CACHE_REGION);

			if (portalCache != null) {
				portalCache.removeFromGroup(getCacheGroup(), key);
			}
		} catch (PortalCacheException e) {// TODO log a warning?
		}
	}

	/**
	 * Clear the cache
	 */
	public void clearCache() {
		try {
			PortalCache portalCache = PortalCache.getCache(CACHE_REGION);

			if (portalCache != null) {
				portalCache.clearGroup(getCacheGroup());
			}
		} catch (PortalCacheException e) {// TODO log a warning?
		}
	}

	/**
	 * Get the cache group name (=classname)
	 * @return cache group name
	 */
	protected String getCacheGroup() {
		return getClass().getName();
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.contentnode.parttype.ExtensiblePartType#cleanAfterRender()
	 */
	public void cleanAfterRender() {}
}
