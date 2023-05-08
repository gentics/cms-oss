package com.gentics.contentnode.job;

import com.gentics.api.lib.i18n.I18nString;
import com.gentics.contentnode.factory.InstantPublishingTrx;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;
import com.gentics.lib.i18n.CNI18nString;

public class LocalizeTemplateCallable extends AbstractLocalizeCallable {
	/**
	 * Template ID
	 */
	protected int templateId;

	/**
	 * Create an instance for the given template and channel
	 * 
	 * @param templateId template ID
	 * @param channelId channel ID
	 * @param disableInstantPublish true if instant publishing shall be disabled
	 */
	public LocalizeTemplateCallable(int templateId, int channelId, boolean disableInstantPublish) {
		super(channelId, disableInstantPublish);
		this.templateId = templateId;
	}

	@Override
	public GenericResponse call() throws Exception {
		try (InstantPublishingTrx ip = new InstantPublishingTrx(!disableInstantPublish)) {
			Transaction t = TransactionManager.getCurrentTransaction();
			Template template = t.getObject(Template.class, templateId);
			if (template == null) {
				I18nString message = new CNI18nString("template.notfound");
				message.setParameter("0", String.valueOf(templateId));
				throw new EntityNotFoundException(message.toString());
			}

			Node channel = template.getChannel();

			if (channel != null && channel.getId().equals(channelId)) {
				return new GenericResponse();
			}

			Integer channelSetId = template.getChannelSetId();

			// create a copy of the template
			Template localCopy = (Template) template.copy();

			// set the channel information (master objects and their local copies are grouped together with their common channelset id)
			localCopy.setChannelInfo(channelId, channelSetId);

			// save and unlock the local copy
			localCopy.save();
			localCopy.unlock();

			I18nString message = new CNI18nString("template.localize.success");
			return new GenericResponse(null, new ResponseInfo(ResponseCode.OK, message.toString()));
		}
	}
}
