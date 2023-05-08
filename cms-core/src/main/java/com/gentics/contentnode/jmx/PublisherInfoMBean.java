package com.gentics.contentnode.jmx;

/**
 * Management Bean interface for publisher information
 */
public interface PublisherInfoMBean {

	/**
	 * Check whether publish process is currently running
	 * @return true if the publish process is running, false if not
	 */
	boolean isRunning();

	/**
	 * Check whether the publish process is multithreaded
	 * @return true for multithreaded publishing
	 */
	boolean isMultiThreaded();

	/**
	 * Get the current publisher state
	 * @return current publisher state
	 */
	String getState();

	/**
	 * Get the total number of pages to publish in the current publish run
	 * @return total number of pages to publish
	 */
	int getPagesToPublish();

	/**
	 * Get the number of pages already published in the current publish run
	 * @return number of published pages
	 */
	int getPublishedPages();

	/**
	 * Get the number of remaining pages in the current publish run
	 * @return number of remaining pages
	 */
	int getRemainingPages();

	/**
	 * Get the number of pages to publish in the current publish run for the node
	 * @param nodeId node ID
	 * @return number of pages to publish
	 */
	int getPagesToPublish(int nodeId);

	/**
	 * Get the number of pages already published in the current publish run for the node
	 * @param nodeId node ID
	 * @return number of published pages
	 */
	int getPublishedPages(int nodeId);

	/**
	 * Get the number of remaining pages in the current publish run for the node
	 * @param nodeId node ID
	 * @return number of remaining pages
	 */
	int getRemainingPages(int nodeId);

	/**
	 * Get the total number of files to publish in the current publish run
	 * @return total number of files to publish
	 */
	int getFilesToPublish();

	/**
	 * Get the number of files already published in the current publish run
	 * @return number of published files
	 */
	int getPublishedFiles();

	/**
	 * Get the number of remaining files in the current publish run
	 * @return number of remaining files
	 */
	int getRemainingFiles();

	/**
	 * Get the number of files to publish in the current publish run for the node
	 * @param nodeId node ID
	 * @return number of files to publish
	 */
	int getFilesToPublish(int nodeId);

	/**
	 * Get the number of files already published in the current publish run for the node
	 * @param nodeId node ID
	 * @return number of published files
	 */
	int getPublishedFiles(int nodeId);

	/**
	 * Get the number of remaining files in the current publish run for the node
	 * @param nodeId node ID
	 * @return number of remaining files
	 */
	int getRemainingFiles(int nodeId);

	/**
	 * Get the total number of folders to publish in the current publish run
	 * @return total number of folders to publish
	 */
	int getFoldersToPublish();

	/**
	 * Get the number of folders already published in the current publish run
	 * @return number of published folders
	 */
	int getPublishedFolders();

	/**
	 * Get the number of remaining folders in the current publish run
	 * @return number of remaining folders
	 */
	int getRemainingFolders();

	/**
	 * Get the number of folders to publish in the current publish run for the node
	 * @param nodeId node ID
	 * @return number of folders to publish
	 */
	int getFoldersToPublish(int nodeId);

	/**
	 * Get the number of folders already published in the current publish run for the node
	 * @param nodeId node ID
	 * @return number of published folders
	 */
	int getPublishedFolders(int nodeId);

	/**
	 * Get the number of remaining folders in the current publish run for the node
	 * @param nodeId node ID
	 * @return number of remaining folders
	 */
	int getRemainingFolders(int nodeId);

	/**
	 * Get the total number of forms to publish in the current publish run
	 * @return total number of forms to publish
	 */
	int getFormsToPublish();

	/**
	 * Get the number of forms already published in the current publish run
	 * @return number of published forms
	 */
	int getPublishedForms();

	/**
	 * Get the number of remaining forms in the current publish run
	 * @return number of remaining forms
	 */
	int getRemainingForms();

	/**
	 * Get the number of forms to publish in the current publish run for the node
	 * @param nodeId node ID
	 * @return number of forms to publish
	 */
	int getFormsToPublish(int nodeId);

	/**
	 * Get the number of forms already published in the current publish run for the node
	 * @param nodeId node ID
	 * @return number of published forms
	 */
	int getPublishedForms(int nodeId);

	/**
	 * Get the number of remaining forms in the current publish run for the node
	 * @param nodeId node ID
	 * @return number of remaining forms
	 */
	int getRemainingForms(int nodeId);
}
