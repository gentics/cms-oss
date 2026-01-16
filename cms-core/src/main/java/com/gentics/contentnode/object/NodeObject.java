/*
 * @author Stefan Hepp
 * @date 02.02.2006
 * @version $Id: NodeObject.java,v 1.16 2010-10-08 11:45:32 norbert Exp $
 */
package com.gentics.contentnode.object;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.db.DBUtils.BatchUpdater;
import com.gentics.contentnode.events.DependencyObject;
import com.gentics.contentnode.factory.NodeFactory;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.lib.db.ParamsExecutor;
import com.gentics.lib.db.SQLExecutor;
import com.gentics.lib.etc.StringUtils;

/**
 * Basic Object handled by the factory.
 * TODO think about enhancement of the object id (should contain a unique installation prefix) => Maybe use a class NodeObjectID?
 */
public interface NodeObject extends Serializable {

	/**
	 * Class for global ids for objects
	 */
	public static class GlobalId implements Serializable {

		/**
		 * This map holds all global prefixes as Strings. Every global prefix
		 * will only occur once in this map, so that no duplicate Strings are
		 * stored in instances of this class (which saves a lot of memory).
		 */
		public final static Map<String, String> GLOBALPREFIX = new HashMap<String, String>();

		/**
		 * serial version uid
		 */
		private static final long serialVersionUID = -5869364562341076489L;

		/**
		 * Regex pattern for the new UUIDs in the format
		 * ABCD.6d75467a-4b1c-11e5-a8d2-00270e06eab6
		 */
		public final static String PATTERN_UUID = "[0-9a-z]{4}\\.[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

		/**
		 * Regex pattern for the old global IDs in the format ABCD.123
		 */
		public final static String PATTERN_OLD_ID = ".{4}\\.\\d+";

		/**
		 * Global Prefix
		 */
		protected String globalPrefix;

		/**
		 * Global ID
		 */
		protected Integer globalId;

		/**
		 * (non-Javadoc)
		 * @see com.gentics.contentnode.object.NodeObject.GlobalId.PATTERN_UUID
		 */
		protected static Pattern uuidPattern = null;

		/**
		 * (non-Javadoc)
		 * @see com.gentics.contentnode.object.NodeObject.GlobalId.PATTERN_OLD_ID
		 */
		protected static Pattern oldIdPattern = null;

		static {
			uuidPattern  = Pattern.compile(PATTERN_UUID, Pattern.CASE_INSENSITIVE);
			oldIdPattern = Pattern.compile(PATTERN_OLD_ID);
		}

		/**
		 * Create an Instance for globalid (as String in the format [globalprefix].[globalid])
		 * @param globalId globalid as String
		 */
		public GlobalId(String globalId) {
			this.globalId = ObjectTransformer.getInteger(globalId.substring(globalId.indexOf('.') + 1), null);
			if (this.globalId != null) {
				this.globalPrefix = getUniqueGlobalPrefix(globalId.substring(0, globalId.indexOf('.')));
			} else {
				this.globalPrefix = globalId;
		}
		}

		/**
		 * Create an instance with given global prefix and global id
		 * @param globalPrefix global prefix
		 * @param globalId global id
		 */
		public GlobalId(String globalPrefix, Integer globalId) {
			this.globalId = globalId;
			if (globalId != null) {
				this.globalPrefix = getUniqueGlobalPrefix(globalPrefix);
			} else {
				this.globalPrefix = globalPrefix;
		}
		}

		/**
		 * Get the global id
		 * @return global id
		 */
		public Integer getGlobalId() {
			return globalId;
		}

		/**
		 * Get the global prefix
		 * @return global prefix
		 */
		public String getGlobalPrefix() {
			return globalPrefix;
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			if (globalId != null) {
			StringBuffer buffer = new StringBuffer();

			buffer.append(globalPrefix).append(".").append(globalId);
			return buffer.toString();
			} else {
				return globalPrefix;
		}
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		public boolean equals(Object obj) {
			if (obj instanceof GlobalId) {
				GlobalId globalId = (GlobalId) obj;

				return StringUtils.isEqual(globalId.getGlobalPrefix(), getGlobalPrefix())
						&& ObjectTransformer.getInt(globalId.getGlobalId(), 0) == ObjectTransformer.getInt(getGlobalId(), 0);
			} else {
				return false;
			}
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		public int hashCode() {
			int hash = 0;
			if (globalId != null) {
				hash += globalId.hashCode();
		}
			if (globalPrefix != null) {
				hash += globalPrefix.hashCode();
			}
			return hash;
		}

		/**
		 * Get the JVM wide unique String representing the given global prefix
		 * @param globalPrefix global prefix
		 * @return global prefix as JVM wide unique String object
		 */
		public final static String getUniqueGlobalPrefix(String globalPrefix) {
			if (globalPrefix.length() > 4) {
				globalPrefix = globalPrefix.substring(0, 4);
			}
			// check whether the global prefix is found in the map
			String uniqueGlobalPrefix = GLOBALPREFIX.get(globalPrefix);

			// not found in the map, so generate a new copy of the String
			if (uniqueGlobalPrefix == null) {
				// really make a new String (saves memory, because the new
				// String will really just contain the needed characters)
				uniqueGlobalPrefix = new String(globalPrefix);
				// put the prefix into the map
				GLOBALPREFIX.put(uniqueGlobalPrefix, uniqueGlobalPrefix);
			}
	
			return uniqueGlobalPrefix;
		}

		/**
		 * Get the globalid for the given tablename and the given local id. Returns null if localid is <= 0. The method will create a new 
		 * globalid of none was found.
		 * 
		 * @param tableName name of the table
		 * @param localId local id
		 * @return globalid or null
		 * @throws NodeException
		 */
		public final static GlobalId getGlobalId(String tableName, int localId) throws NodeException {
			if (localId <= 0) {
				return null;
		}
			return getGlobalId(tableName, Collections.singletonMap("id", localId));
		}

		/**
		 * Get the globalid for the given tablename and the given local id(s). Returns null if localid is null or no globalid is found
		 * @param tablename name of the table
		 * @param map of local Ids
		 * @return globalid or null
		 * @throws NodeException
		 */
		public final static GlobalId getGlobalId(String tablename, final Map<String, Integer> localIds) throws NodeException {
			if (ObjectTransformer.isEmpty(localIds)) {
				return null;
			}
			boolean foundNonZero = false;
			boolean first = true;
			List<Integer> params = new ArrayList<Integer>();
			StringBuilder sql = new StringBuilder("SELECT uuid FROM ");
			sql.append(tablename);

			for (Map.Entry<String, Integer> entry : localIds.entrySet()) {
				int localId = ObjectTransformer.getInt(entry.getValue(), 0);
				if (localId > 0) {
					foundNonZero = true;
				}
				if (first) {
					sql.append(" WHERE ");
					first = false;
					} else {
					sql.append(" AND ");
					}
				sql.append(entry.getKey()).append(" = ?");
				params.add(localId);
			}

			if (!foundNonZero) {
					return null;
				}

			final String[] uuid = new String[1];
			DBUtils.executeStatement(sql.toString(), new ParamsExecutor(params.toArray()) {
				@Override
				public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
					if (rs.next()) {
						uuid[0] = rs.getString("uuid");
			}
		}
			});

			if (uuid[0] == null) {
				return null;
			} else {
				return new GlobalId(uuid[0]);
			}
		}

		/**
		 * Checks whether the given id string is a globalid. A globalId which contains four characters followed by a dot and a subsequent
		 * number
		 * 
		 * @param id
		 * @return
		 */
		public static boolean isGlobalId(String id) {
			if (id == null) {
				return false;
			}
			if (oldIdPattern.matcher(id).matches() || uuidPattern.matcher(id).matches()) {
				return true;
			} else {
				return false;
			}
		}

		/**
		 * Get the local id of the object with this globalid stored in the given tablename
		 * @param tableName table name
		 * @return localid or null if not found
		 * @throws NodeException
		 */
		public Integer getLocalId(String tableName) throws NodeException {
			if (StringUtils.isEmpty(tableName)) {
				return null;
			}

			IdFetcher ex = new IdFetcher();
			DBUtils.executeStatement("SELECT id FROM " + tableName + " WHERE uuid = ?", ex);
			return ex.id != 0 ? ex.id : null;
				}

		/**
		 * Get the local id of the object with this globalid having the given class
		 * @param clazz object class
		 * @return local id or null if not found
		 * @throws NodeException
		 */
		public Integer getLocalId(Class<? extends NodeObject> clazz) throws NodeException {
			String tableName = TransactionManager.getCurrentTransaction().getTable(clazz);
			if (ObjectTransformer.isEmpty(tableName) && clazz.equals(ObjectTagDefinition.class)) {
				tableName = "objtag";
			}
			return getLocalId(tableName);
				}

		/**
		 * Inner Helper class to get id from a table
		 */
		private class IdFetcher extends SQLExecutor {
			int id = 0;

			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				stmt.setString(1, GlobalId.this.toString());
			}

			@Override
			public void handleResultSet(ResultSet rs) throws SQLException,
						NodeException {
				if (rs.next()) {
					id = rs.getInt("id");
				}
			}
		}
	}

	/**
	 * get the id of this object.
	 * The Ids must be greater than 0.
	 *
	 * @return the id of this object, or 0 if not jet stored into the db.
	 */
	Integer getId();

	/**
	 * Check whether the object is new (not yet persisted).
	 * An object is considered new if it does not have a local ID yet
	 * @return true, iff the object is new
	 */
	default boolean isNew() {
		return ObjectTransformer.getInt(getId(), 0) <= 0;
	}

	/**
	 * Get the factory with which this object was created. This is a shortcut to
	 * getObjectInfo().getFactory().
	 * TODO make transaction-save (=remove)
	 * @return the factory of this object.
	 */
	NodeFactory getFactory();

	/**
	 * get an info Object of this instance.
	 * @return an info about this instance.
	 */
	NodeObjectInfo getObjectInfo();

	/**
	 * get the Objecttype of this instance.
	 * This method will not handle dependencies
	 *
	 * @return the ttype of this instance
	 */
	Integer getTType();

	/**
	 * Trigger an event on the NodeObject
	 * @param object dependency object.
	 * @param property modified property (may be null)
	 * @param eventMask event mask.
	 * @param depth depth of the triggered event starting with 0.
	 * @param channelId channel id for triggering the event on a specific channel, 0 for all channels
	 * @throws NodeException
	 * @see com.gentics.contentnode.events.Events containt constants for
	 *      eventMask
	 * @see NodeObject to construct dependencyobject.
	 */
	void triggerEvent(DependencyObject object, String[] property, int eventMask, int depth, int channelId) throws NodeException;

	/**
	 * Deletes the Object. If the object is recyclable and the wastebin is activated for the node, the object will be put into the wastebin
	 * @throws InsufficientPrivilegesException if the transaction doesn't have the right to delete the object
	 * @throws NodeException if an internal error occurs
	 */
	void delete() throws InsufficientPrivilegesException, NodeException;

	/**
	 * Deletes the Object. If the object is recyclable and the wastebin is activated for the node, the object will be put into the wastebin, unless force is set to true.
	 * @param force true to irrevocably delete the object, even it can be put into the wastebin (or already is in the wastebin)
	 * @throws InsufficientPrivilegesException
	 * @throws NodeException
	 */
	void delete(boolean force) throws InsufficientPrivilegesException, NodeException;

	/**
	 * Return true if this object is deleted (and put into the wastebin), false if not
	 * @return true for deleted objects, false otherwise
	 */
	boolean isDeleted();

	/**
	 * Get timestamp, when the object was deleted or 0 if the object was not deleted
	 * @return timestamp of deletion (0 if not deleted)
	 */
	int getDeleted();

	/**
	 * Return true iff the object can be put into the wastebin
	 * @return true iff object can be put into the wastebin
	 */
	default boolean isRecyclable() {
		return false;
	}

	/**
	 * Get the user who deleted the object or null if the object is not deleted
	 * @return user who deleted the object or null
	 * @throws NodeException
	 */
	SystemUser getDeletedBy() throws NodeException;

	/**
	 * Restore the (deleted) object from the wastebin
	 * @throws NodeException
	 */
	void restore() throws NodeException;

	/**
	 * Save the Object with the given userGroup ID
	 * @param userGroupId ID of the user Group
	 * @return true when the object was modified, false if not
	 * @throws InsufficientPrivilegesException
	 * @throws NodeException
	 */
	boolean save(Integer userGroupId) throws InsufficientPrivilegesException, NodeException;

	/**
	 * Save the Object
	 * @return true when the object was modified, false if not
	 * @throws InsufficientPrivilegesException
	 * @throws NodeException
	 */
	boolean save() throws InsufficientPrivilegesException, NodeException;

	/**
	 * Save the object using the (optionally given) batch updater
	 * @param batchUpdater optional batch updater
	 * @return true when the object was modified, false if not
	 * @throws InsufficientPrivilegesException
	 * @throws NodeException
	 */
	default boolean saveBatch(BatchUpdater batchUpdater) throws InsufficientPrivilegesException, NodeException {
		return save();
	}

	/**
	 * Unlock the object
	 * @throws NodeException
	 */
	void unlock() throws NodeException;

	/**
	 * Dirt the caches of sub objects
	 * @throws NodeException
	 */
	void dirtCache() throws NodeException;

	/**
	 * Get the parent object (if the object exists within a structure) or null
	 * @throws NodeException
	 */
	NodeObject getParentObject() throws NodeException;

	/**
	 * Creates a copy of the given object (and all contained
	 * subobjects), containing the same data, which is not yet stored in the
	 * database (have no id's set) as editable objects
	 * @return editable copy of the object
	 * @throws NodeException
	 */
	NodeObject copy() throws NodeException;

	/**
	 * Copy the given object over this object
	 * @param original given object, must be of the same class
	 * @throws ReadOnlyException
	 * @throws NodeException
	 */
	<T extends NodeObject> void copyFrom(T original) throws ReadOnlyException, NodeException;

	/**
	 * Get the published version of this object. If the object is not yet
	 * published, this will return null.
	 * 
	 * @return published version of the object
	 * @throws NodeException
	 */
	NodeObject getPublishedObject() throws NodeException;

	/**
	 * Get the udate of the object. Returns -1 if the udate could not be determined (for example, when the object is new)
	 * @return udate of the object
	 * @see #getEffectiveUdate()
	 */
	int getUdate();

	/**
	 * Get the effective udate for this object. The difference between this
	 * method and {@link #getUdate()} is, that this method will also consider
	 * the udates of subobjects and will calculate the newest of all udates,
	 * whereas {@link #getUdate()} gets the udate of the object (alone).<br/>
	 * Example: calling {@link #getUdate()} for a {@link Page} will get the
	 * date, when the page record was changed the last time (e.g. page name was
	 * modified). Calling this method for a page will also consider changes in
	 * contents, objecttags, ...
	 * 
	 * @return effective udate
	 * @throws NodeException
	 */
	int getEffectiveUdate() throws NodeException;

	/**
	 * Get the global id of the object
	 * @return global id
	 */
	GlobalId getGlobalId();

	/**
	 * Set the global id of a new created object.
	 * @param globalId global id
	 * @throws ReadOnlyException is thrown, when the object is not editable
	 * @throws NodeException is thrown, if the object already has a global id set
	 */
	void setGlobalId(GlobalId globalId) throws ReadOnlyException, NodeException;

	/**
	 * Reload the object (as unmodifiable object)
	 * @return reloaded object
	 * @throws NodeException
	 */
	@SuppressWarnings("unchecked")
	default <T extends NodeObject> T reload() throws NodeException {
		return (T)TransactionManager.getCurrentTransaction().getObject(this);
	}
}
