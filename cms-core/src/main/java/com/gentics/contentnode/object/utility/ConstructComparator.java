package com.gentics.contentnode.object.utility;

import java.util.Comparator;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.rest.model.request.ConstructSortAttribute;
import com.gentics.contentnode.rest.model.request.SortOrder;
import com.gentics.lib.etc.StringUtils;

public class ConstructComparator implements Comparator<Construct> {
	protected ConstructSortAttribute sortBy;
	protected SortOrder sortOrder;

	public ConstructComparator(ConstructSortAttribute sortBy, SortOrder sortOrder) {
		this.sortBy = sortBy;
		this.sortOrder = sortOrder;
	}

	public int compare(com.gentics.contentnode.object.Construct construct1,
			com.gentics.contentnode.object.Construct construct2) {
		if (construct1 == null && construct2 == null) {
			return 0;
		} else if (construct1 == null) {
			return (sortOrder == SortOrder.asc || sortOrder == SortOrder.ASC) ? -1 : 1;
		} else if (construct2 == null) {
			return (sortOrder == SortOrder.asc || sortOrder == SortOrder.ASC) ? 1 : -1;
		}
		int compare = 0;

		switch (sortBy) {
		case category:
			try {
				com.gentics.contentnode.object.ConstructCategory category1 = construct1.getConstructCategory();
				com.gentics.contentnode.object.ConstructCategory category2 = construct2.getConstructCategory();

				if (category1 == null) {
					compare = (category2 == null) ? 0 : 1;
				} else if (category2 == null) {
					compare = -1;
				} else {
					compare = category1.getSortorder() - category2.getSortorder();
					if (compare == 0) {
						compare = StringUtils.mysqlLikeCompare(category1.getName().toString(),
								category2.getName().toString());
					}
				}
				break;
			} catch (NodeException e) {
				return 0;
			}

		case description:
			compare = StringUtils.mysqlLikeCompare(ObjectTransformer.getString(construct1.getDescription(), ""),
					ObjectTransformer.getString(construct2.getDescription(), ""));
			break;

		case keyword:
			compare = StringUtils.mysqlLikeCompare(construct1.getKeyword(), construct2.getKeyword());
			break;

		case name:
			compare = StringUtils.mysqlLikeCompare(ObjectTransformer.getString(construct1.getName(), ""),
					ObjectTransformer.getString(construct2.getName(), ""));
			break;

		default:
			return 0;
		}

		if (compare == 0 && sortBy != ConstructSortAttribute.name) {
			compare = StringUtils.mysqlLikeCompare(ObjectTransformer.getString(construct1.getName(), ""),
					ObjectTransformer.getString(construct2.getName(), ""));
		}

		if (sortOrder == SortOrder.desc || sortOrder == SortOrder.DESC) {
			compare = -compare;
		}

		return compare;
	}
}
