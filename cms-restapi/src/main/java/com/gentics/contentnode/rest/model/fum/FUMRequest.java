package com.gentics.contentnode.rest.model.fum;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gentics.contentnode.rest.model.User;

/**
 * Request to the external FUM URL
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FUMRequest {
	private String id;

	private int fileid;

	private String filename;

	private String mimetype;

	private String url;

	private String postponeurl;

	private String lang;

	private Map<String, Object> folder;

	private Map<String, Object> options;

	private User user;

	/**
	 * Temporary file name
	 * @return file name
	 */
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	/**
	 * Internal file ID
	 * @return internal file ID
	 */
	public int getFileid() {
		return fileid;
	}

	public void setFileid(int fileid) {
		this.fileid = fileid;
	}

	/**
	 * Name of the file
	 * @return
	 */
	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	/**
	 * Detected mimetype
	 * @return
	 */
	public String getMimetype() {
		return mimetype;
	}

	public void setMimetype(String mimetype) {
		this.mimetype = mimetype;
	}

	/**
	 * URL to get the file contents
	 * @return
	 */
	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * Postback URL for finishing postponed FUM actions
	 * @return
	 */
	public String getPostponeurl() {
		return postponeurl;
	}

	public void setPostponeurl(String postponeurl) {
		this.postponeurl = postponeurl;
	}

	/**
	 * Language of the user
	 * @return
	 */
	public String getLang() {
		return lang;
	}

	public void setLang(String lang) {
		this.lang = lang;
	}

	/**
	 * Folder data
	 * @return
	 */
	public Map<String, Object> getFolder() {
		return folder;
	}

	public void setFolder(Map<String, Object> folder) {
		this.folder = folder;
	}

	/**
	 * Additional options
	 * @return
	 */
	public Map<String, Object> getOptions() {
		return options;
	}

	public void setOptions(Map<String, Object> options) {
		this.options = options;
	}

	/**
	 * User
	 * @return
	 */
	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}
}
