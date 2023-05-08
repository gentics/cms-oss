package com.gentics.contentnode.object.utility;

import java.util.Comparator;

import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.rest.model.request.SortOrder;
import com.gentics.contentnode.rest.model.request.TemplateSortAttribute;
import com.gentics.lib.etc.StringUtils;

/**
 * Comparator for sorting CNTemplates
 */
public class CNTemplateComparator implements Comparator<Template> {
	protected TemplateSortAttribute sortBy;
	protected SortOrder sortOrder;

	public CNTemplateComparator(TemplateSortAttribute sortBy, SortOrder sortOrder) {
		this.sortBy = sortBy != null ? sortBy : TemplateSortAttribute.name;
		this.sortOrder = sortOrder != null ? sortOrder : SortOrder.asc;
	}

	/* (non-Javadoc)
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	public int compare(Template t1, Template t2) {
		int compare = 0;

		switch (sortBy) {
		case name:
			compare = StringUtils.mysqlLikeCompare(t1.getName(), t2.getName());
			break;

		default:
			compare = 0;
			break;
		}

		if (sortOrder == SortOrder.desc || sortOrder == SortOrder.DESC) {
			compare = -compare;
		}

		return compare;
	}
}