package com.gentics.contentnode.job;

import com.gentics.api.lib.i18n.I18nString;
import com.gentics.contentnode.factory.InstantPublishingTrx;
import com.gentics.contentnode.factory.PublishCacheTrx;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;
import com.gentics.lib.i18n.CNI18nString;

/**
 * Callable implementation for localizing a page
 */
public class LocalizePageCallable extends AbstractLocalizeCallable {
	/**
	 * Page ID
	 */
	protected int pageId;

	/**
	 * Create an instance for the given page and channel
	 * 
	 * @param pageId page ID
	 * @param channelId channel ID
	 * @param disableInstantPublish true if instant publishing shall be disabled
	 */
	public LocalizePageCallable(int pageId, int channelId, boolean disableInstantPublish) {
		super(channelId, disableInstantPublish);
		this.pageId = pageId;
	}

	@Override
	public GenericResponse call() throws Exception {
		try (InstantPublishingTrx ip = new InstantPublishingTrx(!disableInstantPublish)) {
			Transaction t = TransactionManager.getCurrentTransaction();
			Page page = t.getObject(Page.class, pageId);
			if (page == null) {
				I18nString message = new CNI18nString("page.notfound");
				message.setParameter("0", String.valueOf(pageId));
				throw new EntityNotFoundException(message.toString());
			}

			Node channel = page.getChannel();

			if (channel != null && channel.getId().equals(channelId)) {
				return new GenericResponse(null, new ResponseInfo(ResponseCode.OK, "Page is master in the channel"));
			}

			Integer channelSetId = page.getChannelSetId();
			if (checkConflictingObject(Page.class, channelSetId)) {
				return new GenericResponse(null, new ResponseInfo(ResponseCode.OK, "Already localized"));
			}

			if (page.isOnline()) {
				// create a copy of the published version
				Page localCopy = null;
				try (PublishCacheTrx pcTrx = new PublishCacheTrx(false)) {
					localCopy = (Page) page.getPublishedObject().copy();
				}

				// set the channel information (master pages and their local copies are grouped together with their common channelset id)
				localCopy.setChannelInfo(channelId, channelSetId);

				// save and publish the local copy
				localCopy.save();
				localCopy.publish();

				// if the page is modified (different from the published version), update the local copy, too
				if (page.isModified()) {
					t.commit(false);
					long oldTimestamp = t.getTimestamp();
					try {
						t.setTimestamp(oldTimestamp + 1000L);
						localCopy.copyFrom(page);
						localCopy.save();
						t.commit(false);
					} finally {
						t.setTimestamp(oldTimestamp);
					}
				}

				// unlock the local copy
				localCopy.unlock();
			} else {
				// create a copy of the page
				Page localCopy = (Page) page.copy();

				// set the channel information (master pages and their local copies are grouped together with their common channelset id)
				localCopy.setChannelInfo(channelId, channelSetId);

				// save and unlock the local copy
				localCopy.save();
				localCopy.unlock();
			}

			I18nString message = new CNI18nString("page.localize.success");
			return new GenericResponse(null, new ResponseInfo(ResponseCode.OK, message.toString()));
		}
	}
}
