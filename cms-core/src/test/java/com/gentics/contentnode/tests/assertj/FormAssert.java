package com.gentics.contentnode.tests.assertj;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Form;

/**
 * Assert for forms
 */
public class FormAssert extends PublishableNodeObjectAssert<FormAssert, Form> {

	protected FormAssert(Form actual) {
		super(actual, FormAssert.class);
	}

	public FormAssert hasName(String name) {
		assertThat(actual.getName()).as(descriptionText() + " name").isEqualTo(name);
		return this;
	}

	public FormAssert isDeleted() throws NodeException {
		assertThat(actual.isDeleted()).as(descriptionText() + " deleted").isTrue();
		return this;
	}

	public FormAssert isNotDeleted() throws NodeException {
		assertThat(actual.isDeleted()).as(descriptionText() + " deleted").isFalse();
		return this;
	}

	public FormAssert isLocked() throws NodeException {
		assertThat(actual.isLocked()).as(descriptionText() + " locked").isTrue();
		return myself;
	}

	public FormAssert isNotLocked() throws NodeException {
		assertThat(actual.isLocked()).as(descriptionText() + " locked").isFalse();
		return myself;
	}

	public FormAssert hasFormType(String formType) throws NodeException {
		assertThat(actual.getFormType()).as(descriptionText() + " formType").isEqualTo(formType);
		return this;
	}

	public FormAssert hasFormData(String formData) throws NodeException {
		JsonMapper mapper = JsonMapper.builder().enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS).build();
		try {
			Map<?, ?> expectedFormDataMap = mapper.readValue(formData, Map.class);
			Map<?, ?> actualFormDataMap = mapper.convertValue(actual.getData(), Map.class);

			assertThat(actualFormDataMap).as(descriptionText() + " form data").isEqualTo(expectedFormDataMap);
			return this;
		} catch (JsonProcessingException e) {
			throw new NodeException(e);
		}
	}

	public FormAssert hasFormData(JsonNode formData) throws NodeException {
		// if the actual form data is an ObjectNode (which it should be), we
		// remove the old-ui-schema from it, because we do not want to compare it
		JsonNode actualFormData = actual.getData();
		if (actualFormData instanceof ObjectNode actualFormDataAsObjectNode) {
			actualFormData = actualFormDataAsObjectNode.without("old-ui-schema");
		}

		assertThat(actualFormData).as(descriptionText() + " form data").isEqualTo(formData);
		return this;
	}
}
