/*
 * @author norbert
 * @date 29.12.2005
 * @version $Id: VersionedObject.java,v 1.5 2006-03-14 11:51:35 norbert Exp $
 */
package com.gentics.lib.datasource;

import java.util.Date;

import com.gentics.api.lib.datasource.VersioningDatasource.Version;
import com.gentics.api.lib.resolving.Resolvable;

/**
 * Interface of objects that can be versioned with a {@link com.gentics.api.lib.datasource.VersioningDatasource}
 * @author norbert
 */
public interface VersionedObject extends Resolvable {

	/**
	 * Name of the property for the versiontimestamp
	 */
	public final static String VERSIONTIMESTAMP_PROPERTY = "versionTimestamp";

	/**
	 * Get the version timestamp of the object. returns -1 if the object
	 * represents the currently active one, or the timestamp if the object holds
	 * data of a past or future version
	 * @return version timestamp, or -1 for the current object
	 */
	int getVersionTimestamp();

	/**
	 * Set the version timestamp of the object. set -1 for the current
	 * version.
	 * @param versionTimestamp new version timestamp
	 */
	void setVersionTimestamp(int versionTimestamp);

	/**
	 * Return true when the object holds data of a past version
	 * @return true for a past version, false for current or future versions
	 */
	boolean isPastVersion();

	/**
	 * Return true when the object holds data of a future version
	 * @return true for a future version, false for current or past versions
	 */
	boolean isFutureVersion();

	/**
	 * Return true when the object holds data of the current version
	 * @return true for the current version, false for past or future versions
	 */
	boolean isCurrentVersion();

	/**
	 * Get the version date of the object. get the current date for
	 * current versions or the date of the versiontimestamp for past/future
	 * versions
	 * @return date of the version
	 */
	Date getVersionDate();

	/**
	 * Get the current version of this object. Returns this object if it already is the current version.
	 * @return current version of this object.
	 */
	VersionedObject getCurrentVersion();

	/**
	 * Get the version of this object by timestamp. Returns the version (which
	 * may be this object) or null, if no object was found at the
	 * versionTimestamp
	 * @param versionTimestamp version timestamp
	 * @return version of the object or null
	 */
	VersionedObject getVersion(int versionTimestamp);

	/**
	 * get the list of available versions of the object
	 * @return list of available versions (empty list if no versioning)
	 */
	Version[] getVersions();

	/**
	 * Check whether the object instance is versioned anyway. <br>An instance
	 * of {@link VersionedObject} may not be versioned at all, when the managing
	 * {@link com.gentics.api.lib.datasource.VersioningDatasource} is configured
	 * to not do versioning, or when the object specifically is excluded from
	 * versioning.
	 * @return true when the object is versioned, false if not
	 */
	boolean isVersioned();
}
