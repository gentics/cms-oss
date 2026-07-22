package com.gentics.contentnode.tests.assertj;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Form;

/**
 * Assert for forms
 */
public class FormAssert extends PublishableNodeObjectAssert<FormAssert, Form> {

	protected FormAssert(Form actual) {
		super(actual, FormAssert.class);
	}

	public FormAssert isDeleted() throws NodeException {
		assertThat(actual.isDeleted()).as(descriptionText() + " deleted").isTrue();
		return this;
	}

	public FormAssert isNotDeleted() throws NodeException {
		assertThat(actual.isDeleted()).as(descriptionText() + " deleted").isFalse();
		return this;
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
		assertThat(actual.getData()).as(descriptionText() + " form data").isEqualTo(formData);
		return this;
	}
}
