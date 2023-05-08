/*
 * @author herbert
 * @date 09.08.2006
 * @version $Id: AbstractCacheableDatasource.java,v 1.5.4.1 2011-04-07 09:57:50 norbert Exp $
 */
package com.gentics.api.lib.datasource;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import com.gentics.api.lib.cache.PortalCache;
import com.gentics.api.lib.cache.PortalCacheException;
import com.gentics.api.lib.datasource.Datasource.Sorting;
import com.gentics.lib.datasource.DatasourceRowResolver;
import com.gentics.lib.log.NodeLogger;

/**
 * A abstract datasource which should make it easy to implement caching
 * for subclasses.<br>
 * <br>
 * Implementors can use {@link #setCacheEnabled(boolean)} to 
 * enable/disable the cache (by default it is enabled).
 * Before calling to the underlying storage engine, call
 * {@link #getCachedResult(DatasourceResultCacheKeyBase)} to retrieve
 * a cached result, and afterwards {@link #putCachedResult(DatasourceResultCacheKeyBase, Collection)}
 * to put a newly retrieved result into the cache.<br>
 * <br>
 * A cache key can either be retrieved using 
 * {@link #getCacheKey(String, Object[], int, int, Sorting[], Map)}
 * or by implementing a custom {@link DatasourceResultCacheKeyBase}
 * or extending {@link DatasourceResultCacheKey}
 * 
 * @author herbert
 */
public abstract class AbstractCacheableDatasource extends AbstractDatasource {
    
	/**
	 * Defines if the cache is enabled for this instance. - Since this is an
	 * AbstractCacheableDatasource it is by default enabled.
	 */
	protected boolean cacheEnabled = true;

	/**
	 * Name of the cacheregion for queryresults
	 */
	protected final static String RESULTSCACHEREGION = "gentics-portal-cachedatasource-results";
    
	/**
	 * name of the cacheregion for count results
	 */
	protected final static String COUNTCACHEREGION = "gentics-portal-cachedatasource-count";

	/**
	 * The cache for queryresults
	 */
	private static PortalCache queryResultsCache = null;
    
	private static PortalCache countResultsCache = null;
    
	protected NodeLogger logger = null;
    
	/**
	 * Default constructor which will by default enable caching.
	 * @param id datasource id
	 */
	public AbstractCacheableDatasource(String id) {
		super(id);
		logger = NodeLogger.getNodeLogger(this.getClass());
	}
    
	/**
	 * This constructor can be used if cache should not be used 
	 * (by default the cache is enabled)
	 * @param id datasource id
	 * @param cacheEnabled false if caching should be disabled.
	 */
	public AbstractCacheableDatasource(String id, boolean cacheEnabled) {
		this(id);
		this.setCacheEnabled(cacheEnabled);
	}
    
	/**
	 * Sets if the cache should be enabled or disabled.
	 * @param cacheEnabled true if cache should be enabled / false otherwise.
	 */
	protected void setCacheEnabled(boolean cacheEnabled) {
		this.cacheEnabled = cacheEnabled;
		// TODO clear cache if it is disabled ?
	}
    
	/**
	 * Returns if the cache is enabled.
	 * @return true if cache is enabled.
	 */
	public boolean isCacheEnabled() {
		return this.cacheEnabled;
	}
    
	/**
	 * Interface used for uniquely identifying a DatasourceResult.
	 * Implementors might want to use {@link DatasourceResultCacheKey} instead.
	 * @author herbert
	 */
	public static interface DatasourceResultCacheKeyBase extends Serializable {}

	/**
	 * A simple cache key used for caching datasource results. implementations
	 * of Datasource can either directly use this implementation, or if it
	 * is not sufficient can subclass this implementation.
	 * 
	 * @author herbert
	 */
	public static class DatasourceResultCacheKey implements DatasourceResultCacheKeyBase, Serializable {
		private static final long serialVersionUID = 1L;

		private String query;

		private Object[] params;

		private int start;

		private int count;

		private Sorting[] sortedColumns;
        
		private Object[] additionalParameters;

		/**
		 * Creates a new ResultCacheKey. it's arguments need to uniquely identify the result.
		 * The arguments are roughly equal to the getResult methods of the Datasource interface.
		 * 
		 * @param query the query in the native language of the datasource - must not be null.
		 * @param params parameters used to bind variables in the query string - may be null.
		 * @param start start index (when using paging)
		 * @param count count (when using paging)
		 * @param sortedColumns sorted columns when result is sorted - may be null.
		 * @param additionalParameters can be used for additional parameters which determine the result of this query (e.g. basedn for LDAP)
		 * @throws NullPointerException if query is null.
		 */
		public DatasourceResultCacheKey(String query, Object[] params, int start, int count, Sorting[] sortedColumns, Object[] additionalParameters) {
			if (query == null) {
				throw new NullPointerException("query argument must not be null.");
			}
			this.query = query;
			this.params = params;
			this.start = start;
			this.count = count;
			this.sortedColumns = sortedColumns;
			this.additionalParameters = additionalParameters;
		}

		public boolean equals(Object obj) {
			if (!(obj instanceof DatasourceResultCacheKey)) {
				return false;
			}
			DatasourceResultCacheKey cacheKey = (DatasourceResultCacheKey) obj;

			return cacheKey.query.equals(this.query) && Arrays.equals(cacheKey.params, this.params) && cacheKey.start == this.start && cacheKey.count == count
					&& Arrays.equals(cacheKey.sortedColumns, this.sortedColumns) && Arrays.equals(cacheKey.additionalParameters, this.additionalParameters);
		}

		public int hashCode() {
			int hashCode = query.hashCode();

			if (params != null) {
				for (int i = 0; i < params.length; i++) {
					// shamelessly copied from List#hashCode()
					hashCode = 31 * hashCode + params[i].hashCode();
				}
			}
            
			hashCode = 31 * (31 * hashCode + start) + count;
            
			// HashCode for Sorted Columns
			if (sortedColumns != null) {
				for (int i = 0; i < sortedColumns.length; i++) {
					hashCode = 31 * hashCode + sortedColumns[i].hashCode();
				}
			}
            
			// HashCode for AdditionalParameters
			for (int i = 0; i < additionalParameters.length; i++) {
				hashCode = 31 * hashCode + (additionalParameters[i] == null ? 1 : additionalParameters[i].hashCode());
			}
			return hashCode;
		}

		public String toString() {
			StringBuffer buf = new StringBuffer("[CacheKey: query=").append(query).append("; params=[");

			if (params != null) {
				for (int i = 0; i < params.length; i++) {
					buf.append(params[i].toString()).append(", ");
				}
				buf.replace(buf.length() - 2, buf.length(), "];");
			}
			buf.append(" start=").append(start).append("; count=").append(count);
            
			// sorted columns
			if (sortedColumns != null) {
				buf.append("; sortedColumns=[");
				for (int i = 0; i < sortedColumns.length; i++) {
					buf.append(sortedColumns[i].toString()).append(", ");
				}
				buf.replace(buf.length() - 2, buf.length(), "];");
			}
            
			// additional Parameters
			buf.append(" additionalParameters=[");
			for (int i = 0; i < additionalParameters.length; i++) {
				buf.append(additionalParameters[i] == null ? "null" : additionalParameters[i].toString()).append(", ");
			}
			buf.replace(buf.length() - 2, buf.length(), "];");
			return buf.toString();
		}
	}
    
	/**
	 * Datasource cache key for count statements.
	 */
	public static class DatasourceCountCacheKey implements DatasourceResultCacheKeyBase, Serializable {
		private static final long serialVersionUID = 1L;
        
		private String query;
		private Object[] params;
		private Object[] additionalParameters;
        
		public DatasourceCountCacheKey(String query, Object[] params, Object[] additionalParameters) {
			if (query == null) {
				throw new NullPointerException("query argument must not be null.");
			}
			this.query = query;
			this.params = params;
			this.additionalParameters = additionalParameters;
		}

		public boolean equals(Object obj) {
			if (!(obj instanceof DatasourceCountCacheKey)) {
				return false;
			}
			DatasourceCountCacheKey cacheKey = (DatasourceCountCacheKey) obj;

			return cacheKey.query.equals(this.query) && Arrays.equals(cacheKey.params, this.params)
					&& Arrays.equals(cacheKey.additionalParameters, this.additionalParameters);
		}
        
		public int hashCode() {
			int hashCode = query.hashCode();

			if (params != null) {
				for (int i = 0; i < params.length; i++) {
					// shamelessly copied from List#hashCode()
					hashCode = 31 * hashCode + params[i].hashCode();
				}
			}
            
			// HashCode for AdditionalParameters
			for (int i = 0; i < additionalParameters.length; i++) {
				hashCode = 31 * hashCode + (additionalParameters[i] == null ? 1 : additionalParameters[i].hashCode());
			}
			return hashCode;
		}
        
		public String toString() {
			StringBuffer buf = new StringBuffer("[CacheKey: query=").append(query).append("; params=[");

			if (params != null) {
				for (int i = 0; i < params.length; i++) {
					buf.append(params[i].toString()).append(", ");
				}
				buf.replace(buf.length() - 2, buf.length(), "];");
			}
			// additional Parameters
			buf.append(" additionalParameters=[");
			for (int i = 0; i < additionalParameters.length; i++) {
				buf.append(additionalParameters[i] == null ? "null" : additionalParameters[i].toString()).append(", ");
			}
			buf.replace(buf.length() - 2, buf.length(), "];");
			return buf.toString();
		}
	}

	/**
	 * Creates and returns the cache key for the given parameters.
	 * Most parameters are equivalent to the parameters of {@link Datasource#getResult(DatasourceFilter, String[], int, int, Sorting[], Map)}.
	 * The only exception beeing query and objects. Except 'query' all parameters are optional and may be null.
	 * @param query The Datasource / database specific query (e.g. for a datasource relying on a SQL database this would be the SQL query). Must not be null.
	 * @param objects The Datasource / database specific Objects which are referenced from the query (e.g. for a datasource relying on a SQL database this would be the variables binded in the SQL query)
	 * @param start The index of the first item which was returned
	 * @param count the nubmer of items in this result
	 * @param sortedColumns the sort order of the result
	 * @param specificParameters additional parameters given to getResult(..) method.
	 * @return cache key
	 * @throws NullPointerException if query is null.
	 */
	protected DatasourceResultCacheKeyBase getCacheKey(String query, Object[] objects, int start, int count, Sorting[] sortedColumns, Map specificParameters) {
		return new DatasourceResultCacheKey(query, objects, start, count, sortedColumns, new Object[] { specificParameters });
	}
    
	/**
	 * Creates and returns a new cache key for a count statement.
	 * 
	 * @param query datasource specific query
	 * @param objects datasource specific objects which are references from the query (e.g. bind variables)
	 * @param specificParameters additional parameters given to getCount(..) method.
	 * @return a new cache key.
	 * @throws NullPointerException if query is null.
	 */
	protected DatasourceResultCacheKeyBase getCacheKeyForCount(String query, Object[] objects, Map specificParameters) {
		return new DatasourceCountCacheKey(query, objects, new Object[] { specificParameters });
	}

	/**
	 * Returns the Query Results Cache.
	 * @return query results cache.
	 */
	protected PortalCache getQueryResultsCache() {
		if (queryResultsCache != null || !cacheEnabled) {
			return queryResultsCache;
		}
		return createQueryResultsCache();
	}
    
	protected PortalCache getCountResultsCache() {
		if (countResultsCache != null || !cacheEnabled) {
			return countResultsCache;
		}
		return createCountResultsCache();
	}
    
	/**
	 * This method should only be called by {@link #getQueryResultsCache()} - it creates
	 * the query results cache and returns it.
	 * @return
	 * @see #getQueryResultsCache()
	 */
	private synchronized PortalCache createQueryResultsCache() {
		if (queryResultsCache != null) {
			return queryResultsCache;
		}
		try {
			// get the portal cache for queryresults
			queryResultsCache = PortalCache.getCache(RESULTSCACHEREGION);
		} catch (PortalCacheException e) {
			logger.error("Error while initializing the portal cache for the query results." + " Query results will not be cached.", e);
		} catch (NoClassDefFoundError e) {
			logger.error("Error while initializing the portal cache for the query results." + " Query results will not be cached.", e);
		}
		return queryResultsCache;
	}
    
	private synchronized PortalCache createCountResultsCache() {
		if (countResultsCache != null) {
			return countResultsCache;
		}
		try {
			countResultsCache = PortalCache.getCache(COUNTCACHEREGION);
		} catch (PortalCacheException e) {
			logger.error("Error while initializing the portal cache for the query results." + " Query results will not be cached.", e);
		} catch (NoClassDefFoundError e) {
			logger.error("Error while initializing the portal cache for the query results." + " Query results will not be cached.", e);
		}
		return countResultsCache;
	}

	/**
	 * Returns the cached results of a query.
	 * @param cacheKey cache key
	 * @return the cached object, or null if it was not found, or if an error happened.
	 */
	protected Collection getCachedResult(DatasourceResultCacheKeyBase cacheKey) {
		if (!this.cacheEnabled) {
			return null;
		}
		PortalCache cache = getQueryResultsCache();

		if (cache == null) {
			return null;
		}
		Object obj;

		try {
			obj = cache.getFromGroup(this.getUniqueDatasourceIdentifier(), cacheKey);
		} catch (PortalCacheException e) {
			// I don't think a user of this method will ever be interested into which error happened, so just log it and return null.
			logger.error("Error while trying to retrieve cached Datasource Results.", e);
			return null;
		}
		return (Collection) obj;
	}
    
	/**
	 * Returns the cached count for the given cache key or null if none was cached.
	 * @param cacheKey cache key
	 * @return object count or null
	 */
	protected Integer getCachedCount(DatasourceResultCacheKeyBase cacheKey) {
		if (!this.cacheEnabled) {
			return null;
		}
		PortalCache cache = getCountResultsCache();

		if (cache == null) {
			return null;
		}
		Object obj;

		try {
			obj = cache.getFromGroup(this.getUniqueDatasourceIdentifier(), cacheKey);
		} catch (PortalCacheException e) {
			logger.error("Error while trying to retrieved cached count.", e);
			return null;
		}
		return (Integer) obj;
	}

	/**
	 * Stores the given result collection into the cache for later retrieval.
	 * The given result Collection needs to be Serializable. Each item in it has to be Serializable
	 * and must not contain any references to this Datasource.
	 * 
	 * @param cacheKey the cache to to uniquely identify the given result
	 * @param result the result which was produced with the cacheKey.
	 * @return true if it was successfully put into the cache, false otherwise (e.g. if cache was disabled, or an error occurred).
	 */
	protected boolean putCachedResult(DatasourceResultCacheKeyBase cacheKey, Collection result) {
		if (!this.cacheEnabled) {
			return false;
		}
		PortalCache cache = getQueryResultsCache();

		if (cache == null) {
			return false;
		}
		try {
			cache.putIntoGroup(this.getUniqueDatasourceIdentifier(), cacheKey, result);
		} catch (PortalCacheException e) {
			logger.error("Error while trying to put Datasource Result into cache.", e);
			return false;
		}
		return true;
	}
    
	/**
	 * Put a result of a getCount() method into the cache
	 * @param cacheKey cache key
	 * @param count count to be cached
	 * @return true iff put succeeded
	 * @see #getCachedCount(com.gentics.api.lib.datasource.AbstractCacheableDatasource.DatasourceResultCacheKeyBase)
	 */
	protected boolean putCachedCount(DatasourceResultCacheKeyBase cacheKey, Integer count) {
		if (!this.cacheEnabled) {
			return false;
		}
		PortalCache cache = getCountResultsCache();

		if (cache == null) {
			return false;
		}
		try {
			cache.putIntoGroup(this.getUniqueDatasourceIdentifier(), cacheKey, count);
		} catch (PortalCacheException e) {
			logger.error("Error while trying to put count result into cache.", e);
			return false;
		}
		return true;
	}
    
	/**
	 * Clears the cache for this datasource.
	 * @return true if everything went successful and cache was cleared, false if cache is disabled or an error occurred.
	 */
	protected boolean clearCache() {
		if (!isCacheEnabled()) {
			return false;
		}
		PortalCache cache = getQueryResultsCache();

		try {
			cache.clearGroup(getUniqueDatasourceIdentifier());
		} catch (PortalCacheException e) {
			logger.error("Error while clearing cache {" + getUniqueDatasourceIdentifier() + "}", e);
			return false;
		}
		return true;
	}
    
	/**
	 * Stores the unique datasource identifier created by
	 * {@link #getUniqueDatasourceIdentifier()} so it does
	 * not have to be recreated each time, since it shouldn't
	 * change anyway.
	 */
	private String cachedUniqueDatasourceIdentifier;
    
	/**
	 * Returns a unique datasource identifier.
	 * By default simply prefixes {@link #getDatasourceIdentifier()} with
	 * the class name.
	 * @return globally unique identifier.
	 */
	protected String getUniqueDatasourceIdentifier() {
		if (cachedUniqueDatasourceIdentifier == null) {
			return cachedUniqueDatasourceIdentifier = new StringBuffer(this.getClass().getName()).append('|').append(getDatasourceIdentifier()).toString();
		}
		return cachedUniqueDatasourceIdentifier;
	}

	/**
	 * Returns a unique identifier for this datasource.
	 * Needs to uniquely identify this datasource backend (e.g. database)
	 * in the scope of all datasources of the same type.
	 * (e.g. the string representation of the HandlePool) -
	 * should be a constant value which does not change in the lifetime
	 * of the datasource.
	 * @return unique identifier or null if handle pool not set
	 */
	public String getDatasourceIdentifier() {
		String id = this.getId();

		if (id != null) {
			return id;
		}
		if (getHandlePool() == null) {
			return null;
		}
		return getHandlePool().toString();
	}
}
