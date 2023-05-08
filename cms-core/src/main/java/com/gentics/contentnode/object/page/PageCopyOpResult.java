package com.gentics.contentnode.object.page;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Level;

import com.gentics.contentnode.msg.DefaultNodeMessage;
import com.gentics.contentnode.msg.NodeMessage;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.OpResult;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.OpResult.Status;
import com.gentics.lib.i18n.CNI18nString;

/**
 * Class that stores the internal state and information for the page copy
 * action.
 * 
 * @author johannes2
 * 
 */
public class PageCopyOpResult extends OpResult {

	protected List<PageCopyOpResultInfo> copyInfos = new ArrayList<PageCopyOpResultInfo>();

	/**
	 * Create an instance with status {@link Status#OK} and optional list of
	 * messages
	 * 
	 * @param messages
	 *            optional list of messages
	 */
	public PageCopyOpResult(NodeMessage... messages) {
		super(Status.OK, messages);
	}

	/**
	 * Create an instance with given status and optional list of messages
	 * 
	 * @param status
	 *            status
	 * @param messages
	 *            optional list of messages
	 */
	public PageCopyOpResult(Status status, NodeMessage... messages) {
		super(status, messages);
	}

	/**
	 * Return the list of copy result infos. Each resultinfo contains the
	 * information which page was copied to which target folder for which
	 * channel.
	 * 
	 * @return
	 */
	public List<PageCopyOpResultInfo> getCopyInfos() {
		return copyInfos;
	}

	/**
	 * Return an instance of {@link PageCopyOpResult} with status
	 * {@link Status#FAILURE} and a single message
	 * 
	 * @param msgKey
	 *            message key
	 * @param msgParameters
	 *            optional message parameters
	 * @return PageCopyOpResult instance
	 */
	public static PageCopyOpResult fail(String msgKey, String... msgParameters) {
		CNI18nString message = new CNI18nString(msgKey);
		for (String msgParam : msgParameters) {
			message.addParameter(msgParam);
		}
		return new PageCopyOpResult(Status.FAILURE, new DefaultNodeMessage(Level.ERROR, Page.class, message.toString()));
	}

	/**
	 * Merges the given result into this one. This merge call will not merge the
	 * op result status.
	 * 
	 * @param result
	 *            Result that should be merged
	 */
	public void mergeData(PageCopyOpResult result) {
		getCopyInfos().addAll(result.getCopyInfos());
	}

	/**
	 * Adds info to the result that contains info where and from what the copy
	 * was created.
	 * 
	 * @param sourcePage
	 *            Page that was used in order to create the copy.
	 * @param pageCopy
	 *            Page that was created by the copy call.
	 * @param targetFolder
	 *            Folder in which the page was copied.
	 * @param targetChannelId
	 *            Channel in which the page was copied.
	 */
	public PageCopyOpResultInfo addPageCopyInfo(Page sourcePage, Page pageCopy, Folder targetFolder, Integer targetChannelId) {
		PageCopyOpResultInfo resultInfo = new PageCopyOpResultInfo(sourcePage, pageCopy, targetFolder, targetChannelId);
		this.copyInfos.add(resultInfo);
		return resultInfo;
	}
}
