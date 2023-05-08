/*
 * @author Stefan Hepp
 * @date 10.05.2006
 * @version $Id: PortalCacheAttributes.java,v 1.1.4.2 2011-04-07 09:57:53 norbert Exp $
 */
package com.gentics.api.lib.cache;

/**
 * The PortalCacheAttributes interface defines attributes which can be stored
 * to cached elements.
 */
public interface PortalCacheAttributes {
    
	/**
	 * true if this cache object should not be expired -
	 * maxLive will be ignored
	 * @return true if object should not expire
	 */
	boolean getIsEternal();
    
	/**
	 * Sets this cache object eternal - it will live not expire.
	 * @param isEternal true for making eternal
	 */
	void setIsEternal(boolean isEternal);

	/**
	 * get the creation date as timestamp in milliseconds from the entry.
	 * The value is required for maxAge to work.
	 * @return the creation date of an entry in milliseconds.
	 */
	long getCreateDate();

	/**
	 * get the last access date of the cache element as timestamp in milliseconds.
	 * The value is required for maxIdleTime to work.
	 * @return the last access date of an entry.
	 */
	long getLastAccessDate();

	/**
	 * update the current access time to the current timestamp.
	 */
	void setLastAccessDateToNow();

	/**
	 * get the maximum age that the cache entry may exist in seconds.
	 * @return the max age of an object in seconds.
	 * @see #getIsEternal()
	 */
	int getMaxAge();

	/**
	 * set the maximum age of a cache entry - has to be &gt; 0
	 * @param maxAge the max age of the entry in seconds.
	 * @see #setIsEternal(boolean)
	 */
	void setMaxAge(int maxAge);

	/**
	 * get the maximum idle time of the cache entry in seconds. If the last accesstime
	 * or the value is 0, the idle time is ignored.
	 * @return the max idle time of the cache entry in seconds.
	 */
	int getMaxIdleTime();

	/**
	 * set the maximum idle time of the cache entry in seconds.
	 * @param maxIdleTime the max idle time of the entry in seconds.
	 */
	void setMaxIdleTime(int maxIdleTime);

	/**
	 * get the approximate size of the entry in bytes.
	 * The value may be used by the cache to calculate the current cache size.
	 * @return the size of the entry in bytes.
	 */
	int getSize();

	/**
	 * set the cache entry size in bytes.
	 * @param size the approximate size of the entry in bytes.
	 */
	void setSize(int size);

}
