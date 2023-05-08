/*
 * @author norbert
 * @date 31.05.2005
 * @version $Id: VersioningDatasource.java,v 1.9 2009-12-16 16:12:07 herbert Exp $
 * @gentics.sdk
 */
package com.gentics.api.lib.datasource;

import java.util.Collection;
import java.util.Date;
import java.util.Map;

import com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter;
import com.gentics.api.lib.resolving.Changeable;

/**
 * Interface for datasource that may support versioning (although specific
 * datasources using implementations of this interface may be configured not to
 * use versioning). Generally, objects managed by versioning datasources should
 * implement the Interface {@link com.gentics.lib.datasource.VersionedObject}.<br>
 * Implementation note: Do not implement this interface directly, but extend
 * {@link com.gentics.api.lib.datasource.AbstractVersioningDatasource} instead.
 */
public interface VersioningDatasource extends Datasource {

	/**
	 * constant for an empty version list
	 */
	public final static Version[] EMPTY_VERSIONLIST = new Version[0];

	/**
	 * provides meta information about a single version of a dataset.
	 * @author laurin
	 */
	public class Version implements Comparable {
		private int timestamp;

		private Date date;

		private int diffCount;

		private String user;

		private boolean autoupdate = false;

		/**
		 * get number of dataset rows, which differ to the last version.
		 * @return number of datasetrows.
		 */
		public int getDiffCount() {
			return diffCount;
		}

		/**
		 * sets the diffcount.
		 * @param diffCount number of datasetrows.
		 */
		public void setDiffCount(int diffCount) {
			this.diffCount = diffCount;
		}

		/**
		 * get the timestamp, this version was stored with.
		 * @return the timstamp.
		 */
		public int getTimestamp() {
			return timestamp;
		}

		/**
		 * sets the timstamp.
		 * @param timestamp the timestamp.
		 */
		public void setTimestamp(int timestamp) {
			this.timestamp = timestamp;
			date = new Date((long) timestamp * 1000L);
		}

		/**
		 * get the userid, this version was stored with.
		 * @return the user.
		 */
		public String getUser() {
			return user;
		}

		/**
		 * set the userid
		 * @param user the user.
		 */
		public void setUser(String user) {
			this.user = user;
		}

		/**
		 * @return Returns the date.
		 */
		public Date getDate() {
			return date;
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		public int compareTo(Object o) {
			Version v = (Version) o;

			return timestamp - v.timestamp;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		public boolean equals(Object obj) {
			if (obj instanceof Version) {
				return timestamp == ((Version) obj).timestamp;
			} else {
				return false;
			}
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		public int hashCode() {
			return timestamp;
		}

		/**
		 * @return Returns the autoupdate.
		 */
		public boolean isAutoupdate() {
			return autoupdate;
		}

		/**
		 * @param autoupdate The autoupdate to set.
		 */
		public void setAutoupdate(boolean autoupdate) {
			this.autoupdate = autoupdate;
		}
	}

	/**
	 * return true when this datasource supports versioning, false if not
	 * @return true/false depending whether versioning is supported
	 */
	boolean isVersioning();

	/**
	 * set the version timestamp to be used for querying, when the datasource is
	 * versioning. when -1 is set as timestamp, queries will be done with the
	 * current versions
	 * @see #isVersioning()
	 * @param timestamp timestamp to be used or -1 to reset versioned querying
	 */
	void setVersionTimestamp(int timestamp);

	/**
	 * check the requirements for versioning
	 * @return true when all requirements are met, false if not
	 */
	boolean checkRequirements();

	/**
	 * get the lst of available versions for the given contentid
	 * @param id id of the object
	 * @return list of available versions (empty list if no versioning)
	 */
	Version[] getVersions(String id);

	/**
	 * Create a datasource object with the given object data at the given
	 * timestamp
	 * @param objectParameters object data
	 * @param versionTimestamp version timestamp
	 * @return the created datasource object
	 * @throws DatasourceException in case of errors
	 */
	Changeable create(Map objectParameters, int versionTimestamp) throws DatasourceException;

	/**
	 * Get the resolvables matching the given datasource filter at the timestamp
	 * @param filter datasource filter
	 * @param prefillAttributes array of attribute names to prefill (null or empty for no prefilling)
	 * @param versionTimestamp version timestamp (-1 for current versions)
	 * @return collection of resolvables matching the datasource filter at the
	 *         given versionTimestamp
	 * @throws DatasourceException in case of errors
	 */
	Collection getResult(DatasourceFilter filter, String[] prefillAttributes, final int versionTimestamp) throws DatasourceException;

	/**
	 * Get the resolvables matching the given datasource filter at the timestamp
	 * @param filter datasource filter
	 * @param prefillAttributes array of attribute names to prefill (null or empty for no prefilling)
	 * @param start index of the first returned object
	 * @param count maximum number of objects returned, -1 for all objects
	 * @param sortedColumns sorted columns, may be null (no sorting used)
	 * @param versionTimestamp version timestamp (-1 for current versions)
	 * @return collection of resolvables matching the datasource filter at the
	 *         given versionTimestamp
	 * @throws DatasourceException in case of errors
	 */
	Collection getResult(DatasourceFilter filter, String[] prefillAttributes, int start,
			int count, Sorting[] sortedColumns, final int versionTimestamp) throws DatasourceException;

	/**
	 * Get the resolvables matching the given datasource filter at the timestamp
	 * @param filter datasource filter
	 * @param prefillAttributes array of attribute names to prefill (null or empty for no prefilling)
	 * @param start index of the first returned object
	 * @param count maximum number of objects returned, -1 for all objects
	 * @param sortedColumns sorted columns, may be null (no sorting used)
	 * @param specificParameters map of specific parameters, which will be
	 *        interpreted by some specific Datasources, may be null or empty (no
	 *        specific parameters)
	 * @param versionTimestamp version timestamp (-1 for current versions)
	 * @return collection of resolvables matching the datasource filter at the
	 *         given versionTimestamp
	 * @throws DatasourceException in case of errors
	 */
	Collection getResult(DatasourceFilter filter, String[] prefillAttributes, int start,
			int count, Sorting[] sortedColumns, Map specificParameters, final int versionTimestamp) throws DatasourceException;

	/**
	 * Get the number of resolvables matching the given filter at the
	 * versionTimestamp
	 * @param filter datasource filter
	 * @param versionTimestamp version timestamp, -1 for current versions
	 * @return number of resolvables matching the datasource filter at the
	 *         versionTimestamp
	 * @throws DatasourceException in case of errors
	 */
	int getCount(DatasourceFilter filter, int versionTimestamp) throws DatasourceException;

	/**
	 * Get the number of resolvables matching the given filter at the
	 * versionTimestamp
	 * @param filter datasource filter
	 * @param specificParameters map of specific parameters, which will be
	 *        interpreted by some specific Datasources, may be null or empty (no
	 *        specific parameters)
	 * @param versionTimestamp version timestamp, -1 for current versions
	 * @return number of resolvables matching the datasource filter at the
	 *         versionTimestamp
	 * @throws DatasourceException in case of errors
	 */
	int getCount(DatasourceFilter filter, Map specificParameters, int versionTimestamp) throws DatasourceException;
}
