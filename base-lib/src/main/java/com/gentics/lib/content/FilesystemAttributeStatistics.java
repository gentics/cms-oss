package com.gentics.lib.content;

import java.util.HashMap;
import java.util.Map;

import com.gentics.lib.etc.StatsItem;

/**
 * Class for encapsulating filesystem attribute statistics
 */
public class FilesystemAttributeStatistics {
	/**
	 * Map of stats Items
	 */
	protected Map<Item, StatsItem> statsItems = new HashMap<Item, StatsItem>(Item.values().length);

	/**
	 * Create an instance
	 */
	public FilesystemAttributeStatistics() {
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
		SAVE("Save filesystem attributes"),
		REUSE("Find reusable fs attr file"),
		LINK("Create fs attr Hardlink"),
		FILE("Write fs attr file"),
		CHECK("Check file existence"),
		MD5("Determine md5sum");

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
