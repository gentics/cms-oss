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
	 * Constructs a new PublishLogEntry with the specified object ID, type, status, and user. Used to
	 * persist entity to database. The id and date fields are auto generated.
	 *
	 * @param objId  the object ID
	 * @param type   the type of the object
	 * @param status the status of the publish operation
	 * @param user   the user performing the operation
	 */
	public PublishLogEntry(int objId, String type, int status, int user) {
		this.objId = objId;
		this.type = type;
		this.state = status;
		this.user = user;
	}

	/**
	 * Constructs a new PublishLogEntry with the specified ID, object ID, type, status, user, and
	 * date.
	 *
	 * @param id     the ID of the log entry
	 * @param objId  the object ID
	 * @param type   the type of the object
	 * @param status the status of the publish operation
	 * @param user   the user performing the operation
	 * @param date   the date of the log entry
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
	 * @param type  the type of object
	 * @param objId the id of the object
	 * @return an Optional containing the publish log entry if found, otherwise empty
	 * @throws NodeException if an error occurs during the load operation
	 */
	public Optional<PublishLogEntry> loadByTypeAndId(String type, int objId) throws NodeException {
		var query = "SELECT * FROM publish_protocol WHERE type = ? AND obj_id = ? ORDER BY id DESC";
		return DBUtils.select(query, stmt -> {
			stmt.setString(1, type);
			stmt.setInt(2, objId);
		}, resultSet -> {
			if (resultSet.next()) {
				var id = resultSet.getInt("id");
				var state = resultSet.getBoolean("state") ? 1 : 0;
				var user = resultSet.getInt("user");
				var timestamp = resultSet.getTimestamp("date");
				var dateTime = timestamp.toLocalDateTime();

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
		return DBUtils.select("SELECT * FROM publish_protocol ORDER BY ID DESC", resultSet -> {
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


	/**
	 * Gets the ID of the publish log entry.
	 *
	 * @return the ID of the publish log entry
	 */
	public int getId() {
		return id;
	}

	/**
	 * Sets the ID of the publish log entry.
	 *
	 * @param id the ID to set
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * Gets the object ID associated with the publish log entry.
	 *
	 * @return the object ID
	 */
	public int getObjId() {
		return objId;
	}

	/**
	 * Sets the object ID associated with the publish log entry.
	 *
	 * @param objId the object ID to set
	 */
	public void setObjId(int objId) {
		this.objId = objId;
	}

	/**
	 * Gets the type of the object being published.
	 *
	 * @return the type of the object
	 */
	public String getType() {
		return type;
	}

	/**
	 * Sets the type of the object being published.
	 *
	 * @param type the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * Gets the state of the publish operation.
	 *
	 * @return the state of the publish operation
	 */
	public int getState() {
		return state;
	}

	/**
	 * Sets the state of the publish operation.
	 *
	 * @param state the state to set
	 */
	public void setState(int state) {
		this.state = state;
	}

	/**
	 * Gets the user who performed the publish operation.
	 *
	 * @return the user who performed the publish operation
	 */
	public int getUser() {
		return user;
	}

	/**
	 * Sets the user who performed the publish operation.
	 *
	 * @param user the user to set
	 */
	public void setUser(int user) {
		this.user = user;
	}

	/**
	 * Gets the date and time when the publish log entry was created.
	 *
	 * @return the date and time of the publish log entry
	 */
	public LocalDateTime getDate() {
		return date;
	}

	/**
	 * Sets the date and time when the publish log entry was created.
	 *
	 * @param date the date and time to set
	 */
	public void setDate(LocalDateTime date) {
		this.date = date;
	}

}
