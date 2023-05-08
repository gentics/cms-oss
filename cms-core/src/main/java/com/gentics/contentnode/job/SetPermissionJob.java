package com.gentics.contentnode.job;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.quartz.JobDataMap;
import org.quartz.JobExecutionException;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.model.perm.PermType;
import com.gentics.contentnode.rest.model.request.SetPermsRequest;
import com.gentics.lib.i18n.CNI18nString;

/**
 * Job to set permissions
 */
public class SetPermissionJob extends AbstractUserActionJob {
	/**
	 * Name of the type parameter
	 */
	public final static String PARAM_TYPE = "type";

	/**
	 * Name of the ID parameter
	 */
	public final static String PARAM_ID = "id";

	/**
	 * Name of the request parameter
	 */
	public final static String PARAM_REQUEST = "request";

	/**
	 * Object type
	 */
	protected int objType;

	/**
	 * Object ID
	 */
	protected int objId;

	/**
	 * Request
	 */
	protected SetPermsRequest req;

	/**
	 * Create empty instance
	 */
	public SetPermissionJob() {
	}

	/**
	 * Create an instance and set the parameters
	 * @param objType object type
	 * @param objId object id
	 * @param request set request
	 */
	public SetPermissionJob(int objType, int objId, SetPermsRequest request) {
		addParameter(PARAM_TYPE, objType);
		addParameter(PARAM_ID, objId);
		addParameter(PARAM_REQUEST, request);
	}

	@Override
	public String getJobDescription() {
		return new CNI18nString("set folder permission").toString();
	}

	@Override
	protected boolean getJobParameters(JobDataMap map) {
		objType = ObjectTransformer.getInt(map.get(PARAM_TYPE), 0);
		objId = ObjectTransformer.getInt(map.get(PARAM_ID), 0);
		req = (SetPermsRequest)map.get(PARAM_REQUEST);
		return true;
	}

	@Override
	protected void processAction() throws InsufficientPrivilegesException, NodeException, JobExecutionException {
		Transaction t = TransactionManager.getCurrentTransaction();
		if (req.isSubGroups() || req.isSubObjects()) {
			t.setInstantPublishingEnabled(false);
		}
		PermHandler permHandler = t.getPermHandler();
		objType = normalizeType(objType);

		// get the object
		Class<? extends NodeObject> objClass = t.getClass(objType);
		if (objClass == null) {
			throw new EntityNotFoundException("Could not find object type " + objType);
		}
		if (objClass.equals(Node.class)) {
			objClass = Folder.class;
		}
		NodeObject object = t.getObject(objClass, objId);
		if (object == null) {
			throw new EntityNotFoundException("Could not find object " + objType + "." + objId);
		}

		// check whether the user is allowed to change permissions for the object
		if (!permHandler.canSetPerms(object)) {
			throw new InsufficientPrivilegesException("Not allowed to change permissions on object " + + objType + "." + objId, object, PermType.setperm);
		}

		// get the group
		UserGroup group = t.getObject(UserGroup.class, req.getGroupId());
		if (group == null) {
			throw new EntityNotFoundException("Did not find group with id " + req.getGroupId());
		}

		// check whether the user is allowed to change permissions for the group
		if (!permHandler.canSetPerms(group)) {
			throw new InsufficientPrivilegesException("Not allowed to change permissions on group " + req.getGroupId(), group, PermType.setperm);
		}

		List<UserGroup> groups = new ArrayList<UserGroup>();
		groups.add(group);

		// optionally get subgroups
		if (req.isSubGroups()) {
			recursiveAddSubGroups(group, groups);
		}

		// set the permission
		setPermissions(object, groups, req.getPerm(), req.getRoleIds(), permHandler, req.isSubObjects());

		t.commit(false);
	}

	/**
	 * Recursively add all child groups to the given list
	 * @param group group to start with
	 * @param subGroups list where all child groups will be added
	 * @throws NodeException
	 */
	protected void recursiveAddSubGroups(UserGroup group, List<UserGroup> subGroups) throws NodeException {
		List<UserGroup> childGroups = group.getChildGroups();
		subGroups.addAll(childGroups);
		for (UserGroup child : childGroups) {
			recursiveAddSubGroups(child, subGroups);
		}
	}

	/**
	 * Set the permission on the given object.
	 * Recursive setting is currently only implemented for folders
	 * @param object object for which the permission shall be set
	 * @param groups groups for which the permissions shall be set
	 * @param perm permission bits
	 * @param roleIds all role IDs that should be set after this method completes. IDs not mentioned will be removed. If null, no changes to the roles are performed 
	 * @param permHandler perm handler
	 * @param recursive true for setting recursively
	 * @throws NodeException
	 */
	protected void setPermissions(NodeObject object, List<UserGroup> groups, String perm, Set<Integer> roleIds, PermHandler permHandler, boolean recursive) throws NodeException {

		if (perm != null) {
			// set the permission
			PermHandler.setPermissions(ObjectTransformer.getInt(object.getTType(), 0), ObjectTransformer.getInt(object.getId(), 0), groups, perm);
		}

		if (roleIds != null) {
			PermHandler.setRoles(ObjectTransformer.getInt(object.getTType(), 0), ObjectTransformer.getInt(object.getId(), 0), groups, roleIds);
		}

		if (object instanceof Folder) {
			Folder folder = (Folder)object;

			if (folder.isRoot()) {
				if (perm != null) {
					PermHandler.setPermissions(Node.TYPE_NODE, ObjectTransformer.getInt(object.getId(), 0), groups, perm);
				}
				if (roleIds != null) {
					PermHandler.setRoles(Node.TYPE_NODE, ObjectTransformer.getInt(object.getId(), 0), groups, roleIds);
				}
			}

			if (recursive) {
				// for setting permissions recursively, we get all child folders
				List<Folder> children = folder.getChildFolders();
				for (Folder child : children) {
					// only set permissions on master folders, which we are allowed to set permissions
					if (child.isMaster() && permHandler.canSetPerms(child)) {
						setPermissions(child, groups, perm, roleIds, permHandler, recursive);
					}
				}

				// now also get all local child folders in channels
				Transaction t = TransactionManager.getCurrentTransaction();
				Node node = folder.getNode();
				Collection<Node> channels = node.getAllChannels();
				for (Node channel : channels) {
					t.setChannelId(channel.getId());
					List<Folder> channelChildren = new ArrayList<Folder>();
					try {
						// get all children in the channel
						channelChildren.clear();
						channelChildren.addAll(folder.getChildFolders());
						// remove all folders, that either are inherited or localized, remaining folders will be channel local
						for (Iterator<Folder> i = channelChildren.iterator(); i.hasNext();) {
							Folder channelChild = i.next();
							if (channelChild.isInherited() || !channelChild.isMaster() || !permHandler.canSetPerms(channelChild)) {
								i.remove();
							}
						}
					} finally {
						t.resetChannel();
					}

					for (Folder channelChild : channelChildren) {
						setPermissions(channelChild, groups, perm, roleIds, permHandler, recursive);
					}
				}
			}
		}
	}

	/**
	 * Normalize the object type for permission checks
	 * @param objType object type
	 * @return normalized object type
	 */
	protected int normalizeType(int objType) {
		switch(objType) {
		case Folder.TYPE_INHERITED_FOLDER:
			return Folder.TYPE_FOLDER;
		case Node.TYPE_CHANNEL:
			return Node.TYPE_NODE;
		default:
			return objType;
		}
	}
}
