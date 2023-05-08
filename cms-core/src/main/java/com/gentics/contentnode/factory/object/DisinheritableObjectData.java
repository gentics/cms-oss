package com.gentics.contentnode.factory.object;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents data of an object needed to describe its multichannelling
 * inheritance state.
 *
 * @author escitalopram
 *
 */
public class DisinheritableObjectData {
	private final boolean excluded;

	/**
	 * Returns whether the object represented by this instance is excluded from
	 * multichannelling.
	 *
	 * @return true iff object is excluded
	 */
	public boolean isExcluded() {
		return excluded;
	}

	/**
	 * Returns the set of disinherited channel IDs for the represented
	 * object.
	 *
	 * @return the set
	 */
	public Set<Integer> getDisinheritedChannelIds() {
		return disinheritedChannelIds;
	}

	private final Set<Integer> disinheritedChannelIds = new HashSet<>();

	/**
	 * Creates a new instance.
	 *
	 * @param excluded
	 *            whether the object represented is excluded from
	 *            multichannelling
	 */
	public DisinheritableObjectData(boolean excluded) {
		this.excluded = excluded;
	}

	/**
	 * Adds a channel ID to the list of disinherited channels.
	 *
	 * @param id
	 *            the channel ID to add
	 */
	public void addDisinheritedChannelId(int id) {
		disinheritedChannelIds.add(id);
	}
}
