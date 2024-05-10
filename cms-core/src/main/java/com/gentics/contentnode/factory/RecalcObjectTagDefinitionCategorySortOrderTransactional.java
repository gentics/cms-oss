package com.gentics.contentnode.factory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.object.ObjectTagDefinitionCategory;

/**
 * {@link Transactional} implementation which updates the sort order of {@link ObjectTagDefinitionCategory} entities
 */
public class RecalcObjectTagDefinitionCategorySortOrderTransactional extends AbstractRecalcSortOrderTransactional {
	/**
	 * Singleton instance
	 */
	protected static RecalcObjectTagDefinitionCategorySortOrderTransactional singleton = new RecalcObjectTagDefinitionCategorySortOrderTransactional();

	/**
	 * Creator for the singleton
	 */
	private RecalcObjectTagDefinitionCategorySortOrderTransactional() {
	}

	/**
	 * Create instance for an optional category.
	 * If the optional is present, the ID and current sort order are put into the transaction attribute
	 * @param category optional category
	 * @throws NodeException
	 */
	public RecalcObjectTagDefinitionCategorySortOrderTransactional(Optional<ObjectTagDefinitionCategory> category) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		if (category.isPresent()) {
			Map<Integer, Integer> map = getMapFromTransaction(t, true);
			map.put(category.get().getId(), category.get().getSortorder());
		}
	}

	@Override
	public Transactional getSingleton(Transaction t) {
		return singleton;
	}

	@Override
	protected Map<Integer, Integer> getCurrentSortOrderMap() throws NodeException {
		return DBUtils.select("SELECT id, sortorder FROM objprop_category", rs -> {
			Map<Integer, Integer> tmpMap = new HashMap<>();
			while (rs.next()) {
				tmpMap.put(rs.getInt("id"), rs.getInt("sortorder"));
			}
			return tmpMap;
		});
	}

	@Override
	protected void updateSortOrder(Transaction t, int id, int sortOrder) throws NodeException {
		DBUtils.update("UPDATE objprop_category SET sortorder = ? WHERE id = ?", sortOrder, id);
		t.dirtObjectCache(ObjectTagDefinitionCategory.class, id);
	}
}
