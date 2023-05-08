package com.gentics.contentnode.object;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.gentics.lib.i18n.CNI18nString;

/**
 * Class for universally usable object counters.
 * Provides methods to increase the count of objects and the get the current counters
 */
public class ObjectCounter {

	/**
	 * I18nStrings for singular and plural of specific objects
	 */
	protected static Map<Class<? extends NodeObject>, String[]> i18nKeyMap = new LinkedHashMap<Class<? extends NodeObject>, String[]>();

	static {
		i18nKeyMap.put(Page.class, new String[] { "page", "pages"});
		i18nKeyMap.put(File.class, new String[] { "file", "files"});
		i18nKeyMap.put(ImageFile.class, new String[] { "image", "images"});
		i18nKeyMap.put(Template.class, new String[] { "template", "templates"});
		i18nKeyMap.put(Folder.class, new String[] { "folder", "folders"});
	}

	/**
	 * Map of currently used counters
	 */
	protected Map<Class<? extends NodeObject>, Counter> counters = new HashMap<Class<? extends NodeObject>, Counter>();

	/**
	 * Increase the counter for objects of the given class
	 * @param clazz class
	 */
	public void inc(Class<? extends NodeObject> clazz) {
		Counter counter = counters.get(clazz);

		if (counter == null) {
			counter = new Counter();
			counters.put(clazz, counter);
		}

		counter.inc();
	}

	/**
	 * Get the current count for objects of the given class
	 * @param clazz class
	 * @return current count
	 */
	public int getCount(Class<? extends NodeObject> clazz) {
		Counter counter = counters.get(clazz);

		if (counter == null) {
			return 0;
		} else {
			return counter.getValue();
		}
	}

	/**
	 * Check whether any of the counter has a positive value
	 * @return true if any of the counter has a positive value, false if not
	 */
	public boolean hasCount() {
		for (Counter counter : counters.values()) {
			if (counter.getValue() > 0) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Get an i18n String for the counts (which are not 0).
	 * The output will be something like: "1 Page, 2 Templates, 3 Folders."
	 * @return i18n String
	 */
	public String getI18nString() {
		StringBuffer i18nBuf = new StringBuffer();

		if (hasCount()) {
			for (Map.Entry<Class<? extends NodeObject>, String[]> entry : i18nKeyMap.entrySet()) {
				int count = getCount(entry.getKey());

				if (count == 1) {
					if (i18nBuf.length() > 0) {
						i18nBuf.append(", ");
					}
					i18nBuf.append(count).append(" ").append(new CNI18nString(entry.getValue()[0]).toString());
				} else if (count > 1) {
					if (i18nBuf.length() > 0) {
						i18nBuf.append(", ");
					}
					i18nBuf.append(count).append(" ").append(new CNI18nString(entry.getValue()[1]).toString());
				}
			}
			i18nBuf.append(".");
		}
		return i18nBuf.toString();
	}

	/**
	 * Counter class
	 */
	protected static class Counter {

		/**
		 * Internal value
		 */
		protected int value = 0;

		/**
		 * Increase the value by one
		 */
		public void inc() {
			value++;
		}

		/**
		 * Get the current value
		 * @return value
		 */
		public int getValue() {
			return value;
		}
	}
}
