package com.gentics.contentnode.rest.model.formtype;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Controls in a form type configuration
 */
public class Controls extends LinkedHashMap<String, ControlConfig> {
	private static final long serialVersionUID = -4458659862850019195L;

	/**
	 * Empty constructor
	 */
	public Controls() {
	}

	/**
	 * Create instance as copy of the given map
	 * @param map map to copy
	 */
	public Controls(Map<String, ControlConfig> map) {
		Optional.ofNullable(map).ifPresent(this::putAll);
	}
}
