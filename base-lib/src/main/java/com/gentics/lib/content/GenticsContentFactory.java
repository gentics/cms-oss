/*
 * @author Erwin Mascher (e.mascher@gentics.com)
 * @date 20.10.2003
 * @version $Id: GenticsContentFactory.java,v 1.1.4.1 2011-04-07 09:57:53 norbert Exp $
 */
package com.gentics.lib.content;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gentics.api.lib.cache.PortalCache;
import com.gentics.api.lib.cache.PortalCacheException;
import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.lib.datasource.DatasourceException;
import com.gentics.api.lib.datasource.DatasourceHandle;
import com.gentics.api.lib.datasource.DatasourceRow;
import com.gentics.api.lib.datasource.HandlePool;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.lib.base.CMSUnavailableException;
import com.gentics.lib.base.NodeIllegalArgumentException;
import com.gentics.lib.content.DatatypeHelper.AttributeType;
import com.gentics.lib.datasource.CNDatasource;
import com.gentics.lib.datasource.SQLHandle;
import com.gentics.lib.db.DBHandle;
import com.gentics.lib.db.SimpleResultRow;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.log.NodeLogger;

import gnu.trove.THashMap;

/**
 * helper class to create GenticsContentObjects
 * @author Erwin Mascher (e.mascher@gentics.com)
 */

public final class GenticsContentFactory {
	/**
	 * Cached null value
	 */
	public final static Object CACHED_NULL = new Object();

	/**
	 * static sequence for creation of unique temporary contentid's for new objects
	 */
	private static long idSequence = 0;

	/**
	 * Name of the cacheregion for objects
	 */
	public final static String OBJECTCACHEREGION = "gentics-portal-contentrepository-objects";

	/**
	 * Name of the cacheregion for attributes
	 */
	public final static String ATTRIBUTESCACHEREGION = "gentics-portal-contentrepository-atts";

	/**
	 * flag to mark whether the cache is activated or not
	 */
	protected static boolean cacheActivated = false;

	static {
		try {
			// get the portal cache for contentobjects
			PortalCache genticsContentObjectCache = PortalCache.getCache(OBJECTCACHEREGION);
			// get the portal cache for contentattributes
			PortalCache genticsContentAttributeCache = PortalCache.getCache(ATTRIBUTESCACHEREGION);

			if (genticsContentObjectCache != null && genticsContentAttributeCache != null) {
				cacheActivated = true;
			}
		} catch (PortalCacheException e) {
			NodeLogger.getNodeLogger(GenticsContentFactory.class).error(
					"Error while initializing the portal cache for the GenticsContentFactory." + " GenticsContentObjects will not be cached.", e);
		} catch (NoClassDefFoundError e) {
			NodeLogger.getNodeLogger(GenticsContentFactory.class).error(
					"Error while initializing the portal cache for the GenticsContentFactory." + " GenticsContentObjects will not be cached.", e);
		}
	}

	/**
	 * Creates a GenticsContentObject representing the given contentId
	 * @param contentId the object's contentId
	 * @param handlepool pool of db handles
	 * @return a GenticsContentObject or null if this contentId does not exists
	 * @deprecated use {@link #createContentObject(String, Datasource)} instead
	 */
	public static GenticsContentObject createContentObject(String contentId,
			HandlePool handlepool) throws CMSUnavailableException, NodeIllegalArgumentException {
		SQLHandle handle = (SQLHandle) handlepool.getHandle();

		if (handle == null) {
			throw new CMSUnavailableException("Could not find a valid handle");
		}
		return createContentObject(contentId, handle.getDBHandle());
	}

	/**
	 * create a contentobject with given contentid
	 * @param contentId contentid
	 * @param handle database handle
	 * @return the content object if it exists or null
	 * @throws CMSUnavailableException
	 * @throws NodeIllegalArgumentException
	 * @deprecated use {@link #createContentObject(String, Datasource)} instead
	 */
	public static GenticsContentObject createContentObject(String contentId, DBHandle handle) throws CMSUnavailableException, NodeIllegalArgumentException {
		return createContentObject(contentId, handle, true);
	}

	/**
	 * create a contentobject with given contentid
	 * @param contentId contentid
	 * @param handle database handle
	 * @param checkExistance true when existance of objects needs to be checked,
	 *        false if not
	 * @return the content object if it exists or null
	 * @throws CMSUnavailableException
	 * @throws NodeIllegalArgumentException
	 * @deprecated use {@link #createContentObject(String, Datasource, boolean)} instead
	 */
	public static GenticsContentObject createContentObject(String contentId, DBHandle handle,
			boolean checkExistance) throws CMSUnavailableException,
				NodeIllegalArgumentException {
		GenticsContentObjectImpl ret = new GenticsContentObjectImpl(contentId, handle);

		if (!checkExistance || ret.exists()) {
			return ret;
		}
		return null;
	}

	/**
	 * create a contentobject with given contentid at a given timestamp
	 * @param contentId contentid
	 * @param handle database handle
	 * @param timestamp timestamp of the version
	 * @param checkExistance true when the existance of the object has to be
	 *        checked
	 * @return the content object if it exists or null
	 * @throws CMSUnavailableException
	 * @throws NodeIllegalArgumentException
	 * @deprecated use {@link #createContentObject(String, Datasource, int, boolean)} instead
	 */
	public static GenticsContentObject createContentObject(String contentId, DBHandle handle,
			int timestamp, boolean checkExistance) throws CMSUnavailableException,
				NodeIllegalArgumentException {
		GenticsContentObjectImpl ret = new GenticsContentObjectImpl(contentId, handle, timestamp);

		if (timestamp != -1 || (!checkExistance || (ret != null && ret.exists()))) {
			return ret;
		}

		return null;
	}

	/**
	 * create a contentobject with given contentid at a given timestamp
	 * @param contentId contentid
	 * @param handle database handle
	 * @param timestamp timestamp of the version
	 * @return the content object if it exists or null
	 * @throws CMSUnavailableException
	 * @throws NodeIllegalArgumentException
	 * @deprecated use {@link #createContentObject(String, Datasource, int)} instead
	 */
	public static GenticsContentObject createContentObject(String contentId, DBHandle handle,
			int timestamp) throws CMSUnavailableException, NodeIllegalArgumentException {
		return createContentObject(contentId, handle, timestamp, true);
	}

	/**
	 * cretae a new (versioned) contentobject of given type at the given
	 * timestamp
	 * @param type type of the contentobject to create
	 * @param handle database handle
	 * @param timestamp timestamp of the object's version
	 * @return the new contentobject
	 * @throws CMSUnavailableException
	 * @throws NodeIllegalArgumentException
	 * @deprecated use {@link #createContentObject(int, Datasource, int)} instead
	 */
	public static GenticsContentObject createContentObject(int type, DBHandle handle,
			int timestamp) throws CMSUnavailableException, NodeIllegalArgumentException {
		GenticsContentObjectImpl ret = new GenticsContentObjectImpl(type, handle, timestamp, getUniqueTemporaryContentId());

		return ret;
	}

	/**
	 * create a new contentobject of given type
	 * @param type type of the contentobject to create
	 * @param handle database handle
	 * @return the new contentobject
	 * @throws CMSUnavailableException
	 * @throws NodeIllegalArgumentException
	 * @deprecated use {@link #createContentObject(int, Datasource)} instead
	 */
	public static GenticsContentObject createContentObject(int type, DBHandle handle) throws CMSUnavailableException, NodeIllegalArgumentException {
		GenticsContentObjectImpl ret = new GenticsContentObjectImpl(type, handle, getUniqueTemporaryContentId());

		return ret;
	}

	/**
	 * create a new contentobject of given type
	 * @param type type of the contentobject to create
	 * @param handlepool pool of db handles
	 * @return the new contentobject
	 * @throws CMSUnavailableException
	 * @throws NodeIllegalArgumentException
	 * @deprecated use {@link #createContentObject(int, Datasource)} instead
	 */
	public static GenticsContentObject createContentObject(int type, HandlePool handlepool) throws CMSUnavailableException, NodeIllegalArgumentException {
		SQLHandle handle = (SQLHandle) handlepool.getHandle();

		if (handle == null) {
			throw new CMSUnavailableException("Could not find a valid handle");
		}
		return createContentObject(type, handle.getDBHandle());
	}

	/**
	 * create a new contentobject of given type
	 * @param type type of the contentobject to create
	 * @param ds datasource used to handle the object
	 * @return the new contentobject
	 * @throws CMSUnavailableException
	 * @throws NodeIllegalArgumentException
	 */
	public static GenticsContentObject createContentObject(int type, Datasource ds) throws CMSUnavailableException, NodeIllegalArgumentException {
		return createContentObject(type, ds, null);
	}

	/**
	 * Create a new contentobject of given type with given temporary contentid
	 * @param type type of the contentobject to create
	 * @param ds datasource used to handle the object
	 * @param temporaryContentId temporary contentid (when null, a new temporary contentid will be created)
	 * @return the new contentobject
	 * @throws CMSUnavailableException
	 * @throws NodeIllegalArgumentException
	 */
	public static GenticsContentObject createContentObject(int type, Datasource ds, String temporaryContentId) throws CMSUnavailableException, NodeIllegalArgumentException {
		GenticsContentObjectImpl ret = new GenticsContentObjectImpl(type, getHandle(ds),
				temporaryContentId != null ? temporaryContentId : getUniqueTemporaryContentId());

		ret.setDatasource(ds);
		return ret;
	}

	/**
	 * create a new contentobject of givne type at the given timestamp
	 * @param type type of the contentobject to create
	 * @param ds datasource used to handle the object
	 * @param timestamp timestamp of the object's version
	 * @return the new contentobject
	 * @throws CMSUnavailableException
	 * @throws NodeIllegalArgumentException
	 */
	public static GenticsContentObject createContentObject(int type, Datasource ds,
			int timestamp) throws CMSUnavailableException, NodeIllegalArgumentException {
		return createContentObject(type, ds, timestamp, null);
	}

	/**
	 * create a new contentobject of given type at the given timestamp
	 * @param type type of the contentobject to create
	 * @param ds datasource used to handle the object
	 * @param timestamp timestamp of the object's version
	 * @param temporaryContentId temporary contentid (when null, a new temporary contentid will be created)
	 * @return the new contentobject
	 * @throws CMSUnavailableException
	 * @throws NodeIllegalArgumentException
	 */
	public static GenticsContentObject createContentObject(int type, Datasource ds,
			int timestamp, String temporaryContentId) throws CMSUnavailableException, NodeIllegalArgumentException {
		GenticsContentObjectImpl ret = new GenticsContentObjectImpl(type, getHandle(ds), timestamp,
				temporaryContentId != null ? temporaryContentId : getUniqueTemporaryContentId());

		ret.setDatasource(ds);
		return ret;
	}

	/**
	 * create a contentobject with given id
	 * @param contentId contentid
	 * @param ds datasource used to handle the object
	 * @return the contentobject or null if it does not exist
	 * @throws CMSUnavailableException
	 * @throws NodeIllegalArgumentException
	 */
	public static GenticsContentObject createContentObject(String contentId, Datasource ds) throws CMSUnavailableException, NodeIllegalArgumentException {
		return createContentObject(contentId, ds, -1, true);
	}

	/**
	 * create a versioned contentobject with given id
	 * @param contentId contentid
	 * @param ds datasource used to handle the object
	 * @param timestamp of the object'S version
	 * @return the contentobject at the timestamp
	 * @throws CMSUnavailableException
	 * @throws NodeIllegalArgumentException
	 */
	public static GenticsContentObject createContentObject(String contentId, Datasource ds,
			int timestamp) throws CMSUnavailableException, NodeIllegalArgumentException {
		return createContentObject(contentId, ds, timestamp, true);
	}

	/**
	 * Create a contentobject with given contentid
	 * @param contentId contentid
	 * @param ds datasource used to handle the object
	 * @param checkExistance true when existance of objects need to be checked, false if not
	 * @return the content object if exists or null
	 * @throws CMSUnavailableException
	 * @throws NodeIllegalArgumentException
	 */
	public static GenticsContentObject createContentObject(String contentId, Datasource ds,
			boolean checkExistance) throws CMSUnavailableException,
				NodeIllegalArgumentException {
		return createContentObject(contentId, ds, -1, checkExistance);
	}

	/**
	 * Create a contentobject with metadata given in the row
	 * @param row resultrow holding the objects metadata
	 * @param ds datasource
	 * @param timestamp version timestamp, -1 for the current version
	 * @return the GenticsContentObject
	 */
	public static GenticsContentObject createContentObject(SimpleResultRow row, Datasource ds,
			int timestamp) {
		// create the object (no need to fetch it from the cache, since all
		// metadata are present)
		GenticsContentObject newObject = new GenticsContentObjectImpl(row, ds, timestamp);

		try {
			// put the object into the cache
			cacheObject(ds, newObject);
		} catch (Exception e) {
			NodeLogger.getNodeLogger(GenticsContentFactory.class).warn("Error while caching object", e);
		}
		return newObject;
	}

	/**
	 * Create a contentobject with given contentid at a given timestamp
	 * @param contentId contentid
	 * @param ds datasource used to handle the object
	 * @param timestamp timestamp of the version
	 * @param checkExistance true when existance of objects need to be checked, false if not
	 * @return the content object if exists or null
	 * @throws CMSUnavailableException
	 * @throws NodeIllegalArgumentException
	 */
	public static GenticsContentObject createContentObject(String contentId, Datasource ds,
			int timestamp, boolean checkExistance) throws CMSUnavailableException, NodeIllegalArgumentException {
		// first try to fetch the object from the cache
		GenticsContentObject cachedObject;

		try {
			cachedObject = getCachedObject(ds, contentId, timestamp);
			if (cachedObject != null) {
				return cachedObject;
			}
		} catch (PortalCacheException e) {
			NodeLogger.getNodeLogger(GenticsContentFactory.class).warn("Error while fetching cached object", e);
		}

		// no cached object, so create a new object
		if (ds == null) {
			throw new NodeIllegalArgumentException("Datasource is null. Perhaps GenticsContentModule was not yet initialized or datasource setting is wrong.");
		}
		GenticsContentObjectImpl ret = new GenticsContentObjectImpl(contentId, getHandle(ds), timestamp);

		ret.setDatasource(ds);
		if (!checkExistance || ret.exists()) {
			try {
				// put the object into the cache
				cacheObject(ds, ret);
			} catch (Exception e) {
				NodeLogger.getNodeLogger(GenticsContentFactory.class).warn("Error while caching object", e);
			}
			return ret;
		}
		return null;
	}

	public static GenticsContentResult createResultFromAttribute(GenticsContentAttribute attr) throws CMSUnavailableException {
		GenticsContentObject rel;
		Vector v = new Vector();

		while ((rel = attr.getNextContentObject()) != null) {
			v.add(rel);
		}
		return new GenticsContentResultImpl(v);
	}

	public static GenticsContentResult createResultFromObject(GenticsContentObject obj) throws CMSUnavailableException {
		Vector v = new Vector();

		v.add(obj);
		return new GenticsContentResultImpl(v);
	}

	public static GenticsContentResult createEmptyResult() throws CMSUnavailableException {
		return new GenticsContentResultImpl(new Vector());
	}

	public static GenticsContentObject[] createContentObjects(Datasource datasource,
			String[] contentIds, String[] prefetchAttribs) throws CMSUnavailableException,
				NodeIllegalArgumentException {
		// TODO use cache here
		DBHandle handle = getHandle(datasource);

		GenticsContentObject[] objects = new GenticsContentObject[contentIds.length];

		for (int i = 0; i < contentIds.length; i++) {
			objects[i] = new GenticsContentObjectImpl(contentIds[i], handle);
		}
		prefillContentObjects(datasource, objects, prefetchAttribs);
		return objects;
	}

	public static void prefillContentObjects(Datasource datasource,
			GenticsContentObject[] contentObjects, String[] prefetchAttribs) throws CMSUnavailableException, NodeIllegalArgumentException {
		GenticsContentObjectImpl.prefillContentObjects(datasource, contentObjects, prefetchAttribs, false);
	}

	/**
	 * prefill the objects in the array with values for the given attributes
	 * @param datasource datasource
	 * @param contentObjects array of contentobjects
	 * @param prefetchAttribs array of attribute names to prefill in the objects
	 * @param timestamp timestamp for versioned data
	 * @param omitLinkedObjectsIfPossible omit fetching of linked objects, if this is possible
	 * @throws CMSUnavailableException
	 * @throws NodeIllegalArgumentException
	 */
	public static void prefillContentObjects(Datasource datasource,
			GenticsContentObject[] contentObjects, String[] prefetchAttribs, int timestamp,
			boolean omitLinkedObjectsIfPossible) throws CMSUnavailableException, NodeIllegalArgumentException {
		GenticsContentObjectImpl.prefillContentObjects(datasource, contentObjects, prefetchAttribs, timestamp, omitLinkedObjectsIfPossible);
	}

	/**
	 * prefill the objects in the collection with the values for the given
	 * attributes
	 * @param datasource datasource
	 * @param objects collection of GenticsContentObject's or
	 *        ResolvableGenticsContentObject's
	 * @param prefetchAttribs array of attribute names to prefill in the objects
	 * @param timestamp
	 * @param omitLinkedObjectsIfPossible omit fetching of linked objects, if this is possible
	 * @throws CMSUnavailableException
	 * @throws NodeIllegalArgumentException
	 */
	public static void prefillContentObjects(Datasource datasource, Collection objects,
			String[] prefetchAttribs, int timestamp, boolean omitLinkedObjectsIfPossible) throws CMSUnavailableException,
				NodeIllegalArgumentException {
		GenticsContentObject[] objectArray = new GenticsContentObject[objects.size()];
		int i = 0;

		for (Iterator iter = objects.iterator(); iter.hasNext();) {
			Object element = (Object) iter.next();

			if (element instanceof GenticsContentObject) {
				objectArray[i++] = (GenticsContentObject) element;
			} else if (element instanceof ResolvableGenticsContentObject) {
				objectArray[i++] = ((ResolvableGenticsContentObject) element).getContentobject();
			} else if (element instanceof DatasourceRow) {
				Object rowObject = ((DatasourceRow) element).toObject();

				if (rowObject instanceof GenticsContentObject) {
					objectArray[i++] = (GenticsContentObject) rowObject;
				} else if (rowObject instanceof ResolvableGenticsContentObject) {
					objectArray[i++] = ((ResolvableGenticsContentObject) rowObject).getContentobject();
				} else {
					throw new NodeIllegalArgumentException("collection may only contain GenticsContentObjects and ResolvableGenticsContentObjects");
				}
			} else if (element != null) {
				throw new NodeIllegalArgumentException("collection may only contain GenticsContentObjects and ResolvableGenticsContentObjects");
			}
		}

		prefillContentObjects(datasource, objectArray, prefetchAttribs, timestamp, omitLinkedObjectsIfPossible);
	}

	/**
	 * prefill the objects in the collection with the values for the given
	 * attributes
	 * @param datasource datasource
	 * @param objects collection of GenticsContentObject's or
	 *        ResolvableGenticsContentObject's
	 * @param prefetchAttribs array of attribute names to prefill in the objects
	 * @throws CMSUnavailableException
	 * @throws NodeIllegalArgumentException
	 */
	public static void prefillContentObjects(Datasource datasource, Collection objects,
			String[] prefetchAttribs) throws CMSUnavailableException,
				NodeIllegalArgumentException {
		prefillContentObjects(datasource, objects, prefetchAttribs, -1, false);
	}

	/**
	 * Prefill the objects in the collection with the values for the given
	 * attributes
	 * @param datasource datasource
	 * @param objects collection of objects to prefill
	 * @param prefetchAttribs array of attribute names to prefill in the objects
	 * @param versiontimestamp versiontimestamp (-1 for current version)
	 * @throws CMSUnavailableException
	 * @throws NodeIllegalArgumentException
	 */
	public static void prefillContentObjects(Datasource datasource, Collection objects,
			String[] prefetchAttribs, int versiontimestamp) throws CMSUnavailableException,
				NodeIllegalArgumentException {
		prefillContentObjects(datasource, objects, prefetchAttribs, versiontimestamp, false);
	}

	/**
	 * Get a unique temporary contentid for a new object
	 * @return unique temporary contentid
	 */
	protected static synchronized String getUniqueTemporaryContentId() {
		return String.valueOf(++idSequence);
	}

	/**
	 * Check whether the given contentid is temporary
	 * @param contentid contentid to check
	 * @return true when the contentid is temporary, false if not
	 */
	public final static boolean isTemporary(String contentid) {
		return contentid != null && contentid.indexOf('.') < 0;
	}

	/**
	 * Get the cached object for the given contentid and timestamp from the
	 * given datasource
	 * @param ds datasource responsible for the object
	 * @param contentid contentid of the object
	 * @param timestamp version timestamp of the objects version
	 * @return the cached object or null if none cached
	 * @throws PortalCacheException
	 */
	public static GenticsContentObject getCachedObject(Datasource ds, String contentid, int timestamp) throws PortalCacheException {
		GenticsContentObjectImpl cachedObject = null;

		if (cacheActivated) {
			PortalCache genticsContentObjectCache = PortalCache.getCache(OBJECTCACHEREGION);
			Object cacheKey = getCacheKey(ds, contentid, timestamp);

			if (cacheKey != null) {
				Object object = genticsContentObjectCache.getFromGroup(ds.toString(), cacheKey);

				if (object instanceof GenticsContentObjectImpl) {
					try {
						// clone the original cached object
						cachedObject = (GenticsContentObjectImpl) ((GenticsContentObjectImpl) object).clone();
						cachedObject.prepareForUseAfterCaching(ds);
					} catch (CloneNotSupportedException e) {
						NodeLogger.getNodeLogger(GenticsContentFactory.class).error("Error while fetching object from cache", e);
					}
				}
			}
		}
		return cachedObject;
	}

	/**
	 * Get the cached data map containing cached attributes for the given cache key from the given attribute cache.
	 * The returned map is synchronized and may therefore be modified to change cached values.
	 * @param ds datasource
	 * @param cacheKey cache key
	 * @param attributeCache attribute cache
	 * @param createIfNull true to create a new map if none exists, false to return null if no map is cached
	 * @return the cached map (synchronized) or null if no map is cached
	 * @throws PortalCacheException
	 */
	protected static Map<String, Object> getCachedDataMap(Datasource ds, Object cacheKey, PortalCache attributeCache, boolean createIfNull)
			throws PortalCacheException {
		Map<String, Object> cachedDataMap = null;
		Object cachedData = attributeCache.getFromGroup(ds.toString(), cacheKey);
		if (cachedData instanceof Map<?, ?>) {
			cachedDataMap = (Map<String, Object>) cachedData;
		} else if (createIfNull) {
			cachedDataMap = Collections.synchronizedMap(new THashMap(1));
			attributeCache.putIntoGroup(ds.toString(), cacheKey, cachedDataMap);
		}
		return cachedDataMap;
	}

	/**
	 * Get the cached attribute for the given object
	 * @param ds datasource responsible for the object
	 * @param object object
	 * @param name name of the cached attribute
	 * @return instance of the cached attribute or null if none cached
	 * @throws PortalCacheException
	 */
	public static GenticsContentAttribute getCachedAttribute(Datasource ds,
			GenticsContentObject object, String name) throws PortalCacheException {
		GenticsContentAttributeImpl cachedAttribute = null;

		if (cacheActivated) {
			if (ds instanceof CNDatasource) {
				CNDatasource cnDs = (CNDatasource) ds;
				String cacheRegion = cnDs.getAttributeCacheRegion(name);

				if (cacheRegion == null) {
					return null;
				}
				PortalCache attributeCache = PortalCache.getCache(cacheRegion);

				if (attributeCache == null) {
					return null;
				}

				Object cacheKey = getCacheKey(ds, object.getContentId(), object.getVersionTimestamp());
				if (cacheKey != null) {
					Map<String, Object> cachedDataMap = getCachedDataMap(cnDs, cacheKey, attributeCache, false);
					if (cachedDataMap != null) {
						// check whether the attribute is cached
						if (!cachedDataMap.containsKey(name)) {
							return null;
						}
						Object cachedValue = cachedDataMap.get(name);
						if (GenticsContentFactory.CACHED_NULL.equals(cachedValue)) {
							cachedValue = null;
						}
						Object[] cachedValues = null;
						if (cachedValue instanceof Object[]) {
							cachedValues = (Object[])cachedValue;
						} else {
							cachedValues = new Object[]{cachedValue};
						}
						if (cachedValues != null) {
							try {
								DBHandle handle = cnDs.getHandle().getDBHandle();
								cachedAttribute = new GenticsContentAttributeImpl(object, handle, name, cachedValues, DatatypeHelper.getDatatype(handle,
										name), DatatypeHelper.isMultivalue(handle, name), DatatypeHelper.isFilesystem(handle, name));
								cachedAttribute.prepareForUseAfterCaching();
							} catch (Exception e) {
								NodeLogger.getNodeLogger(GenticsContentFactory.class).warn("Error while getting cached attribute", e);
							}
						}
					}
				}
			}
		}
		return cachedAttribute;
	}

	/**
	 * Put the given object into the cache
	 * @param ds datasource
	 * @param object object to put into the cache
	 * @throws PortalCacheException
	 * @throws CloneNotSupportedException
	 */
	public static void cacheObject(Datasource ds, GenticsContentObject object) throws PortalCacheException, CloneNotSupportedException {
		// only cache the object when the cache is enabled and the object really
		// exists (the call to object.exists() additionally initializes the
		// object so that this need not be done again)
		if (cacheActivated && (ds instanceof CNDatasource) && ((CNDatasource) ds).isCacheEnabled() && object.exists()) {
			Object cacheKey = getCacheKey(ds, object.getContentId(), object.getVersionTimestamp());

			if (cacheKey != null) {
				// clone the object before putting it into the cache
				GenticsContentObjectImpl cachedObject = (GenticsContentObjectImpl) ((GenticsContentObjectImpl) object).clone();

				// prepare the object to be cached
				cachedObject.prepareForCaching();
				PortalCache genticsContentObjectCache = PortalCache.getCache(OBJECTCACHEREGION);

				genticsContentObjectCache.putIntoGroup(ds.toString(), cacheKey, cachedObject);
			}
		}
	}

	/**
	 * Put the given attribute into the cache
	 * @param ds datasource
	 * @param object object of the attribute
	 * @param attribute attribute to cache
	 * @throws PortalCacheException
	 * @throws CloneNotSupportedException
	 */
	public static void cacheAttribute(Datasource ds, GenticsContentObject object,
			GenticsContentAttribute attribute) throws PortalCacheException,
				CloneNotSupportedException {
		// attributes of type FOREIGNOBJ may not be cached (because we cannot
		// clear the cache when a foreign linked object changes)
		if (cacheActivated && (ds instanceof CNDatasource) && ((CNDatasource) ds).isCacheEnabled() && attribute.isCacheable()) {
			// get the cache region
			String cacheRegion = ((CNDatasource) ds).getAttributeCacheRegion(attribute.getAttributeName());

			if (cacheRegion == null) {
				// this attribute shall not be cached
				return;
			}
			PortalCache attributeCache = PortalCache.getCache(cacheRegion);

			if (attributeCache == null) {
				return;
			}

			Object cacheKey = getCacheKey(ds, object.getContentId(), object.getVersionTimestamp());
			if (cacheKey != null) {
				Map<String, Object> cachedDataMap = getCachedDataMap(ds, cacheKey, attributeCache, true);
				cachedDataMap.put(attribute.getAttributeName(), ((GenticsContentAttributeImpl)attribute).getCachableValue());
			}
		}
	}

	/**
	 * Remove the given object from the cache
	 * @param ds datasource
	 * @param contentId content id
	 * @throws PortalCacheException
	 */
	public static void uncacheObject(Datasource ds, String contentId) throws PortalCacheException {
		uncacheObject(ds, contentId, -1);
	}

	/**
	 * Remove the given object from the cache
	 * @param ds datasource
	 * @param contentId content id
	 * @param versiontimestamp
	 * @throws PortalCacheException
	 */
	public static void uncacheObject(Datasource ds, String contentId, int versiontimestamp) throws PortalCacheException {
		if (cacheActivated && (ds instanceof CNDatasource) && ((CNDatasource) ds).isCacheEnabled()) {
			CNDatasource cnDS = (CNDatasource) ds;

			int[] ids;
			AttributeType[] attributeTypes;

			try {
				// split the contentid in obj_type and obj_id
				ids = GenticsContentObjectImpl.splitContentId(contentId);

				// get the attribute types for the obj_type
				attributeTypes = DatatypeHelper.getAttributeTypes(cnDS.getHandle().getDBHandle(), null, null, null, new int[] { ids[0]}, null, null);
			} catch (Exception e) {
				throw new PortalCacheException("Could not uncache object with contentid {" + contentId + "}", e);
			}

			Object cacheKey = getCacheKey(ds, contentId, versiontimestamp);

			if (cacheKey != null) {
				PortalCache genticsContentObjectCache = PortalCache.getCache(OBJECTCACHEREGION);

				genticsContentObjectCache.removeFromGroup(ds.toString(), cacheKey);
			}

			// uncache all attributes
			// collect the cache regions here
			List<String> cacheRegions = new ArrayList<String>();
			for (int i = 0; i < attributeTypes.length; i++) {
				String attributeTypeName = attributeTypes[i].getName();

				// get the cache region
				String cacheRegion = ((CNDatasource) ds).getAttributeCacheRegion(attributeTypeName);

				if (cacheRegion == null) {
					// this attribute shall not be cached
					continue;
				}
				if (!cacheRegions.contains(cacheRegion)) {
					cacheRegions.add(cacheRegion);
				}
			}

			// now clear all entries for the attributes
			for (String cacheRegion : cacheRegions) {
				PortalCache attributeCache = PortalCache.getCache(cacheRegion);

				if (attributeCache == null) {
					continue;
				}

				cacheKey = getCacheKey(ds, contentId, versiontimestamp);
				if (cacheKey != null) {
					attributeCache.removeFromGroup(ds.toString(), cacheKey);
				}
			}
		}
	}

	/**
	 * Remove the given object from the cache
	 * @param ds datasource
	 * @param object object
	 * @throws PortalCacheException
	 */
	public static void uncacheObject(Datasource ds, GenticsContentObject object) throws PortalCacheException {
		uncacheObject(ds, object.getContentId(), object.getVersionTimestamp());
	}

	/**
	 * Remove the given attribute from the cache
	 * @param ds datasource
	 * @param object object
	 * @param attribute attribute
	 * @throws PortalCacheException
	 */
	public static void uncacheAttribute(Datasource ds, GenticsContentObject object,
			GenticsContentAttribute attribute) throws PortalCacheException {
		uncacheAttribute(ds, object, attribute.getAttributeName());
	}

	/**
	 * Remove the given attribute from the cache
	 * @param ds datasource
	 * @param object object
	 * @param attribute attribute name
	 * @throws PortalCacheException
	 */
	public static void uncacheAttribute(Datasource ds,
			GenticsContentObject object, String attribute) throws PortalCacheException {
		if (cacheActivated && (ds instanceof CNDatasource) && ((CNDatasource) ds).isCacheEnabled()) {

			// get the cache region
			String cacheRegion = ((CNDatasource) ds).getAttributeCacheRegion(attribute);

			if (cacheRegion == null) {
				// this attribute shall not be cached
				return;
			}
			PortalCache attributeCache = PortalCache.getCache(cacheRegion);

			if (attributeCache == null) {
				return;
			}

			Object cacheKey = getCacheKey(ds, object.getContentId(), object.getVersionTimestamp());
			if (cacheKey != null) {
				Map<String, Object> cachedDataMap = getCachedDataMap(ds, cacheKey, attributeCache, true);
				if (cachedDataMap != null) {
					cachedDataMap.remove(attribute);
				}
			}
		}
	}

	/**
	 * Generate the cachekey for the given data
	 * @param ds datasource
	 * @param contentid contentid of the cached object
	 * @param timestamp version timestamp
	 * @return the cachekey or null if the object is not cacheable
	 */
	protected static Object getCacheKey(Datasource ds, String contentid, int timestamp) {
		if (!(ds instanceof CNDatasource) || !((CNDatasource) ds).isCacheEnabled() || ObjectTransformer.isEmpty(contentid)) {
			return null;
		}
		String dsstring = ds.toString();
		StringBuffer cacheKey = new StringBuffer(dsstring.length() + contentid.length() + 10);

		cacheKey.append(dsstring).append(contentid).append(timestamp);
		return cacheKey.toString();
	}

	/**
	 * Generate the cachekey for the given data
	 * @param ds datasource
	 * @param contentid contentid of the cached object
	 * @param attributeName name of the attribute
	 * @param timestamp version timestamp
	 * @return the cachekey or null if the object is not cacheable
	 */
	protected static Object getCacheKey(Datasource ds, String contentid, String attributeName, int timestamp) {
		if (!(ds instanceof CNDatasource) || !((CNDatasource) ds).isCacheEnabled() || ObjectTransformer.isEmpty(contentid)
				|| ObjectTransformer.isEmpty(attributeName)) {
			return null;
		}
		String dsstring = ds.toString();
		StringBuffer cacheKey = new StringBuffer(dsstring.length() + contentid.length() + attributeName.length() + 10);

		cacheKey.append(dsstring).append(contentid).append(attributeName).append(timestamp);
		return cacheKey.toString();
	}

	/**
	 * Clear all caches
	 * @throws PortalCacheException
	 */
	public static void clearCaches(Datasource ds) throws PortalCacheException {
		PortalCache genticsContentAttributeCache = PortalCache.getCache(ATTRIBUTESCACHEREGION);

		if (genticsContentAttributeCache != null) {
			genticsContentAttributeCache.clearGroup(ds.toString());
		}
		PortalCache genticsContentObjectCache = PortalCache.getCache(OBJECTCACHEREGION);

		if (genticsContentObjectCache != null) {
			genticsContentObjectCache.clearGroup(ds.toString());
		}
		// also clear eventually existing custom cache regions
		if (ds instanceof CNDatasource) {
			List customCaches = ((CNDatasource) ds).getCustomCacheRegions();

			for (Iterator iterator = customCaches.iterator(); iterator.hasNext();) {
				String regionName = (String) iterator.next();
				PortalCache cache = PortalCache.getCache(regionName);

				if (cache != null) {
					cache.clearGroup(ds.toString());
				}
			}
		}
	}

	/**
	 * Check whether the given attribute name is an attribute path (path entries
	 * are separated by "."). This implementation supports attribute names, that
	 * contain a ".", when the "." is escaped by "\"
	 * @param attributeName attribute name to test
	 * @return true when the attribute name is a path (consisting of at least
	 *         two entries), false for normal attribute names (which might eventually contain escaped ".")
	 */
	public static boolean isAttributePath(String attributeName) {
		return getFirstDotPosition(attributeName) >= 0;
	}

	/**
	 * Get the first unescaped dot position in the attribute name, or -1 if none found
	 * @param attributeName attribute name
	 * @return position of the first unescaped dot, or -1 if none found
	 */
	public static int getFirstDotPosition(String attributeName) {
		if (StringUtils.isEmpty(attributeName)) {
			return -1;
		}
		int pos = -1;
		boolean unescapedDotFound = false;

		while (true) {
			pos = attributeName.indexOf('.', pos + 1);
			if (pos < 0) {
				break;
			} else if (pos == 0) {
				unescapedDotFound = true;
				break;
			} else if (pos > 0 && attributeName.charAt(pos - 1) != '\\') {
				unescapedDotFound = true;
				break;
			}
		}
		return unescapedDotFound ? pos : -1;
	}

	/**
	 * Extract the first attribute from the given attribute path. The first
	 * attribute will be normalized (eventually escaped \. in the name will be
	 * replaced by .), the remaining path will NOT be normalized.
	 * @param attributePath attribute path
	 * @return String array containing of the first attribute and the remaining
	 *         path
	 */
	public static String[] splitAttributePath(String attributePath) {
		int dotPos = getFirstDotPosition(attributePath);

		if (dotPos < 0) {
			return new String[] { attributePath};
		} else {
			return new String[] { normalizeAttributeName(attributePath.substring(0, dotPos)), attributePath.substring(dotPos + 1)};
		}
	}

	/**
	 * Normalize the given attribute name (replace all \. by .)
	 * @param attributeName attribute name
	 * @return normalized attribute name
	 */
	public static String normalizeAttributeName(String attributeName) {
		return attributeName.replaceAll("\\\\\\.", ".");
	}

	/**
	 * Differentially clear caches of objects in the given datasource, which
	 * were modified after the given lastupdate timestamp
	 * @param cnDS datasource
	 * @param lastupdate timestamp of the last update
	 */
	public static void clearDifferentialCaches(CNDatasource cnDS, long lastupdate) {
		NodeLogger logger = NodeLogger.getNodeLogger(GenticsContentFactory.class);

		try {
			Collection modifiedObjects = cnDS.getObjectsModifiedSince(lastupdate);

			for (Iterator iterator = modifiedObjects.iterator(); iterator.hasNext();) {
				String contentId = (String) iterator.next();

				try {
					GenticsContentFactory.uncacheObject(cnDS, contentId);
				} catch (Exception e) {
					logger.warn("Error while uncaching object {" + contentId + "}", e);
				}
			}

			// clear the query caches
			cnDS.clearQueryCache();

			if (logger.isDebugEnabled()) {
				int objects = modifiedObjects.size();

				logger.debug("Cleared caches for " + objects + " objects");
			}

		} catch (Exception e) {
			logger.warn(
					"Error while checking for objects modified since " + lastupdate
					+ " (because differential sync checking is enabled). Falling back to clearing all caches.",
					e);
			cnDS.clearCaches();
		}
	}
    
	/**
	 * Refresh cached objects in the given datasource that were
	 * modified since given lastupdate timestamp
	 * @param cnDS datasource
	 * @param lastupdate timestamp of the last update, -1 if unavailable
	 * @throws DataSourceException 
	 */
	public static void refreshCaches(CNDatasource cnDS, long lastupdate) throws DatasourceException {
		NodeLogger logger = NodeLogger.getNodeLogger(GenticsContentFactory.class);

		try {
			if (logger.isInfoEnabled()) {
				logger.info("Refreshing caches for " + cnDS);
			}

			// retrieve changed objects
			Collection<String> objects = cnDS.getObjectsModifiedSince(lastupdate);

			// retrieve objects that match the filter
			DatasourceFilter filter = cnDS.getCacheWarmingFilter();
			Collection<Resolvable> result = cnDS.getResult(filter, null);

			// objects found in the result will not be uncached
			for (Iterator<Resolvable> i = result.iterator(); i.hasNext();) {
				Resolvable res = i.next();

				if (objects.contains(res.get("contentid"))) {
					objects.remove(res.get("contentid"));
				} else {
					i.remove();
				}
			}

			// clear cache for objects that have been modified but do not match the filter
			for (String unCache : objects) {
				GenticsContentFactory.uncacheObject(cnDS, unCache);
			}
        	
			// create an array of GenticsContentObjects out of the result Collection to pass to prefillContentObjects below
			List<GenticsContentObject> contentObjects = new Vector<GenticsContentObject>(result.size());

			DBHandle dbHandle = cnDS.getHandle().getDBHandle();
			Map<Integer, List<String>> attributeTypeMap = new HashMap<Integer, List<String>>();

			for (Resolvable resolvable : result) {
				if (resolvable instanceof GenticsContentObject) {
					GenticsContentObject gcnObject = (GenticsContentObject) resolvable;

					contentObjects.add(gcnObject);

					List<String> attributes = attributeTypeMap.get(gcnObject.getObjectType());

					if (attributes == null) {
						// remove the attributes from the cache, that shall not be warmed
						AttributeType[] attributeTypes = DatatypeHelper.getAttributeTypes(dbHandle, null, null, null, new int[] { gcnObject.getObjectType()},
								null, null);

						attributes = new Vector<String>();
						for (AttributeType attrType : attributeTypes) {
							attributes.add(attrType.getName());
						}

						attributes.removeAll(Arrays.asList(cnDS.getCacheWarmingAttributes()));
						attributeTypeMap.put(gcnObject.getObjectType(), attributes);
					}

					for (String attr : attributes) {
						uncacheAttribute(cnDS, gcnObject, attr);
					}
				}
			}
 		    
			GenticsContentObjectImpl.prefillContentObjects(cnDS, contentObjects.toArray(new GenticsContentObject[contentObjects.size()]),
					cnDS.getCacheWarmingAttributes(), -1, true, true);

			// clear the query caches
			cnDS.clearQueryCache();

			if (logger.isDebugEnabled()) {
				int objectCount = objects.size();

				logger.debug("Cleared caches for " + objectCount + " objects");
			}

		} catch (Exception e) {
			logger.warn(
					"Error while checking for objects modified since " + lastupdate
					+ " (because differential sync checking is enabled). Falling back to clearing all caches.",
					e);
			cnDS.clearCaches();
			throw new DatasourceException("Error while refreshing caches", e);
		} finally {
			if (logger.isInfoEnabled()) {
				logger.info("Finished refreshing caches for " + cnDS);
			}
		}
	}
    
	/**
	 * Generate and return the path to store an attribute within its datasource
	 * @param type attribute type
	 * @param id attribute id
	 * @param name attribute name
	 * @param sortorder attribute sortorder
	 * @param transactionid attribute transaction ID
	 * @return The full path to the file
	 * @throws DatasourceException
	 */
	public static String getStoragePath(String contentId, String name, int sortorder, int transactionid) {

		String md5 = StringUtils.md5(contentId).toLowerCase();

		// generate full path by taking the base path and adding two subfolders
		// based on the md5 hash of the id
		StringBuffer buffer = new StringBuffer();

		buffer.append(md5.substring(0, 2));
		buffer.append("/");
		buffer.append(md5.substring(3, 5));
		buffer.append("/");

		// generate the filename
		buffer.append(contentId);
		buffer.append(".");
		buffer.append(name);
		buffer.append(".");
		buffer.append(Integer.toString(sortorder));
		buffer.append(".");
		buffer.append(Integer.toString(transactionid));

		return buffer.toString();
	}

	/**
	 * Get the list of files that are value files for the object with given contentid
	 * @param basePath base path
	 * @param contentId content id
	 * @return list of files
	 */
	public static File[] getValueFiles(String basePath, String contentId) {
		String md5 = StringUtils.md5(contentId).toLowerCase();
		StringBuffer buffer = new StringBuffer();

		buffer.append(md5.substring(0, 2));
		buffer.append("/");
		buffer.append(md5.substring(3, 5));
		buffer.append("/");

		final String filenamePrefix = contentId + ".";
		File objectPath = new File(basePath, buffer.toString());

		if (objectPath.exists() && objectPath.isDirectory()) {
			return objectPath.listFiles(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return name.startsWith(filenamePrefix);
				}
			});
		} else {
			return new File[0];
		}
	}

	/**
	 * Get the transaction ID from the given storage path or 0 if the storage
	 * path is empty or does not conform to the expected pattern
	 * @param path storage path
	 * @return transaction ID
	 */
	public static int getTransactionId(String path) {
		StoragePathInfo info = new StoragePathInfo(path);

		return info.transactionId;
	}

	/**
	 * Get a valid DBHandle for the given datasource
	 * @param ds datasource
	 * @return valid DBHandle
	 * @throws CMSUnavailableException if no valid DBHandle is available
	 */
	public static DBHandle getHandle(Datasource ds) throws CMSUnavailableException {
		DatasourceHandle handle = ds.getHandlePool().getHandle();
		if (handle instanceof SQLHandle) {
			return ((SQLHandle) handle).getDBHandle();
		} else {
			throw new CMSUnavailableException("Could not find a valid handle for " + ds);
		}
	}

	/**
	 * Encapsulation class for info parsed from a storage path
	 */
	public static class StoragePathInfo {
		protected static Pattern STORAGE_PATH_PATTERN = Pattern.compile("([0-9]+\\.[0-9]+)\\.([^\\.]+)\\.([0-9]+)\\.([0-9]+)");

		/**
		 * ContentId
		 */
		public String contentId;

		/**
		 * Attribute Name
		 */
		public String name;

		/**
		 * sortorder
		 */
		public int sortorder;

		/**
		 * transaction ID
		 */
		public int transactionId;

		/**
		 * Create an instance based on the storage path
		 * @param storagePath storage path
		 * @throws IllegalArgumentException
		 */
		public StoragePathInfo(String storagePath) throws IllegalArgumentException {
			File file = new File(storagePath);
			String fileName = file.getName();
			Matcher matcher = STORAGE_PATH_PATTERN.matcher(fileName);

			if (!matcher.matches()) {
				throw new IllegalArgumentException("[" + fileName + "] does not match the storage path pattern " + STORAGE_PATH_PATTERN.pattern());
			}

			contentId = matcher.group(1);
			name = matcher.group(2);
			sortorder = ObjectTransformer.getInt(matcher.group(3), 0);
			transactionId = ObjectTransformer.getInt(matcher.group(4), 0);
		}
	}
}
