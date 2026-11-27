package com.gentics.contentnode.devtools.model;

import java.io.Serializable;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gentics.contentnode.rest.model.AbstractModel;
import com.gentics.contentnode.rest.model.PortalImplementationModel.Type;

/**
 * Model of a Portal Implementation in a devtool package (contents of the structure file).
 * The contained files are stored as plain files in the package, so they are not part of this model.
 */
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class PortalImplementationModel extends AbstractModel implements Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 1868218469283066946L;

	private String keyword;

	private String name;

	private String description;

	private Type type;

	/**
	 * Keyword
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
	 * Name
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
	 * Type
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
}
