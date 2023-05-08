package com.gentics.contentnode.publish;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Implementation of {@link FileUtils} that uses Java methods for creation of links.
 */
public class FileUtilsImpl implements FileUtils {

	@Override
	public boolean supportsSymlinks() {
		return true;
	}

	@Override
	public boolean createSymlink(File src, File dest) throws IOException {
		try {
			Files.createSymbolicLink(dest.toPath(), src.toPath());
			return true;
		} catch (UnsupportedOperationException e) {
			return false;
		}
	}

	@Override
	public boolean createLink(File src, File dest) throws IOException {
		try {
			Files.createLink(dest.toPath(), src.toPath());
			return true;
		} catch (UnsupportedOperationException e) {
			return false;
		}
	}

	@Override
	public boolean createCopy(File src, File dest) throws IOException {
		try {
			Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
			return true;
		} catch (UnsupportedOperationException e) {
			return false;
		}
	}

	@Override
	public boolean deleteDirectory(File directory) throws IOException {
		org.apache.commons.io.FileUtils.deleteDirectory(directory);
		return true;
	}
}
