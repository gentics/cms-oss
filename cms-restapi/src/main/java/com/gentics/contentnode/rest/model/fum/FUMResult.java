package com.gentics.contentnode.rest.model.fum;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * FUM Result Model
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FUMResult {
	private FUMResponseStatus status;

	private String msg;

	private String filename;

	private String mimetype;

	private String url;

	/**
	 * FUM status
	 * @return
	 */
	public FUMResponseStatus getStatus() {
		return status;
	}

	public FUMResult setStatus(FUMResponseStatus status) {
		this.status = status;
		return this;
	}

	/**
	 * FUM Message
	 * @return
	 */
	public String getMsg() {
		return msg;
	}

	public FUMResult setMsg(String msg) {
		this.msg = msg;
		return this;
	}

	/**
	 * Optional new filename
	 * @return
	 */
	public String getFilename() {
		return filename;
	}

	public FUMResult setFilename(String filename) {
		this.filename = filename;
		return this;
	}

	/**
	 * Optional new mimetype
	 * @return
	 */
	public String getMimetype() {
		return mimetype;
	}

	public FUMResult setMimetype(String mimetype) {
		this.mimetype = mimetype;
		return this;
	}

	/**
	 * Optional URL to download file contents (if FUM modified the file)
	 * @return
	 */
	public String getUrl() {
		return url;
	}

	public FUMResult setUrl(String url) {
		this.url = url;
		return this;
	}
}
