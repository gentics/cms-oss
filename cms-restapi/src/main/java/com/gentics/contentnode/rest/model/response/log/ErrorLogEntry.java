package com.gentics.contentnode.rest.model.response.log;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Model of a logged error
 */
@XmlRootElement
public class ErrorLogEntry implements Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 7206563093490152725L;

	private int id;

	private String sid;

	private String user;

	private int haltId;

	private String request;

	private int errorDo;

	private int timestamp;

	private String detail;

	private String stacktrace;

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
	 * @return fluent API
	 */
	public ErrorLogEntry setId(int id) {
		this.id = id;
		return this;
	}

	/**
	 * SID of the session
	 * @return SID
	 */
	public String getSid() {
		return sid;
	}

	/**
	 * Set the SID
	 * @param sid SID
	 * @return fluent API
	 */
	public ErrorLogEntry setSid(String sid) {
		this.sid = sid;
		return this;
	}

	/**
	 * Lastname and Firstname of the acting User
	 * @return user name
	 */
	public String getUser() {
		return user;
	}

	/**
	 * Set user name
	 * @param user name
	 * @return fluent API
	 */
	public ErrorLogEntry setUser(String user) {
		this.user = user;
		return this;
	}

	/**
	 * Halt ID
	 * @return halt ID
	 */
	public int getHaltId() {
		return haltId;
	}

	/**
	 * Set the halt ID
	 * @param haltId halt ID
	 * @return fluent API
	 */
	public ErrorLogEntry setHaltId(int haltId) {
		this.haltId = haltId;
		return this;
	}

	/**
	 * Request
	 * @return request
	 */
	public String getRequest() {
		return request;
	}

	/**
	 * Set the request
	 * @param request
	 * @return fluent API
	 */
	public ErrorLogEntry setRequest(String request) {
		this.request = request;
		return this;
	}

	/**
	 * Error DO
	 * @return DO
	 */
	public int getErrorDo() {
		return errorDo;
	}

	/**
	 * Set the error DO
	 * @param errorDo DO
	 * @return fluent API
	 */
	public ErrorLogEntry setErrorDo(int errorDo) {
		this.errorDo = errorDo;
		return this;
	}

	/**
	 * Timestamp
	 * @return timestamp
	 */
	public int getTimestamp() {
		return timestamp;
	}

	/**
	 * Set the timestamp
	 * @param timestamp
	 * @return fluent API
	 */
	public ErrorLogEntry setTimestamp(int timestamp) {
		this.timestamp = timestamp;
		return this;
	}

	/**
	 * Details about error
	 * @return details
	 */
	public String getDetail() {
		return detail;
	}

	/**
	 * Set details
	 * @param detail
	 * @return fluent API
	 */
	public ErrorLogEntry setDetail(String detail) {
		this.detail = detail;
		return this;
	}

	/**
	 * Error stacktrace
	 * @return stacktrace
	 */
	public String getStacktrace() {
		return stacktrace;
	}

	/**
	 * Set the stacktrace
	 * @param stacktrace
	 * @return fluent API
	 */
	public ErrorLogEntry setStacktrace(String stacktrace) {
		this.stacktrace = stacktrace;
		return this;
	}
}
