package com.gentics.lib.datasource.mccr;

import java.io.Serializable;
import java.sql.SQLException;
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
import com.gentics.api.lib.datasource.ChannelTree;
import com.gentics.api.lib.datasource.ChannelTreeNode;
import com.gentics.api.lib.datasource.DatasourceChannel;
import com.gentics.api.lib.datasource.DatasourceException;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.lib.datasource.object.ObjectAttributeBean;
import com.gentics.lib.datasource.object.ObjectTypeBean;
import com.gentics.lib.db.DB;
import com.gentics.lib.db.DBHandle;
import com.gentics.lib.db.SimpleResultProcessor;
import com.gentics.lib.db.SimpleResultRow;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.log.NodeLogger;

/**
 * Static helper class for caching in a multichannelling aware content repository
 */
public class MCCRCacheHelper {

	/**
	 * Name of the cache region for objects (keys are the internal object IDs) (group names with channel)
	 */
	public final static String OBJECTCACHEREGION = "gentics-mccr-objects";

	/**
	 * Name of the cache region for resolving contentid to internal IDs (group names with channel)
	 */
	public final static String OBJECTCONTENTIDCACHEREGION = "gentics-mccr-objects-contentid";

	/**
	 * Name of the cache region for resolving channelset to internal IDs (group names with channel)
	 */
	public final static String OBJECTCHANNELSETCACHEREGION = "gentics-mccr-objects-channelset";

	/**
	 * Name of the cache region for attributes (group names with channel)
	 */
	public final static String ATTRIBUTESCACHEREGION = "gentics-mccr-atts";

	/**
	 * Name of the cache region for general datasource data (group names without channel)
	 */
	public final static String DSCACHEREGION = "gentics-mccr-data";

	/**
	 * Name of the cacheregion for queryresults
	 */
	public final static String RESULTSCACHEREGION = "gentics-mccr-results";

	/**
	 * Name of the cache region for filesystem attribute files
	 */
	public final static String FSATTRIBUTESREGION = "gentics-mccr-fsattrfiles";

	/**
	 * Cache key for the types entry
	 */
	public final static String TYPESCACHEKEY = "gentics-mccr-types";

	/**
	 * Cache key for the channels
	 */
	public final static String CHANNELSCACHEKEY = "gentics-mccr-channels";

	/**
	 * flag to mark whether the cache is activated or not
	 */
	protected static boolean cacheActivated = false;

	/**
	 * pattern for interpretation of custom attribute cache settings
	 */
	protected static Pattern customAttributeCacheSettingsPattern = Pattern.compile("cache\\.attribute\\.([^.]+)(\\.region)?");

	/**
	 * Logger instance
	 */
	protected static NodeLogger logger = NodeLogger.getNodeLogger(MCCRHelper.class);

	static {
		try {
			PortalCache typesCache = PortalCache.getCache(DSCACHEREGION);

			if (typesCache != null) {
				cacheActivated = true;
			}
		} catch (PortalCacheException e) {
			logger.error("Error while initializing cache for multichannelling datasources.", e);
		}
	}

	/**
	 * Clear all caches for the given datasource
	 * @param ds datasource
	 */
	public static void clear(MCCRDatasource ds) {
		clear(ds, true);
	}

	/**
	 * Clear all caches for the given datasource
	 * @param ds datasource
	 * @param typeCaches true to also clear the type caches, false if not
	 */
	public static void clear(MCCRDatasource ds, boolean typeCaches) {
		if (cacheActivated) {
			try {
				String groupName = getGroupName(ds);

				PortalCache.getCache(ATTRIBUTESCACHEREGION).clearGroup(groupName);
				PortalCache.getCache(OBJECTCACHEREGION).clearGroup(groupName);
				PortalCache.getCache(OBJECTCHANNELSETCACHEREGION).clearGroup(groupName);
				PortalCache.getCache(OBJECTCONTENTIDCACHEREGION).clearGroup(groupName);

				// clear custom cache regions
				List<String> cacheRegions = ds.getCustomCacheRegions();

				for (String region : cacheRegions) {
					PortalCache.getCache(region).clearGroup(groupName);
				}

				clearResults(ds);

				if (typeCaches) {
					PortalCache.getCache(DSCACHEREGION).clearGroup(groupName);
				}
			} catch (Exception e) {
				logger.error("Error while clearing datasource caches for " + ds, e);
			}
		}
	}

	/**
	 * Get the cached object types of the given datasource or null if not cached
	 * @param ds datasource
	 * @param mutable true to get changeable objects, false if not
	 * @return cached object types or null
	 */
	@SuppressWarnings("unchecked")
	public static Map<Integer, ObjectTypeBean> getTypes(MCCRDatasource ds, boolean mutable) {
		if (cacheActivated) {
			try {
				String groupName = getGroupName(ds);
				PortalCache typesCache = PortalCache.getCache(DSCACHEREGION);
				Object cachedTypes = typesCache.getFromGroup(groupName, TYPESCACHEKEY);

				if (cachedTypes != null) {
					if (mutable) {
						return copy((Map<Integer, ObjectTypeBean>) cachedTypes, true);
					} else {
						return (Map<Integer, ObjectTypeBean>)cachedTypes;
					}
				}
			} catch (Exception e) {
				logger.error("Error while getting cached types for " + ds, e);
			}
		}

		return null;
	}

	/**
	 * Put the given object types into the cache
	 * @param ds datasource
	 * @param objectTypes map of object types to cache
	 */
	public static void put(MCCRDatasource ds, Map<Integer, ObjectTypeBean> objectTypes) {
		if (cacheActivated) {
			try {
				String groupName = getGroupName(ds);
				PortalCache typesCache = PortalCache.getCache(DSCACHEREGION);

				typesCache.putIntoGroup(groupName, TYPESCACHEKEY, copy(objectTypes, false));
			} catch (Exception e) {
				logger.error("Error while putting types into cache for " + ds, e);
			}
		}
	}

	/**
	 * Get the cached channel tree of the given datasource or null if not cached
	 * @param ds datasource
	 * @return cached channel tree or null
	 */
	public static ChannelTree getChannelTree(MCCRDatasource ds) {
		if (cacheActivated) {
			try {
				String groupName = getGroupName(ds);
				PortalCache channelsCache = PortalCache.getCache(DSCACHEREGION);
				Object cachedChannels = channelsCache.getFromGroup(groupName, CHANNELSCACHEKEY);

				if (cachedChannels != null) {
					return copy((ChannelTree) cachedChannels);
				}
			} catch (Exception e) {
				logger.error("Error while getting cached channels for " + ds, e);
			}
		}

		return null;
	}

	/**
	 * Put the given channel tree into the cache
	 * @param ds datasource
	 * @param channelTree channel tree to cache
	 */
	public static void put(MCCRDatasource ds, ChannelTree channelTree) {
		if (cacheActivated) {
			try {
				String groupName = getGroupName(ds);
				PortalCache channelsCache = PortalCache.getCache(DSCACHEREGION);

				channelsCache.putIntoGroup(groupName, CHANNELSCACHEKEY, copy(channelTree));
			} catch (Exception e) {
				logger.error("Error while putting channel tree into cache for " + ds, e);
			}
		}
	}

	/**
	 * Put an object into the cache
	 * @param object object to cache
	 */
	public static void put(MCCRObject object) {
		put(object, null);
	}

	/**
	 * Put an object into the cache
	 * @param object object to cache
	 * @param alternativeContentId alternative contentid
	 */
	public static void put(MCCRObject object, String alternativeContentId) {
		if (object == null) {
			return;
		}
		try {
			if (cacheActivated && object.exists() && object.ds.isCacheEnabled()) {
				String groupName = getGroupName(object.ds);

				// cache the attribute values
				if (!ObjectTransformer.isEmpty(object.attributes)) {
					for (Map.Entry<String, Object> entry : object.attributes.entrySet()) {
						put(object, entry.getKey(), entry.getValue());
					}
				}

				// copy the object to be put into the cache
				object = copy(null, object, CopyFor.PUT);
				String contentId = getContentIdCacheKey(object);

				getCachedIntMap(groupName, contentId, PortalCache.getCache(OBJECTCONTENTIDCACHEREGION), true).put(object.channelId, object.id);
				getCachedIntMap(groupName, object.channelsetId, PortalCache.getCache(OBJECTCHANNELSETCACHEREGION), true).put(object.channelId, object.id);
				getCachedObjectMap(groupName, object.id, PortalCache.getCache(OBJECTCACHEREGION), true).put(object.channelId, object);
				if (!StringUtils.isEmpty(alternativeContentId) && !StringUtils.isEqual(alternativeContentId, contentId)) {
					getCachedIntMap(groupName, alternativeContentId, PortalCache.getCache(OBJECTCONTENTIDCACHEREGION), true).put(object.channelId, object.id);
				}
			}
		} catch (Exception e) {
			logger.error("Error while putting object " + object + " into cache", e);
		}
	}

	/**
	 * Get the cached object with given internal id
	 * @param ds datasource
	 * @param id internal id
	 * @return cached object or null
	 */
	public static MCCRObject getById(MCCRDatasource ds, int id) {
		if (cacheActivated && ds.isCacheEnabled()) {
			try {
				List<DatasourceChannel> channels = ds.getChannels();

				for (DatasourceChannel channel : channels) {
					MCCRObject mccrObject = getById(ds, id, channel.getId());

					if (mccrObject != null) {
						return mccrObject;
					}
				}
			} catch (DatasourceException e) {
				logger.error("Error while getting object with id " + id + " for " + ds, e);
			}
		}
		return null;
	}

	/**
	 * Get the cached object with given internal id for the given channel
	 * @param ds datasource
	 * @param id internal id
	 * @param channelId channel id
	 * @return cached object or null
	 */
	public static MCCRObject getById(MCCRDatasource ds, int id, int channelId) {
		try {
			if (cacheActivated && ds.isCacheEnabled()) {
				String groupName = getGroupName(ds);
				SimpleObjectMap cachedDataMap = getCachedObjectMap(groupName, id, PortalCache.getCache(OBJECTCACHEREGION), false);
				if (ObjectTransformer.isEmpty(cachedDataMap)) {
					return null;
				}
				MCCRObject cached = cachedDataMap.get(channelId);

				if (cached != null) {
					return copy(ds, cached, CopyFor.GET);
				} else {
					return null;
				}
			}
		} catch (Exception e) {
			logger.error("Error while getting object with id " + id + " for " + ds, e);
		}
		return null;
	}

	/**
	 * Get the cached object with given channelset id
	 * @param ds datasource
	 * @param channelsetId channelset id
	 * @return cached object or null
	 */
	public static MCCRObject getByChannelsetId(MCCRDatasource ds, int channelsetId) {
		if (cacheActivated && ds.isCacheEnabled()) {
			try {
				List<DatasourceChannel> channels = ds.getChannels();

				for (DatasourceChannel channel : channels) {
					MCCRObject mccrObject = getByChannelsetId(ds, channelsetId, channel.getId());

					if (mccrObject != null) {
						return mccrObject;
					}
				}
			} catch (DatasourceException e) {
				logger.error("Error while getting object with channelsetId " + channelsetId + " for " + ds, e);
			}
		}
		return null;
	}

	/**
	 * Get the cached object with given channelset id for the given channel
	 * @param ds datasource
	 * @param channelsetId channelset id
	 * @param channelId channel id
	 * @return cached object or null
	 */
	public static MCCRObject getByChannelsetId(MCCRDatasource ds, int channelsetId, int channelId) {
		try {
			if (cacheActivated && ds.isCacheEnabled()) {
				String groupName = getGroupName(ds);
				SimpleIntMap cachedDataMap = getCachedIntMap(groupName, channelsetId, PortalCache.getCache(OBJECTCHANNELSETCACHEREGION), false);
				if (ObjectTransformer.isEmpty(cachedDataMap)) {
					return null;
				}
				int id = ObjectTransformer.getInt(cachedDataMap.get(channelId), 0);

				if (id > 0) {
					MCCRObject cached = getById(ds, id, channelId);

					if (cached != null) {
						return cached;
					} else {
						// uncache the channelsetId entry, because it obviously was old
						cachedDataMap.clear(channelId);
						return null;
					}
				} else {
					return null;
				}
			}
		} catch (Exception e) {
			logger.error("Error while getting object with channelsetId " + channelsetId + " for " + ds, e);
		}
		return null;
	}

	/**
	 * Get the cached object with given content id
	 * @param ds datasource
	 * @param contentId content id
	 * @return cached object or null
	 */
	public static MCCRObject getByContentId(MCCRDatasource ds, String contentId) {
		if (cacheActivated && ds.isCacheEnabled()) {
			try {
				List<DatasourceChannel> channels = ds.getChannels();

				for (DatasourceChannel channel : channels) {
					MCCRObject mccrObject = getByContentId(ds, contentId, channel.getId());

					if (mccrObject != null) {
						return mccrObject;
					}
				}
			} catch (DatasourceException e) {
				logger.error("Error while getting object with contentId " + contentId + " for " + ds, e);
			}
		}
		return null;
	}

	/**
	 * Get the cached object with given content id for the given channel
	 * @param ds datasource
	 * @param contentId content id
	 * @param channelId channel id
	 * @return cached object or null
	 */
	public static MCCRObject getByContentId(MCCRDatasource ds, String contentId, int channelId) {
		try {
			if (cacheActivated && ds.isCacheEnabled()) {
				String groupName = getGroupName(ds);
				SimpleIntMap cachedDataMap = getCachedIntMap(groupName, contentId, PortalCache.getCache(OBJECTCONTENTIDCACHEREGION), false);
				if (ObjectTransformer.isEmpty(cachedDataMap)) {
					return null;
				}
				int id = ObjectTransformer.getInt(cachedDataMap.get(channelId), 0);

				if (id > 0) {
					MCCRObject cached = getById(ds, id, channelId);

					if (cached != null) {
						return cached;
					} else {
						// uncache the contentid entry, because it obviously was old
						cachedDataMap.clear(channelId);
						return null;
					}
				} else {
					return null;
				}
			}
		} catch (Exception e) {
			logger.error("Error while getting object with contentId " + contentId + " for " + ds, e);
		}
		return null;
	}

	/**
	 * Clear the caches for the given object (incl. attributes)
	 * @param obj object
	 * @param allChannels true to clear the caches for all channels, false for only the given channel
	 */
	public static void clear(MCCRObject obj, boolean allChannels) {
		try {
			if (cacheActivated && obj.ds.isCacheEnabled()) {
				String groupName = getGroupName(obj.ds);

				// get all attribute cache regions
				List<String> regions = new ArrayList<String>();
				Collection<ObjectAttributeBean> types = MCCRHelper.getAttributeTypes(obj.ds, obj.contentId.objType);
				for (ObjectAttributeBean attr : types) {
					String region = obj.ds.getAttributeCacheRegion(attr.getName());
					if (region != null && !regions.contains(region)) {
						regions.add(region);
					}
				}

				if (allChannels) {
					PortalCache.getCache(OBJECTCACHEREGION).removeFromGroup(groupName, obj.id);
					PortalCache.getCache(OBJECTCHANNELSETCACHEREGION).removeFromGroup(groupName, obj.channelsetId);
					PortalCache.getCache(OBJECTCONTENTIDCACHEREGION).removeFromGroup(groupName, getContentIdCacheKey(obj));
					
					// clear all attribute regions
					Object attrCacheKey = getAttributeCacheKey(obj, null);
					for (String region : regions) {
						PortalCache.getCache(region).removeFromGroup(groupName, attrCacheKey);
					}
				} else {
					clearForChannel(OBJECTCACHEREGION, groupName, obj.id, obj.channelId);
					clearForChannel(OBJECTCHANNELSETCACHEREGION, groupName, obj.channelsetId, obj.channelId);
					clearForChannel(OBJECTCONTENTIDCACHEREGION, groupName, getContentIdCacheKey(obj), obj.channelId);

					// clear all attribute regions
					Object attrCacheKey = getAttributeCacheKey(obj, null);
					for (String region : regions) {
						AttributesPerChannel attributesPerChannel = getCachedAttributes(obj.ds, groupName, attrCacheKey, PortalCache.getCache(region), false);
						if (attributesPerChannel != null) {
							attributesPerChannel.clear(obj.channelId);
						}
					}
				}
			}
		} catch (Exception e) {
			logger.error("Error while clearing cache for " + obj);
		}
	}

	/**
	 * Get the cached attribute value for the given attribute of the given object
	 * @param object object
	 * @param attributeName name of the attribute
	 * @return cached attribute values or null if not cached. If a null value is cached, this method returns the {@link CacheDummy} object.
	 */
	public static Object get(MCCRObject object, String attributeName) {
		try {
			if (cacheActivated && object.ds.isCacheEnabled()) {
				String region = object.ds.getAttributeCacheRegion(attributeName);

				if (region != null) {
					String groupName = getGroupName(object.ds);
					Object cacheKey = getAttributeCacheKey(object, attributeName);

					AttributesPerChannel attributesPerChannel = getCachedAttributes(object.ds, groupName, cacheKey, PortalCache.getCache(region), false);
					if (attributesPerChannel == null) {
						return null;
					} else {
						return attributesPerChannel.get(object.channelId, attributeName);
					}
				}
			}
		} catch (Exception e) {
			logger.error("Error while getting attribute " + attributeName + " for " + object, e);
		}
		return null;
	}

	/**
	 * Put the attribute value for the given object into the cache
	 * @param object object
	 * @param attributeName attribute name
	 * @param value attribute value
	 */
	public static void put(MCCRObject object, String attributeName, Object value) {
		try {
			if (cacheActivated && object.ds.isCacheEnabled()) {
				String region = object.ds.getAttributeCacheRegion(attributeName);

				if (region != null) {
					String groupName = getGroupName(object.ds);
					Object cacheKey = getAttributeCacheKey(object, attributeName);

					// transform the value to be stored
					if (ObjectTransformer.isEmpty(value)) {
						value = CacheDummy.get();
					} else if (value instanceof Collection) {
						@SuppressWarnings("unchecked")
						Collection<Object> values = (Collection<Object>)value;

						if (values.size() == 1) {
							// store single values as values, not arrays
							value = values.iterator().next();
						} else {
							// collections with more than 1 element are transformed to arrays
							value = (Object[]) values.toArray(new Object[values.size()]);
						}
					}

					PortalCache cache = PortalCache.getCache(region);
					AttributesPerChannel attributesPerChannel = getCachedAttributes(object.ds, groupName, cacheKey, cache, true);
					attributesPerChannel.put(object.channelId, attributeName, value);
				}
			}
		} catch (Exception e) {
			logger.error("Error while putting value for attribute " + attributeName + " of " + object + " into cache", e);
		}
	}

	/**
	 * Clear the caches for the given object attribute
	 * @param object object
	 * @param attributeName attribute name
	 */
	public static void clear(MCCRObject object, String attributeName) {
		try {
			if (cacheActivated && object.ds.isCacheEnabled()) {
				String region = object.ds.getAttributeCacheRegion(attributeName);

				if (region != null) {
					String groupName = getGroupName(object.ds);
					Object cacheKey = getAttributeCacheKey(object, attributeName);

					AttributesPerChannel attributesPerChannel = getCachedAttributes(object.ds, groupName, cacheKey, PortalCache.getCache(region), false);
					if (attributesPerChannel != null) {
						attributesPerChannel.clear(object.channelId, attributeName);
					}
				}
			}
		} catch (Exception e) {
			logger.error("Error while clearing cache for attribute " + attributeName + " of " + object, e);
		}
	}

	/**
	 * Get the cache key to cache results
	 * @param ds datasource (must have the correct channel set)
	 * @param sqlStatement sql statement
	 * @param params parameters
	 * @param start start index
	 * @param count maximum number of objects returned
	 * @return cache key
	 * @throws DatasourceException
	 */
	public static String getResultsCacheKey(MCCRDatasource ds, String sqlStatement, Object[] params, int start, int count) throws DatasourceException {
		if (sqlStatement == null || params == null || !ds.isCacheEnabled()) {
			return null;
		}
		
		// prepare the stringbuffer. the initial size is a rough estimation of
		// the needed size
		StringBuffer cacheKey = new StringBuffer(ds.toString().length() + sqlStatement.length() + params.length * 5 + 6 + 4);
		
		// ds identification
		cacheKey.append(ds.toString()).append("|");
		// channel ids
		List<DatasourceChannel> channels = ds.getChannels();

		for (DatasourceChannel channel : channels) {
			cacheKey.append(channel.getId()).append("|");
		}
		// sql statement
		cacheKey.append(sqlStatement).append("|");
		// parameters
		for (int i = 0; i < params.length; ++i) {
			cacheKey.append(params[i]).append("|");
		}
		// paging
		cacheKey.append(start).append("|").append(count);
		
		return cacheKey.toString();
	}

	/**
	 * Get the cached result with the given cache key
	 * @param ds datasource
	 * @param resultsCacheKey cache key
	 * @return cached result or null
	 */
	public static SimpleResultProcessor getResult(MCCRDatasource ds, String resultsCacheKey) {
		if (cacheActivated && ds.isCacheEnabled()) {
			try {
				String groupName = ds.toString();
				PortalCache resultsCache = PortalCache.getCache(RESULTSCACHEREGION);
				Object cachedResult = resultsCache.getFromGroup(groupName, resultsCacheKey);

				if (cachedResult instanceof SimpleResultProcessor) {
					return (SimpleResultProcessor) cachedResult;
				}
			} catch (Exception e) {
				logger.error("Error while getting cached results for " + ds, e);
			}
		}

		return null;
	}

	/**
	 * Put the result into the cache
	 * @param ds datasource
	 * @param resultsCacheKey cache key
	 * @param result result to be cached
	 */
	public static void put(MCCRDatasource ds, String resultsCacheKey, SimpleResultProcessor result) {
		if (cacheActivated && ds.isCacheEnabled()) {
			try {
				String groupName = ds.toString();
				PortalCache resultsCache = PortalCache.getCache(RESULTSCACHEREGION);

				resultsCache.putIntoGroup(groupName, resultsCacheKey, result);
			} catch (Exception e) {
				logger.error("Error while putting results into cache for " + ds, e);
			}
		}
	}

	/**
	 * Get the cached count with the given cache key
	 * @param ds datasource
	 * @param resultsCacheKey cache key
	 * @return cached count or null
	 */
	public static Integer getCount(MCCRDatasource ds, String resultsCacheKey) {
		if (cacheActivated && ds.isCacheEnabled()) {
			try {
				String groupName = ds.toString();
				PortalCache resultsCache = PortalCache.getCache(RESULTSCACHEREGION);
				Object cachedResult = resultsCache.getFromGroup(groupName, resultsCacheKey);

				if (cachedResult instanceof Integer) {
					return (Integer) cachedResult;
				}
			} catch (Exception e) {
				logger.error("Error while getting cached count for " + ds, e);
			}
		}

		return null;
	}

	/**
	 * Put the count into the cache
	 * @param ds datasource
	 * @param resultsCacheKey results cache key
	 * @param count count to be cached
	 */
	public static void put(MCCRDatasource ds, String resultsCacheKey, int count) {
		if (cacheActivated && ds.isCacheEnabled()) {
			try {
				String groupName = ds.toString();
				PortalCache resultsCache = PortalCache.getCache(RESULTSCACHEREGION);

				resultsCache.putIntoGroup(groupName, resultsCacheKey, count);
			} catch (Exception e) {
				logger.error("Error while putting count into cache for " + ds, e);
			}
		}
	}

	/**
	 * Clear the results and count caches for the datasource
	 * @param ds datasource
	 */
	public static void clearResults(MCCRDatasource ds) {
		if (cacheActivated && ds.isCacheEnabled()) {
			try {
				String groupName = ds.toString();
				PortalCache resultsCache = PortalCache.getCache(RESULTSCACHEREGION);

				resultsCache.clearGroup(groupName);
			} catch (Exception e) {
				logger.error("Error while clearing results cache for " + ds, e);
			}
		}
	}

	/**
	 * Refresh cached objects in the given datasource that were
	 * modified since given lastupdate timestamp (in all channels)
	 * @param ds datasource
	 * @param timestamp timestamp
	 */
	public static void refreshCaches(MCCRDatasource ds, long timestamp) {
		try {
			if (logger.isInfoEnabled()) {
				logger.info("Refreshing caches for " + ds);
			}
			refreshCaches(ds, timestamp, ds.getChannelStructure().getChildren());

			// clear the query caches
			clearResults(ds);
		} catch (Exception e) {
			logger.warn(
					"Error while checking for objects modified since " + timestamp
					+ " (because differential sync checking is enabled). Falling back to clearing all caches.",
					e);
			ds.clearCaches();
		} finally {
			if (logger.isInfoEnabled()) {
				logger.info("Finished refreshing caches for " + ds);
			}
		}
	}

	/**
	 * Interpret the given parameters and create the custom attribute cache settings map
	 * @param parameters given datasource parameters
	 * @return map holding the custom attribute cache settings or null if none found
	 */
	public static Map<String, AttributeCache> getCustomCacheSettings(Map<String, String> parameters) {
		Map<String, AttributeCache> customCacheSettings = null;

		// now check all parameters for custom attribute cache settings
		for (Map.Entry<String, String> entry : parameters.entrySet()) {
			String key = entry.getKey();

			if (!StringUtils.isEmpty(key) && key.startsWith("cache.attribute.")) {
				// found a special attribute setting
				Matcher matcher = customAttributeCacheSettingsPattern.matcher(key);

				if (matcher.matches()) {
					String attributeName = matcher.group(1);
					boolean regionSetting = !StringUtils.isEmpty(matcher.group(2));

					if (customCacheSettings == null) {
						// only create the map when needed
						customCacheSettings = new HashMap<String, AttributeCache>();
					}

					AttributeCache currentSetting = customCacheSettings.get(attributeName);

					if (currentSetting != null && !currentSetting.enabled) {// ignore any further settings, caching is deactivated
						// for this attribute
					} else {
						if (regionSetting) {
							customCacheSettings.put(attributeName, new AttributeCache(entry.getValue()));
						} else {
							boolean active = ObjectTransformer.getBoolean(entry.getValue(), true);

							if (!active) {
								customCacheSettings.put(attributeName, new AttributeCache(false));
							}
						}
					}
				}
			}
		}

		return customCacheSettings;
	}

	/**
	 * Get cached filesystem attribute file in basePath starting with prefix.
	 * There is no guarantee, that the file still exists
	 * @param basePath base path
	 * @param prefix prefix
	 * @return name of the file
	 */
	public static String getFSAttributeFile(String basePath, String prefix) {
		try {
			if (cacheActivated) {
				return ObjectTransformer.getString(PortalCache.getCache(FSATTRIBUTESREGION).getFromGroup(basePath, prefix), null);
			}
		} catch (Exception e) {
			logger.error("Error while getting fs attribute file in " + basePath + " starting with " + prefix, e);
		}
		return null;
	}

	/**
	 * Put filesystem attribute file in basePath starting with prefix into the cache
	 * @param basePath base path
	 * @param prefix prefix
	 * @param fileName name of the file
	 */
	public static void putFSAttributeFile(String basePath, String prefix, String fileName) {
		try {
			if (cacheActivated) {
				if (fileName == null) {
					PortalCache.getCache(FSATTRIBUTESREGION).removeFromGroup(basePath, prefix);
				} else {
					PortalCache.getCache(FSATTRIBUTESREGION).putIntoGroup(basePath, prefix, fileName);
				}
			}
		} catch (Exception e) {
			logger.error("Error while putting fs attribute file in " + basePath + " starting with " + prefix + " into cache", e);
		}
	}

	/**
	 * Get the cached container for storing channel specific attributes for an object
	 * @param ds datasource
	 * @param groupName cache group name
	 * @param cacheKey cache key
	 * @param attributeCache cache instance
	 * @param createIfNull true to create (and put into the cache) if not found
	 * @return cached container
	 * @throws PortalCacheException
	 */
	protected static AttributesPerChannel getCachedAttributes(MCCRDatasource ds, String groupName, Object cacheKey, PortalCache attributeCache, boolean createIfNull)
			throws PortalCacheException {
		AttributesPerChannel entry = null;
		Object cachedData = attributeCache.getFromGroup(groupName, cacheKey);
		if (cachedData instanceof AttributesPerChannel) {
			entry = (AttributesPerChannel)cachedData;
		} else if (createIfNull) {
			entry = new AttributesPerChannel();
			attributeCache.putIntoGroup(groupName, cacheKey, entry);
		}
		return entry;
	}

	/**
	 * Get cached {@link SimpleIntMap} instance
	 * @param groupName group name
	 * @param cacheKey cache key
	 * @param cache cache
	 * @param createIfNull true to create an instance if not found
	 * @return cached instance
	 * @throws PortalCacheException
	 */
	protected static SimpleIntMap getCachedIntMap(String groupName, Object cacheKey, PortalCache cache, boolean createIfNull)
			throws PortalCacheException {
		SimpleIntMap cached = null;
		Object cachedObject = cache.getFromGroup(groupName, cacheKey);
		if (cachedObject instanceof SimpleIntMap) {
			cached = (SimpleIntMap)cachedObject;
		} else if (createIfNull) {
			cached = new SimpleIntMap();
			cache.putIntoGroup(groupName, cacheKey, cached);
		}
		return cached;
	}

	/**
	 * Get cached {@link SimpleObjectMap} instance
	 * @param groupName group name
	 * @param cacheKey cache key
	 * @param cache cache
	 * @param createIfNull true to create an instance if not found
	 * @return cached instance
	 * @throws PortalCacheException
	 */
	protected static SimpleObjectMap getCachedObjectMap(String groupName, Object cacheKey, PortalCache cache, boolean createIfNull)
			throws PortalCacheException {
		SimpleObjectMap cached = null;
		Object cachedObject = cache.getFromGroup(groupName, cacheKey);
		if (cachedObject instanceof SimpleObjectMap) {
			cached = (SimpleObjectMap)cachedObject;
		} else if (createIfNull) {
			cached = new SimpleObjectMap();
			cache.putIntoGroup(groupName, cacheKey, cached);
		}
		return cached;
	}

	/**
	 * Clear the cached data for the given channel
	 * @param region cache region
	 * @param groupName group name
	 * @param cacheKey cache key
	 * @param channelId channel id
	 * @throws PortalCacheException
	 */
	protected static void clearForChannel(String region, String groupName, Object cacheKey, int channelId) throws PortalCacheException {
		PortalCache cache = PortalCache.getCache(region);
		Object cached = cache.getFromGroup(groupName, cacheKey);
		if (cached instanceof SimpleIntMap) {
			((SimpleIntMap) cached).clear(channelId);
		} else if (cached instanceof SimpleObjectMap) {
			((SimpleObjectMap) cached).clear(channelId);
		} else if (cached instanceof AttributesPerChannel) {
			((AttributesPerChannel) cached).clear(channelId);
		}
	}

	/**
	 * Recursive method to refresh the caches in the list of channels (and their subchannels)
	 * @param ds datasource
	 * @param timestamp timestamp
	 * @param channels list of channels
	 */
	protected static void refreshCaches(MCCRDatasource ds, long timestamp, List<ChannelTreeNode> channels) throws Exception {
		for (ChannelTreeNode node : channels) {
			// set the channel
			ds.setChannel(node.getChannel().getId());
			// refresh the caches for the channel
			doRefreshCaches(ds, ObjectTransformer.getInt(node.getChannel().getId(), 0), timestamp);

			// do the recursion
			refreshCaches(ds, timestamp, node.getChildren());
		}
	}

	/**
	 * Refresh cached objects in the given datasource that were
	 * modified since given lastupdate timestamp for a specified channel
	 * @param ds datasource
	 * @param channelId channel id
	 * @param timestamp update timestamp
	 */
	protected static void doRefreshCaches(MCCRDatasource ds, int channelId, long timestamp) throws Exception {
		boolean cacheWarming = ds.isCacheWarmingActive();

		// retrieve changed objects
		List<MCCRObject> objects = getObjectsModifiedSince(ds, channelId, timestamp);

		// retrieve objects that match the filter, if cache warming is active
		Collection<Resolvable> result = Collections.emptyList();

		if (cacheWarming) {
			result = ds.getResult(ds.getCacheWarmingFilter(channelId, timestamp), null);
		}

		// objects found in the result will not be uncached
		// after this loop, "result" will contain the objects matching the cachewarming filter, that are modified (need to be refreshed)
		// and "objects" will contain the id's of objects that are not to be warmed but are modified (need to be removed from cache)
		for (Iterator<Resolvable> i = result.iterator(); i.hasNext();) {
			Resolvable res = i.next();

			if (res instanceof MCCRObject) {
				int index = Collections.binarySearch(objects, (MCCRObject) res, MCCRHelper.COMPARATOR);

				if (index >= 0) {
					objects.remove(index);
				} else {
					i.remove();
				}
			}
		}

		// clear cache for objects that have been modified but do not match the filter
		for (MCCRObject unCache : objects) {
			clear(unCache, false);
		}

		// do some cache warming
		if (!result.isEmpty()) {
			// prepare attributes to warm
			List<String> warmingAttributes = Arrays.asList(ds.getCacheWarmingAttributes());

			// create the list of objects for which attributes shall be warmed
			List<MCCRObject> warmingObjects = new Vector<MCCRObject>(result.size());
    		
			for (Resolvable resolvable : result) {
				if (resolvable instanceof MCCRObject) {
					MCCRObject mccrObject = (MCCRObject) resolvable;

					warmingObjects.add(mccrObject);

					// put the object in the cache
					put(mccrObject);

					// get all attributes of the type
					Collection<ObjectAttributeBean> attributes = MCCRHelper.getAttributeTypes(ds, mccrObject.contentId.objType);

					// remove the attributes, that shall NOT be warmed from the cache
					for (ObjectAttributeBean attr : attributes) {
						if (!warmingAttributes.contains(attr.getName())) {
							clear(mccrObject, attr.getName());
						}
					}
				}
			}
    		
			// load the attributes, that shall be warmed
			MCCRHelper.batchLoadAttributes(ds, warmingObjects, warmingAttributes, false);
		}

		if (logger.isDebugEnabled()) {
			int objectCount = objects.size();

			logger.debug("Cleared caches for " + objectCount + " objects");
		}
	}

	/**
	 * Make a copy of the object types map
	 * @param objectTypes map of object types
	 * @param mutable true to get a mutable copy, false for an immutable one
	 * @return copy of the map
	 */
	protected static Map<Integer, ObjectTypeBean> copy(Map<Integer, ObjectTypeBean> objectTypes, boolean mutable) {
		if (objectTypes == null) {
			return null;
		}
		Map<Integer, ObjectTypeBean> copy = new HashMap<Integer, ObjectTypeBean>(objectTypes.size());

		for (Map.Entry<Integer, ObjectTypeBean> entry : objectTypes.entrySet()) {
			copy.put(entry.getKey(), entry.getValue().copy(mutable));
		}

		return copy;
	}

	/**
	 * Make a copy of the channel tree
	 * @param channelTree channel tree
	 * @return copy of the channel tree
	 */
	protected static ChannelTree copy(ChannelTree channelTree) {
		if (channelTree == null) {
			return null;
		}
		ChannelTree copy = new ChannelTree();

		for (ChannelTreeNode treeNode : channelTree.getChildren()) {
			copy.getChildren().add(copy(treeNode));
		}

		return copy;
	}

	/**
	 * Make a copy of the channel tree node
	 * @param treeNode channel tree node
	 * @return copy of the channel tree node
	 */
	protected static ChannelTreeNode copy(ChannelTreeNode treeNode) {
		if (treeNode == null) {
			return null;
		}
		ChannelTreeNode copy = new ChannelTreeNode(copy(treeNode.getChannel()));

		for (ChannelTreeNode subTreeNode : treeNode.getChildren()) {
			copy.getChildren().add(copy(subTreeNode));
		}

		return copy;
	}

	/**
	 * Make a copy of the datasource channel
	 * @param channel channel
	 * @return copy of the datasource channel
	 */
	protected static DatasourceChannel copy(DatasourceChannel channel) {
		if (channel == null) {
			return null;
		}
		return new DatasourceChannel(channel.getId(), channel.getName());
	}

	/**
	 * Make a copy of the given object (that can be stored in the cache)
	 * @param ds datasource
	 * @param object object
	 * @param copyFor for what operation is the object copied
	 * @return copy of the object
	 */
	protected static MCCRObject copy(MCCRDatasource ds, MCCRObject object, CopyFor copyFor) {
		MCCRObject copy = new MCCRObject(null, object.id);

		switch (copyFor) {
		case PUT:
			// set attributes to null (attributes are cached individually)
			copy.attributes = null;
			break;

		case GET:
			copy.ds = ds;
			break;
		}
		// copy all other meta attributes
		copy.channelId = object.channelId;
		copy.channelsetId = object.channelsetId;
		copy.contentId = new ContentId(object.contentId.objType, object.contentId.objId);
		copy.updateTimestamp = object.updateTimestamp;

		return copy;
	}

	protected static Object copy(Object value, CopyFor copyFor) {
		Object copy = value;

		switch (copyFor) {
		case PUT:
			if (value == null) {
				copy = CacheDummy.get();
			} else if (value instanceof Collection) {
				@SuppressWarnings("unchecked")
				Collection<Object> col = (Collection<Object>)value;
				Object[] arr = (Object[]) col.toArray(new Object[col.size()]);
				for (int i = 0; i < arr.length; i++) {
					arr[i] = copy(arr[i], copyFor);
				}

				return arr;
			}
			break;
		case GET:
			break;
		}
		return copy;
	}

	/**
	 * Get the channel unspecific group name for the datasource
	 * @param ds datasource
	 * @return group name
	 */
	protected static String getGroupName(MCCRDatasource ds) {
		return ds.toString();
	}

	/**
	 * Get the cache key for storing an object by its channel_id|contentid
	 * @param object object
	 * @return cache key
	 */
	protected static String getContentIdCacheKey(MCCRObject object) {
		return object.contentId.toString();
	}

	/**
	 * Get the cache key for storing an attribute value
	 * @param object object
	 * @param attributeName attribute name
	 * @return cache key
	 */
	protected static Integer getAttributeCacheKey(MCCRObject object, String attributeName) {
		return object.channelsetId;
	}

	/**
	 * Get all objects that were modified since the given timestamp in the given channel
	 * @param channelId channel id
	 * @param timestamp timestamp to start checking
	 * @return list of modified objects, sorted by ascending id
	 * @throws SQLException 
	 */
	protected static List<MCCRObject> getObjectsModifiedSince(MCCRDatasource ds, int channelId, long timestamp) throws DatasourceException {
		try {
			List<MCCRObject> modifiedObjects = new Vector<MCCRObject>();
			DBHandle handle = ds.getHandle();
			SimpleResultProcessor proc = new SimpleResultProcessor();
			List<Object> params = new ArrayList<Object>();

			params.add(channelId);
			params.add(timestamp);
			DB.query(handle,
					"select * from " + handle.getContentMapName() + " where channel_id = ? AND updatetimestamp > ? order by id asc",
					(Object[]) params.toArray(new Object[params.size()]),
					proc);
			for (SimpleResultRow row : proc) {
				modifiedObjects.add(new MCCRObject(ds, row));
			}
			return modifiedObjects;
		} catch (SQLException e) {
			throw new DatasourceException("Error while getting modified objects for " + ds, e);
		}
	}

	/**
	 * Cache dummy class, that contains a single instance. This instance is put into the cache as representant for a "null" value.
	 */
	protected static class CacheDummy implements Serializable {

		/**
		 * Serial Version UID
		 */
		private static final long serialVersionUID = -3540278195142936344L;

		/**
		 * Single instance
		 */
		protected final static CacheDummy instance = new CacheDummy();

		/**
		 * Create the cache dummy
		 */
		protected CacheDummy() {}

		/**
		 * Get the cache dummy
		 * @return cache dummy
		 */
		public static CacheDummy get() {
			return instance;
		}
	}

	/**
	 * Enumeration of operations, an object is copied for
	 */
	protected static enum CopyFor {

		/**
		 * The object is copied to be put into the cache
		 */
		PUT, /**
		 * The object is copied to return from the cache
		 */ GET;
	}

	/**
	 * Helper class for attribute cache settings
	 */
	protected static class AttributeCache {

		/**
		 * Flag to set whether caching the attribute is enabled
		 */
		protected boolean enabled;

		/**
		 * Cache region for the attribute
		 */
		protected String region;

		public AttributeCache(boolean enabled) {
			this.enabled = enabled;
			this.region = ATTRIBUTESCACHEREGION;
		}

		public AttributeCache(String region) {
			this.region = region;
			this.enabled = true;
		}
	}

	/**
	 * Implementation of a very memory efficient map
	 *
	 * @param <K> Key class
	 * @param <T> Value class
	 */
	protected static class SimpleMap<K extends Comparable<? super K>, T> implements Serializable {
		/**
		 * Serial Version UID
		 */
		private static final long serialVersionUID = 6683852629859982075L;

		/**
		 * Keys sorted by their natural order
		 */
		private Object[] keys;

		/**
		 * Values for the keys
		 */
		protected Object[] values;

		/**
		 * Get the value for the given key
		 * @param key key
		 * @return value or null
		 */
		@SuppressWarnings("unchecked")
		public synchronized T get(K key) {
			int index = index(key);
			return (T)(index >= 0 ? values[index] : null);
		}

		/**
		 * Put the value for the given key
		 * @param key key
		 * @param value value
		 */
		public synchronized void put(K key, T value) {
			int index = index(key);
			if (index >= 0) {
				values[index] = value;
			} else {
				Map<K, T> map = asMap();
				map.put(key, value);
				fromMap(map);
			}
		}

		/**
		 * Clear the data
		 */
		public synchronized void clear() {
			if (values != null) {
				for (int index = 0; index < values.length; index++) {
					values[index] = null;
				}
			}
		}

		/**
		 * Clear data for the given key
		 * @param key key
		 */
		public synchronized void clear(K key) {
			int index = index(key);
			if (index >= 0) {
				values[index] = null;
			}
		}

		/**
		 * Get the index for the given key or a value < 0, if the key does not exist
		 * @param key key
		 * @return index
		 */
		protected int index(K key) {
			return keys != null ? Arrays.binarySearch(keys, key) : -1;
		}

		/**
		 * Transform the data into a map
		 * @return map
		 */
		@SuppressWarnings("unchecked")
		protected Map<K, T> asMap() {
			Map<K, T> map = new HashMap<>();
			if (keys != null) {
				for (int index = 0; index < keys.length; index++) {
					map.put((K)keys[index], (T)values[index]);
				}
			}
			return map;
		}

		/**
		 * Set the data from the given map
		 * @param map map
		 */
		protected void fromMap(Map<K, T> map) {
			keys = new Object[map.size()];
			values = new Object[map.size()];

			List<K> keyList = new ArrayList<>(map.keySet());
			Collections.sort(keyList);
			for (int index = 0; index < keyList.size(); index++) {
				keys[index] = keyList.get(index);
				values[index] = map.get(keys[index]);
			}
		}
	}

	/**
	 * Attributes container, which is put into the cache (in the channels container)
	 */
	protected static class Attributes extends SimpleMap<String, Object> {
		/**
		 * Serial Version UID
		 */
		private static final long serialVersionUID = -8920772646594358027L;

		@SuppressWarnings("unchecked")
		@Override
		public synchronized Object get(String key) {
			Object cached = super.get(key);

			if (cached instanceof Collection) {
				// collections need to be duplicated, to make the independent
				// from the cached collection
				List<Object> values = new ArrayList<Object>();

				values.addAll((Collection<? extends Object>) cached);
				return values;
			} else {
				return cached;
			}
		}
	}

	/**
	 * Simple Map with Integers as keys and attributes
	 */
	protected static class SimpleIntMap extends SimpleMap<Integer, Integer> {
		/**
		 * Serial Version UID
		 */
		private static final long serialVersionUID = 3960970154454993399L;
	}

	/**
	 * Simple Map with Integers as keys and MCCRObjects as values
	 */
	protected static class SimpleObjectMap extends SimpleMap<Integer, MCCRObject> {
		/**
		 * Serial Version UID
		 */
		private static final long serialVersionUID = -1814234971008915045L;
	}

	/**
	 * Memory efficient object for storing channel specific attributes
	 */
	protected static class AttributesPerChannel extends SimpleMap<Integer, Attributes> {
		/**
		 * Serial Version UID
		 */
		private static final long serialVersionUID = -2053915256235450468L;

		/**
		 * Get the attribute value for the given channel (null if not cached)
		 * @param channelId channel id
		 * @param attribute attribute name
		 * @return cached value or null
		 */
		public synchronized Object get(int channelId, String attribute) {
			Attributes entry = get(channelId);
			if (entry == null) {
				return null;
			}
			return entry.get(attribute);
		}

		/**
		 * Store the given value into the container for the given attribute and channel
		 * @param channelId channel ID
		 * @param attribute attribute name
		 * @param value value to store (may be null)
		 */
		public synchronized void put(int channelId, String attribute, Object value) {
			Attributes entry = get(channelId);
			if (entry == null) {
				entry = new Attributes();
				Map<Integer, Attributes> map = asMap();
				map.put(channelId, entry);
				fromMap(map);
			}

			// reuse cached value
			if (value != null && values != null) {
				boolean found = false;
				for (Object o : values) {
					Attributes attr = (Attributes)o;
					if (attr.values != null) {
						for (Object cached : attr.values) {
							if (value.equals(cached)) {
								value = cached;
								found = true;
								break;
							}
						}
					}
					if (found) {
						break;
					}
				}
			}

			entry.put(attribute, value);
		}

		/**
		 * Clear the attributes for the given channel
		 * @param channelId channel ID
		 */
		public synchronized void clear(Integer channelId) {
			Attributes entry = get(channelId);
			if (entry != null) {
				entry.clear();
			}
		}

		/**
		 * Clear the attribute for the given channel
		 * @param channelId channel ID
		 * @param attributeName attribute name
		 */
		public synchronized void clear(Integer channelId, String attributeName) {
			Attributes entry = get(channelId);
			if (entry != null) {
				entry.clear(attributeName);
			}
		}
	}
}
