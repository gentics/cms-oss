package com.gentics.contentnode.publish.protocol;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PublishLogEntry {

	private int id;

	private int objId;

	private String type;

	private int state;

	private int user;

	private Date date;

	public PublishLogEntry() {
	}

	public PublishLogEntry(int objId, String type, int status, int user) {
		this.objId = objId;
		this.type = type;
		this.state = status;
		this.user = user;
	}

	public PublishLogEntry(int id, int objId, String type, int status, int user, Date date) {
		this.id = id;
		this.objId = objId;
		this.type = type;
		this.state = status;
		this.user = user;
		this.date = date;
	}

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

	public PublishLogEntry load(int id) throws NodeException {
		return DBUtils.select("SELECT * FROM publish_protocol WHERE id = " + id, resultSet -> {
			int objId = resultSet.getInt(1);
			String type = resultSet.getString(2);
			int state = resultSet.getInt(3);
			int user = resultSet.getInt(4);
			Date date = resultSet.getDate(5);

			return new PublishLogEntry(id, objId, type, state, user, date);
		});
	}

	public List<PublishLogEntry> loadAll() throws NodeException {
		return DBUtils.select("SELECT * FROM publish_protocol", resultSet -> {
			List<PublishLogEntry> entries = new ArrayList<>();
			while (resultSet.next()) {
				int id = resultSet.getInt("id");
				int objId = resultSet.getInt("obj_id");
				String type = resultSet.getString("type");
				int state = resultSet.getInt("state");
				int user = resultSet.getInt("user");
				Date date = resultSet.getDate("date");

				entries.add(new PublishLogEntry(id, objId, type, state, user, date));
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

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

}
