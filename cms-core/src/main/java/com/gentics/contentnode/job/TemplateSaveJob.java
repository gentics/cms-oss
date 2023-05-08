package com.gentics.contentnode.job;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionException;

import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.i18n.I18nString;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionException;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.object.UpdatePagesResult;
import com.gentics.contentnode.msg.DefaultNodeMessage;
import com.gentics.contentnode.msg.NodeMessage;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.rest.model.Tag;
import com.gentics.contentnode.rest.model.Template;
import com.gentics.contentnode.rest.util.MiscUtils;
import com.gentics.contentnode.rest.util.ModelBuilder;
import com.gentics.lib.i18n.CNI18nString;

/**
 * Background job that saves a template.
 * Saving a template might need to update lots of pages, which could take a while
 */
public class TemplateSaveJob extends AbstractUserActionJob {
	/**
	 * Name of the job parameter holding the template model
	 */
	public final static String PARAM_MODEL = "templateModel";

	/**
	 * Name of the job parameter holding the list of tags to delete
	 */
	public final static String PARAM_TAGS = "tagsToDelete";

	/**
	 * Name of the job parameter whether to unlock the template after saving
	 */
	public final static String PARAM_UNLOCK = "unlock";

	/**
	 * Name of the job parameter whether pages shall be synchronized
	 */
	public final static String PARAM_SYNC_PAGES = "syncPages";

	/**
	 * Name of the job parameter holding the list of tags to sync
	 */
	public final static String PARAM_SYNC = "sync";

	/**
	 * Name of the job parameter to force sync of incompatible tags
	 */
	public final static String PARAM_FORCE_SYNC = "forceSync";

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
		Transaction t = TransactionManager.getCurrentTransaction();
		addParameter(AbstractUserActionJob.PARAM_SESSIONID, t.getSessionId());
		addParameter(AbstractUserActionJob.PARAM_USERID, new Integer(t.getUserId()));
		addParameter(PARAM_MODEL, restModel);
		if (tagNamesToDelete != null) {
			addParameter(PARAM_TAGS, new ArrayList<String>(tagNamesToDelete));
		}
		addParameter(PARAM_UNLOCK, unlock);
		addParameter(PARAM_SYNC_PAGES, syncPages);
		if (sync != null) {
			addParameter(PARAM_SYNC, new ArrayList<String>(sync));
		}
		addParameter(PARAM_FORCE_SYNC, force);
	}

	@Override
	public String getJobDescription() {
		return new CNI18nString("templatesavejob").toString();
	}

	@SuppressWarnings("unchecked")
	@Override
	protected boolean getJobParameters(JobDataMap map) {
		restModel = (Template)map.get(PARAM_MODEL);
		tagNamesToDelete = (List<String>)map.get(PARAM_TAGS);
		unlock = map.getBoolean(PARAM_UNLOCK);
		syncPages = map.getBoolean(PARAM_SYNC_PAGES);
		sync = (List<String>)map.get(PARAM_SYNC);
		forceSync = map.getBoolean(PARAM_FORCE_SYNC);

		return restModel != null;
	}

	@Override
	protected void processAction() throws InsufficientPrivilegesException, NodeException, JobExecutionException {
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

		addMessage(new DefaultNodeMessage(Level.INFO, Template.class, msgBuilder.toString()));

		if (result != null) {
			for (NodeMessage message : result.getMessages()) {
				addMessage(message);
			}
		}
	}
}
