package com.gentics.contentnode.rest.model.formtype;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Blocks in a form type configuration
 */
public class Blocks extends LinkedHashMap<String, BlockConfig> {
	private static final long serialVersionUID = 4568988146352298072L;

	/**
	 * Empty constructor
	 */
	public Blocks() {
	}

	/**
	 * Create instance as copy of the given map
	 * @param map map to copy
	 */
	public Blocks(Map<String, BlockConfig> map) {
		Optional.ofNullable(map).ifPresent(this::putAll);
	}
}
