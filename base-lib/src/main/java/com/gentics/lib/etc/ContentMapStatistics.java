package com.gentics.lib.etc;

import java.util.HashMap;
import java.util.Map;

/**
 * Class encapsulating statistics for publishing into a ContentMap
 */
public class ContentMapStatistics {
	/**
	 * Map of stats Items
	 */
	protected Map<Item, StatsItem> statsItems = new HashMap<Item, StatsItem>(Item.values().length);

	/**
	 * Create an instance
	 */
	public ContentMapStatistics() {
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
		PREPARE_DATA("prepare data"),
		PUBLISH_HANDLER("publish handlers"),
		WRITE_FOLDER("writing folder"),
		WRITE_FILE("writing files"),
		WRITE_PAGE("writing pages"),
		PREPARE("Prepare changeable"),
		EXISTENCE("Check existence");

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
