package com.gentics.contentnode.rest.model;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Model of a ContentRepository Fragment
 */
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown=true)
public class ContentRepositoryFragmentModel extends AbstractModel implements Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 4003797469898693230L;

	/**
	 * Local ID
	 */
	private Integer id;

	/**
	 * Name
	 */
	private String name;

	/**
	 * Local ID
	 * @return local ID
	 */
	public Integer getId() {
		return id;
	}

	/**
	 * Set the local ID
	 * @param id ID
	 */
	public void setId(Integer id) {
		this.id = id;
	}

	/**
	 * Fragment name
	 * @return name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the name
	 * @param name name
	 */
	public void setName(String name) {
		this.name = name;
	}
}
