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
			if (StringUtils.isNotBlank(stringValue)) {
				if (stringValue.trim().startsWith("[")) {
					JsonArray array = new JsonArray(stringValue);
					IntStream.range(0, array.size()).forEach(i -> resolvableKeys.add(Integer.toString(i)));
				} else {
					JsonObject object = new JsonObject(stringValue);
					resolvableKeys.addAll(object.fieldNames());
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
				if (current instanceof JsonContent jc) {
					if (key.indexOf("[") > 0 && key.endsWith("]")) {
						String subkeyOrIndex = key.substring(key.indexOf("[")+1, key.indexOf("]")+1);
						String rootKey = key.substring(0, key.indexOf("["));
						JsonObject jcc = jc.getObject();
						if (jcc != null) {
							Object root = jcc.getValue(rootKey);
							if (root instanceof JsonArray ja) {
								try {
									int i = Integer.parseInt(subkeyOrIndex);
									if (ja.size() > i) {
										current = ja.getValue(i);
									} else {
										return null;
									}
								} catch (NumberFormatException e) {
								}
							} else if (root instanceof JsonObject jo) {
								current = jo.getValue(subkeyOrIndex);
							} else {
								return null;
							}
						} else {
							return null;
						}
					} else if (jc.isArray()) {
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
				return current;
			}
		}		
		return null;
	}
}
