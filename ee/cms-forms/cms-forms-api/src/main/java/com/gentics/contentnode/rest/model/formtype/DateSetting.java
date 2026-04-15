package com.gentics.contentnode.rest.model.formtype;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * Date that can be selected. Value will be a ISO-Date string
 */
@JsonTypeName(DateSetting.TYPE)
public class DateSetting extends BaseSetting {
	private static final long serialVersionUID = 2206236044599941701L;

	public final static String TYPE = "date";

	/**
	 * If the editor should also be able to select a time"
	 */
	@JsonProperty
	public boolean allowTime;

	/**
	 * Creator
	 * @param id ID
	 * @param labelI18n label
	 */
	@JsonCreator
	public DateSetting(@JsonProperty(value = "id", required = true) String id,
			@JsonProperty(value = "labelI18n", required = true) Map<String, String> labelI18n) {
		super(id, labelI18n);
	}
}
