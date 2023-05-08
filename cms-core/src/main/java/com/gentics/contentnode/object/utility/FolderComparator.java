package com.gentics.contentnode.object.utility;

import java.util.Comparator;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.rest.util.ModelBuilder;
import com.gentics.lib.etc.StringUtils;

/**
 * Comparator for sorting folders
 * @author norbert
 */
public class FolderComparator extends AbstractComparator implements Comparator<Folder> {

	/**
	 * Create a new FolderComparator
	 * @param attribute the sorted attibute
	 * @param way sortway
	 */
	public FolderComparator(String attribute, String way) {
		super(attribute, way);
	}

	/* (non-Javadoc)
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	public int compare(com.gentics.contentnode.object.Folder f1,
			com.gentics.contentnode.object.Folder f2) {
		int cmp = 0;

		switch (attribute) {
		case EDIT_DATE:
			cmp = f1.getEDate().compareTo(f2.getEDate()) * way;
			break;

		case CREATE_DATE:
			cmp = f1.getCDate().compareTo(f2.getCDate()) * way;
			break;

		case NAME:
			cmp = StringUtils.mysqlLikeCompare(f1.getName(), f2.getName()) * way;
			break;
			
		case MASTER_NODE:
			try {
				cmp = StringUtils.mysqlLikeCompare(f1.getMasterNodeFolderName(), f2.getMasterNodeFolderName()) * way;
			} catch (NodeException e) {
				e.printStackTrace();
				cmp = 0;
			}
			break;

		case EXCLUDED:
			try {
				cmp = compareMCExclusion(f1, f2) * way;
			} catch (NodeException e) {
				logger.error("Error while sorting " + f1 + " and " + f2 + " by mc exclusion", e);
			}
			break;
		case DELETED_BY:
			cmp = compareUser(f1, f2, Folder::getDeletedBy);
			break;
		case DELETED_DATE:
			cmp = (f1.getDeleted() - f2.getDeleted()) * way;
			break;
		case PATH:
			try {
				String path1 = ModelBuilder.getFolderPath(f1);
				String path2 = ModelBuilder.getFolderPath(f2);
				cmp = StringUtils.mysqlLikeCompare(path1, path2) * way;
			} catch (NodeException e) {
				cmp = 0;
			}
			break;
		case PUBLISH_DIR:
			cmp = StringUtils.mysqlLikeCompare(f1.getPublishDir(), f2.getPublishDir()) * way;
			break;
		case CREATOR:
			cmp = compareUser(f1, f2, Folder::getCreator);
			break;
		case EDITOR:
			cmp = compareUser(f1, f2, Folder::getEditor);
			break;
		case NODE:
			cmp = compareNode(f1, f2);
			break;
		default:
			cmp = 0;
			break;
		}

		if (cmp == 0) {
			cmp = (ObjectTransformer.getInt(f1.getId(), 0) - ObjectTransformer.getInt(f2.getId(), 0)) * way;
		}

		return cmp;
	}
}