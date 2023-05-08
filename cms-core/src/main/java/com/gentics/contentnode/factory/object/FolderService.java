package com.gentics.contentnode.factory.object;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Folder;

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
}
