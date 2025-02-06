package com.gentics.contentnode.object;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gentics.contentnode.rest.model.AbstractModel;
import com.gentics.contentnode.rest.model.OverviewSetting;

/**
 * Model of a part, which is used for generation of the implementation hash
 */
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class StagedPartModel extends AbstractModel {
	/**
	 * Keyword
	 */
	private String keyword;

	/**
	 * Editable flag
	 */
	private boolean editable;

	/**
	 * Id of the part type
	 */
	private int typeId;

	/**
	 * Id of the metalanguage
	 */
	private int mlId;

	/**
	 * Overview setting for overview parts
	 */
	private OverviewSetting overviewSetting;

	/**
	 * Default value for all other part types
	 */
	private StagedValueModel defaultValue;

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
	public StagedPartModel setKeyword(String keyword) {
		this.keyword = keyword;
		return this;
	}

	/**
	 * Get the editable flag
	 * @return flag
	 */
	public boolean isEditable() {
		return editable;
	}

	/**
	 * Set the editable flag
	 * @param editable flag
	 * @return fluent API
	 */
	public StagedPartModel setEditable(boolean editable) {
		this.editable = editable;
		return this;
	}

	/**
	 * Get the type ID
	 * @return type ID
	 */
	public int getTypeId() {
		return typeId;
	}

	/**
	 * Set the type ID
	 * @param typeId type ID
	 * @return fluent API
	 */
	public StagedPartModel setTypeId(int typeId) {
		this.typeId = typeId;
		return this;
	}

	/**
	 * Get the metalanguage ID
	 * @return metalanguage ID
	 */
	public int getMlId() {
		return mlId;
	}

	/**
	 * Set the metalanguage ID
	 * @param mlId metalanguage ID
	 * @return fluent API
	 */
	public StagedPartModel setMlId(int mlId) {
		this.mlId = mlId;
		return this;
	}

	/**
	 * Get the overview settings of the part (for overview parts)
	 * @return overview settings
	 */
	public OverviewSetting getOverviewSetting() {
		return overviewSetting;
	}

	/**
	 * Set the overview settings
	 * @param overviewSetting overview settings
	 * @return fluent API
	 */
	public StagedPartModel setOverviewSetting(OverviewSetting overviewSetting) {
		this.overviewSetting = overviewSetting;
		return this;
	}

	/**
	 * Get the default value (for neither text nor overview parts)
	 * @return default value
	 */
	public StagedValueModel getDefaultValue() {
		return defaultValue;
	}

	/**
	 * Get the default value
	 * @param defaultValue default value
	 * @return fluent API
	 */
	public StagedPartModel setDefaultValue(StagedValueModel defaultValue) {
		this.defaultValue = defaultValue;
		return this;
	}
}
