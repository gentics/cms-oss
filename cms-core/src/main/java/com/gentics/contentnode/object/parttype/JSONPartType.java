package com.gentics.contentnode.object.parttype;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.exception.RestMappedException;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.ValueContainer;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.Property.Type;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.util.MiscUtils;
import com.gentics.mesh.json.JsonUtil;

import jakarta.ws.rs.core.Response.Status;

/**
 * A parttype for storing JSON content. Parttype ID = 44.
 */
public class JSONPartType extends TextPartType {

	private static final long serialVersionUID = -4534399369711092989L;

	protected JsonNode json;

	protected ArrayNode arrayNode;

	protected ObjectNode objectNode;

	/**
	 * Flag, which is set when the text is not valid JSON.
	 * (no initializer, because {@link #parseText()} is already called by the constructor of the superclass)
	 */
	protected boolean invalidJson;

	public JSONPartType(Value value) throws NodeException {
		super(value, TextPartType.REPLACENL_EXTENDEDNL2BR);
	}

	@Override
	public Type getPropertyType() {
		return Property.Type.RICHTEXT;
	}

	@Override
	public String parseText() throws NodeException {
		String parsedText = super.parseText();
		json = null;
		arrayNode = null;
		objectNode = null;
		invalidJson = false;

		if (StringUtils.isNotBlank(parsedText)) {
			try {
				json = MiscUtils.newObjectMapper().readTree(parsedText);
				if (json instanceof ArrayNode array) {
					arrayNode = array;
				}
				if (json instanceof ObjectNode object) {
					objectNode = object;
				}
			} catch (JsonProcessingException e) {
				// invalid JSON must not fail here (the part type is also created when the text is set), so that it can be rejected by validateValue() when saving
				invalidJson = true;
			}
		}
		return parsedText;
	}

	@Override
	public String render(RenderResult result, String template) throws NodeException {
		if (invalidJson) {
			throw new NodeException("Invalid JSON");
		}
		return super.render(result, template);
	}

	@Override
	public void validateValue(Part part, Value value, ValueContainer container, Function<String, RestMappedException> exceptionSupplier) throws NodeException {
		String stringValue = value.getValueText();
		if (StringUtils.isEmpty(stringValue)) {
			// Nothing to validate
			return;
		}
		ObjectMapper objectMapper = MiscUtils.newObjectMapper();
		try {
			JsonNode jsonNode = objectMapper.readTree(stringValue);
			if (!StringUtils.isEmpty(part.getInfoText())) {
				JsonNode jsonSchemaContent = objectMapper.readTree(part.getInfoText());
				JsonNode[] allowedSchemas = null;
				if (jsonSchemaContent.isArray()) {
					ArrayNode jsonSchemas = (ArrayNode)jsonSchemaContent;

					allowedSchemas = IntStream.range(0, jsonSchemas.size()).mapToObj(jsonSchemas::get)
							.filter(JsonNode::isObject).map(ObjectNode.class::cast).toArray(size -> new JsonNode[size]);
				} else {
					allowedSchemas = new JsonNode[] { jsonSchemaContent };
				}
				if (allowedSchemas != null && Arrays.asList(allowedSchemas).stream().noneMatch(schema1 -> JsonUtil.validate(schema1, jsonNode) == Boolean.TRUE)) {
					throw exceptionSupplier.apply(I18NHelper.get("validation.jsonschema.nomatch"))
						.setMessageType(Message.Type.CRITICAL).setResponseCode(ResponseCode.INVALIDDATA).setStatus(Status.BAD_REQUEST);
					}
			}
		} catch (JsonProcessingException e) {
			throw exceptionSupplier.apply(I18NHelper.get("validation.json.unparseable"))
				.setMessageType(Message.Type.CRITICAL).setResponseCode(ResponseCode.INVALIDDATA).setStatus(Status.BAD_REQUEST);
		}
	}

	@Override
	public String getPartValidationMessageKey() {
		return "validation.json.part.failed";
	}

	@Override
	public String getTagPartValidationMessageKey() {
		return "validation.json.tag.part.failed";
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
