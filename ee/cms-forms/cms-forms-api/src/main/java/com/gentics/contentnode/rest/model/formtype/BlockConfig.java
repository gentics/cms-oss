package com.gentics.contentnode.rest.model.formtype;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Configuration of a block
 */
public class BlockConfig extends ElementConfig {
	private static final long serialVersionUID = 1993861779319215964L;

	/**
	 * If this block may contain other elements. Used for grouping together related elements
	 */
	@JsonProperty
	public boolean container;

	/**
	 * List of element types which are allowed to be inserted into this container
	 */
	@JsonProperty
	public List<String> whitelist;

	/**
	 * Creator
	 * @param labelI18n label
	 */
	@JsonCreator
	public BlockConfig(@JsonProperty(value = "labelI18n", required = true) Map<String, String> labelI18n) {
		super(labelI18n);
	}
}
