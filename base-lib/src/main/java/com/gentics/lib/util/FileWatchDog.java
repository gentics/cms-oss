/*
 * @author norbert
 * @date 30.08.2006
 * @version $Id: FileWatchDog.java,v 1.6 2008-05-26 15:05:56 norbert Exp $
 */
package com.gentics.lib.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.util.Properties;

import com.gentics.lib.log.NodeLogger;

/**
 * Helper class for periodic checking of file changes (e.g. for configuration
 * files)
 */
public class FileWatchDog implements Serializable {

	/**
	 * serial version uid
	 */
	private static final long serialVersionUID = 465763227057143593L;

	/**
	 * watched file
	 */
	private File watchedFile;

	/**
	 * check interval in ms, defaults to 1 minute. when set to -1, no check is
	 * done, 0 will check on every access.
	 */
	private int checkInterval = 60000;

	/**
	 * last modification timestamp, -1 if not yet set
	 */
	private long lastModification = -1;

	/**
	 * last checking timestamp, -1 if not yet set
	 */
	private long lastCheckTimestamp = -1;

	/**
	 * file read as properties
	 */
	private Properties properties;

	/**
	 * cached file content
	 */
	private String fileContent;

	/**
	 * encoding of the file content
	 */
	private String contentEncoding;

	/**
	 * length of the read buffer
	 */
	private final static int READ_BUFFER_LENGTH = 4096;

	/**
	 * the logger
	 */
	private static NodeLogger logger = NodeLogger.getNodeLogger(FileWatchDog.class);

	/**
	 * Create an instance watching the given file. The file will be checked
	 * every minute for modifications.
	 * @param watchedFile watched file
	 */
	public FileWatchDog(File watchedFile) {
		if (logger.isDebugEnabled()) {
			logger.debug("Creating FileWatchDog for file {" + watchedFile.getAbsolutePath() + "}");
		}
		this.watchedFile = watchedFile;
	}

	/**
	 * Create an instance watching the given file
	 * @param watchedFile watched file
	 * @param checkInterval check interval in ms, set to -1 for disable
	 *        modification checking and 0 for modification checking on every
	 *        access.
	 */
	public FileWatchDog(File watchedFile, int checkInterval) {
		this(watchedFile);
		if (logger.isDebugEnabled()) {
			logger.debug("Setting check interval for file {" + watchedFile.getAbsolutePath() + "} to {" + checkInterval + "} ms.");
		}
		this.checkInterval = checkInterval;
	}

	/**
	 * Check whether the file needs to be checked again
	 * @return true when the file needs to be checked, false if not
	 */
	protected boolean needsChecking() {
		// the file needs to be rechecked if
		// - it has never been checked before or not info about last
		// modification is available
		// OR
		// - checkInterval is positive and one of the following is true
		// 1. the checkinterval is 0 (check on every access)
		// 2. the last check was too long ago (longer than checkInterval ms)

		long now = System.currentTimeMillis();
		boolean checkNeeded = lastCheckTimestamp < 0 || lastModification < 0
				|| (checkInterval >= 0 && (checkInterval == 0 || (lastCheckTimestamp + checkInterval < now)));

		if (logger.isDebugEnabled()) {
			logger.debug(
					"last check: " + lastCheckTimestamp + ", last modification: " + lastModification + ", check interval: " + checkInterval + ", now: " + now + " => "
					+ (checkNeeded ? "" : "no ") + "check needed");
		}

		return checkNeeded;
	}

	/**
	 * Check the file for modifications.
	 * @return true when the file has changed, false if not
	 */
	public boolean checkFile() {
		// first check whether the file needs to be checked
		if (needsChecking()) {
			long modification = watchedFile.lastModified();
			boolean changeDetected = modification != lastModification;

			if (logger.isDebugEnabled()) {
				logger.debug("file modification: " + modification + " => " + (changeDetected ? "" : "no ") + "change detected");
			}
			lastModification = modification;
			lastCheckTimestamp = System.currentTimeMillis();
			return changeDetected;
		} else {
			return false;
		}
	}

	/**
	 * Reset the read file content, when the file was changed (a change was
	 * detected, so the file needs to be re-read).
	 */
	protected void resetFile() {
		if (checkFile()) {
			if (logger.isDebugEnabled()) {
				logger.debug("reseting cached file content");
			}
			properties = null;
			fileContent = null;
			contentEncoding = null;
			lastModification = watchedFile.lastModified();
		}
	}

	/**
	 * Get the file contents as properties (file is supposed to be encoded UTF-8)
	 * @return file contents as properties
	 * @throws IOException when the file cannot be read as properties
	 */
	public Properties getFileAsProperties() throws IOException {
		return getFileAsProperties(null, "UTF-8");
	}

	/**
	 * Get the file contents as properties
	 * @param encoding file encoding
	 * @return file contents as properties
	 * @throws IOException when the file cannot be read as properties
	 */
	public Properties getFileAsProperties(String encoding) throws IOException {
		return getFileAsProperties(null, encoding);
	}

	/**
	 * Get the file contents as properties. This method will not fail when the
	 * file does not exist, but return the default properties.
	 * @param defaultProperties default properties
	 * @return file contents as properties or default properties when the file
	 *         does not exist
	 */
	public Properties getFileAsProperties(Properties defaultProperties, String encoding) throws IOException {
		resetFile();
		if (properties == null && watchedFile.exists()) {
			if (logger.isDebugEnabled()) {
				logger.debug("Reloading file as properties");
			}
			InputStream inputStream = new FileInputStream(watchedFile);

			try {
				properties = new Properties(defaultProperties);
				properties = FileUtil.loadProperties(inputStream, properties, encoding);
			} finally {
				try {
					inputStream.close();
				} catch (IOException e) {
					NodeLogger.getNodeLogger(getClass()).warn("Error while closing filestream", e);
				}
			}
		}

		return properties != null ? properties : defaultProperties;
	}

	/**
	 * Get the file contents as properties. The file is supposed to be encoded in UTF-8
	 * @param defaultProperties default properties
	 * @return file contents as properties
	 * @throws IOException
	 */
	public Properties getFileAsProperties(Properties defaultProperties) throws IOException {
		return getFileAsProperties(defaultProperties, "UTF-8");
	}

	/**
	 * Get the file as input stream
	 * @return file inputstream
	 * @throws IOException when the file cannot be read
	 */
	public InputStream getFileInputStream() throws IOException {
		return new FileInputStream(watchedFile);
	}

	/**
	 * Get the file content in the default encoding (UTF-8)
	 * @return file content
	 * @throws IOException
	 */
	public String getFileContent() throws IOException {
		return getFileContent("UTF-8");
	}

	/**
	 * Get the file content with the given encoding
	 * @param encoding encoding of the file
	 * @return file content
	 * @throws IOException
	 */
	public String getFileContent(String encoding) throws IOException {
		resetFile();
		if (fileContent == null || !encoding.equals(contentEncoding)) {
			if (logger.isDebugEnabled()) {
				logger.debug("Reloading file content");
			}
			StringBuffer content = new StringBuffer();
			char[] buffer = new char[READ_BUFFER_LENGTH];
			int read = 0;
			InputStream inputStream = new FileInputStream(watchedFile);
			Reader fileReader = new InputStreamReader(inputStream, encoding);

			try {
				while ((read = fileReader.read(buffer)) >= 0) {
					content.append(buffer, 0, read);
				}
			} finally {
				try {
					inputStream.close();
					fileReader.close();
				} catch (IOException e) {
					logger.warn("Error while closing file stream", e);
				}
			}
			fileContent = content.toString();
			contentEncoding = encoding;
		}

		return fileContent;
	}

	/**
	 * Get the watched file
	 * @return watched file
	 */
	public File getWatchedFile() {
		return watchedFile;
	}
}
