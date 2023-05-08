package com.gentics.lib.datasource.mccr;

import java.util.HashMap;
import java.util.Map;

import com.gentics.lib.etc.StatsItem;

public class MCCRStats {
	/**
	 * Map of stats Items
	 */
	protected Map<Item, StatsItem> statsItems = new HashMap<Item, StatsItem>(Item.values().length);

	/**
	 * Create an instance
	 */
	public MCCRStats() {
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
		UPDATEOBJ("Update object"),
		STOREATTR("Store attribute"),
		BATCHEDSQL("Batched SQL");

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
