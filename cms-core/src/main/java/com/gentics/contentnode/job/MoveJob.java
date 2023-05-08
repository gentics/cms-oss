package com.gentics.contentnode.job;

import java.util.Collection;
import java.util.HashSet;
import java.util.StringTokenizer;

import org.quartz.JobDataMap;
import org.quartz.JobExecutionException;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.msg.NodeMessage;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.OpResult;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.i18n.CNI18nString;

/**
 * Background job that moves objects
 */
public class MoveJob extends AbstractUserActionJob {
	/**
	 * Parameter that specifies the objects to move
	 */
	public static final String PARAM_IDS = "ids";
    
	/**
	 * Parameter that specifies the objecttype that should be moved
	 */
	public static final String PARAM_CLASS = "type";

	/**
	 * Parameter that specifies the target id
	 */
	public static final String PARAM_TARGET_ID = "targetId";

	/**
	 * Parameter that specifies the target channel id
	 */
	public static final String PARAM_TARGET_CHANNEL_ID = "targetChannelId";

	/**
	 * class of objects to be deleted
	 */
	protected Class<? extends NodeObject> clazz;

	/**
	 * collection of object id's to be deleted
	 */
	protected Collection<Integer> ids;

	/**
	 * Target folder ID
	 */
	protected int targetId;

	/**
	 * Target channel ID
	 */
	protected int targetChannelId;

	/**
	 * Create an instance
	 */
	public MoveJob() {
	}

	/**
	 * Create an instance with data
	 * @param sessionId session id
	 * @param userId user id
	 * @param clazz class of objects to move
	 * @param ids comma separated id list of objects to move
	 * @param targetId target folder id
	 * @param targetChannelId target channel id
	 */
	public MoveJob(String sessionId, int userId, Class<? extends NodeObject> clazz, String ids, int targetId, int targetChannelId) {
		addParameter(AbstractUserActionJob.PARAM_SESSIONID, sessionId);
		addParameter(AbstractUserActionJob.PARAM_USERID,userId);
		addParameter(PARAM_CLASS, clazz);
		addParameter(PARAM_IDS, ids);
		addParameter(PARAM_TARGET_ID, targetId);
		addParameter(PARAM_TARGET_CHANNEL_ID, targetChannelId);
	}

	/**
	 * Create an instance with data
	 * @param sessionId sesison id
	 * @param userId user id
	 * @param clazz class of objects to move
	 * @param ids list of IDs of objects to move
	 * @param targetId target folder id
	 * @param targetChannelId target channel id
	 */
	public MoveJob(String sessionId, int userId, Class<? extends NodeObject> clazz, Collection<Integer> ids, int targetId, int targetChannelId) {
		this(sessionId, userId, clazz, StringUtils.merge((Integer[]) ids.toArray(new Integer[ids.size()]), ","), targetId, targetChannelId);
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.job.AbstractUserActionJob#getJobParameters(org.quartz.JobDataMap)
	 */
	@SuppressWarnings("unchecked")
	protected boolean getJobParameters(JobDataMap map) {
		clazz = (Class<? extends NodeObject>) map.get(PARAM_CLASS);
		String idList = ObjectTransformer.getString(map.get(PARAM_IDS), "");
		targetId = ObjectTransformer.getInt(map.get(PARAM_TARGET_ID), -1);
		targetChannelId = ObjectTransformer.getInt(map.get(PARAM_TARGET_CHANNEL_ID), -1);

		ids = new HashSet<Integer>();
		StringTokenizer tokenizer = new StringTokenizer(idList, ",");

		while (tokenizer.hasMoreTokens()) {
			String id = tokenizer.nextToken();

			try {
				ids.add(new Integer(Integer.parseInt(id)));
			} catch (NumberFormatException e) {
			}
		}

		return clazz != null && ids != null && targetId > 0 && targetChannelId >= 0;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.job.AbstractUserActionJob#getJobDescription()
	 */
	@Override
	public String getJobDescription() {
		return new CNI18nString("movejob").toString();
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.job.AbstractUserActionJob#processAction()
	 */
	@Override
	protected void processAction() throws InsufficientPrivilegesException, NodeException, JobExecutionException {
		// Stores current state of instant publishing, so that it can be re-enabled if it needs to be disabled
		boolean instantPublishingEnabled = t.isInstantPublishingEnabled();

		// get the target folder
		Folder target = t.getObject(Folder.class, targetId);

		// Disable instant publishing when moving multiple objects or when moving folders, regardless of the number of folders being moved
		if (ids.size() > 1) {
			t.setInstantPublishingEnabled(false);
		} else {
			// Check if single object is a folder
			Integer id = ids.toArray(new Integer[ids.size()])[0];
			NodeObject nodeObject = t.getObject(clazz, id);

			if (nodeObject != null && nodeObject instanceof Folder) {
				t.setInstantPublishingEnabled(false);
			}
		}

		boolean success = true;
		// Move the objects
		for (Integer id : ids) {
			NodeObject nodeObject = t.getObject(clazz, id, true);

			if (nodeObject instanceof Folder) {
				OpResult result = ((Folder) nodeObject).move(target, targetChannelId);
				switch (result.getStatus()) {
				case OK:
					t.commit(false);
					break;
				case FAILURE:
					for (NodeMessage msg : result.getMessages()) {
						addMessage(msg);
					}
					success = false;
					t.rollback(false);
					break;
				}
			}
			checkForInterruption();
		}

		// Restores temporarily disabled instant publishing
		t.setInstantPublishingEnabled(instantPublishingEnabled);

		if (!success) {
			jobResult = RESULT_FAILURE;
		}
	}
}
