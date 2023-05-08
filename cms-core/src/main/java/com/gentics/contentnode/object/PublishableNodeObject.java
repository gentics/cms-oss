package com.gentics.contentnode.object;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.ContentNodeDate;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;

import io.reactivex.Flowable;

/**
 * Interface for objects, which can be published
 */
public interface PublishableNodeObject extends NodeObject {
	/**
	 * Check whether the object is online or not
	 * @return true when the object is online
	 * @throws NodeException
	 */
	boolean isOnline() throws NodeException;

	/**
	 * Check whether the object is modified. That means, that the last object version is not the published one.
	 * @return true iff object is modified
	 * @throws NodeException
	 */
	boolean isModified() throws NodeException;

	/**
	 * get the publish date as a unix timestamp 
	 * @return publish date unix timestamp
	 */
	ContentNodeDate getPDate();

	/**
	 * Get the user, who published the object (last)
	 * @return last publisher, may be null
	 * @throws NodeException
	 */
	SystemUser getPublisher() throws NodeException;

	/**
	 * Publish the object
	 * @throws ReadOnlyException
	 * @throws NodeException
	 */
	default void publish() throws ReadOnlyException, NodeException {
		publish(0, false);
	}

	/**
	 * Publish the object at the given time
	 * @param at timestamp for scheduled publishing, 0 for publishing immediately
	 * @param keepTimePubVersion true to keep the currently set timepub version, false to create a new version
	 * @throws ReadOnlyException
	 * @throws NodeException
	 */
	void publish(int at, boolean keepTimePubVersion) throws ReadOnlyException, NodeException;

	/**
	 * Get Publish At time
	 * @return Publish At time
	 */
	ContentNodeDate getTimePub();

	/**
	 * Get Publish At version
	 * @return Publish At version
	 * @throws NodeException
	 */
	NodeObjectVersion getTimePubVersion() throws NodeException;

	/**
	 * Take the object offline
	 * @throws ReadOnlyException
	 * @throws NodeException
	 */
	default void takeOffline() throws ReadOnlyException, NodeException {
		takeOffline(0);
	}

	/**
	 * Take the object offline at the given time
	 * @param at timestamp for scheduled taking offline, 0 for taking offline immediately
	 * @throws ReadOnlyException
	 * @throws NodeException
	 */
	void takeOffline(int at) throws ReadOnlyException, NodeException;

	/**
	 * Get scheduled time for taking the object offline
	 * @return ContentNodeDate instance
	 */
	ContentNodeDate getTimeOff();

	/**
	 * Sets the current status dependent on the time management settings.
	 * @return true if timemanagement was handled, false if not
	 */
	boolean handleTimemanagement() throws NodeException;

	/**
	 * Clear "Publish At" data
	 * @throws NodeException
	 */
	void clearTimePub() throws NodeException;

	/**
	 * Clear "Offline At" data
	 * @throws NodeException
	 */
	void clearTimeOff() throws NodeException;

	/**
	 * Check whether the object is planned for publishing or taking offline
	 * @return true iff object is planned
	 * @throws NodeException
	 */
	default boolean isPlanned() throws NodeException {
		return getTimePub().getIntTimestamp() != 0 || getTimeOff().getIntTimestamp() != 0;
	}

	/**
	 * Get all versions of the current object in descending order (latest version first).
	 * The default implementation will load the versions on every call (using {@link #loadVersions()}), but
	 * implementing classes may store the versions as class members
	 */
	default NodeObjectVersion[] getVersions() throws NodeException {
		return loadVersions();
	}

	/**
	 * Load all versions of the current object in descending order (latest version first)
	 */
	default NodeObjectVersion[] loadVersions() throws NodeException {
		List<NodeObjectVersion> list = DBUtils.select(
				"SELECT id, timestamp, user_id, published, nodeversion FROM nodeversion WHERE o_type = ? AND o_id = ? ORDER BY timestamp ASC",
				ps -> {
					ps.setInt(1, getTType());
					ps.setInt(2, getId());
				}, rs -> {
					Transaction t = TransactionManager.getCurrentTransaction();
					List<NodeObjectVersion> tmp = new ArrayList<>();
					while (rs.next()) {
						tmp.add(new NodeObjectVersion(rs.getInt("id"), rs.getString("nodeversion"),
								t.getObject(SystemUser.class, rs.getInt("user_id")),
								new ContentNodeDate(rs.getInt("timestamp")), rs.isLast(),
								rs.getBoolean("published")));
					}
					return tmp;
				});
		Collections.reverse(list);
		return (NodeObjectVersion[]) list.toArray(new NodeObjectVersion[list.size()]);
	}

	

	/**
	 * Get the published version, if it is marked or null, if not marked
	 * @return published version or null
	 */
	default NodeObjectVersion getPublishedVersion() throws NodeException {
		NodeObjectVersion[] versions = getVersions();

		if (!ObjectTransformer.isEmpty(versions)) {
			for (NodeObjectVersion v : versions) {
				if (v.isPublished()) {
					return v;
				}
			}
		}

		return null;
	}

	/**
	 * Returns this version of the object
	 * @return object version
	 */
	default NodeObjectVersion getVersion() throws NodeException {
		NodeObjectVersion[] versions = getVersions();
		NodeObjectInfo objectInfo = getObjectInfo();

		if (objectInfo.isCurrentVersion()) {
			// when the object is the current version, get the last nodeversion
			if (versions != null && versions.length > 0) {
				return versions[0];
			}
		} else {
			// this is a versioned object, so find the associated version
			int versionTimestamp = objectInfo.getVersionTimestamp();

			if (versions != null) {
				for (NodeObjectVersion v : versions) {
					if (v.getDate().getIntTimestamp() <= versionTimestamp) {
						return v;
					}
				}
			}
		}
		return null;
	}

	/**
	 * Get the version with given version number
	 * @param number version number
	 * @return version or null, if not found
	 * @throws NodeException
	 */
	default NodeObjectVersion getVersion(String number) throws NodeException {
		return Flowable.fromArray(getVersions()).filter(v -> v.getNumber().equals(number)).firstElement().blockingGet();
	}

	/**
	 * Get the version with given timestamp
	 * @param timestamp timestamp
	 * @return version or null, if not found
	 * @throws NodeException
	 */
	default NodeObjectVersion getVersion(int timestamp) throws NodeException {
		return Flowable.fromArray(getVersions()).filter(v -> v.getDate().getIntTimestamp() == timestamp).firstElement()
				.blockingGet();
	}

	/**
	 * Get the version with given ID
	 * @param id ID
	 * @return version or null, if not found
	 * @throws NodeException
	 */
	default NodeObjectVersion getVersionById(int id) throws NodeException {
		return Flowable.fromArray(getVersions()).filter(v -> v.getId() == id).firstElement().blockingGet();
	}

	/**
	 * Get the last version of the object
	 * @return object version
	 * @throws NodeException
	 */
	default NodeObjectVersion getLastVersion() throws NodeException {
		NodeObjectVersion[] versions = getVersions();
		if (versions != null && versions.length > 0) {
			return versions[0];
		} else {
			return null;
		}
	}

	/**
	 * Restore the given object version
	 * @param toRestore object version to restore
	 * @throws NodeException
	 */
	void restoreVersion(NodeObjectVersion toRestore) throws NodeException;

	/**
	 * Purge all Object Versions older than the given Object Version
	 * @param oldestKeptVersion oldest Object Version to keep
	 * @throws NodeException
	 */
	void purgeOlderVersions(NodeObjectVersion oldestKeptVersion) throws NodeException;

	/**
	 * Clear all internally stored versions
	 */
	void clearVersions();
}
