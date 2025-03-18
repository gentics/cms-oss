/*
 * @author norbert
 * @date 14.03.2011
 * @version $Id: Group.java,v 1.1.2.2 2011-03-17 13:38:55 norbert Exp $
 */
package com.gentics.contentnode.rest.model;

import java.io.Serializable;
import java.util.List;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Group information in REST calls
 */
@XmlRootElement
public class Group implements Serializable {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 6850453396089817188L;

	/**
	 * id of the Group object
	 */
	private Integer id;

	/**
	 * name of the group
	 */
	private String name;

	/**
	 * Description of the group
	 */
	private String description;

	/**
	 * Children of this group
	 */
	private List<Group> children;

	/**
	 * Empty constructor
	 */
	public Group() {}

	/**
	 * Group ID
	 * @return the id
	 */
	public Integer getId() {
		return id;
	}

	/**
	 * Group name
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Description
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(Integer id) {
		this.id = id;
	}

	/**
	 * Set group name
	 * @param name the name to set
	 * @return fluent API
	 */
	public Group setName(String name) {
		this.name = name;
		return this;
	}

	/**
	 * Set description
	 * @param description the description to set
	 * @return fluent API
	 */
	public Group setDescription(String description) {
		this.description = description;
		return this;
	}

	/**
	 * List of child groups
	 * @return children
	 */
	public List<Group> getChildren() {
		return children;
	}

	/**
	 * Set the children
	 * @param children children
	 */
	public void setChildren(List<Group> children) {
		this.children = children;
	}

	@Override
	public String toString() {
		return String.format("Group %s (%d)", name, id);
	}
}
