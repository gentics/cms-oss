/*
 * @author norbert
 * @date 15.12.2009
 * @version $Id: UnlinkTemplateJob.java,v 1.2 2010-01-29 15:18:46 norbert Exp $
 */
package com.gentics.contentnode.job;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.quartz.JobDataMap;
import org.quartz.JobExecutionException;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.msg.NodeMessage;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Template;
import com.gentics.lib.i18n.CNI18nString;

/**
 * @author norbert
 *
 */
public class UnlinkTemplateJob extends AbstractUserActionJob {

	/**
	 * Parameter that specifies the template id's to be unlinked
	 */
	public static final String PARAM_TEMPLATEID = "templateId";
    
	/**
	 * Parameter that specifies the folder id's from which the templates shall be unlinked
	 */
	public static final String PARAM_FOLDERID = "folderId";

	/**
	 * collection of template id's
	 */
	protected Collection<Integer> templateIds;

	/**
	 * collection of folder id's
	 */
	protected Collection<Integer> folderIds;

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.job.AbstractUserActionJob#getJobDescription()
	 */
	public String getJobDescription() {
		return new CNI18nString("unlinktemplatejob").toString();
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.job.AbstractUserActionJob#getJobParameters(org.quartz.JobDataMap)
	 */
	protected boolean getJobParameters(JobDataMap map) {
		templateIds = ObjectTransformer.getCollection(map.get(PARAM_TEMPLATEID), null);
		folderIds = ObjectTransformer.getCollection(map.get(PARAM_FOLDERID), null);

		return templateIds != null && folderIds != null;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.job.AbstractUserActionJob#processAction(java.lang.Integer, java.lang.String, org.quartz.JobDataMap)
	 */
	protected void processAction() throws InsufficientPrivilegesException, NodeException, JobExecutionException {
		// first check which templates are localized copies
		List<Integer> toUnlink = new Vector<Integer>();

		for (Integer templateId : templateIds) {
			Template template = t.getObject(Template.class, templateId);

			if (template != null && !template.isMaster()) {
				// localized copies will be deleted
				template.delete();
			} else {
				// master templates will be unlinked
				toUnlink.add(templateId);
			}
		}

		// now unlink the templates
		for (Integer folderId : folderIds) {
			Folder folder = t.getObject(Folder.class, folderId);

			if (folder != null) {
				for (Integer templateId : toUnlink) {
					folder.unlinkTemplate(templateId);
				}
			}
			checkForInterruption();
		}

		Collection<NodeMessage> msgs = t.getRenderResult().getMessages();

		for (NodeMessage msg : msgs) {
			addMessage(msg);
		}
	}
}
