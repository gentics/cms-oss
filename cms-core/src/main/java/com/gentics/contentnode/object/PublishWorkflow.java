/*
 * @author norbert
 * @date 14.03.2011
 * @version $Id: PublishWorkflow.java,v 1.1.2.5 2011-03-17 13:38:55 norbert Exp $
 */
package com.gentics.contentnode.object;

import java.util.List;

import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.contentnode.etc.ContentNodeDate;
import com.gentics.contentnode.factory.TType;

/**
 * Class implementing Publish Workflows for pages
 */
@TType(PublishWorkflow.TYPE_PUBLISHWORKFLOW)
public abstract class PublishWorkflow extends AbstractContentObject {

	/**
	 * The ttype of the publish workflow object as defined in system/include/public.inc.php
	 */
	public static final int TYPE_PUBLISHWORKFLOW = 150;

	/**
	 * Create an instance of a page workflow
	 * @param id id of the workflow
	 * @param info object info
	 */
	protected PublishWorkflow(Integer id, NodeObjectInfo info) {
		super(id, info);
	}

	/**
	 * Get the page of the publish workflow
	 * @return page of the publish workflow
	 * @throws NodeException
	 */
	public abstract Page getPage() throws NodeException;

	/**
	 * Set the page id
	 * @param pageId page id
	 * @throws ReadOnlyException
	 */
	public void setPageId(Integer pageId) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * Get the current publish workflow step
	 * @return current publish workflow step
	 * @throws NodeException
	 */
	public abstract PublishWorkflowStep getCurrentStep() throws NodeException;

	/**
	 * Get the list of publish workflow steps
	 * @return list of publish workflow steps
	 * @throws NodeException
	 */
	public abstract List<PublishWorkflowStep> getSteps() throws NodeException;

	/**
	 * Add a new publish workflow step (at the end of the list). If groups are
	 * given, the workflow step will be assigned to exactly that groups,
	 * otherwise the super groups of the user are used.
	 * @param message step message
	 * @param groups optional list of groups to which the step will be assigned
	 * @throws NodeException
	 */
	public void addStep(String message, UserGroup... groups) throws NodeException {
		failReadOnly();
	}

	/**
	 * Revoke the last step
	 * @throws NodeException
	 */
	public void revokeStep() throws NodeException {
		failReadOnly();
	}

	/**
	 * get the creator
	 * @return creator
	 * @throws NodeException
	 */
	public abstract SystemUser getCreator() throws NodeException;

	/**
	 * get the creation date
	 * @return creation date
	 */
	public abstract ContentNodeDate getCDate();

	/**
	 * get the editor
	 * @return editor
	 * @throws NodeException
	 */
	public abstract SystemUser getEditor() throws NodeException;

	/**
	 * get the last edit date
	 * @return last edit date
	 */
	public abstract ContentNodeDate getEDate();

	@Override
	public void delete(boolean force) throws InsufficientPrivilegesException, NodeException {
		performDelete();
	}

	/**
	 * Check whether this workflow allows the current user to edit the page
	 * @return true if the current user is allowed to edit the page in the workflow, false if not
	 * @throws NodeException
	 */
	public abstract boolean allowsEditing() throws NodeException;

	/**
	 * Performs the delete of the Workflow
	 * @throws NodeException
	 */
	protected abstract void performDelete() throws NodeException;
}
