package com.gentics.contentnode.rest.model;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * dumbed down representation of a page
 * @author clemens
 */
@XmlRootElement
public class SimplePage implements Serializable {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 6544293060579318477L;

	/**
	 * Id of the page
	 */
	private Integer id;

	/**
	 * Name of the page
	 */
	private String name;

	/**
	 * Path to the page, separated by '/', starting and ending with '/'
	 */
	private String path;

	/**
	 * Online flag
	 */
	private Boolean online;

	/**
	 * Modified flag
	 */
	private Boolean modified;

	/**
	 * URL to the page
	 */
	private String url;

	/**
	 * Constructor used by JAXB
	 */
	public SimplePage() {}

	/**
	 * @return the id
	 */
	public Integer getId() {
		return this.id;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Online flag
	 * @return online flag
	 */
	public Boolean getOnline() {
		return online;
	}

	/**
	 * Modified flag
	 * @return modified flag
	 */
	public Boolean getModified() {
		return modified;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(Integer id) {
		this.id = id;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Set online flag
	 * @param online online flag
	 */
	public void setOnline(Boolean online) {
		this.online = online;
	}

	/**
	 * Set modified flag
	 * @param modified modified flag
	 */
	public void setModified(Boolean modified) {
		this.modified = modified;
	}

	/**
	 * Get the URL
	 * @return the URL
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * Set the URL to the page
	 * @param url URL to the page
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * Get the path to the page
	 * @return path to the page
	 */
	public String getPath() {
		return path;
	}

	/**
	 * Set the path to the page
	 * @param path path to the page
	 */
	public void setPath(String path) {
		this.path = path;
	}
}
