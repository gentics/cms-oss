package com.gentics.contentnode.rest.model.fum;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Response model for request to finish postponed FUM action
 */
@XmlRootElement
public class FUMStatusResponse {
	private FUMStatus status;

	private String type;

	private String msg;

	/**
	 * Status
	 * @return
	 */
	public FUMStatus getStatus() {
		return status;
	}

	public FUMStatusResponse setStatus(FUMStatus status) {
		this.status = status;
		return this;
	}

	/**
	 * Type
	 * @return
	 */
	public String getType() {
		return type;
	}

	public FUMStatusResponse setType(String type) {
		this.type = type;
		return this;
	}

	/**
	 * Message
	 * @return
	 */
	public String getMsg() {
		return msg;
	}

	public FUMStatusResponse setMsg(String msg) {
		this.msg = msg;
		return this;
	}
}
