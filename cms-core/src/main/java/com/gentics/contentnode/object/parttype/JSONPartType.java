package com.gentics.contentnode.object.parttype;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.Property.Type;
import com.gentics.contentnode.rest.util.MiscUtils;

/**
 * A parttype for storing JSON content. Parttype ID = 44.
 */
public class JSONPartType extends TextPartType {

	private static final long serialVersionUID = -4534399369711092989L;

	protected JsonNode json;

	protected ArrayNode arrayNode;

	protected ObjectNode objectNode;

	public JSONPartType(Value value) throws NodeException {
		super(value, TextPartType.REPLACENL_EXTENDEDNL2BR);
	}

	@Override
	public Type getPropertyType() {
		return Property.Type.RICHTEXT;
	}

	@Override
	public void setText(String text) throws NodeException {
		super.setText(text);
		try {
			json = null;
			arrayNode = null;
			objectNode = null;

			json = MiscUtils.newObjectMapper().readTree(text);
			if (json instanceof ArrayNode array) {
				arrayNode = array;
			}
			if (json instanceof ObjectNode object) {
				objectNode = object;
			}
		} catch (JsonProcessingException e) {
			throw new NodeException("Invalid JSON");
		}
	}

	@Override
	public Set<String> getResolvableKeys() {
		Set<String> resolvableKeys = new HashSet<>();
		if (arrayNode != null) {
			IntStream.range(0, arrayNode.size()).forEach(i -> resolvableKeys.add(Integer.toString(i)));
		} else if (objectNode != null) {
			objectNode.fieldNames().forEachRemaining(resolvableKeys::add);
		}
		return resolvableKeys;
	}

	@Override
	public Object get(String key) {
		if (arrayNode != null) {
			try {
				int i = Integer.parseInt(key);
				if (arrayNode.size() > i) {
					return arrayNode.get(i);
				} else {
					return null;
				}
			} catch (NumberFormatException e) {
				return null;
			}
		} else if (objectNode != null) {
			return objectNode.get(key);
		} else {
			return null;
		}
	}
}
