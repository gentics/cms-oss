package com.gentics.contentnode.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.regex.Matcher;

import org.eclipse.jetty.util.URIUtil;

import com.gentics.contentnode.devtools.PackageSynchronizer;
import com.gentics.contentnode.runtime.ConfigurationValue;

/**
 * Variant of the {@link PackageRewriteRule}, which only rewrites the request, if the requested file exists in the files directory of the devtool package.
 * The regex must contain two groups, the first one matching the package name, the second one matching the path of the file within the files directory.
 * If the file does not exist, the rule does not apply, so that the request can be handled by another servlet (e.g. proxied to a portal).
 */
public class PackageFileRewriteRule extends PackageRewriteRule {
	/**
	 * Create an instance
	 * @param regex regex, which must contain two groups (package name and file path)
	 * @param replacement replacement
	 */
	public PackageFileRewriteRule(String regex, String replacement) {
		super(regex, replacement);
	}

	@Override
	public Handler apply(Handler input, Matcher matcher) throws IOException {
		if (!packageFileExists(URIUtil.decodePath(matcher.group(1)), URIUtil.decodePath(matcher.group(2)))) {
			return null;
		}
		return super.apply(input, matcher);
	}

	/**
	 * Check whether the given file exists as regular file in the files directory of the given package
	 * @param packageName package name
	 * @param filePath file path within the files directory
	 * @return true iff the file exists
	 */
	protected boolean packageFileExists(String packageName, String filePath) {
		try {
			Path filesDir = Path.of(ConfigurationValue.PACKAGES_PATH.get(), packageName, PackageSynchronizer.FILES_DIR).toAbsolutePath()
					.normalize();
			Path file = filesDir.resolve(filePath).normalize();
			return file.startsWith(filesDir) && !file.equals(filesDir) && Files.isRegularFile(file);
		} catch (InvalidPathException e) {
			return false;
		}
	}
}
