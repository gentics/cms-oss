package com.gentics.lib.etc;

/**
 * @author Erwin Mascher (e.mascher@gentics.com) Date: 03.02.2004
 */
public class TimedCache {
	private long cacheTime;

	private long insTime;

	private com.gentics.lib.etc.CacheTimeoutListener listener;

	private Object CachedObject;

	public TimedCache(long cacheTime, com.gentics.lib.etc.CacheTimeoutListener listener) {
		this.cacheTime = cacheTime;
		this.listener = listener;
		this.insTime = 0;
	}

	public Object get() {
		if (insTime == 0 || System.currentTimeMillis() > insTime + cacheTime) {
			set(listener.updateCacheObject(CachedObject));
		}
		return CachedObject;
	}

	public void set(Object o) {
		insTime = System.currentTimeMillis();
		CachedObject = o;
	}
}
