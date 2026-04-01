package com.gentics.contentnode.rest.model.formtype;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * Text which may be entered on a per language basis
 */
@JsonTypeName(TranslationSetting.TYPE)
public class TranslationSetting extends BaseSetting {
	private static final long serialVersionUID = 871244316182096624L;

	protected final static String TYPE = "translation";

	/**
	 * Whether rich content can be entered
	 */
	@JsonProperty
	public boolean richContent;

	/**
	 * Creator
	 * @param id ID
	 * @param labelI18n label
	 */
	@JsonCreator
	public TranslationSetting(@JsonProperty(value = "id", required = true) String id,
			@JsonProperty(value = "labelI18n", required = true) Map<String, String> labelI18n) {
		super(id, labelI18n);
	}
}
