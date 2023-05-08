package com.gentics.contentnode.tests.rest;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.api.lib.exception.NodeException;

/**
 * Test cases for reading all user data
 */
public class SystemuserReadAllTest extends AbstractSystemuserDataTest {
	/**
	 * Test reading all data
	 * @throws NodeException
	 */
	@Test
	public void testReadAll() throws NodeException {
		for (Type type : Type.values()) {
			storeInDB(NODE_USER, type.getFieldName(), type.getExpected());
		}

		JsonNode data = read(NODE_USER);
		assertThat(data).as("Read data").isNotNull();

		for (Type type : Type.values()) {
			String fieldName = type.getFieldName();
			if (type.equals(Type.NULL)) {
				assertThat(data.get(type.getFieldName())).as("Data for " + fieldName).isNotNull().isEqualTo(new ObjectMapper().getNodeFactory().nullNode());
			} else {
				assertThat(data.get(type.getFieldName())).as("Data for " + fieldName).isNotNull().isEqualTo(type.getDataSupplier().supply());
			}
		}
	}

	/**
	 * Test reading all data for another user
	 * @throws NodeException
	 */
	@Test
	public void testDataIntegrity() throws NodeException {
		for (Type type : Type.values()) {
			storeInDB(NODE_USER, type.getFieldName(), type.getExpected());
		}

		JsonNode data = read(GENTICS_USER);
		assertThat(data).as("Read data for other user").isNotNull();

		for (Type type : Type.values()) {
			String fieldName = type.getFieldName();
			assertThat(data.has(fieldName)).as("Data for " + fieldName + " is present").isFalse();
		}
	}
}
