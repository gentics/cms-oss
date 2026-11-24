package com.gentics.contentnode.factory.object;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.OpResult;

import java.util.Collection;

/**
 * Interface for {@link ExtensibleObjectService} implementations for {@link Folder}s containing
 * empty default implementations.
 */
public interface FolderService extends ExtensibleObjectService<Folder> {
	@Override
	default void onSave(Folder object, boolean isNew, boolean contentModified, int editorId) throws NodeException {
	}

	@Override
	default void onDelete(Folder object, boolean wastebin, int userId) throws NodeException {
	}

	/**
	 * Check whether the given folder (including the folder structure below) may be moved into the target folder, which belongs to another node.
	 * This is called before anything is changed, so implementations can veto the move by returning a failed {@link OpResult}.
	 * @param folder folder to be moved
	 * @param target target folder (in another node)
	 * @param subFolderIds IDs of the moved folder and all its (master) subfolders
	 * @return {@link OpResult#OK} if the move is allowed, a failed {@link OpResult} otherwise
	 * @throws NodeException
	 */
	default OpResult checkMoveToOtherNode(Folder folder, Folder target, Collection<Integer> subFolderIds) throws NodeException {
		return OpResult.OK;
	}
}
