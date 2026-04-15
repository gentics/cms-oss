package com.gentics.contentnode.rest.model.formtype;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.gentics.contentnode.rest.deserializer.BooleanOrEditableDeserializer;

/**
 * Allow the editor to create options which the user can then use
 */
@JsonTypeName(OptionsSetting.TYPE)
public class OptionsSetting extends BaseSetting {
	private static final long serialVersionUID = -2269868773759926805L;

	public final static String TYPE = "options";

	/**
	 * If a boolean, defines if the rendered select for the end-user is multiple or not; If 'editable' the editor can define it.
	 */
	@JsonProperty(defaultValue = "false")
	@JsonDeserialize(using = BooleanOrEditableDeserializer.class)
	public Object multiple;

	/**
	 * Creator
	 * @param id ID
	 * @param labelI18n label
	 */
	@JsonCreator
	public OptionsSetting(@JsonProperty(value = "id", required = true) String id,
			@JsonProperty(value = "labelI18n", required = true) Map<String, String> labelI18n) {
		super(id, labelI18n);
	}
}
