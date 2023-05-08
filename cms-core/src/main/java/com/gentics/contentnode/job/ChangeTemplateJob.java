package com.gentics.contentnode.job;

import java.util.Collection;

import org.quartz.JobDataMap;
import org.quartz.JobExecutionException;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Page;
import com.gentics.lib.i18n.CNI18nString;

/**
 * A job that changes the template for a list of pages
 */
public class ChangeTemplateJob extends AbstractUserActionJob {

	/**
	 * Parameter that specifies the pages for which the template shall be changed
	 */
	public static final String PARAM_IDS = "ids";

	/**
	 * Parameter that specifies whether tags shall be sync'ed
	 */
	public static final String PARAM_SYNCTAGS = "synctags";

	/**
	 * Parameter that specifies the new template id
	 */
	public static final String PARAM_TEMPLATE_ID = "templateid";

	/**
	 * list of page ids
	 */
	protected Collection<Integer> pageIds;

	/**
	 * Template id
	 */
	protected Integer templateId;

	/**
	 * True whether tags shall be synchronized, false if not
	 */
	protected boolean syncTags;

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.job.AbstractUserActionJob#getJobDescription()
	 */
	@Override
	public String getJobDescription() {
		return new CNI18nString("backgroundjob_changetemplate").toString();
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.job.AbstractUserActionJob#processAction()
	 */
	@Override
	protected void processAction() throws InsufficientPrivilegesException,
				NodeException, JobExecutionException {
		// iterate the pages
		for (Integer pageId : pageIds) {
			// check whether job was interrupted
			checkForInterruption();

			// get the page (editable). This will lock the page
			Page page = t.getObject(Page.class, pageId, true);
			boolean onlineAndNotModified = page.isOnline() && !page.isModified() && page.getTimePub().getIntTimestamp() == 0;

			// set the template id
			page.setTemplateId(templateId, syncTags);

			// save the page
			page.save();

			// unlock the page
			page.unlock();

			// if the page was published, it will be republished now
			if (onlineAndNotModified) {
				page.publish();
			}

			// Page variant will cause deadlock while locking the content again
			t.commit(false);
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.job.AbstractUserActionJob#getJobParameters(org.quartz.JobDataMap)
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected boolean getJobParameters(JobDataMap map) {
		pageIds = ObjectTransformer.getCollection(map.get(PARAM_IDS), null);
		templateId = ObjectTransformer.getInteger(map.get(PARAM_TEMPLATE_ID), null);
		syncTags = ObjectTransformer.getBoolean(map.get(PARAM_SYNCTAGS), false);

		return pageIds != null && templateId != null;
	}
}
