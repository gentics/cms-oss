/*
 * @author Stefan Hepp
 * @date 23.1.2005
 * @version $Id: PublishInfo.java,v 1.9 2010-09-28 17:01:31 norbert Exp $
 */
package com.gentics.contentnode.publish;

import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.distributed.DistributionUtil;
import com.gentics.contentnode.etc.BiFunction;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.jmx.PublisherInfo;
import com.gentics.contentnode.msg.NodeMessage;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Form;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.rest.model.response.admin.PublishInfoResponse;

/**
 * This is an interface for a container of publish process infos.
 * The publishinfo contains collected/summarized informations about the
 * currently running publish processes.
 */
public interface PublishInfo {
	public static final int RETURN_CODE_SUCCESS = 0;
	public static final int RETURN_CODE_ERROR = 1;

	/**
	 * Consumer that transforms the node model into the given rest model
	 */
	public final static BiFunction<PublishInfo, PublishInfoResponse, PublishInfoResponse> NODE2REST = (info, infoModel) -> {
		infoModel.setEstimatedDuration(info.getEstimatedDuration());
		Transaction t = TransactionManager.getCurrentTransaction();
		// get the publisher info from the master, since the publish process will always run on the master
		PublisherInfo publisherInfo;
		try {
			publisherInfo = DistributionUtil.call(new GetPublisherInfo());
		} catch (Exception e) {
			throw new NodeException(e);
		}

		// get all nodes, that have publishing enabled
		List<Node> publishedNodes = t.getObjects(Node.class, DBUtils.select("SELECT id FROM node WHERE disable_publish = ?", ps -> {
			ps.setInt(1, 0);
		}, DBUtils.IDS));

		// get IDs of all nodes, which publish files into a content repository
		int[] fileNodeIds = publishedNodes.stream().filter(Node::doPublishContentMapFiles).filter(n -> {
			try {
				return n.getContentRepository() != null;
			} catch (NodeException e) {
				return false;
			}
		}).mapToInt(Node::getId).toArray();

		// get IDs of all nodes, which publish folders into a content repository
		int[] folderNodeIds = publishedNodes.stream().filter(Node::doPublishContentMapFolders).filter(n -> {
			try {
				return n.getContentRepository() != null;
			} catch (NodeException e) {
				return false;
			}
		}).mapToInt(Node::getId).toArray();

		// get IDs of all nodes, which publish pages
		int[] pageNodeIds = publishedNodes.stream().mapToInt(Node::getId).toArray();

		PublishQueueStats stats = PublishQueueStats.get();
		infoModel.setFiles(stats.count(File.TYPE_FILE, publisherInfo::getPublishedFiles, publisherInfo::getRemainingFiles, fileNodeIds));
		infoModel.setFolders(stats.count(Folder.TYPE_FOLDER, publisherInfo::getPublishedFolders, publisherInfo::getRemainingFolders, folderNodeIds));
		infoModel.setPages(stats.count(Page.TYPE_PAGE, publisherInfo::getPublishedPages, publisherInfo::getRemainingPages, pageNodeIds));
		infoModel.setForms(stats.count(Form.TYPE_FORM, publisherInfo::getPublishedForms, publisherInfo::getRemainingForms, pageNodeIds));

		if (StringUtils.isBlank(info.getCurrentPhaseName())) {
			infoModel.setPhase(I18NHelper.get("waiting"));
		} else {
			infoModel.setPhase(I18NHelper.get(info.getCurrentPhaseName().toLowerCase()));
		}
		infoModel.setProgress(info.getProgess());
		infoModel.setFailed(info.getReturnCode() != RETURN_CODE_SUCCESS);
		infoModel.setRunning(PublishController.isRunning());
		infoModel.setTotalWork(info.getTotalWork());
		infoModel.setTotalWorkDone(info.getTotalDoneWork());
		return infoModel;
	};

	/**
	 * Lambda that transforms the node model into the rest model
	 */
	public final static Function<PublishInfo, PublishInfoResponse> TRANSFORM2REST = info -> {
		return NODE2REST.apply(info, new PublishInfoResponse());
	};

	/**
	 * get the progress in percent.
	 * @return the progress of the publish process in percent.
	 */
	int getProgess();

	/**
	 * get the estimated remaining duration in seconds.
	 * @return the estimated duration of the publish process in seconds.
	 */ 
	int getEstimatedDuration();
    
	/**
	 * Return the ETA for the current phase in seconds.
	 * @return eta in seconds for the current phase.
	 */
	int getEstimatedDurationForCurrentPhase();

	/**
	 * get a status message about the currently running process(es).
	 * @return a short status message.
	 */
	String getStatusMessage();
    
	/**
	 * Returns a list of {@link com.gentics.contentnode.msg.NodeMessage} objects which
	 * were generated during the publish run.
	 * @return list of node messages.
	 */
	Collection<NodeMessage> getMessages();

	/**
	 * The return code of a publish run.
	 * @return the return code.
	 * @see #RETURN_CODE_ERROR
	 * @see #RETURN_CODE_SUCCESS
	 */
	int getReturnCode();
    
	/**
	 * returns the approximate deviation for the {@link #getEstimatedDuration()} in percent.
	 * @return deviation in percent.
	 */
	int getDeviation();

	int getTotalWork();

	int getTotalDoneWork();

	int getRemainingPageCount();

	int getRemainingFileCount();

	int getRemainingFolderCount();

	int getRemainingFormCount();

	/**
	 * Returns the number of successfully published folders.
	 *
	 * @return the number of successfully published folders.
	 */
	int getPublishedFolderCount();

	/**
	 * Returns the number of successfully published pages.
	 *
	 * @return the number of successfully published pages.
	 */
	int getPublishedPageCount();

	/**
	 * Returns the number of successfully published files.
	 *
	 * @return the number of successfully published files.
	 */
	int getPublishedFileCount();

	/**
	 * Returns the number of successfully published forms.
	 *
	 * @return the number of successfully published forms.
	 */
	int getPublishedFormCount();

	/**
	 * returns the name of the current phase, or "" if none is running.
	 */
	String getCurrentPhaseName();
    
	/**
	 * return the number of the current phase. or -1
	 */
	int getCurrentPhaseNumber();

	/**
	 * returns the number of phases which are part of a publish run.
	 */
	int getPhaseCount();
    
	/**
	 * Returns a list of all phase names.
	 */
	String[] getAllPhaseNames();

	boolean isInitialized();
    
	float getCurrentCpuUsage();
    
	long getCurrentHeapMemoryUsage();

	long getCurrentThreadCount();

	float getLoadAverage();

	float getLoadLimit();
    
	public void setCurrentCpuUsage(float currentCpuUsage);

	public void setCurrentHeapMemoryUsage(long currentHeapMemoryUsage);
   
	public void setCurrentThreadCount(long currentThreadCount);
 
	public void setLoadAverage(float loadAverage);

	public void setLoadLimit(float loadLimit);

	/**
	 * @return the publishThreadInfos
	 */
	public List getPublishThreadInfos();

	/**
	 * @param publishThreadInfos the publishThreadInfos to set
	 */
	public void setPublishThreadInfos(List publishThreadInfos);
    
	public int getThreadLimit();
    
	public void setThreadLimit(int limit);
    
	public void addMessage(NodeMessage message);

}
