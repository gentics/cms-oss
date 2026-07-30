package com.gentics.portalnode.formatter;

import java.io.Serializable;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.httpclient.params.HttpConnectionParams;

import com.gentics.api.lib.cache.PortalCache;
import com.gentics.api.lib.cache.PortalCacheAttributes;
import com.gentics.api.lib.cache.PortalCacheException;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.portalnode.imp.AbstractGenticsImp;
import com.gentics.lib.log.NodeLogger;

/**
 * Imp for accessing the given URL and returning the content fetched from there
 */
public class URLIncludeImp extends AbstractGenticsImp {

	/**
	 * Name of the cache region
	 */
	protected final static String CACHE_REGION = "";

	/**
	 * Default timeout for the connection manager
	 */
	protected final static long DEFAULT_CONNECTION_MANAGER_TIMEOUT = 1000;
    
	/**
	 * Default host connection limit
	 */
	protected final static int DEFAULT_HOST_CONNECTION_LIMIT = 20;
    
	/**
	 * Default total connection limit
	 */
	protected final static int DEFAULT_MAX_CONNECTION_LIMIT = 20;
    
	/**
	 * The logger
	 */
	protected final static NodeLogger logger = NodeLogger.getNodeLogger(URLIncludeImp.class);

	/**
	 * Cache instance for the cache region
	 */
	protected static PortalCache cache;

	/**
	 * connection manager instance
	 */
	protected static MultiThreadedHttpConnectionManager connectionManager = null;

	/**
	 * http client instance
	 */
	protected static HttpClient client;

	static {
		try {
			cache = PortalCache.getCache(CACHE_REGION);
		} catch (PortalCacheException e) {
			logger.error("Error while initializing cache region " + CACHE_REGION, e);
		}

		// create the connection manager and the http client instance
		connectionManager = new MultiThreadedHttpConnectionManager();
        
		int maxHostConnections = ObjectTransformer.getInteger(System.getProperty(HttpConnectionManagerParams.MAX_HOST_CONNECTIONS),
				DEFAULT_HOST_CONNECTION_LIMIT);

		logger.debug("Using max host connections: " + HttpConnectionManagerParams.MAX_HOST_CONNECTIONS + " {" + maxHostConnections + "}");

		int maxTotalConnections = ObjectTransformer.getInteger(System.getProperty(HttpConnectionManagerParams.MAX_TOTAL_CONNECTIONS),
				DEFAULT_MAX_CONNECTION_LIMIT);

		logger.debug("Using max total connections: " + HttpConnectionManagerParams.MAX_TOTAL_CONNECTIONS + " {" + maxTotalConnections + "}");
        
		connectionManager.getParams().setMaxTotalConnections(maxTotalConnections);
		connectionManager.getParams().setDefaultMaxConnectionsPerHost(maxHostConnections);

		client = new HttpClient(connectionManager);
        
		// Set timeout on how long we’ll wait for a connection from the pool
		client.getParams().setLongParameter(HttpClientParams.CONNECTION_MANAGER_TIMEOUT, DEFAULT_CONNECTION_MANAGER_TIMEOUT);
		client.getParams().setParameter(HttpClientParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler(0, false));
	}

	/**
	 * Access the given URL and return the content
	 * 
	 * @param url
	 *            URL to be requested
	 * @param cacheLifeTime
	 *            cache lifetime in secs, defaults to 300 secs (5 mins)
	 * @param timeout
	 *            timeout for accessing the URL and getting the contents in ms,
	 *            defaults to 2000 ms (2 secs)
	 * @param defaultContent
	 *            default content to be returned, if the URL cannot be accessed
	 * @return content fetched from the URL. If null is given and the URL cannot
	 *         be accessed, an exception is thrown
	 * @throws NodeException
	 *             if the URL cannot be accessed and no defaultContent was given
	 */
    
	public String includeUrl(String url, int cacheLifeTime, int timeout, String defaultContent) throws NodeException {
    	
		if (logger.isDebugEnabled()) {
			logger.debug("include(" + url + ", " + cacheLifeTime + ", " + timeout + ", " + defaultContent + ")");
		}
        
		// check whether a URL was given
		if (ObjectTransformer.isEmpty(url)) {
			logger.warn("Error while including URL: URL was empty");
			return "";
		}

		// check whether the content is cached
		if (cache != null) {
			try {
				Object cachedObject = cache.get(url);

				if (CachedError.isCachedError(cachedObject)) {
					if (logger.isDebugEnabled()) {
						logger.debug("Got cached error");
					}
					return handleError(url, "Access to URL {" + url + "} failed and failure was cached.", defaultContent, null, false, cacheLifeTime);
				}
				String content = ObjectTransformer.getString(cachedObject, null);

				if (content != null) {
					if (logger.isDebugEnabled()) {
						logger.debug("Got content from cache");
					}
					return content;
				}
			} catch (PortalCacheException e) {
				logger.warn("Error while getting cached content for {" + url + "}", e);
			}
		}

		// request the URL and get the contents
		GetMethod getRequest = new GetMethod(url);

		getRequest.getParams().setCookiePolicy(CookiePolicy.IGNORE_COOKIES);
		getRequest.setFollowRedirects(true);
		getRequest.getParams().setSoTimeout(timeout);
		getRequest.getParams().setIntParameter(HttpConnectionParams.CONNECTION_TIMEOUT, timeout);

		String content = null;

		try {
			int status = client.executeMethod(getRequest);

			switch (status) {
			case HttpStatus.SC_OK:
			case HttpStatus.SC_NO_CONTENT:
				if (logger.isDebugEnabled()) {
					logger.debug("Got content from URL {" + url + "}");
				}
				content = getRequest.getResponseBodyAsString();
				break;

			default:
				return handleError(url, "Error while accessing url {" + url + "}, response code was " + status, defaultContent, null, true, cacheLifeTime);
			}
		} catch (Exception e) {
			return handleError(url, "Error while accessing url {" + url + "}", defaultContent, e, true, cacheLifeTime);
		}

		// put contents into cache and return it
		if (cache != null) {
			try {
				cache.put(url, content, new CacheAttributes(cacheLifeTime));
			} catch (PortalCacheException e) {
				logger.warn("Error while putting content of {" + url + "} into cache", e);
			}
		}

		return content;
	}
    
	public String include(Object url, Object cacheLifeTime, Object timeout,
			Object defaultContent) throws NodeException {
    
		// get the parameters
		String sURL = ObjectTransformer.getString(url, null);
		int iCacheLifeTime = ObjectTransformer.getInt(cacheLifeTime, 300);
		int iTimeout = ObjectTransformer.getInt(timeout, 2000);
		String sDefaultContent = ObjectTransformer.getString(defaultContent, null);

		return includeUrl(sURL, iCacheLifeTime, iTimeout, sDefaultContent);
        
	}

	/**
	 * Method to handle an error
	 * 
	 * @param URL
	 *            url that was accessed
	 * @param message
	 *            message of the error
	 * @param defaultContent
	 *            default content to be returned in case of an error, may be
	 *            null
	 * @param t
	 *            throwable (root cause, may be null)
	 * @param putIntoCache
	 *            true if the error shall be cached, false if not (because it
	 *            already is cached)
	 * @param cacheLifeTime
	 *            cache life time in seconds
	 * @return the content to return
	 * @throws NodeException
	 */
	protected String handleError(String URL, String message,
			String defaultContent, Throwable t, boolean putIntoCache,
			int cacheLifeTime) throws NodeException {
		// log an error
		logger.error(message, t);

		if (putIntoCache && cache != null) {
			try {
				cache.put(URL, CachedError.getInstance(), new CacheAttributes(cacheLifeTime));
			} catch (PortalCacheException e) {}
		}

		// either return the default content or throw an exception
		if (defaultContent != null) {
			return defaultContent;
		} else {
			throw new NodeException(message, t);
		}
	}

	/**
	 * Access the given URL and return the content
	 * 
	 * @param url
	 *            URL to be requested
	 * @param cacheLifeTime
	 *            cache lifetime in secs
	 * @param timeout
	 *            timeout for accessing the URL and getting the contents in ms
	 * @return content fetched from the URL.
	 * @throws NodeException
	 *             if the URL cannot be accessed
	 */
	public String include(Object url, Object cacheLifeTime, Object timeout) throws NodeException {
		return include(url, cacheLifeTime, timeout, null);
	}

	/**
	 * Access the given URL and return the content
	 * 
	 * @param url
	 *            URL to be requested
	 * @param cacheLifeTime
	 *            cache lifetime in secs
	 * @return content fetched from the URL.
	 * @throws NodeException
	 *             if the URL cannot be accessed
	 */
	public String include(Object url, Object cacheLifeTime) throws NodeException {
		return include(url, cacheLifeTime, null, null);
	}

	/**
	 * Access the given URL and return the content
	 * 
	 * @param url
	 *            URL to be requested
	 * @return content fetched from the URL.
	 * @throws NodeException
	 *             if the URL cannot be accessed
	 */
	public String include(Object url) throws NodeException {
		return include(url, null, null, null);
	}

	/**
	 * Internal class for cache attributes. This is used to set the maximum age
	 * of cache entries
	 */
	public static class CacheAttributes implements PortalCacheAttributes {

		/**
		 * Maximum Age
		 */
		private int maxAge;

		/**
		 * Creation date
		 */
		private long createDate;

		/**
		 * Last access time
		 */
		private long lastAccessTime;

		/**
		 * Create an instance of the Cache Attributes
		 * 
		 * @param maxAge
		 *            maximum age (in secs)
		 */
		public CacheAttributes(int maxAge) {
			this.maxAge = maxAge;
			createDate = System.currentTimeMillis();
			lastAccessTime = createDate;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.gentics.api.lib.cache.PortalCacheAttributes#getCreateDate()
		 */
		public long getCreateDate() {
			return createDate;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.gentics.api.lib.cache.PortalCacheAttributes#getIsEternal()
		 */
		public boolean getIsEternal() {
			return false;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * com.gentics.api.lib.cache.PortalCacheAttributes#getLastAccessDate()
		 */
		public long getLastAccessDate() {
			return lastAccessTime;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.gentics.api.lib.cache.PortalCacheAttributes#getMaxAge()
		 */
		public int getMaxAge() {
			return maxAge;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.gentics.api.lib.cache.PortalCacheAttributes#getMaxIdleTime()
		 */
		public int getMaxIdleTime() {
			return 0;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.gentics.api.lib.cache.PortalCacheAttributes#getSize()
		 */
		public int getSize() {
			return 0;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * com.gentics.api.lib.cache.PortalCacheAttributes#setIsEternal(boolean)
		 */
		public void setIsEternal(boolean isEternal) {}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * com.gentics.api.lib.cache.PortalCacheAttributes#setLastAccessDateToNow
		 * ()
		 */
		public void setLastAccessDateToNow() {
			lastAccessTime = System.currentTimeMillis();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.gentics.api.lib.cache.PortalCacheAttributes#setMaxAge(int)
		 */
		public void setMaxAge(int maxAge) {
			this.maxAge = maxAge;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * com.gentics.api.lib.cache.PortalCacheAttributes#setMaxIdleTime(int)
		 */
		public void setMaxIdleTime(int maxIdleTime) {}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.gentics.api.lib.cache.PortalCacheAttributes#setSize(int)
		 */
		public void setSize(int size) {}
	}

	/**
	 * This class is just for caching errors.
	 */
	public static class CachedError implements Serializable {

		/**
		 * Serial Version UID
		 */
		private static final long serialVersionUID = -3780962562170649534L;

		/**
		 * Singleton for the cached error
		 */
		protected final static CachedError instance = new CachedError();

		/**
		 * Private constructor to prohibit external object creation
		 */
		private CachedError() {}

		/**
		 * Get the singleton
		 * 
		 * @return singleton
		 */
		public static CachedError getInstance() {
			return instance;
		}

		/**
		 * Check whether the given object is a cached error
		 * 
		 * @param o
		 *            object to check
		 * @return true if it is a cached error, false if not
		 */
		public static boolean isCachedError(Object o) {
			return (o instanceof CachedError);
		}
	}
}
