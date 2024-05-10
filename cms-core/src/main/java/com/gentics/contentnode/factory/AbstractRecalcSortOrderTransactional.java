package com.gentics.contentnode.factory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.lib.log.NodeLogger;

/**
 * Abstract base class for {@link Transactional} implementations, which recalculates the sort order of all elements of a specific type.
 * The rules are
 * <ol>
 * <li>Sort orders must start with 1</li>
 * <li>Sort orders must be a sequence, without duplicates</li>
 * <li>Sort orders of elements, which were explicitly changed during the transaction have priority over other sort orders</li>
 * </ol>
 */
public abstract class AbstractRecalcSortOrderTransactional extends AbstractTransactional {
	protected final static NodeLogger logger = NodeLogger.getNodeLogger(AbstractRecalcSortOrderTransactional.class);

	@Override
	public int getThreshold(Transaction t) {
		return 0;
	}

	@Override
	public void onDBCommit(Transaction t) throws NodeException {
		// get the ids and sort orders of entities which were modified during the transaction
		Map<Integer, Integer> mapOfModifiedEntities = getMapFromTransaction(t, false);

		// reorganize the data as list of pairs (id, sortorder), sorted by sortorder
		List<Pair<Integer, Integer>> pairs = mapOfModifiedEntities.entrySet().stream().map(entry -> Pair.of(entry.getKey(), entry.getValue())).collect(Collectors.toList());
		pairs.sort((p1, p2) -> Integer.compare(p1.getRight(), p2.getRight()));

		// get the current stored sort orders
		Map<Integer, Integer> sortOrderMap = getCurrentSortOrderMap();

		// reorganize as sorted list of IDs (sorted by sort order), after removing the IDs of elements, which were modified
		List<Integer> currentIds = new ArrayList<>(sortOrderMap.keySet());
		currentIds.removeAll(mapOfModifiedEntities.keySet());
		currentIds.sort((id1, id2) -> Integer.compare(sortOrderMap.get(id1), sortOrderMap.get(id2)));

		// now insert the modified entity IDs at their requested sort order positions, moving all other entities back
		for (Pair<Integer, Integer> pair : pairs) {
			int id = pair.getLeft();
			int sortOrder = pair.getRight();

			int insertPosition = Math.max(0, Math.min(sortOrder - 1, currentIds.size()));
			currentIds.add(insertPosition, id);
		}

		// finally, check for which entities the sort order needs to be updated and update
		AtomicInteger sortOrderCounter = new AtomicInteger(1);
		for (int id : currentIds) {
			int sortOrder = sortOrderCounter.getAndIncrement();
			int currentSortOrder = sortOrderMap.get(id);

			if (sortOrder != currentSortOrder) {
				updateSortOrder(t, id, sortOrder);
			}
		}
	}

	@Override
	public boolean onTransactionCommit(Transaction t) {
		return false;
	}

	/**
	 * Get the map of entity ids to requested sort orders for entities, which were changed during the transaction (map is stored as transaction attribute)
	 * @param t transaction
	 * @param createIfMissing true to create the attribute, it it is missing, false to return an empty map in such cases
	 * @return map (never null)
	 */
	@SuppressWarnings("unchecked")
	protected Map<Integer, Integer> getMapFromTransaction(Transaction t, boolean createIfMissing) {
		String attributeName = String.format("%s.map", getClass().getSimpleName());
		if (createIfMissing) {
			return (Map<Integer, Integer>) t.getAttributes().computeIfAbsent(attributeName, key -> new HashMap<>());
		} else {
			return (Map<Integer, Integer>) t.getAttributes().getOrDefault(attributeName, Collections.emptyMap());
		}
	}

	/**
	 * Get a map of IDs to sort orders, which is currently stored in the DB
	 * @return map of current sort orders per ID
	 * @throws NodeException
	 */
	protected abstract Map<Integer, Integer> getCurrentSortOrderMap() throws NodeException;

	/**
	 * Update the sort order of the element with given ID
	 * @param t transaction
	 * @param id entity id
	 * @param sortOrder sort order to set
	 * @throws NodeException
	 */
	protected abstract void updateSortOrder(Transaction t, int id, int sortOrder) throws NodeException;
}
