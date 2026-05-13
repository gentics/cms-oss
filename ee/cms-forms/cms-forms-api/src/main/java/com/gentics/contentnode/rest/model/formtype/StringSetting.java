package com.gentics.contentnode.rest.model.formtype;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * Simple string that can be entered
 */
@JsonTypeName(StringSetting.TYPE)
public class StringSetting extends BaseSetting {
	private static final long serialVersionUID = -6854988596492614954L;

	public final static String TYPE = "string";

	/**
	 * Whether rich content can be entered
	 */
	@JsonProperty
	public boolean richContent;

	/**
     * If it should render a multiline input (textarea) for editing instead.
     */
	@JsonProperty
	public boolean multiline;

	/**
	 * If the editor should be able to enter multiple values.
	 */
	@JsonProperty
	public boolean multiple;

	/**
	 * Creator
	 * @param id ID
	 * @param labelI18n label
	 */
	@JsonCreator
	public StringSetting(@JsonProperty(value = "id", required = true) String id,
			@JsonProperty(value = "labelI18n", required = true) Map<String, String> labelI18n) {
		super(id, labelI18n);
	}
}
