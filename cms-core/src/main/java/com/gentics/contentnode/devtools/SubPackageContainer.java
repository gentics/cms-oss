package com.gentics.contentnode.devtools;

import java.nio.file.Path;

import com.gentics.api.lib.exception.NodeException;

/**
 * Container for Sub packages
 */
public class SubPackageContainer extends PackageContainer<SubPackageSynchronizer> {
	/**
	 * Create an instance
	 * @param root root path
	 * @throws NodeException
	 */
	protected SubPackageContainer(Path root) throws NodeException {
		super(SubPackageSynchronizer.class, root);
	}
}
