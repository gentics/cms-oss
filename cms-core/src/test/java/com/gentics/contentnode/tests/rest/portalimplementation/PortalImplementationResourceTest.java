package com.gentics.contentnode.tests.rest.portalimplementation;

import static com.gentics.contentnode.perm.PermHandler.PERM_CHANGE_PERM;
import static com.gentics.contentnode.perm.PermHandler.PERM_CONTENTREPOSITORY_CREATE;
import static com.gentics.contentnode.perm.PermHandler.PERM_CONTENTREPOSITORY_DELETE;
import static com.gentics.contentnode.perm.PermHandler.PERM_CONTENTREPOSITORY_UPDATE;
import static com.gentics.contentnode.perm.PermHandler.PERM_VIEW;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.PortalImplementation;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.Permissions;
import com.gentics.contentnode.rest.client.exceptions.RestException;
import com.gentics.contentnode.rest.model.PortalImplementationListResponse;
import com.gentics.contentnode.rest.model.PortalImplementationModel;
import com.gentics.contentnode.rest.model.PortalImplementationModel.Type;
import com.gentics.contentnode.rest.model.request.SetPermsRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.PortalImplementationResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.tests.assertj.GCNAssertions;
import com.gentics.contentnode.testutils.Creator;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.RESTAppContext;
import com.gentics.contentnode.testutils.RESTAppContext.LoggedInClient;

/**
 * Test cases for the REST resource /portalimplementations
 */
public class PortalImplementationResourceTest {
	private static DBTestContext testContext = new DBTestContext();

	private static RESTAppContext restContext = new RESTAppContext();

	@ClassRule
	public static RuleChain chain = RuleChain.outerRule(testContext).around(restContext);

	private final static String PATH = "portalimplementations";

	/**
	 * Admin user (member of the node group)
	 */
	private final static String ADMIN_LOGIN = "piadmin";

	/**
	 * Restricted user (member of a subgroup of the node group)
	 */
	private final static String USER_LOGIN = "piuser";

	private final static String PASSWORD = "test";

	private static UserGroup nodeGroup;

	private static UserGroup subGroup;

	private static SystemUser admin;

	private static SystemUser user;

	/**
	 * Initial permissions on the admin type for the node group, as set by the changelog
	 */
	private static String initialNodeGroupPerm;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		if (TransactionManager.getCurrentTransactionOrNull() != null) {
			TransactionManager.getCurrentTransaction().commit();
		}

		// read the initial permissions before any test changes them
		initialNodeGroupPerm = Trx.supply(() -> DBUtils.select(
				"SELECT perm FROM perm WHERE usergroup_id = 2 AND o_type = " + PortalImplementation.TYPE_PORTAL_IMPLEMENTATIONS, rs -> {
					return rs.next() ? rs.getString("perm") : null;
				}));

		nodeGroup = Trx.supply(t -> t.getObject(UserGroup.class, 2));
		subGroup = Trx.supply(() -> Creator.createUsergroup("PI Subgroup", "", nodeGroup));
		admin = Trx.supply(() -> Creator.createUser(ADMIN_LOGIN, PASSWORD, "PI", "Admin", "", Arrays.asList(nodeGroup)));
		user = Trx.supply(() -> Creator.createUser(USER_LOGIN, PASSWORD, "PI", "User", "", Arrays.asList(subGroup)));
	}

	@Before
	public void setup() throws NodeException {
		Trx.operate(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			for (PortalImplementation impl : t.getObjects(PortalImplementation.class,
					DBUtils.select("SELECT id FROM portal_implementation", DBUtils.IDS))) {
				impl.delete(true);
			}
		});

		// the node group has all permissions, the subgroup has only access to the admin parts (but not on portal implementations)
		Trx.operate(() -> {
			PermHandler.setPermissions(PortalImplementation.TYPE_PORTAL_IMPLEMENTATIONS, Arrays.asList(nodeGroup), PermHandler.FULL_PERM);
			PermHandler.setPermissions(PermHandler.TYPE_ADMIN, Arrays.asList(subGroup), Permissions.get(PERM_VIEW).toString());
			PermHandler.setPermissions(PermHandler.TYPE_CONADMIN, Arrays.asList(subGroup), Permissions.get(PERM_VIEW).toString());
			PermHandler.setPermissions(PortalImplementation.TYPE_PORTAL_IMPLEMENTATIONS, Arrays.asList(subGroup), PermHandler.EMPTY_PERM);
		});
	}

	/**
	 * Test that the changelog grants the node group the same permissions on portal implementations as on contentrepositories
	 * @throws NodeException
	 */
	@Test
	public void testInitialAdminPermissions() throws NodeException {
		String crPerm = Trx.supply(() -> DBUtils.select(
				"SELECT perm FROM perm WHERE usergroup_id = 2 AND o_type = 10207", rs -> {
					return rs.next() ? rs.getString("perm") : null;
				}));
		assertThat(crPerm).as("Permissions on ContentRepositories").isNotNull();
		assertThat(initialNodeGroupPerm).as("Initial permissions on Portal Implementations").isEqualTo(crPerm);
	}

	/**
	 * Test creating a portal implementation with all attributes
	 * @throws Exception
	 */
	@Test
	public void testCreate() throws Exception {
		int before = (int) (System.currentTimeMillis() / 1000);
		PortalImplementationModel created = create(ADMIN_LOGIN, model("php", "PHP Portal", Type.PHP).setDescription("The description"));
		int after = (int) (System.currentTimeMillis() / 1000);

		assertThat(created.getId()).as("ID").isNotNull().isPositive();
		assertThat(created.getGlobalId()).as("Global ID").isNotBlank();
		assertThat(created.getKeyword()).as("Keyword").isEqualTo("php");
		assertThat(created.getName()).as("Name").isEqualTo("PHP Portal");
		assertThat(created.getDescription()).as("Description").isEqualTo("The description");
		assertThat(created.getType()).as("Type").isEqualTo(Type.PHP);
		assertThat(created.getCreator()).as("Creator").isNotNull();
		assertThat(created.getCreator().getId()).as("Creator ID").isEqualTo(admin.getId());
		assertThat(created.getEditor()).as("Editor").isNotNull();
		assertThat(created.getEditor().getId()).as("Editor ID").isEqualTo(admin.getId());
		assertThat(created.getCdate()).as("Creation date").isBetween(before, after);
		assertThat(created.getEdate()).as("Edit date").isBetween(before, after);

		// get it again
		try (LoggedInClient client = restContext.client(ADMIN_LOGIN, PASSWORD)) {
			PortalImplementationResponse response = client.get().base().path(PATH).path(Integer.toString(created.getId())).request()
					.get(PortalImplementationResponse.class);
			client.get().assertResponse(response);
			PortalImplementationModel loaded = response.getPortalImplementation();
			assertThat(loaded.getKeyword()).as("Loaded keyword").isEqualTo("php");
			assertThat(loaded.getType()).as("Loaded type").isEqualTo(Type.PHP);
			assertThat(loaded.getGlobalId()).as("Loaded global ID").isEqualTo(created.getGlobalId());
			assertThat(loaded.getCreator().getId()).as("Loaded creator ID").isEqualTo(admin.getId());

			// get by global ID
			response = client.get().base().path(PATH).path(created.getGlobalId()).request().get(PortalImplementationResponse.class);
			client.get().assertResponse(response);
			assertThat(response.getPortalImplementation().getId()).as("ID loaded by global ID").isEqualTo(created.getId());
		}
	}

	/**
	 * Test creating a portal implementation with a provided global ID
	 * @throws Exception
	 */
	@Test
	public void testCreateWithGlobalId() throws Exception {
		String globalId = "A547.12345678-1234-1234-1234-123456789abc";
		PortalImplementationModel created = create(ADMIN_LOGIN, withGlobalId(model("java", "Java Portal", Type.Java), globalId));
		assertThat(created.getGlobalId()).as("Global ID").isEqualTo(globalId);
		assertThat(created.getType()).as("Type").isEqualTo(Type.Java);

		// creating another one with the same global ID fails
		try (LoggedInClient client = restContext.client(ADMIN_LOGIN, PASSWORD)) {
			Response response = post(client, withGlobalId(model("other", "Other Portal", Type.PHP), globalId));
			assertThat(response.getStatus()).as("Response status").isEqualTo(Status.CONFLICT.getStatusCode());
		}
	}

	/**
	 * Test that keyword must be unique
	 * @throws Exception
	 */
	@Test
	public void testDuplicateKeyword() throws Exception {
		create(ADMIN_LOGIN, model("php", "PHP Portal", Type.PHP));

		try (LoggedInClient client = restContext.client(ADMIN_LOGIN, PASSWORD)) {
			Response response = post(client, model("php", "Other Portal", Type.Java));
			assertThat(response.getStatus()).as("Response status").isEqualTo(Status.CONFLICT.getStatusCode());
			GenericResponse body = response.readEntity(GenericResponse.class);
			assertThat(body.getResponseInfo().getResponseCode()).as("Response code").isEqualTo(ResponseCode.INVALIDDATA);
			GCNAssertions.assertThat(body).containsMessage(Message.Type.CRITICAL, "portal_implementation.duplicate.keyword", "php");
		}
		assertThat(countPortalImplementations()).as("Number of Portal Implementations").isEqualTo(1);
	}

	/**
	 * Test that name must be unique
	 * @throws Exception
	 */
	@Test
	public void testDuplicateName() throws Exception {
		create(ADMIN_LOGIN, model("php", "PHP Portal", Type.PHP));

		try (LoggedInClient client = restContext.client(ADMIN_LOGIN, PASSWORD)) {
			Response response = post(client, model("other", "PHP Portal", Type.Java));
			assertThat(response.getStatus()).as("Response status").isEqualTo(Status.CONFLICT.getStatusCode());
			GenericResponse body = response.readEntity(GenericResponse.class);
			assertThat(body.getResponseInfo().getResponseCode()).as("Response code").isEqualTo(ResponseCode.INVALIDDATA);
			GCNAssertions.assertThat(body).containsMessage(Message.Type.CRITICAL, "portal_implementation.duplicate.name", "PHP Portal");
		}
		assertThat(countPortalImplementations()).as("Number of Portal Implementations").isEqualTo(1);
	}

	/**
	 * Test that updating to a keyword of another portal implementation fails
	 * @throws Exception
	 */
	@Test
	public void testUpdateDuplicateKeyword() throws Exception {
		create(ADMIN_LOGIN, model("php", "PHP Portal", Type.PHP));
		PortalImplementationModel java = create(ADMIN_LOGIN, model("java", "Java Portal", Type.Java));

		try (LoggedInClient client = restContext.client(ADMIN_LOGIN, PASSWORD)) {
			Response response = client.get().base().path(PATH).path(Integer.toString(java.getId())).request()
					.put(Entity.entity(new PortalImplementationModel().setKeyword("php"), MediaType.APPLICATION_JSON_TYPE));
			assertThat(response.getStatus()).as("Response status").isEqualTo(Status.CONFLICT.getStatusCode());
		}
	}

	/**
	 * Test that keyword, name and type are mandatory
	 * @throws Exception
	 */
	@Test
	public void testMandatoryAttributes() throws Exception {
		try (LoggedInClient client = restContext.client(ADMIN_LOGIN, PASSWORD)) {
			for (PortalImplementationModel invalid : Arrays.asList(model(null, "Name", Type.PHP), model("keyword", null, Type.PHP),
					model("keyword", "Name", null), model("  ", "Name", Type.PHP))) {
				Response response = post(client, invalid);
				assertThat(response.getStatus()).as("Response status").isEqualTo(Status.BAD_REQUEST.getStatusCode());
				response.close();
			}
		}
		assertThat(countPortalImplementations()).as("Number of Portal Implementations").isEqualTo(0);
	}

	/**
	 * Test updating a portal implementation: changed attributes are stored, editor and edit date are updated, creator is unchanged
	 * @throws Exception
	 */
	@Test
	public void testUpdate() throws Exception {
		// create as the restricted user (who gets create permission for that)
		Trx.operate(() -> PermHandler.setPermissions(PortalImplementation.TYPE_PORTAL_IMPLEMENTATIONS, Arrays.asList(subGroup),
				Permissions.get(PERM_VIEW, PERM_CONTENTREPOSITORY_CREATE).toString()));
		PortalImplementationModel created = create(USER_LOGIN, model("php", "PHP Portal", Type.PHP).setDescription("Old description"));

		// make the creation date and edit date older, so that the update can be detected
		Trx.operate(() -> DBUtils.update("UPDATE portal_implementation SET cdate = 1000, edate = 1000 WHERE id = ?", created.getId()));
		Trx.operate(t -> t.dirtObjectCache(PortalImplementation.class, created.getId()));

		try (LoggedInClient client = restContext.client(ADMIN_LOGIN, PASSWORD)) {
			PortalImplementationResponse response = client.get().base().path(PATH).path(Integer.toString(created.getId())).request()
					.put(Entity.entity(new PortalImplementationModel().setName("New Name").setDescription("New description").setType(Type.Java),
							MediaType.APPLICATION_JSON_TYPE), PortalImplementationResponse.class);
			client.get().assertResponse(response);
			PortalImplementationModel updated = response.getPortalImplementation();

			assertThat(updated.getKeyword()).as("Keyword").isEqualTo("php");
			assertThat(updated.getName()).as("Name").isEqualTo("New Name");
			assertThat(updated.getDescription()).as("Description").isEqualTo("New description");
			assertThat(updated.getType()).as("Type").isEqualTo(Type.Java);
			assertThat(updated.getCdate()).as("Creation date").isEqualTo(1000);
			assertThat(updated.getEdate()).as("Edit date").isGreaterThan(1000);
			assertThat(updated.getCreator().getId()).as("Creator ID").isEqualTo(user.getId());
			assertThat(updated.getEditor().getId()).as("Editor ID").isEqualTo(admin.getId());
		}
	}

	/**
	 * Test deleting a portal implementation, which also removes its permissions
	 * @throws Exception
	 */
	@Test
	public void testDelete() throws Exception {
		PortalImplementationModel created = create(ADMIN_LOGIN, model("php", "PHP Portal", Type.PHP));
		assertThat(countPerms(created.getId())).as("Number of permission entries before deletion").isPositive();

		try (LoggedInClient client = restContext.client(ADMIN_LOGIN, PASSWORD)) {
			Response response = client.get().base().path(PATH).path(Integer.toString(created.getId())).request().delete();
			assertThat(response.getStatus()).as("Response status").isEqualTo(Status.NO_CONTENT.getStatusCode());

			response = client.get().base().path(PATH).path(Integer.toString(created.getId())).request().get();
			assertThat(response.getStatus()).as("Response status of get after delete").isEqualTo(Status.NOT_FOUND.getStatusCode());
		}

		assertThat(countPortalImplementations()).as("Number of Portal Implementations").isEqualTo(0);
		assertThat(countPerms(created.getId())).as("Number of permission entries after deletion").isEqualTo(0);
	}

	/**
	 * Test that the creating groups get initial permissions (without the grant permission, if the group may not grant permissions on the admin type)
	 * @throws Exception
	 */
	@Test
	public void testInitialPermissions() throws Exception {
		Trx.operate(() -> PermHandler.setPermissions(PortalImplementation.TYPE_PORTAL_IMPLEMENTATIONS, Arrays.asList(subGroup),
				Permissions.get(PERM_VIEW, PERM_CONTENTREPOSITORY_CREATE).toString()));
		PortalImplementationModel created = create(USER_LOGIN, model("php", "PHP Portal", Type.PHP));

		assertPermBits(subGroup, created.getId(), PERM_VIEW, PERM_CONTENTREPOSITORY_UPDATE, PERM_CONTENTREPOSITORY_DELETE);
		// node group may grant permissions on the admin type (FULL_PERM)
		assertPermBits(nodeGroup, created.getId(), PERM_VIEW, PERM_CHANGE_PERM, PERM_CONTENTREPOSITORY_UPDATE, PERM_CONTENTREPOSITORY_DELETE);
	}

	/**
	 * Test that a user without create permission cannot create portal implementations
	 * @throws Exception
	 */
	@Test
	public void testCreateWithoutPermission() throws Exception {
		Trx.operate(() -> PermHandler.setPermissions(PortalImplementation.TYPE_PORTAL_IMPLEMENTATIONS, Arrays.asList(subGroup),
				Permissions.get(PERM_VIEW).toString()));

		try (LoggedInClient client = restContext.client(USER_LOGIN, PASSWORD)) {
			Response response = post(client, model("php", "PHP Portal", Type.PHP));
			assertThat(response.getStatus()).as("Response status").isEqualTo(Status.FORBIDDEN.getStatusCode());
		}
		assertThat(countPortalImplementations()).as("Number of Portal Implementations").isEqualTo(0);
	}

	/**
	 * Test that a user without view permission on the admin type cannot list portal implementations
	 * @throws Exception
	 */
	@Test
	public void testListWithoutAdminPermission() throws Exception {
		try (LoggedInClient client = restContext.client(USER_LOGIN, PASSWORD)) {
			Response response = client.get().base().path(PATH).request().get();
			assertThat(response.getStatus()).as("Response status").isEqualTo(Status.FORBIDDEN.getStatusCode());
		}
	}

	/**
	 * Test that permissions on single portal implementations can be set via the /perm resource, and are respected for list, get, update and delete
	 * @throws Exception
	 */
	@Test
	public void testInstancePermissions() throws Exception {
		PortalImplementationModel visible = create(ADMIN_LOGIN, model("php", "PHP Portal", Type.PHP));
		PortalImplementationModel hidden = create(ADMIN_LOGIN, model("java", "Java Portal", Type.Java));

		// user may view the admin type
		Trx.operate(() -> PermHandler.setPermissions(PortalImplementation.TYPE_PORTAL_IMPLEMENTATIONS, Arrays.asList(subGroup),
				Permissions.get(PERM_VIEW).toString()));

		// admin grants view permission on the first one via the REST API
		try (LoggedInClient client = restContext.client(ADMIN_LOGIN, PASSWORD)) {
			GenericResponse response = client.get().base().path("perm").path("portalimplementation").path(Integer.toString(visible.getId()))
					.request().post(Entity.entity(new SetPermsRequest().setGroupId(subGroup.getId()).setPerm(Permissions.get(PERM_VIEW).toString()),
							MediaType.APPLICATION_JSON_TYPE), GenericResponse.class);
			client.get().assertResponse(response);
		}

		try (LoggedInClient client = restContext.client(USER_LOGIN, PASSWORD)) {
			PortalImplementationListResponse list = client.get().base().path(PATH).request().get(PortalImplementationListResponse.class);
			client.get().assertResponse(list);
			List<String> keywords = list.getItems().stream().map(PortalImplementationModel::getKeyword).collect(Collectors.toList());
			assertThat(keywords).as("Listed keywords").containsExactly("php");

			Response response = client.get().base().path(PATH).path(Integer.toString(visible.getId())).request().get();
			assertThat(response.getStatus()).as("Status of get visible").isEqualTo(Status.OK.getStatusCode());
			response.close();

			response = client.get().base().path(PATH).path(Integer.toString(hidden.getId())).request().get();
			assertThat(response.getStatus()).as("Status of get hidden").isEqualTo(Status.FORBIDDEN.getStatusCode());
			response.close();

			response = client.get().base().path(PATH).path(Integer.toString(visible.getId())).request()
					.put(Entity.entity(new PortalImplementationModel().setName("Changed"), MediaType.APPLICATION_JSON_TYPE));
			assertThat(response.getStatus()).as("Status of update without edit permission").isEqualTo(Status.FORBIDDEN.getStatusCode());
			response.close();

			response = client.get().base().path(PATH).path(Integer.toString(visible.getId())).request().delete();
			assertThat(response.getStatus()).as("Status of delete without delete permission").isEqualTo(Status.FORBIDDEN.getStatusCode());
			response.close();
		}
		assertThat(countPortalImplementations()).as("Number of Portal Implementations").isEqualTo(2);
	}

	/**
	 * Create a model
	 * @param keyword keyword
	 * @param name name
	 * @param type type
	 * @return model
	 */
	protected static PortalImplementationModel model(String keyword, String name, Type type) {
		return new PortalImplementationModel().setKeyword(keyword).setName(name).setType(type);
	}

	/**
	 * Set the global ID on the model
	 * @param model model
	 * @param globalId global ID
	 * @return model
	 */
	protected static PortalImplementationModel withGlobalId(PortalImplementationModel model, String globalId) {
		model.setGlobalId(globalId);
		return model;
	}

	/**
	 * Post the model to create a portal implementation
	 * @param client client
	 * @param model model
	 * @return response
	 */
	protected static Response post(LoggedInClient client, PortalImplementationModel model) throws RestException {
		return client.get().base().path(PATH).request().post(Entity.entity(model, MediaType.APPLICATION_JSON_TYPE));
	}

	/**
	 * Create a portal implementation as the given user and assert success
	 * @param login login
	 * @param model model
	 * @return created model
	 * @throws RestException
	 */
	protected static PortalImplementationModel create(String login, PortalImplementationModel model) throws RestException {
		try (LoggedInClient client = restContext.client(login, PASSWORD)) {
			PortalImplementationResponse response = client.get().base().path(PATH).request()
					.post(Entity.entity(model, MediaType.APPLICATION_JSON_TYPE), PortalImplementationResponse.class);
			client.get().assertResponse(response);
			assertThat(response.getPortalImplementation()).as("Created Portal Implementation").isNotNull();
			return response.getPortalImplementation();
		}
	}

	/**
	 * Count the portal implementations in the DB
	 * @return count
	 * @throws NodeException
	 */
	protected static int countPortalImplementations() throws NodeException {
		return Trx.supply(() -> DBUtils.select("SELECT id FROM portal_implementation", DBUtils.IDS).size());
	}

	/**
	 * Count the permission entries of the portal implementation
	 * @param id ID
	 * @return count
	 * @throws NodeException
	 */
	protected static int countPerms(int id) throws NodeException {
		return Trx.supply(() -> DBUtils.select("SELECT usergroup_id id FROM perm WHERE o_type = ? AND o_id = ?", ps -> {
			ps.setInt(1, PortalImplementation.TYPE_PORTAL_IMPLEMENTATION);
			ps.setInt(2, id);
		}, DBUtils.IDS).size());
	}

	/**
	 * Assert that the group has exactly the given permission bits on the portal implementation
	 * @param group group
	 * @param id portal implementation ID
	 * @param bits expected bits
	 * @throws NodeException
	 */
	protected static void assertPermBits(UserGroup group, int id, Integer... bits) throws NodeException {
		Set<Integer> expected = new HashSet<>(Arrays.asList(bits));
		Trx.operate(t -> {
			PermHandler groupPermHandler = t.getGroupPermHandler(group.getId());
			for (int bit = 0; bit < 32; bit++) {
				assertThat(groupPermHandler.checkPermissionBit(PortalImplementation.TYPE_PORTAL_IMPLEMENTATION, id, bit))
						.as(String.format("Permission bit %d for group %s", bit, group.getName())).isEqualTo(expected.contains(bit));
			}
		});
	}
}
