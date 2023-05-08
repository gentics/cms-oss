package com.gentics.node.tests.cache;

import com.gentics.api.lib.cache.PortalCacheAttributes;

/**
 * Test implementation of {@link PortalCacheAttributes}
 */
public class TestedPortalCacheAttributes implements PortalCacheAttributes {
	private boolean isEternal;

	private long createDate = System.currentTimeMillis();

	private long lastAccessDate = 0L;

	private int maxAge = 0;

	private int maxIdleTime = 0;

	private int size = 0;

	@Override
	public boolean getIsEternal() {
		return isEternal;
	}

	@Override
	public void setIsEternal(boolean isEternal) {
		this.isEternal = isEternal;
	}

	@Override
	public long getCreateDate() {
		return createDate;
	}

	@Override
	public long getLastAccessDate() {
		return lastAccessDate;
	}

	@Override
	public void setLastAccessDateToNow() {
		lastAccessDate = System.currentTimeMillis();
	}

	@Override
	public int getMaxAge() {
		return maxAge;
	}

	@Override
	public void setMaxAge(int maxAge) {
		this.maxAge = maxAge;
	}

	@Override
	public int getMaxIdleTime() {
		return maxIdleTime;
	}

	@Override
	public void setMaxIdleTime(int maxIdleTime) {
		this.maxIdleTime = maxIdleTime;
	}

	@Override
	public int getSize() {
		return size;
	}

	@Override
	public void setSize(int size) {
		this.size = size;
	}
}
