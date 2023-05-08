/*
 * @author norbert
 * @date 22.09.2006
 * @version $Id: ReloadableProperties.java,v 1.1 2006-09-22 10:20:55 norbert Exp $
 */
package com.gentics.lib.i18n;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Properties;

import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.util.FileWatchDog;

/**
 * Implementation of reloadable properties. The properties are fetched from the given FileWatchDog.
 */
public class ReloadableProperties extends Properties {

	/**
	 * serial version id
	 */
	private static final long serialVersionUID = 7414934219285567680L;

	/**
	 * watched property file
	 */
	protected FileWatchDog propertiesFile;

	/**
	 * original default values
	 */
	protected Properties originalDefaults;

	/**
	 * logger
	 */
	protected static NodeLogger logger = NodeLogger.getNodeLogger(ReloadableProperties.class);

	/**
	 * 
	 */
	public ReloadableProperties(FileWatchDog propertiesFile) {
		this(propertiesFile, null);
	}

	/**
	 * @param defaults
	 */
	public ReloadableProperties(FileWatchDog propertiesFile, Properties defaults) {
		super();
		originalDefaults = defaults;
		this.propertiesFile = propertiesFile;
	}

	/**
	 * Reload the properties from the watched file
	 */
	protected void reloadProperties() {
		try {
			this.defaults = propertiesFile.getFileAsProperties(originalDefaults);
		} catch (IOException e) {
			logger.error("Error while reading properties from file {" + propertiesFile.getWatchedFile().getAbsolutePath() + "}", e);
		}
	}

	/* (non-Javadoc)
	 * @see java.util.Properties#getProperty(java.lang.String)
	 */
	public String getProperty(String key) {
		reloadProperties();
		return super.getProperty(key);
	}

	/* (non-Javadoc)
	 * @see java.util.Properties#getProperty(java.lang.String, java.lang.String)
	 */
	public String getProperty(String key, String defaultValue) {
		reloadProperties();
		return super.getProperty(key, defaultValue);
	}

	/* (non-Javadoc)
	 * @see java.util.Properties#list(java.io.PrintStream)
	 */
	public void list(PrintStream out) {
		reloadProperties();
		super.list(out);
	}

	/* (non-Javadoc)
	 * @see java.util.Properties#list(java.io.PrintWriter)
	 */
	public void list(PrintWriter out) {
		reloadProperties();
		super.list(out);
	}

	/* (non-Javadoc)
	 * @see java.util.Properties#propertyNames()
	 */
	public Enumeration propertyNames() {
		reloadProperties();
		return super.propertyNames();
	}
}
