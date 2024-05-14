package com.gentics.contentnode.factory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.object.ConstructCategory;

/**
 * {@link Transactional} implementation which updates the sort order of {@link ConstructCategory} entities
 */
public class RecalcConstructCategorySortOrderTransactional extends AbstractRecalcSortOrderTransactional {
	/**
	 * Singleton instance
	 */
	protected static RecalcConstructCategorySortOrderTransactional singleton = new RecalcConstructCategorySortOrderTransactional();

	/**
	 * Creator for the singleton
	 */
	private RecalcConstructCategorySortOrderTransactional() {
	}

	/**
	 * Create instance for an optional category.
	 * If the optional is present, the ID and current sort order are put into the transaction attribute
	 * @param constructCategory optional category
	 * @throws NodeException
	 */
	public RecalcConstructCategorySortOrderTransactional(Optional<ConstructCategory> constructCategory) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		if (constructCategory.isPresent()) {
			Map<Integer, Integer> map = getMapFromTransaction(t, true);
			map.put(constructCategory.get().getId(), constructCategory.get().getSortorder());
		}
	}

	@Override
	public Transactional getSingleton(Transaction t) {
		return singleton;
	}

	@Override
	protected Map<Integer, Integer> getCurrentSortOrderMap() throws NodeException {
		return DBUtils.select("SELECT id, sortorder FROM construct_category", rs -> {
			Map<Integer, Integer> tmpMap = new HashMap<>();
			while (rs.next()) {
				tmpMap.put(rs.getInt("id"), rs.getInt("sortorder"));
			}
			return tmpMap;
		});
	}

	@Override
	protected void updateSortOrder(Transaction t, int id, int sortOrder) throws NodeException {
		DBUtils.update("UPDATE construct_category SET sortorder = ? WHERE id = ?", sortOrder, id);
		t.dirtObjectCache(ConstructCategory.class, id);
	}
}
