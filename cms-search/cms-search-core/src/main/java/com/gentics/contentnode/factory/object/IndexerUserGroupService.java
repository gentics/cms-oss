package com.gentics.contentnode.factory.object;

import java.util.List;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Transactional;
import com.gentics.contentnode.factory.UpdatePermissionsInIndexTransactional;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.object.search.Indexer;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;

/**
 * Implementation of {@link UserGroupService} that updates the permission information in search indices
 */
public class IndexerUserGroupService implements UserGroupService {
	@Override
	public void onSave(UserGroup object, boolean isNew, boolean contentModified, int userId) throws NodeException {
		if (isNew) {
			updateFolderPermissionsInIndex(object.getId(), false);
		}
	}

	@Override
	public void onDelete(UserGroup object, boolean wastebin, int userId) throws NodeException {
		updateFolderPermissionsInIndex(object.getId(), false);
	}

	/**
	 * Update folder permissions in the index
	 * @param groupId group ID
	 * @param immediately true to update immediately, false to update in a {@link Transactional}
	 * @throws NodeException
	 */
	protected void updateFolderPermissionsInIndex(int groupId, boolean immediately) throws NodeException {
		if (NodeConfigRuntimeConfiguration.isFeature(Feature.ELASTICSEARCH)) {
			// get all folder IDs, which have permissions set for the group
			List<Integer> folderIds = DBUtils
					.select("SELECT o_id FROM perm WHERE usergroup_id = ? AND o_type IN (?, ?, ?)", ps -> {
						int index = 1;
						ps.setInt(index++, groupId);
						ps.setInt(index++, Node.TYPE_NODE);
						ps.setInt(index++, Folder.TYPE_FOLDER);
						ps.setInt(index++, Node.TYPE_CHANNEL);
					}, DBUtils.allInt("o_id"));
	
			if (immediately) {
				Indexer.updatePermissions(folderIds, true);
			} else {
				TransactionManager.getCurrentTransaction()
						.addTransactional(new UpdatePermissionsInIndexTransactional(folderIds));
			}
		}
	}
}
