package com.gentics.contentnode.rest.deserializer;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.gentics.contentnode.rest.model.formtype.OptionsSetting;

/**
 * Deserializer for {@link OptionsSetting#multiple}
 */
public class BooleanOrEditableDeserializer extends JsonDeserializer<Object> {
	private static final String CONSTANT_VALUE = "editable";

	@Override
	public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
		if (p.currentToken().isBoolean()) {
			return p.getBooleanValue();
		} else if (p.currentToken().isScalarValue()) {
			String value = p.getValueAsString();
			// Optionally validate that it matches your constant
			if (value.equals(CONSTANT_VALUE)) {
				return value;
			}
			throw new JsonMappingException(p, "String value must be '" + CONSTANT_VALUE + "'");
		}
		throw new JsonMappingException(p, "Expected boolean or string, but got " + p.currentToken());
	}
}
