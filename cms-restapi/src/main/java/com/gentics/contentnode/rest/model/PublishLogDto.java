package com.gentics.contentnode.rest.model;


/**
 * Data Transfer Object (DTO) for publish log entries.
 *
 * @param objId the object ID associated with the publish log entry
 * @param type the type of the object being published
 * @param state the state of the publish operation
 * @param user the user who performed the publish operation
 * @param date the date and time when the publish log entry was created
 */
public record PublishLogDto(
		int objId,
		String type,
		String state,
		int user,
		String date
) {

}
