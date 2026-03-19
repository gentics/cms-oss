package com.gentics.contentnode.rest.model.formtype;

import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.Size;

/**
 * Picker where a reference to one or more CMS Objects can be selected
 */
public class ReferenceSetting extends BaseSetting {
	private static final long serialVersionUID = -4930905191871790064L;

	/**
	 * Allowed referenced types
	 */
	@JsonProperty
	public Set<ReferenceType> referenceTypes;

	@JsonCreator
	public ReferenceSetting(@JsonProperty(value = "id", required = true) String id,
			@JsonProperty(value = "labelI18n", required = true) Map<String, String> labelI18n,
			@JsonProperty(value = "referenceTypes", required = true) @Size(min = 1) Set<ReferenceType> referenceTypes) {
		super(id, labelI18n);
		this.referenceTypes = referenceTypes;
	}
}
