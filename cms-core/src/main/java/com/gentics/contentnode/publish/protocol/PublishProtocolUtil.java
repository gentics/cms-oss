package com.gentics.contentnode.publish.protocol;

import static com.gentics.contentnode.object.Form.TYPE_FORM;
import static com.gentics.contentnode.object.Page.TYPE_PAGE;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.object.PublishableNodeObject;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
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

		logger.info(
				String.format("Storing publish state information of '%s' with id '%s' with state '%s'.",
						publishLogEntry.getType(), publishLogEntry.getObjId(), publishLogEntry.getState()));
		publishLogEntry.save();
	}

	/**
	 * Retrieves a publish log entry by its object ID.
	 *
	 * @param objId the object ID of the publish log entry
	 * @return the publish log entry
	 * @throws NodeException           if an error occurs during retrieval
	 * @throws EntityNotFoundException if the entry is not found
	 */
	public static PublishLogEntry getPublishLogEntryByObjectId(String type, int objId) throws NodeException {
		return new PublishLogEntry().loadByTypeAndId(type, objId).orElseThrow(
				() -> new EntityNotFoundException(I18NHelper.get("publish_protocol.entry_not_found",
						String.valueOf(type), String.valueOf(objId))));
	}


	/**
	 * Retrieves all publish log entries.
	 *
	 * @return a list of publish log entries
	 */
	public static List<PublishLogEntry> getLastUnpublishedEntriesForType() {
		try {
			return new PublishLogEntry().loadAll();
		} catch (Exception e) {
			logger.error("Something went wrong while retrieving the publish protocol", e);
			return new ArrayList<>();
		}
	}


	public static List<PublishLogEntry> getLastUnpublishedEntriesForType(String type, List<Integer> objIds) {
		try {
			return new PublishLogEntry().loadManyByType(type, PublishState.OFFLINE.toString() ,objIds);
		} catch (Exception e) {
			logger.error("Something went wrong while retrieving the publish protocol", e);
			return new ArrayList<>();
		}
	}

	/**
	 * Adds unpublished information to the provided list of Page objects.
	 *
	 * @param restPages the list of Page objects to which unpublished information will be added
	 */
	public static void addUnpublishedInformation(List<com.gentics.contentnode.rest.model.Page> restPages) {
		var pageIds = restPages.stream().map(page -> page.getId()).toList();
		var logEntries = getLastUnpublishedEntriesForType(PublishType.PAGE.toString(), pageIds);
		logEntries.forEach(publishLogEntry ->
				restPages.stream().filter(page -> page.getId() == publishLogEntry.getObjId())
						.findAny().ifPresent(page -> page.setUnpublishedDate((int)publishLogEntry.getDate().toEpochSecond(
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
