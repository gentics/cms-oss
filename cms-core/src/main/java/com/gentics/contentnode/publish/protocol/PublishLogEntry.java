package com.gentics.contentnode.publish.protocol;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Represents a log entry entity for the publish protocol.
 */
public class PublishLogEntry {

	/**
	 * The identifier for the publish log entry.
	 */
	private int id;

	/**
	 * The object ID associated with the publish log entry.
	 */
	private int objId;

	/**
	 * The type of the object being published.
	 */
	private String type;

	/**
	 * The state of the publish operation.
	 */
	private int state;

	/**
	 * The user who performed the publish operation.
	 */
	private int user;

	/**
	 * The date and time when the publish log entry was created.
	 */
	private LocalDateTime date;

	public PublishLogEntry() {
	}

	/**
	 * Constructs a new PublishLogEntry with the specified object ID, type, status, and user.
	 * Used to persist entity to database. The id and date fields are auto generated.
	 *
	 * @param objId the object ID
	 * @param type the type of the object
	 * @param status the status of the publish operation
	 * @param user the user performing the operation
	 */
	public PublishLogEntry(int objId, String type, int status, int user) {
		this.objId = objId;
		this.type = type;
		this.state = status;
		this.user = user;
	}

	/**
	 * Constructs a new PublishLogEntry with the specified ID, object ID, type, status, user, and date.
	 *
	 * @param id the ID of the log entry
	 * @param objId the object ID
	 * @param type the type of the object
	 * @param status the status of the publish operation
	 * @param user the user performing the operation
	 * @param date the date of the log entry
	 */
	public PublishLogEntry(int id, int objId, String type, int status, int user, LocalDateTime date) {
		this.id = id;
		this.objId = objId;
		this.type = type;
		this.state = status;
		this.user = user;
		this.date = date;
	}


	/**
	 * Saves the publish log entry to the database.
	 *
	 * @throws NodeException if an error occurs during the save operation
	 */
	public void save() throws NodeException {
		DBUtils.executeInsert(
				"INSERT INTO publish_protocol (obj_id, type, state, user) VALUES (?, ?, ?, ?)",
				new Object[]{
						this.getObjId(),
						this.getType(),
						this.getState(),
						this.getUser(),
				});
	}

	/**
	 * Loads a publish log entry by a specified field and value.
	 *
	 * @param fieldName the name of the field
	 * @param value the value of the field
	 * @return an Optional containing the publish log entry if found, otherwise empty
	 * @throws NodeException if an error occurs during the load operation
	 */
	public Optional<PublishLogEntry> loadByField(String fieldName, int value) throws NodeException {
		var query = String.format("SELECT * FROM publish_protocol WHERE %s = %s ORDER BY id DESC", fieldName, value);
		return DBUtils.select(query, resultSet -> {
			if (resultSet.next()) {
				var objId = resultSet.getInt("obj_id");
				var type = resultSet.getString("type");
				var state = resultSet.getBoolean("state") ? 1 : 0;
				var user = resultSet.getInt("user");
				var timestamp = resultSet.getTimestamp("date");
				LocalDateTime dateTime = timestamp.toLocalDateTime();

				return Optional.of(new PublishLogEntry(id, objId, type, state, user, dateTime));
			}
			return Optional.empty();
		});
	}

	/**
	 * Loads all publish log entries from the database.
	 *
	 * @return a list of publish log entries
	 * @throws NodeException if an error occurs during the load operation
	 */
	public List<PublishLogEntry> loadAll() throws NodeException {
		return DBUtils.select("SELECT * FROM publish_protocol", resultSet -> {
			List<PublishLogEntry> entries = new ArrayList<>();
			while (resultSet.next()) {
				var id = resultSet.getInt("id");
				var objId = resultSet.getInt("obj_id");
				var type = resultSet.getString("type");
				var state = resultSet.getInt("state");
				var user = resultSet.getInt("user");
				var timestamp = resultSet.getTimestamp("date");
				LocalDateTime dateTime = timestamp.toLocalDateTime();

				entries.add(new PublishLogEntry(id, objId, type, state, user, dateTime));
			}
			return entries;
		});
	}


	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getObjId() {
		return objId;
	}

	public void setObjId(int objId) {
		this.objId = objId;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public int getState() {
		return state;
	}

	public void setState(int state) {
		this.state = state;
	}

	public int getUser() {
		return user;
	}

	public void setUser(int user) {
		this.user = user;
	}

	public LocalDateTime getDate() {
		return date;
	}

	public void setDate(LocalDateTime date) {
		this.date = date;
	}

}
