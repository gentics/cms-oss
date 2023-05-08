package com.gentics.contentnode.jmx;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * PublisherInfo Management Bean
 */
public class PublisherInfo implements PublisherInfoMBean, Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Flag to mark whether the publish process is running
	 */
	protected boolean running = false;

	/**
	 * Flag to mark whether the publish process is multithreaded
	 */
	protected boolean multiThreaded = false;

	/**
	 * Total number of pages to publish in the current publish run
	 */
	protected Map<Integer, AtomicInteger> pagesToPublish = new HashMap<>();

	/**
	 * Number of pages already published in the current publish run
	 */
	protected Map<Integer, AtomicInteger> publishedPages = new HashMap<>();

	/**
	 * Total number of files to publish in the current publish run
	 */
	protected Map<Integer, AtomicInteger> filesToPublish = new HashMap<>();

	/**
	 * Number of files already published in the current publish run
	 */
	protected Map<Integer, AtomicInteger> publishedFiles = new HashMap<>();

	/**
	 * Total number of folders to publish in the current publish run
	 */
	protected Map<Integer, AtomicInteger> foldersToPublish = new HashMap<>();

	/**
	 * Number of folders already published in the current publish run
	 */
	protected Map<Integer, AtomicInteger> publishedFolders = new HashMap<>();

	/**
	 * Total number of forms to publish in the current publish run
	 */
	protected Map<Integer, AtomicInteger> formsToPublish = new HashMap<>();

	/**
	 * Number of forms already published in the current publish run
	 */
	protected Map<Integer, AtomicInteger> publishedForms = new HashMap<>();

	/**
	 * Current publisher phase
	 */
	protected PublisherPhase phase = PublisherPhase.IDLE;

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.jmx.PublisherInfoMBean#isRunning()
	 */
	public boolean isRunning() {
		return running;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.jmx.PublisherInfoMBean#isMultiThreaded()
	 */
	public boolean isMultiThreaded() {
		return multiThreaded;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.jmx.PublisherInfoMBean#getState()
	 */
	public String getState() {
		return phase.toString();
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.jmx.PublisherInfoMBean#getPagesToPublish()
	 */
	public synchronized int getPagesToPublish() {
		return pagesToPublish.values().stream().mapToInt(AtomicInteger::get).sum();
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.jmx.PublisherInfoMBean#getPublishedPages()
	 */
	public synchronized int getPublishedPages() {
		return publishedPages.values().stream().mapToInt(AtomicInteger::get).sum();
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.jmx.PublisherInfoMBean#getRemainingPages()
	 */
	public synchronized int getRemainingPages() {
		return Math.max(0, getPagesToPublish() - getPublishedPages());
	}

	@Override
	public synchronized int getPagesToPublish(int nodeId) {
		AtomicInteger count = pagesToPublish.get(nodeId);
		return count != null ? count.get() : 0;
	}

	@Override
	public synchronized int getPublishedPages(int nodeId) {
		AtomicInteger count = publishedPages.get(nodeId);
		return count != null ? count.get() : 0;
	}

	@Override
	public synchronized int getRemainingPages(int nodeId) {
		return Math.max(0, getPagesToPublish(nodeId) - getPublishedPages(nodeId));
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.jmx.PublisherInfoMBean#getFilesToPublish()
	 */
	public synchronized int getFilesToPublish() {
		return filesToPublish.values().stream().mapToInt(AtomicInteger::get).sum();
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.jmx.PublisherInfoMBean#getPublishedFiles()
	 */
	public synchronized int getPublishedFiles() {
		return publishedFiles.values().stream().mapToInt(AtomicInteger::get).sum();
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.jmx.PublisherInfoMBean#getRemainingFiles()
	 */
	public synchronized int getRemainingFiles() {
		return Math.max(0, getFilesToPublish() - getPublishedFiles());
	}

	@Override
	public synchronized int getFilesToPublish(int nodeId) {
		AtomicInteger count = filesToPublish.get(nodeId);
		return count != null ? count.get() : 0;
	}

	@Override
	public synchronized int getPublishedFiles(int nodeId) {
		AtomicInteger count = publishedFiles.get(nodeId);
		return count != null ? count.get() : 0;
	}

	@Override
	public synchronized int getRemainingFiles(int nodeId) {
		return Math.max(0, getFilesToPublish(nodeId) - getPublishedFiles(nodeId));
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.jmx.PublisherInfoMBean#getFoldersToPublish()
	 */
	public synchronized int getFoldersToPublish() {
		return foldersToPublish.values().stream().mapToInt(AtomicInteger::get).sum();
	}
	
	/* (non-Javadoc)
	 * @see com.gentics.contentnode.jmx.PublisherInfoMBean#getPublishedFolders()
	 */
	public synchronized int getPublishedFolders() {
		return publishedFolders.values().stream().mapToInt(AtomicInteger::get).sum();
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.jmx.PublisherInfoMBean#getRemainingFolders()
	 */
	public synchronized int getRemainingFolders() {
		return Math.max(0, getFoldersToPublish() - getPublishedFolders());
	}

	@Override
	public synchronized int getFoldersToPublish(int nodeId) {
		AtomicInteger count = foldersToPublish.get(nodeId);
		return count != null ? count.get() : 0;
	}

	@Override
	public synchronized int getPublishedFolders(int nodeId) {
		AtomicInteger count = publishedFolders.get(nodeId);
		return count != null ? count.get() : 0;
	}

	@Override
	public synchronized int getRemainingFolders(int nodeId) {
		return Math.max(0, getFoldersToPublish(nodeId) - getPublishedFolders(nodeId));
	}

	@Override
	public synchronized int getFormsToPublish() {
		return formsToPublish.values().stream().mapToInt(AtomicInteger::get).sum();
	}

	@Override
	public synchronized int getPublishedForms() {
		return publishedForms.values().stream().mapToInt(AtomicInteger::get).sum();
	}

	@Override
	public synchronized int getRemainingForms() {
		return Math.max(0, getFormsToPublish() - getPublishedForms());
	}

	@Override
	public synchronized int getFormsToPublish(int nodeId) {
		AtomicInteger count = formsToPublish.get(nodeId);
		return count != null ? count.get() : 0;
	}

	@Override
	public synchronized int getPublishedForms(int nodeId) {
		AtomicInteger count = publishedForms.get(nodeId);
		return count != null ? count.get() : 0;
	}

	@Override
	public synchronized int getRemainingForms(int nodeId) {
		return Math.max(0, getFormsToPublish(nodeId) - getPublishedForms(nodeId));
	}

	/**
	 * Set whether the publish process is running
	 * @param running true if the publish process is running, false if not
	 */
	public synchronized void setRunning(boolean running) {
		this.running = running;
		if (!this.running) {
			pagesToPublish.clear();
			publishedPages.clear();
			filesToPublish.clear();
			publishedFiles.clear();
			foldersToPublish.clear();
			publishedFolders.clear();
			formsToPublish.clear();
			publishedForms.clear();
		}
	}

	/**
	 * Set whether the publish process is multithreaded
	 * @param multiThreaded true for multithreaded
	 */
	public void setMultiThreaded(boolean multiThreaded) {
		this.multiThreaded = multiThreaded;
	}

	/**
	 * Set the publisher phase
	 * @param phase publisher phase
	 */
	public void setPhase(PublisherPhase phase) {
		this.phase = phase;
	}

	/**
	 * Set the number of objects to publish for the given node
	 * @param nodeId node ID
	 * @param pagesToPublish pages to publish
	 * @param filesToPublish files to publish
	 * @param foldersToPublish folders to publish
	 * @param formsToPublish forms to publish
	 */
	public synchronized void setObjectsToPublish(int nodeId, int pagesToPublish, int filesToPublish, int foldersToPublish, int formsToPublish) {
		this.pagesToPublish.computeIfAbsent(nodeId, id -> new AtomicInteger()).set(pagesToPublish);
		this.filesToPublish.computeIfAbsent(nodeId, id -> new AtomicInteger()).set(filesToPublish);
		this.foldersToPublish.computeIfAbsent(nodeId, id -> new AtomicInteger()).set(foldersToPublish);
		this.formsToPublish.computeIfAbsent(nodeId, id -> new AtomicInteger()).set(formsToPublish);
	}

	/**
	 * Increase the number of published pages by one
	 * @param nodeId ID of the node, for which the page was published
	 */
	public synchronized void publishedPage(int nodeId) {
		publishedPages.computeIfAbsent(nodeId, id -> new AtomicInteger()).incrementAndGet();
	}

	/**
	 * Increase the number of published files by one
	 * @param nodeId ID of the node, for which the file was published
	 */
	public synchronized void publishedFile(int nodeId) {
		publishedFiles.computeIfAbsent(nodeId, id -> new AtomicInteger()).incrementAndGet();
	}

	/**
	 * Set the number of published files to the number of files to publish
	 */
	public synchronized void publishedAllFiles() {
		Set<Integer> ids = new HashSet<>();
		ids.addAll(publishedFiles.keySet());
		ids.addAll(filesToPublish.keySet());
		for (Integer nodeId : ids) {
			publishedFiles.computeIfAbsent(nodeId, id -> new AtomicInteger()).set(filesToPublish.getOrDefault(nodeId, new AtomicInteger()).get());
		}
	}

	/**
	 * Increase the number of published folders by one
	 * @param nodeId ID of the node, for which the folder was published
	 */
	public synchronized void publishedFolder(int nodeId) {
		publishedFolders.computeIfAbsent(nodeId, id -> new AtomicInteger()).incrementAndGet();
	}

	/**
	 * Set the number of published folders to the number of files to publish
	 */
	public synchronized void publishedAllFolders() {
		Set<Integer> ids = new HashSet<>();
		ids.addAll(publishedFolders.keySet());
		ids.addAll(foldersToPublish.keySet());
		for (Integer nodeId : ids) {
			publishedFolders.computeIfAbsent(nodeId, id -> new AtomicInteger()).set(foldersToPublish.getOrDefault(nodeId, new AtomicInteger()).get());
		}
	}

	/**
	 * Increase the number of published forms by one
	 * @param nodeId ID of the node, for which the folder was published
	 */
	public synchronized void publishedForm(int nodeId) {
		publishedForms.computeIfAbsent(nodeId, id -> new AtomicInteger()).incrementAndGet();
	}

	/**
	 * Set the number of published forms to the number of files to publish
	 */
	public synchronized void publishedAllForms() {
		Set<Integer> ids = new HashSet<>();
		ids.addAll(publishedForms.keySet());
		ids.addAll(formsToPublish.keySet());
		for (Integer nodeId : ids) {
			publishedForms.computeIfAbsent(nodeId, id -> new AtomicInteger()).set(formsToPublish.getOrDefault(nodeId, new AtomicInteger()).get());
		}
	}
}
