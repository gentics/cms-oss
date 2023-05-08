package com.gentics.contentnode.rest.model;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Model for Regex definition
 */
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown=true)
public class RegexModel implements Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 6011055069589211591L;

	/**
	 * Local ID
	 */
	private Integer id;

	/**
	 * Name
	 */
	private String name;

	/**
	 * Description
	 */
	private String description;

	/**
	 * Regex
	 */
	private String expression;

	/**
	 * Local Regex ID
	 * @return ID
	 */
	public Integer getId() {
		return id;
	}

	/**
	 * Set the local ID
	 * @param id local ID
	 */
	public void setId(Integer id) {
		this.id = id;
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
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Description
	 * @return description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Set description
	 * @param description description
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Expression
	 * @return expression
	 */
	public String getExpression() {
		return expression;
	}

	/**
	 * Set expression
	 * @param expression expression
	 */
	public void setExpression(String expression) {
		this.expression = expression;
	}
}
