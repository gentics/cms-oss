package com.gentics.contentnode.object.parttype;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.Property.Type;
import com.gentics.mesh.core.rest.node.field.JsonContent;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * A parttype for storing JSON content. Parttype ID = 44.
 */
public class JSONPartType extends TextPartType {

	private static final long serialVersionUID = -4534399369711092989L;

	public JSONPartType(Value value) throws NodeException {
		super(value, TextPartType.REPLACENL_EXTENDEDNL2BR);
	}

	@Override
	public Type getPropertyType() {
		return Property.Type.RICHTEXT;
	}

	@Override
	public Set<String> getResolvableKeys() {
		Set<String> resolvableKeys = new HashSet<>();
		Value value = getValueObject();
		if (value != null) {
			String stringValue = value.getValueText();
			Object current = JsonContent.fromString(stringValue);
			if (current instanceof JsonContent jc) {
				if (jc.isArray()) {
					IntStream.range(0, jc.getArray().size()).forEach(i -> resolvableKeys.add(Integer.toString(i)));
				} else {
					resolvableKeys.addAll(jc.getObject().fieldNames());
				}
			}
		}
		return resolvableKeys;
	}

	@Override
	public Object get(String key) {
		Value value = getValueObject();
		if (value != null) {
			String stringValue = value.getValueText();
			if (StringUtils.isNotBlank(stringValue)) {
				Object current = JsonContent.fromString(stringValue);
				if (StringUtils.isNotBlank(key)) {
					if (current instanceof JsonContent jc) {
						if (jc.isArray()) {
							try {
								int i = Integer.parseInt(key);
								JsonArray ja = jc.getArray();
								if (ja.size() > i) {
									current = ja.getValue(i);
								} else {
									return null;
								}
							} catch (NumberFormatException e) {
								return null;
							}
						} else {
							current = jc.getObject().getValue(key);
						}
					} else if (current instanceof JsonObject jo) {
						current = jo.getValue(key);
					} else {
						return null;
					}
				}
				return current;
			}
		}		
		return null;
	}
}
