package com.gentics.contentnode.job;

import com.gentics.api.lib.i18n.I18nString;
import com.gentics.contentnode.factory.InstantPublishingTrx;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.Message.Type;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;
import com.gentics.lib.i18n.CNI18nString;

/**
 * Callable implementation for localizing a folder
 */
public class LocalizeFolderCallable extends AbstractLocalizeCallable {
	/**
	 * Folder ID
	 */
	protected int folderId;

	/**
	 * Create an instance for the given folder and channel
	 * 
	 * @param folderId folder ID
	 * @param channelId channel ID
	 * @param disableInstantPublish true if instant publishing shall be disabled
	 */
	public LocalizeFolderCallable(int folderId, int channelId, boolean disableInstantPublish) {
		super(channelId, disableInstantPublish);
		this.folderId = folderId;
	}

	@Override
	public GenericResponse call() throws Exception {
		try (InstantPublishingTrx ip = new InstantPublishingTrx(!disableInstantPublish)) {
			Transaction t = TransactionManager.getCurrentTransaction();
			Folder folder = t.getObject(Folder.class, folderId);
			if (folder == null) {
				I18nString message = new CNI18nString("folder.notfound");
				message.setParameter("0", String.valueOf(folderId));
				throw new EntityNotFoundException(message.toString());
			}

			Node channel = folder.getChannel();

			if (channel != null && channel.getId().equals(channelId)) {
				return new GenericResponse();
			}

			Integer channelSetId = folder.getChannelSetId();
			if (checkConflictingObject(Folder.class, channelSetId)) {
				return new GenericResponse();
			}

			// create a copy of the folder
			Folder localCopy = (Folder) folder.copy();

			// set the channel information (master objects and their local copies are grouped together with their common channelset id)
			localCopy.setChannelInfo(channelId, channelSetId);

			// save and unlock the local copy
			localCopy.save();

			I18nString message = new CNI18nString("folder.localize.success");
			String translated = message.toString();
			return new GenericResponse(new Message(Type.SUCCESS, translated), new ResponseInfo(ResponseCode.OK, translated));
		}
	}
}
