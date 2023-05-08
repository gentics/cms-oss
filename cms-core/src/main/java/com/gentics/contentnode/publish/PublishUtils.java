/*
 * @author tobiassteiner
 * @date Mar 15, 2011
 * @version $Id: PublishUtils.java,v 1.1.2.1 2011-03-15 17:26:52 tobiassteiner Exp $
 */
package com.gentics.contentnode.publish;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.log.ActionLogger;

/**
 * Holds static utility methods factored out of publishing related classes. 
 */
public class PublishUtils {

	/**
	 * Get the database timestamp of the start of a publish run at the given transaction timestamp.
	 * @param timestamp The transaction timestamp of the publish run to look for.
	 * @return If no publish run is found at the given transaction timestamp, this method returns -1.
	 */
	public static int getDatabaseStartTimeOfPublish(int timestamp) throws NodeException {
		return DBUtils.select("SELECT unix_timestamp(insert_timestamp) as timestamp FROM logcmd WHERE timestamp = ? AND cmd_desc_id = ? LIMIT 1", stmt -> {
			stmt.setInt(1, timestamp);
			stmt.setInt(2, ActionLogger.PUBLISH_START);
		}, rs -> {
			if (!rs.next()) {
				return -1;
			} else {
				return rs.getInt("timestamp");
			}
		});
	}
}
