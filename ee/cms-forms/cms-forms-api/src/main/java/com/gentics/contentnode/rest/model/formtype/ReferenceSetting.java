package com.gentics.contentnode.rest.model.formtype;

import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import jakarta.validation.constraints.Size;

/**
 * Picker where a reference to one or more CMS Objects can be selected
 */
@JsonTypeName(ReferenceSetting.TYPE)
public class ReferenceSetting extends BaseSetting {
	private static final long serialVersionUID = -4930905191871790064L;

	protected final static String TYPE = "reference";

	/**
	 * Allowed referenced types
	 */
	@JsonProperty
	public Set<ReferenceType> referenceTypes;

	/**
	 * Set of allowed markup languages for pages
	 */
	@JsonProperty
	public Set<Integer> allowedMarkupLanguageIds;

	/**
	 * If multiple values can be selected
	 */
	@JsonProperty
	public boolean multiple;

	/**
	 * Title in the repository browser which is displayed to the editor
	 */
	@JsonProperty
	public Map<String, String> titleI18n;

	/**
	 * Whether the repository browser shall be restricted to the current node
	 */
	@JsonProperty
	public boolean onlyInCurrentNode;

	@JsonCreator
	public ReferenceSetting(@JsonProperty(value = "id", required = true) String id,
			@JsonProperty(value = "labelI18n", required = true) Map<String, String> labelI18n,
			@JsonProperty(value = "referenceTypes", required = true) @Size(min = 1) Set<ReferenceType> referenceTypes) {
		super(id, labelI18n);
		this.referenceTypes = referenceTypes;
	}
}
