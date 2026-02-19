package com.gentics.contentnode.job;

import com.gentics.api.lib.i18n.I18nString;
import com.gentics.contentnode.factory.InstantPublishingTrx;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.Message.Type;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;
import com.gentics.lib.i18n.CNI18nString;

/**
 * Callable implementation for localizing a file
 */
public class LocalizeFileCallable extends AbstractLocalizeCallable {
	/**
	 * File ID
	 */
	protected int fileId;

	/**
	 * Create an instance for the given file and channel
	 * 
	 * @param fileId file ID
	 * @param channelId channel ID
	 * @param disableInstantPublish true if instant publishing shall be disabled
	 */
	public LocalizeFileCallable(int fileId, int channelId, boolean disableInstantPublish) {
		super(channelId, disableInstantPublish);
		this.fileId = fileId;
	}

	@Override
	public GenericResponse call() throws Exception {
		try (InstantPublishingTrx ip = new InstantPublishingTrx(!disableInstantPublish)) {
			Transaction t = TransactionManager.getCurrentTransaction();
			File file = t.getObject(File.class, fileId);
			if (file == null) {
				I18nString message = new CNI18nString("file.notfound");
				message.setParameter("0", String.valueOf(fileId));
				throw new EntityNotFoundException(message.toString());
			}

			Node channel = file.getChannel();

			if (channel != null && channel.getId().equals(channelId)) {
				return new GenericResponse();
			}

			Integer channelSetId = file.getChannelSetId();
			if (checkConflictingObject(File.class, channelSetId)) {
				return new GenericResponse();
			}

			// create a copy of the object
			File localCopy = (File) file.copy();

			// set the channel information (master objects and their local copies are grouped together with their common channelset id)
			localCopy.setChannelInfo(channelId, channelSetId);

			// save and unlock the local copy
			localCopy.save();

			I18nString message = new CNI18nString("file.localize.success");
			String translated = message.toString();
			return new GenericResponse(new Message(Type.SUCCESS, translated), new ResponseInfo(ResponseCode.OK, translated));
		}
	}
}
