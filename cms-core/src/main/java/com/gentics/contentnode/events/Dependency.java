/*
 * @author norbert
 * @date 18.01.2007
 * @version $Id: Dependency.java,v 1.3.8.1 2011-02-07 14:56:04 norbert Exp $
 */
package com.gentics.contentnode.events;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Node;

/**
 * Interface representing dependencies.
 * TODO: document types of dependencies here
 */
public interface Dependency extends Comparable<Dependency> {

	/**
	 * Get source of the dependency
	 * @return dependency source
	 * @throws NodeException
	 */
	DependencyObject getSource() throws NodeException;

	/**
	 * Get dependent objects
	 * @return dependent objects
	 * @throws NodeException
	 */
	DependencyObject getDependent() throws NodeException;

	/**
	 * Get the source property. This may be null when the dependency is no
	 * "property" dependency
	 * @return name of the source property or null
	 * @throws NodeException
	 */
	String getSourceProperty() throws NodeException;

	/**
	 * Store this dependency in the database
	 * @throws NodeException
	 */
	void store() throws NodeException;

	/**
	 * Update the dependent properties
	 * @throws NodeException
	 */
	void update() throws NodeException;

	/**
	 * Trigger the dependency with the given event mask
	 * @param eventMask event mask
	 * @param depth depth of the triggered dependency
	 * @param node node to trigger this dependency for (may be null to trigger for all nodes this dependency belongs to)
	 * @throws NodeException
	 */
	void triggerDependency(int eventMask, int depth, Node node) throws NodeException;

	/**
	 * Check whether the dependency is new (is not yet stored in the db)
	 * @return true for new dependencies, false for existing ones
	 */
	boolean isNew();

	/**
	 * Check whether the dependency is old (shall be removed from the db)
	 * @return true for old dependencies, false for new or still existing ones
	 */
	boolean isOld();

	/**
	 * Check whether the dependency has been modified. A dependency is modified if the list of dependent properties has changed
	 * @return true for a modified dependency, false for an unmodified dependency
	 */
	boolean isModified();

	/**
	 * Set this dependency to be still existing in the db (subsequent calls to
	 * {@link #isOld()} will return false).
	 */
	void setExisting();

	/**
	 * Get the id of the dependency
	 * @return id of the dependency
	 */
	int getId();

	/**
	 * Get the dependency mask
	 * @return dependency mask
	 */
	int getMask();

	/**
	 * Get the (sorted) list of dependent properties
	 * @return list of dependent properties
	 */
	Map<String, Set<Integer>> getDependentProperties();

	/**
	 * Get the (sorted) list of currently stored dependent properties
	 * @return list of dependent properties
	 */
	Map<String, Set<Integer>> getStoredDependentProperties();

	/**
	 * Add the given dependent property
	 * @param channelId channel ID
	 * @param property dependent property
	 */
	void addDependentProperty(int channelId, String property);

	/**
	 * Add the given channel id to the list of channel Ids of this dependency (if not done so before)
	 * @param channelId channel id
	 */
	void addChannelId(int channelId);

	/**
	 * Add the given list of channels (comma separated list)
	 * @param channels channels to add as comma separated list
	 */
	void addChannels(String channels);

	/**
	 * Get the list of channel ids for this dependency
	 * @return list of channel ids
	 */
	List<Integer> getChannelIds();

	/**
	 * Remove the given channel id from the list of channel Ids of this dependency
	 * @param channelId channel id
	 */
	void removeChannelId(int channelId);

	/**
	 * Merge the given dependency into this one
	 * @param dep dependency to merge
	 */
	void merge(Dependency dep);

	/**
	 * Preserve all dependent properties for other channels than the given by copying from the stored dependent properties to the "active"
	 * @param channelId channel Id
	 */
	void preserveOtherDependentProperties(Integer channelId);
}
