package com.gentics.contentnode.job;

import java.util.List;
import java.util.Map;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.i18n.I18nString;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionException;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.object.UpdatePagesResult;
import com.gentics.contentnode.msg.NodeMessage;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.rest.model.Tag;
import com.gentics.contentnode.rest.model.Template;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.Message.Type;
import com.gentics.contentnode.rest.util.MiscUtils;
import com.gentics.contentnode.rest.util.ModelBuilder;
import com.gentics.lib.i18n.CNI18nString;

/**
 * Background job that saves a template.
 * Saving a template might need to update lots of pages, which could take a while
 */
public class TemplateSaveJob extends AbstractBackgroundJob {
	/**
	 * List of tags to delete
	 */
	protected List<String> tagNamesToDelete;

	/**
	 * Template model to save
	 */
	protected Template restModel;

	/**
	 * True to unlock the template after saving
	 */
	protected boolean unlock;

	/**
	 * True to synchronize pages
	 */
	protected boolean syncPages;

	/**
	 * List of tags to sync
	 */
	protected List<String> sync;

	/**
	 * True to force sync of incompatible tags
	 */
	protected boolean forceSync;

	/**
	 * Default constructor, used by quartz
	 */
	public TemplateSaveJob() {
	}

	/**
	 * Create an instance and add the given paramters
	 * @param restModel rest model of the template
	 * @param tagNamesToDelete list of tag names to delete
	 * @param unlock true to unlock the template
	 * @param syncPages true to synchronize pages
	 * @param sync list of tag names to synchronize
	 * @param force true for force sync of incompatible tags
	 * @throws TransactionException
	 */
	public TemplateSaveJob(Template restModel, List<String> tagNamesToDelete, boolean unlock, boolean syncPages, List<String> sync, boolean force)
			throws TransactionException {
		this.restModel = restModel;
		this.tagNamesToDelete = tagNamesToDelete;
		this.unlock = unlock;
		this.syncPages = syncPages;
		this.sync = sync;
		this.forceSync = force;
	}

	/**
	 * Get the job description
	 * @return job description
	 */
	public String getJobDescription() {
		return new CNI18nString("templatesavejob").toString();
	}

	@Override
	protected void processAction() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		com.gentics.contentnode.object.Template template = ModelBuilder.getTemplate(restModel);

		// Object tag permission checking
		Map<String, Tag> restTags = restModel.getObjectTags();
		Map<String, ObjectTag> objectTags = template.getObjectTags();
		if (objectTags != null) {
			if (restTags != null) {
				MiscUtils.checkObjectTagEditPermissions(restTags, objectTags, true);
			}

			if (tagNamesToDelete != null) {
				// Since there is no permission bit for deletion for
				// object properties, we just check for the edit permission.
				MiscUtils.checkObjectTagEditPermissions(tagNamesToDelete, objectTags, false);
			}
		}

		// remove the tags, if requested to do so
		ModelBuilder.deleteTags(tagNamesToDelete, template);

		// save the template
		template.save(false);

		// unlock the template if requested
		if (unlock) {
			template.unlock();
		}

		t.commit(false);

		StringBuilder msgBuilder = new StringBuilder();
		msgBuilder.append(new CNI18nString("template.save.success").toString());
		// if requested, we now will synchronize the pages
		UpdatePagesResult result = null;
		if (syncPages) {
			result = template.updatePages(100, 5, sync, forceSync);
			I18nString message = new CNI18nString("pages.synchronized");
			message.setParameter("0", result.getNumPagesUpdated());
			msgBuilder.append("\n").append(message.toString());

			if (result.getNumErrors() != 0) {
				message = new CNI18nString("pages.synchronized.errors");
				message.setParameter("0", result.getNumErrors());
				msgBuilder.append("\n").append(message.toString());
			}
		}

		addMessage(new Message().setType(Type.SUCCESS).setMessage(msgBuilder.toString()));

		if (result != null) {
			for (NodeMessage message : result.getMessages()) {
				addMessage(MiscUtils.getMessageFromNodeMessage(message));
			}
		}
	}
}
