package com.gentics.contentnode.factory.object;

import java.util.Set;

import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.contentnode.object.Disinheritable;

/**
 * Interface for disinheritable factory objects. It contains methods needed for
 * loading and saving them.
 *
 * @author escitalopram
 *
 */
interface DisinheritableInternal<T> extends Disinheritable<T> {
	/**
	 * Returns the object's representation of its disinherited channel ID set as
	 * currently recorded in the database.
	 *
	 * @return the disinherited channel id set
	 */
	public Set<Integer> getOriginalDisinheritedNodeIds();

	/**
	 * Sets the set of the original disinherited node IDs.
	 *
	 * @param nodeIds
	 *            the new set of node IDs
	 */
	public void setOriginalDisinheritedNodeIds(Set<Integer> nodeIds);

	/**
	 * Sets the last saved state of the "excluded" property
	 *
	 * @param value
	 *            the new value
	 * @throws ReadOnlyException
	 */
	public void setExcluded(boolean value) throws ReadOnlyException;
}
