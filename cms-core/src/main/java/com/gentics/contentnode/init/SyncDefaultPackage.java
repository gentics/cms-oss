package com.gentics.contentnode.init;

import java.nio.file.Path;

import com.gentics.contentnode.utils.ResourcePath;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.devtools.MainPackageSynchronizer;
import com.gentics.contentnode.devtools.SynchronizableNodeObject;
import com.gentics.contentnode.devtools.Synchronizer;
import com.gentics.contentnode.factory.object.UserLanguageFactory;
import com.gentics.contentnode.factory.object.UserLanguageFactory.WithAllLanguages;

/**
 * Initialization job that synchronizes the default package from the filesystem into the CMS
 */
public class SyncDefaultPackage extends InitJob {

	@Override
	public void execute() throws NodeException {
		try (ResourcePath resourcePath = new ResourcePath("/packages/DefaultElements/")) {
			Path path = resourcePath.getPath();

			if (path == null) {
				logger.info("Did not find path to DefaultElements");
				return;
			}

			logger.info(String.format("Synchronizing default elements from devtool package located at '%s'", path));
			// we synchronize for all existing languages, because some languages might get activated later
			try (WithAllLanguages ac = UserLanguageFactory.withAllLanguages()) {
				MainPackageSynchronizer synchronizer = new MainPackageSynchronizer(path, false);
				for (Class<? extends SynchronizableNodeObject> clazz : Synchronizer.CLASSES) {
					logger.info(String.format("Start synchronizing objects of class '%s'", clazz));
					synchronizer.syncAllFromFilesystem(clazz);
				}
			}
			logger.info("Done synchronizing default elements");
		}
	}
}
