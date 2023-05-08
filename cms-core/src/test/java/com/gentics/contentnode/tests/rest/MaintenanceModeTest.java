package com.gentics.contentnode.tests.rest;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.rest.util.MiscUtils.setPermissions;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.NODE_GROUP_ID;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createSystemUser;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createUserGroup;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.assertRequiredPermissions;
import static org.assertj.core.api.Assertions.fail;

import java.util.Arrays;

import javax.ws.rs.client.Entity;

import org.apache.commons.lang3.tuple.Triple;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.NodeSetup.NODESETUP_KEY;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.Permissions;
import com.gentics.contentnode.perm.TypePerms;
import com.gentics.contentnode.rest.client.exceptions.MaintenanceModeRestException;
import com.gentics.contentnode.rest.client.exceptions.RestException;
import com.gentics.contentnode.rest.model.request.MaintenanceModeRequest;
import com.gentics.contentnode.rest.model.response.MaintenanceResponse;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.RESTAppContext;
import com.gentics.contentnode.testutils.RESTAppContext.LoggedInClient;
import com.gentics.contentnode.testutils.RESTAppContext.Type;

/**
 * Test cases for the maintenance mode
 */
public class MaintenanceModeTest {
	private static DBTestContext testContext = new DBTestContext();

	/**
	 * REST App context
	 */
	private static RESTAppContext restContext = new RESTAppContext(Type.jetty);

	@ClassRule
	public static RuleChain chain = RuleChain.outerRule(testContext).around(restContext);

	private static UserGroup group;
	private static SystemUser user;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		group = supply(() -> createUserGroup("TestGroup", NODE_GROUP_ID));
		user = supply(() -> createSystemUser("Tester", "Tester", null, "tester", "tester", Arrays.asList(group)));
	}

	@Before
	public void setup() throws NodeException {
		operate(() -> {
			DBUtils.update("DELETE FROM nodesetup WHERE name IN (?, ?)", NODESETUP_KEY.maintenancemode.toString(),
					NODESETUP_KEY.maintenancebanner.toString());
		});
	}

	@Test
	public void testActivate() throws NodeException, RestException {
		assertRequiredPermissions(group, "tester", "tester", restContext, target -> {
			return target.path("admin").path("maintenance").request()
					.post(Entity.json(new MaintenanceModeRequest().setMaintenance(true)), MaintenanceResponse.class);
		}, Triple.of(PermHandler.TYPE_ADMIN, 1, PermHandler.PERM_VIEW),
				Triple.of(PermHandler.TYPE_MAINTENCANCE, 1, PermHandler.PERM_VIEW));

		// revoke permission on system maintenance
		operate(() -> setPermissions(TypePerms.systemmaintenance, 1, Arrays.asList(group), PermHandler.EMPTY_PERM, false));
		// login is expected to fail
		try (LoggedInClient client = restContext.client("tester", "tester")) {
			fail("The login is expected to fail");
		} catch (MaintenanceModeRestException e) {
			// this is expected
		}

		// grant permission on system maintenance
		operate(() -> setPermissions(TypePerms.systemmaintenance, 1, Arrays.asList(group), Permissions.get(PermHandler.PERM_VIEW).toString(), false));
		// login is expected to work
		try (LoggedInClient client = restContext.client("tester", "tester")) {
		}
	}
}
