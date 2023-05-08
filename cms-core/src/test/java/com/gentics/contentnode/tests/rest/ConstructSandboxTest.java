package com.gentics.contentnode.tests.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import com.gentics.contentnode.rest.resource.parameter.EmbedParameterBean;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Vector;

import org.junit.Rule;
import org.junit.Test;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ConstructCategory;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.rest.model.response.ConstructLoadResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.resource.impl.ConstructResourceImpl;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.lib.db.SQLExecutor;

/**
 * Sandbox Tests for Constructs (and Construct Categories)
 */
public class ConstructSandboxTest {

	@Rule
	public DBTestContext testContext = new DBTestContext();

	/**
	 * ID of the overview construct
	 */
	public final static int CONSTRUCT_ID = 5;

	/**
	 * Name of the test category
	 */
	public final static String TEST_CAT_NAME = "Test Category";

	/**
	 * ID of the editor (systemuser)
	 */
	public final static int EDITOR_ID = 26;

	/**
	 * Test adding/removing permissions on construct categories when they are created/deleted
	 * @throws Exception
	 */
	@Test
	public void testConstructCategoryPermissions() throws Exception {
		// create a construct category
		Transaction t = TransactionManager.getCurrentTransaction();
		ConstructCategory category = t.createObject(ConstructCategory.class);

		category.setName(TEST_CAT_NAME, 1);
		category.save();
		t.commit(false);

		final Integer id = category.getId();

		// check whether permissions have been added for all groups
		final List<Integer> groupIds = new Vector<Integer>();

		DBUtils.executeStatement("SELECT id FROM usergroup", new SQLExecutor() {
			@Override
			public void handleResultSet(ResultSet rs) throws SQLException,
						NodeException {
				while (rs.next()) {
					groupIds.add(rs.getInt("id"));
				}
			}
		});
		final List<Integer> permGroupIds = new Vector<Integer>();
		final String expectedPerm = "10000000000000000000000000000000";

		DBUtils.executeStatement("SELECT * FROM perm WHERE o_type = ? AND o_id = ?", new SQLExecutor() {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				stmt.setInt(1, ConstructCategory.TYPE_CONSTRUCT_CATEGORY);
				stmt.setObject(2, id);
			}

			@Override
			public void handleResultSet(ResultSet rs) throws SQLException,
						NodeException {
				while (rs.next()) {
					assertEquals("Permission settings", expectedPerm, rs.getString("perm"));
					permGroupIds.add(rs.getInt("usergroup_id"));
				}
			}
		});

		// check whether all expected groups have been found
		groupIds.removeAll(permGroupIds);
		assertEquals("Groups without permissions", 0, groupIds.size());

		// now remove the construct category
		category = t.getObject(ConstructCategory.class, id);
		assertNotNull("Load Construct Category", category);
		category.delete();
		t.commit(false);

		DBUtils.executeStatement("SELECT * FROM perm WHERE o_type = ? AND o_id = ?", new SQLExecutor() {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				stmt.setInt(1, ConstructCategory.TYPE_CONSTRUCT_CATEGORY);
				stmt.setObject(2, id);
			}

			@Override
			public void handleResultSet(ResultSet rs) throws SQLException,
						NodeException {
				while (rs.next()) {
					fail("Still found permissions for deleted category");
				}
			}
		});
	}

	/**
	 * Tests whether the construct load method works as expected by loading 
	 * a previously created construct.
	 * @throws Exception
	 */
	@Test
	public void testConstructLoad() throws Exception {
		// create a construct category
		Transaction t = TransactionManager.getCurrentTransaction();
		final ConstructCategory nodeCat = t.createObject(ConstructCategory.class);
		final int TEST_CAT_ORDER = 1;

		nodeCat.setName(TEST_CAT_NAME, 1);
		nodeCat.setSortorder(TEST_CAT_ORDER);
		nodeCat.save();
		t.commit(false);

		// assign a tagtype to it
		Construct construct = t.getObject(Construct.class, CONSTRUCT_ID, true);

		construct.setConstructCategoryId(nodeCat.getId());
		construct.save();
		t.commit(false);

		// get the construct categories
		ConstructLoadResponse response = new ConstructResourceImpl().load(
				ObjectTransformer.getInteger(construct.getId(), -1),
				new EmbedParameterBean().withEmbed("category"));

		com.gentics.contentnode.rest.model.ConstructCategory retrievedCategory = response.getConstruct().getCategory();

		assertEquals("Check response code", ResponseCode.OK, response.getResponseInfo().getResponseCode());
		assertEquals("The construct name did not match the expected one.", construct.getName().toString(), response.getConstruct().getName().toString());
		assertEquals("The category of the construct should match to category of the node construct.", TEST_CAT_NAME, retrievedCategory.getName());

		// Map<String, com.gentics.contentnode.rest.model.ConstructCategory> constructCategories = response.getConstructs();
		// assertTrue("Check whether non-trivial categories were found", constructCategories.size() >= 2);
		// for (com.gentics.contentnode.rest.model.ConstructCategory category : constructCategories.values()) {
		// assertEquals("Check visibility flag for " + category.getName(), true, category.isVisibleInMenu());
		// }

		//
		// for (com.gentics.contentnode.rest.model.Construct constr : constructListResponse.getConstructs()) {
		// assertEquals("Check visibility flag for " + constr.getName(), true, constr.isVisibleInMenu());
		// }
		//
		// // modify the permissions for all groups of the editor
		// SystemUser editor = t.getObject(SystemUser.class, EDITOR_ID);
		// final List<UserGroup> editorGroups = editor.getUserGroups();
		// DBUtils.executeUpdateStatement(
		// "UPDATE perm SET perm = ? WHERE o_type = ? AND o_id = ? AND usergroup_id IN ("
		// + StringUtils.repeat("?", editorGroups.size(), ",")
		// + ")", new SQLExecutor() {
		// @Override
		// public void prepareStatement(PreparedStatement stmt)
		// throws SQLException {
		// stmt.setString(1, "00000000000000000000000000000000");
		// stmt.setInt(2, ConstructCategory.TYPE_CONSTRUCT_CATEGORY);
		// stmt.setObject(3, nodeCat.getId());
		// int i = 4;
		// for (UserGroup group : editorGroups) {
		// stmt.setObject(i++, group.getId());
		// }
		// }
		// });
		// t.commit(false);
		//
		// // start a new transaction for the editor
		// context.getContentNodeFactory().startTransaction(null, EDITOR_ID, true);
		// // get the resource (it will be bound to the new editor transaction)
		// constructResource = getConstructResource();
		//
		// // get the construct categories
		// response = constructResource.list(null, null, null, null, null, null, null, null, null);
		// assertEquals("Check response code", ResponseCode.OK, response.getResponseInfo().getResponseCode());
		//
		// //		constructCategories = response.getConstructCategories();
		// //		assertTrue("Check whether non-trivial categories were found", constructCategories.size() >= 2);
		// //		for (com.gentics.contentnode.rest.model.ConstructCategory category : constructCategories.values()) {
		// //			assertEquals("Check visibility flag for " + category.getName(), !TEST_CAT_NAME.equals(category.getName()), category.isVisibleInMenu());
		// //		}
		//
		// // get the construct list
		// constructListResponse = constructResource.list(0,
		// -1, null, null, null, null, null, ConstructSortAttribute.name,
		// SortOrder.asc);
		// assertEquals("Check response code", ResponseCode.OK, constructListResponse.getResponseInfo().getResponseCode());
		//
		// for (com.gentics.contentnode.rest.model.Construct constr : constructListResponse.getConstructs()) {
		// assertEquals("Check visibility flag for " + constr.getName(), CONSTRUCT_ID != constr.getId(), constr.isVisibleInMenu());
		// }
	}

	/**
	 * The id of the test template.  This template contains 9 tags, that have a
	 * text part that require digits as valid values.
	 */
	public final static int TEMPLATE_ID = 94;

	/**
	 * Retrieves the test template in read/write mode.
	 *
	 * @return Template object in read/write mode.
	 */
	public static Template getTestTemplate() throws NodeException {
		return ((Template)
				TransactionManager.getCurrentTransaction().getObject(Template.class, TEMPLATE_ID, true)
				);
	}
}
