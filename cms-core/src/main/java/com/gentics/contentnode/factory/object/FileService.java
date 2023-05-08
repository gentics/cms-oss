package com.gentics.contentnode.factory.object;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.File;

/**
 * Interface for {@link ExtensibleObjectService} implementations for {@link File}s containing
 * empty default implementations.
 */
public interface FileService extends ExtensibleObjectService<File> {
	@Override
	default void onSave(File object, boolean isNew, boolean contentModified, int editorId) throws NodeException {
	}

	@Override
	default void onDelete(File object, boolean wastebin, int userId) throws NodeException {
	}
}
