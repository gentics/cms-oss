package com.gentics.contentnode.publish.mesh;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Model of translations map. Keys are the language codes, values are maps of key -> translation
 */
public class TranslationsModel extends LinkedHashMap<String, Map<String, String>> {
	private static final long serialVersionUID = 5855937677838746817L;
}
