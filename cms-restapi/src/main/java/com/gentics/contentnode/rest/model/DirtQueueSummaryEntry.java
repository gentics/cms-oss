package com.gentics.contentnode.rest.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * REST Model of an entry in the dirt queue summary
 */
public class DirtQueueSummaryEntry implements Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 4061381600209736219L;

	private String label;

	private int count;

	/**
	 * Action label
	 * @return label
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * Set the label
	 * @param label label
	 * @return fluent API
	 */
	public DirtQueueSummaryEntry setLabel(String label) {
		this.label = label;
		return this;
	}

	/**
	 * Number of subsequent entries of the same type
	 * @return count
	 */
	public int getCount() {
		return count;
	}

	/**
	 * Set entry count
	 * @param count count
	 * @return fluent API
	 */
	public DirtQueueSummaryEntry setCount(int count) {
		this.count = count;
		return this;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof DirtQueueSummaryEntry) {
			DirtQueueSummaryEntry other = (DirtQueueSummaryEntry) obj;
			return Objects.equals(label, other.label) && Objects.equals(count, other.count);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(label, count);
	}

	@Override
	public String toString() {
		return String.format("%s: %d", label, count);
	}
}
