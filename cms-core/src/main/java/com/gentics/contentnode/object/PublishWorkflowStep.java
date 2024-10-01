/*
 * @author norbert
 * @date 14.03.2011
 * @version $Id: PublishWorkflowStep.java,v 1.1.2.1 2011-03-14 15:12:26 norbert Exp $
 */
package com.gentics.contentnode.object;

import java.util.List;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.contentnode.factory.TType;

/**
 * Class representing steps in a page publish workflow
 */
@TType(PublishWorkflowStep.TYPE_PUBLISHWORKFLOW_STEP)
public abstract class PublishWorkflowStep extends AbstractContentObject implements MetaDateNodeObject {

	/**
	 * The ttype of the publish workflow step object as defined in system/include/public.inc.php
	 */
	public static final int TYPE_PUBLISHWORKFLOW_STEP = 151;

	/**
	 * Create an instance of the publish workflow step
	 * @param id id of the publish workflow
	 * @param info object info
	 */
	protected PublishWorkflowStep(Integer id, NodeObjectInfo info) {
		super(id, info);
	}

	/**
	 * Get the workflow
	 * @return workflow
	 * @throws NodeException
	 */
	public abstract PublishWorkflow getWorkflow() throws NodeException;

	/**
	 * Set the workflow id
	 * @param workflowId workflow id
	 * @throws ReadOnlyException
	 */
	public void setWorkflowId(int workflowId) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * Get the message
	 * @return message
	 */
	public abstract String getMessage();

	/**
	 * Set the message
	 * @param message message
	 * @throws ReadOnlyException
	 */
	public void setMessage(String message) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * Get the sortorder
	 * @return sortorder
	 */
	public abstract int getSortorder();

	/**
	 * Set a new sortorder
	 * @param sortOrder sortorder
	 * @throws ReadOnlyException
	 */
	public void setSortOrder(int sortOrder) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * Get true when the page has been modified while in this step
	 * @return true if the page has been modified, false if not
	 */
	public abstract boolean isPageModified();

	/**
	 * Set whether the page is modified while in this step
	 * @param modified true when the page is modified, false if not
	 * @return true if the page was modified before, false if not
	 * @throws ReadOnlyException
	 */
	public boolean setPageModified(boolean modified) throws ReadOnlyException {
		failReadOnly();
		return false;
	}

	/**
	 * get the creator
	 * @return creator
	 * @throws NodeException
	 */
	public abstract SystemUser getCreator() throws NodeException;

	/**
	 * get the editor
	 * @return editor
	 * @throws NodeException
	 */
	public abstract SystemUser getEditor() throws NodeException;

	/**
	 * Get the list of usergroups assigned to this step
	 * @return list of usergroups for this step
	 * @throws NodeException
	 */
	public abstract List<UserGroup> getUserGroups() throws NodeException;
}
