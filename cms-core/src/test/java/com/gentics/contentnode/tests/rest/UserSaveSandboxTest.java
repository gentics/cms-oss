package com.gentics.contentnode.tests.rest;

import static org.junit.Assert.*;

import org.junit.Rule;
import org.junit.Test;

import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.rest.model.User;
import com.gentics.contentnode.rest.model.request.UserSaveRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.resource.UserResource;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestUtils;
import com.gentics.contentnode.testutils.DBTestContext;

public class UserSaveSandboxTest {

	@Rule
	public DBTestContext testContext = new DBTestContext();

	@Test
	public void testSaveUser() throws Exception {

		SystemUser testUser;
		try (Trx trx = new Trx()) {
			Transaction t = TransactionManager.getCurrentTransaction();
			testUser = t.createObject(SystemUser.class);
			testUser.setDescription("description");
			testUser.setEmail("user@nowhere.tld");
			testUser.setFirstname("Joe");
			testUser.setLastname("Doe");
			testUser.setPassword("geheim");
			testUser.setLogin("joe123");
			testUser.save();
			trx.success();
		}

		// 1. Just update password
		UserResource userResource = ContentNodeRESTUtils.getUserResource();
		UserSaveRequest request = new UserSaveRequest();
		User user = new User();
		user.setPassword("vielgeheimer");
		request.setUser(user);
		GenericResponse response = userResource.save(testUser.getId(), request);
		ContentNodeTestUtils.assertResponseCodeOk(response);

		try (Trx trx = new Trx()) {
			Transaction t = TransactionManager.getCurrentTransaction();
			SystemUser reloaded = t.getObject(SystemUser.class, testUser.getId());
			assertFalse("The password should have been updated.", reloaded.getPassword().equalsIgnoreCase("geheim"));
		}

		// 2. Update other fields as well
		request = new UserSaveRequest();
		user = new User();
		user.setPassword("vielgeheimer");
		user.setDescription("changedDescription");
		user.setLastName("changedLastname");
		user.setFirstName("changedFirstname");
		user.setLogin("changedLogin");
		user.setEmail("changedEmail");
		request.setUser(user);
		response = userResource.save(testUser.getId(), request);
		ContentNodeTestUtils.assertResponseCodeOk(response);

		try (Trx trx = new Trx()) {
			Transaction t = TransactionManager.getCurrentTransaction();
			SystemUser reloaded = t.getObject(SystemUser.class, testUser.getId());
			assertFalse("The password should have been updated.", reloaded.getPassword().equalsIgnoreCase("geheim"));
			assertEquals("changedDescription", reloaded.getDescription());
			assertEquals("changedEmail", reloaded.getEmail());
			assertEquals("changedLogin", reloaded.getLogin());
			assertEquals("changedFirstname", reloaded.getFirstname());
			assertEquals("changedLastname", reloaded.getLastname());

		}

	}
}
