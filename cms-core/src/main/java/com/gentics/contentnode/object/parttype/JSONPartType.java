package com.gentics.contentnode.object.parttype;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.Property.Type;
import com.gentics.mesh.core.rest.node.field.JsonContent;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ParseContext;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;

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
	public Object get(String key) {
		Value value = getValueObject();
		if (value != null) {
			String stringValue = value.getValueText();
			if (StringUtils.isNotBlank(stringValue)) {
				if (key.startsWith("$")) {
					JsonPath jsonPath = JsonPath.compile(stringValue);
					ParseContext context = JsonPath.using(new JacksonJsonProvider());
					List<?> parsed = context.parse(stringValue).read(jsonPath, List.class);
					// TODO make consistent over any number of results, including 0?
					if (parsed.size() == 1) {
						return parsed.get(0);
					} else if (parsed.size() > 1) {
						return parsed;
					}
				} else {
					Object current = new JsonContent().setString(stringValue);
					String[] parts = key.split("\\.");
					for (String part : parts) {
						if (current instanceof JsonContent jc) {
							if (part.indexOf("[") > 0 && part.endsWith("]")) {
								String subkeyOrIndex = part.substring(part.indexOf("[")+1, part.indexOf("]")+1);
								String rootKey = part.substring(0, part.indexOf("["));
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
								current = jc.getArray();
							} else {
								current = jc.getObject();
							}
						} else if (current instanceof JsonObject jo) {
							current = jo.getValue(part);
						} else {
							return null;
						}
					}
					return current;
				}
			}
		}		
		return null;
	}
}
