package com.gentics.contentnode.rest.model.formtype;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * Integer number that can be entered
 */
@JsonTypeName(IntegerSetting.TYPE)
public class IntegerSetting extends BaseSetting {
	private static final long serialVersionUID = -5310346505920772072L;

	public final static String TYPE = "integer";

	/**
	 * Creator
	 * @param id ID
	 * @param labelI18n label
	 */
	@JsonCreator
	public IntegerSetting(@JsonProperty(value = "id", required = true) String id,
			@JsonProperty(value = "labelI18n", required = true) Map<String, String> labelI18n) {
		super(id, labelI18n);
	}
}
