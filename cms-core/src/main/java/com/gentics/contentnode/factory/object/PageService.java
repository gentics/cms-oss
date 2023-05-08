package com.gentics.contentnode.factory.object;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Page;

/**
 * Interface for {@link ExtensibleObjectService} implementations for {@link Page}s containing
 * empty default implementations.
 */
public interface PageService extends ExtensiblePublishableObjectService<Page> {
	@Override
	default void onSave(Page object, boolean isNew, boolean contentModified, int editorId) throws NodeException {
	}

	@Override
	default void onDelete(Page object, boolean wastebin, int userId) throws NodeException {
	}

	@Override
	default void onPublish(Page object, boolean wasOnline, int userId) throws NodeException {
	}

	@Override
	default void onTakeOffline(Page object, boolean wasOnline, int userId) throws NodeException {
	}
}
