package com.gentics.contentnode.object.utility;

import java.util.Comparator;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.lib.etc.StringUtils;


/**
 * Comparator for sorting templates
 * @author norbert
 */
public class TemplateComparator extends AbstractComparator implements Comparator<com.gentics.contentnode.rest.model.Template> {

	/**
	 * Create an new TemplateComparator
	 * @param attribute the sorted attribute
	 * @param way sortway
	 */
	public TemplateComparator(String attribute, String way) {
		super(attribute, way);
	}

	/* (non-Javadoc)
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	public int compare(com.gentics.contentnode.rest.model.Template t1, com.gentics.contentnode.rest.model.Template t2) {
		if (t1 == null && t2 == null) {
			return 0;
		} else if (t1 == null) {
			return -way;
		} else if (t2 == null) {
			return way;
		}

		int cmp = 0;

		switch (attribute) {
		case NAME:
			cmp = StringUtils.mysqlLikeCompare(t1.getName(), t2.getName()) * way;
			break;

		case MASTER_NODE:
			cmp = StringUtils.mysqlLikeCompare(t1.getMasterNode(), t2.getMasterNode()) * way;
			break;

		default:
			cmp = 0;
			break;
		}

		if (cmp == 0) {
			cmp = (ObjectTransformer.getInt(t1.getId(), 0) - ObjectTransformer.getInt(t2.getId(), 0)) * way;
		}

		return cmp;
	}
}
