package com.gentics.contentnode.object.utility;

import java.util.Comparator;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.rest.util.ModelBuilder;
import com.gentics.lib.etc.StringUtils;

/**
 * Comparator for sorting files
 * @author clemens
 */
public class FileComparator extends AbstractComparator implements Comparator<File> {

	/**
	 * generates a new FileComparator 
	 * @param attribute the attribute to sort by. May be one of
	 *      name (default), edate, type or size
	 * @param way sort way, may be "asc" or "desc" - defaults to asc
	 */
	public FileComparator(String attribute, String way) {
		super(attribute, way);
	}

	/* (non-Javadoc)
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	public int compare(File f1, File f2) {
		int cmp = 0;

		switch (this.attribute) {
		case NAME:
			cmp = StringUtils.mysqlLikeCompare(f1.getName(), f2.getName()) * way;
			break;

		case EDIT_DATE:
			cmp = f1.getEDate().compareTo(f2.getEDate()) * way;
			break;

		case CREATE_DATE:
			cmp = f1.getCDate().compareTo(f2.getCDate()) * way;
			break;

		case TYPE:
			cmp = StringUtils.mysqlLikeCompare(f1.getFiletype(), f2.getFiletype()) * way;
			break;

		case SIZE:
			if (f1.getFilesize() > f2.getFilesize()) {
				cmp = 1 * way;
			} else if (f1.getFilesize() == f2.getFilesize()) {
				cmp = 0;
			} else if (f1.getFilesize() < f2.getFilesize()) {
				cmp = -1 * way;
			}
			break;

		case FOLDER:
			try {
				cmp = StringUtils.mysqlLikeCompare(f1.getFolder().getName(), f2.getFolder().getName()) * way;
			} catch (NodeException e) {
				cmp = 0;
			}
			break;
			
		case MASTER_NODE:
			try {
				cmp = StringUtils.mysqlLikeCompare(f1.getMasterNodeFolderName(), f2.getMasterNodeFolderName()) * way;
			} catch (NodeException e) {
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
			cmp = compareUser(f1, f2, File::getDeletedBy);
			break;
		case DELETED_DATE:
			cmp = (f1.getDeleted() - f2.getDeleted()) * way;
			break;
		case PATH:
			try {
				String path1 = ModelBuilder.getFolderPath(f1.getFolder());
				String path2 = ModelBuilder.getFolderPath(f2.getFolder());
				cmp = StringUtils.mysqlLikeCompare(path1, path2) * way;
			} catch (NodeException e) {
				cmp = 0;
			}
			break;
		case CREATOR:
			cmp = compareUser(f1, f2, File::getCreator);
			break;
		case EDITOR:
			cmp = compareUser(f1, f2, File::getEditor);
			break;
		case NODE:
			cmp = compareNode(f1, f2);
			break;
		case CUSTOM_OR_DEFAULT_EDIT_DATE:
			cmp = f1.getCustomOrDefaultEDate().compareTo(f2.getCustomOrDefaultEDate()) * way;
			break;
		case CUSTOM_OR_DEFAULT_CREATE_DATE:
			cmp = f1.getCustomOrDefaultCDate().compareTo(f2.getCustomOrDefaultCDate()) * way;
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