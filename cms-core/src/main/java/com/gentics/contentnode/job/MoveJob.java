package com.gentics.contentnode.job;

import java.util.Collection;
import java.util.HashSet;
import java.util.StringTokenizer;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.msg.NodeMessage;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.OpResult;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.i18n.CNI18nString;

/**
 * Background job that moves objects
 */
public class MoveJob extends AbstractBackgroundJob {
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
	 * Create an instance with data
	 * @param clazz class of objects to move
	 * @param ids comma separated id list of objects to move
	 * @param targetId target folder id
	 * @param targetChannelId target channel id
	 */
	public MoveJob(Class<? extends NodeObject> clazz, String ids, int targetId, int targetChannelId) {
		this.clazz = clazz;
		String idList = ids;
		this.ids = new HashSet<Integer>();
		StringTokenizer tokenizer = new StringTokenizer(idList, ",");

		while (tokenizer.hasMoreTokens()) {
			String id = tokenizer.nextToken();

			try {
				this.ids.add(Integer.parseInt(id));
			} catch (NumberFormatException e) {
			}
		}

		this.targetId = targetId;
		this.targetChannelId = targetChannelId;
	}

	/**
	 * Create an instance with data
	 * @param clazz class of objects to move
	 * @param ids list of IDs of objects to move
	 * @param targetId target folder id
	 * @param targetChannelId target channel id
	 */
	public MoveJob(Class<? extends NodeObject> clazz, Collection<Integer> ids, int targetId, int targetChannelId) {
		this(clazz, StringUtils.merge((Integer[]) ids.toArray(new Integer[ids.size()]), ","), targetId, targetChannelId);
	}

	@Override
	public String getJobDescription() {
		return new CNI18nString("movejob").toString();
	}

	@Override
	protected void processAction() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
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
					success = false;
					for (NodeMessage msg : result.getMessages()) {
						addMessage(msg);
					}
					t.rollback(false);
					break;
				}
			}
			if (t.isInterrupted()) {
				if (t.isOpen()) {
					t.rollback();
				}
				throw new NodeException();
			}
		}

		// Restores temporarily disabled instant publishing
		t.setInstantPublishingEnabled(instantPublishingEnabled);
	}
}
