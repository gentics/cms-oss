package com.gentics.contentnode.publish;

import java.util.HashMap;
import java.util.Map;

import com.gentics.lib.etc.StatsItem;

/**
 * Statistics for PublishablePages
 */
public class PublishablePageStatistics {
	/**
	 * Map of stats Items
	 */
	protected Map<Item, StatsItem> statsItems = new HashMap<Item, StatsItem>(Item.values().length);

	/**
	 * Create an instance
	 */
	public PublishablePageStatistics() {
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
		GET("Get PublishablePage instance"),
		CACHE("Access PublishablePage cache"),
		MODEL("Create PublishablePage model"),
		CREATE("Create PublishablePage instance"),
		PUT("Put PublishablePage into cache");

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
