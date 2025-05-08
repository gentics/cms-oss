package com.gentics.contentnode.object.utility;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.rest.util.ModelBuilder;
import com.gentics.lib.etc.StringUtils;

/**
 * Comparator for sorting pages
 * @author norbert
 *
 */
public class PageComparator extends AbstractComparator implements Comparator<Page> {
	/**
	 * Get a comparator for sorting according to the sort parameter bean (may be null)
	 * @param sortBean sort parameter bean (may be null)
	 * @return comparator (may be null)
	 */
	public static Comparator<Page> get(SortParameterBean sortBean) {
		if (sortBean == null) {
			return null;
		}
		List<Comparator<Page>> comparators = parse(sortBean.sort).stream().map(pair -> new PageComparator(pair.getLeft(), pair.getRight() ? "asc" : "desc"))
				.collect(Collectors.toList());
		return new MultiComparator<>(comparators);
	}

	/**
	 * Create a new PageComparator
	 * @param attribute the sorted attribute
	 * @param way sortway
	 */
	public PageComparator(String attribute, String way) {
		super(attribute, way);
	}

	/* (non-Javadoc)
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	public int compare(Page p1, Page p2) {
		if (p1 == null && p2 == null) {
			return 0;
		} else if (p1 == null) {
			return -way;
		} else if (p2 == null) {
			return way;
		}

		int cmp = 0;

		switch (attribute) {
		case NAME:
			cmp = StringUtils.mysqlLikeCompare(p1.getName(), p2.getName()) * way;
			break;

		case EDIT_DATE:
			cmp = p1.getEDate().compareTo(p2.getEDate()) * way;
			break;

		case CREATE_DATE:
			cmp = p1.getCDate().compareTo(p2.getCDate()) * way;
			break;

		case PUBLISH_DATE:
			cmp = p1.getPDate().compareTo(p2.getPDate()) * way;
			break;

		case FILENAME:
			cmp = StringUtils.mysqlLikeCompare(p1.getFilename(), p2.getFilename()) * way;
			break;

		case TEMPLATE:
			try {
				cmp = StringUtils.mysqlLikeCompare(p1.getTemplate().getName(), p2.getTemplate().getName()) * way;
			} catch (NodeException e) {
				cmp = 0;
			}
			break;

		case FOLDER:
			try {
				cmp = StringUtils.mysqlLikeCompare(p1.getFolder().getName(), p2.getFolder().getName()) * way;
			} catch (NodeException e) {
				cmp = 0;
			}
			break;
			
		case MASTER_NODE:
			try {
				cmp = StringUtils.mysqlLikeCompare(p1.getMasterNodeFolderName(), p2.getMasterNodeFolderName()) * way;
			} catch (NodeException e) {
				cmp = 0;
			}
			break;

		case PRIORITY:
			cmp = (p1.getPriority() - p2.getPriority()) * way;
			break;

		case EXCLUDED:
			try {
				cmp = compareMCExclusion(p1, p2) * way;
			} catch (NodeException e) {
				logger.error("Error while sorting " + p1 + " and " + p2 + " by mc exclusion", e);
			}
			break;
		case DELETED_BY:
			cmp = compareUser(p1, p2, Page::getDeletedBy);
			break;
		case DELETED_DATE:
			cmp = (p1.getDeleted() - p2.getDeleted()) * way;
			break;
		case PATH:
			try {
				String path1 = ModelBuilder.getFolderPath(p1.getFolder());
				String path2 = ModelBuilder.getFolderPath(p2.getFolder());
				cmp = StringUtils.mysqlLikeCompare(path1, path2) * way;
			} catch (NodeException e) {
				cmp = 0;
			}
			break;
		case CREATOR:
			cmp = compareUser(p1, p2, Page::getCreator);
			break;
		case EDITOR:
			cmp = compareUser(p1, p2, Page::getEditor);
			break;
		case PUBLISHER:
			cmp = compareUser(p1, p2, Page::getPublisher);
			break;
		case NODE:
			cmp = compareNode(p1, p2);
			break;
		case CUSTOM_EDIT_DATE:
			cmp = p1.getCustomEDate().compareTo(p2.getCustomEDate()) * way;
			break;
		case CUSTOM_CREATE_DATE:
			cmp = p1.getCustomCDate().compareTo(p2.getCustomCDate()) * way;
			break;
		case CUSTOM_OR_DEFAULT_EDIT_DATE:
			cmp = p1.getCustomOrDefaultEDate().compareTo(p2.getCustomOrDefaultEDate()) * way;
			break;
		case CUSTOM_OR_DEFAULT_CREATE_DATE:
			cmp = p1.getCustomOrDefaultCDate().compareTo(p2.getCustomOrDefaultCDate()) * way;
			break;
		default:
			cmp = 0;
			break;
		}

		if (cmp == 0) {
			cmp = (ObjectTransformer.getInt(p1.getId(), 0) - ObjectTransformer.getInt(p2.getId(), 0)) * way;
		}

		return cmp;
	}
}