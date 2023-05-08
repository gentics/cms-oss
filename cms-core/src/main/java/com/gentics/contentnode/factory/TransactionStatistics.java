package com.gentics.contentnode.factory;

import java.util.HashMap;
import java.util.Map;

import com.gentics.lib.etc.StatsItem;

/**
 * Class encapsulating transaction statistics
 */
public class TransactionStatistics {
	/**
	 * Map of stats Items
	 */
	protected Map<Item, StatsItem> statsItems = new HashMap<Item, StatsItem>(Item.values().length);

	/**
	 * Create an instance
	 */
	public TransactionStatistics() {
		for (Item item : Item.values()) {
			statsItems.put(item, new StatsItem());
		}
	}

	/**
	 * Get the StatsItem
	 * @param item item
	 * @return stats item
	 */
	public StatsItem get(Item item) {
		return statsItems.get(item);
	}

	/**
	 * Available stats items
	 */
	public static enum Item {
		RENDER_PAGE("Rendering pages"),
		RENDER_PAGE_CONTENT("Render page content"),
		RENDER_PAGE_ATTS("Render page attributes"),
		GET_NEXT_PAGE("Get next page"),
		CHECK_INHERITED_SOURCE("Check inherited sources"),
		FETCH_OBJECT("Fetching objects"),
		ACCESS_CACHE("Accessing Cache"),
		ACCESS_DB("Accessing DB"),
		MC_FALLBACK("Multichannelling Fallback"),
		QUEUE_FULL_WAIT("Waiting due to full queue");

		/**
		 * Description
		 */
		protected String description;

		/**
		 * Create an instance
		 * @param description description
		 */
		private Item(String description) {
			this.description = description;
		}

		/**
		 * Get the description
		 * @return description
		 */
		public String getDescription() {
			return description;
		}
	}
}
