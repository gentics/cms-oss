package com.gentics.contentnode.init;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.devtools.MainPackageSynchronizer;
import com.gentics.contentnode.devtools.SynchronizableNodeObject;
import com.gentics.contentnode.devtools.Synchronizer;
import com.gentics.contentnode.factory.object.UserLanguageFactory;
import com.gentics.contentnode.factory.object.UserLanguageFactory.WithAllLanguages;
import com.google.common.io.Files;

/**
 * Initialization job that synchronizes the default package from the filesystem into the CMS
 */
public class SyncDefaultPackage extends InitJob {
	/**
	 * Pattern for path of URL, which locates resource in a jar file
	 */
	private static final Pattern jarPattern = Pattern.compile("file:(?<jar>.*)!(.*)");

	/**
	 * Temporary directory, where the default package is extracted (will be removed when job is done)
	 */
	protected File tempDir = null;

	@Override
	public void execute() throws NodeException {
		Path path = getPath("/packages/DefaultElements/");

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
		} finally {
			// if the resource was extracted into a temporary directory, we delete it now
			if (tempDir != null) {
				logger.info(String.format("Deleting temporary package location %s", tempDir.getAbsolutePath()));
				FileUtils.deleteQuietly(tempDir);
			}
		}
		logger.info("Done synchronizing default elements");
	}

	/**
	 * Get the {@link Path} to the resource from the class path.
	 * If the resource is already extracted, this method will just return the Path to the extracted resource.
	 * If the resource is located in a jar file, the contents will be extracted into a temporary directory (which will be set to {@link #tempDir}).
	 * @param resource resource to be returned as Path
	 * @return Path to the resource (may be null, if resource is not found)
	 * @throws NodeException
	 */
	protected Path getPath(String resource) throws NodeException {
		resource = StringUtils.prependIfMissing(resource, "/");
		URL url = getClass().getResource(resource);
		Path path = null;

		if (url == null) {
			return null;
		}

		if (StringUtils.equals("jar", url.getProtocol())) {
			Matcher m = jarPattern.matcher(url.getPath());
			if (m.matches()) {
				tempDir = Files.createTempDir();
				logger.info(String.format("Extracting /packages/DefaultElements/ to %s", tempDir.getAbsolutePath()));

				path = new File(tempDir, resource).toPath();
				String pathToJar = m.group("jar");

				try (JarFile jar = new JarFile(pathToJar)) {
					Enumeration<JarEntry> entries = jar.entries();
					while (entries.hasMoreElements()) {
						JarEntry entry = entries.nextElement();
						String entryName = StringUtils.prependIfMissing(entry.getName(), "/");
						if (StringUtils.startsWith(entryName, resource)) {
							logger.debug(entryName);
							File dest = new File(tempDir, entry.getName());
							if (entry.isDirectory()) {
								// create directory
								dest.mkdirs();
							} else {
								// copy file
								try (InputStream in = jar.getInputStream(entry)) {
									FileUtils.copyInputStreamToFile(in, dest);
								}
							}
						}
					}
				} catch (IOException e) {
					throw new NodeException(String.format("Error while extracting /packages/DefaultElements/ to %s",
							tempDir.getAbsolutePath()), e);
				}
			} else {
				throw new NodeException(String.format(
						"Error while extracting /packages/DefaultElements/: Resource path %s does not match expected pattern",
						url.getPath()));
			}
		} else if (StringUtils.equals("file", url.getProtocol())) {
			path = new File(url.getPath()).toPath();
			logger.info(String.format("Serving DefaultElements from path %s", url.getPath()));
		}

		return path;
	}
}
