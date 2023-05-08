package com.gentics.contentnode.jmx;

/**
 * Instance of hte PublishWorkerInfo Management Bean
 */
public class PublishWorkerInfo implements PublishWorkerInfoMBean {

	/**
	 * Current page ID
	 */
	protected String currentPageId;

	/**
	 * Timestamp when the current page was started (in ms)
	 */
	protected long startTime = 0;

	/**
	 * Number of pages done (including the current page)
	 */
	protected long pagesDone = 0;

	/**
	 * Total duration (excluding the current page)
	 */
	protected long totalDuration = 0;

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.jmx.PublishWorkerInfoMBean#getCurrentPageId()
	 */
	public String getCurrentPageId() {
		return currentPageId;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.jmx.PublishWorkerInfoMBean#getCurrentDuration()
	 */
	public long getCurrentDuration() {
		if (startTime > 0) {
			return System.currentTimeMillis() - startTime;
		} else {
			return 0;
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.jmx.PublishWorkerInfoMBean#getAverageDuration()
	 */
	public long getAverageDuration() {
		if (pagesDone == 0) {
			return 0;
		} else {
			long currentTotalDuration = totalDuration + getCurrentDuration();

			return currentTotalDuration / pagesDone;
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.jmx.PublishWorkerInfoMBean#getPagesDone()
	 */
	public long getPagesDone() {
		return pagesDone;
	}

	/**
	 * Start working on the page with given ID
	 * @param pageId page ID
	 */
	public void startPage(String pageId) {
		stopPage();
		currentPageId = pageId;
		startTime = System.currentTimeMillis();
		pagesDone++;
	}

	/**
	 * Stop working on the current page (if any)
	 */
	public void stopPage() {
		if (currentPageId != null) {
			totalDuration += getCurrentDuration();
			currentPageId = null;
			startTime = 0;
			currentPageId = null;
		}
	}
}
