/*
 * @author norbert
 * @date 14.03.2011
 * @version $Id: PublishWorkflowFactory.java,v 1.1.2.8 2011-03-29 15:33:37 norbert Exp $
 */
package com.gentics.contentnode.factory.object;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.ContentNodeDate;
import com.gentics.contentnode.etc.LangTrx;
import com.gentics.contentnode.factory.DBTable;
import com.gentics.contentnode.factory.DBTables;
import com.gentics.contentnode.factory.FactoryHandle;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.messaging.Message;
import com.gentics.contentnode.messaging.MessageSender;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.NodeObjectInfo;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.PublishWorkflow;
import com.gentics.contentnode.object.PublishWorkflowStep;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.object.UserGroup.ReductionType;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.i18n.CNI18nString;

/**
 * Factory for instances of {@link PublishWorkflow} and {@link PublishWorkflowStep}
 */
@DBTables({
	@DBTable(clazz = PublishWorkflow.class, name = "publishworkflow"),
	@DBTable(clazz = PublishWorkflowStep.class, name = "publishworkflow_step") })
public class PublishWorkflowFactory extends AbstractFactory {

	/**
	 * SQL Statement to insert a new publish workflow
	 */
	protected final static String INSERT_WORKFLOW_SQL = "INSERT INTO publishworkflow (page_id, creator, cdate, editor, edate) VALUES (?, ?, ?, ?, ?)";

	/**
	 * SQL Statement to update a publish workflow
	 */
	protected final static String UPDATE_WORKFLOW_SQL = "UPDATE publishworkflow SET page_id = ?, currentstep_id = ?, editor = ?, edate = ? WHERE id = ?";

	/**
	 * SQL Statement to insert a new publish workflow step
	 */
	protected final static String INSERT_WORKFLOW_STEP_SQL = "INSERT INTO publishworkflow_step (publishworkflow_id, sortorder, modified, message, creator, cdate, editor, edate) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

	/**
	 * SQL Statement to update a publish workflow step
	 */
	protected final static String UPDATE_WORKFLOW_STEP_SQL = "UPDATE publishworkflow_step SET modified = ?, editor = ?, edate = ? WHERE id = ?";

	/**
	 * Create an instance of the factory
	 */
	public PublishWorkflowFactory() {
		super();
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.factory.object.AbstractFactory#loadResultSet(java.lang.Class, java.lang.Object, com.gentics.lib.base.object.NodeObjectInfo, com.gentics.contentnode.factory.object.FactoryDataRow, java.util.List[])
	 */
	@Override
	protected <T extends NodeObject> T loadResultSet(Class<T> clazz, Integer id, NodeObjectInfo info,
			FactoryDataRow rs, List<Integer>[] idLists) throws SQLException, NodeException {
		if (PublishWorkflow.class.equals(clazz)) {
			int pageId = rs.getInt("page_id");
			int currentStepId = rs.getInt("currentstep_id");
			int creatorId = rs.getInt("creator");
			ContentNodeDate cdate = new ContentNodeDate(rs.getInt("cdate"));
			int editorId = rs.getInt("editor");
			ContentNodeDate edate = new ContentNodeDate(rs.getInt("edate"));

			return (T) new FactoryPublishWorkflow(id, info, pageId, currentStepId, creatorId, cdate, editorId, edate);
		} else if (PublishWorkflowStep.class.equals(clazz)) {
			int publishWorkflowId = rs.getInt("publishworkflow_id");
			int sortOrder = rs.getInt("sortorder");
			boolean modified = rs.getBoolean("modified");
			String message = rs.getString("message");
			int creatorId = rs.getInt("creator");
			ContentNodeDate cdate = new ContentNodeDate(rs.getInt("cdate"));
			int editorId = rs.getInt("editor");
			ContentNodeDate edate = new ContentNodeDate(rs.getInt("edate"));

			return (T) new FactoryPublishWorkflowStep(id, info, publishWorkflowId, sortOrder, modified, message, creatorId, cdate, editorId, edate);
		} else {
			throw new NodeException(getClass() + " cannot generate instance of " + clazz);
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.factory.BatchObjectFactory#batchLoadObjects(java.lang.Class, java.util.Collection, com.gentics.lib.base.object.NodeObjectInfo)
	 */
	public <T extends NodeObject> Collection<T> batchLoadObjects(Class<T> clazz, Collection<Integer> ids, NodeObjectInfo info) throws NodeException {
		String idSql = buildIdSql(ids);

		if (PublishWorkflow.class.equals(clazz)) {
			return batchLoadDbObjects(clazz, ids, info, "SELECT * FROM publishworkflow WHERE id IN " + idSql);
		} else if (PublishWorkflowStep.class.equals(clazz)) {
			return batchLoadDbObjects(clazz, ids, info, "SELECT * FROM publishworkflow_step WHERE id IN " + idSql);
		} else {
			throw new NodeException(getClass() + " cannot generate instance of " + clazz);
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.factory.ObjectFactory#createObject(com.gentics.lib.base.factory.FactoryHandle, java.lang.Class)
	 */
	public <T extends NodeObject> T createObject(FactoryHandle handle, Class<T> clazz) throws NodeException {
		if (PublishWorkflow.class.equals(clazz)) {
			return (T) new EditableFactoryPublishWorkflow(handle.createObjectInfo(PublishWorkflow.class, true));
		} else if (PublishWorkflowStep.class.equals(clazz)) {
			return (T) new EditableFactoryPublishWorkflowStep(handle.createObjectInfo(PublishWorkflowStep.class, true));
		} else {
			throw new NodeException(getClass() + " cannot create objects of " + clazz);
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.factory.ObjectFactory#loadObject(java.lang.Class, java.lang.Integer, com.gentics.lib.base.object.NodeObjectInfo)
	 */
	public <T extends NodeObject> T loadObject(Class<T> clazz, Integer id, NodeObjectInfo info) throws NodeException {
		if (PublishWorkflow.class.equals(clazz)) {
			return loadDbObject(clazz, id, info, "SELECT * FROM publishworkflow WHERE id = ?", null, null);
		} else if (PublishWorkflowStep.class.equals(clazz)) {
			return loadDbObject(clazz, id, info, "SELECT * FROM publishworkflow_step WHERE id = ?", null, null);
		} else {
			throw new NodeException(getClass() + " cannot generate instance of " + clazz);
		}
	}

	@Override
	public <T extends NodeObject> T getEditableCopy(T object, NodeObjectInfo info) throws NodeException, ReadOnlyException {
		if (object == null) {
			return null;
		}

		if (object instanceof FactoryPublishWorkflow) {
			EditableFactoryPublishWorkflow editableCopy = new EditableFactoryPublishWorkflow((FactoryPublishWorkflow) object, info);

			return (T) editableCopy;
		} else if (object instanceof FactoryPublishWorkflowStep) {
			EditableFactoryPublishWorkflowStep editableCopy = new EditableFactoryPublishWorkflowStep((FactoryPublishWorkflowStep) object, info);

			return (T) editableCopy;
		} else {
			throw new NodeException("PublishWorkflowFactory cannot create editable copy for object of " + object.getObjectInfo().getObjectClass());
		}
	}

	/**
	 * Deletes a workflow but instead of directly deleting it the action is cached and performed on flush. 
	 * @param workflow The workflow to delete
	 * @throws NodeException
	 */
	public void deleteWorkflow(PublishWorkflow workflow) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Collection<PublishWorkflow> deleteList = getDeleteList(t, PublishWorkflow.class);

		deleteList.add(workflow);
	}

	@Override
	public void flush() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		if (!isEmptyDeleteList(t, PublishWorkflow.class)) {
			flushDelete("DELETE FROM publishworkflow WHERE id IN", PublishWorkflow.class);
		}
	}

	/**
	 * For the current user, get the supergroups having any of the given permbits for
	 * the given page. If the list of all groups would include groups that have
	 * a parent - child relationship (not necessarily direct), only the child.
	 * groups are returned.
	 * @param page page in question
	 * @param permBits required permbits
	 * @return list of supergroups
	 * @throws NodeException
	 */
	public static List<UserGroup> getSuperGroups(Page page, Integer... permBits) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		// get the current user object
		SystemUser systemUser = (SystemUser) t.getObject(SystemUser.class, t.getUserId());

		// get the groups of the user
		List<UserGroup> userGroups = systemUser.getUserGroups();

		// just retain the groups that give the user the permission to view and
		// edit the page
		userGroups = reduceUserGroups(userGroups, page, PermCheckType.ALL, PermHandler.PERM_PAGE_VIEW, PermHandler.PERM_PAGE_UPDATE);

		// reduce the list to not contain descendants (which means: get the
		// highest groups that give the user the edit permission)
		userGroups = UserGroup.reduceUserGroups(userGroups, ReductionType.PARENT);

		// for every group, collect all parents
		List<UserGroup> superGroups = new Vector<UserGroup>();

		for (UserGroup group : userGroups) {
			// get the group's parents
			List<UserGroup> parents = group.getParents();

			// reduce the list of parents to groups that have the given permbits
			parents = reduceUserGroups(parents, page, PermCheckType.ANY, permBits);

			// remove all parents, which we already collected
			parents.removeAll(superGroups);

			// add the remaining parents
			superGroups.addAll(parents);
		}

		// now reduce the supergroups to not contain descendants (which means:
		// get the lowest supergroups that have the permissions)
		superGroups = UserGroup.reduceUserGroups(superGroups, ReductionType.CHILD);

		return superGroups;
	}

	/**
	 * Reduce the list of groups to only contain groups that have the requested permission bits for the given page
	 * @param list list of groups to reduce
	 * @param page page in question
	 * @param type ALL if all of the permission bits must be set, ANY if at least one must be set
	 * @param permBits permission bits
	 * @return reduced list of groups
	 * @throws NodeException
	 */
	protected static List<UserGroup> reduceUserGroups(List<UserGroup> list, Page page, PermCheckType type,
			Integer... permBits) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		List<UserGroup> reducedList = new Vector<UserGroup>(list);
		Integer folderId = page.getFolder().getId();

		// iterate through all groups
		for (Iterator<UserGroup> i = reducedList.iterator(); i.hasNext();) {
			UserGroup group = i.next();
			// get the perm handler for the group
			PermHandler permHandler = t.getGroupPermHandler(ObjectTransformer.getInt(group.getId(), 0));

			if (type == PermCheckType.ALL) {
				// iterate through the perbits
				for (int j = 0; j < permBits.length; j++) {
					if (!permHandler.checkPermissionBit(Folder.TYPE_FOLDER_INTEGER, folderId, permBits[j])) {
						// permbit not set, so remove the group and continue
						i.remove();
						break;
					}
				}
			} else if (type == PermCheckType.ANY) {
				// iterate through the permbits
				boolean foundPermBit = false;

				for (int j = 0; j < permBits.length; j++) {
					if (permHandler.checkPermissionBit(Folder.TYPE_FOLDER_INTEGER, folderId, permBits[j])) {
						// permbit is set, continue
						foundPermBit = true;
						break;
					}
				}

				if (!foundPermBit) {
					// found no perm bit, so remove the group
					i.remove();
				}
			}
		}
		return reducedList;
	}

	/**
	 * Save the publish workflow to the database
	 * @param workflow workflow to save
	 * @throws NodeException
	 */
	private static void savePublishWorkflowObject(EditableFactoryPublishWorkflow workflow) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		boolean isNew = PublishWorkflow.isEmptyId(workflow.getId());

		if (isNew) {
			// insert a new record
			List<Integer> keys = DBUtils.executeInsert(INSERT_WORKFLOW_SQL, new Object[] {
				workflow.pageId, workflow.creatorId, workflow.cdate.getIntTimestamp(), workflow.editorId, workflow.edate.getIntTimestamp()});

			if (keys.size() == 1) {
				// set the new step id
				workflow.setId(keys.get(0));
			} else {
				throw new NodeException("Error while inserting new workflow, could not get the insertion id");
			}
		} else {
			workflow.editorId = t.getUserId();
			workflow.edate = new ContentNodeDate(t.getUnixTimestamp());
			DBUtils.executeUpdate(UPDATE_WORKFLOW_SQL,
					new Object[] { workflow.pageId, workflow.currentStepId, workflow.editorId, workflow.edate.getIntTimestamp(), workflow.getId()});
		}
	}

	/**
	 * Save the publish workflow step to the database
	 * @param step step to save
	 * @throws NodeException
	 */
	private static void savePublishWorkflowStepObject(EditableFactoryPublishWorkflowStep step) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		boolean isNew = PublishWorkflowStep.isEmptyId(step.getId());

		if (isNew) {
			// insert a new record
			List<Integer> keys = DBUtils.executeInsert(INSERT_WORKFLOW_STEP_SQL,
					new Object[] {
				step.publishWorkflowId, step.sortOrder, step.pageModified, step.message, step.creatorId, step.cdate.getIntTimestamp(), step.editorId,
				step.edate.getIntTimestamp()
			});

			if (keys.size() == 1) {
				// set the new step id
				step.setId(keys.get(0));
			} else {
				throw new NodeException("Error while inserting new step, could not get the insertion id");
			}
		} else {
			step.editorId = t.getUserId();
			step.edate = new ContentNodeDate(t.getUnixTimestamp());
			DBUtils.executeUpdate(UPDATE_WORKFLOW_STEP_SQL, new Object[] {
				step.pageModified, step.editorId, step.edate.getIntTimestamp(), step.getId()
			});
		}
	}

	/**
	 * Enum for checking permission bits
	 * @author norbert
	 */
	protected static enum PermCheckType {
		ALL, ANY
	}

	/**
	 * Inner class for {@link PublishWorkflow} instances created by the factory
	 */
	private static class FactoryPublishWorkflow extends PublishWorkflow {

		/**
		 * serial version UID
		 */
		private static final long serialVersionUID = 8475988063022614124L;

		/**
		 * ID of the page
		 */
		protected int pageId;

		/**
		 * ID of the current step
		 */
		protected int currentStepId;

		/**
		 * ID of the creator
		 */
		protected int creatorId;

		/**
		 * Creation date
		 */
		protected ContentNodeDate cdate;

		/**
		 * ID of the editor
		 */
		protected int editorId;

		/**
		 * Edit date
		 */
		protected ContentNodeDate edate;

		/**
		 * List of workflow steps ids
		 */
		protected List<Integer> stepIds;

		/**
		 * Create a new empty instance
		 * @param info object info
		 * @throws NodeException
		 */
		protected FactoryPublishWorkflow(NodeObjectInfo info) throws NodeException {
			super(null, info);
			Transaction t = TransactionManager.getCurrentTransaction();

			creatorId = t.getUserId();
			editorId = t.getUserId();
			cdate = new ContentNodeDate(t.getUnixTimestamp());
			edate = new ContentNodeDate(t.getUnixTimestamp());
		}

		/**
		 * Create an instance of the publish workflow
		 * @param id id
		 * @param info object info
		 * @param pageId page id
		 * @param currentStepId id of the current step
		 * @param creatorId creator id
		 * @param cdate creation date
		 * @param editorId editor id
		 * @param edate edit date
		 */
		protected FactoryPublishWorkflow(Integer id, NodeObjectInfo info, int pageId, int currentStepId, int creatorId,
				ContentNodeDate cdate, int editorId, ContentNodeDate edate) {
			super(id, info);
			this.pageId = pageId;
			this.currentStepId = currentStepId;
			this.creatorId = creatorId;
			this.cdate = cdate;
			this.editorId = editorId;
			this.edate = edate;
		}

		@Override
		public ContentNodeDate getCDate() {
			return cdate;
		}

		@Override
		public SystemUser getCreator() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			SystemUser creator = (SystemUser) t.getObject(SystemUser.class, creatorId);

			assertNodeObjectNotNull(creator, creatorId, "creator");
			return creator;
		}

		@Override
		public PublishWorkflowStep getCurrentStep() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			PublishWorkflowStep currentStep = (PublishWorkflowStep) t.getObject(PublishWorkflowStep.class, currentStepId, getObjectInfo().isEditable());

			assertNodeObjectNotNull(currentStep, currentStepId, "current step", true);
			return currentStep;
		}

		@Override
		public ContentNodeDate getEDate() {
			return edate;
		}

		@Override
		public SystemUser getEditor() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			SystemUser editor = (SystemUser) t.getObject(SystemUser.class, editorId);

			assertNodeObjectNotNull(editor, editorId, "editor");
			return editor;
		}

		@Override
		public Page getPage() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			Page page = (Page) t.getObject(Page.class, pageId);

			assertNodeObjectNotNull(page, pageId, "page");
			return page;
		}

		/**
		 * Get the ids of the publish workflow steps
		 * @return ids of the publish workflow steps
		 * @throws NodeException
		 */
		protected synchronized List<Integer> getStepIds() throws NodeException {
			if (stepIds == null) {
				if (isEmptyId(getId())) {
					stepIds = new ArrayList<>();
				} else {
					stepIds = DBUtils.select(
							"SELECT id FROM publishworkflow_step WHERE publishworkflow_id = ? ORDER BY sortorder ASC",
							ps -> ps.setInt(1, getId()), DBUtils.IDLIST);
				}
			}
			return stepIds;
		}

		@Override
		public List<PublishWorkflowStep> getSteps() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			List<PublishWorkflowStep> objects = t.getObjects(PublishWorkflowStep.class, getStepIds(), getObjectInfo().isEditable());

			List<PublishWorkflowStep> steps = new Vector<PublishWorkflowStep>(objects.size());

			for (NodeObject step : objects) {
				steps.add((PublishWorkflowStep) step);
			}

			return steps;
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.object.NodeObject#copy()
		 */
		public NodeObject copy() throws NodeException {
			throw new NodeException("Copy is not implemented for PublishWorkflows");
		}

		@Override
		public String toString() {
			return "PublishWorkflow {page " + pageId + "," + getId() + "}";
		}

		@Override
		public boolean allowsEditing() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();

			// get the current user
			SystemUser user = (SystemUser) t.getObject(SystemUser.class, t.getUserId());

			// get the groups currently assigned to the page
			PublishWorkflowStep currentStep = getCurrentStep();

			List<UserGroup> stepGroups = currentStep.getUserGroups();

			// check whether the user is member of one of the groups
			boolean isMember = false;

			for (UserGroup group : stepGroups) {
				if (group.isMember(user)) {
					isMember = true;
					break;
				}
			}

			// if the user is not directly a member of any group to
			// which the page is currently assigned, we check, whether
			// the user is member of a super group
			if (!isMember) {
				// get all groups of the user
				List<UserGroup> userGroups = user.getUserGroups();

				// check for super groups of step groups
				for (UserGroup userGroup : userGroups) {
					for (UserGroup stepGroup : stepGroups) {
						// check whether the usergroup is parent of the step group
						if (stepGroup.getParents().contains(userGroup)) {
							// check whether the usergroups gives permission to view the page
							PermHandler groupPermHandler = t.getGroupPermHandler(ObjectTransformer.getInt(userGroup.getId(), 0));

							if (groupPermHandler.canView(getPage())) {
								isMember = true;
								break;
							}
						}
					}

					if (isMember) {
						break;
					}
				}
			}

			return isMember;
		}

		/*
		 * (non-Javadoc)
		 * @see com.gentics.contentnode.object.Page#performDelete()
		 */
		protected void performDelete() throws NodeException {
			getWorkflowFactory().deleteWorkflow(this);
		}

		/**
		 * Get the factory for workflows
		 * @return factory
		 * @throws NodeException
		 */
		private PublishWorkflowFactory getWorkflowFactory() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();

			return (PublishWorkflowFactory) t.getObjectFactory(PublishWorkflow.class);
		}
	}

	/**
	 * Inner class for editable {@link PublishWorkflow} instances created by the factory
	 */
	private static class EditableFactoryPublishWorkflow extends FactoryPublishWorkflow {

		/**
		 * Serial version UID
		 */
		private static final long serialVersionUID = 9150436897265542229L;

		/**
		 * Flag to mark whether the publish workflow has been modified (contains changes which need to be persistet by calling {@link #save()}).
		 */
		private boolean modified = false;

		/**
		 * Editable copies of the workflow steps
		 */
		private List<PublishWorkflowStep> steps = null;

		/**
		 * Create a new empty instance of a publish workflow
		 * @param info info about the instance
		 * @throws NodeException
		 */
		protected EditableFactoryPublishWorkflow(NodeObjectInfo info) throws NodeException {
			super(info);
			modified = true;
		}

		/**
		 * Create an editable copy of the workflow
		 * @param workflow original workflow object
		 * @param info object info
		 * @throws ReadOnlyException
		 * @throws NodeException
		 */
		protected EditableFactoryPublishWorkflow(FactoryPublishWorkflow workflow,
				NodeObjectInfo info) throws ReadOnlyException, NodeException {
			super(workflow.getId(), info, workflow.pageId, workflow.currentStepId, workflow.creatorId, workflow.cdate, workflow.editorId, workflow.edate);
		}

		/**
		 * Set the id after saving the object
		 * @param id id of the object
		 */
		protected void setId(Integer id) {
			if (this.id == null) {
				this.id = id;
			}
		}

		@Override
		public void setPageId(Integer pageId) throws ReadOnlyException {
			int newPageId = ObjectTransformer.getInt(pageId, 0);

			if (this.pageId != newPageId) {
				this.pageId = newPageId;
				modified = true;
			}
		}

		@Override
		public List<PublishWorkflowStep> getSteps() throws NodeException {
			if (steps == null) {
				steps = new Vector<PublishWorkflowStep>(super.getSteps());
			}
			return steps;
		}

		@Override
		public void addStep(String message, UserGroup... groups) throws NodeException {
			// get the current list of steps
			List<PublishWorkflowStep> steps = getSteps();

			// create a new step object
			Transaction t = TransactionManager.getCurrentTransaction();
			PublishWorkflowStep step = (PublishWorkflowStep) t.createObject(PublishWorkflowStep.class);

			step.setMessage(message);

			// add the new step
			steps.add(step);

			// set its sortorder
			step.setSortOrder(steps.size());

			List<UserGroup> userGroups = step.getUserGroups();

			// set the groups for the step
			if (ObjectTransformer.isEmpty(groups)) {
				// when no groups passed to the method, use the super groups of the user
				userGroups.addAll(getSuperGroups(getPage(), PermHandler.PERM_PAGE_UPDATE, PermHandler.PERM_PAGE_PUBLISH));
			} else {
				// assign to the given groups
				userGroups.addAll(Arrays.asList(groups));
			}
		}

		@Override
		public boolean save() throws NodeException {
			assertEditable();
			boolean isModified = this.modified;

			// first save the workflow object
			if (isModified) {
				savePublishWorkflowObject(this);
				this.modified = false;
			}

			// now check all the workflow steps
			List<PublishWorkflowStep> steps = getSteps();

			for (PublishWorkflowStep step : steps) {
				step.setWorkflowId(ObjectTransformer.getInt(getId(), 0));
				isModified |= step.save();
			}

			// remove steps which existed and do not exist any more
			if (!ObjectTransformer.isEmpty(stepIds)) {
				List<Object> stepsToRemove = new Vector<Object>(stepIds);

				for (PublishWorkflowStep step : steps) {
					stepsToRemove.remove(step.getId());
				}

				if (!ObjectTransformer.isEmpty(stepsToRemove)) {
					Transaction t = TransactionManager.getCurrentTransaction();
					PreparedStatement pst = null;

					try {
						StringBuffer sql = new StringBuffer("DELETE FROM publishworkflow_step WHERE publishworkflow_id = ? AND id IN (");

						sql.append(StringUtils.repeat("?", stepsToRemove.size(), ","));
						sql.append(")");
						pst = t.prepareDeleteStatement(sql.toString());
						int pCounter = 1;

						pst.setObject(pCounter++, getId());
						for (Object step : stepsToRemove) {
							pst.setObject(pCounter++, step);
						}

						pst.executeUpdate();
					} catch (SQLException e) {
						throw new NodeException("Error while saving publishworkflow", e);
					} finally {
						t.closeStatement(pst);
					}
				}
			}

			// set the last step to be the current
			if (isModified && !ObjectTransformer.isEmpty(steps)) {
				int newCurrentStepId = ObjectTransformer.getInt(steps.get(steps.size() - 1).getId(), 0);

				if (currentStepId != newCurrentStepId) {
					currentStepId = newCurrentStepId;
					// we need to save again
					savePublishWorkflowObject(this);
					this.modified = true;
				}
			}

			// send page to queue
			getPage().queuePublish(getCreator());

			// dirt the cache
			if (isModified) {
				Transaction t = TransactionManager.getCurrentTransaction();

				t.dirtObjectCache(PublishWorkflow.class, getId());
			}

			return isModified;
		}

		@Override
		public void revokeStep() throws NodeException {
			assertEditable();

			List<PublishWorkflowStep> steps = getSteps();

			if (steps.size() > 0) {
				// remove the last workflow step
				steps.remove(steps.size() - 1);

				// TODO send a message to everybody who was assigned to the step

				currentStepId = ObjectTransformer.getInt(steps.get(steps.size() - 1).getId(), 0);
				modified = true;
			}
		}
	}

	/**
	 * Inner class for {@link PublishWorkflowStep} instances created by the factory
	 */
	private static class FactoryPublishWorkflowStep extends PublishWorkflowStep {

		/**
		 * Serial version UID
		 */
		private static final long serialVersionUID = -7154730856668955186L;

		/**
		 * ID of the publish workflow
		 */
		protected int publishWorkflowId;

		/**
		 * Sortorder
		 */
		protected int sortOrder;

		/**
		 * True when the page has been modified while in this step
		 */
		protected boolean pageModified;

		/**
		 * Message of the step
		 */
		protected String message;

		/**
		 * ID of the creator
		 */
		protected int creatorId;

		/**
		 * Creation date
		 */
		protected ContentNodeDate cdate;

		/**
		 * ID of the editor
		 */
		protected int editorId;

		/**
		 * Edit date
		 */
		protected ContentNodeDate edate;

		/**
		 * IDs of the user groups
		 */
		protected List<Integer> userGroupIds;

		/**
		 * Create a new empty instance
		 * @param info object info
		 * @throws NodeException
		 */
		protected FactoryPublishWorkflowStep(NodeObjectInfo info) throws NodeException {
			super(null, info);
			Transaction t = TransactionManager.getCurrentTransaction();

			creatorId = t.getUserId();
			editorId = t.getUserId();
			cdate = new ContentNodeDate(t.getUnixTimestamp());
			edate = new ContentNodeDate(t.getUnixTimestamp());
		}

		/**
		 * Create an instance of the publish workflow step
		 * @param id id 
		 * @param info object info
		 * @param publishWorkflowId id of the publish workflow
		 * @param sortOrder sortorder
		 * @param modified true when the page has been modified while in this step
		 * @param message message
		 * @param creatorId id of the creator
		 * @param cdate creation date
		 * @param editorId id of the editor
		 * @param edate edit date
		 */
		protected FactoryPublishWorkflowStep(Integer id, NodeObjectInfo info, int publishWorkflowId,
				int sortOrder, boolean modified, String message, int creatorId,
				ContentNodeDate cdate, int editorId, ContentNodeDate edate) {
			super(id, info);
			this.publishWorkflowId = publishWorkflowId;
			this.sortOrder = sortOrder;
			this.message = message;
			this.pageModified = modified;
			this.creatorId = creatorId;
			this.cdate = cdate;
			this.editorId = editorId;
			this.edate = edate;
		}

		@Override
		public ContentNodeDate getCDate() {
			return cdate;
		}

		@Override
		public SystemUser getCreator() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			SystemUser creator = (SystemUser) t.getObject(SystemUser.class, creatorId);

			assertNodeObjectNotNull(creator, creatorId, "creator");
			return creator;
		}

		@Override
		public ContentNodeDate getEDate() {
			return edate;
		}

		@Override
		public SystemUser getEditor() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			SystemUser editor = (SystemUser) t.getObject(SystemUser.class, editorId);

			assertNodeObjectNotNull(editor, editorId, "editor");
			return editor;
		}

		@Override
		public String getMessage() {
			return message;
		}

		@Override
		public int getSortorder() {
			return sortOrder;
		}

		@Override
		public PublishWorkflow getWorkflow() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			PublishWorkflow publishWorkflow = (PublishWorkflow) t.getObject(PublishWorkflow.class, publishWorkflowId);

			assertNodeObjectNotNull(publishWorkflow, publishWorkflowId, "publish workflow");
			return publishWorkflow;
		}

		/**
		 * Get the IDs of the user groups
		 * @return IDs of the user groups
		 * @throws NodeException
		 */
		protected synchronized List<Integer> getUserGroupIds() throws NodeException {
			if (userGroupIds == null) {
				if (isEmptyId(getId())) {
					userGroupIds = new ArrayList<>();
				} else {
					userGroupIds = DBUtils.select(
							"SELECT group_id id FROM publishworkflowstep_group WHERE publishworkflowstep_id = ?",
							ps -> ps.setInt(1, getId()), DBUtils.IDLIST);
				}
			}
			return userGroupIds;
		}

		@SuppressWarnings("unchecked")
		@Override
		public List<UserGroup> getUserGroups() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			List<UserGroup> userGroups = t.getObjects(UserGroup.class, getUserGroupIds());

			return userGroups;
		}

		@Override
		public boolean isPageModified() {
			return pageModified;
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.object.NodeObject#copy()
		 */
		public NodeObject copy() throws NodeException {
			throw new NodeException("Copy is not implemented for PublishWorkflows");
		}

		@Override
		public String toString() {
			return "PublishWorkflowStep {#" + sortOrder + "," + getId() + "}";
		}
	}

	/**
	 * Inner class for editable {@link PublishWorkflowStep} instances created by the factory
	 */
	private static class EditableFactoryPublishWorkflowStep extends FactoryPublishWorkflowStep {

		/**
		 * Flag to mark whether the publish workflow step has been modified (contains changes which need to be persistet by calling {@link #save()}).
		 */
		private boolean modified = false;

		/**
		 * Editable list of user groups
		 */
		private List<UserGroup> groups;

		/**
		 * Create a new empty instance of a publish workflow
		 * @param info info about the instance
		 * @throws NodeException
		 */
		protected EditableFactoryPublishWorkflowStep(NodeObjectInfo info) throws NodeException {
			super(info);
			modified = true;
		}

		/**
		 * Create an editable copy of the workflow step
		 * @param step original workflow step object
		 * @param info object info
		 * @throws ReadOnlyException
		 * @throws NodeException
		 */
		protected EditableFactoryPublishWorkflowStep(FactoryPublishWorkflowStep step,
				NodeObjectInfo info) throws ReadOnlyException, NodeException {
			super(step.getId(), info, step.publishWorkflowId, step.sortOrder, step.pageModified, step.message, step.creatorId, step.cdate, step.editorId,
					step.edate);
		}

		@Override
		public List<UserGroup> getUserGroups() throws NodeException {
			if (groups == null) {
				groups = new Vector<UserGroup>(super.getUserGroups());
			}

			return groups;
		}

		/**
		 * Set the id after saving the object
		 * @param id id of the object
		 */
		protected void setId(Integer id) {
			if (this.id == null) {
				this.id = id;
			}
		}

		@Override
		public void setMessage(String message) throws ReadOnlyException {
			message = ObjectTransformer.getString(message, "");
			if (!StringUtils.isEqual(this.message, message)) {
				this.message = message;
				this.modified = true;
			}
		}

		@Override
		public boolean setPageModified(boolean pageModified) throws ReadOnlyException {
			if (this.pageModified != pageModified) {
				boolean oldPageModified = this.pageModified;

				this.pageModified = pageModified;
				this.modified = true;
				return oldPageModified;
			} else {
				return this.pageModified;
			}
		}

		@Override
		public void setSortOrder(int sortOrder) throws ReadOnlyException {
			if (this.sortOrder != sortOrder) {
				this.sortOrder = sortOrder;
				this.modified = true;
			}
		}

		@Override
		public void setWorkflowId(int workflowId) throws ReadOnlyException {
			if (this.publishWorkflowId != workflowId) {
				this.publishWorkflowId = workflowId;
				this.modified = true;
			}
		}

		@Override
		public boolean save() throws NodeException {
			assertEditable();
			boolean isModified = this.modified;

			// remember if the step is new
			boolean isNew = PublishWorkflowStep.isEmptyId(getId());

			if (isModified) {
				savePublishWorkflowStepObject(this);
				this.modified = false;
			}

			// save the groups (userGroupIds contains the ids of groups that
			// were assigned, groups contains the groups that shall be assigned)
			getUserGroups();
			List<Object> toDelete = new Vector<Object>(userGroupIds);
			List<Object> toAdd = new Vector<Object>();

			// iterate over all groups
			for (UserGroup group : groups) {
				// do not delete the group (it is still assigned)
				toDelete.remove(group.getId());

				// ... but add it (if not there before)
				if (!userGroupIds.contains(group.getId())) {
					toAdd.add(group.getId());
				}
			}

			Transaction t = TransactionManager.getCurrentTransaction();
			PreparedStatement pst = null;

			// eventually delete groups
			if (!ObjectTransformer.isEmpty(toDelete)) {
				try {
					pst = t.prepareDeleteStatement(
							"DELETE FROM publishworkflowstep_group WHERE publishworkflowstep_id = ? AND group_id IN (" + StringUtils.repeat("?", toDelete.size(), ",")
							+ ")");
					int pCounter = 1;

					for (Object groupId : toDelete) {
						pst.setObject(pCounter++, groupId);
					}
					pst.executeUpdate();
					isModified = true;
				} catch (SQLException e) {
					throw new NodeException("Error while removing groups from " + this, e);
				} finally {
					t.closeStatement(pst);
				}
			}

			// eventually add groups
			if (!ObjectTransformer.isEmpty(toAdd)) {
				try {
					pst = t.prepareUpdateStatement("INSERT INTO publishworkflowstep_group (publishworkflowstep_id, group_id) VALUES (?, ?)");
					pst.setObject(1, getId());

					for (Object groupId : toAdd) {
						pst.setObject(2, groupId);
						pst.executeUpdate();
					}
					isModified = true;
				} catch (SQLException e) {
					throw new NodeException("Error while adding groups to " + this, e);
				} finally {
					t.closeStatement(pst);
				}
			}

			// for new steps, we need to send messages to the users of the groups
			if (isNew) {
				List<SystemUser> users = new Vector<SystemUser>();

				for (UserGroup group : groups) {
					List<SystemUser> members = new Vector<SystemUser>(group.getMembers());

					members.removeAll(users);
					users.addAll(members);
				}

				if (!ObjectTransformer.isEmpty(users)) {
					final MessageSender messageSender = new MessageSender();
					String pageId = ObjectTransformer.getString(getWorkflow().getPage().getId(), null);
					CNI18nString autoMessage = new CNI18nString("page.workflow.message");
					SystemUser editor = getEditor();

					autoMessage.setParameter("editor", editor.getFirstname() + " " + editor.getLastname());
					autoMessage.setParameter("pageid", pageId);

					for (SystemUser user : users) {
						try (LangTrx lTrx = new LangTrx(user)) {
							StringBuilder inboxMessage = new StringBuilder();
							inboxMessage.append(autoMessage.toString());
							if (!StringUtils.isEmpty(message)) {
								inboxMessage.append(" ").append(message);
							}
							messageSender.sendMessage(new Message(creatorId, ObjectTransformer.getInt(user.getId(), 0), inboxMessage.toString()));
						}
					}
					t.addTransactional(messageSender);
				}
			}

			// dirt the cache
			if (isModified) {
				t.dirtObjectCache(PublishWorkflowStep.class, getId());
			}

			return isModified;
		}
	}
}
