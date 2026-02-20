package com.gentics.contentnode.job;

import com.gentics.api.lib.i18n.I18nString;
import com.gentics.contentnode.factory.InstantPublishingTrx;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.Message.Type;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;
import com.gentics.lib.i18n.CNI18nString;

/**
 * Callable implementation for localizing an image
 */
public class LocalizeImageCallable extends AbstractLocalizeCallable {
	/**
	 * Image ID
	 */
	protected int imageId;

	/**
	 * Create an instance for the given image and channel
	 * 
	 * @param imageId image ID
	 * @param channelId channel ID
	 * @param disableInstantPublish true if instant publishing shall be disabled
	 */
	public LocalizeImageCallable(int imageId, int channelId, boolean disableInstantPublish) {
		super(channelId, disableInstantPublish);
		this.imageId = imageId;
	}

	@Override
	public GenericResponse call() throws Exception {
		try (InstantPublishingTrx ip = new InstantPublishingTrx(!disableInstantPublish)) {
			Transaction t = TransactionManager.getCurrentTransaction();
			ImageFile image = t.getObject(ImageFile.class, imageId);
			if (image == null) {
				I18nString message = new CNI18nString("image.notfound");
				message.setParameter("0", String.valueOf(imageId));
				throw new EntityNotFoundException(message.toString());
			}

			Node channel = image.getChannel();

			if (channel != null && channel.getId().equals(channelId)) {
				return new GenericResponse();
			}

			Integer channelSetId = image.getChannelSetId();
			if (checkConflictingObject(ImageFile.class, channelSetId)) {
				return new GenericResponse();
			}

			// create a copy of the object
			ImageFile localCopy = (ImageFile) image.copy();

			// set the channel information (master objects and their local copies are grouped together with their common channelset id)
			localCopy.setChannelInfo(channelId, channelSetId);

			// save and unlock the local copy
			localCopy.save();

			I18nString message = new CNI18nString("image.localize.success");
			String translated = message.toString();
			return new GenericResponse(new Message(Type.SUCCESS, translated), new ResponseInfo(ResponseCode.OK, translated));
		}
	}
}
