package com.gentics.contentnode.object;

import java.util.Collection;
import java.util.Optional;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;

/**
 * Marker for node objects, that can take part in the content staging.
 * 
 * @see Feature#CONTENT_STAGING
 * 
 * @author plyhun
 *
 */
public interface StageableNodeObject extends NodeObject {

	/**
	 * Get the stageable name suffix for this node object type.
	 * 
	 * @return
	 */
	String getSuffix();

	/**
	 * Get the closest node folder of this stageable object. Folders return self.
	 * @return 
	 * @throws NodeException 
	 */
	Folder getFolder() throws NodeException;

	/**
	 * Set the parent folder. For new objects, this will simply set the folderId (or motherId), existing objects will be properly moved into the parent folder (in the node)
	 * @param node node
	 * @param parent parent folder
	 * @throws NodeException
	 */
	void setFolder(Node node, Folder parent) throws NodeException;

	/**
	 * Return the object's channel supported representation, if available
	 * @return 
	 * @throws NodeException 
	 */
	default Optional<StageableChanneledNodeObject> maybeHasChannels() throws NodeException {
		return Optional.empty();
	}

	/**
	 * Return true if the object serves as a container for other node objects
	 * @return 
	 */
	default boolean isContainer() {
		return false;
	}

	/**
	 * Return non empty Optional with self, if self is a File.
	 * 
	 * @return
	 */
	default Optional<File> maybeBinary() {
		return Optional.empty();
	}

	/**
	 * Return non empty Optional with self, if self has versions to stage.
	 * @return
	 */
	default Optional<StageableVersionedNodeObject> maybeVersioned() {
		return Optional.empty();
	}

	/**
	 * If a node object considers different variants, that need to be processed along, this returns non-empty Optional with variants container.
	 */
	default Optional<Collection<? extends StageableNodeObject>> maybeGetVariants() throws NodeException {
		return Optional.empty();
	}
}
