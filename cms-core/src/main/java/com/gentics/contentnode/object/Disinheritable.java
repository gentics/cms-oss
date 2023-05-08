package com.gentics.contentnode.object;

import java.util.Set;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.rest.model.PageLanguageCode;

/**
 * Node objects implementing this interface can be generally excluded from
 * multichannelling inheritance or disinherit certain channels. Both kinds of
 * restrictions are per-object.
 *
 * @author escitalopram
 *
 */
public interface Disinheritable<T> extends LocalizableNodeObject<T> {
	/**
	 * Returns the exclusion state for multichannelling inheritance.
	 *
	 * @return true iff object is excluded
	 */
	public boolean isExcluded();

	/**
	 * Indicates whether this object is disinherited by default in newly
	 * created channels.
	 *
	 * @return <code>true</code> if this object is disinherited by default
	 *		in new channels, <code>false</code> otherwise.
	 * @throws NodeException On internal errors
	 */
	public boolean isDisinheritDefault() throws NodeException;

	/**
	 * Sets whether this object should be disinherited by default in newly
	 * created channels.
	 *
	 * @param value If <code>true</code> the object will be disinherited in
	 *		new channels.
	 * @param recursive If <code>true</code> sets the specified value for
	 *		all successor objects.
	 * @throws NodeException On errors while changing the flag.
	 */
	public void setDisinheritDefault(boolean value, boolean recursive) throws NodeException;

	/**
	 * Returns the set of disinherited channels for this object. Only directly
	 * disinherited channels are contained.
	 *
	 * @return the set of inherited channels
	 * @throws NodeException
	 */
	public Set<Node> getDisinheritedChannels() throws NodeException;

	/**
	 * Returns the filename (as used to publish to the filesystem) of the
	 * object, if available.
	 *
	 * @return the filename of this object, or null if it doesn't have a
	 *         filename
	 */
	public String getFilename();

	/**
	 * Return the full publish path (excluding the filename) of the object, if available
	 * @param trailingSlash true to add a trailing slash
	 * @return publish path
	 * @throws NodeException
	 */
	default public String getFullPublishPath(boolean trailingSlash) throws NodeException {
		return getFullPublishPath(trailingSlash, true);
	}

	/**
	 * Return the full publish path (excluding the filename) of the object, if available
	 * @param trailingSlash true to add a trailing slash
	 * @param includeNodePublishDir true to include the node publish directory
	 * @return publish path
	 * @throws NodeException
	 */
	public String getFullPublishPath(boolean trailingSlash, boolean includeNodePublishDir) throws NodeException;

	/**
	 * Return the full publish path (excluding the filename) of the object, that it has for the given folder, if available
	 * @param folder folder
	 * @param trailingSlash true to add a trailing slash
	 * @param includeNodePublishDir true to include the node publish directory
	 * @return publish path
	 * @throws NodeException
	 */
	default public String getFullPublishPath(Folder folder, boolean trailingSlash, boolean includeNodePublishDir) throws NodeException {
		return getFullPublishPath(folder, trailingSlash, getOwningNode().getPageLanguageCode(), includeNodePublishDir);
	}

	/**
	 * Return the full publish path (excluding the filename) of the object, that it has for the given folder, if available
	 * @param folder folder
	 * @param trailingSlash true to add a trailing slash
	 * @param pageLanguageCode page language code setting
	 * @param includeNodePublishDir true to include the node publish directory
	 * @return publish path
	 * @throws NodeException
	 */
	public String getFullPublishPath(Folder folder, boolean trailingSlash, PageLanguageCode pageLanguageCode, boolean includeNodePublishDir) throws NodeException;

	/**
	 * Saves multichannelling restriction states, such as multichannelling exclusion and channel disinheritings.
	 * If the restriction state is changed for a folder, subobjects will also be restricted, if necessary.
	 * When the flag recursive is true, subobjects will get the same settings set (which might also remove restrictions)
	 * @param excluded whether the object should be excluded from multichannelling
	 * @param disinheritedNodes set of disinherited nodes
	 * @param recursive true to set the restrictions recursively (if set on a folder), false if not
	 * @throws NodeException
	 */
	public void changeMultichannellingRestrictions(boolean excluded, Set<Node> disinheritedNodes, boolean recursive) throws NodeException;

	/**
	 * Get the node this object belongs to (in the scope of the current transaction channel)
	 * @return node of the object
	 * @throws NodeException
	 */
	public Node getNode() throws NodeException;
}
