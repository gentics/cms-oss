package com.gentics.contentnode.factory.object;

import java.util.function.Predicate;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.lib.db.SimpleResultProcessor;
import com.gentics.lib.db.SimpleResultRow;
import com.gentics.lib.db.TableVersion;

/**
 * Extension to {@link TableVersion} that filters the returned data to be versioned
 */
public class FilteringTableVersion extends TableVersion {
	/**
	 * Filter
	 */
	protected Predicate<? super SimpleResultRow> filter;

	/**
	 * Create instance
	 */
	public FilteringTableVersion() {
		super(false);
	}

	/**
	 * Set the given filter
	 * @param filter filter
	 * @return fluent API
	 */
	public FilteringTableVersion filter(Predicate<? super SimpleResultRow> filter) {
		this.filter = filter;
		return this;
	}

	@Override
	public SimpleResultProcessor getVersionData(Object[] idData, int time) throws NodeException {
		SimpleResultProcessor data = super.getVersionData(idData, time);
		// if a filter is set and we are getting the current data, apply the filter
		if (filter != null && time < 0) {
			data.asList().removeIf(filter);
		}
		return data;
	}

	@Override
	public SimpleResultProcessor getVersionData(Object[] idData, int time, boolean sortIds,
			boolean addNodeversionTimestamps, boolean addNonVersionedData) throws NodeException {
		SimpleResultProcessor data = super.getVersionData(idData, time, sortIds, addNodeversionTimestamps, addNonVersionedData);
		// if a filter is set and we are getting the current data, apply the filter
		if (filter != null && time < 0) {
			data.asList().removeIf(filter);
		}
		return data;
	}
}
