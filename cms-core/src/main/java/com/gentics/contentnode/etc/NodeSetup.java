package com.gentics.contentnode.etc;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.TransactionException;
import com.gentics.lib.log.NodeLogger;

/**
 * 
 * @author johannes2
 * 
 */
public class NodeSetup {

	/**
	 * logger
	 */
	protected static NodeLogger logger = NodeLogger.getNodeLogger(NodeLogger.class);

	public static enum NODESETUP_KEY {
		maintenancemode, maintenancebanner, selfupdate_status, version, latest_gcnversion, globalprefix
	}

	;

	/**
	 * Returns the value for the given key or null when no value has been specified.
	 * 
	 * @param key
	 * @return
	 * @throws SQLException
	 * @throws TransactionException
	 */
	public static NodeSetupValuePair getKeyValue(NODESETUP_KEY key) throws NodeException {
		if (key == NODESETUP_KEY.globalprefix) {
			// the globalprefix was moved from the nodesetup table into its own table.
			// this was done to reduce the dependency on the nodesetup table, since most of the INSERT statements will invoke a trigger
			// that creates and inserts a new globalid and to generate the globalid, the globalprefix needs to be read.
			return DBUtils.select("SELECT globalprefix FROM globalprefix WHERE id = 1", rs -> {
				if (rs.next()) {
					return new NodeSetupValuePair(0, rs.getString("globalprefix"));
				} else {
					return null;
				}
			});
		}

		return getKeyValue(key.toString());
	}

	/**
	 * Returns the value for the given key or null when no value has been specified.
	 * @param key key
	 * @return value
	 * @throws NodeException
	 */
	public static NodeSetupValuePair getKeyValue(String key) throws NodeException {
		final String SELECT_KEY_VALUES_STATEMENT = "SELECT intvalue, textvalue from nodesetup where name = ?";
		return DBUtils.select(SELECT_KEY_VALUES_STATEMENT, stmt -> {
			stmt.setString(1, key);
		}, rs -> {
			if (rs.next()) {
				return new NodeSetupValuePair(rs.getInt(1), rs.getString(2));
			} else {
				return null;
			}
		});
	}

	/**
	 * Set the int value with the given key in the nodesetup table
	 * @param key key
	 * @param intValue int value
	 * @throws NodeException
	 */
	public static void setKeyValue(NODESETUP_KEY key, int intValue) throws NodeException {
		setKeyValue(key, "", intValue);
	}

	/**
	 * Set the int value with the given key in the nodesetup table
	 * @param key key
	 * @param intValue int value
	 * @throws NodeException
	 */
	public static void setKeyValue(String key, int intValue) throws NodeException {
		setKeyValue(key, "", intValue);
	}

	/**
	 * Set the string value with the given key in the nodesetup table
	 * @param key key
	 * @param stringValue string value
	 * @throws NodeException
	 */
	public static void setKeyValue(NODESETUP_KEY key, String stringValue) throws NodeException {
		setKeyValue(key, stringValue, 0);
	}

	/**
	 * Set the string value with the given key in the nodesetup table
	 * @param key key
	 * @param stringValue string value
	 * @throws NodeException
	 */
	public static void setKeyValue(String key, String stringValue) throws NodeException {
		setKeyValue(key, stringValue, 0);
	}

	/**
	 * Set the values with the given key in the nodesetup table
	 * @param key key
	 * @param stringValue string value
	 * @param intValue int value
	 * @throws NodeException
	 */
	public static void setKeyValue(NODESETUP_KEY key, String stringValue, int intValue) throws NodeException {
		setKeyValue(key.toString(), stringValue, intValue);
	}

	/**
	 * Set the values with the given key in the nodesetup table
	 * @param key key
	 * @param stringValue string value
	 * @param intValue int value
	 * @throws NodeException
	 */
	public static void setKeyValue(String key, String stringValue, int intValue) throws NodeException {
		Map<String, Object> id = new HashMap<>();
		id.put("name", key);

		Map<String, Object> data = new HashMap<>();
		data.put("textvalue", ObjectTransformer.getString(stringValue, ""));
		data.put("intvalue", intValue);
		DBUtils.updateOrInsert("nodesetup", id, data);
	}
}
