package com.gentics.contentnode.factory;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.object.Node;

/**
 * AutoClosable WastebinFilter
 */
public class WastebinFilter implements AutoCloseable {
	/**
	 * Transaction at start
	 */
	private Transaction t;

	/**
	 * Original filter
	 */
	private Wastebin originalFilter;

	/**
	 * Create an instance, that sets the given filter to the current transaction
	 * @param filter filter
	 * @throws NodeException
	 */
	public WastebinFilter(Wastebin filter) throws NodeException {
		t = TransactionManager.getCurrentTransaction();
		originalFilter = t.setWastebinFilter(filter);
	}

	@Override
	public void close() {
		// reset the original wastebin filter
		t.setWastebinFilter(originalFilter);
	}

	/**
	 * Get the wastebin filter depending on whether including the wastebin was requested, the feature is activated and the user has permission for the wastebin of the given node
	 * @param includeWastebin true if including the wastebin was requested
	 * @param node node
	 * @return wastebin filter
	 * @throws NodeException
	 */
	public static WastebinFilter get(boolean includeWastebin, Node node) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		NodePreferences prefs = t.getNodeConfig().getDefaultPreferences();
		Wastebin wastebin = includeWastebin && prefs.isFeature(Feature.WASTEBIN, node) && t.canWastebin(node) ? Wastebin.INCLUDE : Wastebin.EXCLUDE;
		return wastebin.set();
	}
}
