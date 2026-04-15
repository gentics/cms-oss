package com.gentics.contentnode.rest.model.formtype;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * Boolean/Checkbox that can be checked
 */
@JsonTypeName(BooleanSetting.TYPE)
public class BooleanSetting extends BaseSetting {
	private static final long serialVersionUID = -121448269254812535L;

	public final static String TYPE = "boolean";

	/**
	 * Creator
	 * @param id ID
	 * @param labelI18n label
	 */
	@JsonCreator
	public BooleanSetting(@JsonProperty(value = "id", required = true) String id,
			@JsonProperty(value = "labelI18n", required = true) Map<String, String> labelI18n) {
		super(id, labelI18n);
	}
}
