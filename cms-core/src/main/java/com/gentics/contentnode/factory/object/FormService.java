package com.gentics.contentnode.factory.object;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Form;

/**
 * Interface for {@link ExtensibleObjectService} implementations for {@link Form}s containing
 * empty default implementations.
 */
public interface FormService extends ExtensiblePublishableObjectService<Form> {
	@Override
	default void onSave(Form object, boolean isNew, boolean contentModified, int editorId) throws NodeException {
	}

	@Override
	default void onDelete(Form object, boolean wastebin, int userId) throws NodeException {
	}

	@Override
	default void onPublish(Form object, boolean wasOnline, int userId) throws NodeException {
	}

	@Override
	default void onTakeOffline(Form object, boolean wasOnline, int userId) throws NodeException {
	}
}
