package com.gentics.contentnode.tests.rest;

import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;

/**
 * Test cases for storing, reading and deleting various types
 */
@RunWith(value = Parameterized.class)
public class SystemuserStoreReadDataTest extends AbstractSystemuserDataTest {
	@Parameters(name = "{index}: type {0}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (Type type : Type.values()) {
			data.add(new Object[] { type });
		}
		return data;
	}

	@Parameter(0)
	public Type type;

	@Test
	public void testStore() throws NodeException {
		String fieldName = type.getFieldName();

		store(NODE_USER, fieldName, type.getDataSupplier());
		assertStored(NODE_USER, fieldName, type.getExpected());
	}

	@Test
	public void testRead() throws NodeException {
		String fieldName = type.getFieldName();

		storeInDB(NODE_USER, fieldName, type.getExpected());
		assertThat(read(NODE_USER, fieldName)).as("Read data").isEqualTo(type.getDataSupplier().supply());
	}

	@Test
	public void testDelete() throws NodeException {
		String fieldName = type.getFieldName();

		storeInDB(NODE_USER, fieldName, type.getExpected());
		delete(NODE_USER, fieldName);
		assertNotStored(NODE_USER, fieldName);
		assertThat(read(NODE_USER, fieldName)).as("Read data").isNull();
	}

	@Test
	public void testDataIntegrity() throws NodeException {
		String fieldName = type.getFieldName();

		store(NODE_USER, fieldName, type.getDataSupplier());
		assertThat(read(GENTICS_USER, fieldName)).as("Data read for other user").isNull();
	}
}
