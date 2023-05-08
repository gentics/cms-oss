package com.gentics.contentnode.job;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Wastebin;
import com.gentics.contentnode.factory.WastebinFilter;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.lib.db.SQLExecutor;

/**
 * Abstract base class for localize callables
 */
public abstract class AbstractLocalizeCallable implements Callable<GenericResponse> {
	/**
	 * Channel ID for localization
	 */
	protected int channelId;

	/**
	 * True to disable instant publishing
	 */
	protected boolean disableInstantPublish;

	/**
	 * Create an instance for localizing the given object
	 * @param clazz object class
	 * @param objectId object ID
	 * @param channelId channel ID
	 * @param disableInstantPublish true to disable instant publishing
	 * @return callable instance
	 * @throws NodeException
	 */
	public static AbstractLocalizeCallable get(Class<? extends NodeObject> clazz, int objectId, int channelId, boolean disableInstantPublish)
			throws NodeException {
		if (Page.class.isAssignableFrom(clazz)) {
			return new LocalizePageCallable(objectId, channelId, disableInstantPublish);
		} else if (Folder.class.isAssignableFrom(clazz)) {
			return new LocalizeFolderCallable(objectId, channelId, disableInstantPublish);
		} else if (ImageFile.class.isAssignableFrom(clazz)) {
			return new LocalizeImageCallable(objectId, channelId, disableInstantPublish);
		} else if (File.class.isAssignableFrom(clazz)) {
			return new LocalizeFileCallable(objectId, channelId, disableInstantPublish);
		} else if (Template.class.isAssignableFrom(clazz)) {
			return new LocalizeTemplateCallable(objectId, channelId, disableInstantPublish);
		}

		throw new NodeException("Cannot localize object of " + clazz);
	}

	/**
	 * Create an instance for localization in the given channel
	 * @param channelId channel ID
	 * @param disableInstantPublish true to disable instant publishing
	 */
	public AbstractLocalizeCallable(int channelId, boolean disableInstantPublish) {
		this.channelId = channelId;
		this.disableInstantPublish = disableInstantPublish;
	}

	/**
	 * Check for conflicting channelset variants. If conflicting objects in the wastebin are found, they are removed from the wastebin
	 * @param clazz object clazz
	 * @param channelSetId channelset id
	 * @return true if a conflicting object is found, false if not
	 * @throws NodeException
	 */
	protected boolean checkConflictingObject(Class<? extends NodeObject> clazz, int channelSetId) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Map<Integer, Boolean> channelVariants = new HashMap<Integer, Boolean>();
		DBUtils.executeStatement("SELECT id, deleted FROM " + t.getTable(clazz) + " WHERE channelset_id = ? AND channel_id = ?", new SQLExecutor() {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				stmt.setInt(1, channelSetId);
				stmt.setInt(2, channelId);
			}

			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				while (rs.next()) {
					channelVariants.put(rs.getInt("id"), rs.getInt("deleted") > 0);
				}
			}
		});

		for (Map.Entry<Integer, Boolean> entry : channelVariants.entrySet()) {
			Integer variantId = entry.getKey();
			boolean deleted = entry.getValue();

			if (deleted) {
				try (WastebinFilter filter = Wastebin.INCLUDE.set()) {
					t.getObject(clazz, variantId, true).delete(true);
				}
			} else {
				return true;
			}
		}

		return false;
	}
}
