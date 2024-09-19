package com.gentics.contentnode.publish.protocol;

import static com.gentics.contentnode.object.Form.TYPE_FORM;
import static com.gentics.contentnode.object.Page.TYPE_PAGE;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.object.PublishableNodeObject;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.model.ContentNodeItem;
import com.gentics.contentnode.rest.model.PublishableContentItem;
import com.gentics.lib.log.NodeLogger;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing publish protocol entries.
 */
public class PublishProtocolUtil {

	private static final NodeLogger logger = NodeLogger.getNodeLogger(PublishProtocolUtil.class);

	/**
	 * Logs the publish state of a given object.
	 *
	 * @param <T>    the type of the publishable node object
	 * @param object the publishable node object
	 * @param status the status of the publish operation
	 * @param user   the user performing the operation
	 * @throws NodeException if an error occurs during logging
	 */
	public static <T extends PublishableNodeObject> void logPublishState(T object, int status, int user)
			throws NodeException {
		var publishLogEntry = new PublishLogEntry(object.getId(), getType(object.getTType()), status,
				user);

		updatePublishStateOfNodeObject(object, status, user);

		logger.info(
				String.format("Storing publish state information of '%s' with id '%s' with state '%s'.",
						publishLogEntry.getType(), publishLogEntry.getObjId(), publishLogEntry.getState()));
		publishLogEntry.save();
	}


	/**
	 * Updates the publish state of the given PublishableNodeObject.
	 *
	 * @param object the PublishableNodeObject whose publish state is to be updated
	 * @param status the new status to set for the object
	 * @param user   the ID of the user performing the update
	 * @throws NodeException if an error occurs while updating the publish state
	 */
	private static void updatePublishStateOfNodeObject(PublishableNodeObject object, int status,
			int user)
			throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		int timestamp = TransactionManager.getCurrentTransaction().getUnixTimestamp();

		if (PublishState.ONLINE.getValue() == status) {
			DBUtils.update("UPDATE page SET unpublished_date = ?, unpublisher = ? WHERE id = ?", 0, 0,
					object.getId());
			DBUtils.update("UPDATE page SET pdate = ?, publisher = ? WHERE id = ?", timestamp, user,
					object.getId());
		} else if (PublishState.OFFLINE.getValue() == status) {
			DBUtils.update("UPDATE page SET pdate = ?, publisher = ? WHERE id = ?", 0, 0, object.getId());
			DBUtils.update("UPDATE page SET unpublished_date = ?, unpublisher = ? WHERE id = ?",
					timestamp, user, object.getId());
		}
	}

	/**
	 * Retrieves a publish log entry by its object ID.
	 *
	 * @param objId the object ID of the publish log entry
	 * @return the publish log entry
	 * @throws NodeException           if an error occurs during retrieval
	 * @throws EntityNotFoundException if the entry is not found
	 */
	public static PublishLogEntry getPublishLogEntryByObjectId(String type, int objId)
			throws NodeException {
		return new PublishLogEntry().loadByTypeAndId(type, objId).orElseThrow(
				() -> new EntityNotFoundException(I18NHelper.get("publish_protocol.entry_not_found",
						String.valueOf(type), String.valueOf(objId))));
	}


	/**
	 * Retrieves all publish log entries.
	 *
	 * @return a list of publish log entries
	 */
	public static List<PublishLogEntry> getPublishLogEntries() {
		try {
			return new PublishLogEntry().loadAll();
		} catch (Exception e) {
			logger.error("Something went wrong while retrieving the publish protocol", e);
			return new ArrayList<>();
		}
	}


	/**
	 * Retrieves the last unpublished entries for a given type.
	 *
	 * @param type   the type of entries to retrieve
	 * @param objIds the list of object IDs to filter the entries
	 * @return a list of PublishLogEntry objects that are the last unpublished entries for the
	 * specified type
	 */
	public static List<PublishLogEntry> getLastUnpublishedEntriesForType(String type,
			List<Integer> objIds) {
		try {
			return new PublishLogEntry().loadManyByType(type, PublishState.OFFLINE.toString(), objIds);
		} catch (Exception e) {
			logger.error("Something went wrong while retrieving the publish protocol", e);
			return new ArrayList<>();
		}
	}

	/**
	 * Adds unpublished information to the provided list of Page objects.
	 *
	 * @param restModel   the list of ContentNodeItem objects to which unpublished information will be added
	 * @param publishType the publish type e.g.: Page, Form, etc.
	 */
	public static <T extends PublishableContentItem> void addUnpublishedInformation(List<T> restModel, String publishType) {
		var modelIds = restModel.stream().map(ContentNodeItem::getId).toList();
		var logEntries = getLastUnpublishedEntriesForType(publishType, modelIds);
		logEntries.forEach(publishLogEntry ->
				restModel.stream().filter(model -> model.getId() == publishLogEntry.getObjId())
						.findAny().ifPresent(
								model -> model.setUnpublishedDate((int) publishLogEntry.getDate().toEpochSecond(
										ZoneOffset.UTC))));
	}

	/**
	 * Gets the type of the publishable node object.
	 *
	 * @param ttype the type code of the publishable node object
	 * @return the type as a string
	 */
	private static String getType(int ttype) {
		return switch (ttype) {
			case TYPE_PAGE -> PublishType.PAGE.toString();
			case TYPE_FORM -> PublishType.FORM.toString();
			default -> PublishType.OTHER.toString();
		};
	}

}
