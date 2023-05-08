/*
 * @author jan
 * @date Oct 17, 2008
 * @version $Id: SimplePublishInfo.java,v 1.3 2010-09-28 17:01:31 norbert Exp $
 */
package com.gentics.contentnode.publish;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.logging.log4j.Level;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.msg.DefaultNodeMessage;
import com.gentics.contentnode.msg.NodeMessage;
import com.gentics.lib.etc.IWorkPhase;

/**
 * This is a simple implementation of the publishinfo interface.
 * This class stores the cummulative status of all sub-processes.
 */
public class SimplePublishInfo extends CNWorkPhase implements PublishInfo, Serializable {
	/**
	 * Serial Vesion UID
	 */
	private static final long serialVersionUID = 9009053366767580226L;

	private int returnCode;

	private Throwable error;

	private List<IWorkPhase> workPhases = new ArrayList<>();

	private int remainingPageCount = 0;

	private int remainingFileCount = 0;

	private int remainingFolderCount = 0;

	private int remainingFormCount = 0;

	private int publishedFolderCount = 0;

	private int publishedPageCount = 0;

	private int publishedFileCount = 0;

	private int publishedFormCount = 0;

	private int totalPageRenderCount;

	private boolean initialized = false;

	private float currentCpuUsage = -1;

	private long currentHeapMemoryUsage = -1;

	private volatile long currentThreadCount = -1;

	private float loadAverage = -1;

	private float loadLimit = -1;

	private List<PublishThreadInfo> publishThreadInfos = Collections.emptyList();

	private int threadLimit = -1;

	private List<NodeMessage> messages = new Vector<>();

	public SimplePublishInfo() {
		super(null, "Publishing", "Publishing");
	}

	public void init() throws NodeException {
		super.init();
		this.initialized = true;
	}

	public void setTotalPageRenderCount(int totalPageRenderCount) {
		this.totalPageRenderCount = totalPageRenderCount;
		this.remainingPageCount = totalPageRenderCount;
	}

	public void setFileRenderCount(int fileRenderCount) {
		this.remainingFileCount = fileRenderCount;
	}

	public void setFolderRenderCount(int folderRenderCount) {
		this.remainingFolderCount = folderRenderCount;
	}

	public void setFormRenderCount(int formRenderCount) {
		this.remainingFormCount = formRenderCount;
	}

	public void incFileRenderCount(int fileRenderCount) {
		if (this.remainingFileCount == -1) {
			this.remainingFileCount = 0;
		}
		this.remainingFileCount += fileRenderCount;
	}

	public void incFolderRenderCount(int folderRenderCount) {
		if (this.remainingFolderCount == -1) {
			this.remainingFolderCount = 0;
		}
		this.remainingFolderCount += folderRenderCount;
	}

	public void incFormRenderCount(int formRenderCount) {
		if (this.remainingFormCount == -1) {
			this.remainingFormCount = 0;
		}
		this.remainingFormCount += formRenderCount;
	}

	public int getTotalPageRenderCount() {
		return this.totalPageRenderCount;
	}

	public int getPhaseCount() {
		return getPhaseCount(null);
	}

	public int getCurrentPhaseNumber() {
		IWorkPhase phase = getCurrentPhase();

		if (phase == null) {
			return -1;
		}
		return getPhaseCount(phase);
	}

	public String getCurrentPhaseName() {
		IWorkPhase phase = getCurrentPhase();

		if (phase != null) {
			return phase.getName();
		}
		return "";
	}

	public String getStatusMessage() {
		IWorkPhase phase = getCurrentPhase();

		if (phase != null) {
			// Counting all phases

			StringBuffer ret = new StringBuffer("Current Phase: ");

			if (phase != this) {
				ret.append(getPhaseCount(phase)).append('/').append(getPhaseCount(null)).append(' ');
			}
			ret.append(phase.getName()).append("  (").append(phase.getProgress()).append("%)");
			return ret.toString();
		}
		return "No active phase.";
	}

	public void setReturnCode(int returnCode) {
		this.returnCode = returnCode;
	}

	public void setError(Throwable t) {
		this.error = t;
		StringWriter sw = new StringWriter();
		error.printStackTrace(new PrintWriter(sw));
		messages.add(new DefaultNodeMessage(Level.ERROR, Publisher.class, sw.toString(), error));
	}

	/**
	 * add a new publishstatus object for a new sub-process.
	 * @param stage the name of the sub-process.
	 * @param status the status object which should be added to the cummulative status.
	 */
	public void addPublishStatus(String stage, PublishStatus status) {}

	public void addWorkPhase(IWorkPhase phase) {
		workPhases.add(phase);
	}

	public Collection<NodeMessage> getMessages() {
		return messages;
	}

	public int getReturnCode() {
		return returnCode;
	}

	public int getEstimatedDuration() {
		// do not give an ETA if we are not initialized.
		if (!initialized) {
			return 0;
		}
		return getETA() / 1000;
	}

	public int getProgess() {
		// if(getTotalWork() == 0) {
		// return 0;
		// }
		// return 100 / getTotalWork() * getTotalDoneWork();
		if (!initialized) {
			return 0;
		}
		long elapsed = System.currentTimeMillis() - getStartTime();

		if (elapsed < 2000) {
			return 1;
		}
		return (int) (100. / (elapsed + getETA()) * elapsed);
	}

	public boolean isInitialized() {
		return initialized;
	}

	public int getRemainingPageCount() {
		return remainingPageCount;
	}

	public int getRemainingFileCount() {
		return remainingFileCount;
	}

	public int getRemainingFolderCount() {
		return remainingFolderCount;
	}

	@Override
	public int getRemainingFormCount() {
		return remainingFormCount;
	}

	@Override
	public int getPublishedFolderCount() {
		return publishedFolderCount;
	}

	@Override
	public int getPublishedFormCount() {
		return publishedFormCount;
	}

	/**
	 * Set the number of successfully published folders.
	 *
	 * @param publishedFolderCount The number of successfully published folders.
	 */
	public void setPublishedFolderCount(int publishedFolderCount) {
		this.publishedFolderCount = publishedFolderCount;
	}

	@Override
	public int getPublishedPageCount() {
		return publishedPageCount;
	}

	/**
	 * Set the number of successfully published pages.
	 *
	 * @param publishedPageCount The number of successfully published pages.
	 */
	public void setPublishedPageCount(int publishedPageCount) {
		this.publishedPageCount = publishedPageCount;
	}

	@Override
	public int getPublishedFileCount() {
		return publishedFileCount;
	}

	/**
	 * Set the number of successfully published files.
	 *
	 * @param publishedFileCount The number of successfully published files.
	 */
	public void setPublishedFileCount(int publishedFileCount) {
		this.publishedFileCount = publishedFileCount;
	}

	/**
	 * Set the number of successfully published forms.
	 *
	 * @param publishedFormCount The number of successfully published forms.
	 */
	public void setPublishedFormCount(int publishedFormCount) {
		this.publishedFormCount = publishedFormCount;
	}

	/**
	 * Set one page rendered
	 */
	public synchronized void pageRendered() {
		this.remainingPageCount--;
	}

	public synchronized void fileRendered() {
		this.remainingFileCount--;
	}

	public synchronized void folderRendered() {
		this.remainingFolderCount--;
	}

	public synchronized void formRendered() {
		this.remainingFormCount--;
	}

	public String[] getAllPhaseNames() {
		ArrayList<String> list = new ArrayList<>();

		getAllPhaseNames(this, list);
		return list.toArray(new String[list.size()]);
	}

	private void getAllPhaseNames(IWorkPhase phase, ArrayList<String> list) {
		list.add(phase.getName());
		List subPhases = phase.getSubPhases();

		if (subPhases != null && subPhases.size() > 0) {
			for (Iterator i = subPhases.iterator(); i.hasNext();) {
				getAllPhaseNames((IWorkPhase) i.next(), list);
			}
		}
	}

	public int getEstimatedDurationForCurrentPhase() {
		IWorkPhase phase = getCurrentPhase();

		if (phase == null) {
			return 0;
		}
		return phase.getETA() / 1000;
	}

	/**
	 * @return the currentCpuUsage
	 */
	public float getCurrentCpuUsage() {
		return currentCpuUsage;
	}

	/**
	 * @param currentCpuUsage the currentCpuUsage to set
	 */
	public void setCurrentCpuUsage(float currentCpuUsage) {
		this.currentCpuUsage = currentCpuUsage;
	}

	/**
	 * @return the currentHeapMemoryUsage
	 */
	public long getCurrentHeapMemoryUsage() {
		return currentHeapMemoryUsage;
	}

	/**
	 * @param currentHeapMemoryUsage the currentHeapMemoryUsage to set
	 */
	public void setCurrentHeapMemoryUsage(long currentHeapMemoryUsage) {
		this.currentHeapMemoryUsage = currentHeapMemoryUsage;
	}

	/**
	 * @return the currentThreadCount
	 */
	public long getCurrentThreadCount() {
		return currentThreadCount;
	}

	/**
	 * @param currentThreadCount the currentThreadCount to set
	 */
	public void setCurrentThreadCount(long currentThreadCount) {
		this.currentThreadCount = currentThreadCount;
	}

	/**
	 * @return the loadAverage
	 */
	public float getLoadAverage() {
		return loadAverage;
	}

	/**
	 * @param loadAverage the loadAverage to set
	 */
	public void setLoadAverage(float loadAverage) {
		this.loadAverage = loadAverage;
	}

	/**
	 * @return the loadLimit
	 */
	public float getLoadLimit() {
		return loadLimit;
	}

	/**
	 * @param loadLimit the loadLimit to set
	 */
	public void setLoadLimit(float loadLimit) {
		this.loadLimit = loadLimit;
	}

	/**
	 * @return the publishThreadInfos
	 */
	public List<PublishThreadInfo> getPublishThreadInfos() {
		return publishThreadInfos;
	}

	/**
	 * @param publishThreadInfos the publishThreadInfos to set
	 */
	public void setPublishThreadInfos(List publishThreadInfos) {
		this.publishThreadInfos = publishThreadInfos;
	}

	/**
	 * @return the threadLimit
	 */
	public int getThreadLimit() {
		return threadLimit;
	}

	/**
	 * @param threadLimit the threadLimit to set
	 */
	public void setThreadLimit(int threadLimit) {
		this.threadLimit = threadLimit;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.publish.PublishInfo#addMessage(com.gentics.lib.base.NodeMessage)
	 */
	public void addMessage(NodeMessage message) {
		messages.add(message);
	}
}
