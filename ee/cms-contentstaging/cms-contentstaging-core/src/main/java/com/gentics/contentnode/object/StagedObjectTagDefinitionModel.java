package com.gentics.contentnode.object;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gentics.contentnode.devtools.model.ObjectTagDefinitionTypeModel;
import com.gentics.contentnode.rest.model.AbstractModel;

/**
 * Model of an object tag definition, which is used for generation of the implementation hash
 */
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class StagedObjectTagDefinitionModel extends AbstractModel {
	/**
	 * Object tag definition type
	 */
	private ObjectTagDefinitionTypeModel type;

	/**
	 * Keyword
	 */
	private String keyword;

	/**
	 * Construct ID
	 */
	private String constructId;

	/**
	 * Get the type
	 * @return type
	 */
	public ObjectTagDefinitionTypeModel getType() {
		return type;
	}

	/**
	 * Set the type
	 * @param type type
	 * @return fluent API
	 */
	public StagedObjectTagDefinitionModel setType(ObjectTagDefinitionTypeModel type) {
		this.type = type;
		return this;
	}

	/**
	 * Get the keyword
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
	public StagedObjectTagDefinitionModel setKeyword(String keyword) {
		this.keyword = keyword;
		return this;
	}

	/**
	 * Get the construct ID
	 * @return construct ID
	 */
	public String getConstructId() {
		return constructId;
	}

	/**
	 * Set the construct ID
	 * @param constructId construct ID
	 * @return fluent API
	 */
	public StagedObjectTagDefinitionModel setConstructId(String constructId) {
		this.constructId = constructId;
		return this;
	}
}
