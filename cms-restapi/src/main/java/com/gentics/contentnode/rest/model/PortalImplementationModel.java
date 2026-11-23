package com.gentics.contentnode.rest.model;

import java.io.Serializable;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Model of a Portal Implementation
 */
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown=true)
public class PortalImplementationModel extends AbstractModel implements Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 2876190528312398437L;

	/**
	 * Local ID
	 */
	private Integer id;

	/**
	 * Keyword
	 */
	private String keyword;

	/**
	 * Name
	 */
	private String name;

	/**
	 * Description
	 */
	private String description;

	/**
	 * Type
	 */
	private Type type;

	/**
	 * Creator
	 */
	private User creator;

	/**
	 * Creation timestamp
	 */
	private int cdate;

	/**
	 * Last editor
	 */
	private User editor;

	/**
	 * Timestamp of last editing
	 */
	private int edate;

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
	 * @return fluent API
	 */
	public PortalImplementationModel setId(Integer id) {
		this.id = id;
		return this;
	}

	/**
	 * Keyword, which identifies the Portal Implementation. Must be unique.
	 * @return keyword
	 */
	public String getKeyword() {
		return keyword;
	}

	/**
	 * Set the keyword
	 * @param keyword keyword
	 * @return fluent API
	 */
	public PortalImplementationModel setKeyword(String keyword) {
		this.keyword = keyword;
		return this;
	}

	/**
	 * Name of the Portal Implementation. Must be unique.
	 * @return name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the name
	 * @param name name
	 * @return fluent API
	 */
	public PortalImplementationModel setName(String name) {
		this.name = name;
		return this;
	}

	/**
	 * Description
	 * @return description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Set the description
	 * @param description description
	 * @return fluent API
	 */
	public PortalImplementationModel setDescription(String description) {
		this.description = description;
		return this;
	}

	/**
	 * Type of the Portal Implementation
	 * @return type
	 */
	public Type getType() {
		return type;
	}

	/**
	 * Set the type
	 * @param type type
	 * @return fluent API
	 */
	public PortalImplementationModel setType(Type type) {
		this.type = type;
		return this;
	}

	/**
	 * User, who created the Portal Implementation
	 * @return creator
	 */
	public User getCreator() {
		return creator;
	}

	/**
	 * Set the creator
	 * @param creator creator
	 * @return fluent API
	 */
	public PortalImplementationModel setCreator(User creator) {
		this.creator = creator;
		return this;
	}

	/**
	 * Creation timestamp
	 * @return creation timestamp
	 */
	public int getCdate() {
		return cdate;
	}

	/**
	 * Set the creation timestamp
	 * @param cdate timestamp
	 * @return fluent API
	 */
	public PortalImplementationModel setCdate(int cdate) {
		this.cdate = cdate;
		return this;
	}

	/**
	 * User, who last edited the Portal Implementation
	 * @return editor
	 */
	public User getEditor() {
		return editor;
	}

	/**
	 * Set the editor
	 * @param editor editor
	 * @return fluent API
	 */
	public PortalImplementationModel setEditor(User editor) {
		this.editor = editor;
		return this;
	}

	/**
	 * Timestamp of last editing
	 * @return edit timestamp
	 */
	public int getEdate() {
		return edate;
	}

	/**
	 * Set the edit timestamp
	 * @param edate timestamp
	 * @return fluent API
	 */
	public PortalImplementationModel setEdate(int edate) {
		this.edate = edate;
		return this;
	}

	/**
	 * Possible types of Portal Implementations
	 */
	public static enum Type {
		/**
		 * PHP based Portal Implementation
		 */
		PHP,

		/**
		 * Java based Portal Implementation
		 */
		Java
	}
}
