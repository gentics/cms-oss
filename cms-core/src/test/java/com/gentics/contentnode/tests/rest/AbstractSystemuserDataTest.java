package com.gentics.contentnode.tests.rest;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.ClassRule;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Supplier;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.rest.model.response.UserDataResponse;
import com.gentics.contentnode.rest.resource.UserResource;
import com.gentics.contentnode.rest.resource.impl.UserResourceImpl;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Abstract Base Class for Systemuser data tests
 */
public abstract class AbstractSystemuserDataTest {
	public final static String READ_JSON_SQL = "SELECT json from systemuser_data WHERE systemuser_id = ? AND name = ?";

	public final static double STORED_NUMBER = Math.PI;

	public final static String STORED_STRING = "This is the stored textual data";

	public final static boolean STORED_BOOLEAN = true;

	public final static int NODE_USER = 3;

	public final static int GENTICS_USER = 2;

	@ClassRule
	public static DBTestContext context = new DBTestContext();

	@Before
	public void setupData() throws NodeException {
		Trx.operate(() -> {
			DBUtils.executeStatement("DELETE FROM systemuser_data", Transaction.UPDATE_STATEMENT);
		});
	}

	/**
	 * Store the data provided by the supplier dataSupplier for the field fieldName for user userId
	 * @param userId user ID 
	 * @param fieldName field name
	 * @param dataSupplier data supplier
	 * @throws NodeException
	 */
	protected void store(int userId, String fieldName, Supplier<JsonNode> dataSupplier) throws NodeException {
		try (Trx trx = new Trx(Trx.supply(t -> t.getObject(SystemUser.class, userId)))) {
			UserResource res = new UserResourceImpl();
			ContentNodeRESTUtils.assertResponseOK(res.saveUserData(fieldName, dataSupplier.supply()));
			trx.success();
		}
	}

	/**
	 * Read the data for field fieldName for user userId
	 * @param userId user ID
	 * @param fieldName field name
	 * @return read data
	 * @throws NodeException
	 */
	protected JsonNode read(int userId, String fieldName) throws NodeException {
		try (Trx trx = new Trx(Trx.supply(t -> t.getObject(SystemUser.class, userId)))) {
			UserResource res = new UserResourceImpl();
			UserDataResponse response = res.getUserData(fieldName);
			ContentNodeRESTUtils.assertResponseOK(response);
			trx.success();
			return response.getData().isNull() ? null : response.getData();
		}
	}

	/**
	 * Read all user data for the user
	 * @param userId user ID
	 * @return read data
	 * @throws NodeException
	 */
	protected JsonNode read(int userId) throws NodeException {
		try (Trx trx = new Trx(Trx.supply(t -> t.getObject(SystemUser.class, userId)))) {
			UserResource res = new UserResourceImpl();
			UserDataResponse response = res.getAllUserData();
			ContentNodeRESTUtils.assertResponseOK(response);
			trx.success();
			return response.getData().isNull() ? null : response.getData();
		}
	}

	/**
	 * Delete the data for field fieldNAme for the user userId
	 * @param userId user ID
	 * @param fieldName field name
	 * @throws NodeException
	 */
	protected void delete(int userId, String fieldName) throws NodeException {
		try (Trx trx = new Trx(Trx.supply(t -> t.getObject(SystemUser.class, userId)))) {
			UserResource res = new UserResourceImpl();
			ContentNodeRESTUtils.assertResponseOK(res.deleteUserData(fieldName));
			trx.success();
		}
	}

	/**
	 * Directly store the given value in the DB
	 * @param userId user ID
	 * @param fieldName field name
	 * @param json json value
	 * @throws NodeException
	 */
	protected void storeInDB(int userId, String fieldName, String json) throws NodeException {
		Trx.operate(() -> {
			DBUtils.executeUpdate("INSERT INTO systemuser_data (systemuser_id, name, json) VALUES (?, ?, ?)", new Object[] { userId, fieldName, json });
		});
	}

	/**
	 * Assert that the json is stored in the DB correctly
	 * @param userId user ID
	 * @param fieldName field name
	 * @param expected expected json data
	 * @throws NodeException
	 */
	protected void assertStored(int userId, String fieldName, String expected) throws NodeException {
		Trx.operate(() -> {
			String value = DBUtils.select(READ_JSON_SQL, st -> {
				st.setInt(1, userId);
				st.setString(2, fieldName);
			}, rs -> {
				if (rs.next()) {
					return rs.getString("json");
				} else {
					return null;
				}
			});

			assertThat(value).as("Stored json").isEqualTo(expected);
		});
	}

	/**
	 * Assert that no record for the given field exists
	 * @param userId user ID
	 * @param fieldName field name
	 * @throws NodeException
	 */
	protected void assertNotStored(int userId, String fieldName) throws NodeException {
		Trx.operate(() -> {
			DBUtils.executeStatement(READ_JSON_SQL, Transaction.SELECT_STATEMENT, st -> {
				st.setInt(1, userId);
				st.setString(2, fieldName);
			}, rs -> {
				assertThat(rs.next()).as("Record exists").isFalse();
			});
		});
	}

	/**
	 * Tested types
	 */
	public static enum Type {
		NUMBER("numberField", () -> new ObjectMapper().getNodeFactory().numberNode(STORED_NUMBER), String.valueOf(STORED_NUMBER)),
		BOOLEAN("booleanField", () -> new ObjectMapper().getNodeFactory().booleanNode(STORED_BOOLEAN), String.valueOf(STORED_BOOLEAN)),
		NULL("nullField", () -> null, null),
		STRING("stringField", () -> new ObjectMapper().getNodeFactory().textNode(STORED_STRING), "\"" + STORED_STRING + "\""),
		ARRAY("arrayField", () -> {
			ObjectMapper mapper = new ObjectMapper();
			ArrayNode arrayNode = mapper.createArrayNode();
			arrayNode.add(1);
			arrayNode.add("two");
			return arrayNode;
		}, "[1,\"two\"]"),
		OBJECT("objectField", () -> {
			ObjectMapper mapper = new ObjectMapper();
			mapper.createObjectNode();
			ObjectNode objectNode = mapper.getNodeFactory().objectNode();
			objectNode.put("number", 1);
			objectNode.put("string", "two");
			return objectNode;
		}, "{\"number\":1,\"string\":\"two\"}");

		private Supplier<JsonNode> dataSupplier;

		private String expected;

		private String fieldName;

		private Type(String fieldName, Supplier<JsonNode> dataSupplier, String expectedStoredValue) {
			this.fieldName = fieldName;
			this.dataSupplier = dataSupplier;
			this.expected = expectedStoredValue;
		}

		/**
		 * Get the data supplier
		 * @return data supplier
		 */
		public Supplier<JsonNode> getDataSupplier() {
			return dataSupplier;
		}

		/**
		 * Get the expected json representation
		 * @return expected json
		 */
		public String getExpected() {
			return expected;
		}

		/**
		 * Get the field name
		 * @return field name
		 */
		public String getFieldName() {
			return fieldName;
		}
	}
}
