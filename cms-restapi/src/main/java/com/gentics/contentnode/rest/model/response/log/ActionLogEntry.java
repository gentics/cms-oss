package com.gentics.contentnode.rest.model.response.log;

import java.io.Serializable;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Model of an entry in the action log
 */
@XmlRootElement
public class ActionLogEntry implements Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 8791062465173831019L;

	private int id;

	private String user;

	private ActionModel action;

	private ActionLogType type;

	private int objId;

	private int timestamp;

	private String info;

	/**
	 * Entry ID
	 * @return ID
	 */
	public int getId() {
		return id;
	}

	/**
	 * Set the ID
	 * @param id ID
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * Lastname and Firstname of the acting User
	 * @return user name
	 */
	public String getUser() {
		return user;
	}

	/**
	 * Set the user name
	 * @param user name
	 */
	public void setUser(String user) {
		this.user = user;
	}

	/**
	 * Performed action
	 * @return action
	 */
	public ActionModel getAction() {
		return action;
	}

	/**
	 * Set action
	 * @param action
	 */
	public void setAction(ActionModel action) {
		this.action = action;
	}

	/**
	 * Type of the object, the action was performed on
	 * @return type
	 */
	public ActionLogType getType() {
		return type;
	}

	/**
	 * Set object type
	 * @param type
	 */
	public void setType(ActionLogType type) {
		this.type = type;
	}

	/**
	 * ID of the object, the action was performed on
	 * @return object ID
	 */
	public int getObjId() {
		return objId;
	}

	/**
	 * Set object ID
	 * @param objId ID
	 */
	public void setObjId(int objId) {
		this.objId = objId;
	}

	/**
	 * Timestamp of the action
	 * @return timestamp
	 */
	public int getTimestamp() {
		return timestamp;
	}

	/**
	 * Set the timestamp
	 * @param timestamp timestamp
	 */
	public void setTimestamp(int timestamp) {
		this.timestamp = timestamp;
	}

	/**
	 * Additional info
	 * @return info
	 */
	public String getInfo() {
		return info;
	}

	/**
	 * Set info
	 * @param info info
	 */
	public void setInfo(String info) {
		this.info = info;
	}
}
