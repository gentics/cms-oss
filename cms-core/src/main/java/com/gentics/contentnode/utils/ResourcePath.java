package com.gentics.contentnode.utils;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.lib.log.NodeLogger;
import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

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

/**
 * Auto-closable that will make a resource path available in the filesystem.
 *
 * <p>
 *     If the given resource is already a file or directory in the filesystem this wrapper will do nothing. If the
 *     resource path is in a JAR file, it will extract all entries beginning with the resource path to a temporary
 *     directory which will be deleted automatically when {@link #close()} is called.
 * </p>
 */
public class ResourcePath implements AutoCloseable {

	/** Pattern for path of URL, which locates resource in a jar file. */
	private static final Pattern jarPattern = Pattern.compile("file:(?<jar>.*)!(.*)");

	private NodeLogger logger = NodeLogger.getNodeLogger(getClass());

	/** Temporary directory, where the default package is extracted (will be removed when job is done). */
	private File tempDir = null;

	/** Path to the resource to wrap. */
	private final String resource;

	/**
	 * Create new instance for specified resource.
	 *
	 * @param resource The path to the resource to wrap in this auto-closable.
	 */
	public ResourcePath(String resource) {
		this.resource = StringUtils.prependIfMissing(resource, "/");
	}

	/**
	 * Get the {@link Path} to the resource from the class path.
	 * If the resource is already extracted, this method will just return the Path to the extracted resource.
	 * If the resource is located in a jar file, the contents will be extracted into a temporary directory (which will be set to {@link #tempDir}).
	 *
	 * @return Path to the resource (may be null, if resource is not found)
	 * @throws NodeException
	 */
	public Path getPath() throws NodeException {
		URL url = getClass().getResource(resource);
		Path path = null;

		if (url == null) {
			return null;
		}

		if (StringUtils.equals("jar", url.getProtocol())) {
			Matcher m = jarPattern.matcher(url.getPath());

			if (m.matches()) {
				tempDir = Files.createTempDir();
				logger.info(String.format("Extracting %s to %s", resource, tempDir.getAbsolutePath()));

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
								// Create directory.
								dest.mkdirs();
							} else {
								// Copy file.
								try (InputStream in = jar.getInputStream(entry)) {
									FileUtils.copyInputStreamToFile(in, dest);
								}
							}
						}
					}
				} catch (IOException e) {
					throw new NodeException(
						String.format("Error while extracting %s to %s", resource, tempDir.getAbsolutePath()),
						e);
				}
			} else {
				throw new NodeException(String.format(
						"Error while extracting %s: Resource path %s does not match expected pattern",
						resource, url.getPath()));
			}
		} else if (StringUtils.equals("file", url.getProtocol())) {
			path = new File(url.getPath()).toPath();
			logger.info(String.format("Serving %s from path %s", resource, url.getPath()));
		}

		return path;
	}

	@Override
	public void close() throws NodeException {
		// If the resource was extracted into a temporary directory, delete it now.
		if (tempDir != null) {
			try {
				logger.info(String.format("Deleting temporary resource location %s", tempDir.getAbsolutePath()));
				FileUtils.deleteQuietly(tempDir);
			} catch (Exception e) {
				throw new NodeException(e);
			}
		}
	}
}
