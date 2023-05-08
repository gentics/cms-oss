package com.gentics.contentnode.devtools;

import java.nio.file.Path;

import com.gentics.api.lib.exception.NodeException;

/**
 * Container for Main packages
 */
public class MainPackageContainer extends PackageContainer<MainPackageSynchronizer> {
	/**
	 * Create an instance
	 * @param root root path
	 * @throws NodeException
	 */
	protected MainPackageContainer(Path root) throws NodeException {
		super(MainPackageSynchronizer.class, root);
	}
}
