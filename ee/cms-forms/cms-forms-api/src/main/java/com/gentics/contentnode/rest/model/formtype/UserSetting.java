package com.gentics.contentnode.rest.model.formtype;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * Select where one or more options from the user options can be selected
 */
@JsonTypeName(UserSetting.TYPE)
public class UserSetting extends BaseSetting {
	private static final long serialVersionUID = 19895567674627946L;

	protected final static String TYPE = "user";

	/**
	 * If multiple values can be selected
	 */
	@JsonProperty
	public boolean multiple;

	/**
	 * Creator
	 * @param id ID
	 * @param labelI18n label
	 */
	@JsonCreator
	public UserSetting(@JsonProperty(value = "id", required = true) String id,
			@JsonProperty(value = "labelI18n", required = true) Map<String, String> labelI18n) {
		super(id, labelI18n);
	}
}
